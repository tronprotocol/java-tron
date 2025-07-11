package org.tron.core.net;

import static org.tron.core.net.message.MessageTypes.FETCH_INV_DATA;
import static org.tron.core.net.message.MessageTypes.SYNC_BLOCK_CHAIN;

import org.junit.Assert;
import org.junit.Test;

public class P2pRateLimiterTest {
  @Test
  public void test() {
    P2pRateLimiter limiter = new P2pRateLimiter();
    limiter.register(SYNC_BLOCK_CHAIN.asByte(), 2);
    limiter.acquire(SYNC_BLOCK_CHAIN.asByte());
    boolean ret = limiter.tryAcquire(SYNC_BLOCK_CHAIN.asByte());
    Assert.assertTrue(ret);
    limiter.tryAcquire(SYNC_BLOCK_CHAIN.asByte());
    ret = limiter.tryAcquire(SYNC_BLOCK_CHAIN.asByte());
    Assert.assertFalse(ret);
    ret = limiter.tryAcquire(FETCH_INV_DATA.asByte());
    Assert.assertTrue(ret);
  }
}
