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

package org.tron.core;

import com.google.protobuf.ByteString;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.utils.BlockUtil;
import org.tron.core.config.Configuration;
import org.tron.core.config.args.Args;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.BlockHeader;
import org.tron.protos.Protocol.BlockHeader.raw;

public class BlockUtilTest {

  private static final Logger logger = LoggerFactory.getLogger("Test");

  @Before
  public void initConfiguration() {
    Args.setParam(new String[]{}, Configuration.getByPath(Constant.TEST_CONF));
  }

  @Test
  public void testBlockUtil() {
    //test create GenesisBlockCapsule
    BlockCapsule blockCapsule1 = BlockUtil.newGenesisBlockCapsule();
    Sha256Hash sha256Hash = Sha256Hash.wrap(ByteArray
        .fromHexString("0x0000000000000000000000000000000000000000000000000000000000000000"));

    Assert.assertEquals(0, blockCapsule1.getTimeStamp());
    Assert.assertEquals(sha256Hash,
        blockCapsule1.getParentHash());
    Assert.assertEquals(0, blockCapsule1.getNum());

    //test isParentOf method: create blockCapsule2 and blockCapsule3
    // blockCapsule3.setParentHash() equals blockCapsule2.getBlockId
    BlockCapsule blockCapsule2 = new BlockCapsule(Block.newBuilder().setBlockHeader(
        BlockHeader.newBuilder().setRawData(raw.newBuilder().setParentHash(ByteString.copyFrom(
            ByteArray
                .fromHexString("0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b81")))
        )).build());

    BlockCapsule blockCapsule3 = new BlockCapsule(Block.newBuilder().setBlockHeader(
        BlockHeader.newBuilder().setRawData(raw.newBuilder().setParentHash(ByteString.copyFrom(
            ByteArray
                .fromHexString(blockCapsule2.getBlockId().toString())))
        )).build());

    Assert.assertEquals(false, BlockUtil.isParentOf(blockCapsule1, blockCapsule2));
    Assert.assertFalse(BlockUtil.isParentOf(blockCapsule1, blockCapsule2));
    Assert.assertEquals(true, BlockUtil.isParentOf(blockCapsule2, blockCapsule3));
    Assert.assertTrue(BlockUtil.isParentOf(blockCapsule2, blockCapsule3));

  }
}
