package org.tron.common.zksnark;

public class ZksnarkUtils {

  public static void sort(byte[] bytes) {
    int len = bytes.length / 2;
    for (int i = 0; i < len; i++) {
      byte b = bytes[i];
      bytes[i] = bytes[bytes.length - i - 1];
      bytes[bytes.length - i - 1] = b;
    }
  }

}
