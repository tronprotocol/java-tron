package org.tron.core.ibc.communicate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.ChainBaseManager;
import org.tron.core.db.TransactionStore;
import org.tron.core.exception.BadItemException;
import org.tron.protos.Protocol.CrossMessage;

@Slf4j(topic = "Communicate")
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
    TransactionStore transactionStore = chainBaseManager.getTransactionStore();

    try {
      transactionStore.get(hash.getBytes()).getBlockNum();

    } catch (BadItemException e) {
      logger.error("{}", e.getMessage());
    }
    return false;
  }

  @Override
  public boolean broadcastCrossMessage(CrossMessage crossMessage) {
    return false;
  }
}
