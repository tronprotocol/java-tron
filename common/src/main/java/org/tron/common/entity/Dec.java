package org.tron.common.entity;

import java.math.BigInteger;
import java.util.Objects;

public final class Dec implements Comparable<Dec> {
  // number of decimal places
  public static final int precision = 18;
  // bytes required to represent the above precision
  // Ceiling[Log2[999 999 999 999 999 999]]
  public static final int decimalPrecisionBits = 60;
  private static final int maxBitLen = 256;
  private static final int maxDecBitLen = maxBitLen + decimalPrecisionBits;
  // max number of iterations in ApproxRoot function
  private static final int maxApproxRootIterations = 100;
  private static final BigInteger precisionReuse = BigInteger.TEN.pow(precision);
  private static final BigInteger fivePrecision = precisionReuse.divide(BigInteger.valueOf(2));
  private static final BigInteger zeroInt = BigInteger.ZERO;
  private static final BigInteger oneInt = BigInteger.ONE;
  private static final BigInteger tenInt = BigInteger.TEN;
  private static final BigInteger[] precisionMultipliers = new BigInteger[precision + 1];
  private static final String precisionErr
      = "Too much precision, maximum %s, provided %s";
  private static final String overflowErr = "Overflow, maxDecBitLen %s, bitLength %s";
  private static final String NIL = "nil";

  static {
    // Set precision multipliers
    for (int i = 0; i <= precision; i++) {
      precisionMultipliers[i] = calcPrecisionMultiplier(i);
    }
  }

  private final BigInteger i;
  private String s = NIL;

  private Dec(BigInteger val) {
    i = val;
  }

  private Dec(byte[] val) {
    i = new BigInteger(val);
  }


  private static BigInteger precisionInt() {
    return BigInteger.valueOf(precisionReuse.longValueExact());
  }

  public static Dec zeroDec() {
    return new Dec(zeroInt);
  }

  public static Dec oneDec() {
    return new Dec(precisionInt());
  }

  public static Dec smallestDec() {
    return new Dec(oneInt);
  }

  // calculate the precision multiplier
  private static BigInteger calcPrecisionMultiplier(int prec) {
    if (prec > precision) {
      throw new RuntimeException(String.format(precisionErr, precision, prec));
    }
    int zerosToAdd = precision - prec;
    BigInteger multiplier = tenInt.pow(zerosToAdd);
    return multiplier;
  }

  // get the precision multiplier, do not mutate result
  private static BigInteger precisionMultiplier(int prec) {
    if (prec > precision) {
      throw new RuntimeException(String.format(precisionErr, precision, prec));
    }
    return precisionMultipliers[prec];
  }

  // create a new Dec from byte[]
  public static Dec newDec(byte[] data) {
    return new Dec(data);
  }

  // create a decimal from an input decimal string.
// valid must come in the form:
//   (-) whole integers (.) decimal integers
// examples of acceptable input include:
//   -123.456
//   456.7890
//   345
//   -456789
//
// NOTE - An error will return if more decimal places
// are provided in the string than the constant Precision.
//
// CONTRACT - This function does not mutate the input str.
  public static Dec newDec(String str) {
    if (str == null || str.isEmpty()) {
      throw new RuntimeException("decimal string cannot be empty");
    }
    char[] chars = str.toCharArray();
    // first extract any negative symbol
    boolean neg = false;
    if (chars[0] == '-') {
      neg = true;
      str = str.substring(1);
    }

    if (str.isEmpty()) {
      throw new RuntimeException("decimal string cannot be empty");
    }

    String[] strs = str.split("\\.", -1);
    int lenDecs = 0;
    StringBuilder combinedStr = new StringBuilder(strs[0]);

    if (strs.length == 2) { // has a decimal place
      lenDecs = strs[1].length();
      if (lenDecs == 0 || combinedStr.length() == 0) {
        throw new RuntimeException("invalid decimal length");
      }
      combinedStr.append(strs[1]);
    } else if (strs.length > 2) {
      throw new RuntimeException("invalid decimal string");
    }

    if (lenDecs > precision) {
      throw new RuntimeException(String.format("invalid precision; max: %d, got: %d",
          precision, lenDecs));

    }

    // add some extra zero's to correct to the Precision factor
    int zerosToAdd = precision - lenDecs;
    for (int i = 0; i < zerosToAdd; i++) {
      combinedStr.append('0');
    }
    BigInteger combined = new BigInteger(combinedStr.toString(), 10); // base 10

    if (combined.bitLength() > maxBitLen) {
      throw new RuntimeException(String.format("decimal out of range; bitLen: got %d, max %d",
          combined.bitLength(), maxBitLen));
    }
    if (neg) {
      combined = combined.negate();
    }

    return new Dec(combined);
  }


