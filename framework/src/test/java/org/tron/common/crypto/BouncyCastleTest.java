package org.tron.common.crypto;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.math.BigInteger;
import java.security.SignatureException;
import java.util.Arrays;
import org.bouncycastle.crypto.digests.SM3Digest;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;
import org.tron.common.crypto.sm2.SM2;
import org.tron.common.utils.PublicMethod;
import org.tron.common.utils.Sha256Hash;

public class BouncyCastleTest {

  // ECkey
  private String privString = PublicMethod.getRandomPrivateKey();
  private BigInteger privateKey = new BigInteger(privString, 16);
  private String spongyAddress = PublicMethod.getHexAddressByPrivateKey(privString);
  private String spongyPubkey = PublicMethod.getPublicByPrivateKey(privString);

  //SM2
  private String spongyPublickey = PublicMethod.getSM2PublicByPrivateKey(privString);
  private String spongySM2Address = PublicMethod.getSM2AddressByPrivateKey(privString);

  @Test
  public void testHex() {
    ECKey key = ECKey.fromPrivate(privateKey);
    byte[] address = key.getAddress();
    byte[] addressTmp = Arrays.copyOfRange(Hex.decode(spongyAddress), 1, address.length);
    assertEquals(Hex.toHexString(addressTmp),
            Hex.toHexString(Arrays.copyOfRange(address, 1, 21)));
    assertArrayEquals(Arrays.copyOfRange(address, 1, 21),
            addressTmp);
  }

  @Test
  public void testSha256Hash() {
    String msg = "transaction raw data";
    String spongySha256 = "da36dc042630f1aa810171d1fc4db7771a9f12b585848b0fed6caf5c7bd06531";
    String spongySm3 = "5521fbff5abf495e6db8fb4a83ed2bf27b97197757fc5a1002a7edc58b690900";

    byte[] sha256Hash = Sha256Hash.hash(true, msg.getBytes());
    assertEquals(spongySha256, Hex.toHexString(sha256Hash));

    byte[] sm3Hash = Sha256Hash.hash(false, msg.getBytes());
    assertEquals(spongySm3, Hex.toHexString(sm3Hash));
  }

  @Test
  public void testSha3Hash() {
    String msg = "transaction raw data";
    String spongyHash = "429e4ce662a41be0a50e65626f0ec4c8f68d45a57fe80beebab2f82601884795";
    byte[] hash = Hash.sha3(msg.getBytes());
    assertEquals(spongyHash, Hex.toHexString(hash));
  }

  @Test
  public void testECKeyAddress() {
    ECKey key = ECKey.fromPrivate(privateKey);
    byte[] pubkey = key.getPubKey();
    assertEquals(spongyPubkey, Hex.toHexString(pubkey));
    byte[] address = key.getAddress();
    byte[] addressTmp = Arrays.copyOfRange(Hex.decode(spongyAddress), 1, address.length);
    assertEquals(Hex.toHexString(addressTmp),
            Hex.toHexString(Arrays.copyOfRange(address, 1, 21)));
  }

  @Test
  public void testECKeySignature() throws SignatureException {
    SignInterface sign = SignUtils.fromPrivate(Hex.decode(privString), true);
    String msg = "transaction raw data";
    byte[] hash = Sha256Hash.hash(true, msg.getBytes());
    String sig = sign.signHash(hash);
    byte[] address = SignUtils.signatureToAddress(hash, sig, true);
    byte[] addressTmp = Arrays.copyOfRange(Hex.decode(spongyAddress), 1, address.length);
    assertEquals(Hex.toHexString(addressTmp), Hex.toHexString(Arrays.copyOfRange(address, 1, 21)));
  }

  @Test
  public void testECSpongySignature() throws SignatureException {
    String msg = "transaction raw data";
    String spongySig = "GwYii3BGoQq3sdyWiGVv7bGCR5hJy62g+IF+1jPOSqHt"
            + "IDfuKgowhiiK7ivcqk+T7qq/hlfIjaRe+t1drFDZ+Mo=";
    String spongyAddress = "cd2a3d9f938e13cd947ec05abc7fe734df8dd826";
    byte[] hash = Sha256Hash.hash(true, msg.getBytes());
    byte[] address = SignUtils.signatureToAddress(hash, spongySig, true);
    assertEquals(spongyAddress, Hex.toHexString(Arrays.copyOfRange(address, 1, 21)));
  }

  @Test
  public void testSM3Hash() {
    String msg = "transaction raw data";
    String spongyHash = "5521fbff5abf495e6db8fb4a83ed2bf27b97197757fc5a1002a7edc58b690900";
    SM3Digest digest = new SM3Digest();
    digest.update(msg.getBytes(), 0, msg.getBytes().length);
    byte[] hash = new byte[digest.getDigestSize()];
    digest.doFinal(hash, 0);
    assertEquals(spongyHash, Hex.toHexString(hash));
  }

  @Test
  public void testSM2Address() {
    SM2 key = SM2.fromPrivate(privateKey);
    assertEquals(spongyPublickey, Hex.toHexString(key.getPubKey()));
    byte[] address = key.getAddress();
    byte[] addressTmp = Arrays.copyOfRange(Hex.decode(spongySM2Address), 1, address.length);
    assertEquals(Hex.toHexString(addressTmp), Hex.toHexString(Arrays.copyOfRange(address, 1, 21)));
  }

  @Test
  public void testSM2Signature() throws SignatureException {
    SignInterface sign = SignUtils.fromPrivate(Hex.decode(privString), false);
    String msg = "transaction raw data";
    byte[] hash = Sha256Hash.hash(false, msg.getBytes());
    String sig = sign.signHash(hash);
    byte[] address = SignUtils.signatureToAddress(hash, sig, false);
    byte[] addressTmp = Arrays.copyOfRange(Hex.decode(spongySM2Address), 1, address.length);
    assertEquals(Hex.toHexString(addressTmp), Hex.toHexString(Arrays.copyOfRange(address, 1, 21)));
  }

  @Test
  public void testSM2SpongySignature() throws SignatureException {
    String msg = "transaction raw data";
    String spongySig = "HOoyvBLOJ+dKReQdAc6W/ffRi/KmVntco0+xgzmFItEExq/fHF"
            + "veCe0GoCJUBdyHyUFjwn+a18ibtGJcHxnvLj0=";
    String spongyAddress = "7dc44d739a5226c0d3037bb7919f653eb2f938b9";
    byte[] hash = Sha256Hash.hash(false, msg.getBytes());
    byte[] address = SignUtils.signatureToAddress(hash, spongySig, false);
    assertEquals(spongyAddress, Hex.toHexString(Arrays.copyOfRange(address, 1, 21)));
  }
}
