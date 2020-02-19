package org.tron.core.ibc.communicate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tron.core.ChainBaseManager;
import org.tron.core.db.CrossStore;
import org.tron.core.event.EventListener;
import org.tron.core.event.entity.PbftBlockCommitEvent;

@Slf4j(topic = "pbft-block-listener")
@Service
public class CrossChainMsgTimeOutListener implements EventListener<PbftBlockCommitEvent> {

  @Autowired
  private ChainBaseManager chainBaseManager;

  @Override
  public void listener(PbftBlockCommitEvent event) {
    CrossStore crossStore = chainBaseManager.getCrossStore();

  }
}
