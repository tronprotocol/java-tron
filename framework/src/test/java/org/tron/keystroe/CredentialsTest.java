package org.tron.keystroe;

import lombok.var;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.tron.common.crypto.SignInterface;
import org.tron.keystore.Credentials;

public class CredentialsTest {

  @Test
  public void test_equality() {
    var aObject = new Object();
    var si = Mockito.mock(SignInterface.class);
    var si2 = Mockito.mock(SignInterface.class);
    var si3 = Mockito.mock(SignInterface.class);
    var address = "TQhZ7W1RudxFdzJMw6FvMnujPxrS6sFfmj".getBytes();
    var address2 = "TNCmcTdyrYKMtmE1KU2itzeCX76jGm5Not".getBytes();
    Mockito.when(si.getAddress()).thenReturn(address);
    Mockito.when(si2.getAddress()).thenReturn(address);
    Mockito.when(si3.getAddress()).thenReturn(address2);
    var aCredential = Credentials.create(si);
    Assert.assertFalse(aObject.equals(aCredential));
    Assert.assertFalse(aCredential.equals(aObject));
    Assert.assertFalse(aCredential.equals(null));
    var anotherCredential = Credentials.create(si);
    Assert.assertTrue(aCredential.equals(anotherCredential));            
    var aCredential2 = Credentials.create(si2);
    Assert.assertTrue(aCredential.equals(anotherCredential));
    var aCredential3 = Credentials.create(si3);
    Assert.assertFalse(aCredential.equals(aCredential3));
  }
}
