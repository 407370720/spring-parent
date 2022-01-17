package com.emily.infrastructure.mybatis;

import com.emily.infrastructure.common.constant.AopOrderInfo;
import com.emily.infrastructure.core.aop.advisor.AnnotationPointcutAdvisor;
import com.emily.infrastructure.core.aop.pointcut.AnnotationMethodPointcut;
import com.emily.infrastructure.logger.LoggerFactory;
import com.emily.infrastructure.mybatis.interceptor.MybatisMethodInterceptor;
import org.aopalliance.intercept.MethodInterceptor;
import org.apache.ibatis.annotations.Mapper;
import org.slf4j.Logger;
import org.springframework.aop.Advisor;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;

import java.text.MessageFormat;

/**
 * @Description: 控制器切点配置
 * @Author Emily
 * @Version: 1.0
 */
@Configuration
@EnableConfigurationProperties(MybatisProperties.class)
@ConditionalOnProperty(prefix = MybatisProperties.PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class MybatisAutoConfiguration implements BeanFactoryPostProcessor, InitializingBean, DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(MybatisAutoConfiguration.class);

    /**
     * Mybatis请求日志拦截切面增强类
     * checkInherited:是否验证父类或接口集成的注解，如果注解用@Inherited标注则自动集成
     * @return 组合切面增强类
     * @since 4.0.5
     */
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public Advisor mybatisLogAdvisor(MybatisProperties properties) {
        //限定类级别的切点
        Pointcut cpc = new AnnotationMatchingPointcut(Mapper.class, properties.isCheckClassInherited());
        //限定方法级别的切点
        Pointcut mpc = new AnnotationMethodPointcut(Mapper.class, properties.isCheckMethodInherited());
        //组合切面(并集)，一、ClassFilter只要有一个符合条件就返回true，二、
        Pointcut pointcut = new ComposablePointcut(cpc).union(mpc);
        //mybatis日志拦截切面
        MethodInterceptor interceptor = new MybatisMethodInterceptor();
        //切面增强类
        AnnotationPointcutAdvisor advisor = new AnnotationPointcutAdvisor(interceptor, pointcut);
        //切面优先级顺序
        advisor.setOrder(AopOrderInfo.MYBATIS);
        return advisor;
    }

    /**
     * 将指定的bean 角色标记为基础设施类型，相关提示类在 org.springframework.context.support.PostProcessorRegistrationDelegate
     *
     * @param beanFactory
     * @throws BeansException
     */
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        String beanName = MessageFormat.format("{0}-{1}", MybatisProperties.PREFIX, MybatisProperties.class.getName());
        if (beanFactory.containsBeanDefinition(beanName)) {
            BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
            beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        }
    }

    @Override
    public void destroy() throws Exception {
        logger.info("<== 【销毁--自动化配置】----Mybatis日志拦截组件【MybatisAutoConfiguration】");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        logger.info("==> 【初始化--自动化配置】----Mybatis日志拦截组件【MybatisAutoConfiguration】");
    }
}
