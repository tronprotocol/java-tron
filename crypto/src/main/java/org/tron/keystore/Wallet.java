package org.tron.keystore;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.UUID;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.generators.SCrypt;
import org.bouncycastle.crypto.params.KeyParameter;
import org.tron.common.crypto.Hash;
import org.tron.common.crypto.SignInterface;
import org.tron.common.crypto.SignUtils;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.StringUtil;
import org.tron.core.exception.CipherException;

/**
 * <p>Ethereum wallet file management. For reference, refer to <a href="https://github.com/ethereum/wiki/wiki/Web3-Secret-Storage-Definition">
 * Web3 Secret Storage Definition</a> or the <a href="https://github.com/ethereum/go-ethereum/blob/master/accounts/key_store_passphrase.go">
 * Go Ethereum client implementation</a>.</p>
 *
 * <p><strong>Note:</strong> the Bouncy Castle Scrypt implementation {@link SCrypt}, fails to
 * comply
 * with the following Ethereum reference <a href="https://github.com/ethereum/wiki/wiki/Web3-Secret-Storage-Definition#scrypt">
 * Scrypt test vector</a>:</p>
 *
 * <pre>
 * {@code
 * // Only value of r that cost (as an int) could be exceeded for is 1
 * if (r == 1 && N_STANDARD > 65536)
 * {
 *     throw new IllegalArgumentException("Cost parameter N_STANDARD must be > 1 and < 65536.");
 * }
 * }
 * </pre>
 */
public class Wallet {

  // KDF identifiers used in the Web3 Secret Storage "kdf" field.
  // The old name "AES_128_CTR" was misleading — the value is the PBKDF2 KDF
  // identifier, not the cipher (CIPHER below). The inner class name
  // `WalletFile.Aes128CtrKdfParams` is kept for wire-format/Jackson-subtype
  // backward compatibility even though it also reflects the same history.
  protected static final String PBKDF2 = "pbkdf2";
  protected static final String SCRYPT = "scrypt";
  private static final int N_LIGHT = 1 << 12;
  private static final int P_LIGHT = 6;
  private static final int N_STANDARD = 1 << 18;
  private static final int P_STANDARD = 1;
  private static final int R = 8;
  private static final int DKLEN = 32;
  private static final int CURRENT_VERSION = 3;
  private static final String CIPHER = "aes-128-ctr";

  // Upper bounds for untrusted keystore KDF cost parameters. A hostile
  // keystore file may declare arbitrary KDF strengths: extreme scrypt
  // parameters make Bouncy Castle either allocate unbounded memory
  // (n*r*128 bytes — e.g. n=2^22, r=8 requests 4 GiB and dies with OOM)
  // or compute for hours, and out-of-range values can crash the JVM with
  // NegativeArraySizeException / ArithmeticException inside SCrypt.generate.
  // All values here are rejected by validationError() before any KDF runs.
  private static final int SCRYPT_N_MIN = 1 << 12;
  private static final int SCRYPT_N_MAX = 1 << 20;
  private static final int SCRYPT_R_MAX = 8;
  private static final int SCRYPT_P_MAX = 8;
  private static final long SCRYPT_MEMORY_MAX_BYTES = 1L << 30; // 128 * r * n cap = 1 GiB
  private static final int KDF_DKLEN_MAX = 128;
  private static final int PBKDF2_C_MAX = 1 << 20;

  public static WalletFile create(String password, SignInterface sign, int n, int p)
      throws CipherException {

    // Keep create() symmetric with validate()/decrypt(): reject KDF cost
    // parameters that the decryption path would refuse, so create() can
    // never produce a wallet file that only this class rejects.
    String paramError = scryptKdfParamsError(DKLEN, n, R, p);
    if (paramError != null) {
      throw new CipherException(paramError);
    }

    byte[] salt = generateRandomBytes(32);

    byte[] derivedKey = generateDerivedScryptKey(password.getBytes(UTF_8), salt, n, R, p, DKLEN);

    byte[] encryptKey = Arrays.copyOfRange(derivedKey, 0, 16);
    byte[] iv = generateRandomBytes(16);

    byte[] privateKeyBytes = sign.getPrivateKey();

    byte[] cipherText = performCipherOperation(Cipher.ENCRYPT_MODE, iv, encryptKey,
        privateKeyBytes);

    byte[] mac = generateMac(derivedKey, cipherText);

    return createWalletFile(sign, cipherText, iv, salt, mac, n, p);
  }

