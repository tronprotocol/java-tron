/*
 * Copyright 2014 Andreas Schildbach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tron.core;

import com.google.common.primitives.Longs;
import java.io.Serializable;

/**
 * Represents a monetary Bitcoin value. This class is immutable.
 */
public final class Coin implements Monetary, Comparable<Coin>, Serializable {

  /**
   * Number of decimals for one Bitcoin. This constant is useful for quick adapting to other coins because a lot of
   * constants derive from it.
   */
  public static final int SMALLEST_UNIT_EXPONENT = 8;

  public final long value;

  private Coin(long satoshis) {
    this.value = satoshis;
  }

  public static Coin valueOf(final long satoshis) {
    return new Coin(satoshis);
  }

  @Override
  public int compareTo(final Coin other) {
    return Longs.compare(this.value, other.value);
  }

  @Override
  public int smallestUnitExponent() {
    return SMALLEST_UNIT_EXPONENT;
  }

  @Override
  public long getValue() {
    return value;
  }

  @Override
  public int signum() {
    if (this.value == 0) {
      return 0;
    } else {
      return this.value < 0 ? -1 : 1;
    }

  }
}
