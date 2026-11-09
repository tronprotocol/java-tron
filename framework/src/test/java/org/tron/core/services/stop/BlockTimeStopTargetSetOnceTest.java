package org.tron.core.services.stop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;
import java.time.ZoneOffset;
import java.util.TimeZone;
import org.junit.Test;
import org.tron.common.cron.CronExpression;
import org.tron.common.parameter.CommonParameter;

public class BlockTimeStopTargetSetOnceTest extends ConditionallyStopTest {

  @Override
  protected void initParameter(CommonParameter parameter) {
    try {
      CronExpression cron = new CronExpression("0/30 * * * * ?");
      cron.setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC));
      parameter.setShutdownBlockTime(cron);
    } catch (ParseException e) {
      throw new IllegalStateException(e);
    }
    parameter.getStorage().setMaxFlushCount(1);
  }

  @Override
  protected void check() {
  }

  @Test(timeout = 25_000)
  @Override
  public void testStop() throws Exception {
    long firstTarget = -1;
    for (int i = 0; i < 80 && !tronNetDelegate.isHitDown(); i++) {
      generateBlock();
      if (firstTarget < 0 && dbManager.getLatestSolidityNumShutDown() > 0) {
        firstTarget = dbManager.getLatestSolidityNumShutDown();
      }
    }
    assertEquals(firstTarget, dbManager.getLatestSolidityNumShutDown());
    assertTrue(tronNetDelegate.isHitDown());
  }
}
