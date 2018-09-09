package org.tron.core.net.node;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.testng.Assert;
import org.tron.common.utils.ByteArray;
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
                                .fromHexString(block0.getBlockId().toString())))
                )).build());
        BlockCapsule block2 = new BlockCapsule(Protocol.Block.newBuilder().setBlockHeader(
                Protocol.BlockHeader.newBuilder().setRawData(Protocol.BlockHeader.raw.newBuilder().setNumber(2).setParentHash(ByteString.copyFrom(
                        ByteArray
                                .fromHexString(block1.getBlockId().toString())))
                )).build());

        advBlockDisorder.add(peer1, block0);
        advBlockDisorder.add(peer1, block1);
        advBlockDisorder.add(peer2, block2);

        Assert.assertEquals(advBlockDisorder.get(block0).getPeer(), peer1);
        Assert.assertEquals(advBlockDisorder.get(block1).getPeer(), peer2);

        Assert.assertEquals(advBlockDisorder.get(block0).getBlockCapsule(), block1);
        Assert.assertEquals(advBlockDisorder.get(block1).getBlockCapsule(), block2);

        advBlockDisorder.clear();
        advBlockDisorder.add(peer1, block0);
        advBlockDisorder.add(peer1, block0);
        Assert.assertEquals(advBlockDisorder.getMap().size(), 1);
    }

    @Test
    public void testRemove(){
        PeerConnection peer = new PeerConnection();
        BlockCapsule block0 = BlockUtil.newGenesisBlockCapsule();
        BlockCapsule block1 = new BlockCapsule(Protocol.Block.newBuilder().setBlockHeader(
                Protocol.BlockHeader.newBuilder().setRawData(Protocol.BlockHeader.raw.newBuilder().setNumber(1).setParentHash(ByteString.copyFrom(
                        ByteArray
                                .fromHexString(block0.getBlockId().toString())))
                )).build());
        BlockCapsule block2 = new BlockCapsule(Protocol.Block.newBuilder().setBlockHeader(
                Protocol.BlockHeader.newBuilder().setRawData(Protocol.BlockHeader.raw.newBuilder().setNumber(1).setParentHash(ByteString.copyFrom(
                        ByteArray
                                .fromHexString(block1.getBlockId().toString())))
                )).build());
        advBlockDisorder.add(peer, block1);
        advBlockDisorder.add(peer, block2);
        advBlockDisorder.remove(block0);
        Assert.assertEquals(advBlockDisorder.getMap().size(), 1);
        advBlockDisorder.remove(block1);
        Assert.assertEquals(advBlockDisorder.getMap().size(), 0);

    }

    @Test
    public void testGetBlockCapsule(){
        PeerConnection peer = new PeerConnection();
        BlockCapsule block0 = BlockUtil.newGenesisBlockCapsule();
        BlockCapsule block1 = new BlockCapsule(Protocol.Block.newBuilder().setBlockHeader(
                Protocol.BlockHeader.newBuilder().setRawData(Protocol.BlockHeader.raw.newBuilder().setNumber(1).setParentHash(ByteString.copyFrom(
                        ByteArray
                                .fromHexString(block0.getBlockId().toString())))
                )).build());
        advBlockDisorder.add(peer, block1);
        Assert.assertEquals(advBlockDisorder.get(block0).getBlockCapsule(), block1);
    }

    @Test
    public void testGetPeer(){
        PeerConnection peer = new PeerConnection();
        BlockCapsule block0 = BlockUtil.newGenesisBlockCapsule();
        BlockCapsule block1 = new BlockCapsule(Protocol.Block.newBuilder().setBlockHeader(
                Protocol.BlockHeader.newBuilder().setRawData(Protocol.BlockHeader.raw.newBuilder().setNumber(1).setParentHash(ByteString.copyFrom(
                        ByteArray
                                .fromHexString(block0.getBlockId().toString())))
                )).build());
        advBlockDisorder.add(peer,block1);
        Assert.assertEquals(advBlockDisorder.get(block0).getPeer(), peer);
    }

    @Test
    public void testCleanUnusedBlock(){
        PeerConnection peer = new PeerConnection();
        BlockCapsule block0 = BlockUtil.newGenesisBlockCapsule();
        BlockCapsule block1 = new BlockCapsule(Protocol.Block.newBuilder().setBlockHeader(
                Protocol.BlockHeader.newBuilder().setRawData(Protocol.BlockHeader.raw.newBuilder().setNumber(1).setParentHash(ByteString.copyFrom(
                        ByteArray
                                .fromHexString(block0.getBlockId().toString())))
                )).build());
        BlockCapsule block2 = new BlockCapsule(Protocol.Block.newBuilder().setBlockHeader(
                Protocol.BlockHeader.newBuilder().setRawData(Protocol.BlockHeader.raw.newBuilder().setNumber(2).setParentHash(ByteString.copyFrom(
                        ByteArray
                                .fromHexString(block1.getBlockId().toString())))
                )).build());
        BlockCapsule block3 = new BlockCapsule(Protocol.Block.newBuilder().setBlockHeader(
                Protocol.BlockHeader.newBuilder().setRawData(Protocol.BlockHeader.raw.newBuilder().setNumber(3).setParentHash(ByteString.copyFrom(
                        ByteArray
                                .fromHexString(block2.getBlockId().toString())))
                )).build());
        BlockCapsule block4 = new BlockCapsule(Protocol.Block.newBuilder().setBlockHeader(
                Protocol.BlockHeader.newBuilder().setRawData(Protocol.BlockHeader.raw.newBuilder().setNumber(4).setParentHash(ByteString.copyFrom(
                        ByteArray
                                .fromHexString(block3.getBlockId().toString())))
                )).build());
        BlockCapsule block5 = new BlockCapsule(Protocol.Block.newBuilder().setBlockHeader(
                Protocol.BlockHeader.newBuilder().setRawData(Protocol.BlockHeader.raw.newBuilder().setNumber(5).setParentHash(ByteString.copyFrom(
                        ByteArray
                                .fromHexString(block4.getBlockId().toString())))
                )).build());

        BlockCapsule block6 = new BlockCapsule(Protocol.Block.newBuilder().setBlockHeader(
                Protocol.BlockHeader.newBuilder().setRawData(Protocol.BlockHeader.raw.newBuilder().setNumber(6).setParentHash(ByteString.copyFrom(
                        ByteArray
                                .fromHexString(block5.getBlockId().toString())))
                )).build());
        BlockCapsule solidBlock = new BlockCapsule(Protocol.Block.newBuilder().setBlockHeader(
                Protocol.BlockHeader.newBuilder().setRawData(Protocol.BlockHeader.raw.newBuilder().setNumber(5).setParentHash(ByteString.copyFrom(
                        ByteArray
                                .fromHexString(block4.getBlockId().toString())))
                )).build());
        advBlockDisorder.add(peer, block1);
        advBlockDisorder.add(peer, block2);
        advBlockDisorder.add(peer, block3);
        advBlockDisorder.add(peer, block4);
        advBlockDisorder.add(peer, block5);
        advBlockDisorder.add(peer, block6);

        advBlockDisorder.cleanUnusedBlock(solidBlock.getBlockId());
        Assert.assertEquals(advBlockDisorder.getMap().size(), 2);
    }

    @Before
    public void initConfiguration() {
        Args.setParam(new String[]{}, Constant.TEST_CONF);
    }

}
