package com.jetcahe.support.redis;

import com.alicp.jetcache.external.ExternalCacheConfig;

/**
 * Created on 2016/10/7.
 *
 * @author <a href="mailto:areyouok@gmail.com">huangli</a>
 */
public class JedisClusterCacheConfig<K, V> extends ExternalCacheConfig<K, V> {

    private Class<V> targetClassType;

    public Class<V> getTargetClassType() {
        return targetClassType;
    }

    public void setTargetClassType(Class<V> targetClassType) {
        this.targetClassType = targetClassType;
    }
}
