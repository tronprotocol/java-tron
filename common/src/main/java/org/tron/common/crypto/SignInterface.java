package org.tron.common.crypto;

public interface SignInterface {

  byte[] hash(byte[] message);

  byte[] getPrivateKey();

  byte[] getPubKeyFromPrivateKey(byte[] privateKey);

  byte[] getAddressFromPrivateKey(byte[] privateKey);

  byte[] getAddressFromPublicKey(byte[] publicKey);

  byte[] signMessage(byte[] message, byte[] privateKey);

  byte[] signHash(byte[] hash, byte[] privateKey);

  boolean signatureToAddress(byte[] message, String signatureBase64);

}
