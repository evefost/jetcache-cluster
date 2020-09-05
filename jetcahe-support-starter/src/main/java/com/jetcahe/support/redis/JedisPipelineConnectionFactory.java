package com.jetcahe.support.redis;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import redis.clients.jedis.HostAndPort;

import java.util.HashSet;
import java.util.Set;

public class JedisPipelineConnectionFactory extends JedisConnectionFactory implements BeanFactoryAware {


    private JedisClientConfiguration clientConfiguration;

    private ConfigurableListableBeanFactory beanFactory;

    public JedisPipelineConnectionFactory(RedisClusterConfiguration clusterConfig, JedisClientConfiguration clientConfig) {
        super(clusterConfig, clientConfig);
        this.clientConfiguration = clientConfig;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
    }


    private int getConnectTimeout() {
        return Math.toIntExact(clientConfiguration.getConnectTimeout().toMillis());
    }

    private int getReadTimeout() {
        return Math.toIntExact(clientConfiguration.getReadTimeout().toMillis());
    }


    @Override
    protected JedisPipelineCluster createCluster(RedisClusterConfiguration clusterConfig, GenericObjectPoolConfig poolConfig) {
        Assert.notNull(clusterConfig, "Cluster configuration must not be null!");
        Set<HostAndPort> hostAndPort = new HashSet<>();
        for (RedisNode node : clusterConfig.getClusterNodes()) {
            hostAndPort.add(new HostAndPort(node.getHost(), node.getPort()));
        }
        int redirects = clusterConfig.getMaxRedirects() != null ? clusterConfig.getMaxRedirects() : 5;
        String password = getPassword();
        JedisPipelineCluster cluster = null;
        if (StringUtils.isEmpty(password)) {
            cluster = new JedisPipelineCluster(hostAndPort, getConnectTimeout(), getReadTimeout(), redirects, poolConfig);
        } else {
            cluster = new JedisPipelineCluster(hostAndPort, getConnectTimeout(), getReadTimeout(), redirects, password, poolConfig);
        }
        registerCluster(cluster);
        return cluster;

    }

    private void registerCluster(JedisPipelineCluster cluster) {
        //注入管道操作工具
        beanFactory.registerSingleton("redisCluster", cluster);
        JedisPileLineOperator pileLineOperator = new JedisPileLineOperator(cluster);
        beanFactory.registerSingleton("redisPileLineOperator", pileLineOperator);
    }

}
