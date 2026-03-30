package org.tron.keystore;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.tron.common.crypto.SignInterface;
import org.tron.common.crypto.SignUtils;
import org.tron.common.crypto.sm2.SM2;
import org.tron.common.utils.ByteUtil;

public class CredentialsTest {

  @Test
  public void testCreate() throws NoSuchAlgorithmException {
    Credentials credentials = Credentials.create(SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"), true));
    Assert.assertNotNull("Credentials address create failed!", credentials.getAddress());
    Assert.assertFalse("Credentials address create failed!", credentials.getAddress().isEmpty());
    Assert.assertNotNull("Credentials cryptoEngine create failed", credentials.getSignInterface());
  }

  @Test
  public void testCreateFromSM2() {
    try {
      Credentials.create(SM2.fromNodeId(ByteUtil.hexToBytes("fffffffffff"
          + "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"
          + "fffffffffffffffffffffffffffffffffffffff")));
      Assert.fail("Expected IllegalArgumentException");
    } catch (Exception e) {
      Assert.assertTrue(e instanceof IllegalArgumentException);
    }
  }

  @Test
  public void testEquals() throws NoSuchAlgorithmException {
    Credentials credentials1 = Credentials.create(SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"), true));
    Credentials credentials2 = Credentials.create(SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"), true));
    Assert.assertNotEquals("Credentials instance should be not equal!",
        credentials1, credentials2);
    Assert.assertNotEquals("Credentials instance hashcode should be not equal!",
        credentials1.hashCode(), credentials2.hashCode());
  }

  @Test
  public void testEqualsWithAddressAndCryptoEngine() {
    Object aObject = new Object();
    SignInterface signInterface = Mockito.mock(SignInterface.class);
    SignInterface signInterface2 = Mockito.mock(SignInterface.class);
    SignInterface signInterface3 = Mockito.mock(SignInterface.class);
    byte[] address = "TQhZ7W1RudxFdzJMw6FvMnujPxrS6sFfmj".getBytes();
    byte[] address2 = "TNCmcTdyrYKMtmE1KU2itzeCX76jGm5Not".getBytes();
    Mockito.when(signInterface.getAddress()).thenReturn(address);
    Mockito.when(signInterface2.getAddress()).thenReturn(address);
    Mockito.when(signInterface3.getAddress()).thenReturn(address2);

    Credentials credential = Credentials.create(signInterface);
    Credentials sameCredential = Credentials.create(signInterface);
    Credentials sameAddressDifferentEngineCredential = Credentials.create(signInterface2);
    Credentials differentCredential = Credentials.create(signInterface3);

    Assert.assertFalse(aObject.equals(credential));
    Assert.assertFalse(credential.equals(aObject));
    Assert.assertFalse(credential.equals(null));
    Assert.assertEquals(credential, sameCredential);
    Assert.assertNotEquals(credential, sameAddressDifferentEngineCredential);
    Assert.assertFalse(credential.equals(differentCredential));
  }

}
