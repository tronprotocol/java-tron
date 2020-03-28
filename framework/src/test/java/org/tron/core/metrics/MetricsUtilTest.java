package org.tron.core.metrics;

import org.junit.Assert;
import org.junit.Test;

public class MetricsUtilTest {

  @Test
  public void testCounterInc() {
    MetricsUtil.counterInc(MetricsKey.BLOCKCHAIN_FAIL_FORK_COUNT);
    Assert
        .assertEquals(1, MetricsUtil.getCounter(MetricsKey.BLOCKCHAIN_FAIL_FORK_COUNT).getCount());
  }

  @Test
  public void testMeterMark() {
    MetricsUtil.meterMark(MetricsKey.BLOCKCHAIN_FAIL_FORK_COUNT);
    Assert.assertEquals(1, MetricsUtil.getMeter(MetricsKey.BLOCKCHAIN_FAIL_FORK_COUNT).getCount());
  }

  @Test
  public void testMeterMark2() {
    MetricsUtil.meterMark(MetricsKey.BLOCKCHAIN_FAIL_FORK_COUNT, 2);
    Assert.assertEquals(2, MetricsUtil.getMeter(MetricsKey.BLOCKCHAIN_FAIL_FORK_COUNT).getCount());
  }

  @Test
  public void testHistogramUpdate() {
    MetricsUtil.histogramUpdate(MetricsKey.BLOCKCHAIN_FAIL_FORK_COUNT, 2);
    Assert.assertEquals(2,
        MetricsUtil.getHistogram(MetricsKey.BLOCKCHAIN_FAIL_FORK_COUNT).getCount());
  }
}
