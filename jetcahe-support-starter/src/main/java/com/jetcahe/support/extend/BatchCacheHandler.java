/**
 * Created on  13-09-09 15:59
 */
package com.jetcahe.support.extend;

import com.alibaba.fastjson.JSON;
import com.alicp.jetcache.*;
import com.alicp.jetcache.anno.method.CacheInvokeConfig;
import com.alicp.jetcache.anno.method.CacheInvokeContext;
import com.alicp.jetcache.anno.method.ClassUtil;
import com.alicp.jetcache.anno.support.*;
import com.alicp.jetcache.event.CacheLoadEvent;
import com.jetcahe.support.Pair;
import com.jetcahe.support.InParamParseResult;
import com.jetcahe.support.OutParamParseResult;
import com.jetcahe.support.redis.JedisPileLineOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;


/**
 * 批量缓存处理
 * @author xieyang
 */
public class BatchCacheHandler implements InvocationHandler {
    private static Logger logger = LoggerFactory.getLogger(BatchCacheHandler.class);
    private Object src;
    private Supplier<CacheInvokeContext> contextSupplier;
    private String[] hiddenPackages;
    private ConfigMap configMap;

    private static class CacheContextSupport extends CacheContext {

        public CacheContextSupport() {
            super(null, null);
        }

        static void _enable() {
            enable();
        }

        static void _disable() {
            disable();
        }

        static boolean _isEnabled() {
            return isEnabled();
        }
    }

    public BatchCacheHandler(Object src, ConfigMap configMap, Supplier<CacheInvokeContext> contextSupplier, String[] hiddenPackages) {
        this.src = src;
        this.configMap = configMap;
        this.contextSupplier = contextSupplier;
        this.hiddenPackages = hiddenPackages;
    }

    @Override
    public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable {
        CacheInvokeContext context = null;

        String sig = ClassUtil.getMethodSig(method);
        CacheInvokeConfig cac = configMap.getByMethodInfo(sig);
        if (cac != null) {
            context = contextSupplier.get();
            context.setCacheInvokeConfig(cac);
        }
        if (context == null) {
            return method.invoke(src, args);
        } else {
            context.setInvoker(() -> method.invoke(src, args));
            context.setHiddenPackages(hiddenPackages);
            context.setArgs(args);
            context.setMethod(method);
            return invoke(context);
        }
    }

    public static Object invoke(CacheInvokeContext context) throws Throwable {
        if (context.getCacheInvokeConfig().isEnableCacheContext()) {
            try {
                CacheContextSupport._enable();
                return doInvoke(context);
            } finally {
                CacheContextSupport._disable();
            }
        } else {
            return doInvoke(context);
        }
    }

    private static Object doInvoke(CacheInvokeContext context) throws Throwable {
        CacheInvokeConfig cic = context.getCacheInvokeConfig();
        CachedAnnoConfig cachedConfig = cic.getCachedAnnoConfig();
        if (cachedConfig != null && (cachedConfig.isEnabled() || CacheContextSupport._isEnabled())) {
            return invokeWithCached(context);
        } else if (cic.getInvalidateAnnoConfigs() != null || cic.getUpdateAnnoConfig() != null) {
            return invokeWithInvalidateOrUpdate(context);
        } else {
            return invokeOrigin(context);
        }
    }

    private static Object invokeWithInvalidateOrUpdate(CacheInvokeContext context) throws Throwable {
        Object originResult = invokeOrigin(context);
        context.setResult(originResult);
        CacheInvokeConfig cic = context.getCacheInvokeConfig();
        if (cic.getInvalidateAnnoConfigs() != null) {
            doInvalidate(context, cic.getInvalidateAnnoConfigs());
        }
        CacheUpdateAnnoConfig updateAnnoConfig = cic.getUpdateAnnoConfig();
        if (updateAnnoConfig != null) {
            doUpdate(context, updateAnnoConfig);
        }

        return originResult;
    }

    private static Iterable toIterable(Object obj) {
        if (obj.getClass().isArray()) {
            if (obj instanceof Object[]) {
                return Arrays.asList((Object[]) obj);
            } else {
                List list = new ArrayList();
                int len = Array.getLength(obj);
                for (int i = 0; i < len; i++) {
                    list.add(Array.get(obj, i));
                }
                return list;
            }
        } else if (obj instanceof Iterable) {
            return (Iterable) obj;
        } else {
            return null;
        }
    }

