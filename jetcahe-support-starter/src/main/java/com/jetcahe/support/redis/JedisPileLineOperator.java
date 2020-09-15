package com.jetcahe.support.redis;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alicp.jetcache.CacheGetResult;
import com.alicp.jetcache.CacheResultCode;
import com.alicp.jetcache.CacheValueHolder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jetcahe.support.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * redis 集群管道操作工具类
 *
 * @author xieyang
 */
public class JedisPileLineOperator {

    private static final Logger logger = LoggerFactory.getLogger(JedisPileLineOperator.class);

    private static JedisPipelineCluster cluster;

    private static ObjectMapper mapper = new ObjectMapper();

    public JedisPileLineOperator(JedisPipelineCluster cluster) {
        this.cluster = cluster;
    }


    public static <P> Class<?> getTargetType(Type actualValueType) {
        Class<?> targetType = null;
        if (isArrayType(actualValueType)) {
            ParameterizedType parameterizedType = (ParameterizedType) actualValueType;
            targetType = (Class<?>) parameterizedType.getActualTypeArguments()[0];
        } else {
            targetType = (Class<?>) actualValueType;
        }
        return targetType;
    }

    /**
     * 批量查询数据,key 对应值为组数类  ，返回参值对结果(结果集无序)
     * @param keys
     * @param targetType
     * @param <V>
     * @return
     */
    public static <V> List<Pair<String/*key*/, V>> batchReadPair(Set<String> keys, Class<V> targetType) {
        return batchReadPair(keys, targetType, false, false, null);
    }

    /**
     *  还空值返回
     * 批量查询数据,key 对应值为组数类  ，返回参值对结果(结果集无序)
     * @param keys
     * @param targetType
     * @param isArray
     * @param isHash
     * @param hashFieldName
     * @param <V>
     * @return
     */
    public static <V> List<Pair<String/*key*/, CacheGetResult<V>>> batchReadResultPair(Set<String> keys, Class<V> targetType, boolean isArray, boolean isHash, String hashFieldName) {
        List<Pair<String, String>> pairs = batchReadPair(keys, String.class, isArray, isHash, hashFieldName);
        List<Pair<String/*key*/, CacheGetResult<V>>> pairHolders = new ArrayList<>(pairs.size());
        for(Pair<String, String> pair:pairs){
            String key = pair.getKey();
            String value = pair.getValue();
            if(value!= null){
                CacheValueHolder<String> holder = JSON.parseObject(value, new TypeReference<CacheValueHolder<String>>() {
                });
                CacheValueHolder<V> targetHolder = new CacheValueHolder<>();
                targetHolder.setAccessTime(holder.getAccessTime());
                targetHolder.setExpireTime(holder.getExpireTime());
                V targetValue = JSON.parseObject(holder.getValue(), targetType);
                targetHolder.setValue(targetValue);
                CacheGetResult cacheGetResult = new CacheGetResult(CacheResultCode.SUCCESS, null, targetHolder);
                Pair<String, CacheGetResult<V>> tp = new Pair<>(key,cacheGetResult);
                pairHolders.add(tp);
            }else {
                Pair<String, CacheGetResult<V>> tp = new Pair<>(key, CacheGetResult.NOT_EXISTS_WITHOUT_MSG);
                pairHolders.add(tp);
            }
        }

        return pairHolders;
    }

    /**
     * boolean cacheNullWhenLoaderReturnNull
     * 批量查询数据,key 对应值为组数类  ，返回参值对结果(结果集无序)
     * @param keys
     * @param targetType
     * @param isArray
     * @param isHash
     * @param hashFieldName
     * @param <V>
     * @return
     */
    public static <V> List<Pair<String/*key*/, V>> batchReadPair(Set<String> keys, Class<V> targetType, boolean isArray, boolean isHash, String hashFieldName) {

        Map<JedisPool, List<String>> poolKeys = getReadPoolKeys(keys);
        //缓存结果集
        List<Pair<String/*key*/, V>> cacheResultValues = new ArrayList<>(keys.size());
        poolKeys.keySet().stream().forEach((pool -> {
            readKeysOnPoolNode(pool, poolKeys.get(pool), targetType, isArray, cacheResultValues, isHash, hashFieldName);
        }));
        return cacheResultValues;
    }


