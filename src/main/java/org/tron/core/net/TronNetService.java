package org.tron.core.net;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.overlay.server.ChannelManager;
import org.tron.core.exception.P2pException;
import org.tron.core.exception.P2pException.TypeEnum;
import org.tron.core.net.message.TronMessage;
import org.tron.core.net.messagehandler.BlockMsgHandler;
import org.tron.core.net.messagehandler.ChainInventoryMsgHandler;
import org.tron.core.net.messagehandler.FetchInvDataMsgHandler;
import org.tron.core.net.messagehandler.InventoryMsgHandler;
import org.tron.core.net.messagehandler.SyncBlockChainMsgHadler;
import org.tron.core.net.messagehandler.TransactionsMsgHandler;
import org.tron.core.net.peer.PeerAdv;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.peer.PeerSync;
import org.tron.protos.Protocol.ReasonCode;

@Slf4j
@Component
public class TronNetService {

  @Autowired
  private ChannelManager channelManager;

  @Autowired
  private PeerAdv peerAdv;

  @Autowired
  private PeerSync peerSync;

  @Autowired
  private SyncBlockChainMsgHadler syncBlockChainMsgHadler;

  @Autowired
  private ChainInventoryMsgHandler chainInventoryMsgHandler;

  @Autowired
  private InventoryMsgHandler inventoryMsgHandler;


  @Autowired
  private FetchInvDataMsgHandler fetchInvDataMsgHandler;

  @Autowired
  private BlockMsgHandler blockMsgHandler;

  @Autowired
  private TransactionsMsgHandler transactionsMsgHandler;

  public void start () {
    channelManager.init();
    peerAdv.init();
    peerSync.init();
    transactionsMsgHandler.init();
  }

  public void close () {
    try {
      peerAdv.close();
      peerSync.close();
      transactionsMsgHandler.close();
    }catch (Exception e) {
      logger.error("TronNetService closed failed.",e );
    }
  }

  public void onMessage(PeerConnection peer, TronMessage msg) {
    try {
      switch (msg.getType()) {
        case SYNC_BLOCK_CHAIN:
          syncBlockChainMsgHadler.processMessage(peer, msg);
          break;
        case BLOCK_CHAIN_INVENTORY:
          chainInventoryMsgHandler.processMessage(peer, msg);
          break;
        case INVENTORY:
          inventoryMsgHandler.processMessage(peer, msg);
          break;
        case FETCH_INV_DATA:
          fetchInvDataMsgHandler.processMessage(peer, msg);
          break;
        case BLOCK:
          blockMsgHandler.processMessage(peer, msg);
          break;
        case TRXS:
          transactionsMsgHandler.processMessage(peer, msg);
          break;
        default:
          throw new P2pException(TypeEnum.NO_SUCH_MESSAGE, "No such message");
      }
    }catch (Exception e) {
      processException(peer, msg, e);
    }
  }

  private void processException (PeerConnection peer, TronMessage msg, Exception ex) {

    ReasonCode code = null;
    if (ex instanceof P2pException) {
      TypeEnum type = ((P2pException) ex).getType();
      switch (type) {
        case BAD_TRX:
          code = ReasonCode.BAD_TX;
          break;
        case BAD_BLOCK:
          code = ReasonCode.BAD_BLOCK;
          break;
        case NO_SUCH_MESSAGE:
        case MESSAGE_WITH_WRONG_LENGTH:
        case BAD_MESSAGE:
          code = ReasonCode.BAD_PROTOCOL;
          break;
        case SYNC_FAILED:
          code = ReasonCode.SYNC_FAIL;
          break;
        case UNLINK_BLOCK:
          code = ReasonCode.UNLINKABLE;
          break;
        case DEFAULT:
          code = ReasonCode.UNLINKABLE;
          break;
      }
      logger.error("Process {} from peer {} failed, reason: ", peer.getInetAddress(), msg.getType(), type.getDesc(), ex);
    }else {
      logger.error("Process {} from peer {} failed.", peer.getInetAddress(), msg.getType(), ex);
      code = ReasonCode.UNKNOWN;
    }
    peer.disconnect(code);
  }
}
