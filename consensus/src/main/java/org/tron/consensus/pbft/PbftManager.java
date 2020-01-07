package org.tron.consensus.pbft;

import com.google.protobuf.ByteString;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.consensus.base.Param;
import org.tron.consensus.dpos.MaintenanceManager;
import org.tron.consensus.pbft.message.PbftBaseMessage;
import org.tron.consensus.pbft.message.PbftBlockMessage;
import org.tron.consensus.pbft.message.PbftSrMessage;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.BlockCapsule;

@Slf4j(topic = "pbft")
@Component
public class PbftManager {

  @Autowired
  private PbftMessageHandle pbftMessageHandle;

  @Autowired
  private MaintenanceManager maintenanceManager;

  @Autowired
  private ChainBaseManager chainBaseManager;

  private ExecutorService executorService = Executors.newFixedThreadPool(10,
      r -> new Thread(r, "Pbft"));

  @PostConstruct
  public void init() {
    maintenanceManager.setPbftManager(this);
    pbftMessageHandle.setMaintenanceManager(maintenanceManager);
  }

  public void blockPrePrepare(BlockCapsule block, long epoch) {
    if (!chainBaseManager.getDynamicPropertiesStore().allowPBFT()) {
      return;
    }
    if (!pbftMessageHandle.isSyncing()) {
      if (Param.getInstance().isEnable()) {
        doAction(PbftBlockMessage.buildPrePrepareMessage(block, epoch));
      } else {
        doAction(PbftBlockMessage.buildFullNodePrePrepareMessage(block, epoch));
      }
    }
  }

  public void srPrePrepare(BlockCapsule block, List<ByteString> currentWitness, long epoch) {
    if (!chainBaseManager.getDynamicPropertiesStore().allowPBFT()) {
      return;
    }
    if (!pbftMessageHandle.isSyncing()) {
      if (Param.getInstance().isEnable()) {
        doAction(PbftSrMessage.buildPrePrepareMessage(block, currentWitness, epoch));
      } else {
        doAction(PbftSrMessage.buildFullNodePrePrepareMessage(block, currentWitness, epoch));
      }
    }
  }

  public void forwardMessage(PbftBaseMessage message) {
    pbftMessageHandle.forwardMessage(message);
  }

  public boolean doAction(PbftBaseMessage msg) {
    executorService.submit(() -> {
      logger.info("receive pbft msg: {}", msg);
      switch (msg.getPbftMessage().getRawData().getPbftMsgType()) {
        case PREPREPARE:
          pbftMessageHandle.onPrePrepare(msg);
          break;
        case PREPARE:
          // prepare
          pbftMessageHandle.onPrepare(msg);
          break;
        case COMMIT:
          // commit
          pbftMessageHandle.onCommit(msg);
          break;
        case REQUEST:
          pbftMessageHandle.onRequestData(msg);
          break;
        case VIEW_CHANGE:
          pbftMessageHandle.onChangeView(msg);
          break;
        default:
          break;
      }
    });
    return true;
  }

  public boolean checkIsWitnessMsg(PbftBaseMessage msg) {
    long epoch = msg.getPbftMessage().getRawData().getEpoch();
    List<ByteString> witnessList;
    if (epoch > maintenanceManager.getBeforeMaintenanceTime()) {
      witnessList = maintenanceManager.getCurrentWitness();
    } else {
      witnessList = maintenanceManager.getBeforeWitness();
    }
    return witnessList.contains(ByteString.copyFrom(msg.getPublicKey()));
  }

}
