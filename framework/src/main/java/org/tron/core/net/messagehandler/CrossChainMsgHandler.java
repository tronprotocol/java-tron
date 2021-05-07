package org.tron.core.net.messagehandler;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.ChainBaseManager;
import org.tron.core.db.CrossStore;
import org.tron.core.db.Manager;
import org.tron.core.exception.P2pException;
import org.tron.core.exception.P2pException.TypeEnum;
import org.tron.core.ibc.communicate.CommunicateService;
import org.tron.core.net.message.CrossChainMessage;
import org.tron.core.net.message.TronMessage;
import org.tron.core.net.peer.PeerConnection;
import org.tron.protos.Protocol.CrossMessage.Type;
import org.tron.protos.Protocol.ReasonCode;

@Slf4j(topic = "CROSS")
@Component
public class CrossChainMsgHandler implements TronMsgHandler {

  @Autowired
  private CommunicateService communicateService;

  @Autowired
  private Manager manager;

  @Override
  public void processMessage(PeerConnection peer, TronMessage msg) throws P2pException {
    CrossChainMessage crossChainMessage = (CrossChainMessage) msg;
    if (!communicateService.broadcastCheck(crossChainMessage.getCrossMessage())) {
      return;
    }
    logger.info("receive a cross tx: {}", crossChainMessage.getMessageId());
    manager.addCrossTx(crossChainMessage.getCrossMessage());
    communicateService.broadcastCrossMessage(crossChainMessage.getCrossMessage());
  }
}
