package org.tron.common.math;

import com.google.common.primitives.Bytes;
import java.nio.ByteBuffer;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.context.GlobalContext;
import org.tron.core.store.MathStore;
import org.tron.core.store.StrictMathStore;

/**
 * This class is deprecated and should not be used in new code,
 * for cross-platform consistency, please use {@link StrictMathWrapper} instead,
 * especially for floating-point calculations.
 */
@Deprecated
@Component
@Slf4j(topic = "math")
public class Maths {

  private static Optional<MathStore> mathStore = Optional.empty();
  private static Optional<StrictMathStore> strictMathStore = Optional.empty();

  @Autowired
  public Maths(@Autowired MathStore mathStore, @Autowired StrictMathStore strictMathStore) {
    Maths.mathStore = Optional.ofNullable(mathStore);
    Maths.strictMathStore = Optional.ofNullable(strictMathStore);
  }

  private enum Op {

    POW((byte) 0x01);

    private final byte code;

    Op(byte code) {
      this.code = code;
    }
  }

  /**
   * Returns the value of the first argument raised to the power of the second argument.
   * @param a the base.
   * @param b the exponent.
   * @return the value {@code a}<sup>{@code b}</sup>.
   */
  public static double pow(double a, double b, boolean useStrictMath) {
    double result = MathWrapper.pow(a, b);
    double strictResult = StrictMathWrapper.pow(a, b);
    if (useStrictMath) {
      return strictResult;
    }
    final boolean isNoStrict = Double.compare(result, strictResult) != 0;
    Optional<Long> header = GlobalContext.getHeader();
    header.ifPresent(h -> {
      byte[] key = Bytes.concat(longToBytes(h), new byte[]{Op.POW.code},
          doubleToBytes(a), doubleToBytes(b));
      if (isNoStrict) {
        logger.info("{}\t{}\t{}\t{}\t{}\t{}", h, Op.POW.code, doubleToHex(a), doubleToHex(b),
            doubleToHex(result), doubleToHex(strictResult));
      }
      mathStore.ifPresent(s -> s.put(key, doubleToBytes(result)));
      strictMathStore.ifPresent(s -> s.put(key, doubleToBytes(strictResult)));
    });
    return result;
  }

  static String doubleToHex(double input) {
    // Convert the starting value to the equivalent value in a long
    long doubleAsLong = Double.doubleToRawLongBits(input);
    // and then convert the long to a hex string
    return Long.toHexString(doubleAsLong);
  }

  private static byte[] doubleToBytes(double value) {
    ByteBuffer buffer = ByteBuffer.allocate(Double.BYTES);
    buffer.putDouble(value);
    return buffer.array();
  }

  private static byte[] longToBytes(long value) {
    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
    buffer.putLong(value);
    return buffer.array();
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
