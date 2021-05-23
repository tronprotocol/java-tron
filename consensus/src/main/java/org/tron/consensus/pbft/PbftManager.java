package org.tron.consensus.pbft;

import com.google.protobuf.ByteString;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.consensus.base.Param.Miner;
import org.tron.consensus.dpos.MaintenanceManager;
import org.tron.consensus.pbft.message.PbftBaseMessage;
import org.tron.consensus.pbft.message.PbftMessage;
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

  @PreDestroy
  public void destroy() {
    executorService.shutdown();
  }

  public void blockPrePrepare(BlockCapsule block, long epoch, List<ByteString> signSrList) {
    if (!chainBaseManager.getDynamicPropertiesStore().allowPBFT()) {
      return;
    }
    if (!pbftMessageHandle.isSyncing()) {
      List<Miner> minerList = pbftMessageHandle.getSrMinerList(signSrList);
      if (CollectionUtils.isNotEmpty(minerList)) {
        for (Miner miner : minerList) {
          doAction(PbftMessage.prePrepareBlockMsg(block, epoch, miner));
        }
      } else {
        doAction(PbftMessage.fullNodePrePrepareBlockMsg(block, epoch));
      }
    }
  }

  public void srPrePrepare(BlockCapsule block, List<ByteString> currentWitness, long epoch,
      List<ByteString> signSrList) {
    if (!chainBaseManager.getDynamicPropertiesStore().allowPBFT()) {
      return;
    }
    if (!pbftMessageHandle.isSyncing()) {
      List<Miner> minerList = pbftMessageHandle.getSrMinerList(signSrList);
      if (CollectionUtils.isNotEmpty(minerList)) {
        for (Miner miner : minerList) {
          doAction(PbftMessage.prePrepareSRLMsg(block, currentWitness, epoch, miner));
        }
      } else {
        doAction(PbftMessage.fullNodePrePrepareSRLMsg(block, currentWitness, epoch));
      }
    }
  }

  public void forwardMessage(PbftBaseMessage message) {
    pbftMessageHandle.forwardMessage(message);
  }

  public boolean doAction(PbftMessage msg) {
    executorService.submit(() -> {
      logger.info("receive pbft msg: {}", msg);
      switch (msg.getPbftMessage().getRawData().getMsgType()) {
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

  public boolean verifyMsgSign(PbftBaseMessage msg) {
    return pbftMessageHandle.verifyMsgSign(msg);
  }

}