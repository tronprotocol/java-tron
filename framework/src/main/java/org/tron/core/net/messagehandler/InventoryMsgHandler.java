package org.tron.core.net.messagehandler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.config.args.Args;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.net.message.TronMessage;
import org.tron.core.net.message.adv.InventoryMessage;
import org.tron.core.net.peer.Item;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.service.adv.AdvService;
import org.tron.protos.Protocol.Inventory.InventoryType;

@Slf4j(topic = "net")
@Component
public class InventoryMsgHandler implements TronMsgHandler {

  @Autowired
  private TronNetDelegate tronNetDelegate;

  @Autowired
  private AdvService advService;

  @Autowired
  private TransactionsMsgHandler transactionsMsgHandler;

  @Override
  public void processMessage(PeerConnection peer, TronMessage msg) {
    InventoryMessage inventoryMessage = (InventoryMessage) msg;
    InventoryType type = inventoryMessage.getInventoryType();

    if (!check(peer, inventoryMessage)) {
      return;
    }

    for (Sha256Hash id : inventoryMessage.getHashList()) {
      Item item = new Item(id, type);
      peer.getAdvInvReceive().put(item, System.currentTimeMillis());
      advService.addInv(item);
      if (type.equals(InventoryType.BLOCK) && peer.getAdvInvSpread().getIfPresent(item) == null) {
        peer.setLastInteractiveTime(System.currentTimeMillis());
      }
    }
  }

  private boolean check(PeerConnection peer, InventoryMessage inventoryMessage) {

    InventoryType type = inventoryMessage.getInventoryType();
    int size = inventoryMessage.getHashList().size();

    if (peer.isNeedSyncFromPeer() || peer.isNeedSyncFromUs()) {
      logger.warn("Drop inv: {} size: {} from Peer {}, syncFromUs: {}, syncFromPeer: {}",
          type, size, peer.getInetAddress(), peer.isNeedSyncFromUs(), peer.isNeedSyncFromPeer());
      return false;
    }

    if (type.equals(InventoryType.TRX) && tronNetDelegate.isBlockUnsolidified()) {
      logger.warn("Drop inv: {} size: {} from Peer {}, block unsolidified",
          type, size, peer.getInetAddress());
      return false;
    }

    if (type.equals(InventoryType.TRX) && transactionsMsgHandler.isBusy()) {
      logger.warn("Drop inv: {} size: {} from Peer {}, transactionsMsgHandler is busy",
              type, size, peer.getInetAddress());
      if (Args.getInstance().isOpenPrintLog()) {
        logger.warn("[isBusy]Drop tx list is: {}", inventoryMessage.getHashList());
      }
      return false;
    }

    return true;
  }
}
