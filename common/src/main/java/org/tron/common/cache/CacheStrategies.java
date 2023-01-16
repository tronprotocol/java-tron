package org.tron.common.cache;

import static org.tron.common.cache.CacheType.abi;
import static org.tron.common.cache.CacheType.account;
import static org.tron.common.cache.CacheType.assetIssueV2;
import static org.tron.common.cache.CacheType.code;
import static org.tron.common.cache.CacheType.contract;
import static org.tron.common.cache.CacheType.delegatedResource;
import static org.tron.common.cache.CacheType.delegatedResourceAccountIndex;
import static org.tron.common.cache.CacheType.delegation;
import static org.tron.common.cache.CacheType.properties;
import static org.tron.common.cache.CacheType.recentBlock;
import static org.tron.common.cache.CacheType.storageRow;
import static org.tron.common.cache.CacheType.votes;
import static org.tron.common.cache.CacheType.witness;
import static org.tron.common.cache.CacheType.witnessSchedule;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CacheStrategies {


  public static final String PATTERNS =
      "initialCapacity=%d,maximumSize=%d,expireAfterAccess=%s,concurrencyLevel=%d,recordStats";
  public static final List<CacheType> CACHE_BIG_DBS = Collections.singletonList(delegation);
  private static final int CPUS = Runtime.getRuntime().availableProcessors();
  public static final String CACHE_STRATEGY_DEFAULT =
      String.format(PATTERNS, 1000, 1000, "30s", CPUS);
  private static final String CACHE_STRATEGY_SMALL_DEFAULT =
      String.format(PATTERNS, 100, 100, "30s", CPUS);
  private static final List<CacheType> CACHE_SMALL_DBS = Arrays.asList(recentBlock, witness,
      witnessSchedule, delegatedResource, delegatedResourceAccountIndex,
      votes, abi);
  private static final String CACHE_STRATEGY_NORMAL_DEFAULT =
      String.format(PATTERNS, 500, 500, "30s", CPUS);
  private static final List<CacheType> CACHE_NORMAL_DBS = Arrays.asList(code, contract,
      assetIssueV2, properties);
  private static final String CACHE_STRATEGY_BIG_DEFAULT =
      String.format(PATTERNS, 10000, 10000, "30s", CPUS);
  private static final String CACHE_STRATEGY_HUGE_DEFAULT =
      String.format(PATTERNS, 20000, 20000, "30s", CPUS);
  private static final List<CacheType> CACHE_HUGE_DBS = Arrays.asList(storageRow, account);

  public static final List<String> CACHE_DBS = Stream.of(CACHE_SMALL_DBS, CACHE_NORMAL_DBS,
          CACHE_BIG_DBS, CACHE_HUGE_DBS).flatMap(Collection::stream).map(CacheType::toString)
      .collect(Collectors.toList());


  public static String getCacheStrategy(CacheType dbName) {
    String defaultStrategy = CACHE_STRATEGY_DEFAULT;
    if (CACHE_SMALL_DBS.contains(dbName)) {
      defaultStrategy = CACHE_STRATEGY_SMALL_DEFAULT;
    }
    if (CACHE_NORMAL_DBS.contains(dbName)) {
      defaultStrategy = CACHE_STRATEGY_NORMAL_DEFAULT;
    }
    if (CACHE_BIG_DBS.contains(dbName)) {
      defaultStrategy = CACHE_STRATEGY_BIG_DEFAULT;
    }
    if (CACHE_HUGE_DBS.contains(dbName)) {
      defaultStrategy = CACHE_STRATEGY_HUGE_DEFAULT;
    }
    return defaultStrategy;
  }
}
