package org.tron.core.ibc.connect;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.exception.P2pException;
import org.tron.core.exception.P2pException.TypeEnum;
import org.tron.core.ibc.communicate.CommunicateService;
import org.tron.core.ibc.spv.CrossHeaderMsgProcess;
import org.tron.core.net.message.CrossChainMessage;
import org.tron.core.net.message.TronMessage;
import org.tron.core.net.peer.PeerConnection;
import org.tron.protos.Protocol.ReasonCode;

@Slf4j(topic = "net-cross")
@Component
public class CrossChainMsgProcess {

  @Autowired
  private CommunicateService communicateService;

  @Autowired
  private CrossHeaderMsgProcess crossHeaderMsgProcess;

  protected void onMessage(PeerConnection peer, TronMessage msg) {
    try {
      if (!communicateService.isSyncFinish()) {
        return;
      }
      switch (msg.getType()) {
        case CROSS_MSG: {
          CrossChainMessage crossChainMessage = (CrossChainMessage) msg;
          communicateService.receiveCrossMessage(peer, crossChainMessage.getCrossMessage());
          break;
        }
        case HEADER_UPDATED_NOTICE:
          crossHeaderMsgProcess.handleCrossUpdatedNotice(peer, msg);
          break;
        case HEADER_REQUEST_MESSAGE:
          crossHeaderMsgProcess.handleRequest(peer, msg);
          break;
        case HEADER_INVENTORY:
          crossHeaderMsgProcess.handleInventory(peer, msg);
          break;
        case SR_LIST:
          //blockHeaderSyncHandler.handleSrList(peer, msg);
          break;
        case EPOCH_MESSAGE:
          //blockHeaderSyncHandler.handleEpoch(peer, msg);
        default:
          throw new P2pException(TypeEnum.NO_SUCH_MESSAGE, msg.getType().toString());
      }
    } catch (Exception e) {
      processException(peer, msg, e);
    }
  }

  private void processException(PeerConnection peer, TronMessage msg, Exception ex) {
    ReasonCode code;

    if (ex instanceof P2pException) {
      TypeEnum type = ((P2pException) ex).getType();
      switch (type) {
        case BAD_BLOCK:
          code = ReasonCode.BAD_BLOCK;
          break;
        case NO_SUCH_MESSAGE:
        case MESSAGE_WITH_WRONG_LENGTH:
        case BAD_MESSAGE:
          code = ReasonCode.BAD_PROTOCOL;
          break;
        default:
          code = ReasonCode.UNKNOWN;
          break;
      }
      logger.error("cross chain message from {} process failed, {} \n type: {}, detail: {}.",
          peer.getInetAddress(), msg, type, ex.getMessage());
    } else {
      code = ReasonCode.UNKNOWN;
      logger.error("cross chain message from {} process failed, {}",
          peer.getInetAddress(), msg, ex);
    }
    peer.disconnect(code);
  }

}
