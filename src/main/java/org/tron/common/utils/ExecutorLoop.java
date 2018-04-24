package org.tron.common.utils;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class ExecutorLoop<In> {

  private BlockingQueue<Runnable> queue;
  private ThreadPoolExecutor exec;
  private Consumer<In> consumer;
  private Consumer<Throwable> exceptionHandler;
  private String threadPoolName;

  private static AtomicInteger loopNum = new AtomicInteger(1);
  private AtomicInteger threadNumber = new AtomicInteger(1);

  public ExecutorLoop(
      int threads,
      int queueSize,
      Consumer<In> consumer,
      Consumer<Throwable> exceptionHandler) {

    this.queue = new LimitedQueue<>(queueSize);
    this.exec = new ThreadPoolExecutor(
      threads,
      threads,
      0L,
      TimeUnit.MILLISECONDS,
      queue,
      r -> new Thread(r, threadPoolName + "-" + threadNumber.getAndIncrement())
    );

    this.consumer = consumer;
    this.exceptionHandler = exceptionHandler;
    this.threadPoolName = "loop-" + loopNum.getAndIncrement();
  }

  public void push(final In in) {
    exec.execute(() -> {
      try {
        consumer.accept(in);
      } catch (Throwable e) {
        exceptionHandler.accept(e);
      }
    });
  }

  public void pushAll(final List<In> list) {
    for (In in : list) {
      push(in);
    }
  }

  public ExecutorLoop<In> setThreadPoolName(String threadPoolName) {
    this.threadPoolName = threadPoolName;
    return this;
  }

  public BlockingQueue<Runnable> getQueue() {
    return queue;
  }


  public void shutdown() {
    try {
      exec.shutdown();
    } catch (Exception e) {
    }
  }

  public boolean isShutdown() {
    return exec.isShutdown();
  }

  public void join() throws InterruptedException {
    exec.shutdown();
    exec.awaitTermination(10, TimeUnit.MINUTES);
  }

  private static class LimitedQueue<E> extends LinkedBlockingQueue<E> {

    public LimitedQueue(int maxSize) {
      super(maxSize);
    }

    @Override
    public boolean offer(E e) {
      // turn offer() and add() into a blocking calls (unless interrupted)
      try {
        put(e);
        return true;
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
      }
      return false;
    }
  }
}

