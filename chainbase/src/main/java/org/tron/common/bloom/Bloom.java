package org.tron.common.bloom;

import com.google.protobuf.ByteString;
import java.util.Arrays;
import java.util.Iterator;
import org.tron.common.crypto.Hash;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ByteUtil;
import org.tron.core.capsule.TransactionRetCapsule;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.Protocol.TransactionInfo.Log;

public class Bloom {

  public static final int BLOOM_BIT_SIZE = 2048;
  public static final int BLOOM_BYTE_SIZE = BLOOM_BIT_SIZE / 8;
  private static final int STEPS_8 = 8;
  private static final int ENSURE_BYTE = 255;
  private static final int LOW_3_BITS = getLowBits(BLOOM_BIT_SIZE);
  private byte[] data = new byte[BLOOM_BYTE_SIZE];

  public Bloom() {
  }

  public Bloom(byte[] data) {
    if (data.length != this.data.length) {
      throw new RuntimeException(
          "input data length is not equal to Bloom size " + this.data.length);
    }
    this.data = data;
  }

  //get several low bit. 512 -> 0b1, 1024 -> 0b11, 2048 -> 0b111, 4096-> 0b1111
  public static int getLowBits(int bloomBitSize) {
    return ENSURE_BYTE >> (16 + 1 - Integer.toBinaryString(bloomBitSize).length());
  }

  //only use first six byte
  public static Bloom create(byte[] toBloom) {

    int mov1 =
        (((toBloom[0] & ENSURE_BYTE) & (LOW_3_BITS)) << STEPS_8) + ((toBloom[1]) & ENSURE_BYTE);
    int mov2 =
        (((toBloom[2] & ENSURE_BYTE) & (LOW_3_BITS)) << STEPS_8) + ((toBloom[3]) & ENSURE_BYTE);
    int mov3 =
        (((toBloom[4] & ENSURE_BYTE) & (LOW_3_BITS)) << STEPS_8) + ((toBloom[5]) & ENSURE_BYTE);

    byte[] data = new byte[BLOOM_BYTE_SIZE];
    Bloom bloom = new Bloom(data);

    ByteUtil.setBit(data, mov1, 1);
    ByteUtil.setBit(data, mov2, 1);
    ByteUtil.setBit(data, mov3, 1);

    return bloom;
  }

  public static Bloom createBloom(TransactionRetCapsule transactionRetCapsule) {
    if (transactionRetCapsule == null) {
      return null;
    }
    Iterator<TransactionInfo> it =
        transactionRetCapsule.getInstance().getTransactioninfoList().iterator();
    Bloom blockBloom = null;

    while (it.hasNext()) {
      TransactionInfo transactionInfo = it.next();
      if (transactionInfo == null || transactionInfo.getLogCount() == 0) {
        continue;
      }

      if (blockBloom == null) {
        blockBloom = new Bloom();
      }

      for (Log log : transactionInfo.getLogList()) {
        //log.address doesn't have "41" ahead
        Bloom bloom = Bloom.create(Hash.sha3(log.getAddress().toByteArray()));
        blockBloom.or(bloom);

        for (ByteString topic : log.getTopicsList()) {
          bloom = Bloom.create(Hash.sha3(topic.toByteArray()));
          blockBloom.or(bloom);
        }
      }
    }

    return blockBloom;
  }

  public void or(Bloom bloom) {
    for (int i = 0; i < data.length; ++i) {
      data[i] |= bloom.data[i];
    }
  }

  /**
   * (this || topicBloom) == this
   */
  public boolean matches(Bloom topicBloom) {
    Bloom copy = copy();
    copy.or(topicBloom);
    return this.equals(copy);
  }

  public byte[] getData() {
    return data;
  }

  public Bloom copy() {
    return new Bloom(Arrays.copyOf(getData(), getData().length));
  }

  @Override
  public String toString() {
    return ByteArray.toHexString(data);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Bloom bloom = (Bloom) o;

    return Arrays.equals(data, bloom.data);
  }

  @Override
  public int hashCode() {
    return data != null ? Arrays.hashCode(data) : 0;
  }
}
