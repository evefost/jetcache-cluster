package com.jetcahe.support.redis;

import com.alibaba.fastjson.JSON;
import com.alicp.jetcache.*;
import com.alicp.jetcache.external.AbstractExternalCache;
import com.jetcahe.support.Pair;
import com.jetcahe.support.extend.TargetClassHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.params.SetParams;
import redis.clients.jedis.util.Pool;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Created on 2016/10/7.
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
        try (Jedis jedis = getReadPool((String) key).getResource()) {
            byte[] bytes = jedis.get(((String) key).getBytes());
            if (bytes != null) {
                CacheValueHolder<V> holder = new CacheValueHolder();
                String targetJson = new String(bytes);
                Class<V> aClass = TargetClassHolder.get();
                V parse = JSON.parseObject(targetJson, aClass);
                holder.setValue(parse);
                return new CacheGetResult(CacheResultCode.SUCCESS, null, holder);
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
        List<String> sKeys = new ArrayList<>(keySet);
        Class<V> targetType = null;
        List<Pair<String, V>> pairs = JedisPileLineOperator.batchReadPair(sKeys, targetType);
        try {
            Map<K, CacheGetResult<V>> resultMap = new HashMap<>(pairs.size());
            for (int i = 0; i < pairs.size(); i++) {
                Pair pair = pairs.get(i);
                K key = (K) pair.getKey();
                Object value = pair.getValue();
                if (value != null) {
                    CacheValueHolder<V> holder = (CacheValueHolder<V>) valueDecoder.apply((byte[]) value);
                    if (System.currentTimeMillis() >= holder.getExpireTime()) {
                        resultMap.put(key, CacheGetResult.EXPIRED_WITHOUT_MSG);
                    } else {
                        CacheGetResult<V> r = new CacheGetResult<V>(CacheResultCode.SUCCESS, null, holder);
                        resultMap.put(key, r);
                    }
                } else {
                    resultMap.put(key, CacheGetResult.NOT_EXISTS_WITHOUT_MSG);
                }
            }
            return new MultiGetResult<K, V>(CacheResultCode.SUCCESS, null, resultMap);
        } catch (Exception ex) {
            logError("GET_ALL", "keys(" + keys.size() + ")", ex);
            return new MultiGetResult<K, V>(ex);
        }
    }


    @Override
    protected CacheResult do_PUT(K key, V value, long expireAfterWrite, TimeUnit timeUnit) {
        try (Jedis jedis = getReadPool((String) key).getResource()) {
            CacheValueHolder<V> holder = new CacheValueHolder(value, timeUnit.toMillis(expireAfterWrite));
            String rt = jedis.psetex((String) key, timeUnit.toMillis(expireAfterWrite), JSON.toJSONString(value));
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
        return REMOVE_impl(key, buildKey(key));
    }

    private CacheResult REMOVE_impl(Object key, byte[] newKey) {
        try (Jedis jedis = getReadPool((String) key).getResource()) {
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
        throw new RuntimeException("不支持的操作");
    }

    @Override
    protected CacheResult do_PUT_IF_ABSENT(K key, V value, long expireAfterWrite, TimeUnit timeUnit) {
        try (Jedis jedis = getReadPool((String) key).getResource()) {
            CacheValueHolder<V> holder = new CacheValueHolder(value, timeUnit.toMillis(expireAfterWrite));
            byte[] newKey = buildKey(key);
            SetParams params = new SetParams();
            params.nx()
                    .px(timeUnit.toMillis(expireAfterWrite));
            String rt = jedis.set(newKey, valueEncoder.apply(holder), params);
            if ("OK".equals(rt)) {
                return CacheResult.SUCCESS_WITHOUT_MSG;
            } else if (rt == null) {
                return CacheResult.EXISTS_WITHOUT_MSG;
            } else {
                return new CacheResult(CacheResultCode.FAIL, rt);
            }
        } catch (Exception ex) {
            logError("PUT_IF_ABSENT", key, ex);
            return new CacheResult(ex);
        }
    }

    @Override
    protected boolean needLogStackTrace(Throwable e) {
        if (e instanceof JedisConnectionException) {
            return false;
        }
        return true;
    }
}
