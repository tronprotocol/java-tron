package org.tron.common.crypto;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;
import org.tron.common.crypto.sm2.SM2;
import org.tron.common.utils.PublicMethod;


@Slf4j
public class SignatureInterfaceTest {

  private String SM2_privString = PublicMethod.getSM2RandomPrivateKey();
  private byte[] SM2_privateKey = Hex.decode(SM2_privString);

  private String SM2_pubString = PublicMethod.getSM2PublicByPrivateKey(SM2_privString);
  private byte[] SM2_pubKey = Hex.decode(SM2_pubString);
  private String SM2_address = PublicMethod.getSM2AddressByPrivateKey(SM2_privString);

  private String EC_privString = PublicMethod.getRandomPrivateKey();
  private byte[] EC_privateKey = Hex.decode(EC_privString);

  private String EC_pubString = PublicMethod.getPublicByPrivateKey(EC_privString);
  private byte[] EC_pubKey = Hex.decode(EC_pubString);
  private String EC_address = PublicMethod.getHexAddressByPrivateKey(EC_privString);



  @Test
  public void testContructor() {
    SignInterface sign = new SM2();
    logger.info(Hex.toHexString(sign.getPrivateKey()) + " :SM2 Generated privkey");
    logger.info(Hex.toHexString(sign.getPubKey()) + " :SM2 Generated pubkey");

    sign = new ECKey();
    logger.info(Hex.toHexString(sign.getPrivateKey()) + " :ECDSA Generated privkey");
    logger.info(Hex.toHexString(sign.getPubKey()) + " :ECDSA Generated pubkey");
  }

  @Test
  public void testPirvateKey() {
    SignInterface sign = new SM2(SM2_privateKey, true);
    assertArrayEquals(sign.getPubKey(), SM2_pubKey);

    sign = new ECKey(EC_privateKey, true);
    assertArrayEquals(sign.getPubKey(), EC_pubKey);

  }

  @Test
  public void testPublicKey() {
    SignInterface sign = new SM2(SM2_pubKey, false);
    assertArrayEquals(sign.getPubKey(), SM2_pubKey);

    sign = new ECKey(EC_pubKey, false);
    assertArrayEquals(sign.getPubKey(), EC_pubKey);
  }

  @Test
  public void testNullKey() {
    SignInterface sign = new SM2(SM2_pubKey, false);
    assertEquals(null, sign.getPrivateKey());

    sign = new ECKey(EC_pubKey, false);
    assertEquals(null, sign.getPrivateKey());
  }

  @Test
  public void testAddress() {
    SignInterface sign = new SM2(SM2_pubKey, false);
    byte[] prefix_address = sign.getAddress();
    byte[] address = Arrays.copyOfRange(prefix_address, 1, prefix_address.length);
    byte[] addressTmp = Arrays.copyOfRange(Hex.decode(SM2_address), 1, prefix_address.length);
    assertEquals(Hex.toHexString(addressTmp), Hex.toHexString(address));
    sign = new ECKey(EC_pubKey, false);
    prefix_address = sign.getAddress();
    address = Arrays.copyOfRange(prefix_address, 1, prefix_address.length);
    byte[] ecAddressTmp = Arrays.copyOfRange(Hex.decode(EC_address), 1, prefix_address.length);
    assertEquals(Hex.toHexString(ecAddressTmp), Hex.toHexString(address));
  }
}
