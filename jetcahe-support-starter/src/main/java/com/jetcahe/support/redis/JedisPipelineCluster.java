package com.jetcahe.support.redis;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.util.JedisClusterCRC16;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;


public class JedisPipelineCluster extends JedisCluster {

    private static final Logger logger = LoggerFactory.getLogger(JedisPipelineCluster.class);

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

        private Set<HostAndPort> startNodes;

        private GenericObjectPoolConfig poolConfig;

        private String password;


        private JedisSlotAdvancedConnectionHandler(Set<HostAndPort> nodes, GenericObjectPoolConfig poolConfig, int connectionTimeout, int soTimeout) {
            super(nodes, poolConfig, connectionTimeout, soTimeout);
            this.startNodes = nodes;
            this.poolConfig = poolConfig;
            new ClusterRetryInitShotsCacheTask().start();

        }

        private JedisSlotAdvancedConnectionHandler(Set<HostAndPort> nodes, GenericObjectPoolConfig poolConfig, int connectionTimeout, int soTimeout, String password) {
            super(nodes, poolConfig, connectionTimeout, soTimeout, password);
            this.startNodes = nodes;
            this.poolConfig = poolConfig;
            this.password = password;
            new ClusterRetryInitShotsCacheTask().start();
        }

        private void initializeSlotsCache(Set<HostAndPort> startNodes, GenericObjectPoolConfig poolConfig, String password) {
            for (HostAndPort hostAndPort : startNodes) {
                Jedis jedis = new Jedis(hostAndPort.getHost(), hostAndPort.getPort());
                if (password != null) {
                    jedis.auth(password);
                }
                try {
                    cache.discoverClusterNodesAndSlots(jedis);
                    break;
                } catch (JedisConnectionException e) {
                    // try next nodes
                } finally {
                    if (jedis != null) {
                        jedis.close();
                    }
                }
            }
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

        class ClusterRetryInitShotsCacheTask extends Thread {
            @Override
            public void run() {
                while (true) {
                    try {
                        TimeUnit.SECONDS.sleep(30);
                    } catch (InterruptedException e) {
                    }
                    Map<String, JedisPool> nodes = getNodes();
                    if (nodes == null || nodes.isEmpty()) {
                        logger.warn("redis cluster is initial failure retry initial again ");
                        try {
                            initializeSlotsCache(startNodes, poolConfig, password);
                        } catch (Exception e) {
                            logger.warn("redis cluster is initial failure  ",e);
                        }
                    }
                }
            }

        }
    }
}
