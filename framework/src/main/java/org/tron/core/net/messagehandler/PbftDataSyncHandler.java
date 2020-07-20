package org.tron.core.net.messagehandler;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tron.core.ChainBaseManager;
import org.tron.core.db.PbftSignDataStore;
import org.tron.core.exception.P2pException;
import org.tron.core.net.message.PbftCommitMessage;
import org.tron.core.net.message.TronMessage;
import org.tron.core.net.peer.PeerConnection;
import org.tron.protos.Protocol.PBFTMessage.DataType;
import org.tron.protos.Protocol.PBFTMessage.Raw;

@Slf4j(topic = "pbft-data-sync")
@Service
public class PbftDataSyncHandler implements TronMsgHandler {

  @Autowired
  private ChainBaseManager chainBaseManager;

  @Override
  public void processMessage(PeerConnection peer, TronMessage msg) throws P2pException {
    PbftCommitMessage pbftCommitMessage = (PbftCommitMessage) msg;
    PbftSignDataStore pbftSignDataStore = chainBaseManager.getPbftSignDataStore();
    try {
      Raw raw = Raw.parseFrom(pbftCommitMessage.getPBFTCommitResult().getData());
      if (raw.getDataType() == DataType.BLOCK) {
        if (pbftSignDataStore.getBlockSignData(raw.getViewN()) == null) {
          pbftSignDataStore
              .putBlockSignData(raw.getViewN(), pbftCommitMessage.getPbftSignCapsule());
        }
      } else if (raw.getDataType() == DataType.SRL) {
        if (pbftSignDataStore.getSrSignData(raw.getEpoch()) == null) {
          pbftSignDataStore.putSrSignData(raw.getEpoch(), pbftCommitMessage.getPbftSignCapsule());
        }
      }
    } catch (InvalidProtocolBufferException e) {
      logger.error("", e);
    }
  }
}
