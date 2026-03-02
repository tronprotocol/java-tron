package org.tron.common.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.tron.common.cache.CacheManager.allocate;
import static org.tron.common.cache.CacheStrategies.CACHE_STRATEGY_DEFAULT;
import static org.tron.common.cache.CacheType.findByType;
import static org.tron.common.cache.CacheType.witness;
import static org.tron.common.cache.CacheType.witnessStandby;

import com.google.common.cache.CacheLoader;
import java.util.concurrent.ExecutionException;
import javax.annotation.ParametersAreNonnullByDefault;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;

@Slf4j
public class TronCacheTest {

  private TronCache<String, String> cacheWithLoader;
  private TronCache<String, String> cacheWithoutLoader;
  CacheLoader<String, String> loader = new CacheLoader<String, String>() {
    @Override
    @ParametersAreNonnullByDefault
    public String load(String key) {
      return "Loaded: " + key;
    }
  };

  @Before
  public void setUp() {

    cacheWithLoader = new TronCache<>(witness, CACHE_STRATEGY_DEFAULT, loader);

    cacheWithoutLoader = new TronCache<>(witnessStandby, CACHE_STRATEGY_DEFAULT);
  }

  @Test
  public void testGetIfPresent() {
    cacheWithoutLoader.put("key1", "value1");
    assertEquals("value1", cacheWithoutLoader.getIfPresent("key1"));
    assertNull(cacheWithoutLoader.getIfPresent("key2"));
  }

  @Test
  public void testGetWithLoader() throws ExecutionException {
    String value = cacheWithLoader.get("key1", () -> "Fallback value");
    assertEquals("Fallback value", value);
  }

  @Test
  public void testPutAndGet() {
    cacheWithoutLoader.put("key2", "value2");
    assertEquals("value2", cacheWithoutLoader.getIfPresent("key2"));
    assertEquals(witnessStandby, cacheWithoutLoader.getName());
    logger.info("hash code: {}", cacheWithoutLoader.hashCode());
  }

  @Test
  public void testStats() {
    cacheWithoutLoader.put("key3", "value3");
    assertNotNull(cacheWithoutLoader.stats());
    assertEquals(0, cacheWithoutLoader.stats().hitCount());
    cacheWithoutLoader.getIfPresent("key3");
    assertTrue(cacheWithoutLoader.stats().hitCount() > 0);
  }

  @Test
  public void testInvalidateAll() {
    cacheWithoutLoader.put("key4", "value4");
    assertEquals("value4", cacheWithoutLoader.getIfPresent("key4"));
    cacheWithoutLoader.invalidateAll();
    assertNull(cacheWithoutLoader.getIfPresent("key4"));
  }

  @Test
  public void testEquals() {
    TronCache<String, String> tmpCache = cacheWithoutLoader;
    assertEquals(cacheWithoutLoader, tmpCache);
    assertNotEquals(cacheWithoutLoader, new Object());
    assertNotEquals(cacheWithoutLoader, cacheWithLoader);
    tmpCache = new TronCache<>(witnessStandby, CACHE_STRATEGY_DEFAULT);
    assertEquals(cacheWithoutLoader, tmpCache);
  }

  @Test
  public void testCacheManager() {
    TronCache<String, String> allocate = allocate(witness, CACHE_STRATEGY_DEFAULT);
    TronCache<String, String> allocate1 = allocate(witness, CACHE_STRATEGY_DEFAULT, loader);
    assertNotNull(allocate);
    assertNotNull(allocate1);
    assertThrows(IllegalArgumentException.class, () -> findByType("test"));
  }

}
