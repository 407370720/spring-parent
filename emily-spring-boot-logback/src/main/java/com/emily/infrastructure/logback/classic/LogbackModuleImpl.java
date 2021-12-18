package com.emily.infrastructure.logback.classic;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.emily.infrastructure.logback.LogbackProperties;
import com.emily.infrastructure.logback.appender.LogbackAsyncAppender;
import com.emily.infrastructure.logback.appender.LogbackConsoleAppender;
import com.emily.infrastructure.logback.appender.LogbackRollingFileAppender;
import com.emily.infrastructure.logback.enumeration.LogbackType;
import com.emily.infrastructure.logback.entity.LogbackAppender;

/**
 * @program: spring-parent
 * @description: 分组记录日志
 * @author: Emily
 * @create: 2021/12/12
 */
public class LogbackModuleImpl extends AbstractLogback {

    public LogbackModuleImpl(LogbackProperties properties) {
        super(properties);
    }

    /**
     * 构建Logger对象
     * 日志级别以及优先级排序: OFF > ERROR > WARN > INFO > DEBUG > TRACE >ALL
     *
     * @param fileName 日志文件名|模块名称
     * @return
     */
    @Override
    public Logger getLogger(String appenderName, String path, String fileName) {
        Logger logger = this.getLoggerContext().getLogger(appenderName);
        // 设置是否向上级打印信息
        logger.setAdditive(false);
        // 模块输出日志级别
        Level moduleLevel = Level.toLevel(this.getProperties().getModuleLevel().levelStr);
        LogbackRollingFileAppender rollingFileAppender = new LogbackRollingFileAppender(this.getLoggerContext(), this.getProperties());
        // 获取帮助类对象
        LogbackAppender logbackAppender = LogbackAppender.toAppender(appenderName, path, fileName, LogbackType.MODULE);
        //是否开启异步日志
        if (this.getProperties().isEnableAsyncAppender()) {
            LogbackAsyncAppender logbackAsyncAppender = new LogbackAsyncAppender(this.getLoggerContext(), this.getProperties());
            if (moduleLevel.levelInt == Level.ERROR_INT) {
                logger.addAppender(logbackAsyncAppender.getAsyncAppender(rollingFileAppender.getRollingFileAppender(logbackAppender.builder(Level.ERROR))));
            }
            if (moduleLevel.levelInt == Level.WARN_INT) {
                logger.addAppender(logbackAsyncAppender.getAsyncAppender(rollingFileAppender.getRollingFileAppender(logbackAppender.builder(Level.WARN))));
            }
            if (moduleLevel.levelInt == Level.INFO_INT) {
                logger.addAppender(logbackAsyncAppender.getAsyncAppender(rollingFileAppender.getRollingFileAppender(logbackAppender.builder(Level.INFO))));
            }
            if (moduleLevel.levelInt == Level.DEBUG_INT) {
                logger.addAppender(logbackAsyncAppender.getAsyncAppender(rollingFileAppender.getRollingFileAppender(logbackAppender.builder(Level.DEBUG))));
            }
            if (moduleLevel.levelInt == Level.TRACE_INT) {
                logger.addAppender(logbackAsyncAppender.getAsyncAppender(rollingFileAppender.getRollingFileAppender(logbackAppender.builder(Level.TRACE))));
            }
        } else {
            if (moduleLevel.levelInt == Level.ERROR_INT) {
                logger.addAppender(rollingFileAppender.getRollingFileAppender(logbackAppender.builder(Level.ERROR)));
            }
            if (moduleLevel.levelInt == Level.WARN_INT) {
                logger.addAppender(rollingFileAppender.getRollingFileAppender(logbackAppender.builder(Level.WARN)));
            }
            if (moduleLevel.levelInt == Level.INFO_INT) {
                logger.addAppender(rollingFileAppender.getRollingFileAppender(logbackAppender.builder(Level.INFO)));
            }
            if (moduleLevel.levelInt == Level.DEBUG_INT) {
                logger.addAppender(rollingFileAppender.getRollingFileAppender(logbackAppender.builder(Level.DEBUG)));
            }
            if (moduleLevel.levelInt == Level.TRACE_INT) {
                logger.addAppender(rollingFileAppender.getRollingFileAppender(logbackAppender.builder(Level.TRACE)));
            }
        }
        if (this.getProperties().isEnableModuleConsole()) {
            // 添加控制台appender
            logger.addAppender(new LogbackConsoleAppender(this.getLoggerContext(), this.getProperties()).getConsoleAppender(moduleLevel));
        }
        // 设置日志级别
        logger.setLevel(moduleLevel);
        return logger;
    }
}
