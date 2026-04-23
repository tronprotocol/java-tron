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

  public static WalletFile create(String password, SignInterface sign, int n, int p)
      throws CipherException {

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
      derivedKey = generateDerivedScryptKey(password.getBytes(UTF_8), salt, n, r, p, dklen);
    } else if (kdfParams instanceof WalletFile.Aes128CtrKdfParams) {
      WalletFile.Aes128CtrKdfParams aes128CtrKdfParams =
          (WalletFile.Aes128CtrKdfParams) crypto.getKdfparams();
      int c = aes128CtrKdfParams.getC();
      String prf = aes128CtrKdfParams.getPrf();
      byte[] salt = ByteArray.fromHexString(aes128CtrKdfParams.getSalt());

      derivedKey = generateAes128CtrDerivedKey(password.getBytes(UTF_8), salt, c, prf);
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
   * V3 keystore shape (current version, known cipher, known KDF).
   *
   * <p>Shared by {@link #validate(WalletFile)} (which throws the message)
   * and {@link #isValidKeystoreFile(WalletFile)} (which returns boolean
   * for discovery-style filtering).
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
