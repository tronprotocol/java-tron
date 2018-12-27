package org.tron.common.crypto.digital;

import java.math.BigInteger;
import org.tron.common.utils.ByteUtil;

public class UnsignedLong implements UnsignedNumber {

  private BigInteger value;

  public UnsignedLong(Long v) throws Exception {
    if (v < 0) {
      throw new Exception("Vaule is error!");
    }
    value = BigInteger.valueOf(v);
  }

  public UnsignedLong(byte[] values) throws Exception {
    if (values.length > 8) {
      throw new Exception("length is error!");
    }
    values = ByteUtil.merge(new byte[]{0}, values);
    value = new BigInteger(values);
  }

  public BigInteger multiply(UnsignedLong other) {
    return null;
  }

  public BigInteger add(UnsignedLong other) {
    return null;
  }

  public UnsignedLong[] one2two() {
    return null;
  }

  public static UnsignedLong[] one2two(BigInteger data) {
    return null;
  }

  public UnsignedLong leftShift(int n) {
    return null;
  }

  public UnsignedLong rightShift(int n) {
    return null;
  }

  public byte[] toByteArray() {
    return null;
  }
}
