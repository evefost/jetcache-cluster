package com.jetcahe.support.redis;

import com.alicp.jetcache.external.ExternalCacheBuilder;
import com.alicp.jetcache.external.ExternalCacheConfig;

/**
 * jedis 集群CacheBuilder
 * @author xieyang
 */
public class JedisClusterCacheBuilder<T extends ExternalCacheBuilder<T>> extends ExternalCacheBuilder<T>  {

    @Override
    public ExternalCacheConfig getConfig() {
        if (config == null) {
            config = new JedisClusterCacheConfig();
        }
        return (ExternalCacheConfig) config;
    }


    public JedisClusterCacheBuilder() {
        buildFunc(config -> new JedisClusterCache((JedisClusterCacheConfig) config));
    }


    @Override
    protected void beforeBuild() {
    }
}
