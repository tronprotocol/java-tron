package org.tron.core.ibc.communicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.ChainBaseManager;
import org.tron.protos.Protocol.CrossMessage;

@Service
public class CommunicateService implements Communicate {

  @Autowired
  private ChainBaseManager chainBaseManager;

  @Override
  public void sendCrossMessage(CrossMessage crossMessage) {

  }

  @Override
  public void receiveCrossMessage(CrossMessage crossMessage) {

  }

  @Override
  public boolean validProof(CrossMessage crossMessage) {
    return false;
  }

  @Override
  public boolean checkCommit(Sha256Hash hash) {

    return false;
  }

  @Override
  public boolean broadcastCrossMessage(CrossMessage crossMessage) {
    return false;
  }
}
