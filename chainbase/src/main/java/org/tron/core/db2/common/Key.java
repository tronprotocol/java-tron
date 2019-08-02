package org.tron.core.db2.common;

import java.util.Arrays;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public final class Key {

  final private WrappedByteArray data;

  private Key(WrappedByteArray data) {
    this.data = data;
  }

  public static Key copyOf(byte[] bytes) {
    return new Key(WrappedByteArray.copyOf(bytes));
  }

  public static Key of(byte[] bytes) {
    return new Key(WrappedByteArray.of(bytes));
  }

  public byte[] getBytes() {
    byte[] key = data.getBytes();
    if (key == null) {
      return null;
    }

    return Arrays.copyOf(key, key.length);
  }
}
