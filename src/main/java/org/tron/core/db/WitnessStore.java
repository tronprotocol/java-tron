package org.tron.core.db;

import static org.tron.core.db.fast.FastSyncStoreConstant.TrieEnum.WITNESS;

import com.google.common.collect.Streams;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.db.fast.callback.FastSyncCallBack;
import org.tron.core.db.fast.storetrie.WitnessStoreTrie;

@Slf4j(topic = "DB")
@Component
public class WitnessStore extends TronStoreWithRevoking<WitnessCapsule> {

  @Autowired
  private FastSyncCallBack fastSyncCallBack;

  @Autowired
  private WitnessStoreTrie witnessStoreTrie;

  @Autowired
  protected WitnessStore(@Value("witness") String dbName) {
    super(dbName);
  }

  /**
   * get all witnesses.
   */
  public List<WitnessCapsule> getAllWitnesses() {
    List<WitnessCapsule> witnessCapsuleList = witnessStoreTrie.getAllWitnesses();
    if (CollectionUtils.isNotEmpty(witnessCapsuleList)) {
      return witnessCapsuleList;
    }
    return Streams.stream(iterator())
        .map(Entry::getValue)
        .collect(Collectors.toList());
  }

  @Override
  public WitnessCapsule get(byte[] key) {
    byte[] value = getValue(key);
    return ArrayUtils.isEmpty(value) ? null : new WitnessCapsule(value);
  }

  @Override
  public void put(byte[] key, WitnessCapsule item) {
    super.put(key, item);
    fastSyncCallBack.callBack(key, item.getData(), WITNESS);
  }

  @Override
  public void delete(byte[] key) {
    super.delete(key);
    fastSyncCallBack.delete(key, WITNESS);
  }

  public byte[] getValue(byte[] key) {
    byte[] value = witnessStoreTrie.getValue(key);
    if (ArrayUtils.isEmpty(value)) {
      value = revokingDB.getUnchecked(key);
    }
    return value;
  }

  @Override
  public void close() {
    super.close();
    witnessStoreTrie.close();
  }
}
