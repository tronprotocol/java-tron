package org.tron.common.crypto;

import java.security.SignatureException;

public interface SignInterface {

  byte[] getPrivateKey();

  byte[] getPubKey();

  byte[] getAddress();

  String signHash(byte[] hash);

  byte[] signToAddress(byte[] messageHash, String signatureBase64) throws SignatureException;

  byte[] getNodeId();

  byte[] Base64toBytes (String signature);
}