  public static WalletFile createStandard(String password, SignInterface cryptoEngine)
      throws CipherException {
    return create(password, cryptoEngine, N_STANDARD, P_STANDARD);
  }

  public static WalletFile createLight(String password, SignInterface cryptoEngine)
      throws CipherException {
    return create(password, cryptoEngine, N_LIGHT, P_LIGHT);
  }

  private static WalletFile createWalletFile(
      SignInterface ecKeyPair, byte[] cipherText, byte[] iv, byte[] salt, byte[] mac,
      int n, int p) {

    WalletFile walletFile = new WalletFile();
    walletFile.setAddress(StringUtil.encode58Check(ecKeyPair.getAddress()));

    WalletFile.Crypto crypto = new WalletFile.Crypto();
    crypto.setCipher(CIPHER);
    crypto.setCiphertext(ByteArray.toHexString(cipherText));
    walletFile.setCrypto(crypto);

    WalletFile.CipherParams cipherParams = new WalletFile.CipherParams();
    cipherParams.setIv(ByteArray.toHexString(iv));
    crypto.setCipherparams(cipherParams);

    crypto.setKdf(SCRYPT);
    WalletFile.ScryptKdfParams kdfParams = new WalletFile.ScryptKdfParams();
    kdfParams.setDklen(DKLEN);
    kdfParams.setN(n);
    kdfParams.setP(p);
    kdfParams.setR(R);
    kdfParams.setSalt(ByteArray.toHexString(salt));
    crypto.setKdfparams(kdfParams);

    crypto.setMac(ByteArray.toHexString(mac));
    walletFile.setCrypto(crypto);
    walletFile.setId(UUID.randomUUID().toString());
    walletFile.setVersion(CURRENT_VERSION);

    return walletFile;
  }

  private static byte[] generateDerivedScryptKey(
      byte[] password, byte[] salt, int n, int r, int p, int dkLen) throws CipherException {
    return SCrypt.generate(password, salt, n, r, p, dkLen);
  }

  private static byte[] generateAes128CtrDerivedKey(
      byte[] password, byte[] salt, int c, String prf) throws CipherException {

    if (!"hmac-sha256".equals(prf)) {
      throw new CipherException("Unsupported prf:" + prf);
    }

    // Java 8 supports this, but you have to convert the password to a character array, see
    // http://stackoverflow.com/a/27928435/3211687

    PKCS5S2ParametersGenerator gen = new PKCS5S2ParametersGenerator(new SHA256Digest());
    gen.init(password, salt, c);
    return ((KeyParameter) gen.generateDerivedParameters(256)).getKey();
  }

  private static byte[] performCipherOperation(
      int mode, byte[] iv, byte[] encryptKey, byte[] text) throws CipherException {

    try {
      IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
      Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");

      SecretKeySpec secretKeySpec = new SecretKeySpec(encryptKey, "AES");
      cipher.init(mode, secretKeySpec, ivParameterSpec);
      return cipher.doFinal(text);
    } catch (NoSuchPaddingException | NoSuchAlgorithmException
        | InvalidAlgorithmParameterException | InvalidKeyException
        | BadPaddingException | IllegalBlockSizeException e) {
      throw new CipherException("Error performing cipher operation", e);
    }
  }

  private static byte[] generateMac(byte[] derivedKey, byte[] cipherText) {
    byte[] result = new byte[16 + cipherText.length];

    System.arraycopy(derivedKey, 16, result, 0, 16);
    System.arraycopy(cipherText, 0, result, 16, cipherText.length);

    return Hash.sha3(result);
  }

