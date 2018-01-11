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
