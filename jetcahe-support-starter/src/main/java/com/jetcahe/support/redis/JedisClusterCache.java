package com.jetcahe.support.redis;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alicp.jetcache.*;
import com.alicp.jetcache.external.AbstractExternalCache;
import com.jetcahe.support.Pair;
import com.jetcahe.support.extend.TargetUnSerializableClassHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.util.Pool;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 支持jedis 客户端集群的缓存操作
 *
 * @author <a href="mailto:areyouok@gmail.com">huangli</a>
 */
public class JedisClusterCache<K, V> extends AbstractExternalCache<K, V> {

    private static Logger logger = LoggerFactory.getLogger(JedisClusterCache.class);

    private JedisClusterCacheConfig<K, V> config;


    Function<Object, byte[]> valueEncoder;

    Function<byte[], Object> valueDecoder;

    public JedisClusterCache(JedisClusterCacheConfig<K, V> config) {
        super(config);
        this.config = config;
        this.valueEncoder = config.getValueEncoder();
        this.valueDecoder = config.getValueDecoder();
        if (config.isExpireAfterAccess()) {
            throw new CacheConfigException("expireAfterAccess is not supported");
        }
    }


    @Override
    public CacheConfig<K, V> config() {
        return config;
    }

    @Override
    public <T> T unwrap(Class<T> clazz) {
        if (Pool.class.isAssignableFrom(clazz)) {
            return null;//(T) config.getJedisPool();
        }
        throw new IllegalArgumentException(clazz.getName());
    }

    Pool<Jedis> getReadPool(String key) {
        JedisPool poolFromSlot = JedisPileLineOperator.getPoolFromSlot(key);
        return poolFromSlot;
    }


    @Override
    protected CacheGetResult<V> do_GET(K key) {
        String newKey = rebuildKey(key);
        try (Jedis jedis = getReadPool(newKey).getResource()) {
            byte[] bytes = jedis.get((newKey).getBytes());
            if (bytes != null) {

                String holderJson = new String(bytes);
                CacheValueHolder<String> holder = JSON.parseObject(holderJson, new TypeReference<CacheValueHolder<String>>() {
                });
                if (System.currentTimeMillis() >= holder.getExpireTime()) {
                    return CacheGetResult.EXPIRED_WITHOUT_MSG;
                }
                CacheValueHolder<V> targetHolder = new CacheValueHolder<>();
                targetHolder.setAccessTime(holder.getAccessTime());
                targetHolder.setExpireTime(holder.getExpireTime());
                Class<V> aClass = TargetUnSerializableClassHolder.get();
                V parse = JSON.parseObject(holder.getValue(), aClass);
                targetHolder.setValue(parse);
                return new CacheGetResult(CacheResultCode.SUCCESS, null, targetHolder);
            } else {
                return CacheGetResult.NOT_EXISTS_WITHOUT_MSG;
            }
        } catch (Exception ex) {
            logError("GET", key, ex);
            return new CacheGetResult(ex);
        }
    }

    @Override
    protected MultiGetResult<K, V> do_GET_ALL(Set<? extends K> keys) {
        Set<String> keySet = (Set<String>) keys;
        Class<V> targetType = TargetUnSerializableClassHolder.get();
        List<Pair<String, CacheGetResult<V>>> pairs = JedisPileLineOperator.batchReadResultPair(keySet, targetType, false, false, null);
        try {
            Map<K, CacheGetResult<V>> resultMap = new HashMap<>(pairs.size());
            for (int i = 0; i < pairs.size(); i++) {
                Pair<String, CacheGetResult<V>> pair = pairs.get(i);
                K key = (K) pair.getKey();
                CacheGetResult<V> value = pair.getValue();
                resultMap.put(key, value);
            }
            return new MultiGetResult<K, V>(CacheResultCode.SUCCESS, null, resultMap);
        } catch (Exception ex) {
            logError("GET_ALL", "keys(" + keys.size() + ")", ex);
            return new MultiGetResult<K, V>(ex);
        }
    }


    @Override
    protected CacheResult do_PUT(K key, V value, long expireAfterWrite, TimeUnit timeUnit) {
        String newKey = rebuildKey(key);
        try (Jedis jedis = getReadPool(newKey).getResource()) {
            CacheValueHolder<V> holder = new CacheValueHolder(value, timeUnit.toMillis(expireAfterWrite));
            String rt = jedis.psetex(newKey, timeUnit.toMillis(expireAfterWrite), JSON.toJSONString(holder));
            if ("OK".equals(rt)) {
                return CacheResult.SUCCESS_WITHOUT_MSG;
            } else {
                return new CacheResult(CacheResultCode.FAIL, rt);
            }
        } catch (Exception ex) {
            logError("PUT", key, ex);
            return new CacheResult(ex);
        }
    }

    @Override
    protected CacheResult do_PUT_ALL(Map<? extends K, ? extends V> map, long expireAfterWrite, TimeUnit timeUnit) {
        throw new RuntimeException("不支持的操作");
    }

    @Override
    protected CacheResult do_REMOVE(K key) {
        String newKey = rebuildKey(key);
        try (Jedis jedis = getReadPool(newKey).getResource()) {
            Long rt = jedis.del(newKey);
            if (rt == null) {
                return CacheResult.FAIL_WITHOUT_MSG;
            } else if (rt == 1) {
                return CacheResult.SUCCESS_WITHOUT_MSG;
            } else if (rt == 0) {
                return new CacheResult(CacheResultCode.NOT_EXISTS, null);
            } else {
                return CacheResult.FAIL_WITHOUT_MSG;
            }
        } catch (Exception ex) {
            logError("REMOVE", key, ex);
            return new CacheResult(ex);
        }
    }


    @Override
    protected CacheResult do_REMOVE_ALL(Set<? extends K> keys) {
        try {
            JedisPileLineOperator.batchDelete((Set<String>) keys);
            return CacheResult.SUCCESS_WITHOUT_MSG;
        } catch (Exception ex) {
            logError("REMOVE_ALL", "keys(" + keys.size() + ")", ex);
            return new CacheResult(ex);
        }
    }

    @Override
    protected CacheResult do_PUT_IF_ABSENT(K key, V value, long expireAfterWrite, TimeUnit timeUnit) {
        return null;
    }

    @Override
    protected boolean needLogStackTrace(Throwable e) {
        if (e instanceof JedisConnectionException) {
            return false;
        }
        return true;
    }

    private String rebuildKey(Object key) {
        return (String) key;
    }
}
