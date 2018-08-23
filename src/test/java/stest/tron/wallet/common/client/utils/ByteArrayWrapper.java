package stest.tron.wallet.common.client.utils;
/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.Serializable;
import java.util.Arrays;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.utils.FastByteComparisons;

public class ByteArrayWrapper implements Comparable<ByteArrayWrapper>, Serializable {

  private final byte[] data;
  private int hashCode = 0;

  /**
   * constructor.
   */
  public ByteArrayWrapper(byte[] data) {
    if (data == null) {
      throw new NullPointerException("Data must not be null");
    }
    this.data = data;
    this.hashCode = Arrays.hashCode(data);
  }


  /**
   * equals Objects.
   */
  public boolean equals(Object other) {
    if (other == null || this.getClass() != other.getClass()) {
      return false;
    }
    byte[] otherData = ((ByteArrayWrapper) other).getData();
    return FastByteComparisons.compareTo(
        data, 0, data.length,
        otherData, 0, otherData.length) == 0;
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  @Override
  public int compareTo(ByteArrayWrapper o) {
    return FastByteComparisons.compareTo(
        data, 0, data.length,
        o.getData(), 0, o.getData().length);
  }

  public byte[] getData() {
    return data;
  }

  @Override
  public String toString() {
    return Hex.toHexString(data);
  }
}
