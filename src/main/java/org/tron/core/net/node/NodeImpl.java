package org.tron.core.net.node;

import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.overlay.node.GossipLocalNode;
import org.tron.common.utils.ExecutorLoop;
import org.tron.core.Sha256Hash;
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

  private NodeDelegate del;

  private GossipLocalNode gossipNode = GossipLocalNode.getInstance();

  //loop
  ExecutorLoop<BlockMessage> loopAdvertiseBlock;

  ExecutorLoop<TransactionMessage> loopAdvertiseTrx;

  ExecutorLoop<SyncBlockChainMessage> loopSyncBlockChain;

  ExecutorLoop<FetchBlocksMessage> loopFetchBlocks;

  @Override
  public void onMessage(PeerConnection peer, Message msg) {
    logger.info("on message");
    switch (msg.getType()) {
      case BLOCK:
        onHandleBlockMessage((BlockMessage) msg);
        break;
      case TRX:
        onHandleTransactionMessage((TransactionMessage) msg);
        break;
      case SYNC_BLOCK_CHAIN:
        onHandleSyncBlockChainMessage((SyncBlockChainMessage) msg);
        break;
      case FETCH_BLOCKS:
        onHandleFetchBlocksMessage((FetchBlocksMessage) msg);
        break;
      case BLOCK_INVENTORY:
        onHandleBlockInventoryMessage((BlockInventoryMessage) msg);
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

    if (msg instanceof BlockMessage) {
      loopAdvertiseBlock.push((BlockMessage) msg);
    }
    if (msg instanceof TransactionMessage) {
      loopAdvertiseTrx.push((TransactionMessage) msg);
    }
  }

  @Override
  public void listenOn(String endPoint) {
    return;
  }

  @Override
  public void connectToP2PNetWork() {
    gossipNode.start(this);
    loopAdvertiseBlock = new ExecutorLoop<>(8, 10, a -> {
      logger.info("loop advertise block");
      gossipNode.broadcast(a);
      return null;
    }, throwable -> logger.error("Unhandled exception: ", throwable));

    loopAdvertiseTrx = new ExecutorLoop<>(2, 100, b -> {
      logger.info("loop advertise trx");
      gossipNode.broadcast(b);
      return null;
    }, throwable -> logger.error("Unhandled exception: ", throwable));

    loopFetchBlocks = new ExecutorLoop<>(2, 10, c -> {
      logger.info("loop fetch blocks");
      gossipNode.sendMessage(c.getPeer(), c);
      return null;
    }, throwable -> logger.error("Unhandled exception: ", throwable));

    loopSyncBlockChain = new ExecutorLoop<>(2, 10, d -> {
      logger.info("loop sync block chain");
      gossipNode.sendMessage(d.getPeer(), d);
      return null;
    }, throwable -> logger.error("Unhandled exception: ", throwable));
  }

  @Override
  public void syncFrom(Sha256Hash myHeadBlockHash) {
    List<Sha256Hash> hashList = del.getBlockChainSynopsis(myHeadBlockHash, 100);

    Protocal.Inventory.Builder invBuild = Protocal.Inventory.newBuilder();
    invBuild.setType(Protocal.Inventory.InventoryType.BLOCK);
    int i = 0;
    for (Sha256Hash hash :
        hashList) {
      invBuild.setIds(i++, hash.getByteString());
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
    logger.info("on handle block message");
    del.handleBlock(blkMsg);
  }

  private void onHandleTransactionMessage(TransactionMessage trxMsg) {
    logger.info("on handle transaction message");
    del.handleTransation(trxMsg);
  }

  private void onHandleSyncBlockChainMessage(SyncBlockChainMessage syncMsg) {
    logger.info("on handle sync block chain message");
    List<Sha256Hash> blockIds = del.getBlockIds(syncMsg.getHashList());
    BlockInventoryMessage blkInvMsg = new BlockInventoryMessage(blockIds, syncMsg.getPeer());
    gossipNode.sendMessage(blkInvMsg.getPeer(), blkInvMsg);
  }

  private void onHandleFetchBlocksMessage(FetchBlocksMessage fetchBlksMsg) {
    logger.info("on handle fetch block message");
    Protocal.Inventory inv = fetchBlksMsg.getInventory();
    for (ByteString hash :
        inv.getIdsList()) {
      gossipNode.sendMessage(fetchBlksMsg.getPeer(), del.getData(hash.toByteArray()));
    }
  }

  private void onHandleBlockInventoryMessage(BlockInventoryMessage msg) {
    logger.info("on handle block inventory message");
    List<Sha256Hash> blockIds = del.getBlockIds(msg.getHashList());
    FetchBlocksMessage fetchMsg = new FetchBlocksMessage(blockIds, msg.getPeer());
    loopFetchBlocks.push(fetchMsg);
  }
}
