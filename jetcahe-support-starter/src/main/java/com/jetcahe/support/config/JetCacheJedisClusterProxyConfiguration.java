package com.jetcahe.support.config;

import com.alicp.jetcache.CacheBuilder;
import com.alicp.jetcache.anno.aop.CacheAdvisor;
import com.alicp.jetcache.anno.support.ConfigMap;
import com.alicp.jetcache.autoconfigure.AutoConfigureBeans;
import com.alicp.jetcache.autoconfigure.ConfigTree;
import com.alicp.jetcache.autoconfigure.ExternalCacheAutoInit;
import com.jetcahe.support.annotation.EnableListCache;
import com.jetcahe.support.aop.ClusterJetCacheInterceptor;
import com.jetcahe.support.aop.BatchCacheInterceptor;
import com.jetcahe.support.extend.BatchCacheAdvisor;
import com.jetcahe.support.redis.JedisClusterCacheBuilder;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Created on 2016/11/16.
 *
 * @author <a href="mailto:areyouok@gmail.com">huangli</a>
 */
@Configuration
public class JetCacheJedisClusterProxyConfiguration implements ImportAware, ApplicationContextAware {

    protected AnnotationAttributes enableMethodCache;

    private ApplicationContext applicationContext;

    @Override
    public void setImportMetadata(AnnotationMetadata importMetadata) {
        this.enableMethodCache = AnnotationAttributes.fromMap(
                importMetadata.getAnnotationAttributes(EnableListCache.class.getName(), false));
        if (this.enableMethodCache == null) {
            throw new IllegalArgumentException(
                    "@EnableMethodCache is not present on importing class " + importMetadata.getClassName());
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Bean(name = CacheAdvisor.CACHE_ADVISOR_BEAN_NAME+"batch")
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public BatchCacheAdvisor listCacheAdvisor() {
        ConfigMap configMap = new ConfigMap();

        BatchCacheInterceptor jetCacheInterceptor = new BatchCacheInterceptor();
        jetCacheInterceptor.setCacheConfigMap(configMap);
        jetCacheInterceptor.setApplicationContext(applicationContext);

        BatchCacheAdvisor advisor = new BatchCacheAdvisor();
        advisor.setAdviceBeanName(CacheAdvisor.CACHE_ADVISOR_BEAN_NAME);
        advisor.setAdvice(jetCacheInterceptor);
        advisor.setBasePackages(this.enableMethodCache.getStringArray("basePackages"));
        advisor.setCacheConfigMap(configMap);
        advisor.setOrder(Integer.MAX_VALUE);
        return advisor;
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @Primary
    public ClusterJetCacheInterceptor jetCacheInterceptor() {
        return new ClusterJetCacheInterceptor();
    }


    public static final String AUTO_INIT_BEAN_NAME = "redisClusterAutoInit";

    @Bean(name = AUTO_INIT_BEAN_NAME)
    public RedisClusterAutoInit redisAutoInit() {
        return new RedisClusterAutoInit();
    }


    public static class RedisClusterAutoInit extends ExternalCacheAutoInit {


        public RedisClusterAutoInit() {
            super("redisCluster");
        }

        @Autowired
        private AutoConfigureBeans autoConfigureBeans;

        @Override
        protected CacheBuilder initCache(ConfigTree ct, String cacheAreaWithPrefix) {
            JedisClusterCacheBuilder clusterCacheBuilder = new JedisClusterCacheBuilder();
            return clusterCacheBuilder;
        }
    }

}