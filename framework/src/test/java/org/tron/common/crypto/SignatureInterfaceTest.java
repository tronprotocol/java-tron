package org.tron.common.crypto;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;
import org.tron.common.utils.PublicMethod;


@Slf4j
public class SignatureInterfaceTest {

  private String EC_privString = PublicMethod.getRandomPrivateKey();
  private byte[] EC_privateKey = Hex.decode(EC_privString);

  private String EC_pubString = PublicMethod.getPublicByPrivateKey(EC_privString);
  private byte[] EC_pubKey = Hex.decode(EC_pubString);
  private String EC_address = PublicMethod.getHexAddressByPrivateKey(EC_privString);

  @Test
  public void testContructor() {
    SignInterface sign = new ECKey();
    logger.info(Hex.toHexString(sign.getPrivateKey()) + " :ECDSA Generated privkey");
    logger.info(Hex.toHexString(sign.getPubKey()) + " :ECDSA Generated pubkey");
  }

  @Test
  public void testPirvateKey() {
    SignInterface sign = new ECKey(EC_privateKey, true);
    assertArrayEquals(sign.getPubKey(), EC_pubKey);
  }

  @Test
  public void testPublicKey() {
    SignInterface sign = new ECKey(EC_pubKey, false);
    assertArrayEquals(sign.getPubKey(), EC_pubKey);
  }

  @Test
  public void testNullKey() {
    SignInterface sign = new ECKey(EC_pubKey, false);
    assertEquals(null, sign.getPrivateKey());
  }

  @Test
  public void testAddress() {
    SignInterface sign = new ECKey(EC_pubKey, false);
    byte[] prefix_address = sign.getAddress();
    byte[] address = Arrays.copyOfRange(prefix_address, 1, prefix_address.length);
    byte[] ecAddressTmp = Arrays.copyOfRange(Hex.decode(EC_address), 1, prefix_address.length);
    assertEquals(Hex.toHexString(ecAddressTmp), Hex.toHexString(address));
  }
}
