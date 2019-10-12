package org.tron.consensus.pbft;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.consensus.base.Param;
import org.tron.consensus.pbft.message.PbftBaseMessage;
import org.tron.consensus.pbft.message.PbftBlockMessage;
import org.tron.consensus.pbft.message.PbftSrMessage;
import org.tron.core.db.CommonDataBase;

@Slf4j(topic = "pbft")
@Component
public class PbftMessageAction {

  private long checkPoint = 0;

  @Autowired
  private CommonDataBase commonDataBase;

  public void action(PbftBaseMessage message) {
    switch (message.getType()) {
      case PBFT_BLOCK_MSG: {
        PbftBlockMessage blockMessage = (PbftBlockMessage) message;
        long blockNum = blockMessage.getBlockNum();
        if (blockNum - checkPoint >= Param.getInstance().getCheckMsgCount()) {
          checkPoint = blockNum;
          commonDataBase.saveLatestPbftBlockNum(blockNum);
          logger.info("commit msg block num is:{}", blockNum);
        }
      }
      break;
      case PBFT_SR_MSG: {
        PbftSrMessage srMessage = (PbftSrMessage) message;
        String srString = srMessage.getPbftMessage().getRawData().getData().toStringUtf8();
        commonDataBase.saveCurrentSrList(srString);
        logger.info("sr commit msg :{}, {}", srMessage.getBlockNum(),
            JSON.parseArray(srString, String.class));
      }
      break;
      default:
        break;
    }
  }

}
