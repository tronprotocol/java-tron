package org.tron.utils;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.tron.utils.TypeConversion.bytesToLong;
import static org.tron.utils.TypeConversion.longToBytes;
import static org.tron.utils.TypeConversion.bytesToHexString;
import static org.tron.utils.TypeConversion.hexStringToBytes;


public class TypeConversionTest {
    private static final Logger logger = LoggerFactory.getLogger("test");

    @Test
    public void testLongToBytes() {
        byte[] result = longToBytes(123L);
        logger.info("long 123 to bytes is: {}", result);
    }

    @Test
    public void testBytesToLong() {
        long result = bytesToLong(new byte[]{0, 0, 0, 0, 0, 0, 0, 124});
        logger.info("bytes 124 to long is: {}", result);
    }

    @Test
    public void testBytesToHexString() {
        String result = bytesToHexString(new byte[]{0, 0, 0, 0, 0, 0, 0, 125});
        logger.info("bytes 125 to hex string is: {}", result);
    }

    @Test
    public void testHexStringToBytes() {
        byte[] result = hexStringToBytes("7f");
        logger.info("hex string 7f to bytes is: {}", result);
    }
}
