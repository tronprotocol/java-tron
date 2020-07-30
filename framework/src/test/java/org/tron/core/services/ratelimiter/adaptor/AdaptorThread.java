package org.tron.core.services.ratelimiter.adaptor;

import java.util.concurrent.CountDownLatch;
import org.tron.core.services.ratelimiter.strategy.QpsStrategy;

class AdaptorThread implements Runnable {

  private CountDownLatch latch;
  private QpsStrategy strategy;

  public AdaptorThread(CountDownLatch latch, QpsStrategy strategy) {
    this.latch = latch;
    this.strategy = strategy;
  }

  @Override
  public void run() {
    strategy.acquire();
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      System.out.println(e.getMessage());
    }
    latch.countDown();
  }
}
