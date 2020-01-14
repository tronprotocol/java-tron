package org.tron.consensus.pbft;

import com.google.protobuf.ByteString;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.consensus.pbft.message.PbftMessage;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.PbftSignCapsule;
import org.tron.core.db.RevokingDatabase;
import org.tron.core.event.EventBusService;
import org.tron.core.event.entity.PbftBlockCommitEvent;
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

  public void action(PbftMessage message, List<ByteString> dataSignList) {
    switch (message.getDataType()) {
      case BLOCK: {
        long blockNum = message.getNumber();
        revokingStore.fastFlush();
        chainBaseManager.getCommonDataBase().saveLatestPbftBlockNum(blockNum);
        Raw raw = message.getPbftMessage().getRawData();
        chainBaseManager.getPbftSignDataStore()
            .putBlockSignData(blockNum, new PbftSignCapsule(raw.toByteString(), dataSignList));
        chainBaseManager.getCommonDataBase().saveLatestPbftBlockHash(raw.getData().toByteArray());
        logger.info("commit msg block num is:{}", blockNum);

        eventBusService.postEvent(
            new PbftBlockCommitEvent(blockNum, message.getPbftMessage().getRawData().getData()));
      }
      break;
      case SRL: {
        Raw raw = message.getPbftMessage().getRawData();
        chainBaseManager.getPbftSignDataStore().putSrSignData(message.getEpoch(),
            new PbftSignCapsule(raw.toByteString(), dataSignList));
        logger.info("sr commit msg :{}, epoch:{}", message.getNumber(), message.getEpoch());
      }
      break;
      default:
        break;
    }
  }

}
