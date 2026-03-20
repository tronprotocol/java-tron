package org.tron.common.crypto;

import java.security.SecureRandom;
import java.security.SignatureException;

public class SignUtils {

  public static SignInterface getGeneratedRandomSign(SecureRandom secureRandom) {
    return new ECKey(secureRandom);
  }

  public static SignInterface fromPrivate(byte[] privKeyBytes) {
    return ECKey.fromPrivate(privKeyBytes);
  }

  public static byte[] signatureToAddress(byte[] messageHash, String signatureBase64)
      throws SignatureException {
    try {
      return ECKey.signatureToAddress(messageHash, signatureBase64);
    } catch (Exception e) {
      throw new SignatureException(e);
    }
  }

  public static SignatureInterface fromComponents(byte[] r, byte[] s, byte v) {
    return ECKey.ECDSASignature.fromComponents(r, s, v);
  }

  public static byte[] signatureToAddress(byte[] messageHash, SignatureInterface signatureInterface)
      throws SignatureException {
    return ECKey.signatureToAddress(messageHash, (ECKey.ECDSASignature) signatureInterface);
  }
}
