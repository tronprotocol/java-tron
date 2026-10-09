package org.tron.common.crypto;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.security.SignatureException;
import java.util.Arrays;
import org.junit.Test;
import org.tron.common.crypto.ECKey.ECDSASignature;
import org.tron.common.crypto.sm2.SM2;

public class SignUtilsTest {

  @Test
  public void testValidEcdsaComponents() throws SignatureException {
    byte[] hash = new byte[32];
    ECKey key = ECKey.fromPrivate(BigInteger.TEN);
    ECDSASignature signature = key.sign(hash);

    assertArrayEquals(key.getAddress(), SignUtils.signatureToAddress(hash, signature, true));
    for (boolean strictValidation : new boolean[]{false, true}) {
      assertArrayEquals(key.getAddress(),
          SignUtils.signatureToAddress(hash, signature, true, strictValidation));
    }
  }

  @Test
  public void testValidSm2Components() throws SignatureException {
    byte[] hash = new byte[32];
    SM2 key = SM2.fromPrivate(BigInteger.TEN);
    SM2.SM2Signature signature = key.sign(hash);

    assertArrayEquals(key.getAddress(), SignUtils.signatureToAddress(hash, signature, false));
    for (boolean strictValidation : new boolean[]{false, true}) {
      assertArrayEquals(key.getAddress(),
          SignUtils.signatureToAddress(hash, signature, false, strictValidation));
    }
  }

  @Test
  public void testStrictScalarBounds() {
    byte[] hash = new byte[32];
    ECDSASignature valid = ECKey.fromPrivate(BigInteger.TEN).sign(hash);
    BigInteger order = ECKey.CURVE.getN();

    for (BigInteger scalar : Arrays.asList(BigInteger.ZERO, order, order.add(BigInteger.ONE))) {
      ECDSASignature invalidR = new ECDSASignature(scalar, valid.s);
      invalidR.v = valid.v;
      ECDSASignature invalidS = new ECDSASignature(valid.r, scalar);
      invalidS.v = valid.v;
      for (ECDSASignature signature : Arrays.asList(invalidR, invalidS)) {
        SignatureException componentError = assertThrows(SignatureException.class,
            () -> SignUtils.signatureToAddress(hash, signature, true, true));
        SignatureException base64Error = assertThrows(SignatureException.class,
            () -> SignUtils.signatureToAddress(hash, signature.toBase64(), true, true));
        assertTrue(componentError.getCause() instanceof IllegalArgumentException);
        assertTrue(base64Error.getCause() instanceof IllegalArgumentException);
        assertEquals(base64Error.getCause().getMessage(), componentError.getCause().getMessage());
      }
    }
  }

  @Test
  public void testNullComponents() {
    byte[] hash = new byte[32];
    for (ECDSASignature signature : Arrays.asList(null,
        new ECDSASignature(null, BigInteger.ONE), new ECDSASignature(BigInteger.ONE, null))) {
      SignatureException legacyError = assertThrows(SignatureException.class,
          () -> SignUtils.signatureToAddress(hash, signature, true));
      SignatureException strictError = assertThrows(SignatureException.class,
          () -> SignUtils.signatureToAddress(hash, signature, true, true));
      assertTrue(legacyError.getCause() instanceof IllegalArgumentException);
      assertTrue(strictError.getCause() instanceof IllegalArgumentException);
    }
  }

  @Test
  public void testInvalidHeader() {
    byte[] hash = new byte[32];
    ECDSASignature signature = new ECDSASignature(BigInteger.ONE, BigInteger.ONE);
    signature.v = 26;

    SignatureException error = assertThrows(SignatureException.class,
        () -> SignUtils.signatureToAddress(hash, signature, true, true));
    assertEquals("Header byte out of range: 26", error.getMessage());
    assertNull(error.getCause());
  }
}
