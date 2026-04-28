package org.tron.keystore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

public class WalletFilePojoTest {

  @Test
  public void testWalletFileGettersSetters() {
    WalletFile wf = new WalletFile();
    wf.setAddress("TAddr");
    wf.setId("uuid-123");
    wf.setVersion(3);
    WalletFile.Crypto c = new WalletFile.Crypto();
    wf.setCrypto(c);

    assertEquals("TAddr", wf.getAddress());
    assertEquals("uuid-123", wf.getId());
    assertEquals(3, wf.getVersion());
    assertEquals(c, wf.getCrypto());
  }

  @Test
  public void testWalletFileCryptoV1Setter() {
    WalletFile wf = new WalletFile();
    WalletFile.Crypto c = new WalletFile.Crypto();
    wf.setCryptoV1(c);
    assertEquals(c, wf.getCrypto());
  }

  @Test
  public void testWalletFileEqualsAllBranches() {
    WalletFile a = new WalletFile();
    a.setAddress("TAddr");
    a.setId("id1");
    a.setVersion(3);
    WalletFile.Crypto c = new WalletFile.Crypto();
    a.setCrypto(c);

    WalletFile b = new WalletFile();
    b.setAddress("TAddr");
    b.setId("id1");
    b.setVersion(3);
    b.setCrypto(c);

    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
    assertTrue(a.equals(a));
    assertFalse(a.equals(null));
    assertFalse(a.equals("string"));

    // Different address
    b.setAddress("TOther");
    assertNotEquals(a, b);
    b.setAddress("TAddr");

    // Different id
    b.setId("id2");
    assertNotEquals(a, b);
    b.setId("id1");

    // Different version
    b.setVersion(4);
    assertNotEquals(a, b);
    b.setVersion(3);

    // Different crypto
    b.setCrypto(new WalletFile.Crypto());
    // Still equal since Cryptos are equal (both empty)
    assertEquals(a, b);

    // Null fields
    WalletFile empty = new WalletFile();
    WalletFile empty2 = new WalletFile();
    assertEquals(empty, empty2);
    assertEquals(empty.hashCode(), empty2.hashCode());

    // One side null
    empty2.setAddress("X");
    assertNotEquals(empty, empty2);
  }

  @Test
  public void testCryptoGettersSetters() {
    WalletFile.Crypto c = new WalletFile.Crypto();
    c.setCipher("aes-128-ctr");
    c.setCiphertext("ciphertext");
    c.setKdf("scrypt");
    c.setMac("mac-value");

    WalletFile.CipherParams cp = new WalletFile.CipherParams();
    cp.setIv("ivvalue");
    c.setCipherparams(cp);

    WalletFile.ScryptKdfParams kp = new WalletFile.ScryptKdfParams();
    c.setKdfparams(kp);

    assertEquals("aes-128-ctr", c.getCipher());
    assertEquals("ciphertext", c.getCiphertext());
    assertEquals("scrypt", c.getKdf());
    assertEquals("mac-value", c.getMac());
    assertEquals(cp, c.getCipherparams());
    assertEquals(kp, c.getKdfparams());
  }

  @Test
  public void testCryptoEqualsAllBranches() {
    WalletFile.Crypto a = new WalletFile.Crypto();
    a.setCipher("c1");
    a.setCiphertext("txt");
    a.setKdf("kdf");
    a.setMac("mac");
    WalletFile.CipherParams cp = new WalletFile.CipherParams();
    cp.setIv("iv");
    a.setCipherparams(cp);
    WalletFile.Aes128CtrKdfParams kp = new WalletFile.Aes128CtrKdfParams();
    a.setKdfparams(kp);

    WalletFile.Crypto b = new WalletFile.Crypto();
    b.setCipher("c1");
    b.setCiphertext("txt");
    b.setKdf("kdf");
    b.setMac("mac");
    b.setCipherparams(cp);
    b.setKdfparams(kp);

    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
    assertTrue(a.equals(a));
    assertFalse(a.equals(null));
    assertFalse(a.equals("string"));

    // cipher differs
    b.setCipher("c2");
    assertNotEquals(a, b);
    b.setCipher("c1");

    // ciphertext differs
    b.setCiphertext("other");
    assertNotEquals(a, b);
    b.setCiphertext("txt");

    // kdf differs
    b.setKdf("other");
    assertNotEquals(a, b);
    b.setKdf("kdf");

    // mac differs
    b.setMac("other");
    assertNotEquals(a, b);
    b.setMac("mac");

    // cipherparams differs
    WalletFile.CipherParams cp2 = new WalletFile.CipherParams();
    cp2.setIv("other");
    b.setCipherparams(cp2);
    assertNotEquals(a, b);
    b.setCipherparams(cp);

    // kdfparams differs
    WalletFile.Aes128CtrKdfParams kp2 = new WalletFile.Aes128CtrKdfParams();
    kp2.setC(5);
    b.setKdfparams(kp2);
    assertNotEquals(a, b);
  }

