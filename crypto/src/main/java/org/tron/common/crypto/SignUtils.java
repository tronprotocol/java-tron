package org.tron.common.crypto;

import static org.tron.core.Constant.PER_SIGN_LENGTH;

import java.security.SecureRandom;
import java.security.SignatureException;
import org.tron.common.crypto.ECKey.ECDSASignature;

public class SignUtils {

  /**
   * Strict signature-length check for admission entry-points (RPC broadcast,
   * P2P transaction ingress, peer hello handshake). Accepts exactly
   * {@link org.tron.core.Constant#PER_SIGN_LENGTH PER_SIGN_LENGTH} bytes.
   *
   * <p>Consensus paths use the governance-controlled strict-validation flag. Their
   * legacy behavior remains compatible with historical on-chain signatures that
   * carry trailing padding bytes.
   */
  public static boolean isValidLength(int size) {
    return size == PER_SIGN_LENGTH;
  }

  public static SignInterface getGeneratedRandomSign(SecureRandom secureRandom) {
    return new ECKey(secureRandom);
  }

  public static SignInterface fromPrivate(byte[] privKeyBytes) {
    return ECKey.fromPrivate(privKeyBytes);
  }

  public static byte[] signatureToAddress(byte[] messageHash, String signatureBase64)
      throws SignatureException {
    return signatureToAddress(messageHash, signatureBase64, false);
  }

  public static byte[] signatureToAddress(
      byte[] messageHash, String signatureBase64, boolean strictEcdsaValidation)
      throws SignatureException {
    try {
      return ECKey.signatureToAddress(messageHash, signatureBase64, strictEcdsaValidation);
    } catch (Exception e) {
      throw new SignatureException(e);
    }
  }

  public static ECDSASignature fromComponents(byte[] r, byte[] s, byte v) {
    return ECDSASignature.fromComponents(r, s, v);
  }

  public static byte[] signatureToAddress(byte[] messageHash, ECDSASignature signature)
      throws SignatureException {
    return signatureToAddress(messageHash, signature, false);
  }

  public static byte[] signatureToAddress(
      byte[] messageHash, ECDSASignature signature, boolean strictEcdsaValidation)
      throws SignatureException {
    try {
      return ECKey.signatureToAddress(messageHash, signature, strictEcdsaValidation);
    } catch (RuntimeException e) {
      throw new SignatureException(e);
    }
  }
}
