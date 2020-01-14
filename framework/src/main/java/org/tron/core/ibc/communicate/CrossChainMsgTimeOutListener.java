package org.tron.core.ibc.communicate;

import org.tron.core.event.EventListener;
import org.tron.core.event.entity.PbftBlockCommitEvent;

public class CrossChainMsgTimeOutListener implements EventListener<PbftBlockCommitEvent> {

  @Override
  public void listener(PbftBlockCommitEvent event) {

  }
}
