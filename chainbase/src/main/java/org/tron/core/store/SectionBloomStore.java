package org.tron.core.store;

import com.google.common.collect.Streams;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.common.crypto.Hash;
import org.tron.common.logsfilter.Bloom;
import org.tron.common.utils.ByteUtil;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.capsule.TransactionRetCapsule;
import org.tron.core.db.TronStoreWithRevoking;
import org.tron.core.exception.EventBloomException;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.Protocol.TransactionInfo.Log;

@Slf4j(topic = "DB")
@Component
public class SectionBloomStore extends TronStoreWithRevoking<BytesCapsule> {

  public static int blockPerSection = 2048;
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

  public Bloom initBlockSection(long blockNum, TransactionRetCapsule transactionRetCapsule) {
    Iterator<TransactionInfo> it =
        transactionRetCapsule.getInstance().getTransactioninfoList().iterator();
    Bloom blockBloom = null;

    while (it.hasNext()) {
      TransactionInfo transactionInfo = it.next();
      //if contract address is empty, skip
      if (ArrayUtils.isEmpty(transactionInfo.getContractAddress().toByteArray())) {
        continue;
      }
      if (blockBloom == null) {
        blockBloom = new Bloom();
      }
      Bloom bloom = Bloom.create(Hash.sha3(transactionInfo.getContractAddress().toByteArray()));
      blockBloom.or(bloom);
      for (Log log : transactionInfo.getLogList()) {
        for (ByteString topic : log.getTopicsList()) {
          bloom = Bloom.create(Hash.sha3(topic.toByteArray()));
          blockBloom.or(bloom);
        }
      }
    }

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
    logger.info("write section-bloom {}", blockNum);
    if (CollectionUtils.isEmpty(bitList)) {
      return;
    }

    int section = (int) (blockNum / blockPerSection);
    int blockNumOffset = (int) (blockNum % blockPerSection);
    for (int bitIndex : bitList) {
      // get first from leveldb
      BitSet bitSet = get(section, bitIndex);
      if (Objects.isNull(bitSet)) {
        bitSet = new BitSet(blockPerSection);
      }
      // update
      bitSet.set(blockNumOffset);
      // put into leveldb
      put(section, bitIndex, bitSet);
    }
  }
}
