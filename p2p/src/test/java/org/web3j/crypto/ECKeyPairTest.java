package org.web3j.crypto;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import org.junit.Assert;
import org.junit.Test;
import org.web3j.utils.Numeric;

public class ECKeyPairTest {

  private static final BigInteger PRIVATE_KEY = new BigInteger(
      "a392604efc2fad9c0b3da43b5f698a2e3f270f170d859912be0d54742275c5f6", 16);

  @Test
  public void createFromBigIntegerBytesAndHexAgree() {
    ECKeyPair fromBigInteger = ECKeyPair.create(PRIVATE_KEY);
    ECKeyPair fromBytes =
        ECKeyPair.create(Numeric.toBytesPadded(PRIVATE_KEY, 32));

    Assert.assertEquals(PRIVATE_KEY, fromBigInteger.getPrivateKey());
    Assert.assertEquals(fromBigInteger, fromBytes);
    Assert.assertEquals(fromBigInteger.hashCode(), fromBytes.hashCode());
  }

  @Test
  public void equalsAndHashCode() {
    ECKeyPair pair = ECKeyPair.create(PRIVATE_KEY);
    ECKeyPair same = ECKeyPair.create(PRIVATE_KEY);
    ECKeyPair other = ECKeyPair.create(PRIVATE_KEY.add(BigInteger.ONE));

    Assert.assertEquals(pair, pair);
    Assert.assertEquals(pair, same);
    Assert.assertNotEquals(pair, other);
    Assert.assertNotEquals(pair, null);
    Assert.assertNotEquals(pair, "not a key pair");
    Assert.assertNotEquals(pair.hashCode(), other.hashCode());

    ECKeyPair nulls = new ECKeyPair(null, null);
    Assert.assertEquals(new ECKeyPair(null, null), nulls);
    Assert.assertNotEquals(nulls, pair);
    Assert.assertEquals(0, nulls.hashCode());
  }

  @Test
  public void signProducesACanonicalSignature() {
    byte[] hash = Hash.sha3("hello world".getBytes(StandardCharsets.UTF_8));
    ECDSASignature signature = ECKeyPair.create(PRIVATE_KEY).sign(hash);

    Assert.assertTrue(signature.isCanonical());
    Assert.assertTrue(signature.r.signum() > 0);
    Assert.assertTrue(signature.s.signum() > 0);
  }
}
