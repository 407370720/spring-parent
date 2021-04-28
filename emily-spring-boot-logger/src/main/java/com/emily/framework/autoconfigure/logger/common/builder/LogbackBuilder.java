package com.emily.framework.autoconfigure.logger.common.builder;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.rolling.RollingFileAppender;
import com.emily.framework.autoconfigure.logger.common.appender.AccessLogAsyncAppender;
import com.emily.framework.autoconfigure.logger.common.appender.AccessLogConsoleAppender;
import com.emily.framework.autoconfigure.logger.common.appender.AccessLogRollingFileAppender;
import com.emily.framework.autoconfigure.logger.common.level.AccessLogLevel;
import com.emily.framework.autoconfigure.logger.common.properties.AccessLog;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Emily
 * @program: spring-parent
 * @description: 日志类
 * @create: 2020/08/04
 */
public class LogbackBuilder {
    /**
     * Logger对象容器
     */
    private Map<String, Logger> loggerCache;

    private AccessLog accessLog;

    public LogbackBuilder(AccessLog accessLog) {
        this.loggerCache = new ConcurrentHashMap<>();
        this.accessLog = accessLog;
    }

    /**
     * 获取日志输出对象
     *
     * @return
     */
    public Logger getLogger(Class<?> clazz) {
        return getLogger(clazz, null, null);
    }

    /**
     * 获取日志输出对象
     *
     * @param fileName 日志文件名|模块名称
     * @return
     */
    public Logger getLogger(Class<?> cls, String path, String fileName) {
        /**
         * 判定是否是默认文件名
         */
        boolean defaultBool = !StringUtils.hasLength(path) && !StringUtils.hasLength(fileName);
        String key;
        if (defaultBool) {
            key = cls.getName();
        } else {
            key = String.join(File.separator, path, fileName);
        }
        Logger logger = loggerCache.get(key);
        if (Objects.nonNull(logger)) {
            return logger;
        }
        synchronized (LogbackBuilder.class) {
            logger = loggerCache.get(key);
            if (Objects.nonNull(logger)) {
                return logger;
            }
            if (defaultBool) {
                logger = builder(cls);
                loggerCache.put(key, logger);
            } else {
                logger = builder(cls, path, fileName);
                loggerCache.put(fileName, logger);
            }
        }
        return logger;

    }

    /**
     * 构建Logger对象
     * 日志级别以及优先级排序: OFF > FATAL > ERROR > WARN > INFO > DEBUG > TRACE >ALL
     *
     * @return
     */
    private Logger builder(Class<?> cls) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger logger = loggerContext.getLogger(cls.getName());
        AccessLogRollingFileAppender rollingFileAppender = new AccessLogRollingFileAppender(loggerContext, accessLog);
        RollingFileAppender appenderError = rollingFileAppender.getRollingFileApender(cls.getName(), Level.ERROR.levelStr.toLowerCase(), Level.ERROR.levelStr.toLowerCase(), Level.ERROR);
        RollingFileAppender appenderWarn = rollingFileAppender.getRollingFileApender(cls.getName(), Level.WARN.levelStr.toLowerCase(), Level.WARN.levelStr.toLowerCase(), Level.WARN);
        RollingFileAppender appenderInfo = rollingFileAppender.getRollingFileApender(cls.getName(), Level.INFO.levelStr.toLowerCase(), Level.INFO.levelStr.toLowerCase(), Level.INFO);
        RollingFileAppender appenderDebug = rollingFileAppender.getRollingFileApender(cls.getName(), Level.DEBUG.levelStr.toLowerCase(), Level.DEBUG.levelStr.toLowerCase(), Level.DEBUG);
        RollingFileAppender appenderTrace = rollingFileAppender.getRollingFileApender(cls.getName(), Level.TRACE.levelStr.toLowerCase(), Level.TRACE.levelStr.toLowerCase(), Level.TRACE);
        RollingFileAppender appenderAll = rollingFileAppender.getRollingFileApender(cls.getName(), Level.ALL.levelStr.toLowerCase(), Level.ALL.levelStr.toLowerCase(), Level.ALL);
        //设置是否向上级打印信息
        logger.setAdditive(false);
        if(accessLog.isEnableAsyncAppender()){
            logger.addAppender(new AccessLogAsyncAppender(loggerContext, accessLog).getAsyncAppender(appenderError));
            logger.addAppender(new AccessLogAsyncAppender(loggerContext, accessLog).getAsyncAppender(appenderWarn));
            logger.addAppender(new AccessLogAsyncAppender(loggerContext, accessLog).getAsyncAppender(appenderInfo));
            logger.addAppender(new AccessLogAsyncAppender(loggerContext, accessLog).getAsyncAppender(appenderDebug));
            logger.addAppender(new AccessLogAsyncAppender(loggerContext, accessLog).getAsyncAppender(appenderTrace));
            logger.addAppender(new AccessLogAsyncAppender(loggerContext, accessLog).getAsyncAppender(appenderAll));
        } else {
            logger.addAppender(appenderError);
            logger.addAppender(appenderWarn);
            logger.addAppender(appenderInfo);
            logger.addAppender(appenderDebug);
            logger.addAppender(appenderTrace);
            logger.addAppender(appenderAll);
        }
        logger.addAppender(new AccessLogConsoleAppender(loggerContext, accessLog).getConsoleAppender(AccessLogLevel.getLogLevel(accessLog.getLevel())));

        logger.setLevel(AccessLogLevel.getLogLevel(accessLog.getLevel()));
        return logger;
    }

    /**
     * 构建Logger对象
     * 日志级别以及优先级排序: OFF > FATAL > ERROR > WARN > INFO > DEBUG > TRACE >ALL
     *
     * @param fileName 日志文件名|模块名称
     * @return
     */
    private Logger builder(Class<?> cls, String path, String fileName) {
        if(StringUtils.hasLength(path)){
            // 去除字符串开头斜杠/
            path = path.startsWith(File.separator) ? path.substring(1) : path;
            // 去除字符串末尾斜杠/
            path = path.endsWith(File.separator) ? path.substring(0, path.length()-1) : path;
        }
        //logger 属性name名称
        String name = String.join(".", cls.getName(), fileName);
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        AccessLogRollingFileAppender rollingFileAppender = new AccessLogRollingFileAppender(loggerContext, accessLog);
        //获取Info对应的appender对象
        RollingFileAppender rollingFileAppenderInfo = rollingFileAppender.getRollingFileApender(name, path, fileName, Level.INFO);
        Logger logger = loggerContext.getLogger(name);
        /**
         * 设置是否向上级打印信息
         */
        logger.setAdditive(false);
        //是否开启异步日志
        if(accessLog.isEnableAsyncAppender()){
            logger.addAppender(new AccessLogAsyncAppender(loggerContext, accessLog).getAsyncAppender(rollingFileAppenderInfo));
        } else {
            logger.addAppender(rollingFileAppenderInfo);
        }
        if(accessLog.isEnableModuleConsule()){
            logger.addAppender(new AccessLogConsoleAppender(loggerContext, accessLog).getConsoleAppender(AccessLogLevel.getLogLevel(accessLog.getLevel())));
        }

        logger.setLevel(AccessLogLevel.getLogLevel(accessLog.getLevel()));
        return logger;
    }


}
