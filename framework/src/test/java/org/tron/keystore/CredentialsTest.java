package org.tron.keystore;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import junit.framework.TestCase;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.util.Assert;
import org.tron.common.crypto.SignUtils;
import org.tron.common.crypto.sm2.SM2;
import org.tron.common.utils.ByteUtil;

@Slf4j
public class CredentialsTest extends TestCase {

  @Test
  public void testCreate() throws NoSuchAlgorithmException {
    Credentials credentials = Credentials.create(SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"),true));
    Assert.hasText(credentials.getAddress(),"Credentials address create failed!");
    Assert.notNull(credentials.getSignInterface(),
        "Credentials cryptoEngine create failed");
  }

  @Test
  public void testCreateFromSM2() {
    try {
      Credentials.create(SM2.fromNodeId(ByteUtil.hexToBytes("fffffffffff"
          + "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"
          + "fffffffffffffffffffffffffffffffffffffff")));
    } catch (Exception e) {
      Assert.isInstanceOf(IllegalArgumentException.class, e);
    }
  }

  @Test
  public void testEquals() throws NoSuchAlgorithmException {
    Credentials credentials1 = Credentials.create(SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"),true));
    Credentials credentials2 = Credentials.create(SignUtils.getGeneratedRandomSign(
        SecureRandom.getInstance("NativePRNG"),true));
    Assert.isTrue(!credentials1.equals(credentials2),
        "Credentials instance should be not equal!");
    Assert.isTrue(!(credentials1.hashCode() == credentials2.hashCode()),
        "Credentials instance hashcode should be not equal!");
  }

}