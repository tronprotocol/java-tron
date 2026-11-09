package org.tron.core.services.stop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.protobuf.ByteString;
import java.text.ParseException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;
import org.junit.Test;
import org.tron.common.cron.CronExpression;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.capsule.BlockCapsule;

public class BlockTimeStopForkTest extends ConditionallyStopTest {

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
    while (chainManager.getNextBlockSlotTime() < cronTime - 3000) {
      generateBlock();
    }
    long height = chainManager.getHeadBlockNum() + 1;
    ByteString parent = chainManager.getDynamicPropertiesStore().getLatestBlockHeaderHash()
        .getByteString();
    BlockCapsule losing = createTestBlockCapsule(cronTime, height, parent);
    BlockCapsule beforeCron = createTestBlockCapsule(cronTime - 3000, height, parent);
    BlockCapsule afterCron = createTestBlockCapsule(cronTime + 3000, height + 1,
        beforeCron.getBlockId().getByteString());

    tronNetDelegate.processBlock(losing, false);
    assertEquals(height, dbManager.getLatestSolidityNumShutDown());

    tronNetDelegate.processBlock(beforeCron, false);
    tronNetDelegate.processBlock(afterCron, false);
    assertEquals(afterCron.getBlockId(), chainManager.getHeadBlockId());
    assertEquals(afterCron.getNum(), dbManager.getLatestSolidityNumShutDown());

    while (!tronNetDelegate.isHitDown()) {
      generateBlock();
    }
    assertTrue(tronNetDelegate.isHitDown());
    assertEquals(afterCron.getNum(),
        dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumberFromDB());
  }

  @Test(timeout = 30_000)
  public void forkAboveTheTargetKeepsIt() throws Exception {
    long cronTime = localDateTime.plusSeconds(CRON_SECONDS).toInstant(ZoneOffset.UTC)
        .toEpochMilli();
    while (dbManager.getLatestSolidityNumShutDown() <= 0) {
      generateBlock();
    }
    long target = dbManager.getLatestSolidityNumShutDown();
    assertEquals(cronTime, chainManager.getHeadBlockTimeStamp());

    long height = chainManager.getHeadBlockNum() + 1;
    long slotTime = chainManager.getNextBlockSlotTime();
    ByteString parent = chainManager.getDynamicPropertiesStore().getLatestBlockHeaderHash()
        .getByteString();
    BlockCapsule losing = createTestBlockCapsule(slotTime + 3000, height, parent);
    BlockCapsule winning = createTestBlockCapsule(slotTime, height, parent);
    BlockCapsule next = createTestBlockCapsule(slotTime + 6000, height + 1,
        winning.getBlockId().getByteString());

    tronNetDelegate.processBlock(losing, false);
    tronNetDelegate.processBlock(winning, false);
    tronNetDelegate.processBlock(next, false);

    assertEquals(next.getBlockId(), chainManager.getHeadBlockId());
    assertEquals(target, dbManager.getLatestSolidityNumShutDown());
  }
}
