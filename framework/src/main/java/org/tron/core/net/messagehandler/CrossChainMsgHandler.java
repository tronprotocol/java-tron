package org.tron.core.net.messagehandler;

import com.google.protobuf.InvalidProtocolBufferException;
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

@Component
public class CrossChainMsgHandler implements TronMsgHandler {

  @Autowired
  private ChainBaseManager chainBaseManager;

  @Autowired
  private CommunicateService communicateService;

  @Autowired
  private Manager manager;

  @Override
  public void processMessage(PeerConnection peer, TronMessage msg) throws P2pException {
    CrossChainMessage crossChainMessage = (CrossChainMessage) msg;
    CrossStore crossStore = chainBaseManager.getCrossStore();
    if (!communicateService.isSyncFinish()) {
      return;
    }
    if (crossChainMessage.getCrossMessage().getType() != Type.TIME_OUT
        && !communicateService.validProof(crossChainMessage.getCrossMessage())) {
      //todo: define a new reason code
      //peer.disconnect(ReasonCode.BAD_TX);
      return;
    }
    Sha256Hash txId = Sha256Hash
        .of(true, crossChainMessage.getCrossMessage().getTransaction().getRawData().toByteArray());
    try {
      if (crossStore.getReceiveCrossMsg(txId) != null) {
        return;
      }
    } catch (InvalidProtocolBufferException e) {
      throw new P2pException(TypeEnum.BAD_MESSAGE, "not a cross message");
    }
    //todo:timeout message how to do,save or not
    crossStore.saveReceiveCrossMsg(txId, crossChainMessage.getCrossMessage());
    manager.addCrossTx(crossChainMessage.getCrossMessage());
    communicateService.broadcastCrossMessage(crossChainMessage.getCrossMessage());
  }
}
