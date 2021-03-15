package org.tron.common.utils;

public class ThreadPoolUtil {

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();

    public static final int MAX_POOL_SIZE = CPU_COUNT * 2 + 1;

    public static int getMaxPoolSize() {
        return MAX_POOL_SIZE;
    }
}
