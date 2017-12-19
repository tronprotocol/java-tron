package org.tron.utils;


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public class ExecutorPipeline<In, Out> {

    private BlockingQueue<Runnable> queue;
    private ThreadPoolExecutor exce;
    private boolean preserveOrder = false;
    private Functional.Function<In, Out> processor;
    private ExecutorPipeline<Out, ?> next;
    private Functional.Consumer<Throwable> exceptionHandler;
    private String threadPoolName;
    private AtomicLong orderCounter = new AtomicLong();

    private ReentrantLock lock = new ReentrantLock();
    private long nextOutTaskNumber = 0;
    private Map<Long, Out> orderMap = new HashMap<>();


    private static AtomicInteger pipeNumber = new AtomicInteger(1);
    private AtomicInteger threadNumber = new AtomicInteger(1);

    public ExecutorPipeline(int threads, int queueSize, boolean preserveOrder, Functional.Function<In, Out> processor,
                            Functional.Consumer<Throwable> exceptionHandler) {
        queue = new LimitedQueue<>(queueSize);
        exce = new ThreadPoolExecutor(threads, threads, 0L, TimeUnit.MILLISECONDS, queue, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, threadPoolName + "-" + threadNumber.getAndIncrement());
            }
        });
        this.preserveOrder = preserveOrder;
        this.processor = processor;
        this.exceptionHandler = exceptionHandler;
        this.threadPoolName = "pipe-" + pipeNumber.getAndIncrement();
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

    public ExecutorPipeline<Out, Void> add(int threads, int queueSize, final Functional.Consumer<Out> consumer) {
        return add(threads, queueSize, false, new Functional.Function<Out, Void>() {
            @Override
            public Void apply(Out out) {
                consumer.accept(out);
                return null;
            }
        });
    }

    public <NextOut> ExecutorPipeline<Out, NextOut> add(int threads, int queueSize, boolean preserveOrder,
                                                        Functional.Function<Out, NextOut> processor) {
        ExecutorPipeline<Out, NextOut> ret = new ExecutorPipeline<>(threads, queueSize, preserveOrder, processor,
                exceptionHandler);
        next = ret;
        return ret;
    }

    private void pushNext(long order, Out res) {
        if (next != null) {
            if (!preserveOrder) {
                next.push(res);
            } else {
                lock.lock();
                try {
                    if (order == nextOutTaskNumber) {
                        next.push(res);
                        while (true) {
                            nextOutTaskNumber++;
                            Out out = orderMap.remove(nextOutTaskNumber);
                            if (out == null) break;
                            next.push(out);
                        }
                    } else {
                        orderMap.put(order, res);
                    }
                } finally {
                    lock.unlock();
                }
            }
        }
    }

    public void push(final In in) {
        final long order = orderCounter.getAndIncrement();
        exce.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    pushNext(order, processor.apply(in));
                } catch (Throwable e) {
                    exceptionHandler.accept(e);
                }
            }
        });
    }


}
