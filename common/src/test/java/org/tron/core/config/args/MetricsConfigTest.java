package org.tron.core.config.args;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;

public class MetricsConfigTest {

  private static Config withRef(String hocon) {
    return ConfigFactory.parseString(hocon).withFallback(ConfigFactory.defaultReference());
  }

  private static Config withRef() {
    return ConfigFactory.defaultReference();
  }

  @Test
  public void testDefaults() {
    Config empty = withRef();
    MetricsConfig mc = MetricsConfig.fromConfig(empty);
    assertFalse(mc.isStorageEnable());
    assertFalse(mc.getPrometheus().isEnable());
    assertEquals(9527, mc.getPrometheus().getPort());
    assertEquals(8086, mc.getInfluxdb().getPort());
  }

  @Test
  public void testFromConfig() {
    Config config = withRef(
        "node.metrics {"
            + " storageEnable = true,"
            + " prometheus { enable = true, port = 9999 },"
            + " influxdb { ip = \"10.0.0.1\", port = 9086, database = mydb,"
            + "   metricsReportInterval = 30 } }");
    MetricsConfig mc = MetricsConfig.fromConfig(config);
    assertTrue(mc.isStorageEnable());
    assertTrue(mc.getPrometheus().isEnable());
    assertEquals(9999, mc.getPrometheus().getPort());
    assertEquals("10.0.0.1", mc.getInfluxdb().getIp());
    assertEquals(9086, mc.getInfluxdb().getPort());
    assertEquals("mydb", mc.getInfluxdb().getDatabase());
    assertEquals(30, mc.getInfluxdb().getMetricsReportInterval());
  }
}
