package org.tron.keystore;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.crypto.SignUtils;
import org.tron.core.exception.CipherException;

@Slf4j
public class WalletFileTest {

  @Test
  public void testGetAddress() throws NoSuchAlgorithmException, CipherException {
    WalletFile walletFile1 = Wallet.createStandard("", SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"),true));
    WalletFile walletFile2 = Wallet.createStandard("", SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"),true));
    WalletFile walletFile3 = (WalletFile) getSame(walletFile1);
    Assert.assertNotEquals(walletFile1.getAddress(), walletFile2.getAddress());
    Assert.assertNotEquals(walletFile1.getCrypto(), walletFile2.getCrypto());
    Assert.assertNotEquals(walletFile1.getId(), walletFile2.getId());
    Assert.assertEquals(walletFile1.getVersion(), walletFile2.getVersion());
    Assert.assertNotEquals(walletFile1, walletFile2);
    Assert.assertEquals(walletFile1, walletFile3);
    Assert.assertNotEquals(walletFile1, null);
    Assert.assertNotEquals(walletFile2, new Object());
    Assert.assertNotEquals(0, walletFile1.hashCode());

    WalletFile.CipherParams cipherParams = new WalletFile.CipherParams();
    WalletFile.CipherParams cipherParams1 = new WalletFile.CipherParams();
    WalletFile.CipherParams cipherParams2 = (WalletFile.CipherParams) getSame(cipherParams);
    Assert.assertEquals(cipherParams, cipherParams1);
    Assert.assertEquals(cipherParams, cipherParams2);
    Assert.assertNotEquals(cipherParams, null);
    Assert.assertNotEquals(cipherParams, new Object());
    Assert.assertEquals(0, cipherParams.hashCode());

    WalletFile.Aes128CtrKdfParams aes128CtrKdfParams = new WalletFile.Aes128CtrKdfParams();
    WalletFile.Aes128CtrKdfParams aes128CtrKdfParams1 = new WalletFile.Aes128CtrKdfParams();
    WalletFile.Aes128CtrKdfParams aes128CtrKdfParams2 = (WalletFile.Aes128CtrKdfParams)
            getSame(aes128CtrKdfParams);
    Assert.assertEquals(aes128CtrKdfParams, aes128CtrKdfParams1);
    Assert.assertEquals(aes128CtrKdfParams, aes128CtrKdfParams2);
    Assert.assertNotEquals(aes128CtrKdfParams, null);
    Assert.assertNotEquals(aes128CtrKdfParams, new Object());
    Assert.assertEquals(0, aes128CtrKdfParams.hashCode());

    WalletFile.ScryptKdfParams scryptKdfParams = new WalletFile.ScryptKdfParams();
    WalletFile.ScryptKdfParams scryptKdfParams1 = new WalletFile.ScryptKdfParams();
    WalletFile.ScryptKdfParams scryptKdfParams2 = (WalletFile.ScryptKdfParams)
            getSame(scryptKdfParams);
    Assert.assertEquals(scryptKdfParams, scryptKdfParams1);
    Assert.assertEquals(scryptKdfParams, scryptKdfParams2);
    Assert.assertNotEquals(scryptKdfParams, null);
    Assert.assertNotEquals(scryptKdfParams, new Object());
    Assert.assertEquals(0, scryptKdfParams.hashCode());

    WalletFile.Crypto crypto = new WalletFile.Crypto();
    WalletFile.Crypto crypto1 = new WalletFile.Crypto();
    WalletFile.Crypto crypto2 = (WalletFile.Crypto) getSame(crypto);
    Assert.assertEquals(crypto, crypto1);
    Assert.assertEquals(crypto, crypto2);
    Assert.assertNotEquals(crypto, null);
    Assert.assertNotEquals(crypto, new Object());
    Assert.assertEquals(0, crypto.hashCode());

  }

  private Object getSame(Object obj) {
    return obj;
  }

}