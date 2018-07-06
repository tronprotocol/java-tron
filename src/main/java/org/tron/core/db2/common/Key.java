package org.tron.core.db2.common;

import lombok.EqualsAndHashCode;
import org.tron.core.db.common.WrappedByteArray;

@EqualsAndHashCode
public final class Key {

  final private WrappedByteArray data;

  private Key(WrappedByteArray data) {
    this.data = data;
  }

  public static Key of(byte[] bytes) {
    return new Key(WrappedByteArray.of(bytes));
  }

  public byte[] getBytes() {
    return data.getBytes();
  }
}
