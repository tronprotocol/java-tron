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

  /**
   * Adds two integers together, checking for overflow.
   * @param x the first value.
   * @param y the second value.
   * @return the sum of {@code x} and {@code y}.
   * @throws ArithmeticException if the result overflows an int.
   */
  public static long addExact(long x, long y, boolean useStrictMath) {
    return useStrictMath ? StrictMathWrapper.addExact(x, y) : MathWrapper.addExact(x, y);
  }

  public static int addExact(int x, int y, boolean useStrictMath) {
    return useStrictMath ? StrictMathWrapper.addExact(x, y) : MathWrapper.addExact(x, y);
  }

  public static long floorDiv(long x, long y, boolean useStrictMath) {
    return useStrictMath ? StrictMathWrapper.floorDiv(x, y) : MathWrapper.floorDiv(x, y);
  }

  public static int multiplyExact(int x, int y,  boolean useStrictMath) {
    return useStrictMath ? StrictMathWrapper.multiplyExact(x, y) : MathWrapper.multiplyExact(x, y);
  }

  public static long multiplyExact(long x, long y,  boolean useStrictMath) {
    return useStrictMath ? StrictMathWrapper.multiplyExact(x, y) : MathWrapper.multiplyExact(x, y);
  }

  public static long subtractExact(long x, long y, boolean useStrictMath) {
    return useStrictMath ? StrictMathWrapper.subtractExact(x, y) : MathWrapper.subtractExact(x, y);
  }

  public static int min(int a, int b, boolean useStrictMath) {
    return useStrictMath ? StrictMathWrapper.min(a, b) : MathWrapper.min(a, b);
  }

  public static long min(long a, long b, boolean useStrictMath) {
    return useStrictMath ? StrictMathWrapper.min(a, b) : MathWrapper.min(a, b);
  }

  public static int max(int a, int b, boolean useStrictMath) {
    return useStrictMath ? StrictMathWrapper.max(a, b) : MathWrapper.max(a, b);
  }

  public static long max(long a, long b, boolean useStrictMath) {
    return useStrictMath ? StrictMathWrapper.max(a, b) : MathWrapper.max(a, b);
  }

  public static int round(float a, boolean useStrictMath) {
    return useStrictMath ? StrictMathWrapper.round(a) : MathWrapper.round(a);
  }

  public static long round(double a, boolean useStrictMath) {
    return useStrictMath ? StrictMathWrapper.round(a) : MathWrapper.round(a);
  }

  public static double ceil(double a, boolean useStrictMath) {
    return useStrictMath ? StrictMathWrapper.ceil(a) : MathWrapper.ceil(a);
  }

  public static double signum(double a, boolean useStrictMath) {
    return useStrictMath ? StrictMathWrapper.signum(a) : MathWrapper.signum(a);
  }

  public static double random(boolean useStrictMath) {
    return useStrictMath ? StrictMathWrapper.random() : MathWrapper.random();
  }

  public static long abs(long a, boolean useStrictMath) {
    return useStrictMath ? StrictMathWrapper.abs(a) : MathWrapper.abs(a);
  }

}
