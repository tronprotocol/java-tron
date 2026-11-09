package org.tron.core.services.stop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;
import org.junit.Test;
import org.tron.common.cron.CronExpression;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.capsule.BlockCapsule;

public class BlockTimeStopMissedSlotTest extends ConditionallyStopTest {

  private static final DateTimeFormatter PATTERN = DateTimeFormatter
      .ofPattern("ss mm HH dd MM ? yyyy");
  private static final long CRON_SECONDS = 39;

  @Override
  protected void initParameter(CommonParameter parameter) {
    try {
      CronExpression cron = new CronExpression(
          localDateTime.plusSeconds(CRON_SECONDS).format(PATTERN));
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

  @Test(timeout = 30_000)
  @Override
  public void testStop() throws Exception {
    long cronTime = localDateTime.plusSeconds(CRON_SECONDS).toInstant(ZoneOffset.UTC)
        .toEpochMilli();
    while (chainManager.getNextBlockSlotTime() < cronTime) {
      generateBlock();
    }
    assertEquals(cronTime, chainManager.getNextBlockSlotTime());

    BlockCapsule afterMissedSlot = createTestBlockCapsule(cronTime + 3000,
        chainManager.getHeadBlockNum() + 1,
        chainManager.getDynamicPropertiesStore().getLatestBlockHeaderHash().getByteString());
    tronNetDelegate.processBlock(afterMissedSlot, false);
    assertEquals(afterMissedSlot.getNum(), dbManager.getLatestSolidityNumShutDown());

    while (!tronNetDelegate.isHitDown()) {
      generateBlock();
    }
    assertTrue(tronNetDelegate.isHitDown());
    assertEquals(afterMissedSlot.getNum(),
        dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumberFromDB());
  }
}
