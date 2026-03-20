package org.tron.keystore;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import junit.framework.TestCase;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.util.Assert;
import org.tron.common.crypto.SignUtils;

@Slf4j
public class CredentialsTest extends TestCase {

  @Test
  public void testCreate() throws NoSuchAlgorithmException {
    Credentials credentials = Credentials.create(SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG")));
    Assert.hasText(credentials.getAddress(),"Credentials address create failed!");
    Assert.notNull(credentials.getSignInterface(),
        "Credentials cryptoEngine create failed");
  }

  @Test
  public void testEquals() throws NoSuchAlgorithmException {
    Credentials credentials1 = Credentials.create(SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG")));
    Credentials credentials2 = Credentials.create(SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG")));
    Assert.isTrue(!credentials1.equals(credentials2),
        "Credentials instance should be not equal!");
    Assert.isTrue(!(credentials1.hashCode() == credentials2.hashCode()),
        "Credentials instance hashcode should be not equal!");
  }

}