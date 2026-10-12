package org.tron.keystore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.tron.common.crypto.SignInterface;
import org.tron.common.crypto.SignUtils;
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

}
