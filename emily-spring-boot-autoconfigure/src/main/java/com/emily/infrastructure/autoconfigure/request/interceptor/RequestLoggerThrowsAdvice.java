package com.emily.infrastructure.autoconfigure.request.interceptor;

import com.emily.infrastructure.autoconfigure.request.helper.RequestHelper;
import com.emily.infrastructure.common.base.BaseLogger;
import com.emily.infrastructure.common.enums.DateFormatEnum;
import com.emily.infrastructure.common.exception.BusinessException;
import com.emily.infrastructure.common.exception.PrintExceptionInfo;
import com.emily.infrastructure.common.utils.RequestUtils;
import com.emily.infrastructure.context.logger.LoggerService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.aop.ThrowsAdvice;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author Emily
 * @Description: 在接口到达具体的目标即控制器方法之前获取方法的调用权限，可以在接口方法之前或者之后做Advice(增强)处理
 * @Version: 1.0
 */
public class RequestLoggerThrowsAdvice implements ThrowsAdvice {

    private LoggerService loggerService;

    public RequestLoggerThrowsAdvice(LoggerService loggerService) {
        this.loggerService = loggerService;
    }

    public void afterThrowing(Method method, Object[] args, Object target, Exception e) {
        HttpServletRequest request = RequestUtils.getRequest();
        //封装异步日志信息
        BaseLogger baseLogger = new BaseLogger();
        //事务唯一编号
        baseLogger.setTraceId(RequestUtils.getTraceId());
        //时间
        baseLogger.setTriggerTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern(DateFormatEnum.YYYY_MM_DD_HH_MM_SS_SSS.getFormat())));
        //控制器Class
        baseLogger.setClazz(target.getClass());
        //控制器方法名
        baseLogger.setMethod(method.getName());
        //请求url
        baseLogger.setRequestUrl(request.getRequestURL().toString());
        //请求方法
        baseLogger.setMethod(request.getMethod());
        //请求参数
        baseLogger.setRequestParams(RequestHelper.getParameterMap(request));
        if (e instanceof BusinessException) {
            BusinessException exception = (BusinessException) e;
            baseLogger.setResponseBody(StringUtils.join(e, " 【statusCode】", exception.getStatus(), ", 【errorMessage】", exception.getMessage()));
        } else {
            baseLogger.setResponseBody(PrintExceptionInfo.printErrorInfo(e));
        }
        //记录异常日志
        loggerService.traceResponse(baseLogger);

    }


}