    /**
     * 从某个节点中读取数据
     *
     * @param pool
     * @param targetType
     * @param isArray
     * @param keyValuePairs
     * @param <?>
     */
    private static <V> void readKeysOnPoolNode(JedisPool pool, List<String> keys, Class<V> targetType,
                                               boolean isArray, List<Pair<String, V>> keyValuePairs, boolean isHash, String hashFieldName) {
        Jedis jedis = pool.getResource();
        Pipeline pipeline = jedis.pipelined();
        keys.forEach(key -> {
                    if (isHash) {
                        if (hashFieldName != null) {
                            pipeline.hget(key, hashFieldName);
                        } else {
                            pipeline.hgetAll(key);
                        }
                    } else {
                        pipeline.get(key);
                    }
                }
        );
        List<Object> cacheValues = pipeline.syncAndReturnAll();
        jedis.close();
        for (int i = 0; i < keys.size(); i++) {
            Object value = cacheValues.get(i);
            V targetValue = null;
            String key = keys.get(i);
            try {
                targetValue = parseValue(value, targetType, isArray, key);
            } catch (Exception exception) {
                logger.error("redis 缓存数据转换异常:[{}]type[{{}]", value, targetType.getName(), exception);
            }
            Pair<String, V> pair = new Pair<>(key, targetValue);
            keyValuePairs.add(pair);
        }
    }

