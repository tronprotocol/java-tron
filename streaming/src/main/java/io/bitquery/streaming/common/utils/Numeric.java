package io.bitquery.streaming.common.utils;

import java.math.BigInteger;
import java.util.Arrays;

public final class Numeric {

    private Numeric() {}

    public static String cleanHexPrefix(String input) {
        return containsHexPrefix(input) ? input.substring(2) : input;
    }

    public static boolean containsHexPrefix(String input) {
        return !StringUtil.isEmpty(input) && input.length() > 1 && input.charAt(0) == '0' && input.charAt(1) == 'x';
    }

    public static BigInteger toBigInt(byte[] value) {
        return new BigInteger(1, value);
    }

    public static String toHexStringNoPrefix(BigInteger value) {
        return value.toString(16);
    }

    public static String toHexStringWithPrefixZeroPadded(BigInteger value, int size) {
        return toHexStringZeroPadded(value, size, true);
    }

    private static String toHexStringZeroPadded(BigInteger value, int size, boolean withPrefix) {
        String result = toHexStringNoPrefix(value);
        int length = result.length();
        if (length > size) {
            throw new UnsupportedOperationException("Value " + result + "is larger then length " + size);
        } else if (value.signum() < 0) {
            throw new UnsupportedOperationException("Value cannot be negative");
        } else {
            if (length < size) {
                result = StringUtil.zeros(size - length) + result;
            }

            return withPrefix ? "0x" + result : result;
        }
    }

    public static byte[] hexStringToByteArray(String input) {
        String cleanInput = cleanHexPrefix(input);
        int len = cleanInput.length();
        if (len == 0) {
            return new byte[0];
        } else {
            byte[] data;
            byte startIdx;
            if (len % 2 != 0) {
                data = new byte[len / 2 + 1];
                data[0] = (byte)Character.digit(cleanInput.charAt(0), 16);
                startIdx = 1;
            } else {
                data = new byte[len / 2];
                startIdx = 0;
            }

            for(int i = startIdx; i < len; i += 2) {
                data[(i + 1) / 2] = (byte)((Character.digit(cleanInput.charAt(i), 16) << 4) + Character.digit(cleanInput.charAt(i + 1), 16));
            }

            return data;
        }
    }

    public static String toHexString(byte[] input, int offset, int length, boolean withPrefix) {
        StringBuilder stringBuilder = new StringBuilder();
        if (withPrefix) {
            stringBuilder.append("0x");
        }

        for(int i = offset; i < offset + length; ++i) {
            stringBuilder.append(String.format("%02x", input[i] & 255));
        }

        return stringBuilder.toString();
    }

    public static String toHexString(byte[] input) {
        return toHexString(input, 0, input.length, true);
    }

}
