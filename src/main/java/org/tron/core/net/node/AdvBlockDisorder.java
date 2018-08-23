package org.tron.core.net.node;

import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.net.peer.PeerConnection;
import org.tron.protos.Protocol.Inventory.InventoryType;

import java.util.Comparator;
import java.util.HashMap;

/**
 * @program: java-tron
 * @description: This class is for Mapping Util using.
 * The member variable map is used for store the block's parent hash(key)
 * and the PeerAndBlockCapsule(value).
 * When we handle a block unsuccessfully because of UnLinkedBlockException, we
 * add the parent hash and the PeerAndBlockCapsule to the map.
 * When we handle a block successfully,we can search whether his child block
 * exists in the map, and we can handle this child simultaneously.
 * @author: shydesky@gmail.com
 * @create: 2018-07-13
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
        if(!peerAndBlockCapsule.isEmpty()){
            map.put(block.getParentHash(), peerAndBlockCapsule);
        }
    }

    public void remove(BlockCapsule block){
        map.remove(block.getParentHash());
    }

    public PeerAndBlockCapsule get(Sha256Hash hash){
        return map.get(hash);
    }

    public void clear(){
        this.map.clear();
    }

    public PeerConnection getPeer(BlockCapsule block){
        return get(block.getParentHash()) != null ? get(block.getParentHash()).getPeer(): null;
    }

    public BlockCapsule getNextBlock(BlockCapsule block){
        return get(block.getParentHash()) != null ? get(block.getParentHash()).getBlockCapsule(): null;
    }

    class PeerAndBlockCapsule{
        private PeerConnection peer;
        private BlockCapsule blockCapsule;

        private PeerAndBlockCapsule(PeerConnection peer, BlockCapsule blockCapsule){
            this.peer = peer;
            this.blockCapsule = blockCapsule;
        }

        private boolean isEmpty(){
            return (this.blockCapsule == null || this.peer == null);
        }

        public BlockCapsule getBlockCapsule() {
            return blockCapsule;
        }

        public PeerConnection getPeer() {
            return peer;
        }
    }

    public boolean isOrderedBlock(PeerConnection peer, BlockCapsule block){
        Sha256Hash parentHash = block.getBlockId();
        Item item = new Item(parentHash, InventoryType.BLOCK);
        return !peer.getAdvObjWeRequested().keySet().contains(item);
    }
}



