package org.tron.common.cache;

import com.google.common.base.Objects;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.CacheStats;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import lombok.Getter;

public class TronCache<K, V> {

  @Getter
  private final CacheType name;
  private final Cache<K, V> cache;

  TronCache(CacheType name, String strategy) {
    this.name = name;
    this.cache = CacheBuilder.from(strategy).build();
  }

  TronCache(CacheType name, String strategy, CacheLoader<K, V> loader) {
    this.name = name;
    this.cache = CacheBuilder.from(strategy).build(loader);
  }

  public void put(K k, V v) {
    this.cache.put(k, v);
  }

  public V getIfPresent(K k) {
    return this.cache.getIfPresent(k);
  }

  public V get(K k, Callable<? extends V> loader) throws ExecutionException {
    return this.cache.get(k, loader);
  }

  public CacheStats stats() {
    return this.cache.stats();
  }

  public void invalidateAll() {
    this.cache.invalidateAll();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TronCache<?, ?> tronCache = (TronCache<?, ?>) o;
    return Objects.equal(name, tronCache.name);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name);
  }
}
