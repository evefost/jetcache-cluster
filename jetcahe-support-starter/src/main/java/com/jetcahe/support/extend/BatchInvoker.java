package com.jetcahe.support.extend;

import com.alibaba.fastjson.JSON;
import com.alicp.jetcache.Cache;
import com.alicp.jetcache.CacheBuilder;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.method.CacheInvokeConfig;
import com.alicp.jetcache.anno.method.CacheInvokeContext;
import com.alicp.jetcache.anno.support.CacheInvalidateAnnoConfig;
import com.alicp.jetcache.anno.support.GlobalCacheConfig;
import com.jetcahe.support.CacheCompositeResult;
import com.jetcahe.support.InParamParseResult;
import com.jetcahe.support.OutParamParseResult;
import com.jetcahe.support.Pair;
import com.jetcahe.support.redis.JedisPileLineOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.EvaluationContext;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 批量调用处理
 *
 * @author xieyang
 */
public class BatchInvoker {

    private static Logger logger = LoggerFactory.getLogger(BatchInvoker.class);

    private static Cache localCache;

    /**
     * 批量处理缓存
     *
     * @param context
     * @return
     * @throws Throwable
     */
    public static Object invokeWithCached(CacheInvokeContext context)
            throws Throwable {
        CacheInvokeConfig cic = context.getCacheInvokeConfig();
        BatchCachedAnnoConfig cac = (BatchCachedAnnoConfig) cic.getCachedAnnoConfig();
        Cache cache = context.getCacheFunction().apply(context, cac);
        if (cache == null) {
            logger.error("no cache with name: " + context.getMethod());
            return invokeOrigin(context);
        }

        createLocalCache(cac);

        CacheCompositeResult fromCache = getFromCache(context, cac);
        List<Object> noCacheParamList = fromCache.getNoCacheParams();
        if (noCacheParamList.isEmpty()) {
            //缓存全部命中
            return fromCache.getCaches();
        }
        InParamParseResult inParamParseResult = fromCache.getInParamParseResult();
        List<Pair<Object, Object>> originKeyParams = inParamParseResult.getElementTargetValuePairs();
        boolean paramsNeedChange = noCacheParamList.size()!=originKeyParams.size();
        List dbList;
        if(paramsNeedChange){
            //修改入参/还原入参
            try {
                inParamParseResult.rewriteArgsList(context.getArgs(),noCacheParamList);
                dbList = (List) invokeOrigin(context);
            }finally {
                inParamParseResult.restoreArgsList(context.getArgs());
            }
        }else {
            dbList = (List) invokeOrigin(context);
        }
        List<Object> caches = fromCache.getCaches();
        if (dbList != null && !dbList.isEmpty()) {
            caches.addAll(dbList);
            save2Cache(dbList, inParamParseResult, cac);
        }
        return caches;

    }

    private static CacheCompositeResult getFromCache(CacheInvokeContext context, BatchCachedAnnoConfig cac) {
        //返回对应的参数，用作后续构造无缓存查库入参
        InParamParseResult paramParseResult = BatchExpressUtils.evalKey(context);
        List<Pair<Object, Object>> originKeys = paramParseResult.getElementTargetValuePairs();
        Map<Object/*key*/, Object> keyParamMap = originKeys.stream().map((pair) -> new Pair(cac.getName() + pair.getKey().toString(), pair.getValue())).collect(Collectors.toMap(Pair::getKey, Pair::getValue));
        //转为查缓存的key
        List<String> allCacheKeys = originKeys.stream().map(pair -> cac.getName() + pair.getKey().toString()).collect(Collectors.toList());
        if (cac.getCacheType().equals(CacheType.LOCAL)) {
            CacheCompositeResult compositeResult = getFromLocal(allCacheKeys, keyParamMap);
            compositeResult.setInParamParseResult(paramParseResult);
            return compositeResult;
        } else if (cac.getCacheType().equals(CacheType.REMOTE)) {
            CacheCompositeResult compositeResult = getFromRemote(allCacheKeys, keyParamMap, context, cac);
            compositeResult.setInParamParseResult(paramParseResult);
            return compositeResult;
        }

        CacheCompositeResult fromLocal = getFromLocal(allCacheKeys, keyParamMap);
        List<String> noLocalCacheKeys = fromLocal.getNoCacheKeys();
        fromLocal.setInParamParseResult(paramParseResult);
        if (noLocalCacheKeys.isEmpty()) {
            //本地缓存全部命中
            return fromLocal;
        }
        Map<Object, Object> noLocalCacheKeyParamMap = new HashMap<>(noLocalCacheKeys.size());
        noLocalCacheKeys.forEach((key) -> {
            noLocalCacheKeyParamMap.put(key, keyParamMap.get(key));
        });
        CacheCompositeResult fromRemote = getFromRemote(noLocalCacheKeys, noLocalCacheKeyParamMap, context, cac);
        //合并结果
        List<Object> remoteCaches = fromRemote.getCaches();
        List<Object> noCacheParams = fromRemote.getNoCacheParams();
        fromLocal.getCaches().addAll(remoteCaches);
        fromLocal.setNoCacheParams(noCacheParams);
        return fromLocal;

    }


