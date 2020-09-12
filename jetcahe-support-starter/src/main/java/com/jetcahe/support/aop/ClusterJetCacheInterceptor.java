/**
 * Created on  13-09-18 20:33
 */
package com.jetcahe.support.aop;

import com.alicp.jetcache.anno.aop.CachePointcut;
import com.alicp.jetcache.anno.aop.JetCacheInterceptor;
import com.alicp.jetcache.anno.method.CacheHandler;
import com.alicp.jetcache.anno.method.CacheInvokeConfig;
import com.alicp.jetcache.anno.method.CacheInvokeContext;
import com.alicp.jetcache.anno.support.CachedAnnoConfig;
import com.alicp.jetcache.anno.support.ConfigMap;
import com.alicp.jetcache.anno.support.ConfigProvider;
import com.alicp.jetcache.anno.support.GlobalCacheConfig;
import com.jetcahe.support.extend.BatchCachedAnnoConfig;
import com.jetcahe.support.extend.ExtendCacheHandler;
import com.jetcahe.support.extend.TargetUnSerializableClassHolder;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Method;

/**
 * @author <a href="mailto:areyouok@gmail.com">huangli</a>
 */
public class ClusterJetCacheInterceptor  extends JetCacheInterceptor {

    @Autowired
    private ConfigMap cacheConfigMap;
    private ApplicationContext applicationContext;
    private GlobalCacheConfig globalCacheConfig;
    ConfigProvider configProvider;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public Object invoke(final MethodInvocation invocation) throws Throwable {
        if (configProvider == null) {
            configProvider = applicationContext.getBean(ConfigProvider.class);
        }
        if (configProvider != null && globalCacheConfig == null) {
            globalCacheConfig = configProvider.getGlobalCacheConfig();
        }
        if (globalCacheConfig == null || !globalCacheConfig.isEnableMethodCache()) {
            return invocation.proceed();
        }

        Method method = invocation.getMethod();
        Object obj = invocation.getThis();
        CacheInvokeConfig cac = null;
        if (obj != null) {
            String key = CachePointcut.getKey(method, obj.getClass());
            cac  = cacheConfigMap.getByMethodInfo(key);
        }

        if (cac == null || cac == CacheInvokeConfig.getNoCacheInvokeConfigInstance()) {
            return invocation.proceed();
        }

        CacheInvokeContext context = configProvider.getCacheContext().createCacheInvokeContext(cacheConfigMap);
        context.setTargetObject(invocation.getThis());
        context.setInvoker(invocation::proceed);
        context.setMethod(method);
        context.setArgs(invocation.getArguments());
        context.setCacheInvokeConfig(cac);
        context.setHiddenPackages(globalCacheConfig.getHiddenPackages());
        //
        CachedAnnoConfig cachedConfig = cac.getCachedAnnoConfig();
        if(cachedConfig instanceof BatchCachedAnnoConfig){
            ((BatchCachedAnnoConfig)cachedConfig).setGlobalCacheConfig(globalCacheConfig);
        }
        boolean isCached = false;
        if (cachedConfig != null && cachedConfig.isEnabled()) {
            isCached =true;
            Class<?> returnType = method.getReturnType();
            TargetUnSerializableClassHolder.set(returnType);
        }
        try {
            return ExtendCacheHandler.invoke(context);
        }finally {
            if(isCached){
                TargetUnSerializableClassHolder.remove();
            }
        }
    }

    @Override
    public void setCacheConfigMap(ConfigMap cacheConfigMap) {
        super.setCacheConfigMap(cacheConfigMap);
        this.cacheConfigMap = cacheConfigMap;
    }

}
