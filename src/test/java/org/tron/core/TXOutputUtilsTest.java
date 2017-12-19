package org.tron.core;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TXOutputUtilsTest {
    private static final Logger logger = LoggerFactory.getLogger("test");

    @Test
    public void testNewTXOutput() {
        logger.info("test new TXOutput: {}", TXOutputUtils.newTXOutput(1,
                "12"));
    }

    @Test
    public void testToPrintString() {
        logger.info("test to print string: {}", TXOutputUtils.toPrintString
                (TXOutputUtils.newTXOutput(1, "12")));
    }
}
