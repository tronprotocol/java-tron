package org.tron.core.services.stop;

import static org.junit.Assert.assertEquals;

import java.text.ParseException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;
import org.tron.common.cron.CronExpression;
import org.tron.common.parameter.CommonParameter;

public class BlockTimeStopOffGridTest extends ConditionallyStopTest {

  private static final DateTimeFormatter PATTERN = DateTimeFormatter
      .ofPattern("ss mm HH dd MM ? yyyy");
  private static final long CRON_SECONDS = 37;

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
    // The test config's count of 200 looks back past the cron time, so these blocks would never
    // get a null.
    parameter.getStorage().setMaxFlushCount(1);
  }

  @Override
  protected void check() throws Exception {
    long height = dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumberFromDB();
    long stopTime = chainManager.getBlockById(chainManager.getBlockIdByNum(height)).getTimeStamp();
    long firstSlotAfterCron = localDateTime.plusSeconds(CRON_SECONDS + 2)
        .toInstant(ZoneOffset.UTC).toEpochMilli();
    assertEquals(firstSlotAfterCron, stopTime);
  }
}