  @Test
  public void testCryptoNullFields() {
    WalletFile.Crypto a = new WalletFile.Crypto();
    WalletFile.Crypto b = new WalletFile.Crypto();
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());

    a.setCipher("x");
    assertNotEquals(a, b);
  }

  @Test
  public void testCipherParamsGettersSetters() {
    WalletFile.CipherParams cp = new WalletFile.CipherParams();
    cp.setIv("ivvalue");
    assertEquals("ivvalue", cp.getIv());
  }

  @Test
  public void testCipherParamsEquals() {
    WalletFile.CipherParams a = new WalletFile.CipherParams();
    WalletFile.CipherParams b = new WalletFile.CipherParams();
    assertEquals(a, b);
    a.setIv("iv");
    assertNotEquals(a, b);
    b.setIv("iv");
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
    b.setIv("other");
    assertNotEquals(a, b);
    assertTrue(a.equals(a));
    assertFalse(a.equals(null));
    assertFalse(a.equals("string"));
  }

  @Test
  public void testAes128CtrKdfParamsAllAccessors() {
    WalletFile.Aes128CtrKdfParams p = new WalletFile.Aes128CtrKdfParams();
    p.setDklen(32);
    p.setC(262144);
    p.setPrf("hmac-sha256");
    p.setSalt("saltvalue");

    assertEquals(32, p.getDklen());
    assertEquals(262144, p.getC());
    assertEquals("hmac-sha256", p.getPrf());
    assertEquals("saltvalue", p.getSalt());
  }

  @Test
  public void testAes128CtrKdfParamsEquals() {
    WalletFile.Aes128CtrKdfParams a = new WalletFile.Aes128CtrKdfParams();
    a.setDklen(32);
    a.setC(262144);
    a.setPrf("hmac-sha256");
    a.setSalt("salt");

    WalletFile.Aes128CtrKdfParams b = new WalletFile.Aes128CtrKdfParams();
    b.setDklen(32);
    b.setC(262144);
    b.setPrf("hmac-sha256");
    b.setSalt("salt");

    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
    assertTrue(a.equals(a));
    assertFalse(a.equals(null));
    assertFalse(a.equals("string"));

    b.setDklen(64);
    assertNotEquals(a, b);
    b.setDklen(32);

    b.setC(1);
    assertNotEquals(a, b);
    b.setC(262144);

    b.setPrf("other");
    assertNotEquals(a, b);
    b.setPrf("hmac-sha256");

    b.setSalt("other");
    assertNotEquals(a, b);
    b.setSalt("salt");

    // null fields
    WalletFile.Aes128CtrKdfParams x = new WalletFile.Aes128CtrKdfParams();
    WalletFile.Aes128CtrKdfParams y = new WalletFile.Aes128CtrKdfParams();
    assertEquals(x, y);
    x.setPrf("x");
    assertNotEquals(x, y);
  }

  @Test
  public void testScryptKdfParamsAllAccessors() {
    WalletFile.ScryptKdfParams p = new WalletFile.ScryptKdfParams();
    p.setDklen(32);
    p.setN(262144);
    p.setP(1);
    p.setR(8);
    p.setSalt("saltvalue");

    assertEquals(32, p.getDklen());
    assertEquals(262144, p.getN());
    assertEquals(1, p.getP());
    assertEquals(8, p.getR());
    assertEquals("saltvalue", p.getSalt());
  }

  @Test
  public void testScryptKdfParamsEquals() {
    WalletFile.ScryptKdfParams a = new WalletFile.ScryptKdfParams();
    a.setDklen(32);
    a.setN(262144);
    a.setP(1);
    a.setR(8);
    a.setSalt("salt");

    WalletFile.ScryptKdfParams b = new WalletFile.ScryptKdfParams();
    b.setDklen(32);
    b.setN(262144);
    b.setP(1);
    b.setR(8);
    b.setSalt("salt");

    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
    assertTrue(a.equals(a));
    assertFalse(a.equals(null));
    assertFalse(a.equals("string"));

    b.setDklen(64);
    assertNotEquals(a, b);
    b.setDklen(32);

    b.setN(1);
    assertNotEquals(a, b);
    b.setN(262144);

    b.setP(2);
    assertNotEquals(a, b);
    b.setP(1);

    b.setR(16);
    assertNotEquals(a, b);
    b.setR(8);

    b.setSalt("other");
    assertNotEquals(a, b);

    // null salt
    WalletFile.ScryptKdfParams x = new WalletFile.ScryptKdfParams();
    WalletFile.ScryptKdfParams y = new WalletFile.ScryptKdfParams();
    assertEquals(x, y);
    x.setSalt("x");
    assertNotEquals(x, y);
  }

  @Test
  public void testJsonDeserializeWithScryptKdf() throws Exception {
    String json = "{"
        + "\"address\":\"TAddr\","
        + "\"version\":3,"
        + "\"id\":\"uuid\","
        + "\"crypto\":{"
        + "  \"cipher\":\"aes-128-ctr\","
        + "  \"ciphertext\":\"ct\","
        + "  \"cipherparams\":{\"iv\":\"iv\"},"
        + "  \"kdf\":\"scrypt\","
        + "  \"kdfparams\":{\"dklen\":32,\"n\":262144,\"p\":1,\"r\":8,\"salt\":\"salt\"},"
        + "  \"mac\":\"mac\""
        + "}}";

    WalletFile wf = new ObjectMapper().readValue(json, WalletFile.class);
    assertEquals("TAddr", wf.getAddress());
    assertEquals(3, wf.getVersion());
    assertNotNull(wf.getCrypto());
    assertNotNull(wf.getCrypto().getKdfparams());
    assertTrue(wf.getCrypto().getKdfparams() instanceof WalletFile.ScryptKdfParams);
  }

  @Test
  public void testJsonDeserializeWithAes128Kdf() throws Exception {
    String json = "{"
        + "\"address\":\"TAddr\","
        + "\"version\":3,"
        + "\"crypto\":{"
        + "  \"cipher\":\"aes-128-ctr\","
        + "  \"ciphertext\":\"ct\","
        + "  \"cipherparams\":{\"iv\":\"iv\"},"
        + "  \"kdf\":\"pbkdf2\","
        + "  \"kdfparams\":{\"dklen\":32,\"c\":262144,\"prf\":\"hmac-sha256\",\"salt\":\"salt\"},"
        + "  \"mac\":\"mac\""
        + "}}";

    WalletFile wf = new ObjectMapper().readValue(json, WalletFile.class);
    assertNotNull(wf.getCrypto().getKdfparams());
    assertTrue(wf.getCrypto().getKdfparams() instanceof WalletFile.Aes128CtrKdfParams);
  }

  @Test
  public void testJsonDeserializeCryptoV1Field() throws Exception {
    // Legacy files may use "Crypto" instead of "crypto"
    String json = "{"
        + "\"address\":\"TAddr\","
        + "\"version\":3,"
        + "\"Crypto\":{"
        + "  \"cipher\":\"aes-128-ctr\","
        + "  \"kdf\":\"scrypt\","
        + "  \"kdfparams\":{\"dklen\":32,\"n\":1,\"p\":1,\"r\":8,\"salt\":\"s\"}"
        + "}}";

    WalletFile wf = new ObjectMapper().readValue(json, WalletFile.class);
    assertNotNull(wf.getCrypto());
    assertEquals("aes-128-ctr", wf.getCrypto().getCipher());
  }
}
