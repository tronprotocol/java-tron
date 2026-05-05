package org.tron.core.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigFactory;
import org.junit.Assert;
import org.junit.Test;
import org.tron.core.config.args.CommitteeConfig;
import org.tron.core.config.args.MetricsConfig;
import org.tron.core.config.args.NodeConfig;
import org.tron.core.config.args.RateLimiterConfig;
import org.tron.core.config.args.StorageConfig;
import org.tron.core.config.args.VmConfig;

/**
 * Verifies that BeanDefaults.toConfig() produces a Config that:
 * 1. Contains the correct default values from Java field initializers.
 * 2. Satisfies ConfigBeanFactory.create() without ConfigException.Missing.
 * 3. Is properly overridden when a user value is supplied via withFallback().
 */
public class BeanDefaultsTest {

  // ── VmConfig ─────────────────────────────────────────────────────────────

  @Test
  public void vmConfig_defaultValues() {
    Config cfg = BeanDefaults.toConfig(new VmConfig());

    Assert.assertFalse(cfg.getBoolean("supportConstant"));
    Assert.assertEquals(100_000_000L, cfg.getLong("maxEnergyLimitForConstant"));
    Assert.assertEquals(500, cfg.getInt("lruCacheSize"));
    Assert.assertEquals(0.0, cfg.getDouble("minTimeRatio"), 0.0);
    Assert.assertEquals(5.0, cfg.getDouble("maxTimeRatio"), 0.0);
    Assert.assertEquals(10, cfg.getInt("longRunningTime"));
    Assert.assertFalse(cfg.getBoolean("estimateEnergy"));
    Assert.assertEquals(3, cfg.getInt("estimateEnergyMaxRetry"));
    Assert.assertFalse(cfg.getBoolean("vmTrace"));
    Assert.assertFalse(cfg.getBoolean("saveInternalTx"));
    Assert.assertFalse(cfg.getBoolean("saveFeaturedInternalTx"));
    Assert.assertFalse(cfg.getBoolean("saveCancelAllUnfreezeV2Details"));
  }

  @Test
  public void vmConfig_roundTrip_withConfigBeanFactory() {
    Config defaults = BeanDefaults.toConfig(new VmConfig());
    // ConfigBeanFactory must not throw ConfigException.Missing
    VmConfig vm = ConfigBeanFactory.create(defaults, VmConfig.class);
    Assert.assertFalse(vm.isSupportConstant());
    Assert.assertEquals(500, vm.getLruCacheSize());
  }

  @Test
  public void vmConfig_userValueOverridesDefault() {
    Config user = ConfigFactory.parseString("lruCacheSize = 999");
    Config merged = user.withFallback(BeanDefaults.toConfig(new VmConfig()));
    VmConfig vm = ConfigBeanFactory.create(merged, VmConfig.class);
    Assert.assertEquals(999, vm.getLruCacheSize());
    // other fields keep defaults
    Assert.assertEquals(10, vm.getLongRunningTime());
  }

  // ── NodeConfig nested bean ────────────────────────────────────────────────

  @Test
  public void nodeConfig_defaultScalars() {
    Config cfg = BeanDefaults.toConfig(new NodeConfig());

    Assert.assertEquals(30, cfg.getInt("maxConnections"));
    Assert.assertEquals(8, cfg.getInt("minConnections"));
    Assert.assertEquals(1000, cfg.getInt("maxTps"));
    Assert.assertTrue(cfg.getBoolean("openPrintLog"));
    Assert.assertFalse(cfg.getBoolean("walletExtensionApi"));
  }

  @Test
  public void nodeConfig_nestedBeans_present() {
    Config cfg = BeanDefaults.toConfig(new NodeConfig());

    // listen.port should exist as a nested object
    Assert.assertTrue(cfg.hasPath("listen"));
    Assert.assertEquals(18888, cfg.getInt("listen.port"));

    // discovery.enable
    Assert.assertTrue(cfg.hasPath("discovery"));
    Assert.assertFalse(cfg.getBoolean("discovery.enable"));

    // http.fullNodeEnable
    Assert.assertTrue(cfg.hasPath("http"));
    Assert.assertTrue(cfg.getBoolean("http.fullNodeEnable"));
    Assert.assertEquals(8090, cfg.getInt("http.fullNodePort"));

    // rpc.enable
    Assert.assertTrue(cfg.hasPath("rpc"));
    Assert.assertTrue(cfg.getBoolean("rpc.enable"));
    Assert.assertEquals(50051, cfg.getInt("rpc.port"));
  }

  @Test
  public void nodeConfig_listFields_empty() {
    Config cfg = BeanDefaults.toConfig(new NodeConfig());
    Assert.assertTrue(cfg.getList("active").isEmpty());
    Assert.assertTrue(cfg.getList("passive").isEmpty());
    Assert.assertTrue(cfg.getList("fastForward").isEmpty());
    Assert.assertTrue(cfg.getList("disabledApi").isEmpty());
  }