  public static SignInterface decrypt(String password, WalletFile walletFile,
      boolean ecKey) throws CipherException {

    validate(walletFile);

    WalletFile.Crypto crypto = walletFile.getCrypto();

    byte[] mac = ByteArray.fromHexString(crypto.getMac());
    byte[] iv = ByteArray.fromHexString(crypto.getCipherparams().getIv());
    byte[] cipherText = ByteArray.fromHexString(crypto.getCiphertext());

    byte[] derivedKey;

    WalletFile.KdfParams kdfParams = crypto.getKdfparams();
    if (kdfParams instanceof WalletFile.ScryptKdfParams) {
      WalletFile.ScryptKdfParams scryptKdfParams =
          (WalletFile.ScryptKdfParams) crypto.getKdfparams();
      int dklen = scryptKdfParams.getDklen();
      int n = scryptKdfParams.getN();
      int p = scryptKdfParams.getP();
      int r = scryptKdfParams.getR();
      byte[] salt = ByteArray.fromHexString(scryptKdfParams.getSalt());
      try {
        derivedKey = generateDerivedScryptKey(password.getBytes(UTF_8), salt, n, r, p, dklen);
      } catch (RuntimeException e) {
        // Defense-in-depth: any residual KDF engine failure (e.g. an
        // IllegalArgumentException from Bouncy Castle) must surface as a
        // clean CipherException, never as a raw RuntimeException stack.
        // Errors (OutOfMemoryError etc.) are deliberately not caught —
        // they remain fatal.
        throw new CipherException("Scrypt key derivation failed", e);
      }
    } else if (kdfParams instanceof WalletFile.Aes128CtrKdfParams) {
      WalletFile.Aes128CtrKdfParams aes128CtrKdfParams =
          (WalletFile.Aes128CtrKdfParams) crypto.getKdfparams();
      int c = aes128CtrKdfParams.getC();
      String prf = aes128CtrKdfParams.getPrf();
      byte[] salt = ByteArray.fromHexString(aes128CtrKdfParams.getSalt());

      try {
        derivedKey = generateAes128CtrDerivedKey(password.getBytes(UTF_8), salt, c, prf);
      } catch (RuntimeException e) {
        throw new CipherException("Pbkdf2 key derivation failed", e);
      }
    } else {
      throw new CipherException("Unable to deserialize params: " + crypto.getKdf());
    }

    byte[] derivedMac = generateMac(derivedKey, cipherText);

    if (!java.security.MessageDigest.isEqual(derivedMac, mac)) {
      throw new CipherException("Invalid password provided");
    }

    byte[] encryptKey = Arrays.copyOfRange(derivedKey, 0, 16);
    byte[] privateKey = performCipherOperation(Cipher.DECRYPT_MODE, iv, encryptKey, cipherText);

    SignInterface keyPair = SignUtils.fromPrivate(privateKey, ecKey);

    // Enforce address consistency: if the keystore declares an address, it MUST match
    // the address derived from the decrypted private key. Prevents address spoofing
    // where a crafted keystore displays one address but encrypts a different key.
    String declared = walletFile.getAddress();
    if (declared != null && !declared.isEmpty()) {
      String derived = StringUtil.encode58Check(keyPair.getAddress());
      if (!declared.equals(derived)) {
        throw new CipherException(
            "Keystore address mismatch: file declares " + declared
                + " but private key derives " + derived);
      }
    }

    return keyPair;
  }

