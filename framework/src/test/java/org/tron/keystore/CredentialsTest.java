package org.tron.keystore;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.tron.common.crypto.SignInterface;
import org.tron.common.crypto.sm2.SM2;
import org.tron.common.utils.ByteUtil;
import org.tron.common.utils.StringUtil;

public class CredentialsTest {

  private static final byte[] ADDRESS_1 = ByteUtil.hexToBytes(
      "410102030405060708090a0b0c0d0e0f1011121314");
  private static final byte[] ADDRESS_2 = ByteUtil.hexToBytes(
      "411415161718191a1b1c1d1e1f2021222324252627");

  private SignInterface mockSignInterface(byte[] address) {
    SignInterface signInterface = Mockito.mock(SignInterface.class);
    Mockito.when(signInterface.getAddress()).thenReturn(address);
    return signInterface;
  }

  @Test
  public void testCreate() {
    SignInterface signInterface = mockSignInterface(ADDRESS_1);
    Credentials credentials = Credentials.create(signInterface);
    Assert.assertEquals("Credentials address create failed!",
        StringUtil.encode58Check(ADDRESS_1), credentials.getAddress());
    Assert.assertSame("Credentials cryptoEngine create failed", signInterface,
        credentials.getSignInterface());
  }

  @Test
  public void testCreateFromSM2() {
    Exception e = Assert.assertThrows(Exception.class,
        () -> Credentials.create(SM2.fromNodeId(ByteUtil.hexToBytes("fffffffffff"
            + "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"
            + "fffffffffffffffffffffffffffffffffffffff"))));
    Assert.assertTrue(e instanceof IllegalArgumentException);
  }

  @Test
  public void testEquals() {
    Credentials credentials1 = Credentials.create(mockSignInterface(ADDRESS_1));
    Credentials credentials2 = Credentials.create(mockSignInterface(ADDRESS_2));

    Assert.assertNotEquals("Credentials address fixtures should differ",
        credentials1.getAddress(), credentials2.getAddress());
    Assert.assertNotEquals("Credentials instance should be not equal!",
        credentials1, credentials2);
  }

  @Test
  public void testEqualsWithAddressAndCryptoEngine() {
    Object aObject = new Object();
    SignInterface signInterface = mockSignInterface(ADDRESS_1);
    SignInterface signInterface2 = mockSignInterface(ADDRESS_1);
    SignInterface signInterface3 = mockSignInterface(ADDRESS_2);

    Credentials credential = Credentials.create(signInterface);
    Credentials sameCredential = Credentials.create(signInterface);
    Credentials sameAddressDifferentEngineCredential = Credentials.create(signInterface2);
    Credentials differentCredential = Credentials.create(signInterface3);

    Assert.assertFalse(aObject.equals(credential));
    Assert.assertFalse(credential.equals(aObject));
    Assert.assertFalse(credential.equals(null));
    Assert.assertEquals(credential, sameCredential);
    Assert.assertEquals("Equal credentials must have the same hashCode",
        credential.hashCode(), sameCredential.hashCode());
    Assert.assertNotEquals(credential, sameAddressDifferentEngineCredential);
    Assert.assertFalse(credential.equals(differentCredential));
  }
}
