package org.tron.utils;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger("Test");

    @Test
    public void testLogger() {
        LOGGER.debug("test debug: {}", "success");
        LOGGER.info("test info: {}", "success");
        LOGGER.warn("test warn: {}", "success");
        LOGGER.error("test error: {}", "success");
    }
}
