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
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tron.common.utils;

import com.google.common.primitives.UnsignedBytes;

/**
 * Utility code to do optimized byte-array comparison.
 * This is borrowed and slightly modified from Guava's {@link UnsignedBytes}
 * class to be able to compare arrays that start at non-zero offsets.
 */
@SuppressWarnings("restriction")
public abstract class FastByteComparisons {

  public static boolean isEqual(byte[] b1, byte[] b2) {
    return b1.length == b2.length && compareTo(b1, 0, b1.length, b2, 0,
        b2.length) == 0;
  }

  /**
   * Lexicographically compare two byte arrays.
   *
   * @param b1 buffer1
   * @param s1 offset1
   * @param l1 length1
   * @param b2 buffer2
   * @param s2 offset2
   * @param l2 length2
   * @return int
   */
  public static int compareTo(byte[] b1, int s1, int l1, byte[] b2, int s2,
      int l2) {
    return LexicographicalComparerHolder.BEST_COMPARER.compareTo(
        b1, s1, l1, b2, s2, l2);
  }

  private static Comparer<byte[]> lexicographicalComparerJavaImpl() {
    return LexicographicalComparerHolder.PureJavaComparer.INSTANCE;
  }

  private interface Comparer<T> {
    int compareTo(T buffer1, int offset1, int length1,
        T buffer2, int offset2, int length2);
  }

  /**
   * <p>Uses reflection to gracefully fall back to the Java implementation if
   * {@code Unsafe} isn't available.
   */
  private static class LexicographicalComparerHolder {
    static final String UNSAFE_COMPARER_NAME =
        LexicographicalComparerHolder.class.getName() +
            "$UnsafeComparer";

    static final Comparer<byte[]> BEST_COMPARER = getBestComparer();

    /**
     * Returns the Unsafe-using Comparer, or falls back to the pure-Java
     * implementation if unable to do so.
     */
    static Comparer<byte[]> getBestComparer() {
      try {
        Class<?> theClass = Class.forName(UNSAFE_COMPARER_NAME);

        // yes, UnsafeComparer does implement Comparer<byte[]>
        @SuppressWarnings("unchecked")
        Comparer<byte[]> comparer =
            (Comparer<byte[]>) theClass.getEnumConstants()[0];
        return comparer;
      } catch (Throwable t) { // ensure we really catch *everything*
        return lexicographicalComparerJavaImpl();
      }
    }

    private enum PureJavaComparer implements Comparer<byte[]> {
      INSTANCE;

      @Override
      public int compareTo(byte[] buffer1, int offset1, int length1,
          byte[] buffer2, int offset2, int length2) {
        // Short circuit equal case
        if (buffer1 == buffer2 &&
            offset1 == offset2 &&
            length1 == length2) {
          return 0;
        }
        int end1 = offset1 + length1;
        int end2 = offset2 + length2;
        for (int i = offset1, j = offset2; i < end1 && j < end2; i++,
            j++) {
          int a = (buffer1[i] & 0xff);
          int b = (buffer2[j] & 0xff);
          if (a != b) {
            return a - b;
          }
        }
        return length1 - length2;
      }
    }


  }
}
