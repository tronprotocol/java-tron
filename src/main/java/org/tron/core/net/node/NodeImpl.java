package org.tron.core.net.node;

import com.google.protobuf.ByteString;
import io.scalecube.transport.Address;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import javafx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.overlay.node.GossipLocalNode;
import org.tron.common.utils.ExecutorLoop;
import org.tron.core.Sha256Hash;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.exception.BadBlockException;
import org.tron.core.exception.TraitorPeerException;
import org.tron.core.exception.UnReachBlockException;
import org.tron.core.exception.ValidateSignatureException;
import org.tron.core.net.message.BlockInventoryMessage;
import org.tron.core.net.message.BlockMessage;
import org.tron.core.net.message.ChainInventoryMessage;
import org.tron.core.net.message.FetchInvDataMessage;
import org.tron.core.net.message.Message;
import org.tron.core.net.message.MessageTypes;
import org.tron.core.net.message.SyncBlockChainMessage;
import org.tron.core.net.message.TransactionInventoryMessage;
import org.tron.core.net.message.TransactionMessage;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.peer.PeerConnectionDelegate;
import org.tron.protos.Protocal;
import org.tron.protos.Protocal.BlockInventory.Type;
import org.tron.protos.Protocal.Inventory.InventoryType;


public class NodeImpl extends PeerConnectionDelegate implements Node {

  private HashMap<Address, PeerConnection> mapPeer = new HashMap();

  private final List<Sha256Hash> trxToAdvertise = new ArrayList<>();

  private final List<BlockId> blockToAdvertise = new ArrayList<>();

  private static final Logger logger = LoggerFactory.getLogger("Node");

  private ConcurrentHashMap<Sha256Hash, PeerConnection> syncMap = new ConcurrentHashMap<>();

  private ConcurrentHashMap<Sha256Hash, PeerConnection> fetchMap = new ConcurrentHashMap<>();

  private NodeDelegate del;

  private GossipLocalNode gossipNode;

  private volatile boolean isAdvertiseActive;

  private Thread advertiseLoopThread;

  ExecutorLoop<SyncBlockChainMessage> loopSyncBlockChain;

  ExecutorLoop<FetchInvDataMessage> loopFetchBlocks;

  ExecutorLoop<Message> loopAdvertiseInv;

  @Override
  public void onMessage(PeerConnection peer, Message msg) {
    logger.info("Handle Message: " + msg);
    switch (msg.getType()) {
      case BLOCK:
        onHandleBlockMessage(peer, (BlockMessage) msg);
        break;
      case TRX:
        onHandleTransactionMessage(peer, (TransactionMessage) msg);
        break;
      case SYNC_BLOCK_CHAIN:
        onHandleSyncBlockChainMessage(peer, (SyncBlockChainMessage) msg);
        break;
      case FETCH_INV_DATA:
        onHandleFetchDataMessage(peer, (FetchInvDataMessage) msg);
        break;
      case BLOCK_INVENTORY:
        onHandleBlockInventoryMessage(peer, (BlockInventoryMessage) msg);
        break;
      case BLOCK_CHAIN_INVENTORY:
        onHandleChainInventoryMessage(peer, (ChainInventoryMessage) msg);
        break;
      default:
        throw new IllegalArgumentException("No such message");
    }
  }

