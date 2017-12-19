package org.tron.utils;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

public class ByteArrayTest {
    private static final Logger logger = LoggerFactory.getLogger("test");

    @Test
    public void testToHexString() {
        logger.info("Byte: byte 16 to hex string = {}", ByteArray.toHexString
                (new byte[]{16}));
    }

    @Test
    public void testHexStringToByte() {
        logger.info("Byte: hex string 0x11 to byte = {}", ByteArray
                .fromHexString("0x11"));
        logger.info("Byte: hex string 10 to byte = {}", ByteArray
                .fromHexString("10"));
        logger.info("Byte: hex string 1 to byte = {}", ByteArray
                .fromHexString("1"));
    }

    @Test
    public void testToLong() {
        logger.info("Byte: byte 13 to long = {}", ByteArray.toLong(new
                byte[]{13}));
    }

    @Test
    public void testFromLong() {
        logger.info("Byte: long 127L to byte = {}", ByteArray.fromLong(127L));
    }

    @Test
    public void test2ToHexString() {
        byte[] bs = new byte[]{};

        logger.info("utils.ByteArray.toHexString: {}", ByteArray.toHexString
                (bs));
        logger.info("Hex.toHexString: {}", Hex.toHexString(bs));
    }
}
