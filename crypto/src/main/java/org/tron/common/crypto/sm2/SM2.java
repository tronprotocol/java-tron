package org.tron.common.crypto.sm2;

import static org.tron.common.crypto.Hash.computeAddress;
import static org.tron.common.utils.BIUtil.isLessThan;
import static org.tron.common.utils.ByteUtil.bigIntegerToBytes;

import java.io.Serializable;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Objects;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x9.X9IntegerConverter;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.generators.ECKeyPairGenerator;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECKeyGenerationParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.math.ec.ECAlgorithms;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.SignInterface;
import org.tron.common.crypto.SignatureInterface;
import org.tron.common.crypto.jce.ECKeyFactory;
import org.tron.common.crypto.jce.TronCastleProvider;
import org.tron.common.utils.BIUtil;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ByteUtil;

/**
 * Implement Chinese Commercial Cryptographic Standard of SM2
 */
@Slf4j(topic = "crypto")
public class SM2 implements Serializable, SignInterface {

  private static final BigInteger SM2_N = new BigInteger(
      "FFFFFFFEFFFFFFFFFFFFFFFFFFFFFFFF7203DF6B21C6052B53BBF40939D54123", 16);
  private static final BigInteger SM2_P = new BigInteger(
      "FFFFFFFEFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF00000000FFFFFFFFFFFFFFFF", 16);
  private static final BigInteger SM2_A = new BigInteger(
      "FFFFFFFEFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF00000000FFFFFFFFFFFFFFFC", 16);
  private static final BigInteger SM2_B = new BigInteger(
      "28E9FA9E9D9F5E344D5A9E4BCF6509A7F39789F515AB8F92DDBCBD414D940E93", 16);
  private static final BigInteger SM2_GX = new BigInteger(
      "32C4AE2C1F1981195F9904466A39C9948FE30BBFF2660BE1715A4589334C74C7", 16);
  private static final BigInteger SM2_GY = new BigInteger(
      "BC3736A2F4F6779C59BDCEE36B692153D0A9877CC62A474002DF32E52139F0A0", 16);

  private static final ECDomainParameters eccParam;
  private static final ECParameterSpec eccSpec;
  private static final ECCurve.Fp curve;
  private static final ECPoint eccPointG;

  private static final SecureRandom secureRandom;

  static {
    secureRandom = new SecureRandom();
    curve = new ECCurve.Fp(SM2_P, SM2_A, SM2_B, null, null);
    eccPointG = curve.createPoint(SM2_GX, SM2_GY);
    eccParam = new ECDomainParameters(curve, eccPointG, SM2_N);
    eccSpec = new ECParameterSpec(curve, eccPointG, SM2_N);
  }

  protected final ECPoint pub;

  private final PrivateKey privKey;

  // Transient because it's calculated on demand.
  private transient volatile byte[] pubKeyHash;
  private transient volatile byte[] nodeId;

  public SM2() {
    this(secureRandom);
  }

  /**
   * Generates an entirely new keypair.
   *
   * <p>
   * BouncyCastle will be used as the Java Security Provider
   */

  /**
   * Generate a new keypair using the given Java Security Provider.
   *
   * <p>
   * All private key operations will use the provider.
   */
  public SM2(SecureRandom secureRandom) {

    ECKeyGenerationParameters ecKeyGenerationParameters = new ECKeyGenerationParameters(eccParam,
        secureRandom);
    ECKeyPairGenerator keyPairGenerator = new ECKeyPairGenerator();
    keyPairGenerator.init(ecKeyGenerationParameters);
    AsymmetricCipherKeyPair kp = keyPairGenerator.generateKeyPair();
    ECPrivateKeyParameters ecpriv = (ECPrivateKeyParameters) kp.getPrivate();
    ECPublicKeyParameters ecpub = (ECPublicKeyParameters) kp.getPublic();

    BigInteger privateKey = ecpriv.getD();
    this.privKey = privateKeyFromBigInteger(privateKey);
    this.pub = ecpub.getQ();
  }

