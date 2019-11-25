package org.tron.common.crypto;

import java.security.SignatureException;

public interface SignInterface {

  byte[] hash(byte[] message);

  byte[] getPrivateKey();

  byte[] getPubKey();

  byte[] getAddress();

  String sign(byte[] hash);

  byte[] signatureToAddress(byte[] messageHash, String signatureBase64) throws SignatureException;

}
