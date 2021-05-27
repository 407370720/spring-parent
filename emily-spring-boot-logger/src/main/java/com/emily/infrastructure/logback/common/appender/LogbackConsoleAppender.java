package com.emily.infrastructure.logback.common.appender;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.core.ConsoleAppender;
import com.emily.infrastructure.logback.common.filter.LogbackFilter;
import com.emily.infrastructure.logback.common.properties.Logback;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @author Emily
 * @description: 通过名字和级别设置Appender
 * @create: 2020/08/04
 */
public class LogbackConsoleAppender {
    /**
     * logger上下文
     */
    private LoggerContext loggerContext;
    private Logback accessLog;

    public LogbackConsoleAppender(LoggerContext loggerContext, Logback accessLog) {
        this.loggerContext = loggerContext;
        this.accessLog = accessLog;
    }

    /**
     * 控制台打印appender
     *
     * @param level
     * @return
     */
    public ConsoleAppender getConsoleAppender(Level level) {

        //这里是可以用来设置appender的，在xml配置文件里面，是这种形式：
        ConsoleAppender appender = new ConsoleAppender();

        //这里设置级别过滤器
        LogbackFilter levelController = new LogbackFilter();
        ThresholdFilter levelFilter = levelController.getThresholdLevelFilter(level);
        levelFilter.start();

        //设置上下文，每个logger都关联到logger上下文，默认上下文名称为default。
        // 但可以使用<contextName>设置成其他名字，用于区分不同应用程序的记录。一旦设置，不能修改。
        appender.setContext(loggerContext);
        //appender的name属性
        appender.setName("CONSULE");

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        //设置上下文，每个logger都关联到logger上下文，默认上下文名称为default。
        // 但可以使用<contextName>设置成其他名字，用于区分不同应用程序的记录。一旦设置，不能修改。
        encoder.setContext(loggerContext);
        //设置格式
        encoder.setPattern(accessLog.getCommonPattern());
        //设置编码格式
        encoder.setCharset(Charset.forName(StandardCharsets.UTF_8.name()));
        encoder.start();

        //添加过滤器
        appender.addFilter(levelFilter);
        //设置编码
        appender.setEncoder(encoder);
        appender.start();
        return appender;

    }
}
