package org.tron.core;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TXInputUtilsTest {
    private static final Logger logger = LoggerFactory.getLogger("test");

    @Test
    public void testNewTXInput() {
        logger.info("test new TXInput: {}", TXInputUtils.newTXInput(new
                byte[]{}, 1, new byte[]{}, new byte[]{}));
    }

    @Test
    public void testToPrintString() {
        logger.info("test to print string: {}", TXInputUtils.toPrintString
                (TXInputUtils.newTXInput(new byte[]{}, 1, new byte[]{}, new
                        byte[]{})));
    }
}
