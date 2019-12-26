package org.tron.core.net.messagehandler;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.PbftSignCapsule;
import org.tron.core.db.BlockHeaderStore;
import org.tron.core.db.CommonDataBase;
import org.tron.core.db.PbftSignDataStore;
import org.tron.core.exception.P2pException;
import org.tron.core.net.message.BlockHeaderInventoryMesasge;
import org.tron.core.net.message.BlockHeaderRequestMessage;
import org.tron.core.net.message.BlockHeaderUpdatedNoticeMessage;
import org.tron.core.net.message.BlockInventoryMessage;
import org.tron.core.net.message.EpochMessage;
import org.tron.core.net.message.SrListMessage;
import org.tron.core.net.message.TronMessage;
import org.tron.core.net.peer.PeerConnection;
import org.tron.protos.Protocol;

import java.util.List;

@Slf4j(topic = "net")
@Component
public class BlockHeaderSyncHandler {

  @Autowired
  private BlockHeaderStore blockHeaderStore;

  @Autowired
  private PbftSignDataStore pbftSignDataStore;

  @Autowired
  private CommonDataBase commonDataBase;

  public void HandleUpdatedNotice(PeerConnection peer, TronMessage msg) {
    BlockHeaderUpdatedNoticeMessage noticeMessage = (BlockHeaderUpdatedNoticeMessage) msg;
    long currentBlockHeight = noticeMessage.getCurrentBlockHeight();
  }

  public void handleRequest(PeerConnection peer, TronMessage msg) {
    BlockHeaderRequestMessage requestMessage = (BlockHeaderRequestMessage) msg;
    byte[] chainId = requestMessage.getChainId();
    long blockHeight = requestMessage.getBlockHeight();
    long length = requestMessage.getLength();
  }

  public void handleInventory(PeerConnection peer, TronMessage msg) {
    BlockHeaderInventoryMesasge blockHeaderInventoryMesasge = (BlockHeaderInventoryMesasge) msg;
    List<Protocol.BlockHeader> blockHeaders = blockHeaderInventoryMesasge.getBlockHeaders();
  }

  public void handleSrList(PeerConnection peer, TronMessage msg) {
    SrListMessage srListMessage = (SrListMessage) msg;
    long epoch = srListMessage.getEpoch();
    PbftSignCapsule pbftSignCapsule = new PbftSignCapsule(msg.getData());
    pbftSignDataStore.putSrSignData(epoch, pbftSignCapsule);
  }

  public void handleEpoch(PeerConnection peer, TronMessage msg) {
    EpochMessage epochMessage = (EpochMessage) msg;
    long currentEpoch = epochMessage.getCurrentEpoch();
  }
}
