package com.emily.infrastructure.datasource.redis.factory;

import com.emily.infrastructure.core.helper.ThreadPoolHelper;
import com.emily.infrastructure.datasource.redis.entity.ConnectionInfo;
import com.emily.infrastructure.datasource.redis.thread.RedisDbRunnable;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.lettuce.core.resource.ClientResources;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.data.redis.connection.RedisConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * @program: spring-parent
 * @description: Redis连接工厂类
 * @author: Emily
 * @create: 2021/07/11
 */
public class RedisDbConnectionFactory {

    private RedisProperties properties;
    private ClientResources clientResources;

    public RedisDbConnectionFactory() {
    }

    /**
     * 创建连接工厂类
     *
     * @param redisConfiguration 连接配置
     * @return
     */
    public RedisConnectionFactory getRedisConnectionFactory(ObjectProvider<LettuceClientConfigurationBuilderCustomizer> builderCustomizers, RedisConfiguration redisConfiguration) {

        Assert.notNull(clientResources, "ClientResources must not be null");
        Assert.notNull(clientResources, "RedisDbProperties must not be null");

        LettuceClientConfiguration lettuceClientConfiguration = getLettuceClientConfiguration(builderCustomizers, clientResources, getProperties().getLettuce().getPool());
        return createLettuceConnectionFactory(lettuceClientConfiguration, redisConfiguration);
    }

    private LettuceClientConfiguration getLettuceClientConfiguration(
            ObjectProvider<LettuceClientConfigurationBuilderCustomizer> builderCustomizers,
            ClientResources clientResources, RedisProperties.Pool pool) {
        LettuceClientConfiguration.LettuceClientConfigurationBuilder builder = createBuilder(pool);
        applyProperties(builder);
        if (StringUtils.hasText(getProperties().getUrl())) {
            customizeConfigurationFromUrl(builder);
        }
        builder.clientOptions(createClientOptions());
        builder.clientResources(clientResources);
        builderCustomizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
        return builder.build();
    }

    private LettuceConnectionFactory createLettuceConnectionFactory(LettuceClientConfiguration clientConfig, RedisConfiguration redisConfiguration) {
        LettuceConnectionFactory factory = new LettuceConnectionFactory(redisConfiguration, clientConfig);
        // 创建Redis连接
        factory.afterPropertiesSet();
        // 将RedisConnectionFactory丢入线程池做监控
        ThreadPoolHelper.threadPoolTaskExecutor().execute(new RedisDbRunnable(factory));
        return factory;
    }

    private LettuceClientConfiguration.LettuceClientConfigurationBuilder createBuilder(RedisProperties.Pool pool) {
        if (pool == null) {
            return LettuceClientConfiguration.builder();
        }
        return new RedisPoolBuilderFactory().createBuilder(pool);
    }

    private LettuceClientConfiguration.LettuceClientConfigurationBuilder applyProperties(
            LettuceClientConfiguration.LettuceClientConfigurationBuilder builder) {
        if (getProperties().isSsl()) {
            builder.useSsl();
        }
        // Redis客户端读取超时时间
        if (getProperties().getTimeout() != null) {
            builder.commandTimeout(getProperties().getTimeout());
        }
        // 关闭连接池超时时间
        if (getProperties().getLettuce() != null) {
            RedisProperties.Lettuce lettuce = getProperties().getLettuce();
            if (lettuce.getShutdownTimeout() != null && !lettuce.getShutdownTimeout().isZero()) {
                builder.shutdownTimeout(getProperties().getLettuce().getShutdownTimeout());
            }
        }
        if (StringUtils.hasText(getProperties().getClientName())) {
            builder.clientName(getProperties().getClientName());
        }
        return builder;
    }

    private void customizeConfigurationFromUrl(LettuceClientConfiguration.LettuceClientConfigurationBuilder builder) {
        ConnectionInfo connectionInfo = ConnectionInfo.parseUrl(getProperties().getUrl());
        if (connectionInfo.isUseSsl()) {
            builder.useSsl();
        }
    }

    /**
     * ClientOptions 用于控制客户端行为的客户端选项
     *
     * @return
     */
    private ClientOptions createClientOptions() {
        ClientOptions.Builder builder = initializeClientOptionsBuilder();
        Duration connectTimeout = getProperties().getConnectTimeout();
        if (connectTimeout != null) {
            builder.socketOptions(SocketOptions.builder().connectTimeout(connectTimeout).build());
        }
        return builder.timeoutOptions(TimeoutOptions.enabled()).build();
    }

    /**
     * 拓扑刷新
     * 集群节点增加了关于节点刷新的相关属性配置
     *
     * @return
     */
    private ClientOptions.Builder initializeClientOptionsBuilder() {
        if (getProperties().getCluster() != null) {
            ClusterClientOptions.Builder builder = ClusterClientOptions.builder();
            RedisProperties.Lettuce.Cluster.Refresh refreshProperties = getProperties().getLettuce().getCluster().getRefresh();
            io.lettuce.core.cluster.ClusterTopologyRefreshOptions.Builder refreshBuilder = ClusterTopologyRefreshOptions.builder().dynamicRefreshSources(refreshProperties.isDynamicRefreshSources());
            if (refreshProperties.getPeriod() != null) {
                refreshBuilder.enablePeriodicRefresh(refreshProperties.getPeriod());
            }

            if (refreshProperties.isAdaptive()) {
                refreshBuilder.enableAllAdaptiveRefreshTriggers();
            }

            return builder.topologyRefreshOptions(refreshBuilder.build());
        } else {
            return ClientOptions.builder();
        }
    }

    public RedisProperties getProperties() {
        return properties;
    }

    public void setProperties(RedisProperties properties) {
        this.properties = properties;
    }

    public ClientResources getClientResources() {
        return clientResources;
    }

    public void setClientResources(ClientResources clientResources) {
        this.clientResources = clientResources;
    }
}
