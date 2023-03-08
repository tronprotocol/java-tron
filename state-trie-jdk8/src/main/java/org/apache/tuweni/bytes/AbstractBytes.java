/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.tuweni.bytes;

/**
 * An abstract {@link Bytes} value that provides implementations of {@link #equals(Object)}, {@link #hashCode()} and
 * {@link #toString()}.
 */
public abstract class AbstractBytes implements Bytes {

  static final String HEX_CODE_AS_STRING = "0123456789abcdef";
  static final char[] HEX_CODE = HEX_CODE_AS_STRING.toCharArray();

  private Integer hashCode;

  /**
   * Compare this value and the provided one for equality.
   *
   * <p>
   * Two {@link Bytes} values are equal is they have contain the exact same bytes.
   *
   * @param obj The object to test for equality with.
   * @return {@code true} if this value and {@code obj} are equal.
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof Bytes)) {
      return false;
    }

    Bytes other = (Bytes) obj;
    if (this.size() != other.size()) {
      return false;
    }

    for (int i = 0; i < size(); i++) {
      if (this.get(i) != other.get(i)) {
        return false;
      }
    }

    return true;
  }

  protected int computeHashcode() {
    int result = 1;
    for (int i = 0; i < size(); i++) {
      result = 31 * result + get(i);
    }
    return result;
  }

  @Override
  public int hashCode() {
    if (this.hashCode == null) {
      this.hashCode = computeHashcode();
    }
    return this.hashCode;
  }

  @Override
  public String toString() {
    return toHexString();
  }
}
