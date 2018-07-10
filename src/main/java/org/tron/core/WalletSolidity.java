package org.tron.core;

import com.google.protobuf.ByteString;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI.TransactionList;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.TransactionInfoCapsule;
import org.tron.core.db.Manager;
import org.tron.core.db.api.StoreAPI;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.NonUniqueObjectException;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.TransactionInfo;

@Slf4j
@Component
public class WalletSolidity {

  @Autowired
  private StoreAPI storeAPI;
  @Autowired
  private Manager dbManager;

  public Transaction getTransactionById(ByteString id) {
    try {
      Transaction transactionById = storeAPI
          .getTransactionById(ByteArray.toHexString(id.toByteArray()));
      return transactionById;
    } catch (NonUniqueObjectException e) {
      e.printStackTrace();
    }
    return null;
  }

  public TransactionInfo getTransactionInfoById(ByteString id) {
    if (Objects.isNull(id)) {
      return null;
    }
    TransactionInfoCapsule transactionInfoCapsule = null;
    try {
      transactionInfoCapsule = dbManager.getTransactionHistoryStore()
          .get(id.toByteArray());
    } catch (BadItemException e) {
    }
    if (transactionInfoCapsule != null) {
      return transactionInfoCapsule.getInstance();
    }
    return null;
  }

  public TransactionList getTransactionsFromThis(ByteString thisAddress, long offset, long limit) {
    List<Transaction> transactionsFromThis = storeAPI
        .getTransactionsFromThis(ByteArray.toHexString(thisAddress.toByteArray()), offset, limit);
    TransactionList transactionList = TransactionList.newBuilder()
        .addAllTransaction(transactionsFromThis).build();
    return transactionList;
  }

  public TransactionList getTransactionsToThis(ByteString toAddress, long offset, long limit) {
    List<Transaction> transactionsToThis = storeAPI
        .getTransactionsToThis(ByteArray.toHexString(toAddress.toByteArray()), offset, limit);
    TransactionList transactionList = TransactionList.newBuilder()
        .addAllTransaction(transactionsToThis).build();
    return transactionList;
  }
}
