package org.tron.keystore;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import junit.framework.TestCase;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.crypto.SignUtils;
import org.tron.core.exception.CipherException;

@Slf4j
public class WalletFileTest extends TestCase {


  @Test
  public void testGetAddress() throws NoSuchAlgorithmException, CipherException {
    WalletFile walletFile1 = Wallet.createStandard("", SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"),true));
    WalletFile walletFile2 = Wallet.createStandard("", SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"),true));
    Assert.assertTrue(!walletFile1.getAddress().equals(walletFile2.getAddress()));
    Assert.assertTrue(!walletFile1.getCrypto().equals(walletFile2.getCrypto()));
    Assert.assertTrue(!walletFile1.getId().equals(walletFile2.getId()));
    Assert.assertTrue(walletFile1.getVersion() == walletFile2.getVersion());
  }

}