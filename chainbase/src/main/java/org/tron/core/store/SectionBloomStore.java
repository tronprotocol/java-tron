package org.tron.core.store;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.common.bloom.Bloom;
import org.tron.common.utils.ByteUtil;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.capsule.TransactionRetCapsule;
import org.tron.core.db.TronStoreWithRevoking;
import org.tron.core.exception.EventBloomException;

@Slf4j(topic = "DB")
@Component
public class SectionBloomStore extends TronStoreWithRevoking<BytesCapsule> {

  public static final int BLOCK_PER_SECTION = 2048;
  private List<Integer> bitList;

  @Autowired
  public SectionBloomStore(@Value("section-bloom") String dbName) {
    super(dbName);
  }

  @Override
  public BytesCapsule get(byte[] key) {
    byte[] value = revokingDB.getUnchecked(key);
    if (ArrayUtils.isEmpty(value)) {
      return null;
    }
    return new BytesCapsule(value);
  }

  @Override
  public boolean has(byte[] key) {
    byte[] value = revokingDB.getUnchecked(key);

    return !ArrayUtils.isEmpty(value);
  }

  private long combineKey(int section, int bitIndex) {
    return section * 1_000_000L + bitIndex;
  }

  public BitSet get(int section, int bitIndex) throws EventBloomException {
    long keyLong = combineKey(section, bitIndex);
    byte[] key = Long.toHexString(keyLong).getBytes();
    BytesCapsule bytesCapsule = get(key);
    if (bytesCapsule == null) {
      return null;
    }
    byte[] data;
    try {
      data = ByteUtil.decompress(bytesCapsule.getData());
    } catch (Exception e) {
      throw new EventBloomException("decompress byte failed");
    }
    return BitSet.valueOf(data);
  }

  public void put(int section, int bitIndex, BitSet bitSet) throws EventBloomException {
    long keyLong = combineKey(section, bitIndex);
    byte[] key = Long.toHexString(keyLong).getBytes();
    byte[] compressData = ByteUtil.compress(bitSet.toByteArray());
    super.put(key, new BytesCapsule(compressData));
  }

  public Bloom initBlockSection(TransactionRetCapsule transactionRetCapsule) {
    Bloom blockBloom = Bloom.createBloom(transactionRetCapsule);

    if (Objects.isNull(blockBloom)) {
      bitList = null;
      return null;
    }

    bitList = new ArrayList<>();
    BitSet bs = BitSet.valueOf(blockBloom.getData());
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      // operate on index i here
      if (i == Integer.MAX_VALUE) {
        break; // or (i+1) would overflow
      }
      bitList.add(i);
    }

    return blockBloom;
  }

  public void write(long blockNum) throws EventBloomException {
    if (CollectionUtils.isEmpty(bitList)) {
      return;
    }

    int section = (int) (blockNum / BLOCK_PER_SECTION);
    int blockNumOffset = (int) (blockNum % BLOCK_PER_SECTION);
    for (int bitIndex : bitList) {
      // get first from leveldb
      BitSet bitSet = get(section, bitIndex);
      if (Objects.isNull(bitSet)) {
        bitSet = new BitSet(BLOCK_PER_SECTION);
      }
      // update
      bitSet.set(blockNumOffset);
      // put into leveldb
      put(section, bitIndex, bitSet);
    }
  }
}
