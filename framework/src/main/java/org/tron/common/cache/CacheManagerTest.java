package org.tron.common.cache;

import org.junit.Assert;
import org.junit.Test;

public class CacheManagerTest {

  @Test
  public void allocate() {
    String strategy = String.format(CacheStrategies.PATTERNS, 1, 1, "30s", 1);
    TronCache<String, String> cache = CacheManager.allocate(CacheType.witnessStandby, strategy);
    Assert.assertNull(cache.getIfPresent("test"));
  }
}
