/*
package org.tron.core.net.node;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.testng.Assert;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.Constant;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.utils.BlockUtil;
import org.tron.core.config.args.Args;
import org.tron.core.net.peer.PeerConnection;
import org.tron.protos.Protocol;

@Slf4j
public class AdvBlockDisorderTest {

    AdvBlockDisorder advBlockDisorder = new AdvBlockDisorder();

    @Test
    public void testAdd(){
        PeerConnection peer1 = new PeerConnection();
        PeerConnection peer2 = new PeerConnection();

        BlockCapsule block0 = BlockUtil.newGenesisBlockCapsule();
        BlockCapsule block1 = new BlockCapsule(Protocol.Block.newBuilder().setBlockHeader(
                Protocol.BlockHeader.newBuilder().setRawData(Protocol.BlockHeader.raw.newBuilder().setNumber(1).setParentHash(ByteString.copyFrom(
                        ByteArray
                                .fromHexString("1304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b81")))
                )).build());
        BlockCapsule block2 = new BlockCapsule(Protocol.Block.newBuilder().setBlockHeader(
                Protocol.BlockHeader.newBuilder().setRawData(Protocol.BlockHeader.raw.newBuilder().setNumber(2).setParentHash(ByteString.copyFrom(
                        ByteArray
                                .fromHexString("2304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b81")))
                )).build());
        advBlockDisorder.add(block0.getParentHash(), peer1, block0);
        advBlockDisorder.add(block1.getParentHash(), peer1,block1);
        advBlockDisorder.add(block2.getParentHash(), peer2,block2);

        Assert.assertEquals(advBlockDisorder.getBlockCapsulePeer(block0.getParentHash()), peer1);
        Assert.assertEquals(advBlockDisorder.getPeer(block1.getParentHash()), peer1);
        Assert.assertEquals(advBlockDisorder.getPeer(block2.getParentHash()),peer2);

        Assert.assertEquals(advBlockDisorder.getBlockCapsule(block0.getParentHash()), block0);
        Assert.assertEquals(advBlockDisorder.getBlockCapsule(block1.getParentHash()), block1);
        Assert.assertEquals(advBlockDisorder.getBlockCapsule(block2.getParentHash()), block2);

        advBlockDisorder.clear();
        advBlockDisorder.add(block0.getParentHash(), peer1, block0);
        advBlockDisorder.add(block0.getParentHash(), peer1, block0);
        Assert.assertEquals(advBlockDisorder.getMap().size(), 1);
    }

    @Test
    public void testRemove(){
        PeerConnection peer = new PeerConnection();
        BlockCapsule block = BlockUtil.newGenesisBlockCapsule();
        advBlockDisorder.add(block.getParentHash(), peer, block);
        advBlockDisorder.remove(block.getParentHash());
        Assert.assertEquals(advBlockDisorder.getMap().size(), 0);
        advBlockDisorder.add(block.getParentHash(), peer, block);
        advBlockDisorder.remove(Sha256Hash.of("1234567".getBytes()));
        Assert.assertEquals(advBlockDisorder.getMap().size(), 1);

    }

    @Test
    public void testGetBlockCapsule(){
        PeerConnection peer = new PeerConnection();
        BlockCapsule block = BlockUtil.newGenesisBlockCapsule();
        advBlockDisorder.add(block.getParentHash(), peer, block);
        Assert.assertEquals(advBlockDisorder.getBlockCapsule(block.getParentHash()), block);
    }

    @Test
    public void testGetPeer(){
        PeerConnection peer = new PeerConnection();
        BlockCapsule block = BlockUtil.newGenesisBlockCapsule();
        advBlockDisorder.add(block.getParentHash(), peer,block);
        Assert.assertEquals(advBlockDisorder.getPeer(block.getParentHash()), peer);
    }

    @Before
    public void initConfiguration() {
        Args.setParam(new String[]{}, Constant.TEST_CONF);
    }

}
*/
