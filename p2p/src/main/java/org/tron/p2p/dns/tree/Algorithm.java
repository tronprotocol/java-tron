package org.tron.p2p.dns.tree;


import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.Base64;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.ec.CustomNamedCurves;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.Base32;
import org.tron.p2p.utils.ByteArray;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Sign;
import org.web3j.crypto.Sign.SignatureData;

public class Algorithm {

  private static final int truncateLength = 26;
  public static final String padding = "=";

  /**
   * return compress public key with hex
   */
  public static String compressPubKey(BigInteger pubKey) {
    String pubKeyYPrefix = pubKey.testBit(0) ? "03" : "02";
    String pubKeyHex = pubKey.toString(16);
    String pubKeyX = pubKeyHex.substring(0, 64);
    String hexPub = pubKeyYPrefix + pubKeyX;
    return hexPub;
  }

  public static String decompressPubKey(String hexPubKey) {
    X9ECParameters CURVE_PARAMS = CustomNamedCurves.getByName("secp256k1");
    ECDomainParameters CURVE =
        new ECDomainParameters(
            CURVE_PARAMS.getCurve(),
            CURVE_PARAMS.getG(),
            CURVE_PARAMS.getN(),
            CURVE_PARAMS.getH());
    byte[] pubKey = ByteArray.fromHexString(hexPubKey);
    ECPoint ecPoint = CURVE.getCurve().decodePoint(pubKey);
    byte[] encoded = ecPoint.getEncoded(false);
    BigInteger n = new BigInteger(1, Arrays.copyOfRange(encoded, 1, encoded.length));
    return ByteArray.toHexString(n.toByteArray());
  }

  public static ECKeyPair generateKeyPair(String privateKey) {
    BigInteger privKey = new BigInteger(privateKey, 16);
    BigInteger pubKey = Sign.publicKeyFromPrivate(privKey);
    return new ECKeyPair(privKey, pubKey);
  }

  /**
   * The produced signature is in the 65-byte [R || S || V] format where V is 0 or 1.
   */
  public static byte[] sigData(String msg, String privateKey) {
    ECKeyPair keyPair = generateKeyPair(privateKey);
    Sign.SignatureData signature = Sign.signMessage(msg.getBytes(), keyPair, true);
    byte[] data = new byte[65];
    System.arraycopy(signature.getR(), 0, data, 0, 32);
    System.arraycopy(signature.getS(), 0, data, 32, 32);
    data[64] = signature.getV()[0];
    return data;
  }

  public static BigInteger recoverPublicKey(String msg, byte[] sig) throws SignatureException {
    int recId = sig[64];
    if (recId < 27) {
      recId += 27;
    }
    Sign.SignatureData signature = new SignatureData((byte) recId, ByteArray.subArray(sig, 0, 32),
        ByteArray.subArray(sig, 32, 64));
    return Sign.signedMessageToKey(msg.getBytes(), signature);
  }

  /**
   * @param publicKey uncompress hex publicKey
   * @param msg to be hashed message
   */
  public static boolean verifySignature(String publicKey, String msg, byte[] sig)
      throws SignatureException {
    BigInteger pubKey = new BigInteger(publicKey, 16);
    BigInteger pubKeyRecovered = recoverPublicKey(msg, sig);
    return pubKey.equals(pubKeyRecovered);
  }

  //we only use fix width hash
  public static boolean isValidHash(String base32Hash) {
    if (base32Hash == null || base32Hash.length() != truncateLength || base32Hash.contains("\r")
        || base32Hash.contains("\n")) {
      return false;
    }
    StringBuilder sb = new StringBuilder(base32Hash);
    for (int i = 0; i < 32 - truncateLength; i++) {
      sb.append(padding);
    }
    try {
      Base32.decode(sb.toString());
    } catch (Exception e) {
      return false;
    }
    return true;
  }

  public static String encode64(byte[] content) {
    String base64Content = new String(Base64.getUrlEncoder().encode(content),
        StandardCharsets.UTF_8);
    return StringUtils.stripEnd(base64Content, padding);
  }

  // An Encoding is a radix 64 encoding/decoding scheme, defined by a
  // 64-character alphabet. The most common encoding is the "base64"
  // encoding defined in RFC 4648 and used in MIME (RFC 2045) and PEM
  // (RFC 1421).  RFC 4648 also defines an alternate encoding, which is
  // the standard encoding with - and _ substituted for + and /.
  public static byte[] decode64(String base64Content) {
    return Base64.getUrlDecoder().decode(base64Content);
  }

  public static String encode32(byte[] content) {
    String base32Content = new String(Base32.encode(content), StandardCharsets.UTF_8);
    return StringUtils.stripEnd(base32Content, padding);
  }

  /**
   * first get the hash of string, then get first 16 letter, last encode it with base32
   */
  public static String encode32AndTruncate(String content) {
    return encode32(ByteArray.subArray(Hash.sha3(content.getBytes()), 0, 16))
        .substring(0, truncateLength);
  }

  /**
   * if content's length is not multiple of 8, we padding it
   */
  public static byte[] decode32(String content) {
    int left = content.length() % 8;
    StringBuilder sb = new StringBuilder(content);
    if (left > 0) {
      for (int i = 0; i < 8 - left; i++) {
        sb.append(padding);
      }
    }
    return Base32.decode(sb.toString());
  }
}
