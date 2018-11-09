package org.tron.core.net.messagehandler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.net.TronProxy;
import org.tron.core.net.message.InventoryMessage;
import org.tron.core.net.message.TronMessage;
import org.tron.core.net.peer.Item;
import org.tron.core.net.peer.PeerAdv;
import org.tron.core.net.peer.PeerConnection;
import org.tron.protos.Protocol.Inventory.InventoryType;

@Slf4j
@Component
public class InventoryMsgHandler implements TronMsgHandler{

  @Autowired
  private TronProxy tronProxy;

  @Autowired
  private PeerAdv peerAdv;

  @Autowired
  private TransactionsMsgHandler transactionsMsgHandler;

  private int maxCountIn10s = 10_000;

  @Override
  public void processMessage (PeerConnection peer, TronMessage msg) throws Exception {

    InventoryMessage inventoryMessage = (InventoryMessage) msg;
    InventoryType type = inventoryMessage.getInventoryType();

    if (transactionsMsgHandler.isBusy() && type.equals(InventoryType.TRX)) {
      logger.warn("Too many trx msg to handle, drop inventory msg from peer {}, size {}",
          peer.getInetAddress(), inventoryMessage.getHashList().size());
      return;
    }

    int count = peer.getNodeStatistics().messageStatistics.tronInTrxInventoryElement.getCount(10);
    if (count > maxCountIn10s) {
      logger.warn("Inv is overload from peer {}, size {}", peer.getInetAddress(), count);
      return;
    }

    for (Sha256Hash id : inventoryMessage.getHashList()) {
      Item item = new Item(id, type);
      boolean spreadFlag = false;
      boolean requestFlag = false;
      for (PeerConnection p: tronProxy.getActivePeer()) {
        if (p.getAdvInvSpread().containsKey(item)) {
          spreadFlag = true;
        }
        if (p.getAdvInvRequest().containsKey(item)) {
          requestFlag = true;
        }
      }
      if (!spreadFlag && !peer.isNeedSyncFromPeer() && !peer.isNeedSyncFromUs()) {
        peer.getAdvInvReceive().put(item, System.currentTimeMillis());
        if (!requestFlag) {
          if (!peerAdv.addInv(item)) {
            logger.info("This item {} from peer {} Already exist.", item, peer.getInetAddress());
            continue;
          }
        }
      }
    }
  }
}
