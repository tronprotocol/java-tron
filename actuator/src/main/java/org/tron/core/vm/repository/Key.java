package org.tron.core.vm.repository;

import java.util.Arrays;
import org.apache.commons.lang3.ArrayUtils;

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
    return data != null ? ArrayUtils.hashCode(data) : 0;
  }
}
