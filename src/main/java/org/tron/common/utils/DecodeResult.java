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

package org.tron.common.utils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.spongycastle.util.encoders.Hex;

@SuppressWarnings("serial")
public class DecodeResult implements Serializable {

  private int pos;
  private Object decoded;

  public DecodeResult(int pos, Object decoded) {
    this.pos = pos;
    this.decoded = decoded;
  }

  public int getPos() {
    return pos;
  }

  public Object getDecoded() {
    return decoded;
  }

  public String toString() {
    return asString(this.decoded);
  }

  private String asString(Object decoded) {
    if (decoded instanceof String) {
      return (String) decoded;
    } else if (decoded instanceof byte[]) {
      return Hex.toHexString((byte[]) decoded);
    } else if (decoded instanceof Object[]) {
      return Arrays.stream((Object[]) decoded)
              .map(this::asString)
              .collect(Collectors.joining());
    }
    throw new RuntimeException("Not a valid type. Should not occur");
  }
}
