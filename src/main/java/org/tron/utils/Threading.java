package org.tron.utils;


import com.google.common.util.concurrent.CycleDetectingLockFactory;
import org.tron.core.Utils;

import java.util.concurrent.locks.ReentrantLock;

public class Threading {

    public static CycleDetectingLockFactory factory;

    public static ReentrantLock lock(String name) {
        if (Utils.isAndroidRuntime())
            return new ReentrantLock(true);
        else
            return factory.newReentrantLock(name);
    }
}
