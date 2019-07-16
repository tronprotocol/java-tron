package org.tron.common.crypto.digital;

public interface UnsignedNumber {

  public UnsignedNumber leftShift(int n);

  public UnsignedNumber rightShift(int n);

  public byte[] toByteArray();
}
