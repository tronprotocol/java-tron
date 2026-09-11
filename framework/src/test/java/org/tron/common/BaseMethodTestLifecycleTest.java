package org.tron.common;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.tron.common.application.TronApplicationContext;
import org.tron.core.config.args.Args;

public class BaseMethodTestLifecycleTest {

  @Test
  public void closesContextWhenSubclassCleanupFails() {
    BaseMethodTest fixture = new BaseMethodTest() {
      @Override
      protected void beforeDestroy() {
        throw new IllegalStateException("intentional cleanup failure");
      }
    };
    fixture.context = Mockito.mock(TronApplicationContext.class);
    Args.setParam(new String[0], TestConstants.TEST_CONF);
    try {
      IllegalStateException failure = Assert.assertThrows(IllegalStateException.class,
          fixture::destroyContext);
      Assert.assertEquals("intentional cleanup failure", failure.getMessage());
      Mockito.verify(fixture.context).close();
      Assert.assertEquals(0, Args.getInstance().getHttpMaxMessageSize());
    } finally {
      Args.clearParam();
    }
  }
}