  public SM2(byte[] key, boolean isPrivateKey) {
    if (isPrivateKey) {
      if (!isValidPrivateKey(key)) {
        throw new IllegalArgumentException("Invalid private key in SM2.");
      }

      BigInteger pk = new BigInteger(1, key);
      this.privKey = privateKeyFromBigInteger(pk);
      this.pub = eccParam.getG().multiply(pk);
    } else {
      if (ByteArray.isEmpty(key)) {
        throw new IllegalArgumentException("Empty public key in SM2.");
      }

      ECPoint point = eccParam.getCurve().decodePoint(key);
      if (point.isInfinity() || !point.isValid()) {
        throw new IllegalArgumentException("Public key is not a valid point on SM2 curve.");
      }
      this.privKey = null;
      this.pub = point;
    }
  }

  /**
   * Pair a private key with a public EC point.
   *
   * <p>
   * All private key operations will use the provider.
   */

  public SM2(@Nullable PrivateKey privKey, ECPoint pub) {
    if (privKey == null || isECPrivateKey(privKey)) {
      this.privKey = privKey;
    } else {
      throw new IllegalArgumentException(
          "Expected EC private key, given a private key object with" +
              " class "
              + privKey.getClass() +
              " and algorithm "
              + privKey.getAlgorithm());
    }

    if (pub == null) {
      throw new IllegalArgumentException("Public key should not be null");
    } else {
      this.pub = pub;
    }
  }

  /**
   * Pair a private key integer with a public EC point
   */
  public SM2(@Nullable BigInteger priv, ECPoint pub) {
    this(
        priv == null ? null : privateKeyFromBigInteger(priv),
        pub);
  }

  /**
   * Convert a BigInteger into a PrivateKey object
   */
  private static PrivateKey privateKeyFromBigInteger(BigInteger priv) {
    if (!isValidPrivateKey(priv)) {
      throw new IllegalArgumentException("Invalid private key in SM2.");
    }

    try {
      return ECKeyFactory
          .getInstance(TronCastleProvider.getInstance())
          .generatePrivate(new ECPrivateKeySpec(priv,
              eccSpec));
    } catch (InvalidKeySpecException ex) {
      throw new AssertionError("Assumed correct key spec statically");
    }
  }

  /*
   * Test if a generic private key is an EC private key
   *
   * it is not sufficient to check that privKey is a subtype of ECPrivateKey
   * as the SunPKCS11 Provider will return a generic PrivateKey instance
   * a fallback that covers this case is to check the key algorithm
   */
  private static boolean isECPrivateKey(PrivateKey privKey) {
    return privKey instanceof ECPrivateKey || privKey.getAlgorithm()
        .equals("EC");
  }

  /**
   * Utility for compressing an elliptic curve point. Returns the same point if
   * it's already
   * compressed. See the ECKey class docs for a discussion of point compression.
   *
   * @param uncompressed -
   * @return -
   * @deprecated per-point compression property will be removed in Bouncy Castle
   */
  public static ECPoint compressPoint(ECPoint uncompressed) {
    return eccParam.getCurve().decodePoint(uncompressed.getEncoded(true));
  }

  /**
   * Utility for decompressing an elliptic curve point. Returns the same point if
   * it's already
   * compressed. See the ECKey class docs for a discussion of point compression.
   *
   * @param compressed -
   * @return -
   * @deprecated per-point compression property will be removed in Bouncy Castle
   */
  public static ECPoint decompressPoint(ECPoint compressed) {
    return eccParam.getCurve().decodePoint(compressed.getEncoded(false));
  }

  /**
   * Creates an SM2 given the private key only.
   *
   * @param privKey -
   * @return -
   */
  public static SM2 fromPrivate(BigInteger privKey) {
    if (!isValidPrivateKey(privKey)) {
      throw new IllegalArgumentException("Invalid private key in SM2.");
    }

    return new SM2(privKey, eccParam.getG().multiply(privKey));
  }

  /**
   * Creates an SM2 given the private key only.
   *
   * @param privKeyBytes -
   * @return -
   */
  public static SM2 fromPrivate(byte[] privKeyBytes) {
    if (!isValidPrivateKey(privKeyBytes)) {
      throw new IllegalArgumentException("Invalid private key in SM2.");
    }

    return fromPrivate(new BigInteger(1, privKeyBytes));
  }

