package org.tron.core.metrics;

import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;

@Slf4j
public class MetricsApiServiceTest {

  @Test
  public void metricMeterTest() throws InterruptedException {

    MetricsUtil.meterMark(MetricsKey.BLOCKCHAIN_BLOCK_PROCESS_TIME, 10);
    Assert.assertEquals(10,
        MetricsUtil.getMeter(MetricsKey.BLOCKCHAIN_BLOCK_PROCESS_TIME).getCount());
    MetricsUtil.meterMark(MetricsKey.BLOCKCHAIN_BLOCK_PROCESS_TIME, 20);
    Assert.assertEquals(30,
        MetricsUtil.getMeter(MetricsKey.BLOCKCHAIN_BLOCK_PROCESS_TIME).getCount());
    MetricsUtil.meterMark(MetricsKey.BLOCKCHAIN_BLOCK_PROCESS_TIME, 20);
    TimeUnit.SECONDS.sleep(59);
    // meanRate
    MetricsUtil.meterMark(MetricsKey.BLOCKCHAIN_BLOCK_PROCESS_TIME, 10);
    Assert.assertEquals(1.0,
        MetricsUtil.getMeter(MetricsKey.BLOCKCHAIN_BLOCK_PROCESS_TIME).getMeanRate(), 0.1);
    // One minute exponentially moving average rate
    double expWRate =
        (10 - MetricsUtil.getMeter(MetricsKey.BLOCKCHAIN_BLOCK_PROCESS_TIME).getMeanRate())
            * ((double) 2 / (4 + 1))
            + MetricsUtil.getMeter(MetricsKey.BLOCKCHAIN_BLOCK_PROCESS_TIME).getMeanRate();
    // compare with estimate exp rate
    Assert.assertEquals(expWRate,
        MetricsUtil.getMeter(MetricsKey.BLOCKCHAIN_BLOCK_PROCESS_TIME).getOneMinuteRate(), 1);
  }

  @Test
  public void metricCounterTest() {
    String key = "testCounter";
    MetricsUtil.getCounter(key);
    MetricsUtil.counterInc(key, 2);
    Assert.assertEquals(2.0, MetricsUtil.getCounter(key).getCount(), 0.1);
  }

  @Test
  public void metricHistogram() {
    String key = "testHistogram";
    MetricsUtil.getHistogram(key);
    for (int i = 0; i < 100; i++) {
      MetricsUtil.histogramUpdate(key, 100 - i);
    }
    // sort array [1,2,3,4,5,.....,100]
    Assert.assertEquals(99.0,
        MetricsUtil.getHistogram(key).getSnapshot().get99thPercentile(),
        0.1);
    Assert.assertEquals(75.0,
        MetricsUtil.getHistogram(key).getSnapshot().get75thPercentile(),
        0.1);
    Assert.assertEquals(98.0,
        MetricsUtil.getHistogram(key).getSnapshot().get98thPercentile(),
        0.1);
    Assert.assertEquals(75.0, MetricsUtil.getHistogram(key).getSnapshot().get75thPercentile(),
        0.1);
    Assert.assertEquals(100, MetricsUtil.getHistogram(key).getCount(),
        0.1);
  }


  @Test
  public void MonitorApiIntervalTest() throws InterruptedException {
  }


}
