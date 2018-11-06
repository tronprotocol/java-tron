package org.tron.core.net.messagehandler;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.net.TronManager;
import org.tron.core.net.TronProxy;
import org.tron.core.net.message.InventoryMessage;
import org.tron.core.net.message.TronMessage;
import org.tron.core.net.node.NodeImpl.PriorItem;
import org.tron.core.net.peer.Item;
import org.tron.core.net.peer.PeerAdv;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.peer.PeerSync;
import org.tron.protos.Protocol.Inventory.InventoryType;

@Slf4j
@Component
public class InventoryMsgHandler implements TronMsgHandler{

  @Autowired
  private TronProxy tronProxy;

  @Autowired
  private PeerSync peerSync;

  @Autowired
  private PeerAdv peerAdv;

  @Setter
  private TronManager tronManager;

  @Autowired
  private TransactionsMsgHandler transactionsMsgHandler;

  @Override
  public void processMessage (PeerConnection peer, TronMessage msg) throws Exception {

    InventoryMessage inventoryMessage = (InventoryMessage) msg;
    InventoryType type = inventoryMessage.getInventoryType();

    if (transactionsMsgHandler.isBusy() && type.equals(InventoryType.TRX)) {
      logger.warn("Too many trx msg to handle, drop inventory msg from peer {}, size {}",
          peer.getInetAddress(), inventoryMessage.getHashList().size());
      return;
    }
    for (Sha256Hash id : inventoryMessage.getHashList()) {
      if (type.equals(InventoryType.TRX) && peerAdv.getTrxCache().getIfPresent(id) != null) {
        logger.info("trx {} from peer {} Already exist.", id, peer.getNode().getHost());
        continue;
      }
      boolean spreadFlag = false;
      boolean requestFlag = false;
      for (PeerConnection p: tronProxy.getActivePeer()) {
        if (p.getAdvObjWeSpread().containsKey(id)) {
          spreadFlag = true;
        }
        if (p.getAdvObjWeRequested().containsKey(new Item(id, type))) {
          requestFlag = true;
        }
      }

      if (!spreadFlag && !peer.isNeedSyncFromPeer() && !peer.isNeedSyncFromUs()) {
        peer.getAdvObjSpreadToUs().put(id, System.currentTimeMillis());
        if (!requestFlag) {
          PriorItem targetPriorItem = this.advObjToFetch.get(id);
          if (targetPriorItem != null) {
            //another peer tell this trx to us, refresh its time.
            targetPriorItem.refreshTime();
          } else {
            fetchWaterLine.increase();
            this.advObjToFetch.put(id, new PriorItem(new Item(id, msg.getInventoryType()),
                fetchSequenceCounter.incrementAndGet()));
          }
        }
      }
    }
  }
}
