package org.tron.common.utils;

import java.util.Optional;
import java.util.concurrent.Callable;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

@Deprecated
@Slf4j(topic = "DB")
public class AssetConverter<T, V> {

  private static final int DEFAULT_THREAD_POOL_SIZE = ThreadPoolUtil.getMaxPoolSize();
  private static final int DEFAULT_QUEUE_SIZE = 2_000_000;

  private ExecutorService executorService = Executors.newFixedThreadPool(DEFAULT_THREAD_POOL_SIZE + 1);

  private List<Future<?>> futures = new ArrayList<>();

  private Queue<T> queue = new LinkedBlockingQueue<>(DEFAULT_QUEUE_SIZE);

  private Iterable<T> source;

  private Function<T, V> consumer;
  private Function<V, Void> succeed;
  private Runnable error;

  private AtomicBoolean readFinish = new AtomicBoolean(false);

  private Stats stats = new Stats();

  private boolean allow = false;

  public AssetConverter<T, V> allow(boolean allow) {
    this.allow = allow;
    return this;
  }

  public AssetConverter<T, V> onEmit(Iterable<T> source) {
    this.source = source;
    return this;
  }

  public AssetConverter<T, V> onConsume(Function<T, V> consumer) {
    this.consumer = consumer;
    return this;
  }

  public AssetConverter<T, V> onSucceed(Function<V, Void> succeed) {
    this.succeed = succeed;
    return this;
  }

  public AssetConverter<T, V> onError(Runnable error) {
    this.error = error;
    return this;
  }

  public AssetConverter<T, V> execute() {
    if (!allow) {
      return this;
    }

    // read
    stats.readStarter.set(System.currentTimeMillis());
    executorService.execute(() -> {
      for (T t : source) {
        queue.add(t);
        stats.readCount.incrementAndGet();
        stats.readCost.set(System.currentTimeMillis() - stats.readStarter.get());
      }

      readFinish.set(true);
    });

    // write
    stats.writeStarter.set(System.currentTimeMillis());
    for (int i = 0; i < DEFAULT_THREAD_POOL_SIZE; i++) {

      futures.add(executorService.submit(() -> {
        while (true) {
          try {
            T t = queue.poll();
            if (readFinish.get() && Objects.isNull(t)) {
              break;
            }

            if (Objects.isNull(t)) {
              TimeUnit.MILLISECONDS.sleep(5);
              continue;
            }
            V v = consumer.apply(t);
            if (Objects.nonNull(succeed)) {
              succeed.apply(v);
            }
            stats.writeCount.incrementAndGet();
            stats.writeCost.getAndSet(System.currentTimeMillis() - stats.writeStarter.get());
          } catch (Exception e) {
            if (Objects.nonNull(error)) {
              error.run();
            }
          }
        }
      }));
    }

    return this;
  }

  public void done() {
    done(null);
  }

  public void done(Runnable runnable) {
    for (Future<?> future : futures) {
      try {
        future.get();
      } catch (InterruptedException | ExecutionException e) {
        logger.error(e.getMessage(), e);
      }
    }

    if (Objects.nonNull(runnable)) {
      runnable.run();
    }
  }

  public Stats stats() {
    return stats;
  }

  @ToString(exclude = {"readStarter", "writeStarter"})
  public static class Stats {
    private AtomicLong readCount = new AtomicLong(0);
    private AtomicLong readStarter = new AtomicLong(0);
    private AtomicLong readCost = new AtomicLong(0);
    private AtomicLong writeCount = new AtomicLong(0);
    private AtomicLong writeStarter = new AtomicLong(0);
    private AtomicLong writeCost = new AtomicLong(0);

    public long getReadCount() {
      return readCount.get();
    }

    public long getReadCost() {
      return readCost.get();
    }

    public long getWriteCount() {
      return writeCount.get();
    }

    public long getWriteCost() {
      return writeCost.get();
    }
  }
}
