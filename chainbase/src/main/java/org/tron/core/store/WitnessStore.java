package org.tron.core.store;

import com.google.common.collect.Streams;
import com.google.protobuf.ByteString;
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
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.config.Parameter;
import org.tron.core.db.TronStoreWithRevoking;

@Slf4j(topic = "DB")
@Component
public class WitnessStore extends TronStoreWithRevoking<WitnessCapsule> {

  @Autowired
  protected WitnessStore(@Value("witness") String dbName) {
    super(dbName);
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

  public List<WitnessCapsule> getWitnessStandby(boolean isSortOpt) {
    List<WitnessCapsule> ret;
    List<WitnessCapsule> all = getAllWitnesses();
    sortWitnesses(all, isSortOpt);
    if (all.size() > Parameter.ChainConstant.WITNESS_STANDBY_LENGTH) {
      ret = new ArrayList<>(all.subList(0, Parameter.ChainConstant.WITNESS_STANDBY_LENGTH));
    } else {
      ret = new ArrayList<>(all);
    }
    // trim voteCount = 0
    ret.removeIf(w -> w.getVoteCount() < 1);
    return ret;
  }

  public void sortWitnesses(List<WitnessCapsule> witnesses, boolean isSortOpt) {
    witnesses.sort(Comparator.comparingLong(WitnessCapsule::getVoteCount).reversed()
        .thenComparing(isSortOpt
            ? Comparator.comparing(WitnessCapsule::createReadableString).reversed()
            : Comparator.comparingInt((WitnessCapsule w) -> w.getAddress().hashCode()).reversed()));
  }

  public void sortWitness(List<ByteString> list, boolean isSortOpt) {
    list.sort(Comparator.comparingLong((ByteString b) -> get(b.toByteArray()).getVoteCount())
        .reversed().thenComparing(isSortOpt
            ? Comparator.comparing(
                (ByteString b) -> ByteArray.toHexString(b.toByteArray())).reversed()
            : Comparator.comparingInt(ByteString::hashCode).reversed()));
  }
}
