package org.tron.core.metrics;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.parameter.CommonParameter;

public class MetricsUtilTest {

  private String test1 = "test1";
  private String test2 = "test2";
  private String test3 = "test3";
  private String test4 = "test4";
  private boolean metricsEnabled;

  @Before
  public void setUp() {
    metricsEnabled = CommonParameter.getInstance().isNodeMetricsEnable();
    CommonParameter.getInstance().setNodeMetricsEnable(true);
  }

  @After
  public void tearDown() {
    CommonParameter.getInstance().setNodeMetricsEnable(metricsEnabled);
  }

  @Test
  public void testCounterInc() {
    long count = MetricsUtil.getCounter(test1).getCount();
    MetricsUtil.counterInc(test1);
    Assert.assertEquals(count + 1, MetricsUtil.getCounter(test1).getCount());
  }

  @Test
  public void testMeterMark() {
    long count = MetricsUtil.getMeter(test2).getCount();
    MetricsUtil.meterMark(test2);
    Assert.assertEquals(count + 1, MetricsUtil.getMeter(test2).getCount());
  }

  @Test
  public void testMeterMark2() {
    long count = MetricsUtil.getMeter(test3).getCount();
    MetricsUtil.meterMark(test3, 1);
    Assert.assertEquals(count + 1, MetricsUtil.getMeter(test3).getCount());
  }

  @Test
  public void testHistogramUpdate() {
    long count = MetricsUtil.getHistogram(test4).getCount();
    MetricsUtil.histogramUpdate(test4, 1);
    Assert.assertEquals(count + 1,
        MetricsUtil.getHistogram(test4).getCount());
  }
}