    public static Type findActualValueTypeArgument(Object targetBean) {
        Class targetClass = targetBean.getClass();
        Type[] genericInterfaces = targetClass.getGenericInterfaces();
        for (Type type : genericInterfaces) {
            if (type instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) type;
                Type actualTypeArgument = parameterizedType.getActualTypeArguments()[1];
                return actualTypeArgument;
            }
        }
        return null;
    }

    public static Type findFieldActualValueTypeArgument(Class targetType, String fieldName) throws
            NoSuchFieldException {
        Field declaredField = targetType.getDeclaredField(fieldName);
        Type type = declaredField.getGenericType();
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type actualTypeArgument = parameterizedType.getActualTypeArguments()[0];
            return actualTypeArgument;
        }
        return null;
    }

    public static boolean isArrayType(Type type) {
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type rawType = parameterizedType.getRawType();
            return Collection.class.isAssignableFrom((Class<?>) rawType);
        }
        if (type instanceof Class) {
            Class cType = (Class) type;
            if (cType.isAssignableFrom(List.class)) {
                return true;
            }
        }

        return false;
    }

    /**
     * @param keys
     * @param <P>  入参实体
     * @return
     */
    public static <P> Map<JedisPool, List<String>> getReadPoolKeys(Set<String> keys) {
        Map<JedisPool, List<String>> poolKeyMap = new HashMap<>();
        for (String key : keys) {
            JedisPool jedisPool = cluster.getPoolFromSlot(key);
            if (poolKeyMap.keySet().contains(jedisPool)) {
                List<String> poolKeys = poolKeyMap.get(jedisPool);
                poolKeys.add(key);
            } else {
                List<String> poolKeys = new ArrayList<>();
                poolKeys.add(key);
                poolKeyMap.put(jedisPool, poolKeys);
            }
        }
        return poolKeyMap;
    }

    /**
     * 批量写缓存
     * @param toCacheList
     * @param seconds
     * @param <P>
     */
    public static <P> void batchWritePair(List<Pair<String/*key*/, P>> toCacheList,long seconds) {
        batchWritePair(toCacheList,seconds, false, null);
    }

    public static <P> void batchWritePair(List<Pair<String, P>> toCacheList,long seconds , boolean isHash, String hashFieldName) {
        if (seconds <= 0) {
            return;
        }
        Map<JedisPool, List<Pair<String, P>>> poolKeys = getWritePoolKeys(toCacheList);
        for (JedisPool jedisPool : poolKeys.keySet()) {
            Jedis jedis = jedisPool.getResource();
            Pipeline pipeline = jedis.pipelined();
            List<Pair<String, P>> poolData = poolKeys.get(jedisPool);
            poolData.forEach(pair -> {
                P value = pair.getValue();
                CacheValueHolder<P> holder = new CacheValueHolder(value, seconds*1000);
                String jsonValue = covert2CacheValue(holder);
                if (isHash) {
                    if (hashFieldName != null) {
                        pipeline.hset(pair.getKey(), hashFieldName, jsonValue);

                    } else {
                        TypeReference<Map<String, String>> typeReference = new TypeReference<Map<String, String>>() {
                        };
                        Map<String, String> hashMap = JSON.parseObject(jsonValue, typeReference);
                        pipeline.hmset(pair.getKey(), hashMap);
                    }
                    pipeline.expire(pair.getKey(), (int)seconds);
                } else {
                    pipeline.setex(pair.getKey(), (int)seconds, jsonValue);
                }
            });
            pipeline.sync();
            jedis.close();
        }
    }

    public static <P> Map<JedisPool, List<Pair<String, P>>> getWritePoolKeys(List<Pair<String, P>> queryKeyPairList) {
        Map<JedisPool, List<Pair<String, P>>> poolKeyMap = new HashMap<>();
        for (Pair<String, P> pair : queryKeyPairList) {
            String key = pair.getKey();
            JedisPool jedisPool = cluster.getPoolFromSlot(key);
            if (poolKeyMap.keySet().contains(jedisPool)) {
                List<Pair<String, P>> poolKeys = poolKeyMap.get(jedisPool);
                poolKeys.add(pair);
            } else {
                List<Pair<String, P>> poolKeys = new ArrayList<>();
                poolKeys.add(pair);
                poolKeyMap.put(jedisPool, poolKeys);
            }
        }
        return poolKeyMap;
    }

    private static String covert2CacheValue(Object value) {
        if (value instanceof String) {
            return (String) value;
        }
        if (value instanceof Integer) {
            return value.toString();
        }
        if (value instanceof Long) {
            return value.toString();
        }
        if (value instanceof Short) {
            return value.toString();
        }
        if (value instanceof Double) {
            return value.toString();
        }
        if (value instanceof Float) {
            return value.toString();
        }
        if (value instanceof Boolean) {
            return value.toString();
        }
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            logger.error("对象转[{}]json失败:", value, e);
        }
        return null;
    }


    /**
     * 批量删除操作
     *
     * @param deleteKeys
     * @param <?>
     */
    public static void batchDelete(Set<String> deleteKeys) {
        List<Pair<String, String>> queryKeyPairList = new ArrayList<>(deleteKeys.size());
        deleteKeys.forEach((key) -> queryKeyPairList.add(new Pair<>(key, key)));
        Map<JedisPool, List<String>> poolKeys = getReadPoolKeys(deleteKeys);
        for (JedisPool jedisPool : poolKeys.keySet()) {
            Jedis jedis = jedisPool.getResource();
            Pipeline pipeline = jedis.pipelined();
            List<String> keys = poolKeys.get(jedisPool);
            keys.forEach((key) -> pipeline.del(key));
            pipeline.sync();
            jedis.close();
        }

    }


    private static <V> V parseValue(Object srcValue, Class<V> targetType, boolean isArray, String key) throws
            NoSuchFieldException {
        if (srcValue == null) {
            return null;
        }
        if (srcValue instanceof String) {
            return (V) parseStringValue((String) srcValue, targetType, isArray);
        } else if (srcValue instanceof Map) {
            Map<String, String> map = (Map) srcValue;
            if (map.size() == 0) {
                if (logger.isDebugEnabled()) {
                    logger.debug("has key of  [{}]  value is expire ", key);
                }
                return null;
            }
            //处理hash 结果集
            BeanWrapper beanWrapper = new BeanWrapperImpl(targetType);
            for (Map.Entry<String, String> entry : map.entrySet()) {
                String hkey = entry.getKey();
                PropertyDescriptor propertyDescriptor = beanWrapper.getPropertyDescriptor(hkey);
                if (propertyDescriptor == null) {
                    continue;
                }
                Class<?> propertyType = propertyDescriptor.getPropertyType();
                if (isArrayType(propertyType)) {
                    //处理列表泛参
                    Class fieldGenericType = (Class) findFieldActualValueTypeArgument(targetType, propertyDescriptor.getName());
                    Object targetValue = parseStringValue(entry.getValue(), fieldGenericType, true);
                    beanWrapper.setPropertyValue(hkey, targetValue);
                } else {
                    Object targetValue = parseStringValue(entry.getValue(), propertyDescriptor.getPropertyType(), false);
                    beanWrapper.setPropertyValue(hkey, targetValue);
                }
            }
            return (V) beanWrapper.getWrappedInstance();
        }
        logger.error("不支持的数据转换[{}][{}]", srcValue, targetType);
        return null;
    }

    private static Object parseStringValue(String srcValue, Class<?> targetType, boolean isArray) {
        if (isArray) {
            try {
                return JSON.parseArray(srcValue, targetType);
            } catch (Exception ex) {
                logger.error("解释json[{}] 数据有问题", srcValue, ex);
            }
            return null;
        }
        if (targetType.isAssignableFrom(String.class)) {
            return srcValue;
        }
        if (targetType.isAssignableFrom(Integer.class)) {

            return Integer.parseInt(srcValue);
        }
        if (targetType.isAssignableFrom(Long.class)) {
            return Long.parseLong(srcValue);
        }
        if (targetType.isAssignableFrom(Short.class)) {
            return Short.parseShort(srcValue);
        }
        if (targetType.isAssignableFrom(Double.class)) {
            return Double.parseDouble(srcValue);
        }
        if (targetType.isAssignableFrom(Float.class)) {
            return Float.parseFloat(srcValue);
        }

        if (targetType.isAssignableFrom(Boolean.class)) {
            return Boolean.parseBoolean(srcValue);
        }
        try {
            return JSON.parseObject(srcValue, targetType);
        } catch (Exception ex) {
            logger.error("解释json[{}] targetType[{}]数据有问题", srcValue, targetType.getName(), ex);
        }
        return null;
    }


    public static JedisPool getPoolFromSlot(String redisKey) {
        return cluster.getPoolFromSlot(redisKey);
    }
}