  /**
   * Returns a description of the first schema violation found in
   * {@code walletFile}, or {@code null} if the file matches the supported
   * V3 keystore shape (current version, known cipher, known KDF, and KDF
   * cost parameters within safe bounds).
   *
   * <p>KDF cost bounds are enforced here — the single chokepoint shared by
   * {@link #validate(WalletFile)} (which throws the message) and
   * {@link #isValidKeystoreFile(WalletFile)} (which returns boolean for
   * discovery-style filtering) — so untrusted keystore files can never
   * drive the KDF into unbounded memory allocation, multi-hour CPU
   * exhaustion, or engine-level crashes before {@link #decrypt} runs.
   */
  private static String validationError(WalletFile walletFile) {
    if (walletFile.getVersion() != CURRENT_VERSION) {
      return "Wallet version is not supported";
    }
    WalletFile.Crypto crypto = walletFile.getCrypto();
    if (crypto == null) {
      return "Missing crypto section";
    }
    String cipher = crypto.getCipher();
    if (cipher == null || !cipher.equals(CIPHER)) {
      return "Wallet cipher is not supported";
    }
    String kdf = crypto.getKdf();
    if (kdf == null || (!kdf.equals(PBKDF2) && !kdf.equals(SCRYPT))) {
      return "KDF type is not supported";
    }
    WalletFile.KdfParams kdfParams = crypto.getKdfparams();
    if (kdfParams == null) {
      // Structural stubs (e.g. tooling fixtures) may omit kdfparams entirely;
      // there are no cost parameters to bound. Real keystores always carry them.
      return null;
    }
    if (SCRYPT.equals(kdf)) {
      if (!(kdfParams instanceof WalletFile.ScryptKdfParams)) {
        return "Scrypt KDF params are missing or malformed";
      }
      WalletFile.ScryptKdfParams params = (WalletFile.ScryptKdfParams) kdfParams;
      return scryptKdfParamsError(params.getDklen(), params.getN(), params.getR(), params.getP());
    }
    if (!(kdfParams instanceof WalletFile.Aes128CtrKdfParams)) {
      return "Pbkdf2 KDF params are missing or malformed";
    }
    WalletFile.Aes128CtrKdfParams params = (WalletFile.Aes128CtrKdfParams) kdfParams;
    return pbkdf2KdfParamsError(params.getDklen(), params.getC());
  }

  private static String scryptKdfParamsError(int dklen, int n, int r, int p) {
    // dklen must yield at least 32 bytes: generateMac copies derivedKey[16..32)
    // and AES-128 needs bytes [0..16); smaller keys throw raw ArrayIndexOutOfBoundsException.
    if (dklen < 32 || dklen > KDF_DKLEN_MAX) {
      return "Scrypt dklen is out of range [32, " + KDF_DKLEN_MAX + "]: " + dklen;
    }
    if (n < SCRYPT_N_MIN || n > SCRYPT_N_MAX || (n & (n - 1)) != 0) {
      return "Scrypt n must be a power of 2 in [" + SCRYPT_N_MIN + ", " + SCRYPT_N_MAX
          + "]: " + n;
    }
    if (r < 1 || r > SCRYPT_R_MAX) {
      return "Scrypt r is out of range [1, " + SCRYPT_R_MAX + "]: " + r;
    }
    if (p < 1 || p > SCRYPT_P_MAX) {
      return "Scrypt p is out of range [1, " + SCRYPT_P_MAX + "]: " + p;
    }
    // Compute in long arithmetic: hostile r * n must not overflow int.
    long memoryBytes = 128L * r * n;
    if (memoryBytes > SCRYPT_MEMORY_MAX_BYTES) {
      return "Scrypt memory requirement 128*r*n=" + memoryBytes
          + " exceeds the " + SCRYPT_MEMORY_MAX_BYTES + " byte limit";
    }
    return null;
  }

  private static String pbkdf2KdfParamsError(int dklen, int c) {
    if (dklen < 1 || dklen > KDF_DKLEN_MAX) {
      return "Pbkdf2 dklen is out of range [1, " + KDF_DKLEN_MAX + "]: " + dklen;
    }
    if (c < 1 || c > PBKDF2_C_MAX) {
      return "Pbkdf2 iteration count c is out of range [1, " + PBKDF2_C_MAX + "]: " + c;
    }
    return null;
  }

  static void validate(WalletFile walletFile) throws CipherException {
    String error = validationError(walletFile);
    if (error != null) {
      throw new CipherException(error);
    }
  }

  /**
   * Returns {@code true} iff {@code walletFile} has the shape of a
   * decryptable V3 keystore: non-null address, supported version, non-null
   * crypto section with a supported cipher and KDF. Intended for
   * discovery-style filtering (e.g. listing or duplicate detection) where
   * we want to skip JSON stubs that would later fail {@link #validate}.
   */
  public static boolean isValidKeystoreFile(WalletFile walletFile) {
    return walletFile != null
        && walletFile.getAddress() != null
        && validationError(walletFile) == null;
  }

  public static byte[] generateRandomBytes(int size) {
    byte[] bytes = new byte[size];
    new SecureRandom().nextBytes(bytes);
    return bytes;
  }
}