  // create a new Dec from long assuming whole number
  public static Dec newDec(long i) {
    return newDecWithPrec(i, 0);
  }


  // create a new Dec from integer with decimal place at prec
// CONTRACT: prec <= precision
  public static Dec newDecWithPrec(long i, int prec) {
    return new Dec(BigInteger.valueOf(i).multiply(precisionMultiplier(prec)));
  }

  // create a new Dec from big integer assuming whole numbers
// CONTRACT: prec <= Precision
  public static Dec newDec(BigInteger i) {
    return newDecWithPrec(i, 0);
  }

  // create a new Dec from big integer assuming whole numbers
// CONTRACT: prec <= Precision
  public static Dec newDecWithPrec(BigInteger i, int prec) {
    return new Dec(i.multiply(precisionMultiplier(prec)));
  }

  // create a new Dec from int assuming whole numbers
// CONTRACT: prec <= Precision
  public static Dec newDec(int i) {
    return newDecWithPrec(i, 0);
  }


  // minimum decimal between two
  public static Dec minDec(Dec d1, Dec d2) {
    if (d1.lt(d2)) {
      return d1;
    }
    return d2;
  }

  // maximum decimal between two
  public static Dec maxDec(Dec d1, Dec d2) {
    if (d1.lt(d2)) {
      return d2;
    }
    return d1;
  }

  // create a new Dec from big integer with decimal place at prec
// CONTRACT: prec <= Precision
  public static Dec newDecWithPrec(int i, int prec) {
    return new Dec(BigInteger.valueOf(i).multiply(precisionMultiplier(prec)));
  }

  // is decimal nil
  public boolean isNil() {
    return this.i == null;
  }

  // is equal to zero
  public boolean isZero() {
    return (this.i).signum() == 0;
  }

  // is negative
  public boolean isNegative() {
    return (this.i).signum() == -1;
  }

  // is positive
  public boolean isPositive() {
    return (this.i).signum() == 1;
  }

  // equal decimals
  public boolean eq(Dec d2) {
    return (this.i).compareTo(d2.i) == 0;
  }

  // greater than
  public boolean gt(Dec d2) {
    return (this.i).compareTo(d2.i) > 0;
  }

  // greater than or equal
  public boolean gte(Dec d2) {
    return (this.i).compareTo(d2.i) >= 0;
  }

  // less than
  public boolean lt(Dec d2) {
    return (this.i).compareTo(d2.i) < 0;
  }

  // less than or equal
  public boolean lte(Dec d2) {
    return (this.i).compareTo(d2.i) <= 0;
  }

  // reverse the decimal sign
  public Dec neg() {
    return new Dec(this.i.negate());
  }

  // absolute value
  public Dec abs() {
    return new Dec(this.i.abs());
  }

  @Override
  public int compareTo(Dec d2) {
    return (this.i).compareTo(d2.i);
  }

  // BigInt returns a copy of the underlying BigInteger.
  public BigInteger bigInt() {
    if (this.isNil()) {
      return null;
    }
    return new BigInteger(this.i.toByteArray());
  }

  public byte[] toByteArray() {
    return this.i.toByteArray();
  }

