package org.tron.common.math;

/**
 * This class is deprecated and should not be used in new code,
 * for cross-platform consistency, please use {@link StrictMathWrapper} instead,
 * especially for floating-point calculations.
 */
@Deprecated
public class MathWrapper {

  public static double pow(double a, double b) {
    return Math.pow(a, b);
  }
}
