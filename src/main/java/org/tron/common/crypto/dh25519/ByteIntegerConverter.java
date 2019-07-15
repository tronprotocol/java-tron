package org.tron.common.crypto.dh25519;

public class ByteIntegerConverter {

  static long load_3(byte[] data, int offset)
  {
    long result;
    result = (data[offset + 0]&0xFFL);
    result |= (data[offset + 1]&0xFFL) << 8;
    result |= (data[offset + 2]&0xFFL) << 16;
    return result;
  }

  static long load_4(byte[] data, int offset)
  {
    long result;
    result = (data[offset + 0]&0xFFL);
    result |= (data[offset + 1]&0xFFL) << 8;
    result |= (data[offset + 2]&0xFFL) << 16;
    result |= (data[offset + 3]&0xFFL) << 24;
    return result;
  }
}