  // addition
  public Dec add(Dec d2) {
    BigInteger res = this.i.add(d2.i);
    if (res.bitLength() > maxDecBitLen) {
      throw new RuntimeException(String.format(overflowErr, maxDecBitLen, res.bitLength()));
    }
    return new Dec(res);
  }

  // addition
  public Dec sub(Dec d2) {
    BigInteger res = this.i.subtract(d2.i);
    if (res.bitLength() > maxDecBitLen) {
      throw new RuntimeException(String.format(overflowErr, maxDecBitLen, res.bitLength()));
    }
    return new Dec(res);
  }

  // multiplication
  public Dec mul(Dec d2) {
    BigInteger mul = this.i.multiply(d2.i);
    BigInteger chopped = chopPrecisionAndRound(mul);

    if (chopped.bitLength() > maxDecBitLen) {
      throw new RuntimeException(String.format(overflowErr, maxDecBitLen, chopped.bitLength()));
    }
    return new Dec(chopped);
  }

  // multiplication truncate
  public Dec mulTruncate(Dec d2) {
    BigInteger mul = this.i.multiply(d2.i);
    BigInteger chopped = chopPrecisionAndTruncate(mul);

    if (chopped.bitLength() > maxDecBitLen) {
      throw new RuntimeException(String.format(overflowErr, maxDecBitLen, chopped.bitLength()));
    }
    return new Dec(chopped);
  }

  // multiplication
  public Dec mul(int i) {
    return mul(BigInteger.valueOf(i));
  }

  //multiplication with long
  public Dec mul(long l) {
    return mul(BigInteger.valueOf(l));
  }

  //multiplication with BigInteger
  public Dec mul(BigInteger v) {
    BigInteger mul = this.i.multiply(v);
    if (mul.bitLength() > maxDecBitLen) {
      throw new RuntimeException(String.format(overflowErr, maxDecBitLen, mul.bitLength()));
    }
    return new Dec(mul);
  }

  // quotient
  public Dec quo(Dec d2) {
    // multiply precision twice
    BigInteger mul = this.i.multiply(precisionReuse);
    mul = mul.multiply(precisionReuse);

    BigInteger quo = mul.divide(d2.i);
    BigInteger chopped = chopPrecisionAndRound(quo);

    if (chopped.bitLength() > maxDecBitLen) {
      throw new RuntimeException(String.format(overflowErr, maxDecBitLen, chopped.bitLength()));
    }
    return new Dec(chopped);
  }

  // quotient truncate
  public Dec quoTruncate(Dec d2) {
    // multiply precision twice
    BigInteger mul = this.i.multiply(precisionReuse);
    mul = mul.multiply(precisionReuse);

    BigInteger quo = mul.divide(d2.i);
    BigInteger chopped = chopPrecisionAndTruncate(quo);

    if (chopped.bitLength() > maxDecBitLen) {
      throw new RuntimeException(String.format(overflowErr, maxDecBitLen, chopped.bitLength()));
    }
    return new Dec(chopped);
  }

  // quotient, round up
  public Dec quoRoundUp(Dec d2) {
    // multiply precision twice
    BigInteger mul = this.i.multiply(precisionReuse);
    mul = mul.multiply(precisionReuse);

    BigInteger quo = mul.divide(d2.i);
    BigInteger chopped = chopPrecisionAndRoundUp(quo);

    if (chopped.bitLength() > maxDecBitLen) {
      throw new RuntimeException(String.format(overflowErr, maxDecBitLen, chopped.bitLength()));
    }
    return new Dec(chopped);
  }

  // quotient
  public Dec quo(int i) {
    return quo(BigInteger.valueOf(i));
  }

  // QuoInt64 - quotient with long
  public Dec quo(long l) {
    return quo(BigInteger.valueOf(l));
  }

  //  quotient with BigInteger
  public Dec quo(BigInteger i) {
    BigInteger mul = this.i.divide(i);
    return new Dec(mul);
  }

