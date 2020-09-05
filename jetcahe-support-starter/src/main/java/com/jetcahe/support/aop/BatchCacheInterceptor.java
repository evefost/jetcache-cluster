/**
 * Created on  13-09-18 20:33
 */
package com.jetcahe.support.aop;

import com.alicp.jetcache.anno.method.CacheInvokeConfig;
import com.alicp.jetcache.anno.method.CacheInvokeContext;
import com.alicp.jetcache.anno.support.ConfigMap;
import com.alicp.jetcache.anno.support.ConfigProvider;
import com.alicp.jetcache.anno.support.GlobalCacheConfig;
import com.jetcahe.support.extend.BatchCacheHandler;
import com.jetcahe.support.extend.BatchCachePointcut;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.Method;

/**
 * 批量缓存支持
 * @author <a href="mailto:areyouok@gmail.com">huangli</a>
 */
public class BatchCacheInterceptor implements MethodInterceptor, ApplicationContextAware {

    //private static final Logger logger = LoggerFactory.getLogger(JetCacheInterceptor.class);

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
            String key = BatchCachePointcut.getKey(method, obj.getClass());
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
        return BatchCacheHandler.invoke(context);
    }

    public void setCacheConfigMap(ConfigMap cacheConfigMap) {
        this.cacheConfigMap = cacheConfigMap;
    }

}
