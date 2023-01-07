package org.tron.core.store;

import com.google.common.collect.Streams;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.common.cache.CacheManager;
import org.tron.common.cache.CacheStrategies;
import org.tron.common.cache.CacheType;
import org.tron.common.cache.TronCache;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.config.Parameter;
import org.tron.core.db.TronStoreWithRevoking;

@Slf4j(topic = "DB")
@Component
public class WitnessStore extends TronStoreWithRevoking<WitnessCapsule> {
 // cache for 127 SR
  private final TronCache<Integer, List<WitnessCapsule>> witnessStandbyCache;

  @Autowired
  protected WitnessStore(@Value("witness") String dbName) {
    super(dbName);
    String strategy =  String.format(CacheStrategies.PATTERNS, 1, 1, "30s", 1);
    witnessStandbyCache = CacheManager.allocate(CacheType.witnessStandby, strategy);
  }

  /**
   * get all witnesses.
   */
  public List<WitnessCapsule> getAllWitnesses() {
    return Streams.stream(iterator())
        .map(Entry::getValue)
        .collect(Collectors.toList());
  }

  @Override
  public WitnessCapsule get(byte[] key) {
    byte[] value = revokingDB.getUnchecked(key);
    return ArrayUtils.isEmpty(value) ? null : new WitnessCapsule(value);
  }

  public List<WitnessCapsule> getWitnessStandby() {
    List<WitnessCapsule> list =
        witnessStandbyCache.getIfPresent(Parameter.ChainConstant.WITNESS_STANDBY_LENGTH);
    if (list != null) {
      return list;
    }
    return updateWitnessStandby(null);
  }

  public List<WitnessCapsule> updateWitnessStandby(List<WitnessCapsule> all) {
    List<WitnessCapsule> ret;
    if (all == null) {
      all = getAllWitnesses();
    }
    all.sort(Comparator.comparingLong(WitnessCapsule::getVoteCount)
        .reversed().thenComparing(Comparator.comparingInt(
            (WitnessCapsule w) -> w.getAddress().hashCode()).reversed()));
    if (all.size() > Parameter.ChainConstant.WITNESS_STANDBY_LENGTH) {
      ret = new ArrayList<>(all.subList(0, Parameter.ChainConstant.WITNESS_STANDBY_LENGTH));
    } else {
      ret = new ArrayList<>(all);
    }
    // trim voteCount = 0
    ret.removeIf(w -> w.getVoteCount() < 1);
    witnessStandbyCache.put(Parameter.ChainConstant.WITNESS_STANDBY_LENGTH, ret);
    return ret;
  }

}
