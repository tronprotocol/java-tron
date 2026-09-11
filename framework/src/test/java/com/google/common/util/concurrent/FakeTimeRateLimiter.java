package com.google.common.util.concurrent;

import org.tron.common.math.StrictMathWrapper;

/** Test-only clock for exercising real Guava permit accounting without wall-clock sleeps. */
public final class FakeTimeRateLimiter {

  private FakeTimeRateLimiter() {
  }

  public static RateLimiter create(double permitsPerSecond) {
    return RateLimiter.create(permitsPerSecond, new Stopwatch());
  }

  public static RateLimiter createWithStoredPermit(double permitsPerSecond) {
    Stopwatch clock = new Stopwatch();
    RateLimiter limiter = RateLimiter.create(permitsPerSecond, clock);
    clock.sleepMicrosUninterruptibly((long) StrictMathWrapper.ceil(1_000_000 / permitsPerSecond));
    return limiter;
  }

  private static final class Stopwatch extends RateLimiter.SleepingStopwatch {
    private long micros;

    @Override
    protected long readMicros() {
      return micros;
    }

    @Override
    protected void sleepMicrosUninterruptibly(long sleepMicros) {
      micros += sleepMicros;
    }
  }
}
