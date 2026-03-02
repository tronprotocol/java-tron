package org.tron.core.services.stop;

import java.text.ParseException;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.cron.CronExpression;
import org.tron.common.parameter.CommonParameter;

@Slf4j
public class BlockTimeStopTest extends ConditionallyStopTest {
  private static final DateTimeFormatter pattern = DateTimeFormatter
      .ofPattern("ss mm HH dd MM ? yyyy");
  private static final String time = localDateTime.plusSeconds(12 * 3).format(pattern);

  private static CronExpression cronExpression;


  static {
    try {
      cronExpression = new CronExpression(time);
    } catch (ParseException e) {
      logger.error("{}", e.getMessage());
    }
  }

  @Test
  public void isValidExpression() {
    Assert.assertTrue(CronExpression.isValidExpression(cronExpression.getCronExpression()));
    ParseException err = Assert.assertThrows(ParseException.class, () ->
        CronExpression.validateExpression("invalid expression"));
    Assert.assertEquals("Illegal characters for this position: 'INV'", err.getMessage());
  }

  @Test
  public void  getNextTime() {
    Date date = cronExpression.getNextValidTimeAfter(new Date());
    Date invalidDate = cronExpression.getNextInvalidTimeAfter(new Date());
    Assert.assertNotEquals(date, invalidDate);
  }


  protected void initParameter(CommonParameter parameter) {
    parameter.setShutdownBlockTime(cronExpression);
  }

  @Override
  protected void check() throws Exception {
    long height = dbManager
        .getDynamicPropertiesStore().getLatestBlockHeaderNumberFromDB();
    Assert.assertTrue(cronExpression.isSatisfiedBy(new Date(chainManager
        .getBlockById(chainManager.getBlockIdByNum(height)).getTimeStamp())));
  }
}
