package org.tron.core.db2.common;

import java.util.Arrays;
import lombok.EqualsAndHashCode;
import org.tron.core.db.common.WrappedByteArray;

@EqualsAndHashCode
public final class Key {

  final private WrappedByteArray data;

  private Key(WrappedByteArray data) {
    this.data = data;
  }

  public static Key of(byte[] bytes) {
    byte[] key = null;
    if (bytes != null) {
      key = Arrays.copyOf(bytes, bytes.length);
    }
    return new Key(WrappedByteArray.of(key));
  }

  public byte[] getBytes() {
    byte[] key = data.getBytes();
    if (key == null) {
      return null;
    }

    return Arrays.copyOf(key, key.length);
  }
}