    private static void doInvalidate(CacheInvokeContext context, List<CacheInvalidateAnnoConfig> annoConfig) {
        for (CacheInvalidateAnnoConfig config : annoConfig) {
            doInvalidate(context, config);
        }
    }

    private static void doInvalidate(CacheInvokeContext context, CacheInvalidateAnnoConfig cac) {

        boolean condition = ExpressionUtil.evalCondition(context, cac);
        if (!condition) {
            return;
        }
        try {
            InParamParseResult inParamParseResult = (InParamParseResult) ExpressionUtil.evalKey(context, cac);
            List<Pair<Object, Object>> originKeys = inParamParseResult.getElementTargetValuePairs();
            List<String> keys = originKeys.stream().map(pair->cac.getName()+pair.getKey().toString()).collect(Collectors.toList());
            JedisPileLineOperator.batchDelete(keys);
        }catch (Exception e){
            logger.error("删缓存失败",e);
        }
    }

    private static void doUpdate(CacheInvokeContext context, CacheUpdateAnnoConfig updateAnnoConfig) {
        Cache cache = context.getCacheFunction().apply(context, updateAnnoConfig);
        if (cache == null) {
            return;
        }
        boolean condition = ExpressionUtil.evalCondition(context, updateAnnoConfig);
        if (!condition) {
            return;
        }
        Object key = ExpressionUtil.evalKey(context, updateAnnoConfig);
        Object value = ExpressionUtil.evalValue(context, updateAnnoConfig);
        if (key == null || value == ExpressionUtil.EVAL_FAILED) {
            return;
        }
        if (updateAnnoConfig.isMulti()) {
            if (value == null) {
                return;
            }
            Iterable keyIt = toIterable(key);
            Iterable valueIt = toIterable(value);
            if (keyIt == null) {
                logger.error("jetcache @CacheUpdate key is not instance of Iterable or array: " + updateAnnoConfig.getDefineMethod());
                return;
            }
            if (valueIt == null) {
                logger.error("jetcache @CacheUpdate value is not instance of Iterable or array: " + updateAnnoConfig.getDefineMethod());
                return;
            }

            List keyList = new ArrayList();
            List valueList = new ArrayList();
            keyIt.forEach(o -> keyList.add(o));
            valueIt.forEach(o -> valueList.add(o));
            if (keyList.size() != valueList.size()) {
                logger.error("jetcache @CacheUpdate key size not equals with value size: " + updateAnnoConfig.getDefineMethod());
                return;
            } else {
                Map m = new HashMap();
                for (int i = 0; i < valueList.size(); i++) {
                    m.put(keyList.get(i), valueList.get(i));
                }
                cache.putAll(m);
            }
        } else {
            cache.put(key, value);
        }
    }

