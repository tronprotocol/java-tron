package org.tron.core.db;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.common.utils.StringUtil;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.db.common.iterator.WitnessIterator;

@Slf4j
@Component
public class WitnessStore extends TronStoreWithRevoking<WitnessCapsule> {

  @Autowired
  protected WitnessStore(@Value("witness") String dbName) {
    super(dbName);
  }

  @Override
  public WitnessCapsule get(byte[] key) {
    byte[] value = dbSource.getData(key);
    return ArrayUtils.isEmpty(value) ? null : new WitnessCapsule(value);
  }

  @Override
  public boolean has(byte[] key) {
    byte[] account = dbSource.getData(key);
    if (account == null) {
      // For debugging
      String readableWitnessAddress = StringUtil.createReadableString(account);
      List<String> allReadableWitnessAddress =
          StringUtil.getAddressStringListFromByteArray(dbSource.allKeys());
      logger.warn(
          "address is {},witness is {},allWitness : ",
          key,
          readableWitnessAddress,
          allReadableWitnessAddress);
    }
    return null != account;
  }

  @Override
  public void put(byte[] key, WitnessCapsule item) {
    super.put(key, item);
    if (Objects.nonNull(indexHelper)) {
      indexHelper.update(item.getInstance());
    }
  }

  /**
   * get all witnesses.
   */
  public List<WitnessCapsule> getAllWitnesses() {
    return dbSource
        .allValues()
        .stream()
        .map(bytes -> new WitnessCapsule(bytes))
        .collect(Collectors.toList());
  }

  @Override
  public Iterator<Entry<byte[], WitnessCapsule>> iterator() {
    return new WitnessIterator(dbSource.iterator());
  }

  @Override
  public void delete(byte[] key) {
    deleteIndex(key);
    super.delete(key);
  }

  private void deleteIndex(byte[] key) {
    if (Objects.nonNull(indexHelper)) {
      WitnessCapsule item = get(key);
      if (Objects.nonNull(item)) {
        indexHelper.remove(item.getInstance());
      }
    }
  }
}
