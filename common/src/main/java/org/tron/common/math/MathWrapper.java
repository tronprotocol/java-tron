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

  public static long addExact(long x, long y) {
    return Math.addExact(x, y);
  }

  public static int addExact(int x, int y) {
    return Math.addExact(x, y);
  }

  public static long floorDiv(long x, long y) {
    return Math.floorDiv(x, y);
  }

  public static int multiplyExact(int x, int y) {
    return Math.multiplyExact(x, y);
  }

  public static long multiplyExact(long x, long y) {
    return Math.multiplyExact(x, y);
  }

  public static long subtractExact(long x, long y) {
    return Math.subtractExact(x, y);
  }

  public static int min(int a, int b) {
    return Math.min(a, b);
  }

  public static long min(long a, long b) {
    return Math.min(a, b);
  }

  public static int max(int a, int b) {
    return Math.max(a, b);
  }

  public static long max(long a, long b) {
    return Math.max(a, b);
  }

  public static int round(float a) {
    return Math.round(a);
  }

  public static long round(double a) {
    return Math.round(a);
  }

  public static double ceil(double a) {
    return Math.ceil(a);
  }

  public static double signum(double a) {
    return Math.signum(a);
  }

  public static double random() {
    return Math.random();
  }

  public static long abs(long a) {
    return Math.abs(a);
  }
}
