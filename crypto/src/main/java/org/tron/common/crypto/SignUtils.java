package org.tron.common.crypto;

import static org.tron.core.Constant.MAX_PER_SIGN_LENGTH;
import static org.tron.core.Constant.PER_SIGN_LENGTH;

import java.security.SecureRandom;
import java.security.SignatureException;

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
