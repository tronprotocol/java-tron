package org.tron.common.math;

public class StrictMathWrapper {

  public static double pow(double a, double b) {
    return StrictMath.pow(a, b);
  }

  /**
   *  *** methods are same as {@link java.lang.Math} methods, guaranteed by the call start ***
   */

  /**
   * finally calls {@link java.lang.Math#addExact(long, long)}
   */

  public static long addExact(long x, long y) {
    return StrictMath.addExact(x, y);
  }

  /**
   * finally calls {@link java.lang.Math#addExact(int, int)}
   */

  public static int addExact(int x, int y) {
    return StrictMath.addExact(x, y);
  }

  /**
   * finally calls {@link java.lang.Math#subtractExact(long, long)}
   */

  public static long subtractExact(long x, long y) {
    return StrictMath.subtractExact(x, y);
  }

  /**
   * finally calls {@link java.lang.Math#floorMod(long, long)}
   */
  public static long multiplyExact(long x, long y) {
    return StrictMath.multiplyExact(x, y);
  }

  public static long multiplyExact(long x, int y) {
    return multiplyExact(x, (long) y);
  }

  public static int multiplyExact(int x, int y) {
    return StrictMath.multiplyExact(x, y);
  }

  /**
   * finally calls {@link java.lang.Math#floorDiv(long, long)}
   */
  public static long floorDiv(long x, long y) {
    return StrictMath.floorDiv(x, y);
  }

  public static long floorDiv(long x, int y) {
    return floorDiv(x, (long) y);
  }

  /**
   * finally calls {@link java.lang.Math#min(int, int)}
   */
  public static int min(int a, int b) {
    return StrictMath.min(a, b);
  }

  /**
   * finally calls {@link java.lang.Math#min(long, long)}
   */
  public static long min(long a, long b) {
    return StrictMath.min(a, b);
  }

  /**
   * finally calls {@link java.lang.Math#max(int, int)}
   */
  public static int max(int a, int b) {
    return StrictMath.max(a, b);
  }

  /**
   * finally calls {@link java.lang.Math#max(long, long)}
   */
  public static long max(long a, long b) {
    return StrictMath.max(a, b);
  }

  /**
   * finally calls {@link java.lang.Math#round(float)}
   */
  public static int round(float a) {
    return StrictMath.round(a);
  }

  /**
   * finally calls {@link java.lang.Math#round(double)}
   */
  public static long round(double a) {
    return StrictMath.round(a);
  }

  /**
   * finally calls {@link java.lang.Math#signum(double)}
   */
  public static double signum(double d) {
    return StrictMath.signum(d);
  }

  /**
   * finally calls {@link java.lang.Math#signum(float)}
   */
  public static long abs(long a) {
    return StrictMath.abs(a);
  }

  /**
   *  *** methods are same as {@link java.lang.Math} methods, guaranteed by the call end ***
   */

  /**
   *  *** methods are same as {@link java.lang.Math} methods by mathematical  algorithms***
   * /


   /**
   * mathematical integer: ceil(i) = floor(i) = i
   * @return the smallest (closest to negative infinity) double value that is greater
   * than or equal to the argument and is equal to a mathematical integer.
   */
  public static double ceil(double a) {
    return StrictMath.ceil(a);
  }

  /**
   * *** methods are no matters  ***
   */
  public static double random() {
    return StrictMath.random();
  }

}
