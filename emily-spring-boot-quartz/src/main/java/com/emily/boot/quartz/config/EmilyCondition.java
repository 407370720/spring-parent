package com.emily.boot.quartz.config;

import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * @program: spring-parent
 * @description:
 * @create: 2020/09/30
 */
public class EmilyCondition implements ConfigurationCondition {
    @Override
    public ConfigurationPhase getConfigurationPhase() {
        return ConfigurationPhase.PARSE_CONFIGURATION;
    }

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        boolean containBeanA = context.getBeanFactory().containsBean("beanA");
        return containBeanA;
    }
}
