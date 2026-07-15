package org.tron.core.net.messagehandler;

import java.util.HashSet;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.config.args.Args;
import org.tron.core.exception.P2pException;
import org.tron.core.exception.P2pException.TypeEnum;
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
  public void processMessage(PeerConnection peer, TronMessage msg) throws P2pException {
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
        long headNum = tronNetDelegate.getHeadBlockId().getNum();
        if (new BlockId(id).getNum() > headNum) {
          peer.setLastInteractiveTime(System.currentTimeMillis());
        }
      }
    }
  }

  private boolean check(PeerConnection peer, InventoryMessage inventoryMessage)
      throws P2pException {

    List<Sha256Hash> hashList = inventoryMessage.getHashList();
    if (hashList.size() != new HashSet<>(hashList).size()) {
      throw new P2pException(TypeEnum.BAD_MESSAGE,
          "Inventory contains duplicate hashes, size: " + hashList.size());
    }

    InventoryType type = inventoryMessage.getInventoryType();
    if (type != InventoryType.TRX && type != InventoryType.BLOCK) {
      throw new P2pException(TypeEnum.BAD_MESSAGE,
          "unknown inventory type: " + inventoryMessage.getInventory().getTypeValue());
    }
    int size = hashList.size();

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