  @Test
  public void nodeConfig_pBFTFields_usePropertyNameAsIs() {
    Config cfg = BeanDefaults.toConfig(new NodeConfig());
    // setPBFTEnable → Introspector property name "PBFTEnable" (capital P, two consecutive
    // uppercase letters → JavaBean spec forbids decapitalization).
    // ConfigBeanFactory looks up configProps.get("PBFTEnable"), so the map key must match.
    Assert.assertTrue(cfg.hasPath("http.PBFTEnable"));
    Assert.assertTrue(cfg.getBoolean("http.PBFTEnable"));
    Assert.assertEquals(8092, cfg.getInt("http.PBFTPort"));

    Assert.assertTrue(cfg.hasPath("rpc.PBFTEnable"));
    Assert.assertEquals(50071, cfg.getInt("rpc.PBFTPort"));
  }

  @Test
  public void nodeConfig_roundTrip_withConfigBeanFactory() {
    Config defaults = BeanDefaults.toConfig(new NodeConfig());
    // Must not throw — all keys present
    NodeConfig nc = ConfigBeanFactory.create(defaults, NodeConfig.class);
    Assert.assertEquals(30, nc.getMaxConnections());
    Assert.assertEquals(18888, nc.getListenPort());
    Assert.assertTrue(nc.getRpc().isEnable());
  }

  // ── StorageConfig nested bean ─────────────────────────────────────────────

  @Test
  public void storageConfig_defaultValues() {
    Config cfg = BeanDefaults.toConfig(new StorageConfig());

    Assert.assertEquals("LEVELDB", cfg.getString("db.engine"));
    Assert.assertFalse(cfg.getBoolean("db.sync"));
    Assert.assertEquals("database", cfg.getString("db.directory"));
    Assert.assertEquals(7, cfg.getInt("dbSettings.levelNumber"));
    Assert.assertEquals(1, cfg.getInt("checkpoint.version"));
    Assert.assertTrue(cfg.getBoolean("checkpoint.sync"));
    Assert.assertEquals(1, cfg.getInt("snapshot.maxFlushCount"));
    Assert.assertTrue(cfg.getList("properties").isEmpty());
  }

  @Test
  public void storageConfig_roundTrip_withConfigBeanFactory() {
    Config defaults = BeanDefaults.toConfig(new StorageConfig());
    StorageConfig sc = ConfigBeanFactory.create(defaults, StorageConfig.class);
    Assert.assertEquals("LEVELDB", sc.getDb().getEngine());
    Assert.assertEquals(7, sc.getDbSettings().getLevelNumber());
  }

  // ── MetricsConfig nested sub-beans ───────────────────────────────────────

  @Test
  public void metricsConfig_defaultValues() {
    Config cfg = BeanDefaults.toConfig(new MetricsConfig());

    Assert.assertFalse(cfg.getBoolean("storageEnable"));
    Assert.assertFalse(cfg.getBoolean("prometheus.enable"));
    Assert.assertEquals(9527, cfg.getInt("prometheus.port"));
    Assert.assertEquals("", cfg.getString("influxdb.ip"));
    Assert.assertEquals(8086, cfg.getInt("influxdb.port"));
    Assert.assertEquals("metrics", cfg.getString("influxdb.database"));
    Assert.assertEquals(10, cfg.getInt("influxdb.metricsReportInterval"));
  }

  @Test
  public void metricsConfig_roundTrip() {
    Config defaults = BeanDefaults.toConfig(new MetricsConfig());
    MetricsConfig mc = ConfigBeanFactory.create(defaults, MetricsConfig.class);
    Assert.assertFalse(mc.isStorageEnable());
    Assert.assertEquals(9527, mc.getPrometheus().getPort());
  }

  // ── RateLimiterConfig ────────────────────────────────────────────────────

  @Test
  public void rateLimiterConfig_defaultValues() {
    Config cfg = BeanDefaults.toConfig(new RateLimiterConfig());

    Assert.assertEquals(50000, cfg.getInt("global.qps"));
    Assert.assertEquals(10000, cfg.getInt("global.ip.qps"));
    Assert.assertEquals(1000, cfg.getInt("global.api.qps"));
    Assert.assertTrue(cfg.getList("http").isEmpty());
    Assert.assertTrue(cfg.getList("rpc").isEmpty());
  }

  @Test
  public void rateLimiterConfig_roundTrip() {
    Config defaults = BeanDefaults.toConfig(new RateLimiterConfig());
    RateLimiterConfig rl = ConfigBeanFactory.create(defaults, RateLimiterConfig.class);
    Assert.assertEquals(50000, rl.getGlobal().getQps());
    Assert.assertTrue(rl.getHttp().isEmpty());
  }

  // ── CommitteeConfig ───────────────────────────────────────────────────────

  @Test
  public void committeeConfig_allZeroDefaults() {
    Config cfg = BeanDefaults.toConfig(new CommitteeConfig());

    Assert.assertEquals(0L, cfg.getLong("allowCreationOfContracts"));
    Assert.assertEquals(0L, cfg.getLong("allowMultiSign"));
    Assert.assertEquals(0L, cfg.getLong("allowTvmCancun"));
  }

  @Test
  public void committeeConfig_roundTrip() {
    Config defaults = BeanDefaults.toConfig(new CommitteeConfig());
    CommitteeConfig cc = ConfigBeanFactory.create(defaults, CommitteeConfig.class);
    Assert.assertEquals(0L, cc.getAllowCreationOfContracts());
  }
}