  // Power returns a the result of raising to a positive integer power
  public Dec power(BigInteger power) {
    if (power.signum() == -1) {
      throw new UnsupportedOperationException();
    }
    if (power.signum() == 0) {
      return oneDec();
    }
    Dec tmp = oneDec();
    Dec d =  new Dec(this.i);

    for (BigInteger i = power; i.compareTo(BigInteger.ONE) > 0;) {
      if (i.getLowestSetBit() == 0) {
        tmp = tmp.mul(d);
      }
      i = i.divide(BigInteger.valueOf(2));
      d = d.mul(d);
    }

    return d.mul(tmp);
  }

  // ApproxSqrt is a wrapper around ApproxRoot for the common special case
  // of finding the square root of a number. It returns -(sqrt(abs(d)) if input is negative.
  public  Dec approxSqrt()  {
    return approxRoot(2);
  }

  // ApproxRoot returns an approximate estimation of a Dec's positive real nth root
  // using Newton's method (where n is positive). The algorithm starts with some guess and
  // computes the sequence of improved guesses until an answer converges to an
  // approximate answer.  It returns `|d|.ApproxRoot() * -1` if input is negative.
  // A maximum number of 100 iterations is used a backup boundary condition for
  // cases where the answer never converges enough to satisfy the main condition.
  public  Dec approxRoot(long root ){
    if (isNegative()) {
      return mul(-1).approxRoot(root).mul(-1);
    }

    if (root == 1 || isZero() || this.eq(oneDec())) {
      return new Dec(this.i);
    }

    if (root == 0) {
      return Dec.oneDec();
    }

    BigInteger rootInt = BigInteger.valueOf(root);
    Dec guess = oneDec();
    Dec delta = oneDec();
    Dec smallestDec = smallestDec();

    for (int i = 0; delta.abs().gt(smallestDec) && i < maxApproxRootIterations; i++) {
      Dec prev = guess.power(BigInteger.valueOf(root - 1));
      if (prev.isZero()) {
        prev = smallestDec();
      }
      delta = quo(prev);
      delta = delta.sub(guess);
      delta = delta.quo(rootInt);
      guess = guess.add(delta);
    }
    return guess;
  }

  // Remove a Precision amount of rightmost digits and perform bankers rounding
  // on the remainder (gaussian rounding) on the digits which have been removed.
  //
  // Mutates the input. Use the non-mutative version if that is undesired
  private BigInteger chopPrecisionAndRound(BigInteger d) {
    // remove the negative and add it back when returning
    if (d.signum() == -1) {
      // make d positive, compute chopped value, and then un-mutate d
      d = d.negate();
      d = chopPrecisionAndRound(d);
      d = d.negate();
      return d;
    }

    // get the truncated quotient and remainder
    BigInteger[] res = d.divideAndRemainder(precisionReuse);
    BigInteger quo = res[0];
    BigInteger rem = res[1];

    if (rem.signum() == 0) { // remainder is zero
      return quo;
    }

    switch (rem.compareTo(fivePrecision)) {
      case -1:
        return quo;
      case 1:
        return quo.add(oneInt);
      default: // bankers rounding must take place
        // always round to an even number
        if (quo.getLowestSetBit() == 0) {
          return quo.add(oneInt);
        }
        return quo;
    }
  }

  private BigInteger chopPrecisionAndRoundUp(BigInteger d) {
    // remove the negative and add it back when returning
    if (d.signum() == -1) {
      // make d positive, compute chopped value, and then un-mutate d
      d = d.negate();
      // truncate since d is negative...
      d = chopPrecisionAndTruncate(d);
      d = d.negate();
      return d;
    }

    // get the truncated quotient and remainder
    BigInteger[] res = d.divideAndRemainder(precisionReuse);
    BigInteger quo = res[0];
    BigInteger rem = res[1];

    if (rem.signum() == 0) { // remainder is zero
      return quo;
    }

    return quo.add(oneInt);
  }

  private BigInteger chopPrecisionAndRoundNonMutative(BigInteger d) {
    BigInteger tmp = new BigInteger(d.toByteArray());
    return chopPrecisionAndRound(tmp);
  }

