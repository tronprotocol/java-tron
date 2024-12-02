package org.tron.common.math;

/**
 * This class is deprecated and should not be used in new code,
 * for cross-platform consistency, please use {@link StrictMathWrapper} instead,
 * especially for floating-point calculations.
 */
@Deprecated
public class Maths {

  /**
   * Returns the value of the first argument raised to the power of the second argument.
   * @param a the base.
   * @param b the exponent.
   * @return the value {@code a}<sup>{@code b}</sup>.
   */
  public static double pow(double a, double b, boolean useStrictMath) {
    return useStrictMath ? StrictMathWrapper.pow(a, b) : MathWrapper.pow(a, b);
  }
}