  public static boolean isValidPrivateKey(byte[] keyBytes) {
    if (ByteArray.isEmpty(keyBytes)) {
      return false;
    }

    BigInteger key = new BigInteger(1, keyBytes);
    return key.compareTo(BigInteger.ONE) >= 0 && key.compareTo(SM2_N) < 0;
  }

  public static boolean isValidPrivateKey(BigInteger privateKey) {
    if (privateKey == null) {
      return false;
    }
    return privateKey.compareTo(BigInteger.ONE) >= 0 && privateKey.compareTo(SM2_N) < 0;
  }

  /**
   * Creates an SM2 that cannot be used for signing, only verifying signatures,
   * from the given
   * encoded point. The compression state of pub will be preserved.
   *
   * @param pub -
   * @return -
   */
  public static SM2 fromPublicOnly(byte[] pub) {
    return new SM2((PrivateKey) null, eccParam.getCurve().decodePoint(pub));
  }

  /**
   * Returns public key bytes from the given private key. To convert a byte array
   * into a BigInteger,
   * use <tt> new BigInteger(1, bytes);</tt>
   *
   * @param privKey    -
   * @param compressed -
   * @return -
   */
  public static byte[] publicKeyFromPrivate(BigInteger privKey, boolean compressed) {
    ECPoint point = eccParam.getG().multiply(privKey);
    return point.getEncoded(compressed);
  }

  /**
   * Compute the encoded X, Y coordinates of a public point.
   * <p>
   * This is the encoded public key
   * without the leading byte.
   *
   * @param pubPoint a public point
   * @return 64-byte X,Y point pair
   */
  public static byte[] pubBytesWithoutFormat(ECPoint pubPoint) {
    final byte[] pubBytes = pubPoint.getEncoded(/* uncompressed */ false);
    return Arrays.copyOfRange(pubBytes, 1, pubBytes.length);
  }

  /**
   * Recover the public key from an encoded node id.
   *
   * @param nodeId a 64-byte X,Y point pair
   */
  public static SM2 fromNodeId(byte[] nodeId) {
    if (nodeId == null) {
      throw new IllegalArgumentException("Node ID cannot be null");
    }
    check(nodeId.length == 64, "Expected a 64 byte node id");
    byte[] pubBytes = new byte[65];
    System.arraycopy(nodeId, 0, pubBytes, 1, nodeId.length);
    pubBytes[0] = 0x04; // uncompressed
    return SM2.fromPublicOnly(pubBytes);
  }

  public static byte[] signatureToKeyBytes(byte[] messageHash, String signatureBase64) throws SignatureException {
    if (messageHash == null || signatureBase64 == null) {
      throw new SignatureException("Message hash or signature cannot be null");
    }
    byte[] signatureEncoded;
    try {
      signatureEncoded = Base64.decode(signatureBase64);
    } catch (RuntimeException e) {
      // This is what you getData back from Bouncy Castle if base64 doesn't
      // decode :(
      throw new SignatureException("Could not decode base64", e);
    }
    // Parse the signature bytes into r/s and the selector value.
    if (signatureEncoded.length < 65) {
      throw new SignatureException("Signature truncated, expected 65 " +
          "bytes and got " + signatureEncoded.length);
    }

    return signatureToKeyBytes(
        messageHash,
        SM2Signature.fromComponents(
            Arrays.copyOfRange(signatureEncoded, 1, 33),
            Arrays.copyOfRange(signatureEncoded, 33, 65),
            (byte) (signatureEncoded[0] & 0xFF)));
  }

  public static byte[] signatureToKeyBytes(byte[] messageHash,
      SM2Signature sig) throws SignatureException {
    if (messageHash == null || sig == null) {
      throw new SignatureException("Message hash or signature cannot be null");
    }
    check(messageHash.length == 32, "messageHash argument has length " +
        messageHash.length);
    int header = sig.v;
    // The header byte: 0x1B = first key with even y, 0x1C = first key
    // with odd y,
    // 0x1D = second key with even y, 0x1E = second key
    // with odd y
    if (header < 27 || header > 34) {
      throw new SignatureException("Header byte out of range: " + header);
    }
    if (header >= 31) {
      header -= 4;
    }
    int recId = header - 27;
    byte[] key = recoverPubBytesFromSignature(recId, sig,
        messageHash);
    if (key == null) {
      throw new SignatureException("Could not recover public key from " +
          "signature");
    }
    return key;
  }

