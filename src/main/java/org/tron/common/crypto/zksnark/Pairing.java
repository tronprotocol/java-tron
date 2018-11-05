package org.tron.common.crypto.zksnark;

import java.math.BigInteger;
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

  public static G1Point G1Point(String string) {
    String[] hexArray = string.split(",");
    if (hexArray.length != 2) {
      return null;
    }
    return new G1Point(new BigInteger(ByteArray.fromHexString(hexArray[0].trim())),
        new BigInteger(ByteArray.fromHexString(hexArray[1].trim())));
  }
}
