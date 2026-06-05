package com.saas.libms.config;

import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LoggingConfig {
    public LoggingConfig() {
        LoggingSystem loggingSystem = LoggingSystem.get(LoggingConfig.class.getClassLoader());
        loggingSystem.setLogLevel("org.hibernate.SQL", LogLevel.DEBUG);
        loggingSystem.setLogLevel("org.hibernate.orm.jdbc.bind", LogLevel.TRACE);
        loggingSystem.setLogLevel("org.hibernate.type.descriptor.sql.BasicBinder", LogLevel.TRACE);
        loggingSystem.setLogLevel("com.saas.libms.scheduler", LogLevel.DEBUG);
    }
}
