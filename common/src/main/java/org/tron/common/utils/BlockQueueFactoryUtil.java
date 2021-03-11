package org.tron.common.utils;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class BlockQueueFactoryUtil<T> {
    private static BlockingQueue queue;

    public static synchronized BlockingQueue getInstance() {
        if (null == queue) {
            queue = getInstance(20000);
        }
        return queue;
    }

    public static synchronized BlockingQueue getInstance(int capacity) {
        if (null == queue) {
            queue = new LinkedBlockingDeque<>(capacity);
        }
        return queue;
    }
}
