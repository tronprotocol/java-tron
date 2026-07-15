package org.tron.common.crypto;

import static org.tron.core.Constant.MAX_PER_SIGN_LENGTH;
import static org.tron.core.Constant.PER_SIGN_LENGTH;

import java.security.SecureRandom;
import java.security.SignatureException;
import org.tron.common.crypto.ECKey.ECDSASignature;
import org.tron.common.crypto.sm2.SM2;
import org.tron.common.crypto.sm2.SM2.SM2Signature;

public class SignUtils {

  /**
   * Strict signature-length check for admission entry-points (RPC broadcast,
   * P2P transaction ingress, peer hello handshake). Accepts only sizes in
   * [{@link org.tron.core.Constant#PER_SIGN_LENGTH PER_SIGN_LENGTH},
   * {@link org.tron.core.Constant#MAX_PER_SIGN_LENGTH MAX_PER_SIGN_LENGTH}].
   *
   * <p>Consensus paths (e.g. {@code TransactionCapsule.checkWeight}) intentionally
   * keep the looser {@code size < 65} check to remain compatible with historical
   * on-chain signatures that carry trailing padding bytes; do not call this
   * helper from those paths.
   */
  public static boolean isValidLength(int size) {
    return size >= PER_SIGN_LENGTH && size <= MAX_PER_SIGN_LENGTH;
  }

  public static SignInterface getGeneratedRandomSign(
      SecureRandom secureRandom, boolean isECKeyCryptoEngine) {
    if (isECKeyCryptoEngine) {
      return new ECKey(secureRandom);
    }
    return new SM2(secureRandom);
  }

  public static SignInterface fromPrivate(byte[] privKeyBytes, boolean isECKeyCryptoEngine) {
    if (isECKeyCryptoEngine) {
      return ECKey.fromPrivate(privKeyBytes);
    }
    return SM2.fromPrivate(privKeyBytes);
  }

  public static byte[] signatureToAddress(
      byte[] messageHash, String signatureBase64, boolean isECKeyCryptoEngine)
      throws SignatureException {
    try {
      if (isECKeyCryptoEngine) {
        return ECKey.signatureToAddress(messageHash, signatureBase64);
      }
      return SM2.signatureToAddress(messageHash, signatureBase64);
    } catch (Exception e) {
      throw new SignatureException(e);
    }
  }

  public static SignatureInterface fromComponents(
      byte[] r, byte[] s, byte v, boolean isECKeyCryptoEngine) {
    if (isECKeyCryptoEngine) {
      return ECKey.ECDSASignature.fromComponents(r, s, v);
    }
    return SM2.SM2Signature.fromComponents(r, s, v);
  }

  public static byte[] signatureToAddress(
      byte[] messageHash, SignatureInterface signatureInterface, boolean isECKeyCryptoEngine)
      throws SignatureException {
    if (isECKeyCryptoEngine) {
      return ECKey.signatureToAddress(messageHash, (ECDSASignature) signatureInterface);
    }
    return SM2.signatureToAddress(messageHash, (SM2Signature) signatureInterface);
  }
}
