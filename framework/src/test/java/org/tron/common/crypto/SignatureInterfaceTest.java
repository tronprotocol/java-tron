package org.tron.common.crypto;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.crypto.sm2.SM2;


@Slf4j
public class SignatureInterfaceTest {

  private String SM2_privString = "128B2FA8BD433C6C068C8D803DFF79792A519A5517"
      + "1B1B650C23661D15897263";
  private byte[] SM2_privateKey = Hex.decode(SM2_privString);

  private String SM2_pubString = "04d5548c7825cbb56150a3506cd57464af8a1ae0519"
      + "dfaf3c58221dc810caf28dd921073768fe3d59ce54e79a49445cf73fed23086537"
      + "027264d168946d479533e";
  private String SM2_compressedPubString =
      "02d5548c7825cbb56150a3506cd57464af8a1ae0519dfaf3c58221dc810caf28dd";
  private byte[] SM2_pubKey = Hex.decode(SM2_pubString);
  private byte[] SM2_compressedPubKey = Hex.decode(SM2_compressedPubString);
  private String SM2_address = "62e49e4c2f4e3c0653a02f8859c1e6991b759e87";


  private String EC_privString = "c85ef7d79691fe79573b1a7064c19c1a9819ebdbd1faaab1a8ec92344438aaf4";
  private byte[] EC_privateKey = Hex.decode(EC_privString);

  private String EC_pubString = "040947751e3022ecf3016be03ec77ab0ce3c2662b4843898cb068d74f698ccc"
      + "8ad75aa17564ae80a20bb044ee7a6d903e8e8df624b089c95d66a0570f051e5a05b";
  private String EC_compressedPubString =
      "030947751e3022ecf3016be03ec77ab0ce3c2662b4843898cb068d74f6" + "98ccc8ad";
  private byte[] EC_pubKey = Hex.decode(EC_pubString);
  private byte[] EC_compressedPubKey = Hex.decode(EC_compressedPubString);
  private String EC_address = "cd2a3d9f938e13cd947ec05abc7fe734df8dd826";


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
    assertEquals(SM2_address, Hex.toHexString(address));

    sign = new ECKey(EC_pubKey, false);
    prefix_address = sign.getAddress();
    address = Arrays.copyOfRange(prefix_address, 1, prefix_address.length);
    assertEquals(EC_address, Hex.toHexString(address));
  }
}
