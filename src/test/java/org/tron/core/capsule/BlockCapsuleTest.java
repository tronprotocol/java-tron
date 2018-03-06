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
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.ECKey.ECDSASignature;
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
    byte[] witnessAddress = key.getAddress();
    ByteString bsAddress = ByteString.copyFrom(witnessAddress);

    blockCapsule = new BlockCapsule(Block.newBuilder().setBlockHeader(
        BlockHeader.newBuilder().setRawData(
            raw.newBuilder().setWitnessAddress(bsAddress)
                .build())).build());

    blockCapsule.sign(privKeyBytes);

    Block block = blockCapsule.getBlock();

    byte[] sign = block.getBlockHeader().getWitnessSignature().toByteArray();

    byte[] r = new byte[32];
    byte[] s = new byte[32];

    if (sign.length != 65) {
      return;
    }

    System.arraycopy(sign, 0, r, 0, 32);
    System.arraycopy(sign, 32, s, 0, 32);
    byte revId = sign[64];
    if (revId < 27) {
      revId += 27; //revId -> v
    }

    ECDSASignature signature = ECDSASignature.fromComponents(r, s, revId);

    // test sign
    Assert.assertTrue(
        key.verify(Sha256Hash.of(block.getBlockHeader().getRawData().toByteArray()).getBytes(),
            signature));

    // test validateSignature
    Assert.assertTrue(blockCapsule.validateSignature());
  }

}
