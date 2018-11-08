package org.tron.common.crypto.zksnark;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import org.tron.common.utils.ByteArray;

public class Pairing {

  public static G2Point G2Point(String string) {
    string = string.replace("[", "");
    string = string.replace("]", "");
    String[] hexArray = string.split(",");
    if (hexArray.length != 4) {
      return null;
    }
    return new G2Point(new BigInteger(ByteArray.fromHexString(hexArray[0].trim())),
        new BigInteger(ByteArray.fromHexString(hexArray[1].trim())),
        new BigInteger(ByteArray.fromHexString(hexArray[2].trim())),
        new BigInteger(ByteArray.fromHexString(hexArray[3].trim())));
  }

  public static G2Point G2Point(byte[] bytes) throws IOException {
    if (bytes.length != 128){
      throw new IOException();
    }
    byte[] x1 = Arrays.copyOfRange(bytes, 0, 32);
    byte[] x2 = Arrays.copyOfRange(bytes, 32, 64);
    byte[] y1 = Arrays.copyOfRange(bytes, 64, 96);
    byte[] y2 = Arrays.copyOfRange(bytes, 96, 128);
    return new G2Point(x1, x2, y1, y2);
  }

  public static G1Point G1Point(String string) {
    String[] hexArray = string.split(",");
    if (hexArray.length != 2) {
      return null;
    }
    return new G1Point(new BigInteger(ByteArray.fromHexString(hexArray[0].trim())),
        new BigInteger(ByteArray.fromHexString(hexArray[1].trim())));
  }

  public static void sort(byte[] bytes){
    int len = bytes.length/2;
    for (int i = 0; i < len; i++){
      byte b = bytes[i];
      bytes[i] = bytes[bytes.length - i -1];
      bytes[bytes.length - i -1] = b;
    }
  }

  public static G1Point G1Point(byte[] bytes) throws IOException {
    if (bytes.length != 64){
      throw new IOException();
    }
    byte[] x = Arrays.copyOfRange(bytes, 0,32);
    sort(x);
    byte[] y = Arrays.copyOfRange(bytes, 32,64);
    sort(y);
    return new G1Point(x,y);
  }

}
