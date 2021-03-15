package org.tron.common.utils;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class BlockQueueFactoryUtil {
    private static final BlockingQueue<Map.Entry<byte[], byte[]>> QUEUE
        = new LinkedBlockingDeque<>(2_000_000);

    public static BlockingQueue<Map.Entry<byte[], byte[]>> getInstance() {
        return QUEUE;
    }
}
