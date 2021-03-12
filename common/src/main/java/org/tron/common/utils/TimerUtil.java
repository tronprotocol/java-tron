package org.tron.common.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class TimerUtil {

    public static Timer countDown(String message) {
        Timer timer = new Timer();
        AtomicInteger count = new AtomicInteger();
        timer.schedule(new TimerTask() {
            public void run() {
                int second = count.incrementAndGet();
                if (second % 5 == 0) {
                    logger.info(message + ": {} s", second);
                }
            }
        }, 0, 1000);
        return timer;
    }

    public static void cancel (Timer timer) {
        if (null != timer) {
            timer.cancel();
        }
    }
}
