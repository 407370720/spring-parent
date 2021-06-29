package com.emily.infrastructure.logback.factory;

import ch.qos.logback.classic.Logger;
import com.emily.infrastructure.logback.builder.LogbackBuilder;
import org.slf4j.LoggerFactory;

/**
 * @author Emily
 * @Description: 日志工具类 日志级别总共有TARCE < DEBUG < INFO < WARN < ERROR < FATAL，且级别是逐渐提供，
 * 如果日志级别设置为INFO，则意味TRACE和DEBUG级别的日志都看不到。
 * @Version: 1.0
 */
public class LogbackFactory {
    /**
     * 当前开发模式
     */
    private static boolean debug;
    /**
     * 开启logback日志组件
     */
    private static LogbackBuilder builder;

    public static <T> Logger getLogger(Class cls, String path, String fileName) {
        return builder.getLogger(cls, path, fileName);
    }

    public static <T> void info(Class<T> clazz, String msg) {
        LoggerFactory.getLogger(clazz).info(msg);
    }

    public static <T> void warn(Class<T> clazz, String msg) {
        LoggerFactory.getLogger(clazz).warn(msg);
    }

    public static <T> void debug(Class<T> clazz, String msg) {
        LoggerFactory.getLogger(clazz).debug(msg);
    }

    public static <T> void error(Class<T> clazz, String msg) {
        LoggerFactory.getLogger(clazz).error(msg);
    }

    public static <T> void trace(Class<T> clazz, String msg) {
        LoggerFactory.getLogger(clazz).trace(msg);
    }

    public static <T> void module(Class<T> clazz, String path, String fileName, String msg) {
        builder.getLogger(clazz, path, fileName, true).error(msg);
    }

    public static boolean isDebug() {
        return debug;
    }

    public static void setDebug(boolean isDebug) {
        debug = isDebug;
    }

    public static void setBuilder(LogbackBuilder builder) {
        LogbackFactory.builder = builder;
    }
}
