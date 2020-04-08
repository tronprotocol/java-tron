package org.tron.core.metrics;

import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;

@Slf4j
public class MetricsApiServiceTest {

  @Test
  public void metricMeterTest() throws InterruptedException {
    String key = "testMeter";
    MetricsUtil.meterMark(key, 10);
    Assert.assertEquals(10, MetricsUtil.getMeter(key).getCount());
    MetricsUtil.meterMark(key, 20);
    Assert.assertEquals(30,
        MetricsUtil.getMeter(key).getCount());
    MetricsUtil.meterMark(key, 20);
    Thread.sleep(59000);
    // TimeUnit.SECONDS.sleep(59);
    // meanRate
    MetricsUtil.meterMark(key, 10);
    Assert.assertEquals(1.0,
        MetricsUtil.getMeter(key).getMeanRate(), 0.1);
    // One minute exponentially moving average rate
    double expWRate =
        (10 - MetricsUtil.getMeter(key).getMeanRate())
            * ((double) 2 / (4 + 1))
            + MetricsUtil.getMeter(key).getMeanRate();
    // compare with estimate exp rate
    Assert.assertNotEquals(1.0, MetricsUtil.getMeter(key).getOneMinuteRate(), 1);
    Assert.assertTrue(MetricsUtil.getMeter(key).getOneMinuteRate() < 5.0);
    Assert.assertTrue(MetricsUtil.getMeter(key).getOneMinuteRate() > 3.0);
    // Assert.assertEquals(expWRate,
    //    MetricsUtil.getMeter(key).getOneMinuteRate(), 1);
  }

  @Test
  public void metricCounterTest() {
    String key = "testCounter";
    MetricsUtil.getCounter(key);
    MetricsUtil.counterInc(key);
    MetricsUtil.counterInc(key);
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



}