    private static CacheCompositeResult getFromLocal(List<String> cacheKeys, Map<Object/*key*/,/*param*/ Object> keyParamMap) {
        List<Object> localCaches = new ArrayList<>(cacheKeys.size());
        List<String> noLocalCacheKeys = new ArrayList<>();
        List<Object> noCacheParamList = new ArrayList<>();
        for (String key : cacheKeys) {
            Object value = localCache.get(key);
            if (value != null) {
                localCaches.add(value);
            } else {
                noLocalCacheKeys.add(key);
                noCacheParamList.add(keyParamMap.get(key));
            }
        }
        CacheCompositeResult compositeResult = new CacheCompositeResult();
        compositeResult.setCaches(localCaches);
        compositeResult.setNoCacheParams(noCacheParamList);
        compositeResult.setNoCacheKeys(noLocalCacheKeys);
        return compositeResult;

    }

    private static CacheCompositeResult getFromRemote(List<String> cacheKeys, Map<Object/*key*/,/*param*/Object> keyParamMap, CacheInvokeContext context, BatchCachedAnnoConfig cac) {
        //获取反序列化泛参的实际类型
        Method method = context.getMethod();
        Type genericReturnType = method.getGenericReturnType();
        Class<?> targetType = JedisPileLineOperator.getTargetType(genericReturnType);
        CacheCompositeResult compositeResult = new CacheCompositeResult();
        try {
            List<? extends Pair<String, ?>> remoteCachePairs = JedisPileLineOperator.batchReadPair(cacheKeys, targetType, false, cac.isHash(), cac.getHashField());
            List<Object> remoteCaches = new ArrayList<>();
            List<String> noCacheKeys = new ArrayList<>();
            for (Pair<String, ?> pair : remoteCachePairs) {
                Object value = pair.getValue();
                if (value != null) {
                    remoteCaches.add(value);
                } else {
                    noCacheKeys.add(pair.getKey());
                }
            }
            compositeResult.setCaches(remoteCaches);
            compositeResult.setNoCacheKeys(noCacheKeys);
            List<Object> noCacheParamList = remoteCachePairs.stream().filter(p -> p.getValue() == null).map(p -> keyParamMap.get(p.getKey())).collect(Collectors.toList());
            compositeResult.setNoCacheParams(noCacheParamList);
            return compositeResult;
        } catch (Exception ex) {
            logger.error("获取远程缓存[{}]失败:", JSON.toJSONString(cacheKeys), ex);
        }
        List<Object> noCacheParamList = new ArrayList<>(keyParamMap.size());
        keyParamMap.forEach((k, v) -> noCacheParamList.add(v));
        compositeResult.setCaches(new ArrayList<>());
        compositeResult.setNoCacheKeys(cacheKeys);
        compositeResult.setNoCacheParams(noCacheParamList);
        return compositeResult;

    }

    private static void createLocalCache(BatchCachedAnnoConfig cac) {
        if (localCache == null) {
            GlobalCacheConfig globalCacheConfig = cac.getGlobalCacheConfig();
            Map<String, CacheBuilder> localCacheBuilders = globalCacheConfig.getLocalCacheBuilders();
            CacheBuilder aDefault = localCacheBuilders.get("default");
            synchronized (BatchInvoker.class) {
                if (localCache == null) {
                    localCache = aDefault.buildCache();
                }
            }
        }
    }

    private static void save2Cache(List dbList, InParamParseResult paramParseResult, BatchCachedAnnoConfig cac) {
        try {
            //根据配置信息重新生成对应的缓存key
            EvaluationContext evalContext = paramParseResult.getContext();
            evalContext.setVariable(cac.getReturnListName(), dbList);
            String returnScript = cac.getReturnKey();
            OutParamParseResult parseResult = BatchExpressUtils.parseOutParams(returnScript, evalContext);
            List<Pair<Object, Object>> retPairs = parseResult.getElementTargetValuePairs();
            //转成键值对形式
            List<Pair<String/*key*/, Object>> dbResultKeyValuePairs = retPairs.stream().map(p -> new Pair<String, Object>(cac.getName() + p.getKey().toString(), p.getValue())).collect(Collectors.toList());
            //写到缓存里
            TimeUnit timeUnit = cac.getTimeUnit();
            if (cac.getCacheType().equals(CacheType.LOCAL)) {
                dbResultKeyValuePairs.forEach(p -> localCache.put(p.getKey(), p.getValue(), cac.getLocalExpire(), cac.getTimeUnit()));
                return;
            } else if (cac.getCacheType().equals(CacheType.REMOTE)) {
                long expireSeconds = timeUnit.toSeconds(cac.getExpire());
                JedisPileLineOperator.batchWritePair(dbResultKeyValuePairs, expireSeconds, cac.isHash(), cac.getHashField());
                return;
            }
            dbResultKeyValuePairs.forEach(p -> localCache.put(p.getKey(), p.getValue(), cac.getLocalExpire(), cac.getTimeUnit()));
            long expireSeconds = timeUnit.toSeconds(cac.getExpire());
            JedisPileLineOperator.batchWritePair(dbResultKeyValuePairs, expireSeconds, cac.isHash(), cac.getHashField());
        } catch (Exception e) {
            logger.error("保存缓存失败", e);
        }
    }

    private static Object invokeOrigin(CacheInvokeContext context) throws Throwable {
        return context.getInvoker().invoke();
    }


    public static void doInvalidate(CacheInvokeContext context, CacheInvalidateAnnoConfig cac) {
        try {
            InParamParseResult inParamParseResult = BatchExpressUtils.evalKey(context);
            List<Pair<Object, Object>> originKeys = inParamParseResult.getElementTargetValuePairs();
            List<String> keys = originKeys.stream().map(pair -> cac.getName() + pair.getKey().toString()).collect(Collectors.toList());
            JedisPileLineOperator.batchDelete(keys);
        } catch (Exception e) {
            logger.error("删缓存失败", e);
        }
    }

}
