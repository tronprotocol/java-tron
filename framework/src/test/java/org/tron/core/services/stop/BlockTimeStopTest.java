package org.tron.core.services.stop;

import java.text.ParseException;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.quartz.CronExpression;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.exception.P2pException;

@Slf4j
public class BlockTimeStopTest extends ConditionallyStopTest {

  // @see https://tronscan.org/#/block/12
  private static final String time = "00 52 09 25 06 ? 2018";

  private static CronExpression cronExpression;


  static {
    try {
      cronExpression = new CronExpression(time);
    } catch (ParseException e) {
      logger.error("{}", e.getMessage());
    }
  }


  protected void initParameter(CommonParameter parameter) {
    parameter.setShutdownBlockTime(cronExpression);
    // will ignore
    parameter.setShutdownBlockHeight(48);
    // will ignore
    parameter.setShutdownBlockCount(32);
  }

  @Override
  protected void check() {
    try {
      long height = tronNetDelegate.getDbManager()
          .getDynamicPropertiesStore().getLatestBlockHeaderNumberFromDB();
      Assert.assertTrue(cronExpression.isSatisfiedBy(new Date(tronNetDelegate
          .getBlockTime(tronNetDelegate.getBlockIdByNum(height)))));
    } catch (P2pException e) {
      logger.error("{}", e.getMessage());
    }
  }

  @Override
  protected void initDbPath() {
    dbPath = "output-time-stop";
  }

}
