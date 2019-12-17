package org.tron.consensus.pbft;

import com.alibaba.fastjson.JSON;
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
import org.tron.protos.Protocol.PbftMessage.Raw;

@Slf4j(topic = "pbft")
@Component
public class PbftMessageAction {

  private long checkPoint = 0;

  @Autowired
  private ChainBaseManager chainBaseManager;

  public void action(PbftBaseMessage message, List<ByteString> dataSignList) {
    switch (message.getType()) {
      case PBFT_BLOCK_MSG: {
        PbftBlockMessage blockMessage = (PbftBlockMessage) message;
        long blockNum = blockMessage.getBlockNum();
        if (blockNum - checkPoint >= Param.getInstance().getCheckMsgCount()) {
          checkPoint = blockNum;
          chainBaseManager.getCommonDataBase().saveLatestPbftBlockNum(blockNum);
          Raw raw = blockMessage.getPbftMessage().getRawData();
          chainBaseManager.getPbftSignDataStore()
              .putBlockSignData(blockNum, new PbftSignCapsule(raw.getData(), dataSignList));
          logger.info("commit msg block num is:{}", blockNum);
        }
      }
      break;
      case PBFT_SR_MSG: {
        PbftSrMessage srMessage = (PbftSrMessage) message;
        String srString = srMessage.getPbftMessage().getRawData().getData().toStringUtf8();
        long cycle = chainBaseManager.getDynamicPropertiesStore().getCurrentCycleNumber();
        Raw raw = srMessage.getPbftMessage().getRawData();
        chainBaseManager.getPbftSignDataStore()
            .putSrSignData(cycle, new PbftSignCapsule(raw.getData(), dataSignList));
        logger.info("sr commit msg :{}, {}", srMessage.getBlockNum(),
            JSON.parseArray(srString, String.class));
      }
      break;
      default:
        break;
    }
  }

}
