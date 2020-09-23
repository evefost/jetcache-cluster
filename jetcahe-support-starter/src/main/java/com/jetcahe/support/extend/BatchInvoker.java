package com.jetcahe.support.extend;

import com.alibaba.fastjson.JSON;
import com.alicp.jetcache.Cache;
import com.alicp.jetcache.CacheBuilder;
import com.alicp.jetcache.CacheGetResult;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.method.CacheInvokeConfig;
import com.alicp.jetcache.anno.method.CacheInvokeContext;
import com.alicp.jetcache.anno.support.*;
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
import java.util.*;
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

    private static CacheManager cacheManager;

    public static void setCacheManager(CacheManager cacheManager) {
        BatchInvoker.cacheManager = cacheManager;
    }

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
        boolean paramsNeedChange = noCacheParamList.size() != originKeyParams.size();
        List dbList;
        if (paramsNeedChange) {
            //修改入参/还原入参
            try {
                inParamParseResult.rewriteArgsList(context.getArgs(), noCacheParamList);
                dbList = (List) invokeOrigin(context);
            } finally {
                inParamParseResult.restoreArgsList(context.getArgs());
            }
        } else {
            dbList = (List) invokeOrigin(context);
        }
        List<Object> caches = fromCache.getCaches();
        if (dbList != null) {
            caches.addAll(dbList);
        }
        save2Cache(dbList, inParamParseResult, cac, fromCache.getNoCacheKeys());
        return caches;

    }

    private static CacheCompositeResult getFromCache(CacheInvokeContext context, BatchCachedAnnoConfig cac) {
        //返回对应的参数，用作后续构造无缓存查库入参

        InParamParseResult paramParseResult = BatchExpressUtils.evalKey(context);
        List<Pair<Object, Object>> originKeys = paramParseResult.getElementTargetValuePairs();
        Map<Object/*key*/, Object> keyParamMap = originKeys.stream().map((pair) -> new Pair(buildKey(cac,pair), pair.getValue())).collect(Collectors.toMap(Pair::getKey, Pair::getValue));
        //转为查缓存的key
        Set<String> allCacheKeys = originKeys.stream().map(pair -> buildKey(cac,pair)).collect(Collectors.toSet());
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
        Set<String> noLocalCacheKeys = fromLocal.getNoCacheKeys();
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
        fromLocal.getCaches().addAll(fromRemote.getCaches());
        fromLocal.setNoCacheKeys(fromRemote.getNoCacheKeys());
        fromLocal.setNoCacheParams(fromRemote.getNoCacheParams());
        return fromLocal;

    }



    private static <V> CacheCompositeResult<V> getFromLocal(Set<String> cacheKeys, Map<Object/*key*/,/*param*/ Object> keyParamMap) {
        List<V> localCaches = new ArrayList<>(cacheKeys.size());
        Set<String> noCacheKeys = new HashSet<>();
        List<Object> noCacheParamList = new ArrayList<>();
        for (String key : cacheKeys) {
            CacheGetResult<V> cacheResult = localCache.GET(key);
            processSingeCacheResult(key,cacheResult,localCaches,noCacheKeys,noCacheParamList,keyParamMap);
        }
        CacheCompositeResult compositeResult = new CacheCompositeResult();
        compositeResult.setCaches(localCaches);
        compositeResult.setNoCacheParams(noCacheParamList);
        compositeResult.setNoCacheKeys(noCacheKeys);
        return compositeResult;
    }

    private static <V> void processSingeCacheResult(String key, CacheGetResult<V> cacheResult, List<V> caches , Set<String> noCacheKeys  , List<Object> noCacheParamList, Map<Object/*key*/,/*param*/ Object> keyParamMap){
        if (cacheResult.isSuccess()) {
            V value = cacheResult.getValue();
            //可能存的是空值(db也没有值),因为是列表，不返回空值
            if(value != null){
                caches.add(value);
            }
        } else {
            noCacheKeys.add(key);
            noCacheParamList.add(keyParamMap.get(key));
        }
    }

    private static <V> CacheCompositeResult<V> getFromRemote(Set<String> cacheKeys, Map<Object/*key*/,/*param*/Object> keyParamMap, CacheInvokeContext context, BatchCachedAnnoConfig cac) {
        //获取反序列化泛参的实际类型
        Method method = context.getMethod();
        Type genericReturnType = method.getGenericReturnType();
        Class<V> targetType = (Class<V>) JedisPileLineOperator.getTargetType(genericReturnType);
        CacheCompositeResult compositeResult = new CacheCompositeResult();
        try {
            List<Pair<String, CacheGetResult<V>>> remoteCachePairs = JedisPileLineOperator.batchReadResultPair(cacheKeys, targetType, false, cac.isHash(), cac.getHashField());
            List<V> remoteCaches = new ArrayList<>();
            Set<String> noCacheKeys = new HashSet<>();
            List<Object> noCacheParamList = new ArrayList<>();
            for (Pair<String, CacheGetResult<V>> pair : remoteCachePairs) {
                String key = pair.getKey();
                CacheGetResult<V> cacheResult = pair.getValue();
                processSingeCacheResult(key,cacheResult,remoteCaches,noCacheKeys,noCacheParamList,keyParamMap);
            }
            compositeResult.setCaches(remoteCaches);
            compositeResult.setNoCacheKeys(noCacheKeys);
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

    private static void save2Cache(List dbList, InParamParseResult paramParseResult, BatchCachedAnnoConfig cac, Set<String> noCacheKeys) {

        if(dbList == null){
            return;
        }
        boolean cacheNullValue = cac.isCacheNullValue();
        if(dbList.isEmpty() && !cacheNullValue){
            return;
        }
        try {
            //根据配置信息重新生成对应的缓存key
            EvaluationContext evalContext = paramParseResult.getContext();
            evalContext.setVariable(cac.getReturnListName(), dbList);
            String returnScript = cac.getReturnKey();
            OutParamParseResult parseResult = BatchExpressUtils.parseOutParams(returnScript, evalContext);
            List<Pair<Object, Object>> retPairs = parseResult.getElementTargetValuePairs();
            Map<Object, Object> dbKeyValueMap = new HashMap<>(retPairs.size());
            List<Pair<String, Object>> dbResultKeyValuePairs = new ArrayList<>();
            for (Pair<Object, Object> retPair : retPairs) {
                String key = buildKey(cac,retPair);
                Object value = retPair.getValue();
                dbKeyValueMap.put(key, value);
            }
            if (cacheNullValue) {
                noCacheKeys.forEach(key -> dbResultKeyValuePairs.add(new Pair<>(key, dbKeyValueMap.get(key))));
            } else {
                dbKeyValueMap.forEach((key, value) -> dbResultKeyValuePairs.add(new Pair<String, Object>((String) key, dbKeyValueMap.get(key))));
            }
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
            //无值key = allKey - cacheKeys -dbKeys
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


    public static void doInvalidate(CacheInvokeContext context,Cache cache , CacheInvalidateAnnoConfig cac) {
        try {
            InParamParseResult inParamParseResult = BatchExpressUtils.evalKey(context);
            List<Pair<Object, Object>> originKeys = inParamParseResult.getElementTargetValuePairs();
            Set<String> keys = originKeys.stream().map(pair ->buildKey(cac,pair)).collect(Collectors.toSet());
            localCache.removeAll(keys);
            cache.removeAll(keys);
        } catch (Exception e) {
            logger.error("删缓存失败", e);
        }
    }

    private static String buildKey(CacheAnnoConfig cac,Pair<Object, Object> pair){
        String prefix = cac.getName();
        String cacheKey = prefix+pair.getKey().toString();
        return cacheKey;
    }

}
