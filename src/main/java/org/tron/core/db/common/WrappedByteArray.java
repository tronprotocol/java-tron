package org.tron.core.db.common;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.Arrays;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class WrappedByteArray {

  @Getter
  private byte[] bytes;

  public static WrappedByteArray of(byte[] bytes) {
    return new WrappedByteArray(bytes);
  }

  public static WrappedByteArray copyOf(byte[] bytes) {
    byte[] value = null;
    if (bytes != null) {
      value = Arrays.copyOf(bytes, bytes.length);
    }

    return new WrappedByteArray(value);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WrappedByteArray byteArray = (WrappedByteArray) o;
    return Arrays.equals(bytes, byteArray.bytes);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(bytes);
  }
}
