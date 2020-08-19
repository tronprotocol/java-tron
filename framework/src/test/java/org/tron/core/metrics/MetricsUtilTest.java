package org.tron.core.metrics;

import org.junit.Assert;
import org.junit.Test;

public class MetricsUtilTest {

  private String test1 = "test1";
  private String test2 = "test2";
  private String test3 = "test3";
  private String test4 = "test4";

  @Test
  public void testCounterInc() {
    MetricsUtil.counterInc(test1);
    //Assert
    //    .assertEquals(1, MetricsUtil.getCounter(test1).getCount());
  }

  //@Test
  public void testMeterMark() {
    MetricsUtil.meterMark(test2);
    Assert.assertEquals(1, MetricsUtil.getMeter(test2).getCount());
  }

  //@Test
  public void testMeterMark2() {
    MetricsUtil.meterMark(test3, 1);
    Assert.assertEquals(1, MetricsUtil.getMeter(test3).getCount());
  }

  //@Test
  public void testHistogramUpdate() {
    MetricsUtil.histogramUpdate(test4, 1);
    Assert.assertEquals(1,
        MetricsUtil.getHistogram(test4).getCount());
  }
}
