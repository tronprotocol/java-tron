package org.tron.common.crypto;

public interface SignInterface {

  SignInterface fromPrivKey(byte[] privateKey);

  SignInterface fromPubKey(byte[] publicKey);

  byte[] hash(byte[] message);

  byte[] getPrivateKey();

  byte[] getPubKeyFromPrivateKey();

  byte[] getAddressFromPrivateKey();

  byte[] getAddressFromPublicKey();

  byte[] signMessage(byte[] message);

  byte[] signHash(byte[] hash);

  boolean signatureToAddress(byte[] message, String signatureBase64);

}
