package com.emily.infrastructure.datasource;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceAutoConfigure;
import com.alibaba.druid.spring.boot.autoconfigure.properties.DruidStatProperties;
import com.alibaba.druid.spring.boot.autoconfigure.stat.DruidSpringAopConfiguration;
import com.emily.infrastructure.common.constant.AopOrderInfo;
import com.emily.infrastructure.datasource.context.DynamicMultipleDataSources;
import com.emily.infrastructure.datasource.exception.DataSourceNotFoundException;
import com.emily.infrastructure.datasource.interceptor.DataSourceCustomizer;
import com.emily.infrastructure.datasource.interceptor.DataSourceMethodInterceptor;
import com.emily.infrastructure.logger.LoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.slf4j.Logger;
import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.core.annotation.Order;

import javax.sql.DataSource;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @Description: 控制器切点配置
 * @Author Emily
 * @Version: 1.0
 */
@Configuration
@AutoConfigureBefore(DruidDataSourceAutoConfigure.class)
@EnableConfigurationProperties(DataSourceProperties.class)
@ConditionalOnProperty(prefix = DataSourceProperties.PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class DataSourceAutoConfiguration implements BeanFactoryPostProcessor, InitializingBean, DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(DataSourceAutoConfiguration.class);

    /**
     * 在多个表达式之间使用  || , or 表示  或 ，使用  && , and 表示  与 ， ！ 表示 非
     *
     * @target()可以标注在目标类对象上，但是不可以标注在接口上
     * @within()可以标注在目标类对象上、也可以标注在接口上
     * @annotation()可以标注在目标方法上
     */
    private static final String DEFAULT_POINT_CUT = StringUtils.join("@within(com.emily.infrastructure.datasource.annotation.TargetDataSource) ",
            "or @annotation(com.emily.infrastructure.datasource.annotation.TargetDataSource)");

    /**
     * 方法切入点函数：execution(<修饰符模式>? <返回类型模式> <方法名模式>(<参数模式>) <异常模式>?)  除了返回类型模式、方法名模式和参数模式外，其它项都是可选的
     * 切入点表达式：
     * 第一个*号：表示返回类型，*号表示所有的类型
     * 包名：表示需要拦截的包名，后面的两个句点表示当前包和当前包下的所有子包
     * 第二个*号：表示类名，*号表示所有的类名
     * 第三个*号：表示方法名，*号表示所有的方法，后面的括弧表示方法里面的参数，两个句点表示任意参数
     */
    @Bean
    @ConditionalOnBean(DataSourceCustomizer.class)
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public Advisor dataSourcePointCutAdvice(ObjectProvider<DataSourceCustomizer> dataSourceCustomizers) {
        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        //获取切面表达式
        pointcut.setExpression(DEFAULT_POINT_CUT);
        // 配置增强类advisor
        DefaultPointcutAdvisor advisor = new DefaultPointcutAdvisor();
        advisor.setPointcut(pointcut);
        advisor.setAdvice(dataSourceCustomizers.orderedStream().findFirst().get());
        advisor.setOrder(AopOrderInfo.DATASOURCE);
        return advisor;
    }

    @Bean
    @Order(AopOrderInfo.DATASOURCE_INTERCEPTOR)
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public DataSourceMethodInterceptor dataSourceMethodInterceptor(DataSourceProperties dataSourceProperties) {
        return new DataSourceMethodInterceptor(dataSourceProperties);
    }

    /**
     * 从配置文件中获取多数据源配置信息
     * {@link DataSourceTransactionManagerAutoConfiguration}
     * {@link MybatisAutoConfiguration}
     */
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public DataSource dynamicMultipleDataSources(DataSourceProperties dataSourceProperties) {
        Map<String, DruidDataSource> configs = dataSourceProperties.getConfig();
        if (Objects.isNull(dataSourceProperties.getDefaultConfig())) {
            throw new DataSourceNotFoundException("默认数据库必须配置");
        }
        if (configs.isEmpty()) {
            throw new DataSourceNotFoundException("数据库配置不存在");
        }
        Map<Object, Object> targetDataSources = new HashMap<>(configs.size());
        configs.keySet().forEach(key -> targetDataSources.put(key, configs.get(key)));
        return DynamicMultipleDataSources.build(dataSourceProperties.getDefaultDataSource(), targetDataSources);
    }

    /**
     * 将指定的bean 角色标记为基础设施类型，相关提示类在 org.springframework.context.support.PostProcessorRegistrationDelegate
     * @param beanFactory
     * @throws BeansException
     */
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        String beanName = MessageFormat.format("{0}-{1}", DataSourceProperties.PREFIX, DataSourceProperties.class.getName());
        if (beanFactory.containsBeanDefinition(beanName)) {
            BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
            beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        }

        beanName = DruidSpringAopConfiguration.class.getName();
        if (beanFactory.containsBeanDefinition(beanName)) {
            BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
            beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        }

        beanName = MessageFormat.format("{0}-{1}", "spring.datasource.druid", DruidStatProperties.class.getName());
        if (beanFactory.containsBeanDefinition(beanName)) {
            BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
            beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        }

    }

    @Override
    public void destroy() {
        logger.info("<== 【销毁--自动化配置】----数据库多数据源组件【DataSourceAutoConfiguration】");
    }

    @Override
    public void afterPropertiesSet() {
        logger.info("==> 【初始化--自动化配置】----数据库多数据源组件【DataSourceAutoConfiguration】");
    }
}
