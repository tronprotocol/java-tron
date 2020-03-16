package org.tron.core.ibc.common;

import com.google.protobuf.ByteString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.TransactionStore;
import org.tron.core.ibc.communicate.CommunicateService;
import org.tron.protos.Protocol.CrossMessage;
import org.tron.protos.Protocol.CrossMessage.Type;

@Service
public class CrossChainService {

  @Autowired
  private ChainBaseManager chainBaseManager;

  @Autowired
  private CommunicateService communicateService;

  public boolean checkCrossChainCommit(ByteString txId) {
    TransactionStore transactionStore = chainBaseManager.getTransactionStore();
    TransactionCapsule txCapsule = transactionStore.getUnchecked(txId.toByteArray());
    if (txCapsule == null) {
      return false;
    }
    Sha256Hash callBackTx = CrossUtils.getAddSourceTxId(txCapsule.getInstance());
    CrossMessage crossMessage = transactionStore.getCrossMessage(callBackTx.getBytes());
    if (crossMessage == null || crossMessage.getType() != Type.ACK) {
      return false;
    }
    if (!communicateService.checkCommit(callBackTx)) {
      return false;
    }
    return true;
  }
}