  // chopPrecisionAndTruncate is similar to chopPrecisionAndRound,
// but always rounds down. It does not mutate the input.
  private BigInteger chopPrecisionAndTruncate(BigInteger d) {
    return d.divide(precisionReuse);
  }


  //  rounds the decimal using bankers rounding
  public long roundLong() {
    BigInteger chopped = chopPrecisionAndRoundNonMutative(this.i);
    return chopped.longValueExact();
  }

  //  round the decimal using bankers rounding
  public int roundInt() {
    BigInteger chopped = chopPrecisionAndRoundNonMutative(this.i);
    return chopped.intValueExact();
  }

  //  round the decimal using bankers rounding
  public BigInteger roundBigInt() {
    BigInteger chopped = chopPrecisionAndRoundNonMutative(this.i);
    return chopped;
  }

  // TruncateInt64 truncates the decimals from the number and returns an int64
  public long truncateLong() {
    BigInteger chopped = chopPrecisionAndTruncate(this.i);
    return chopped.longValueExact();
  }

  //  truncates the decimals from the number and returns an Int
  public int truncateInt() {
    return chopPrecisionAndTruncate(this.i).intValueExact();
  }

  //  truncates the decimals from the number and returns an Int
  public BigInteger truncateBigInt() {
    return chopPrecisionAndTruncate(this.i);
  }

  //  truncates the decimals from the number and returns a Dec
  public Dec truncateDec() {
    return new Dec(chopPrecisionAndTruncate(this.i));
  }

  // Ceil returns the smallest integer value (as a decimal) that is greater than
// or equal to the given decimal.
  public Dec ceil() {
    BigInteger tmp = new BigInteger(this.i.toByteArray());

    // get the truncated quotient and remainder
    BigInteger[] res = tmp.divideAndRemainder(precisionReuse);
    BigInteger quo = res[0];
    BigInteger rem = res[1];

    // no need to round with a zero remainder regardless of sign
    if (rem.compareTo(zeroInt) == 0) {
      return newDec(quo);
    }

    if (rem.signum() == -1) {
      return newDec(quo);
    }
    return newDec(quo.add(oneInt));
  }

  // Double returns the double representation of a Dec.
  // Will return the error if the conversion failed.
  public double parseDouble() {
    return Double.parseDouble(this.toString());
  }

  // is integer, e.g. decimals are zero
  public boolean isInteger()  {
    return this.i.divide(precisionReuse).signum() == 0;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Dec dec = (Dec) o;
    return Objects.equals(i, dec.i);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(i);
  }

  @Override
  public String toString() {

    if (this.i == null) {
      return NIL;
    }

    if (!NIL.equals(s)) {
      return s;
    }

    boolean isNeg = this.isNegative();

    Dec tmp = new Dec(this.i);

    if (isNeg) {
      tmp = this.neg();
    }

    String bzInt = tmp.i.toString(10);// base 10

    int inputSize = bzInt.length();

    StringBuilder bzStr = new StringBuilder();

    // TODO: Remove trailing zeros
    // case 1, purely decimal
    if (inputSize <= precision) {
      // 0. prefix
      bzStr.append('0');
      bzStr.append('.');

      // set relevant digits to 0
      for (int i = 0; i < precision - inputSize; i++) {
        bzStr.append('0');
      }
      // set final digits
      bzStr.append(bzInt);
    } else {
      int decPointPlace = inputSize - precision;
      bzStr.append(bzInt, 0, decPointPlace);  // pre-decimal digits
      bzStr.append('.'); // decimal point
      bzStr.append(bzInt, decPointPlace, inputSize); // post-decimal digits
    }

    if (isNeg) {
      bzStr.insert(0, '-');
    }
    s = bzStr.toString().replaceAll("0+$", ""); // Remove trailing zeros
    if (s.endsWith(".")) {
      s = s.substring(0, s.length() - 1); // make 1. to 1
    }
    return s;
  }

}
