package com.emily.framework.context.logger.impl;

import com.emily.framework.common.base.BaseLogger;
import com.emily.framework.common.logger.LoggerUtils;
import com.emily.framework.common.utils.json.JSONUtils;
import com.emily.framework.context.logger.LoggerService;
import org.springframework.scheduling.annotation.Async;

/**
 * @program: spring-parent
 * @description: RestTemplate日志拦服务类
 * @create: 2020/08/24
 */
public class LoggerServiceImpl implements LoggerService {
    /**
     * @Description 记录响应信息
     * @Version 1.0
     */
    @Override
    @Async
    public void traceResponse(BaseLogger baseLogger) {
        if (LoggerUtils.isDebug()) {
            LoggerUtils.info(LoggerServiceImpl.class, JSONUtils.toJSONPrettyString(baseLogger));
        } else {
            LoggerUtils.info(LoggerServiceImpl.class, JSONUtils.toJSONString(baseLogger));
        }
    }
}
