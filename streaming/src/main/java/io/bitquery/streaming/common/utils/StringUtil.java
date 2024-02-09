package io.bitquery.streaming.common.utils;

public class StringUtil {
    public static String zeros(int n) {
        return repeat('0', n);
    }

    public static String repeat(char value, int n) {
        return (new String(new char[n])).replace("\u0000", String.valueOf(value));
    }

    public static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }
}
