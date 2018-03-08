/*
 * java-tron is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-tron is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.core.capsule;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.math.BigInteger;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Base64;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.ECKey.ECDSASignature;
import org.tron.common.utils.ByteArray;
import org.tron.core.Sha256Hash;
import org.tron.core.exception.ValidateSignatureException;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.BlockHeader;
import org.tron.protos.Protocol.BlockHeader.raw;

public class BlockCapsuleTest {

  private static final Logger logger = LoggerFactory.getLogger("Test");
  protected BlockCapsule blockCapsule;

  // test sign and validateSignature together
  @Test
  public void testSign() {
    ECKey key = ECKey.fromPrivate(new BigInteger(
        "48720541756297624231117183381585618702966411811775628910886100667008198869515"));

    byte[] privKeyBytes = key.getPrivKeyBytes();
    byte[] address = key.getAddress();
    ByteString witnessAddress = ByteString.copyFrom(address);

    ByteString parentHash = ByteString.copyFrom(ByteArray.fromHexString("0x000000000000000000"
        + "0000000000000000000000000000000000000000000000"));

    blockCapsule = new BlockCapsule(Protocol.Block.newBuilder().setBlockHeader(
        BlockHeader.newBuilder().setRawData(
            BlockHeader.raw.newBuilder().setWitnessAddress(witnessAddress).setNumber(2)
                .setWitnessId(2).setParentHash(parentHash).build()).build()).build());

    blockCapsule.sign(privKeyBytes);

    String str = "4734364b7278524166444e786b554e55735235664c343071664a6f626e6234314f437644376f4a41"
        + "686b576149776f7a44623356717635797a3941716f6a516c712f394f4e6e6e623971776e386e58354745736"
        + "c7659513d";

    // test sign
    try {
      Block block = Block.parseFrom(blockCapsule.getData());

      String signStr = ByteArray.toHexString(block.getBlockHeader()
          .getWitnessSignature().toByteArray());
      System.out.println("signStr:" + signStr);
      Assert.assertEquals("signature is error", str, signStr);

    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage());
      Assert.assertTrue(false);
    }

    // test validateSignature
    raw.Builder rawData = Block.newBuilder().getBlockHeader().getRawData().toBuilder();

    rawData.setNumber(2);
    rawData.setWitnessId(2);
    rawData.setWitnessAddress(witnessAddress);
    rawData.setParentHash(parentHash);

    ECKey ecKey = ECKey.fromPrivate(privKeyBytes);
    ECDSASignature signature = ecKey.sign(Sha256Hash.of(rawData.build().toByteArray()).getBytes());
    ByteString sign = ByteString.copyFrom(signature.toBase64().getBytes());

    BlockCapsule blockCapsule2 = new BlockCapsule(
        Block.newBuilder()
            .setBlockHeader(
                BlockHeader.newBuilder().setWitnessSignature(sign).setRawData(rawData).build())
            .build());

    System.out.println("testSign().sign:" + ByteArray.toHexString(sign.toByteArray()));

    try {
      Assert.assertTrue("testSign():blockCapsule2.validateSignature() error",
          blockCapsule2.validateSignature());
    } catch (ValidateSignatureException e) {
      logger.debug(e.getMessage());
      Assert.assertTrue(false);
    }

  }

  /*
   * validate signature by different rawData parameters
   */
  @Test
  public void testValidateSignature1() {
    ECKey key = ECKey.fromPrivate(new BigInteger(
        "48720541756297624231117183381585618702966411811775628910886100667008198869515"));

    byte[] privKeyBytes = key.getPrivKeyBytes();
    byte[] address = key.getAddress();
    ByteString witnessAddress = ByteString.copyFrom(address);
    ByteString parentHash = ByteString.copyFrom(ByteArray.fromHexString("0x000000000000000000"
        + "0000000000000000000000000000000000000000000000"));

    // test
    raw.Builder rawData = Block.newBuilder().getBlockHeader().getRawData().toBuilder();

    rawData.setNumber(1);
    rawData.setWitnessId(2);
    rawData.setWitnessAddress(witnessAddress);
    rawData.setParentHash(parentHash);

    ECKey ecKey = ECKey.fromPrivate(privKeyBytes);
    ECDSASignature signature = ecKey.sign(Sha256Hash.of(rawData.build().toByteArray()).getBytes());
    ByteString sign = ByteString.copyFrom(signature.toBase64().getBytes());

    BlockCapsule blockCapsule2 = new BlockCapsule(
        Block.newBuilder()
            .setBlockHeader(
                BlockHeader.newBuilder().setWitnessSignature(sign).setRawData(rawData.build()))
            .build());

    System.out.println("testValidateSignature1().sign1:"
        + ByteArray.toHexString(sign.toByteArray()));
    // test validateSignature
    try {
      Assert.assertTrue("validateSignature1():blockCapsule2.validateSignature() error",
          blockCapsule2.validateSignature());
    } catch (ValidateSignatureException e) {
      logger.debug(e.getMessage());
      Assert.assertTrue(false);
    }

    // before base64 end
    rawData.setNumber(2);
    rawData.setWitnessId(2);

    BlockCapsule blockCapsule3 = new BlockCapsule(
        Block.newBuilder()
            .setBlockHeader(
                BlockHeader.newBuilder()
                    .setWitnessSignature(sign)
                    .setRawData(rawData.build()))
            .build());

    System.out.println("testValidateSignature1().sign2:"
        + ByteArray.toHexString(sign.toByteArray()));

    try {
      Assert.assertFalse("rawDate is different", blockCapsule3.validateSignature());
    } catch (ValidateSignatureException e) {
      logger.debug(e.getMessage());
      Assert.assertTrue(false);
    }
  }

  /*
   * valiedate by change signature
   */
  @Test
  public void testValidataSignature2() {
    ECKey key = ECKey.fromPrivate(new BigInteger(
        "48720541756297624231117183381585618702966411811775628910886100667008198869515"));

    byte[] privKeyBytes = key.getPrivKeyBytes();
    byte[] address = key.getAddress();
    ByteString witnessAddress = ByteString.copyFrom(address);

    // test
    raw.Builder rawData = Block.newBuilder().getBlockHeader().getRawData().toBuilder();

    rawData.setNumber(2);
    rawData.setWitnessId(2);
    rawData.setWitnessAddress(witnessAddress);

    ECKey ecKey = ECKey.fromPrivate(privKeyBytes);
    ECDSASignature signature = ecKey.sign(Sha256Hash.of(rawData.build().toByteArray()).getBytes());
    ByteString sign = ByteString.copyFrom(signature.toBase64().getBytes());

    BlockCapsule blockCapsule2 = new BlockCapsule(
        Block.newBuilder()
            .setBlockHeader(
                BlockHeader.newBuilder().setWitnessSignature(sign).setRawData(rawData.build()))
            .build());

    System.out.println("testValidataSignature2().sign:"
        + ByteArray.toHexString(sign.toByteArray()));
    // test validateSignature
    try {
      Assert.assertTrue("testValidataSignature2(): blockCapsule2.validateSignature() is error",
          blockCapsule2.validateSignature());
    } catch (ValidateSignatureException e) {
      logger.debug(e.getMessage());
      Assert.assertTrue(false);
    }

    // before base64 start
    // correct sign
    byte[] hex = Hex.decode(
        "48446562572f7174336353425a674b7a53584a6a326764466e594c6d2b72705857567848744e723"
            + "56f527734517444522b73413954774f4170574a756f4f5045762b3941634577796767577a4a6c3"
            + "8555947704e6b366b3d");

    String base64String;
    try {
      base64String = new String(hex, "ISO-8859-1");
    } catch (Exception e) {
      e.printStackTrace();
      base64String = "";
    }

    byte[] signatureEncoded = Base64.decode(base64String);

    String str2 = ByteArray.toHexString(signatureEncoded);
    System.out.println("testValidataSignature2().str2:" + str2);

    // correct sign
    ByteString bsWitnessSignature = ByteString.copyFrom(Base64.encode(Hex.decode(
        "1c379b5bfaadddc4816602b3497263da07459d82e6faba57595c47b4daf9a11c3842d0d1fac03d4f0380a5"
            + "626ea0e3c4bfef40704c328205b3265f14606a4d93a9")));

    // error sign test
    ByteString errorSign = ByteString.copyFrom(Base64.encode(Hex.decode(
        "1c379b5bfaadddc4816602b3497263da07459d82e6faba57595c47b4daf9a11c3842d0d1fac03d4f0380a5"
            + "626ea0e3c4bfef40704c328205b3265f14606a4d93a2")));

    // before base64 end
    rawData.setNumber(2);
    rawData.setWitnessId(2);

    BlockCapsule blockCapsule3 = new BlockCapsule(
        Block.newBuilder()
            .setBlockHeader(
                BlockHeader.newBuilder()
                    .setWitnessSignature(errorSign)
                    .setRawData(rawData.build()))
            .build());

    System.out.println("testValidataSignature2().sign:"
        + ByteArray.toHexString(errorSign.toByteArray()));

    try {
      Assert.assertFalse("signature was changed", blockCapsule3.validateSignature());
    } catch (ValidateSignatureException e) {
      logger.debug(e.getMessage());
      Assert.assertTrue(false);
    }

  }

  /*
   * validate signature without witnessAddress
   */
  @Test
  public void testValidateWithoutWitnessAddress() {

    ECKey key = ECKey.fromPrivate(new BigInteger(
        "48720541756297624231117183381585618702966411811775628910886100667008198869515"));

    byte[] privKeyBytes = key.getPrivKeyBytes();

    // test
    raw.Builder rawData = Block.newBuilder().getBlockHeader().getRawData().toBuilder();

    ECKey ecKey = ECKey.fromPrivate(privKeyBytes);
    ECDSASignature signature = ecKey.sign(Sha256Hash.of(rawData.build().toByteArray()).getBytes());
    ByteString sign = ByteString.copyFrom(signature.toBase64().getBytes());

    BlockCapsule blockCapsule = new BlockCapsule(
        Block.newBuilder()
            .setBlockHeader(
                BlockHeader.newBuilder().setWitnessSignature(sign).setRawData(rawData.build()))
            .build());

    System.out.println("testValidateWithoutWitnessAddress().sign:"
        + ByteArray.toHexString(sign.toByteArray()));
    // test validateSignature

    try {
      Assert.assertFalse("validate signature without witnessAddress is error",
          blockCapsule.validateSignature());
    } catch (ValidateSignatureException e) {
      logger.debug(e.getMessage());
      Assert.assertTrue(false);
    }
  }

  /*
   * validate signature without number
   */
  @Test
  public void testValidateWithoutNumber() {
    ECKey key = ECKey.fromPrivate(new BigInteger(
        "48720541756297624231117183381585618702966411811775628910886100667008198869515"));

    byte[] privKeyBytes = key.getPrivKeyBytes();
    byte[] address = key.getAddress();
    ByteString witnessAddress = ByteString.copyFrom(address);
    ByteString parentHash = ByteString.copyFrom(ByteArray.fromHexString("0x000000000000000000"
        + "0000000000000000000000000000000000000000000000"));

    // test
    raw.Builder rawData = Block.newBuilder().getBlockHeader().getRawData().toBuilder();

    rawData.setWitnessId(2);
    rawData.setWitnessAddress(witnessAddress);
    rawData.setParentHash(parentHash);

    ECKey ecKey = ECKey.fromPrivate(privKeyBytes);
    ECDSASignature signature = ecKey.sign(Sha256Hash.of(rawData.build().toByteArray()).getBytes());
    ByteString sign = ByteString.copyFrom(signature.toBase64().getBytes());

    BlockCapsule blockCapsule = new BlockCapsule(
        Block.newBuilder()
            .setBlockHeader(
                BlockHeader.newBuilder().setWitnessSignature(sign).setRawData(rawData.build()))
            .build());

    System.out
        .println("testValidateWithoutNumber().sign:" + ByteArray.toHexString(sign.toByteArray()));

    // test validateSignature
    try {
      Assert.assertTrue("validate signature without number is error",
          blockCapsule.validateSignature());
    } catch (ValidateSignatureException e) {
      logger.debug(e.getMessage());
      Assert.assertTrue(false);
    }
  }

  /*
   * validate signature without witnessId
   */
  @Test
  public void testValidateWithoutWitnessId() {
    ECKey key = ECKey.fromPrivate(new BigInteger(
        "48720541756297624231117183381585618702966411811775628910886100667008198869515"));

    byte[] privKeyBytes = key.getPrivKeyBytes();
    byte[] address = key.getAddress();
    ByteString witnessAddress = ByteString.copyFrom(address);
    ByteString parentHash = ByteString.copyFrom(ByteArray.fromHexString("0x000000000000000000"
        + "0000000000000000000000000000000000000000000000"));

    // test
    raw.Builder rawData = Block.newBuilder().getBlockHeader().getRawData().toBuilder();

    rawData.setNumber(1);
    rawData.setWitnessAddress(witnessAddress);
    rawData.setParentHash(parentHash);

    ECKey ecKey = ECKey.fromPrivate(privKeyBytes);
    ECDSASignature signature = ecKey.sign(Sha256Hash.of(rawData.build().toByteArray()).getBytes());
    ByteString sign = ByteString.copyFrom(signature.toBase64().getBytes());

    BlockCapsule blockCapsule = new BlockCapsule(
        Block.newBuilder()
            .setBlockHeader(
                BlockHeader.newBuilder().setWitnessSignature(sign).setRawData(rawData.build()))
            .build());

    System.out.println("testValidateWithoutWitnessId().sign:"
        + ByteArray.toHexString(sign.toByteArray()));
    // test validateSignature
    try {
      Assert.assertTrue("validate signature without witnessId is error",
          blockCapsule.validateSignature());
    } catch (ValidateSignatureException e) {
      logger.debug(e.getMessage());
      Assert.assertTrue(false);
    }

  }

  /*
   * validate signature without parentHash
   */
  @Test
  public void testValidateWithoutParentHash() {
    ECKey key = ECKey.fromPrivate(new BigInteger(
        "48720541756297624231117183381585618702966411811775628910886100667008198869515"));

    byte[] privKeyBytes = key.getPrivKeyBytes();
    byte[] address = key.getAddress();
    ByteString witnessAddress = ByteString.copyFrom(address);

    // test
    raw.Builder rawData = Block.newBuilder().getBlockHeader().getRawData().toBuilder();

    rawData.setNumber(1);
    rawData.setWitnessId(2);
    rawData.setWitnessAddress(witnessAddress);

    ECKey ecKey = ECKey.fromPrivate(privKeyBytes);
    ECDSASignature signature = ecKey.sign(Sha256Hash.of(rawData.build().toByteArray()).getBytes());
    ByteString sign = ByteString.copyFrom(signature.toBase64().getBytes());

    BlockCapsule blockCapsule = new BlockCapsule(
        Block.newBuilder()
            .setBlockHeader(
                BlockHeader.newBuilder().setWitnessSignature(sign).setRawData(rawData.build()))
            .build());

    System.out.println("testValidateWithoutParentHash().sign:"
        + ByteArray.toHexString(sign.toByteArray()));
    // test validateSignature
    try {
      Assert.assertTrue("validate signature without parentHash is error",
          blockCapsule.validateSignature());
    } catch (ValidateSignatureException e) {
      logger.debug(e.getMessage());
      Assert.assertTrue(false);
    }

  }
}
