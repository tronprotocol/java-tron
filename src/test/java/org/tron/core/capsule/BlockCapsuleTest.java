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
import org.tron.protos.Protocal.Block;
import org.tron.protos.Protocal.BlockHeader;
import org.tron.protos.Protocal.BlockHeader.raw;

public class BlockCapsuleTest {

  private static final Logger logger = LoggerFactory.getLogger("Test");
  protected BlockCapsule blockCapsule;

  @Test
  public void testSign() {
    ECKey key = ECKey.fromPrivate(new BigInteger(
        "48720541756297624231117183381585618702966411811775628910886100667008198869515"));

    byte[] privKeyBytes = key.getPrivKeyBytes();
    byte[] address = key.getAddress();
    ByteString witnessAddress = ByteString.copyFrom(address);

    blockCapsule = new BlockCapsule(
        Block.newBuilder()
            .setBlockHeader(BlockHeader.newBuilder().setRawData(
                raw.newBuilder().setWitnessAddress(witnessAddress).setNumber(2).setWitnessId(2)
                    .build())
                .build()).build());

    blockCapsule.sign(privKeyBytes);
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

    // test
    raw.Builder rawData = Block.newBuilder().getBlockHeader().getRawData().toBuilder();

    rawData.setNumber(1);
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

    System.out.println("sig1:" + ByteArray.toHexString(sign.toByteArray()));
    // test validateSignature
    Assert.assertTrue(blockCapsule2.validateSignature());

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

    System.out.println("sig2:" + ByteArray.toHexString(sign.toByteArray()));

    logger
        .info("Changes in number、WitnessId and address values have an impact on the test results");

    Assert.assertFalse(blockCapsule3.validateSignature());
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

    System.out.println("sig1:" + ByteArray.toHexString(sign.toByteArray()));
    // test validateSignature
    Assert.assertTrue(blockCapsule2.validateSignature());

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
    System.out.println("str2:" + str2);

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

    System.out.println("sig2:" + ByteArray.toHexString(errorSign.toByteArray()));

    Assert.assertFalse(blockCapsule3.validateSignature());
  }

  /*
   * validate signature without witnessAddress
   */
  @Test
  public void testValidateNullParameters() {
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

    System.out.println("sig3:" + ByteArray.toHexString(sign.toByteArray()));
    // test validateSignature
    Assert.assertFalse(blockCapsule.validateSignature());
  }


}
