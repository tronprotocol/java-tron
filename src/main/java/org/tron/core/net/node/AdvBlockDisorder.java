package org.tron.core.net.node;

import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.net.peer.PeerConnection;
import org.tron.protos.Protocol.Inventory.InventoryType;

import java.util.*;

/**
 * @program: java-tron
 * @description: This class is for Mapping Util using.
 * The member variable map is used for store the block's parent hash(key)
 * and the PeerAndBlockCapsule(value).
 * When we receive a disordered block, we
 * add the (parent hash, PeerAndBlockCapsule) to the map.
 * When we receive a ordered block and handle it, if success, we can search whether his child block
 * exists in the map, and we can handle this child simultaneously, if failure, we remove the his child block
 **/
public class AdvBlockDisorder {
    private HashMap<Sha256Hash, PeerAndBlockCapsule> map;

    public AdvBlockDisorder(){
      init();
    }

    public void init(){
        map = new HashMap<>();
    }

    public HashMap<Sha256Hash, PeerAndBlockCapsule> getMap(){
        return map;
    }

    public void add(PeerConnection peer, BlockCapsule block){
        PeerAndBlockCapsule peerAndBlockCapsule = new PeerAndBlockCapsule(peer, block);
        map.put(block.getParentHash(), peerAndBlockCapsule);
    }

    public void remove(BlockCapsule block){
        map.remove(block.getBlockId());
    }

    public PeerAndBlockCapsule get(BlockCapsule block){
        return map.get(block.getBlockId());
    }

    public PeerAndBlockCapsule getNextBlockAndRemove(BlockCapsule block){
        PeerAndBlockCapsule blockPeer = map.get(block.getBlockId());
        if(blockPeer != null){
            map.remove(block.getBlockId());
        }
        return blockPeer;
    }

    public void clear(){
        this.map.clear();
    }

    public void cleanUnusedBlock(BlockCapsule.BlockId blockId){
        long solidBlockNum = blockId.getNum();
        map.entrySet().removeIf(entry -> {
            PeerAndBlockCapsule peerAndBlock = entry.getValue();
            BlockCapsule block = peerAndBlock.getBlockCapsule();
            return block.getNum() < solidBlockNum;
        });

    }

    class PeerAndBlockCapsule{
        private PeerConnection peer;
        private BlockCapsule blockCapsule;

        private PeerAndBlockCapsule(PeerConnection peer, BlockCapsule blockCapsule){
            this.peer = peer;
            this.blockCapsule = blockCapsule;
        }

        public BlockCapsule getBlockCapsule() {
            return blockCapsule;
        }

        public PeerConnection getPeer() {
            return peer;
        }
    }
}



