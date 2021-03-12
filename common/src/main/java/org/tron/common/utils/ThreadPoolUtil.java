package org.tron.common.utils;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPoolUtil {

    private static final ThreadFactory threadFactory = new ThreadFactory() {

        private final AtomicInteger atomicCount = new AtomicInteger(1);

        public Thread newThread(Runnable runnable) {
            return new Thread(runnable, "AsyncTask #" + atomicCount.getAndIncrement());
        }
    };

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();

    private static final int CORE_POOL_SIZE = Math.max(2, Math.max(CPU_COUNT - 1, 4));

    public static final int MAX_POOL_SIZE = CPU_COUNT * 2 + 1;

    private static final int KEEP_ALIVE_SECONDS = 30;

    private static final BlockingQueue<Runnable> sPoolWorkQueue =
            new LinkedBlockingQueue<>();

    public static ThreadPoolExecutor THREAD_POOL_EXECUTOR;

    public static ThreadPoolExecutor getThreadPoolExecutor() {
        if (null == THREAD_POOL_EXECUTOR) {
            THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(
                    CORE_POOL_SIZE,
                    MAX_POOL_SIZE,
                    KEEP_ALIVE_SECONDS,
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(1000)
            );
        }
        return THREAD_POOL_EXECUTOR;
    }

    public static ThreadPoolExecutor getThreadPoolExecutor(int blockCapacity) {
        if (null == THREAD_POOL_EXECUTOR) {
            THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(
                    CORE_POOL_SIZE,
                    MAX_POOL_SIZE,
                    KEEP_ALIVE_SECONDS,
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(blockCapacity)
            );
        }
        return THREAD_POOL_EXECUTOR;
    }

    private static ThreadPoolExecutor getThreadPoolExecutor(
            int corePoolSize,
            int maxPoolSize,
            int keepAliveSeconds,
            TimeUnit unit,
            int blockCapacity
    ) {
        if (null == THREAD_POOL_EXECUTOR) {
            THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(
                    corePoolSize,
                    maxPoolSize,
                    keepAliveSeconds,
                    unit,
                    new LinkedBlockingQueue<>(blockCapacity)
            );
        }
        return THREAD_POOL_EXECUTOR;
    }

    public static void closeTask() {
        THREAD_POOL_EXECUTOR.shutdown();
    }

    public static int getCpuCount() {
        return CPU_COUNT;
    }

    public static int getCorePoolSize() {
        return CORE_POOL_SIZE;
    }

    public static int getMaxPoolSize() {
        return MAX_POOL_SIZE;
    }
}