  public byte[] hash(byte[] message) {
    SM2Signer signer = this.getSM2SignerForHash();
    return signer.generateSM3Hash(message);
  }

  @Override
  public byte[] getPrivateKey() {
    return getPrivKeyBytes();
  }

  /**
   * Gets the encoded public key value.
   *
   * @return 65-byte encoded public key
   */
  @Override
  public byte[] getPubKey() {
    return pub.getEncoded(/* compressed */ false);
  }

  /**
   * Gets the address form of the public key.
   *
   * @return 21-byte address
   */
  @Override
  public byte[] getAddress() {
    if (pubKeyHash == null) {
      pubKeyHash = computeAddress(this.pub);
    }
    return Arrays.copyOf(pubKeyHash, pubKeyHash.length);
  }

  /**
   * Compute the address of the key that signed the given signature.
   *
   * @param messageHash     32-byte hash of message
   * @param signatureBase64 Base-64 encoded signature
   * @return 20-byte address
   */
  public static byte[] signatureToAddress(byte[] messageHash, String signatureBase64) throws SignatureException {
    return computeAddress(signatureToKeyBytes(messageHash,
        signatureBase64));
  }

  /**
   * Compute the address of the key that signed the given signature.
   *
   * @param messageHash 32-byte hash of message
   * @param sig         -
   * @return 20-byte address
   */
  public static byte[] signatureToAddress(byte[] messageHash,
      SM2Signature sig) throws SignatureException {
    return computeAddress(signatureToKeyBytes(messageHash, sig));
  }

  /**
   * Compute the key that signed the given signature.
   *
   * @param messageHash     32-byte hash of message
   * @param signatureBase64 Base-64 encoded signature
   * @return ECKey
   */
  public static SM2 signatureToKey(byte[] messageHash, String signatureBase64) throws SignatureException {
    final byte[] keyBytes = signatureToKeyBytes(messageHash,
        signatureBase64);
    return fromPublicOnly(keyBytes);
  }

  /**
   * Compute the key that signed the given signature.
   *
   * @param messageHash 32-byte hash of message
   * @param sig         -
   * @return ECKey
   */
  public static SM2 signatureToKey(byte[] messageHash, SM2Signature sig) throws SignatureException {
    final byte[] keyBytes = signatureToKeyBytes(messageHash, sig);
    return fromPublicOnly(keyBytes);
  }

  /**
   * Takes the SM3 hash (32 bytes) of data and returns the SM2 signature which
   * including the v
   *
   * @param messageHash -
   * @return -
   * @throws IllegalStateException if this ECKey does not have the private part.
   */
  public SM2Signature sign(byte[] messageHash) {
    if (ByteArray.isEmpty(messageHash) || messageHash.length != 32) {
      throw new IllegalArgumentException("Expected 32 byte input to " +
          "SM2 signature, not " + (messageHash == null ? "null" : messageHash.length));
    }
    // No decryption of private key required.
    SM2Signer signer = getSigner();
    BigInteger[] componets = signer.generateHashSignature(messageHash);

    SM2Signature sig = new SM2Signature(componets[0], componets[1]);
    sig.v = (byte) (findRecId(sig, messageHash) + 27);
    return sig;
  }

  /**
   * Signs the given hash and returns the R and S components as BigIntegers and
   * putData them in SM2Signature
   *
   * @param input to sign
   * @return SM2Signature signature that contains the R and S components
   */
  public String signHash(byte[] input) {
    return sign(input).toBase64();
  }

  public byte[] Base64toBytes(String signature) {
    byte[] signData = Base64.decode(signature);
    byte first = (byte) (signData[0] - 27);
    byte[] temp = Arrays.copyOfRange(signData, 1, 65);
    return ByteUtil.appendByte(temp, first);
  }

  /**
   * Takes the message of data and returns the SM2 signature
   *
   * @param message -
   * @return -
   * @throws IllegalStateException if this ECKey does not have the private part.
   */
  public SM2Signature signMessage(byte[] message, @Nullable String userID) {
    SM2Signature sig = signMsg(message, userID);

    SM2Signer signer = getSigner();
    byte[] messageHash = signer.generateSM3Hash(message);
    sig.v = (byte) (findRecId(sig, messageHash) + 27);
    return sig;
  }

