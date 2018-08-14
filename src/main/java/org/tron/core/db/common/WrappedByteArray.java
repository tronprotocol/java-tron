package org.tron.core.db.common;

import lombok.Getter;
import lombok.Value;

import java.util.Arrays;

@Value(staticConstructor = "of")
public final class WrappedByteArray {

  @Getter
  private byte[] bytes;

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