  @Override
  public Message getMessage(Sha256Hash msgId) {
    return null;
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
      logger.info("Ready to broadcast a block, Its hash is " + msg.getMessageId());
      blockToAdvertise.add(((BlockMessage) msg).getBlockId());
    }
    if (msg instanceof TransactionMessage) {
      trxToAdvertise.add(msg.getMessageId());
    }
  }

  @Override
  public void listen() {
    gossipNode = GossipLocalNode.getInstance();
    gossipNode.setPeerDel(this);
    gossipNode.start();
    isAdvertiseActive = true;
  }

  @Override
  public void close() throws InterruptedException {
    gossipNode.stop();
    loopFetchBlocks.join();
    loopSyncBlockChain.join();
    loopAdvertiseInv.join();
    isAdvertiseActive = false;
    advertiseLoopThread.join();
  }

  @Override
  public void connectToP2PNetWork() {

    // broadcast inv
    loopAdvertiseInv = new ExecutorLoop<>(2, 10, b -> {
      logger.info("loop advertise inv");
      for (PeerConnection peer : mapPeer.values()) {
        if (!peer.isNeedSyncFromUs()) {
          logger.info("Advertise adverInv to " + peer);
          peer.sendMessage(b);
        }
      }
    }, throwable -> logger.error("Unhandled exception: ", throwable));

    // fetch blocks
    loopFetchBlocks = new ExecutorLoop<>(2, 10, c -> {
      logger.info("loop fetch blocks");
      if (fetchMap.containsKey(c.getMessageId())) {
        fetchMap.get(c.getMessageId()).sendMessage(c);
      }
    }, throwable -> logger.error("Unhandled exception: ", throwable));

    // sync block chain
    loopSyncBlockChain = new ExecutorLoop<>(2, 10, d -> {
      logger.info("loop sync block chain");
      if (syncMap.containsKey(d.getMessageId())) {
        syncMap.get(d.getMessageId()).sendMessage(d);
      }
    }, throwable -> logger.error("Unhandled exception: ", throwable));

    advertiseLoopThread = new Thread(() -> {
      while (isAdvertiseActive) {
        if (trxToAdvertise.isEmpty() && blockToAdvertise.isEmpty()) {
          try {
            Thread.sleep(1000);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
        if (!trxToAdvertise.isEmpty()) {
          synchronized (this.trxToAdvertise) {
            loopAdvertiseInv.push(new TransactionInventoryMessage(trxToAdvertise));
            trxToAdvertise.clear();
          }
        }
        if (!blockToAdvertise.isEmpty()) {
          synchronized (this.blockToAdvertise) {
            loopAdvertiseInv.push(new BlockInventoryMessage(blockToAdvertise, Type.ADVTISE));
            blockToAdvertise.clear();
          }
        }
      }
    });
    advertiseLoopThread.start();
  }

  @Override
  public void syncFrom(Sha256Hash myHeadBlockHash) {
    //List<Sha256Hash> hashList = del.getBlockChainSummary(myHeadBlockHash, 100);

    try {
      while (mapPeer.isEmpty()) {
        logger.info("other peer is nil, please wait ... ");
        Thread.sleep(10000L);
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    //loopSyncBlockChain.push(new SyncBlockChainMessage(hashList));
  }


  private void onHandleBlockMessage(PeerConnection peer, BlockMessage blkMsg) {
    logger.info("on handle block message");
    peer.setLastBlockPeerKnow((BlockId) blkMsg.getMessageId());
    try {
      del.handleBlock(blkMsg.getBlockCapsule());
    } catch (ValidateSignatureException e) {
      //TODO process validate signature exception
      e.printStackTrace();
    } catch (BadBlockException e) {

    }
  }

  private void onHandleTransactionMessage(PeerConnection peer, TransactionMessage trxMsg) {
    logger.info("on handle transaction message");
    del.handleTransaction(trxMsg.getTransactionCapsule());
  }

  private void onHandleSyncBlockChainMessage(PeerConnection peer, SyncBlockChainMessage syncMsg) {
    logger.info("on handle sync block chain message");
    List<BlockId> blockIds = new ArrayList<>();
    List<BlockId> summaryCHhainIds = syncMsg.getBlockIds();
    try {
      blockIds = del.getLostBlockIds(summaryCHhainIds);
    } catch (UnReachBlockException e) {
    }

    if (blockIds.isEmpty()) {
      peer.setNeedSyncFromUs(false);
    }

    if (!peer.isNeedSyncFromPeer()
        && !summaryCHhainIds.isEmpty()
        && !del.contain(summaryCHhainIds.get(summaryCHhainIds.size() - 1), MessageTypes.BLOCK)) {
      startSyncWithPeer(peer);
    }
    peer.sendMessage(new ChainInventoryMessage(blockIds));
  }

  private void onHandleFetchDataMessage(PeerConnection peer, FetchInvDataMessage fetchInvDataMsg) {
    logger.info("on handle fetch block message");
    Protocal.Inventory inv = fetchInvDataMsg.getInventory();
    MessageTypes type =
        inv.getType() == InventoryType.BLOCK ? MessageTypes.BLOCK : MessageTypes.TRX;

    //get data one by one
    for (ByteString byteHash : inv.getIdsList()) {
      Sha256Hash hash = Sha256Hash.wrap(byteHash);
      if (del.contain(hash, type)) {
        peer.sendMessage(del.getData(hash, type));
      }
    }
  }

  private void banTraitorPeer (PeerConnection peer) {
    disconnectPeer(peer);
  }

  private void onHandleChainInventoryMessage(PeerConnection peer, ChainInventoryMessage msg) {
    logger.info("on handle block chain inventory message");
    try {
      if (peer.getSyncChainRequested() != null) {
        List<BlockId> blockIds = msg.getBlockIds();

        //check if the peer is a traitor
        if (!blockIds.isEmpty()) {
          long num = blockIds.get(0).getNum();
          for (BlockId id :
              blockIds) {
            if (id.getNum() != num++) {
              throw new TraitorPeerException("We get a not continuous block inv from " + peer);
            }
          }

          BlockId first = blockIds.get(0);

          if (peer.getSyncChainRequested().getKey().isEmpty()) {
            if (first.getNum() != 1) {
              throw new TraitorPeerException("We want a block inv starting from beginning from " + peer);
            }
          } else {
            boolean isFound = false;
            for (BlockId id :
                blockIds) {
              if (id.equals(first)) {
                isFound = true;
              }
            }
            if (!isFound) {
              throw new TraitorPeerException("We get a unlinked block chain from " + peer);
            }
          }
        }
        //check finish

        //here this peer's answer is legal
        peer.setSyncChainRequested(null);
        if (blockIds.isEmpty() && peer.getBlockChainToFetch().isEmpty()) {
          peer.setNeedSyncFromPeer(false);
          //TODO: check whole sync status and notify del sync status.
          //TODO: if sync finish call del.syncToCli();
          return;
        }

        Deque<BlockId> blockIdDeque = new LinkedList<>(blockIds);
        if (!blockIdDeque.isEmpty() && peer.getBlockChainToFetch().isEmpty()) {

        }

      } else {
        throw new TraitorPeerException("We don't send sync request to " + peer);
      }

    } catch (TraitorPeerException e) {
      banTraitorPeer(peer);
    }
    //    List<Sha256Hash> blockIds = del.getLostBlockIds(msg.getHashList());
//    FetchInvDataMessage fetchMsg = new FetchInvDataMessage(blockIds, InventoryType.BLOCK);
//    fetchMap.put(fetchMsg.getMessageId(), peer);
//    loopFetchBlocks.push(fetchMsg);
  }

  private void onHandleBlockInventoryMessage(PeerConnection peer, BlockInventoryMessage msg) {
    logger.info("on handle blocks inventory message");
    //todo: check this peer's advertise history and the history of our request to this peer.
    //simple implement here first
    List<Sha256Hash> fetchList = new ArrayList<>();
    msg.getBlockIds().forEach(hash -> {
      //TODO: Check this block whether we need it,Use peer.invToUs and peer.invWeAdv.
      logger.info("We will fetch " + hash + " from " + peer);
      fetchList.add(hash);
    });
    FetchInvDataMessage fetchMsg = new FetchInvDataMessage(fetchList, InventoryType.BLOCK);
    fetchMap.put(fetchMsg.getMessageId(), peer);
    loopFetchBlocks.push(fetchMsg);
  }

  private void startSync() {
    mapPeer.values().forEach(this::startSyncWithPeer);
  }

  private void startSyncWithPeer(PeerConnection peer) {
    peer.setNeedSyncFromPeer(true);
    peer.getBlockChainToFetch().clear();
    peer.setNumUnfetchBlock(0);
    peer.setLastBlockPeerKnow(del.getGenesisBlock());
    peer.setBanned(false);
    syncNextBatchChainIds(peer);
  }

  @Override
  public PeerConnection getPeer(io.scalecube.transport.Message msg) {
    return mapPeer.get(msg.sender());
  }

  private void syncNextBatchChainIds(PeerConnection peer) {
    try {
      List<BlockId> chainSummary = del
          .getBlockChainSummary(peer.getLastBlockPeerKnow(), peer.getBlockChainToFetch());
      //peer.setLastBlockPeerKnow(chainSummary.isEmpty() ? del.getGenesisBlock()
      //    : chainSummary.get(chainSummary.size() - 1));
      peer.setSyncChainRequested(new Pair<>(chainSummary, System.currentTimeMillis()));
      peer.sendMessage(new SyncBlockChainMessage(chainSummary));
    } catch (Exception e) { //TODO: use tron excpetion here
      e.printStackTrace();
      disconnectPeer(peer);
    }

  }

  @Override
  public void connectPeer(PeerConnection peer) {
    mapPeer.put(peer.getAddress(), peer);
    startSyncWithPeer(peer);
  }

  @Override
  public void disconnectPeer(PeerConnection peer) {
    mapPeer.remove(peer.getAddress());
  }


}

