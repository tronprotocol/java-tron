package org.tron.consensus.pbft;

import com.google.protobuf.ByteString;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.consensus.dpos.DposService;
import org.tron.consensus.pbft.message.PbftMessage;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.PbftSignCapsule;
import org.tron.core.db.BlockStore;
import org.tron.core.db.RevokingDatabase;
import org.tron.core.db2.core.SnapshotManager;
import org.tron.core.event.EventBusService;
import org.tron.core.event.entity.PbftBlockCommitEvent;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.protos.Protocol.PBFTMessage.Raw;

@Slf4j(topic = "pbft")
@Component
public class PbftMessageAction {

  @Autowired
  private ChainBaseManager chainBaseManager;

  @Autowired
  private EventBusService eventBusService;

  @Autowired
  private RevokingDatabase revokingStore;

  @Autowired
  private BlockStore blockStore;

  @Autowired
  private DposService dposService;

  public synchronized void action(PbftMessage message, List<ByteString> dataSignList) {
    switch (message.getDataType()) {
      case BLOCK: {
        long blockNum = message.getNumber();
        SnapshotManager.allowCrossChain = chainBaseManager
            .getDynamicPropertiesStore().allowCrossChain();
        if (chainBaseManager.getDynamicPropertiesStore().allowCrossChain()) {
          long latestSolidifiedBlockNum = chainBaseManager.getDynamicPropertiesStore().getLatestSolidifiedBlockNum();
          dposService.updateSolidBlock();
          long latestBlockNumOnDisk = Optional.ofNullable(
                  blockStore.getLatestBlockFromDisk(1).get(0))
                  .map(BlockCapsule::getNum).orElse(0L);
          if (blockNum > chainBaseManager.getCommonDataBase().getLatestPbftBlockNum()) {
            revokingStore.fastFlush(blockNum, latestBlockNumOnDisk, latestSolidifiedBlockNum);
          }
        }
        Raw raw = message.getPbftMessage().getRawData();
        if (blockNum > chainBaseManager.getCommonDataBase().getLatestPbftBlockNum()) {
          chainBaseManager.getCommonDataBase().saveLatestPbftBlockNum(blockNum);
          chainBaseManager.getCommonDataBase().saveLatestPbftBlockHash(raw.getData().toByteArray());
        }
        chainBaseManager.getPbftSignDataStore()
            .putBlockSignData(blockNum, new PbftSignCapsule(raw.toByteString(), dataSignList));
        logger.info("commit msg block num is:{}", blockNum);

        if (chainBaseManager.getDynamicPropertiesStore().allowCrossChain()) {
          eventBusService.postEvent(
                  new PbftBlockCommitEvent(blockNum,
                          message.getPbftMessage().getRawData().getData()));
        }
      }
      break;
      case SRL: {
        Raw raw = message.getPbftMessage().getRawData();
        chainBaseManager.getPbftSignDataStore().putSrSignData(raw.getEpoch(),
            new PbftSignCapsule(raw.toByteString(), dataSignList));
        logger.info("sr commit msg :{}, epoch:{}", raw.getViewN(), raw.getEpoch());
      }
      break;
      default:
        break;
    }
  }

}
