package com.emily.infrastructure.redis;

import com.emily.infrastructure.redis.utils.RedisUtils;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.Maps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @program: spring-parent
 * @description: Redis多数据源配置
 * @author: Emily
 * @create: 2021/07/13
 */
@Configuration
@EnableConfigurationProperties(RedisDataSourceProperties.class)
public class RedisDataSourceAutoConfiguration {

    private DefaultListableBeanFactory defaultListableBeanFactory;
    private RedisDataSourceProperties redisDataSourceProperties;

    public RedisDataSourceAutoConfiguration(DefaultListableBeanFactory defaultListableBeanFactory, RedisDataSourceProperties redisDataSourceProperties) {
        this.defaultListableBeanFactory = defaultListableBeanFactory;
        this.redisDataSourceProperties = redisDataSourceProperties;
    }

    @PostConstruct
    public void stringRedisTemplate() {
        Map<String, RedisSentinelConfiguration> configs = createConfiguration(redisDataSourceProperties);
        configs.forEach((key, config) -> {
            LettuceConnectionFactory factory = new LettuceConnectionFactory(configs.get(key));
            factory.afterPropertiesSet();
            StringRedisTemplate template = new StringRedisTemplate(factory);
            template.setKeySerializer(stringSerializer());
            template.setValueSerializer(jacksonSerializer());
            template.setHashKeySerializer(stringSerializer());
            template.setHashValueSerializer(jacksonSerializer());
            defaultListableBeanFactory.registerSingleton(RedisUtils.getStringRedisTemplateBeanName(key), template);

            RedisTemplate redisTemplate = new RedisTemplate();
            redisTemplate.setConnectionFactory(factory);
            redisTemplate.setKeySerializer(stringSerializer());
            redisTemplate.setValueSerializer(jacksonSerializer());
            redisTemplate.setHashKeySerializer(stringSerializer());
            redisTemplate.setHashValueSerializer(jacksonSerializer());
            defaultListableBeanFactory.registerSingleton(RedisUtils.getRedisTemplateBeanName(key), template);
        });
    }

    /**
     * 配置第一个数据源的——交易中台
     */
    public Map<String, RedisSentinelConfiguration> createConfiguration(RedisDataSourceProperties redisDataSourceProperties) {
        Map<String, RedisSentinelConfiguration> configs = Maps.newHashMap();
        Map<String, RedisProperties> redisPropertiesMap = redisDataSourceProperties.getConfig();
        redisPropertiesMap.forEach((key, properties) -> {
            RedisSentinelConfiguration result = new RedisSentinelConfiguration();
            result.setDatabase(properties.getDatabase());
            result.setMaster(properties.getSentinel().getMaster());
            result.setPassword(properties.getPassword());
            result.setSentinelPassword(properties.getSentinel().getPassword());
            result.setSentinels(getRedisNodes(properties.getSentinel().getNodes()));
            configs.put(key, result);
        });
        return configs;
    }

    private Iterable<RedisNode> getRedisNodes(List<String> nodes) {

        Set<RedisNode> setRedisNode = new HashSet<>();
        nodes.forEach(node -> {
            String[] nodeInfo = node.split(":");
            setRedisNode.add(RedisNode.newRedisNode().listeningAt(nodeInfo[0], Integer.valueOf(nodeInfo[1])).build());
        });
        return setRedisNode;
    }

    /**
     * 初始化string序列化对象
     */
    public StringRedisSerializer stringSerializer() {
        return new StringRedisSerializer();
    }

    /**
     * 初始化jackson序列化对象
     */
    public Jackson2JsonRedisSerializer<Object> jacksonSerializer() {
        //使用Jackson2JsonRedisSerializer来序列化和反序列化redis的value值
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper objectMapper = new ObjectMapper();

        //指定要序列化的域、field、get和set，以及修饰符范围，ANY是都有包括private和public
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        //objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        //第一个参数用于验证要反序列化的实际子类型是否对验证器使用的任何条件有效，在反序列化时必须设置，否则报异常
        //第二个参数设置序列化的类型必须为非final类型，只有少数的类型（String、Boolean、Integer、Double）可以从JSON中正确推断
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        // 解决jackson2无法反序列化LocalDateTime的问题
        objectMapper.registerModule(new JavaTimeModule());

        jackson2JsonRedisSerializer.setObjectMapper(objectMapper);

        return jackson2JsonRedisSerializer;
    }
}
