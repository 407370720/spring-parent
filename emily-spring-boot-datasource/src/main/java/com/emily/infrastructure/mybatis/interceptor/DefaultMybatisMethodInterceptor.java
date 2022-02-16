package com.emily.infrastructure.mybatis.interceptor;

import com.emily.infrastructure.common.constant.AopOrderInfo;
import com.emily.infrastructure.common.enums.DateFormat;
import com.emily.infrastructure.common.exception.PrintExceptionInfo;
import com.emily.infrastructure.common.utils.json.JSONUtils;
import com.emily.infrastructure.core.entity.BaseLogger;
import com.emily.infrastructure.core.helper.ThreadPoolHelper;
import com.emily.infrastructure.core.trace.context.TraceContextHolder;
import com.emily.infrastructure.logger.LoggerFactory;
import com.google.common.collect.Maps;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;

import java.lang.reflect.Parameter;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * @Description: 在接口到达具体的目标即控制器方法之前获取方法的调用权限，可以在接口方法之前或者之后做Advice(增强)处理
 * @Author Emily
 * @Version: 1.0
 */
public class DefaultMybatisMethodInterceptor implements MybatisCustomizer {

    private static final Logger logger = LoggerFactory.getLogger(DefaultMybatisMethodInterceptor.class);

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        long start = System.currentTimeMillis();
        Object response = null;
        try {
            response = invocation.proceed();
            return response;
        } catch (Throwable ex) {
            response = PrintExceptionInfo.printErrorInfo(ex);
            throw ex;
        } finally {
            BaseLogger baseLogger = new BaseLogger();
            baseLogger.setSystemNumber(TraceContextHolder.get().getSystemNumber());
            baseLogger.setTraceId(TraceContextHolder.get().getTraceId());
            baseLogger.setClientIp(TraceContextHolder.get().getClientIp());
            baseLogger.setServerIp(TraceContextHolder.get().getServerIp());
            baseLogger.setRequestParams(getInParam(invocation));
            baseLogger.setBody(response);
            baseLogger.setUrl(MessageFormat.format("{0}.{1}", invocation.getMethod().getDeclaringClass().getCanonicalName(), invocation.getMethod().getName()));
            baseLogger.setTriggerTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern(DateFormat.YYYY_MM_DDTHH_MM_SS_COLON_SSS.getFormat())));
            baseLogger.setTime(System.currentTimeMillis() - start);
            ThreadPoolHelper.threadPoolTaskExecutor().submit(() -> {
                logger.info(JSONUtils.toJSONString(baseLogger));
            });
            //非servlet上下文移除数据
            TraceContextHolder.remove(TraceContextHolder.get().isServletContext());
        }
    }

    private Map<String, Object> getInParam(MethodInvocation invocation) {
        Map<String, Object> paramMap = Maps.newHashMap();
        try {
            Parameter[] parameters = invocation.getMethod().getParameters();
            Object[] obj = invocation.getArguments();
            for (int i = 0; i < parameters.length; i++) {
                String name = parameters[i].getName();
                Object value = obj[i];
                paramMap.put(name, value);
            }
        } catch (Exception e) {
            logger.error(PrintExceptionInfo.printErrorInfo(e));
        }
        return paramMap;
    }

    @Override
    public int getOrder() {
        return AopOrderInfo.MYBATIS_INTERCEPTOR;
    }
}
