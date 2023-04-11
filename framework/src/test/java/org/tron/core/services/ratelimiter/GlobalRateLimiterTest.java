package org.tron.core.services.ratelimiter;

import java.lang.reflect.Field;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.tron.core.Constant;
import org.tron.core.config.args.Args;

public class GlobalRateLimiterTest {

  @Test
  public void testAcquire() throws Exception {
    String[] a = new String[0];
    Args.setParam(a, Constant.TESTNET_CONF);
    RuntimeData runtimeData = new RuntimeData(null);
    Field field =  runtimeData.getClass().getDeclaredField("address");
    field.setAccessible(true);
    field.set(runtimeData, "127.0.0.1");
    Assert.assertEquals(runtimeData.getRemoteAddr(), "127.0.0.1");
    GlobalRateLimiter.acquire(runtimeData);
  }

  @AfterClass
  public static void destroy() {
    Args.clearParam();
  }
}