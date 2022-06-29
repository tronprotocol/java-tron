package org.tron.core.db2.common;

import java.util.Arrays;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class WrappedByteArray implements Comparable<WrappedByteArray>{

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

  @Override
  public int compareTo(WrappedByteArray o) {
    // short circuit equal case
    if (this == o) {
      return 0;
    }
    // similar to Arrays.compare()
    byte[] d1 = this.getBytes();
    byte[] d2 = o.getBytes();

    if (d1 == null) {
      return d2 == null ? 0 : -1;
    }

    if (d2 == null) {
      return 1;
    }

    final int end1 = d1.length;
    final int end2 = d2.length;

    int cmp;
    for (int i = 0, j = 0; i < end1 && j < end2; i++, j++) {
      if ((cmp = Byte.compare(d1[i], d2[j])) != 0) {
        return cmp;
      }
    }
    return end1 - end2;
  }

}