  /**
   * Signs the given hash and returns the R and S components as BigIntegers and
   * putData them in SM2Signature
   *
   * @param msg to sign
   * @return SM2Signature signature that contains the R and S components
   */
  public SM2Signature signMsg(byte[] msg, @Nullable String userID) {
    if (msg == null) {
      throw new IllegalArgumentException("Expected signature message of " +
          "SM2 is null");
    }
    // No decryption of private key required.
    SM2Signer signer = getSigner();
    BigInteger[] componets = signer.generateSignature(msg);
    return new SM2Signature(componets[0], componets[1]);
  }

  private int findRecId(SM2Signature sig, byte[] messageHash) {
    byte[] thisKey = this.pub.getEncoded(/* compressed */ false);
    for (int i = 0; i < 4; i++) {
      byte[] k = recoverPubBytesFromSignature(i, sig, messageHash);
      if (k != null && Arrays.equals(k, thisKey)) {
        return i;
      }
    }
    throw new RuntimeException("Could not construct a recoverable key" +
        ". This should never happen.");
  }

  private SM2Signer getSigner() {
    SM2Signer signer = new SM2Signer();
    BigInteger d = getPrivKey();
    ECPrivateKeyParameters privateKeyParameters = new ECPrivateKeyParameters(d, eccParam);
    signer.init(true, privateKeyParameters);
    return signer;
  }

  /**
   * used to generate the SM3 hash for SM2 signature generation or verification
   */
  public SM2Signer getSM2SignerForHash() {
    SM2Signer signer = new SM2Signer();
    ECPublicKeyParameters publicKeyParameters = new ECPublicKeyParameters(pub, eccParam);
    signer.init(false, publicKeyParameters);
    return signer;
  }

  /**
   * <p>
   * Given the components of a signature and a selector value, recover and return
   * the public key that generated the signature
   */
  @Nullable
  public static byte[] recoverPubBytesFromSignature(int recId,
      SM2Signature sig,
      byte[] messageHash) {
    if (sig == null || messageHash == null) {
      throw new IllegalArgumentException("Signature or message hash cannot be null");
    }
    check(recId >= 0 && recId <= 3, "recId must be in range [0, 3]");
    check(sig.r != null && sig.r.signum() > 0 && BIUtil.isLessThan(sig.r, SM2_N),
        "r must be in range (0, n)");
    check(sig.s != null && sig.s.signum() > 0 && BIUtil.isLessThan(sig.s, SM2_N),
        "s must be in range (0, n)");
    check(messageHash.length == 32, "messageHash must be 32 bytes");
    // 1.0 For j from 0 to h (h == recId here and the loop is outside
    // this function)
    // 1.1 Let x = r + jn
    BigInteger n = eccParam.getN(); // Curve order.
    BigInteger prime = curve.getQ();
    BigInteger i = BigInteger.valueOf((long) recId / 2);

    BigInteger e = new BigInteger(1, messageHash);
    BigInteger x = sig.r.subtract(e).mod(n); // r = (x + e) mod n
    x = x.add(i.multiply(n));
    // 1.2. Convert the integer x to an octet string X of length mlen
    // using the conversion routine
    // specified in Section 2.3.7, where mlen = ⌈(log2 p)/8⌉ or
    // mlen = ⌈m/8⌉.
    // 1.3. Convert the octet string (16 set binary digits)||X to an
    // elliptic curve point R using the
    // conversion routine specified in Section 2.3.4. If this
    // conversion routine outputs “invalid”, then
    // do another iteration of Step 1.
    //
    // More concisely, what these points mean is to use X as a compressed
    // public key.
    if (x.compareTo(prime) >= 0) {
      // Cannot have point co-ordinates larger than this as everything
      // takes place modulo Q.
      return null;
    }
    // Compressed allKeys require you to know an extra bit of data about the
    // y-coord as there are two possibilities.
    // So it's encoded in the recId.
    ECPoint R = decompressKey(x, (recId & 1) == 1);
    // 1.4. If nR != point at infinity, then do another iteration of
    // Step 1 (callers responsibility).
    if (!R.multiply(n).isInfinity()) {
      return null;
    }

    // recover Q from the formula: s*G + (s+r)*Q = R => Q = (s+r)^(-1) (R-s*G)
    BigInteger sAddR = sig.s.add(sig.r).mod(n);
    if (sAddR.equals(BigInteger.ZERO)) {
      return null;
    }
    BigInteger srInv = sAddR.modInverse(n);
    BigInteger sNeg = BigInteger.ZERO.subtract(sig.s).mod(n);
    BigInteger coeff = srInv.multiply(sNeg).mod(n);

    ECPoint.Fp q = (ECPoint.Fp) ECAlgorithms.sumOfTwoMultiplies(eccParam
        .getG(), coeff, R, srInv);
    return q.getEncoded(/* compressed */ false);
  }

