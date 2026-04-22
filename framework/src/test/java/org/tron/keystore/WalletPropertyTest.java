package org.tron.keystore;

import static org.junit.Assert.assertArrayEquals;

import java.security.SecureRandom;
import org.junit.Test;
import org.tron.common.crypto.SignInterface;
import org.tron.common.crypto.SignUtils;
import org.tron.common.utils.Utils;
import org.tron.core.exception.CipherException;

/**
 * Property-based roundtrip tests: decrypt(encrypt(privateKey, password)) == privateKey.
 * Uses randomized inputs via loop instead of jqwik to avoid dependency verification overhead.
 */
public class WalletPropertyTest {

  private static final SecureRandom RANDOM = new SecureRandom();
  private static final String CHARS =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

  @Test
  public void encryptDecryptRoundtripLight() throws Exception {
    for (int i = 0; i < 100; i++) {
      String password = randomPassword(6, 32);
      SignInterface keyPair = SignUtils.getGeneratedRandomSign(Utils.getRandom(), true);
      byte[] originalKey = keyPair.getPrivateKey();

      WalletFile walletFile = Wallet.createLight(password, keyPair);
      SignInterface recovered = Wallet.decrypt(password, walletFile, true);

      assertArrayEquals("Roundtrip failed at iteration " + i,
          originalKey, recovered.getPrivateKey());
    }
  }

  @Test(timeout = 120000)
  public void encryptDecryptRoundtripStandard() throws Exception {
    // Fewer iterations for standard scrypt (slow, ~10s each)
    for (int i = 0; i < 2; i++) {
      String password = randomPassword(6, 16);
      SignInterface keyPair = SignUtils.getGeneratedRandomSign(Utils.getRandom(), true);
      byte[] originalKey = keyPair.getPrivateKey();

      WalletFile walletFile = Wallet.createStandard(password, keyPair);
      SignInterface recovered = Wallet.decrypt(password, walletFile, true);

      assertArrayEquals("Standard roundtrip failed at iteration " + i,
          originalKey, recovered.getPrivateKey());
    }
  }

  @Test
  public void wrongPasswordFailsDecrypt() throws Exception {
    for (int i = 0; i < 50; i++) {
      String password = randomPassword(6, 16);
      SignInterface keyPair = SignUtils.getGeneratedRandomSign(Utils.getRandom(), true);
      WalletFile walletFile = Wallet.createLight(password, keyPair);

      try {
        Wallet.decrypt(password + "X", walletFile, true);
        throw new AssertionError("Expected CipherException at iteration " + i);
      } catch (CipherException e) {
        // Expected
      }
    }
  }

  private String randomPassword(int minLen, int maxLen) {
    int len = minLen + RANDOM.nextInt(maxLen - minLen + 1);
    StringBuilder sb = new StringBuilder(len);
    for (int i = 0; i < len; i++) {
      sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
    }
    return sb.toString();
  }
}
