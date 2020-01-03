package org.tron.consensus.pbft;

import com.google.protobuf.ByteString;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.consensus.base.Param;
import org.tron.consensus.pbft.message.PbftBaseMessage;
import org.tron.consensus.pbft.message.PbftBlockMessage;
import org.tron.consensus.pbft.message.PbftSrMessage;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.PbftSignCapsule;
import org.tron.core.event.EventBusService;
import org.tron.core.event.entity.PbftBlockCommitEvent;
import org.tron.protos.Protocol.PBFTMessage.Raw;
import org.tron.protos.Protocol.SRL;

@Slf4j(topic = "pbft")
@Component
public class PbftMessageAction {

  private long checkPoint = 0;

  @Autowired
  private ChainBaseManager chainBaseManager;

  @Autowired
  private EventBusService eventBusService;

  public void action(PbftBaseMessage message, List<ByteString> dataSignList) {
    switch (message.getType()) {
      case PBFT_BLOCK_MSG: {
        PbftBlockMessage blockMessage = (PbftBlockMessage) message;
        long blockNum = blockMessage.getNumber();
        if (blockNum - checkPoint >= Param.getInstance().getCheckMsgCount()) {
          checkPoint = blockNum;
          chainBaseManager.getCommonDataBase().saveLatestPbftBlockNum(blockNum);
          Raw raw = blockMessage.getPbftMessage().getRawData();
          chainBaseManager.getPbftSignDataStore()
              .putBlockSignData(blockNum, new PbftSignCapsule(raw.toByteString(), dataSignList));
          logger.info("commit msg block num is:{}", blockNum);
        }
        eventBusService.postEvent(
            new PbftBlockCommitEvent(blockNum, blockMessage.getPbftMessage().getRawData().getData()));
      }
      break;
      case PBFT_SR_MSG: {
        try {
          PbftSrMessage srMessage = (PbftSrMessage) message;
          SRL srList = SRL
              .parseFrom(srMessage.getPbftMessage().getRawData().getData().toByteArray());
          Raw raw = srMessage.getPbftMessage().getRawData();
          chainBaseManager.getPbftSignDataStore().putSrSignData(srList.getEpoch(),
              new PbftSignCapsule(raw.toByteString(), dataSignList));
          logger.info("sr commit msg :{}, cycle:{}", srMessage.getNumber(), srList.getEpoch());
        } catch (Exception e) {
          logger.error("process the sr list error!", e);
        }
      }
      break;
      default:
        break;
    }
  }

}
