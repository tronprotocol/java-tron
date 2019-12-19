package org.tron.core.ibc.communicate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.ChainBaseManager;
import org.tron.core.db.CrossStore;
import org.tron.core.event.EventListener;
import org.tron.core.event.entity.PbftBlockEvent;
import org.tron.protos.Protocol.CrossMessage;
import org.tron.protos.Protocol.CrossMessage.Type;

@Service
public class PbftBlockListener implements EventListener<PbftBlockEvent> {

  private Map<Long, List<Sha256Hash>> callBackTx = new ConcurrentHashMap<>();

  @Autowired
  private CommunicateService communicateService;

  @Autowired
  private ChainBaseManager chainBaseManager;

  @Override
  public void listener(PbftBlockEvent event) {
    List<Sha256Hash> txList = callBackTx.remove(event.getBlockNum());
    if (txList != null) {
      txList.forEach(hash -> {
        if (communicateService.checkCommit(hash)) {
          CrossStore crossStore = chainBaseManager.getCrossStore();
          CrossMessage crossMessage = crossStore.getReceiveCrossMsgUnEx(hash);
          if (crossMessage.getType() == Type.DATA) {
            //send the ack to an other chain
            if (crossMessage.getToChainId().equals(communicateService.getLocalChainId())) {
              crossMessage = crossMessage.toBuilder().setToChainId(crossMessage.getFromChainId())
                  .setFromChainId(crossMessage.getToChainId()).setType(Type.ACK).build();
            }
            communicateService.sendCrossMessage(crossMessage);
          } else if (crossMessage.getType() == Type.ACK) {
            //delete the send to end chain data


          } else {

          }
        }
      });
    }
  }

  public void addCallBackTx(long blockNum, Sha256Hash txHash) {
    CrossStore crossStore = chainBaseManager.getCrossStore();
    CrossMessage crossMessage = crossStore.getReceiveCrossMsgUnEx(txHash);
    if (crossMessage != null) {
      if (callBackTx.containsKey(blockNum)) {
        callBackTx.get(blockNum).add(txHash);
      } else {
        List<Sha256Hash> list = new ArrayList<>();
        list.add(txHash);
        callBackTx.put(blockNum, list);
      }
    }
  }
}
