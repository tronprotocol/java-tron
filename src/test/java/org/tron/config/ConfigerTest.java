package org.tron.config;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.crypto.ECKey;
import org.tron.utils.ByteArray;

public class ConfigerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger("Test");

    @Test
    public void testGetECKey() {
        ECKey key = Configer.getMyKey();

        LOGGER.info("address = {}", ByteArray.toHexString(key.getAddress()));
    }
}
