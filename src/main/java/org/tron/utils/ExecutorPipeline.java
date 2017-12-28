/*
 * java-tron is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-tron is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
