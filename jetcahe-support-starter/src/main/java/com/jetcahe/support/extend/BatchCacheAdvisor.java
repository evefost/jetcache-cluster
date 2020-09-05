package com.jetcahe.support.extend;


import com.alicp.jetcache.anno.aop.CachePointcut;
import com.alicp.jetcache.anno.support.ConfigMap;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractBeanFactoryPointcutAdvisor;

public class BatchCacheAdvisor extends AbstractBeanFactoryPointcutAdvisor {

    public static final String CACHE_ADVISOR_BEAN_NAME = "jetcache2.internalCacheAdvisor";

    private ConfigMap cacheConfigMap;

    private String[] basePackages;

    @Override
    public Pointcut getPointcut() {
        CachePointcut pointcut = new BatchCachePointcut(basePackages);
        pointcut.setCacheConfigMap(cacheConfigMap);
        return pointcut;
    }

    public void setCacheConfigMap(ConfigMap cacheConfigMap) {
        this.cacheConfigMap = cacheConfigMap;
    }

    public void setBasePackages(String[] basePackages) {
        this.basePackages = basePackages;
    }
}