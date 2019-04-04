package org.tron.common.storage;

import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;

public class Key {
  /**
   * data could not be null
   */
  private byte[] data = new byte[0];

  /**
   *
   * @param data
   */
  public Key(byte[] data) {
    if (data != null && data.length != 0) {
      this.data = new byte[data.length];
      System.arraycopy(data, 0, this.data, 0, data.length);
    }
  }

  /**
   *
   * @param key
   */
  private Key(Key key) {
    this.data = new byte[key.getData().length];
    System.arraycopy(key.getData(), 0, this.data, 0, this.data.length);
  }

  /**
   *
   * @return
   */
  public Key clone() {
    return new Key(this);
  }

  /**
   *
   * @return
   */
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
    boolean result = false;
    if (Arrays.equals(key.getData(), this.data)) {
      result = true;
    }
    return result;
  }

  @Override
  public int hashCode() {
    return data != null ? ArrayUtils.hashCode(data) : 0;
  }

  /**
   *
   * @param data
   * @return
   */
  public static Key create(byte[] data) {
    return new Key(data);
  }
}