  /**
   * Decompress a compressed public key (x co-ord and low-bit of y-coord).
   *
   * @param xBN  -
   * @param yBit -
   * @return -
   */

  private static ECPoint decompressKey(BigInteger xBN, boolean yBit) {
    X9IntegerConverter x9 = new X9IntegerConverter();
    byte[] compEnc = x9.integerToBytes(xBN, 1 + x9.getByteLength(eccParam
        .getCurve()));
    compEnc[0] = (byte) (yBit ? 0x03 : 0x02);
    return eccParam.getCurve().decodePoint(compEnc);
  }

  private static void check(boolean test, String message) {
    if (!test) {
      throw new IllegalArgumentException(message);
    }
  }

  /**
   * <p>
   * Verifies the given SM2 signature against the message bytes using the public
   * key bytes.
   * </p>
   * <p>
   * <p>
   * When using native SM2 verification, data must be 32 bytes, and no element may
   * be larger than 520 bytes.
   * </p>
   *
   * @param data      Hash of the data to verify.
   * @param signature signature.
   * @param pub       The public key bytes to use.
   * @return -
   */
  public static boolean verify(byte[] data, SM2Signature signature,
      byte[] pub) {
    if (ByteArray.isEmpty(data) || signature == null || ByteArray.isEmpty(pub)) {
      throw new IllegalArgumentException("Data, signature, or public key cannot be null");
    }
    SM2Signer signer = new SM2Signer();
    ECPublicKeyParameters params = new ECPublicKeyParameters(eccParam
        .getCurve().decodePoint(pub), eccParam);
    signer.init(false, params);
    try {
      return signer.verifyHashSignature(data, signature.r, signature.s);
    } catch (NullPointerException npe) {
      // Bouncy Castle contains a bug that can cause NPEs given
      // specially crafted signatures.
      // Those signatures are inherently invalid/attack sigs so we just
      // fail them here rather than crash the thread.
      logger.error("Caught NPE inside bouncy castle", npe);
      return false;
    }
  }

  /**
   * Verifies a DER-encoded SM2 signature against the provided hash using the
   * public key bytes.
   *
   * @param data      hash of the data to verify
   * @param signature DER-encoded signature
   * @param pub       public key bytes to use
   * @return false when the signature is malformed or invalid
   */
  public static boolean verify(byte[] data, byte[] signature, byte[] pub) {
    if (data == null || signature == null || pub == null) {
      return false;
    }
    try {
      return verify(data, SM2Signature.decodeFromDER(signature), pub);
    } catch (IllegalArgumentException | SignatureException e) {
      return false;
    }
  }

  /**
   * Verifies a DER-encoded SM2 signature against the message bytes using the
   * public key bytes.
   *
   * @param message   message bytes to verify
   * @param signature DER-encoded signature
   * @param pub       public key bytes to use
   * @return false when the signature is malformed or invalid
   */
  public static boolean verifyMessage(byte[] message, byte[] signature, byte[] pub) {
    return verifyMessage(message, signature, pub, null);
  }

  /**
   * Verifies a DER-encoded SM2 signature against the message bytes using the
   * public key bytes.
   *
   * @param message   message bytes to verify
   * @param signature DER-encoded signature
   * @param pub       public key bytes to use
   * @param userID    optional user identifier
   * @return false when the signature is malformed or invalid
   */
  public static boolean verifyMessage(byte[] message, byte[] signature, byte[] pub,
      @Nullable String userID) {
    if (message == null || signature == null || pub == null) {
      return false;
    }
    try {
      SM2 key = SM2.fromPublicOnly(pub);
      byte[] messageHash = key.getSM2SignerForHash().generateSM3Hash(message);
      return verify(messageHash, SM2Signature.decodeFromDER(signature), pub);
    } catch (IllegalArgumentException | SignatureException e) {
      return false;
    }
  }

