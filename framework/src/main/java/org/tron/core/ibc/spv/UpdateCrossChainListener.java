package org.tron.core.ibc.spv;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tron.common.utils.ByteArray;
import org.tron.core.ChainBaseManager;
import org.tron.core.db.CommonDataBase;
import org.tron.core.db.CrossRevokingStore;
import org.tron.core.event.EventListener;
import org.tron.core.event.entity.PbftBlockCommitEvent;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Protocol;
import org.tron.protos.contract.BalanceContract;

@Slf4j(topic = "update-cross-chain-listener")
@Service
public class UpdateCrossChainListener implements EventListener<PbftBlockCommitEvent> {
  @Autowired
  private ChainBaseManager chainBaseManager;

  @Override
  public void listener(PbftBlockCommitEvent event) {
    CrossRevokingStore crossRevokingStore = chainBaseManager.getCrossRevokingStore();
    CommonDataBase commonDataBase = chainBaseManager.getCommonDataBase();
    crossRevokingStore.getAllCrossChainUpdate().forEach(entry -> {
      if (entry.getValue() > 0 && entry.getValue() <= event.getBlockNum()) {
        long registerNum = entry.getKey();
        logger.info("start update cross chain info {}", registerNum);
        try {
          byte[] crossChainInfoBytes = crossRevokingStore.getChainInfo(registerNum);
          BalanceContract.CrossChainInfo crossChainInfo = null;
          if (crossChainInfoBytes == null) {
            throw new ContractValidateException("ChainId has not been registered!");
          }
          try {
            crossChainInfo = BalanceContract.CrossChainInfo.parseFrom(crossChainInfoBytes);
          } catch (InvalidProtocolBufferException e) {
            throw new ContractValidateException(
                    "the format of crossChainInfo stored in db is not right!");
          }
          String chainId = ByteArray.toHexString(crossChainInfo.getChainId().toByteArray());
          commonDataBase.saveLatestHeaderBlockNum(chainId,
                  crossChainInfo.getBeginSyncHeight() - 1, true);
          commonDataBase.saveLatestBlockHeaderHash(chainId,
                  ByteArray.toHexString(crossChainInfo.getParentBlockHash().toByteArray()));
          commonDataBase.saveChainMaintenanceTimeInterval(chainId,
                  crossChainInfo.getMaintenanceTimeInterval());

          long round = crossChainInfo.getBlockTime() / crossChainInfo.getMaintenanceTimeInterval();
          long epoch = (round + 1) * crossChainInfo.getMaintenanceTimeInterval();
          if (crossChainInfo.getBlockTime() % crossChainInfo.getMaintenanceTimeInterval() == 0) {
            epoch = epoch - crossChainInfo.getMaintenanceTimeInterval();
            epoch = epoch < 0 ? 0 : epoch;
          }
          Protocol.SRL.Builder srlBuilder = Protocol.SRL.newBuilder();
          srlBuilder.addAllSrAddress(crossChainInfo.getSrListList());
          Protocol.PBFTMessage.Raw pbftMsgRaw = Protocol.PBFTMessage.Raw.newBuilder()
                  .setData(srlBuilder.build().toByteString())
                  .setEpoch(epoch).build();
          Protocol.PBFTCommitResult.Builder builder = Protocol.PBFTCommitResult.newBuilder();
          builder.setData(pbftMsgRaw.toByteString());
          commonDataBase.saveSRL(chainId, epoch, builder.build());
          commonDataBase.saveCrossNextMaintenanceTime(chainId, epoch);
          int agreeNodeCount = crossChainInfo.getSrListList().size() * 2 / 3 + 1;
          commonDataBase.saveAgreeNodeCount(chainId, agreeNodeCount);
          crossRevokingStore.deleteCrossChainUpdate(registerNum);
          logger.info("success update cross chain info {}", chainId);
        } catch (ContractValidateException e) {
          logger.warn("update cross chain info failed, error message:" + e.getMessage());
        }
      }
    });
  }
}
