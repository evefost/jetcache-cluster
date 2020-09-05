package com.jetcahe.support.redis;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisSlotBasedConnectionHandler;
import redis.clients.jedis.util.JedisClusterCRC16;

import java.util.Set;


public class JedisPipelineCluster extends JedisCluster {


    public JedisPool getPoolFromSlot(String redisKey) {
        return getConnectionHandler().getJedisPoolFromSlot(redisKey);
    }

    public JedisPipelineCluster(Set<HostAndPort> nodes, int timeout, int soTimeout, int maxAttempts, final GenericObjectPoolConfig poolConfig) {
        super(nodes, timeout, soTimeout, maxAttempts, poolConfig);
        connectionHandler = new JedisSlotAdvancedConnectionHandler(nodes, poolConfig, timeout, soTimeout);
    }

    public JedisPipelineCluster(Set<HostAndPort> nodes, int timeout, int soTimeout,
                                int maxAttempts, String password, final GenericObjectPoolConfig poolConfig) {
        super(nodes, timeout, soTimeout, maxAttempts, password, poolConfig);
        connectionHandler = new JedisSlotAdvancedConnectionHandler(nodes, poolConfig, timeout, soTimeout, password);
    }

    private JedisSlotAdvancedConnectionHandler getConnectionHandler() {
        return (JedisSlotAdvancedConnectionHandler) this.connectionHandler;
    }

    private class JedisSlotAdvancedConnectionHandler extends JedisSlotBasedConnectionHandler {

        private JedisSlotAdvancedConnectionHandler(Set<HostAndPort> nodes, GenericObjectPoolConfig poolConfig, int connectionTimeout, int soTimeout) {
            super(nodes, poolConfig, connectionTimeout, soTimeout);
        }

        private JedisSlotAdvancedConnectionHandler(Set<HostAndPort> nodes, GenericObjectPoolConfig poolConfig, int connectionTimeout, int soTimeout, String password) {
            super(nodes, poolConfig, connectionTimeout, soTimeout, password);
        }

        private JedisPool getJedisPoolFromSlot(String redisKey) {
            int slot = JedisClusterCRC16.getSlot(redisKey);
            JedisPool connectionPool = cache.getSlotPool(slot);
            if (connectionPool != null) {
                return connectionPool;
            } else {
                renewSlotCache();
                connectionPool = cache.getSlotPool(slot);
                if (connectionPool != null) {
                    return connectionPool;
                } else {
                    throw new RuntimeException("No reachable node in cluster for slot " + slot);
                }
            }
        }
    }
}