  /**
   * Returns true if the given pubkey is canonical, i.e. the correct length taking
   * into account compression.
   *
   * @param pubkey -
   * @return -
   */
  public static boolean isPubKeyCanonical(byte[] pubkey) {
    if (pubkey[0] == 0x04) {
      // Uncompressed pubkey
      return pubkey.length == 65;
    } else if (pubkey[0] == 0x02 || pubkey[0] == 0x03) {
      // Compressed pubkey
      return pubkey.length == 33;
    } else {
      return false;
    }
  }

  /**
   * @param recId       Which possible key to recover.
   * @param sig         the R and S components of the signature, wrapped.
   * @param messageHash Hash of the data that was signed.
   * @return 20-byte address
   */
  @Nullable
  public static byte[] recoverAddressFromSignature(int recId,
      SM2Signature sig,
      byte[] messageHash) {
    final byte[] pubBytes = recoverPubBytesFromSignature(recId, sig,
        messageHash);
    if (pubBytes == null) {
      return null;
    } else {
      return computeAddress(pubBytes);
    }
  }

  /**
   * @param recId       Which possible key to recover.
   * @param sig         the R and S components of the signature, wrapped.
   * @param messageHash Hash of the data that was signed.
   * @return ECKey
   */
  @Nullable
  public static SM2 recoverFromSignature(int recId, SM2Signature sig,
      byte[] messageHash) {
    final byte[] pubBytes = recoverPubBytesFromSignature(recId, sig,
        messageHash);
    if (pubBytes == null) {
      return null;
    } else {
      return fromPublicOnly(pubBytes);
    }
  }

  /**
   * Returns true if this key doesn't have access to private key bytes. This may
   * be because it was never given any private key bytes to begin with (a watching
   * key).
   *
   * @return -
   */
  public boolean isPubKeyOnly() {
    return privKey == null;
  }

  /**
   * Returns true if this key has access to private key bytes. Does the opposite
   * of {@link
   * #isPubKeyOnly()}.
   *
   * @return -
   */
  public boolean hasPrivKey() {
    return privKey != null;
  }

  /**
   * Generates the NodeID based on this key, that is the public key without first
   * format byte
   */
  public byte[] getNodeId() {
    if (nodeId == null) {
      nodeId = pubBytesWithoutFormat(this.pub);
    }
    return Arrays.copyOf(nodeId, nodeId.length);
  }

  /**
   * Gets the public key in the form of an elliptic curve point object from Bouncy
   * Castle.
   *
   * @return -
   */
  public ECPoint getPubKeyPoint() {
    return pub;
  }

  /**
   * Gets the private key in the form of an integer field element. The public key
   * is derived by performing EC point addition this number of times (i.e. point
   * multiplying).
   *
   * @return -
   * @throws IllegalStateException if the private key bytes are not available.
   */
  public BigInteger getPrivKey() {
    if (privKey == null) {
      throw new ECKey.MissingPrivateKeyException();
    } else if (privKey instanceof BCECPrivateKey) {
      return ((BCECPrivateKey) privKey).getD();
    } else {
      throw new ECKey.MissingPrivateKeyException();
    }
  }

  public String toString() {
    return "pub:" + Hex.toHexString(pub.getEncoded(false));
  }

  /**
   * Verifies the given R/S pair (signature) against a hash using the public key.
   *
   * @param sigHash   -
   * @param signature -
   * @return -
   */
  public boolean verify(byte[] sigHash, SM2Signature signature) {
    return SM2.verify(sigHash, signature, getPubKey());
  }

  /**
   * Returns true if this pubkey is canonical, i.e. the correct length taking into
   * account compression.
   *
   * @return -
   */
  public boolean isPubKeyCanonical() {
    return isPubKeyCanonical(pub.getEncoded(/* uncompressed */ false));
  }

