package org.tron.utils;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggerTest {
    private static final Logger logger = LoggerFactory.getLogger("test");

    @Test
    public void testLogger() {
        logger.debug("test debug: {}", "success");
        logger.info("test info: {}", "success");
        logger.warn("test warn: {}", "success");
        logger.error("test error: {}", "success");
    }
}
