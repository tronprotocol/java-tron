package org.tron.core.config;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Slf4j(topic = "core.config")
@Configuration
@Import(CommonConfig.class)
public class DefaultConfig {

    @Autowired
    ApplicationContext appCtx;

    @Autowired
    CommonConfig commonConfig;

    public DefaultConfig() {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> logger.error("Uncaught exception", e));
    }
}