  /**
   * Returns a 32 byte array containing the private key, or null if the key is
   * encrypted or public only
   *
   * @return -
   */
  @Nullable
  public byte[] getPrivKeyBytes() {
    if (privKey == null) {
      return null;
    } else if (privKey instanceof BCECPrivateKey) {
      return bigIntegerToBytes(((BCECPrivateKey) privKey).getD(), 32);
    } else {
      return null;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    SM2 ecKey = (SM2) o;

    if (!Objects.equals(privKey, ecKey.privKey)) {
      return false;
    }
    return Objects.equals(pub, ecKey.pub);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(getPubKey());
  }

  public static class SM2Signature implements SignatureInterface {

    /**
     * The two components of the signature.
     */
    public final BigInteger r, s;
    public byte v;

    /**
     * Constructs a signature with the given components. Does NOT automatically
     * canonicalise the signature.
     *
     * @param r -
     * @param s -
     */
    public SM2Signature(BigInteger r, BigInteger s) {
      this.r = r;
      this.s = s;
    }

    public SM2Signature(byte[] r, byte[] s, byte v) {
      this.r = new BigInteger(1, r);
      this.s = new BigInteger(1, s);
      this.v = v;
    }

    private static SM2Signature fromComponents(byte[] r, byte[] s) {
      return new SM2Signature(new BigInteger(1, r), new BigInteger(1,
          s));
    }

    /**
     * @param r -
     * @param s -
     * @param v -
     * @return -
     */
    public static SM2Signature fromComponents(byte[] r, byte[] s, byte v) {
      SM2Signature signature = fromComponents(r, s);
      signature.v = v;
      return signature;
    }

    public static SM2Signature decodeFromDER(byte[] signature) throws SignatureException {
      if (ByteArray.isEmpty(signature) || signature.length > 128) {
        throw new SignatureException("Invalid DER signature length");
      }

      try (ASN1InputStream inputStream = new ASN1InputStream(signature)) {
        ASN1Primitive primitive = inputStream.readObject();
        if (!(primitive instanceof ASN1Sequence)) {
          throw new SignatureException("Invalid DER signature format");
        }

        ASN1Sequence sequence = (ASN1Sequence) primitive;
        if (sequence.size() != 2) {
          throw new SignatureException("Invalid DER signature component count");
        }

        BigInteger r = ASN1Integer.getInstance(sequence.getObjectAt(0)).getPositiveValue();
        BigInteger s = ASN1Integer.getInstance(sequence.getObjectAt(1)).getPositiveValue();
        return new SM2Signature(r, s);
      } catch (IOException | IllegalArgumentException e) {
        throw new SignatureException("Could not decode DER signature", e);
      }
    }

    public static boolean validateComponents(BigInteger r, BigInteger s,
        byte v) {

      if (v != 27 && v != 28) {
        return false;
      }

      if (isLessThan(r, BigInteger.ONE)) {
        return false;
      }
      if (isLessThan(s, BigInteger.ONE)) {
        return false;
      }

      if (!isLessThan(r, SM2.SM2_N)) {
        return false;
      }
      return isLessThan(s, SM2.SM2_N);
    }

    public boolean validateComponents() {
      return validateComponents(r, s, v);
    }

    /**
     * @return -
     */
    public String toBase64() {
      byte[] sigData = new byte[65]; // 1 header + 32 bytes for R + 32
      // bytes for S
      sigData[0] = v;
      System.arraycopy(bigIntegerToBytes(this.r, 32), 0, sigData, 1, 32);
      System.arraycopy(bigIntegerToBytes(this.s, 32), 0, sigData, 33, 32);
      return new String(Base64.encode(sigData), StandardCharsets.UTF_8);
    }

    public byte[] toByteArray() {
      final byte fixedV = this.v >= 27
          ? (byte) (this.v - 27)
          : this.v;

      return ByteUtil.merge(
          ByteUtil.bigIntegerToBytes(this.r, 32),
          ByteUtil.bigIntegerToBytes(this.s, 32),
          new byte[] { fixedV });
    }

    public String toHex() {
      return Hex.toHexString(toByteArray());
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      SM2Signature signature = (SM2Signature) o;

      if (!r.equals(signature.r)) {
        return false;
      }
      return s.equals(signature.s);
    }

    @Override
    public int hashCode() {
      int result = r.hashCode();
      result = 31 * result + s.hashCode();
      return result;
    }
  }
}
