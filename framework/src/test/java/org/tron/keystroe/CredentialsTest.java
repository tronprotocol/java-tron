package org.tron.keystroe;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.tron.common.crypto.SignInterface;
import org.tron.keystore.Credentials;

public class CredentialsTest {

  @Test
  public void test_equality() {
    Object aObject = new Object();
    SignInterface si = Mockito.mock(SignInterface.class);
    SignInterface si2 = Mockito.mock(SignInterface.class);
    SignInterface si3 = Mockito.mock(SignInterface.class);
    byte[] address = "TQhZ7W1RudxFdzJMw6FvMnujPxrS6sFfmj".getBytes();
    byte[] address2 = "TNCmcTdyrYKMtmE1KU2itzeCX76jGm5Not".getBytes();
    Mockito.when(si.getAddress()).thenReturn(address);
    Mockito.when(si2.getAddress()).thenReturn(address);
    Mockito.when(si3.getAddress()).thenReturn(address2);
    Credentials aCredential = Credentials.create(si);
    Assert.assertFalse(aObject.equals(aCredential));
    Assert.assertFalse(aCredential.equals(aObject));
    Assert.assertFalse(aCredential.equals(null));
    Credentials anotherCredential = Credentials.create(si);
    Assert.assertTrue(aCredential.equals(anotherCredential));
    Credentials aCredential2 = Credentials.create(si2);
    Assert.assertTrue(aCredential.equals(anotherCredential));
    Credentials aCredential3 = Credentials.create(si3);
    Assert.assertFalse(aCredential.equals(aCredential3));
  }
}
