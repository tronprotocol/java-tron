package org.tron.common.cache;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.CacheStats;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.stream.Collectors;
import org.tron.common.parameter.CommonParameter;

public class CacheManager {

  private static final Map<CacheType, TronCache<?, ?>> CACHES  = Maps.newConcurrentMap();

  public static <K, V> TronCache<K, V> allocate(CacheType name) {
    TronCache<K, V> cache = new TronCache<>(name, CommonParameter.getInstance()
        .getStorage().getCacheStrategy(name));
    CACHES.put(name, cache);
    return cache;
  }

  public  static <K, V> TronCache<K, V> allocate(CacheType name, String strategy) {
    TronCache<K, V> cache = new TronCache<>(name, strategy);
    CACHES.put(name, cache);
    return cache;
  }

  public  static <K, V> TronCache<K, V> allocate(CacheType name, String strategy,
                                                 CacheLoader<K, V> loader) {
    TronCache<K, V> cache = new TronCache<>(name, strategy, loader);
    CACHES.put(name, cache);
    return cache;
  }


  public static void release(TronCache<?, ?> cache) {
    cache.invalidateAll();
  }

  public static Map<String, CacheStats> stats() {
    return CACHES.values().stream().collect(Collectors.toMap(c -> c.getName().toString(),
        TronCache::stats));
  }

}
