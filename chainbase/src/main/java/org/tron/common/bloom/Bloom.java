package org.tron.common.bloom;

import java.util.Arrays;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.TransactionRetCapsule;

public class Bloom {

  public static final int BLOOM_BIT_SIZE = BloomUtils.BLOOM_BIT_SIZE;
  public static final int BLOOM_BYTE_SIZE = BloomUtils.BLOOM_BYTE_SIZE;
  private static final int ENSURE_BYTE = 255;
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
    return new Bloom(BloomUtils.create(toBloom));
  }

  public static Bloom createBloom(TransactionRetCapsule transactionRetCapsule) {
    if (transactionRetCapsule == null) {
      return null;
    }
    byte[] bloom = BloomUtils.createBloom(transactionRetCapsule.getInstance());
    return bloom == null ? null : new Bloom(bloom);
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
