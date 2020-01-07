package org.tron.consensus.pbft;

import com.google.protobuf.ByteString;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.consensus.base.Param;
import org.tron.consensus.pbft.message.PbftMessage;
import org.tron.core.capsule.PbftSignCapsule;
import org.tron.core.db.CommonDataBase;
import org.tron.core.db.PbftSignDataStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.protos.Protocol.PBFTMessage.Raw;
import org.tron.protos.Protocol.SRL;

@Slf4j(topic = "pbft")
@Component
public class PbftMessageAction {

  private long checkPoint = 0;

  @Autowired
  private CommonDataBase commonDataBase;
  @Autowired
  private DynamicPropertiesStore dynamicPropertiesStore;
  @Autowired
  private PbftSignDataStore pbftSignDataStore;

  public void action(PbftMessage message, List<ByteString> dataSignList) {
    switch (message.getDataType()) {
      case BLOCK: {
        long blockNum = message.getNumber();
        if (blockNum - checkPoint >= Param.getInstance().getCheckMsgCount()) {
          checkPoint = blockNum;
          commonDataBase.saveLatestPbftBlockNum(blockNum);
          Raw raw = message.getPbftMessage().getRawData();
          pbftSignDataStore
              .putBlockSignData(blockNum, new PbftSignCapsule(raw.toByteString(), dataSignList));
          logger.info("commit msg block num is:{}", blockNum);
        }
      }
      break;
      case SRL: {
        try {
          SRL srList = SRL
              .parseFrom(message.getPbftMessage().getRawData().getData().toByteArray());
          Raw raw = message.getPbftMessage().getRawData();
          pbftSignDataStore.putSrSignData(message.getEpoch(),
              new PbftSignCapsule(raw.toByteString(), dataSignList));
          logger.info("sr commit msg :{}, epoch:{}", message.getNumber(), message.getEpoch());
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
