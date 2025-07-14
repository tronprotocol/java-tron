package org.tron.common.math;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * This class is deprecated and should not be used in new code,
 * for cross-platform consistency, please use {@link StrictMathWrapper} instead,
 * especially for floating-point calculations.
 */
@Deprecated
public class MathWrapper {

  private static final Map<PowData, Double> powData = Collections.synchronizedMap(new HashMap<>());
  private static final String EXPONENT = "3f40624dd2f1a9fc"; // 1/2000 = 0.0005

  public static double pow(double a, double b) {
    double strictResult = StrictMath.pow(a, b);
    return powData.getOrDefault(new PowData(a, b), strictResult);
  }

  /**
   * This static block is used to initialize the data map.
   */
  static {
    // init main-net pow data start
    addPowData("3ff0192278704be3", EXPONENT, "3ff000033518c576"); //  4137160(block)
    addPowData("3ff000002fc6a33f", EXPONENT, "3ff0000000061d86"); //  4065476
    addPowData("3ff00314b1e73ecf", EXPONENT, "3ff0000064ea3ef8"); //  4071538
    addPowData("3ff0068cd52978ae", EXPONENT, "3ff00000d676966c"); //  4109544
    addPowData("3ff0032fda05447d", EXPONENT, "3ff0000068636fe0"); //  4123826
    addPowData("3ff00051c09cc796", EXPONENT, "3ff000000a76c20e"); //  4166806
    addPowData("3ff00bef8115b65d", EXPONENT, "3ff0000186893de0"); //  4225778
    addPowData("3ff009b0b2616930", EXPONENT, "3ff000013d27849e"); //  4251796
    addPowData("3ff00364ba163146", EXPONENT, "3ff000006f26a9dc"); //  4257157
    addPowData("3ff019be4095d6ae", EXPONENT, "3ff0000348e9f02a"); //  4260583
    addPowData("3ff0123e52985644", EXPONENT, "3ff0000254797fd0"); //  4367125
    addPowData("3ff0126d052860e2", EXPONENT, "3ff000025a6cde26"); //  4402197
    addPowData("3ff0001632cccf1b", EXPONENT, "3ff0000002d76406"); //  4405788
    addPowData("3ff0000965922b01", EXPONENT, "3ff000000133e966"); //  4490332
    addPowData("3ff00005c7692d61", EXPONENT, "3ff0000000bd5d34"); //  4499056
    addPowData("3ff015cba20ec276", EXPONENT, "3ff00002c84cef0e"); //  4518035
    addPowData("3ff00002f453d343", EXPONENT, "3ff000000060cf4e"); //  4533215
    addPowData("3ff006ea73f88946", EXPONENT, "3ff00000e26d4ea2"); //  4647814
    addPowData("3ff00a3632db72be", EXPONENT, "3ff000014e3382a6"); //  4766695
    addPowData("3ff000c0e8df0274", EXPONENT, "3ff0000018b0aeb2"); //  4771494
    addPowData("3ff00015c8f06afe", EXPONENT, "3ff0000002c9d73e"); //  4793587
    addPowData("3ff00068def18101", EXPONENT, "3ff000000d6c3cac"); //  4801947
    addPowData("3ff01349f3ac164b", EXPONENT, "3ff000027693328a"); //  4916843
    addPowData("3ff00e86a7859088", EXPONENT, "3ff00001db256a52"); //  4924111
    addPowData("3ff00000c2a51ab7", EXPONENT, "3ff000000018ea20"); //  5098864
    addPowData("3ff020fb74e9f170", EXPONENT, "3ff00004346fbfa2"); //  5133963
    addPowData("3ff00001ce277ce7", EXPONENT, "3ff00000003b27dc"); //  5139389
    addPowData("3ff005468a327822", EXPONENT, "3ff00000acc20750"); //  5151258
    addPowData("3ff00006666f30ff", EXPONENT, "3ff0000000d1b80e"); //  5185021
    addPowData("3ff000045a0b2035", EXPONENT, "3ff00000008e98e6"); //  5295829
    addPowData("3ff00e00380e10d7", EXPONENT, "3ff00001c9ff83c8"); //  5380897
    addPowData("3ff00c15de2b0d5e", EXPONENT, "3ff000018b6eaab6"); //  5400886
    addPowData("3ff00042afe6956a", EXPONENT, "3ff0000008892244"); //  5864127
    addPowData("3ff0005b7357c2d4", EXPONENT, "3ff000000bb48572"); //  6167339
    addPowData("3ff00033d5ab51c8", EXPONENT, "3ff0000006a279c8"); //  6240974
    addPowData("3ff0000046d74585", EXPONENT, "3ff0000000091150"); //  6279093
    addPowData("3ff0010403f34767", EXPONENT, "3ff0000021472146"); //  6428736
    addPowData("3ff00496fe59bc98", EXPONENT, "3ff000009650a4ca"); //  6432355,6493373
    addPowData("3ff0012e43815868", EXPONENT, "3ff0000026af266e"); //  6555029
    addPowData("3ff00021f6080e3c", EXPONENT, "3ff000000458d16a"); //  7092933
    addPowData("3ff000489c0f28bd", EXPONENT, "3ff00000094b3072"); //  7112412
    addPowData("3ff00009d3df2e9c", EXPONENT, "3ff00000014207b4"); //  7675535
    addPowData("3ff000def05fa9c8", EXPONENT, "3ff000001c887cdc"); //  7860324
    addPowData("3ff0013bca543227", EXPONENT, "3ff00000286a42d2"); //  8292427
    addPowData("3ff0021a2f14a0ee", EXPONENT, "3ff0000044deb040"); //  8517311
    addPowData("3ff0002cc166be3c", EXPONENT, "3ff0000005ba841e"); //  8763101
    addPowData("3ff0000cc84e613f", EXPONENT, "3ff0000001a2da46"); //  9269124
    addPowData("3ff000057b83c83f", EXPONENT, "3ff0000000b3a640"); //  9631452
    // init main-net pow data end
    // add pow data
  }

  private static void addPowData(String a, String b, String ret) {
    powData.put(new PowData(hexToDouble(a), hexToDouble(b)), hexToDouble(ret));
  }

  private static double hexToDouble(String input) {
    // Convert the hex string to a long
    long hexAsLong = Long.parseLong(input, 16);
    // and then convert the long to a double
    return Double.longBitsToDouble(hexAsLong);
  }

  private static class PowData {
    final double a;
    final double b;

    public PowData(double a, double b) {
      this.a = a;
      this.b = b;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      PowData powData = (PowData) o;
      return Double.compare(powData.a, a) == 0 && Double.compare(powData.b, b) == 0;
    }

    @Override
    public int hashCode() {
      return Objects.hash(a, b);
    }
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
