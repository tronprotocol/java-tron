package org.tron.keystore;

import static java.nio.charset.StandardCharsets.UTF_8;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mockStatic;

import java.util.concurrent.atomic.AtomicReference;
import org.bouncycastle.crypto.generators.SCrypt;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.tron.common.crypto.SignInterface;
import org.tron.common.crypto.SignUtils;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.exception.CipherException;

/**
 * Verifies that Wallet.decrypt rejects keystores whose declared address
 * does not match the address derived from the decrypted private key,
 * preventing address-spoofing attacks.
 */
public class WalletAddressValidationTest {

  @Test
  public void testDecryptAcceptsMatchingAddress() throws Exception {
    String password = "test123456";
    SignInterface keyPair = SignUtils.getGeneratedRandomSign(Utils.getRandom());
    WalletFile walletFile = Wallet.createStandard(password, keyPair);

    // createStandard sets the correct derived address — should decrypt fine
    SignInterface recovered = Wallet.decrypt(password, walletFile);
    assertEquals("Private key must match",
        org.tron.common.utils.ByteArray.toHexString(keyPair.getPrivateKey()),
        org.tron.common.utils.ByteArray.toHexString(recovered.getPrivateKey()));
  }

  @Test
  public void testDecryptRejectsSpoofedAddress() throws Exception {
    String password = "test123456";
    SignInterface keyPair = SignUtils.getGeneratedRandomSign(Utils.getRandom());
    WalletFile walletFile = Wallet.createStandard(password, keyPair);

    // Tamper with the address to simulate a spoofed keystore
    walletFile.setAddress("TTamperedAddressXXXXXXXXXXXXXXXXXX");

    try {
      Wallet.decrypt(password, walletFile);
      fail("Expected CipherException due to address mismatch");
    } catch (CipherException e) {
      assertTrue("Error should mention address mismatch, got: " + e.getMessage(),
          e.getMessage().contains("address mismatch"));
    }
  }

  @Test
  public void testDecryptAllowsNullAddress() throws Exception {
    // Ethereum-style keystores may not include the address field — should still decrypt
    String password = "test123456";
    SignInterface keyPair = SignUtils.getGeneratedRandomSign(Utils.getRandom());
    WalletFile walletFile = Wallet.createStandard(password, keyPair);
    walletFile.setAddress(null);

    SignInterface recovered = Wallet.decrypt(password, walletFile);
    assertNotNull(recovered);
    assertEquals(org.tron.common.utils.ByteArray.toHexString(keyPair.getPrivateKey()),
        org.tron.common.utils.ByteArray.toHexString(recovered.getPrivateKey()));
  }

  @Test
  public void testDecryptAllowsEmptyAddress() throws Exception {
    String password = "test123456";
    SignInterface keyPair = SignUtils.getGeneratedRandomSign(Utils.getRandom());
    WalletFile walletFile = Wallet.createStandard(password, keyPair);
    walletFile.setAddress("");

    // Empty-string address is treated as absent (no validation)
    SignInterface recovered = Wallet.decrypt(password, walletFile);
    assertNotNull(recovered);
  }

  @Test
  public void testDecryptClearsTemporaryKeys() throws Exception {
    String password = "test123456";
    SignInterface keyPair = SignUtils.getGeneratedRandomSign(Utils.getRandom());
    WalletFile walletFile = Wallet.createLight(password, keyPair);
    byte[] expectedPrivateKey = keyPair.getPrivateKey();
    byte[] derivedKey = deriveKey(password, walletFile);
    AtomicReference<byte[]> decryptedPrivateKey = new AtomicReference<>();

    try (MockedStatic<SCrypt> mockedScrypt = mockStatic(SCrypt.class);
        MockedStatic<SignUtils> mockedSignUtils = mockStatic(SignUtils.class)) {
      mockedScrypt.when(() -> SCrypt.generate(any(byte[].class), any(byte[].class),
          anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(derivedKey);
      mockedSignUtils.when(() -> SignUtils.fromPrivate(any(byte[].class)))
          .thenAnswer(invocation -> {
            byte[] privateKey = invocation.getArgument(0);
            assertArrayEquals(expectedPrivateKey, privateKey);
            decryptedPrivateKey.set(privateKey);
            return invocation.callRealMethod();
          });

      SignInterface recovered = Wallet.decrypt(password, walletFile);
      assertArrayEquals(expectedPrivateKey, recovered.getPrivateKey());
      assertArrayEquals(keyPair.getAddress(), recovered.getAddress());
    }

    assertArrayEquals(new byte[derivedKey.length], derivedKey);
    byte[] clearedPrivateKey = decryptedPrivateKey.get();
    assertNotNull(clearedPrivateKey);
    assertArrayEquals(new byte[clearedPrivateKey.length], clearedPrivateKey);
  }

  @Test
  public void testDecryptWrapsInvalidPrivateKey() throws Exception {
    String password = "test123456";
    SignInterface keyPair = SignUtils.getGeneratedRandomSign(Utils.getRandom());
    WalletFile walletFile = Wallet.createLight(password, keyPair);
    byte[] expectedPrivateKey = keyPair.getPrivateKey();
    AtomicReference<byte[]> decryptedPrivateKey = new AtomicReference<>();

    try (MockedStatic<SignUtils> mockedSignUtils = mockStatic(SignUtils.class)) {
      mockedSignUtils.when(() -> SignUtils.fromPrivate(any(byte[].class)))
          .thenAnswer(invocation -> {
            byte[] privateKey = invocation.getArgument(0);
            assertArrayEquals(expectedPrivateKey, privateKey);
            decryptedPrivateKey.set(privateKey);
            throw new IllegalArgumentException("Invalid private key");
          });

      CipherException exception = assertThrows(CipherException.class,
          () -> Wallet.decrypt(password, walletFile));
      assertTrue(exception.getMessage().contains("Invalid private key"));
      assertTrue(exception.getCause() instanceof IllegalArgumentException);
    }

    byte[] clearedPrivateKey = decryptedPrivateKey.get();
    assertNotNull(clearedPrivateKey);
    assertArrayEquals(new byte[clearedPrivateKey.length], clearedPrivateKey);
  }

  @Test
  public void testDecryptClearsDerivedKeyOnIncorrectPassword() throws Exception {
    String password = "test123456";
    SignInterface keyPair = SignUtils.getGeneratedRandomSign(Utils.getRandom());
    WalletFile walletFile = Wallet.createLight(password, keyPair);
    byte[] derivedKey = deriveKey("wrong", walletFile);

    try (MockedStatic<SCrypt> mockedScrypt = mockStatic(SCrypt.class)) {
      mockedScrypt.when(() -> SCrypt.generate(any(byte[].class), any(byte[].class),
          anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(derivedKey);
      CipherException exception = assertThrows(CipherException.class,
          () -> Wallet.decrypt("wrong", walletFile));
      assertEquals("Invalid password provided", exception.getMessage());
    }

    assertArrayEquals(new byte[derivedKey.length], derivedKey);
  }

  private static byte[] deriveKey(String password, WalletFile walletFile) {
    WalletFile.ScryptKdfParams params =
        (WalletFile.ScryptKdfParams) walletFile.getCrypto().getKdfparams();
    return SCrypt.generate(password.getBytes(UTF_8), ByteArray.fromHexString(params.getSalt()),
        params.getN(), params.getR(), params.getP(), params.getDklen());
  }
}
