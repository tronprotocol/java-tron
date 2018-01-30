package org.tron.core.net.node;

import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.overlay.node.GossipLocalNode;
import org.tron.common.utils.ExecutorLoop;
import org.tron.core.net.message.BlockInventoryMessage;
import org.tron.core.net.message.BlockMessage;
import org.tron.core.net.message.FetchBlocksMessage;
import org.tron.core.net.message.Message;
import org.tron.core.net.message.SyncBlockChainMessage;
import org.tron.core.net.message.TransactionMessage;
import org.tron.core.net.peer.PeerConnection;
import org.tron.protos.Protocal;

public class NodeImpl extends PeerConnection implements Node {

  private static final Logger logger = LoggerFactory.getLogger("Node");


  private HashMap<byte[], Message> messageCache = new HashMap<>();

  private List<Message> newInventory = new ArrayList<>();

  private NodeDelegate del;

  private GossipLocalNode gossipNode = GossipLocalNode.getInstance();

  //loop
  ExecutorLoop<BlockMessage> loopAdvertiseBlock;

  ExecutorLoop<TransactionMessage> loopAdvertiseTrx;

  ExecutorLoop<SyncBlockChainMessage> loopSyncBlockChain;

  ExecutorLoop<FetchBlocksMessage> loopFetchBlocks;

  @Override
  public void onMessage(PeerConnection peer, Message msg) {
    switch (msg.getType()) {
      case BLOCK:
        onHandleBlockMessage((BlockMessage) msg);
        break;
      case TRX:
        onHandleTranscationMessage((TransactionMessage) msg);
        break;
      case SYNC_BLOCK_CHAIN:
        onHandleSycnBlockChainMessage((SyncBlockChainMessage) msg);
        break;
      case FETCH_BLOCKS:
        onHandleFetchBlocksMessage((FetchBlocksMessage) msg);
        break;
      case BLOCK_INVENTORY:
        onHandleBlockInventoryMssage((BlockInventoryMessage) msg);
        break;
      default:
        throw new IllegalArgumentException("No such message");
    }
  }

  @Override
  public Message getMessage(byte[] itemHash) {
    return messageCache.get(itemHash);

  }

  @Override
  public void setNodeDelegate(NodeDelegate nodeDel) {
    this.del = nodeDel;
  }

  /**
   * broadcast msg.
   *
   * @param msg msg to bradcast
   */
  public void broadcast(Message msg) {
    newInventory.add(msg);
    messageCache.put(msg.getData(), msg);
  }

  @Override
  public void listenOn(String endPoint) {
    return;
  }

  @Override
  public void connectToP2PNetWork() {
    gossipNode.start(this);
    loopAdvertiseBlock = new ExecutorLoop<>(8, 10, a -> {
      gossipNode.broadcast(a);
      return null;
    }, throwable -> logger.error("Unhandled exception: ", throwable));

    loopAdvertiseTrx = new ExecutorLoop<>(2, 100, b -> {
      gossipNode.broadcast(b);
      return null;
    }, throwable -> logger.error("Unhandled exception: ", throwable));

    loopFetchBlocks = new ExecutorLoop<>(2, 10, c -> {
      gossipNode.sendMessage(c.getPeer(), c);
      return null;
    }, throwable -> logger.error("Unhandled exception: ", throwable));

    loopSyncBlockChain = new ExecutorLoop<>(2, 10, d -> {
      gossipNode.sendMessage(d.getPeer(), d);
      return null;
    }, throwable -> logger.error("Unhandled exception: ", throwable));
  }

  @Override
  public void syncFrom(byte[] myHeadBlockHash) {
    ArrayList<byte[]> hashList = del.getBlockChainSynopsis(myHeadBlockHash, 100);

    Protocal.Inventory.Builder invBuild = Protocal.Inventory.newBuilder();
    invBuild.setType(Protocal.Inventory.InventoryType.BLOCK);
    int i = 0;
    for (byte[] hash :
        hashList) {
      invBuild.setIds(i++, ByteString.copyFrom(hash, 0, 31));
  }

    if (gossipNode.getMembers().size() == 0) {
      //todo: a loop here to wait the peers to sync blocks.
      logger.debug("other peer is nil, please wait ... ");
      return;
    }
    loopSyncBlockChain.push(new SyncBlockChainMessage(invBuild.build(),
        gossipNode.getMembers().iterator().next()));
  }

  private void onHandleBlockMessage(BlockMessage blkMsg) {
    del.handleBlock(blkMsg);
  }

  private void onHandleTranscationMessage(TransactionMessage trxMsg) {
    del.handleTransation(trxMsg);
  }

  private void onHandleSycnBlockChainMessage(SyncBlockChainMessage syncMsg) {
    Protocal.Inventory inv = del.getBlockIds(syncMsg.getInventory());
    BlockInventoryMessage blkInvMsg = new BlockInventoryMessage(inv, syncMsg.getPeer());
    gossipNode.sendMessage(blkInvMsg.getPeer(), blkInvMsg);
  }

  private void onHandleFetchBlocksMessage(FetchBlocksMessage fetchBlksMsg) {
    Protocal.Inventory inv = fetchBlksMsg.getInventory();
    for (ByteString hash :
        inv.getIdsList()) {
      gossipNode.sendMessage(fetchBlksMsg.getPeer(), del.getData(hash.toByteArray()));
    }
  }

  private void onHandleBlockInventoryMssage(BlockInventoryMessage msg) {
    Protocal.Inventory inv = del.getBlockIds(msg.getInventory());
    FetchBlocksMessage fetchMsg = new FetchBlocksMessage(inv, msg.getPeer());
    loopFetchBlocks.push(fetchMsg);
  }
}
