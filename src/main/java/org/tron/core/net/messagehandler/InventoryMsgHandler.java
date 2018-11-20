package org.tron.core.net.messagehandler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.config.Parameter.NetConstants;
import org.tron.core.exception.P2pException;
import org.tron.core.exception.P2pException.TypeEnum;
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

    if (!check(peer, inventoryMessage)) {
      return;
    }

    for (Sha256Hash id : inventoryMessage.getHashList()) {
      Item item = new Item(id, type);
      peer.getAdvInvReceive().put(item, System.currentTimeMillis());
      if (!peerAdv.addInv(item)) {
        logger.info("This item {} from peer {} Already exist.", item, peer.getInetAddress());
      }
    }
  }

  private boolean check (PeerConnection peer, InventoryMessage inventoryMessage) throws Exception {
    InventoryType type = inventoryMessage.getInventoryType();
    int size = inventoryMessage.getHashList().size();

    if (size > NetConstants.MAX_INV_FETCH_PER_PEER) {
      throw new P2pException(TypeEnum.BAD_MESSAGE, "size: " + size);
    }

    if (peer.isNeedSyncFromPeer() || peer.isNeedSyncFromUs()) {
      logger.warn("Drop inv: {} size: {} from Peer {}, syncFromUs: {}, syncFromPeer: {}.",
          type, size, peer.getInetAddress(), peer.isNeedSyncFromUs(), peer.isNeedSyncFromPeer());
      return false;
    }
    if (transactionsMsgHandler.isBusy() && type.equals(InventoryType.TRX)) {
      logger.warn("Drop inv: {} size: {} from Peer {}, transactionsMsgHandler is busy.",
          type, size, peer.getInetAddress());
      return false;
    }
    int count = peer.getNodeStatistics().messageStatistics.tronInTrxInventoryElement.getCount(10);
    if (count > maxCountIn10s) {
      logger.warn("Drop inv: {} size: {} from Peer {}, Inv count: {} is overload.",
          type, size, peer.getInetAddress(), count);
      return false;
    }
    return true;
  }
}
