package org.tron.core.ibc.spv;

import static org.tron.core.ibc.spv.CrossHeaderMsgProcess.SYNC_NUMBER;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ByteArray;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.capsule.BlockHeaderCapsule;
import org.tron.core.capsule.PbftSignCapsule;
import org.tron.core.ibc.spv.message.BlockHeaderInventoryMesasge;
import org.tron.core.ibc.spv.message.BlockHeaderRequestMessage;
import org.tron.core.net.message.TronMessage;
import org.tron.core.net.peer.PeerConnection;
import org.tron.protos.Protocol.PBFTCommitResult;
import org.tron.protos.Protocol.SignedBlockHeader;
import org.tron.protos.Protocol.SignedBlockHeader.Builder;

@Slf4j(topic = "cross-localHeaderMsgProcess")
@Service
public class LocalHeaderMsgProcess {

  private volatile Map<String, Long> latestMaintenanceTimeMap = new ConcurrentHashMap<>();

  @Autowired
  private ChainBaseManager chainBaseManager;

  //todo:blockHeight should valid
  public void handleRequest(PeerConnection peer, TronMessage msg) {
    BlockHeaderRequestMessage requestMessage = (BlockHeaderRequestMessage) msg;
    byte[] chainId = requestMessage.getChainId().toByteArray();
    String chainIdString = ByteArray.toHexString(chainId);
    long blockHeight = requestMessage.getBlockHeight();
    long latestMaintenanceTime = requestMessage.getLatestMaintenanceTime();
    long currentBlockHeight = chainBaseManager.getCommonDataBase()
        .getLatestHeaderBlockNum(chainIdString);
    if (!chainBaseManager.chainIsSelected(requestMessage.getChainId())) {
      return;
    }
    logger.info("handleRequest, peer:{}, chainId:{}, request num:{}, current:{}, ",
        peer, chainIdString, blockHeight, currentBlockHeight);
    List<SignedBlockHeader> blockHeaders = new ArrayList<>();
    if (currentBlockHeight > blockHeight) {
      long height = blockHeight + 1;
      boolean isMaintenanceTimeUpdated = false;
      for (int i = 1; i <= SYNC_NUMBER && height < currentBlockHeight; i++) {
        height = blockHeight + i;
        BlockId blockId = chainBaseManager.getBlockHeaderIndexStore()
            .getUnchecked(chainIdString, height);
        BlockHeaderCapsule blockHeaderCapsule = chainBaseManager.getBlockHeaderStore()
            .getUnchecked(chainIdString, blockId);
        PbftSignCapsule pbftSignCapsule = chainBaseManager.getPbftSignDataStore()
            .getCrossBlockSignData(chainIdString, height);
        SignedBlockHeader.Builder builder = SignedBlockHeader.newBuilder();
        builder.setBlockHeader(blockHeaderCapsule.getInstance());
        if (pbftSignCapsule != null) {
          builder.addAllSrsSignature(pbftSignCapsule.getInstance().getSignatureList());
        }
        //
        isMaintenanceTimeUpdated = setSrList(builder, chainIdString,
                blockHeaderCapsule.getTimeStamp(),
                latestMaintenanceTime, isMaintenanceTimeUpdated);
        blockHeaders.add(builder.build());
      }
    } else {
      //todo
    }
    BlockHeaderInventoryMesasge inventoryMesasge =
        new BlockHeaderInventoryMesasge(chainIdString, currentBlockHeight, blockHeaders);
    peer.sendMessage(inventoryMesasge);
  }

  protected boolean setSrList(Builder builder, String chainIdString, long blockTime,
                           long latestMaintenanceTime, boolean isMaintenanceTimeUpdated) {
    long maintenanceTimeInterval = chainBaseManager.getCommonDataBase()
            .getChainMaintenanceTimeInterval(chainIdString);
    long round = blockTime / maintenanceTimeInterval;
    long maintenanceTime = (round + 1) * maintenanceTimeInterval;
    // Long latestMaintenanceTimeTmp = latestMaintenanceTimeMap.get(chainIdString);
    // latestMaintenanceTimeTmp = latestMaintenanceTimeTmp == null ? 0 : latestMaintenanceTimeTmp;
    logger.debug("set sr list, maintenanceTime:{}, latestMaintenanceTime:{}", maintenanceTime,
        latestMaintenanceTime);
    if ((maintenanceTime > latestMaintenanceTime && !isMaintenanceTimeUpdated)
            || (blockTime % maintenanceTimeInterval == 0)) {
      PBFTCommitResult pbftCommitResult = chainBaseManager.getCommonDataBase()
          .getSRLCommit(chainIdString, maintenanceTime);
      if (pbftCommitResult != null) {
        // latestMaintenanceTimeMap.put(chainIdString, maintenanceTime);
        builder.setSrList(pbftCommitResult);
        return true;
      }
    }
    return isMaintenanceTimeUpdated;
  }

}
