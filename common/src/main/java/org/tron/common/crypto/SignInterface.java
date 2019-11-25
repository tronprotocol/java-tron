package org.tron.common.crypto;

public interface SignInterface {
  
  byte[] hash(byte[] message);

  byte[] getPrivateKey();

  byte[] getPubKey();

  byte[] getAddress();

  String sign(byte[] hash);

  byte[] signatureToAddress(byte[] message, String signatureBase64);

}