    private static Object invokeWithCached(CacheInvokeContext context)
            throws Throwable {
        CacheInvokeConfig cic = context.getCacheInvokeConfig();
        BatchCachedAnnoConfig cac = (BatchCachedAnnoConfig) cic.getCachedAnnoConfig();
        Cache cache = context.getCacheFunction().apply(context, cac);
        if (cache == null) {
            logger.error("no cache with name: " + context.getMethod());
            return invokeOrigin(context);
        }

        //返回对应的参数，用作后续构造无缓存查库入参
        InParamParseResult paramParseResult = (InParamParseResult) ExpressionUtil.evalKey(context, cic.getCachedAnnoConfig());
        List<Pair<Object, Object>>  originKeys = paramParseResult.getElementTargetValuePairs();
        Map<Object/*key*/, Object> keyParamMap = originKeys.stream().map((pair) -> new Pair(cac.getName()+pair.getKey().toString(), pair.getValue())).collect(Collectors.toMap(Pair::getKey, Pair::getValue));
        //转为查缓存的key
        List<String> allCacheKeys = originKeys.stream().map(pair->cac.getName()+pair.getKey().toString()).collect(Collectors.toList());
        //获取反序列化泛参的实际类型
        Method method = context.getMethod();
        Type genericReturnType = method.getGenericReturnType();
        Class<?> targetType = JedisPileLineOperator.getTargetType(genericReturnType);
        try {
            if(logger.isDebugEnabled()){
                logger.debug("从缓存中获取数据:[{}]", JSON.toJSONString(allCacheKeys));
            }
            List<? extends Pair<String, ?>> cachePairs = JedisPileLineOperator.batchReadPair(allCacheKeys, targetType,false,cac.isHash(),cac.getHashField());
            List<Object> noCacheParamList = new ArrayList<>();
            List result = new ArrayList<>();
            for(Pair<String,?> pair:cachePairs){
                //缓存没值
                if(pair.getValue() == null){
                    Object elementParam = keyParamMap.get(pair.getKey());
                    noCacheParamList.add(elementParam);
                }else {
                    result.add(pair.getValue());
                }
            }
            if(noCacheParamList.isEmpty()){
                if(logger.isDebugEnabled()){
                    logger.debug("所有数据从缓存中获取");
                }
                return result;
            }
            boolean paramsNeedChange = noCacheParamList.size()!=cachePairs.size();
            List dbList;
            if(paramsNeedChange){
                //修改入参/还原入参
                try {
                    paramParseResult.rewriteArgsList(context.getArgs(),noCacheParamList);
                    dbList = (List) invokeOrigin(context);
                }finally {
                    paramParseResult.restoreArgsList(context.getArgs());
                }
            }else {
                dbList = (List) invokeOrigin(context);
            }

            if(dbList != null && !dbList.isEmpty()){
                result.addAll(dbList);
                //根据配置信息重新生成对应的缓存key
                EvaluationContext evalContext = paramParseResult.getContext();
                evalContext.setVariable(cac.getReturnListName(),dbList);
                ExpressionParser parser = paramParseResult.getParser();
                String returnScript=cac.getReturnKey();
                OutParamParseResult parseResult = BatchSpelEvaluator.parseOutParams(returnScript, parser, evalContext);
                List<Pair<Object, Object>> retPairs = parseResult.getElementTargetValuePairs();
                //转成键值对形式
                List<Pair<String/*key*/,Object>> dbResultKeyValuePairs = retPairs.stream().map(p -> new Pair<String,Object>(cac.getName()+p.getKey().toString(), p.getValue())).collect(Collectors.toList());
                //写到缓存里
                JedisPileLineOperator.batchWritePair(dbResultKeyValuePairs,cac.getExpire(),cac.isHash(),cac.getHashField());
            }
            //处理穿透，找出数据库无值的key
            return result;
        } catch (CacheInvokeException e) {
            throw e.getCause();
        }
    }

    private void processPenetrate(List<String> allCacheKeys,List<? extends Pair<String, ?>> cachePairs,List<Pair<String/*key*/,Object>> dbResultKeyValuePairs){

        List<String> cacheHasValueKeys = cachePairs.stream().filter(p->p.getValue() != null).map(p -> p.getKey()).collect(Collectors.toList());
        List<String> dbValueKeys = dbResultKeyValuePairs.stream().map(p -> p.getKey()).collect(Collectors.toList());
        allCacheKeys.removeAll(dbValueKeys);
        allCacheKeys.removeAll(cacheHasValueKeys);
        List<String> noDataKeys = allCacheKeys;

    }

    private static Object loadAndCount(CacheInvokeContext context, Cache cache, Object key) throws Throwable {
        long t = System.currentTimeMillis();
        Object v = null;
        boolean success = false;
        try {
            v = invokeOrigin(context);
            success = true;
        } finally {
            t = System.currentTimeMillis() - t;
            CacheLoadEvent event = new CacheLoadEvent(cache, t, key, v, success);
            while (cache instanceof ProxyCache) {
                cache = ((ProxyCache) cache).getTargetCache();
            }
            if (cache instanceof AbstractCache) {
                ((AbstractCache) cache).notify(event);
            }
        }
        return v;
    }

    private static Object invokeOrigin(CacheInvokeContext context) throws Throwable {
        return context.getInvoker().invoke();
    }


}
