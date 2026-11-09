package org.tron.core.vm.repository;

import java.security.SecureRandom;
import java.util.Arrays;
import org.apache.commons.lang3.ArrayUtils;
import org.tron.core.vm.config.VMConfig;

public class Key {

  /**
   * data could not be null
   */
  private byte[] data = new byte[0];

  public Key(byte[] data) {
    if (data != null && data.length != 0) {
      this.data = new byte[data.length];
      System.arraycopy(data, 0, this.data, 0, data.length);
    }
  }

  private Key(Key key) {
    this.data = new byte[key.getData().length];
    System.arraycopy(key.getData(), 0, this.data, 0, this.data.length);
  }

  public static Key create(byte[] data) {
    return new Key(data);
  }

  public Key clone() {
    return new Key(this);
  }

  public byte[] getData() {
    return data;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Key key = (Key) o;
    return Arrays.equals(key.getData(), this.data);
  }

  @Override
  public int hashCode() {
    if (!VMConfig.allowTvmResourceOptimization()) {
      return data != null ? ArrayUtils.hashCode(data) : 0;
    }
    return optimizedHash(data, ResourceHashHolder.SEEDS, ResourceHashHolder.LENGTH_SEED);
  }

  private static class ResourceHashHolder {

    // Twenty 32-bit words cover production keys, including the variable-length reward keys.
    // Keep random initialization out of the pre-proposal path.
    private static final long[] SEEDS = new long[20];
    private static final long LENGTH_SEED;

    static {
      SecureRandom random = new SecureRandom();
      LENGTH_SEED = random.nextLong();
      for (int i = 0; i < SEEDS.length; i++) {
        SEEDS[i] = random.nextLong();
      }
    }
  }

  static int optimizedHash(byte[] bytes, long[] seeds, long lengthSeed) {
    if (bytes.length > seeds.length * 4) {
      // Preserve arbitrary-length inputs without reusing coefficients or ignoring the tail.
      return ArrayUtils.hashCode(bytes);
    }
    long hash = lengthSeed * bytes.length;
    for (int offset = 0, index = 0; offset < bytes.length; offset += 4, index++) {
      int length = bytes.length - offset;
      if (length > 4) {
        length = 4;
      }
      // Every unsigned 32-bit word, including the tail, has its own coefficient.
      long word = 0L;
      for (int i = 0; i < length; i++) {
        word |= ((long) bytes[offset + i] & 0xff) << (8 * i);
      }
      hash += word * seeds[index];
    }
    return (int) (hash >>> 32);
  }
}
