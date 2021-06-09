package org.tron.core.ibc.spv;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ByteArray;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.PbftSignCapsule;
import org.tron.core.db.Manager;
import org.tron.core.event.EventListener;
import org.tron.core.event.entity.PbftBlockCommitEvent;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.ibc.communicate.CommunicateService;
import org.tron.core.ibc.connect.CrossChainConnectPool;
import org.tron.core.ibc.spv.message.BlockHeaderUpdatedNoticeMessage;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.BlockHeader;
import org.tron.protos.Protocol.SignedBlockHeader.Builder;

@Slf4j(topic = "cross-blockheader-listener")
@Service
public class SendUpdateBlockHeaderListener implements EventListener<PbftBlockCommitEvent> {

  private static volatile long latestMaintenanceTime;

  @Autowired
  private ChainBaseManager chainBaseManager;

  @Autowired
  private CrossChainConnectPool crossChainConnectPool;

  @Autowired
  private Manager manager;

  @Autowired
  private CommunicateService communicateService;

  @Override
  public void listener(PbftBlockCommitEvent event) {
    if (crossChainConnectPool.getCrossChainConnectPool().size() <= 0) {
      return;
    }
    PbftSignCapsule pbftSignCapsule = chainBaseManager.getPbftSignDataStore()
        .getBlockSignData(event.getBlockNum());
    Protocol.SignedBlockHeader.Builder signedBlockHeaderBuilder = Protocol.SignedBlockHeader
        .newBuilder();
    BlockCapsule blockCapsule = null;
    try {
      blockCapsule = manager.getBlockByNum(event.getBlockNum());
    } catch (ItemNotFoundException | BadItemException e) {
      logger.warn("{}", e.getMessage());
    }
    if (blockCapsule == null) {
      return;
    }
    BlockHeader blockHeader = blockCapsule.getInstance().getBlockHeader();
    signedBlockHeaderBuilder.setBlockHeader(blockHeader)
        .addAllSrsSignature(pbftSignCapsule.getInstance().getSignatureList());
    setSrList(signedBlockHeaderBuilder, blockCapsule.getTimeStamp());
    Protocol.BlockHeaderUpdatedNotice notice = Protocol.BlockHeaderUpdatedNotice.newBuilder()
        .setChainId(communicateService.getLocalChainId())
        .setSignedBlockHeader(signedBlockHeaderBuilder)
        .build();
    crossChainConnectPool.getCrossChainConnectPool().values().forEach(peerConnections -> {
      peerConnections.forEach(peerConnection -> {
        BlockHeaderUpdatedNoticeMessage noticeMessage = new BlockHeaderUpdatedNoticeMessage(notice);
        logger.info("triggerNotice, peer:{}, notice num:{}, chainId:{}",
            peerConnection, noticeMessage.getCurrentBlockHeight(),
            ByteArray.toHexString(noticeMessage.getChainId()));
        peerConnection.fastSend(noticeMessage);
      });
    });
  }

  protected synchronized void setSrList(Builder builder, long blockTime) {
    //
    long round = blockTime / CommonParameter.getInstance().getMaintenanceTimeInterval();
    long maintenanceTime = (round + 1) * CommonParameter.getInstance().getMaintenanceTimeInterval();
    if (maintenanceTime > latestMaintenanceTime) {
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        logger.error("", e);
      }
      PbftSignCapsule srSignCapsule = chainBaseManager.getPbftSignDataStore()
          .getSrSignData(maintenanceTime);
      logger.debug("set sr list, maintenanceTime:{}, latestMaintenanceTime:{}, srSignCapsule:{}",
          maintenanceTime, latestMaintenanceTime, srSignCapsule);
      if (srSignCapsule != null) {
        latestMaintenanceTime = maintenanceTime;
        builder.setSrList(srSignCapsule.getInstance());
      }
    }
  }
}
