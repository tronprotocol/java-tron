package org.tron.core;

import com.google.protobuf.ByteString;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI.AssetIssueList;
import org.tron.api.GrpcAPI.TransactionList;
import org.tron.api.GrpcAPI.WitnessList;
import org.tron.common.utils.ByteArray;
import org.tron.core.db.api.StoreAPI;
import org.tron.protos.Contract.AssetIssueContract;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Witness;

@Slf4j
@Component
public class WalletSolidity {

  @Autowired
  private StoreAPI storeAPI;

  public WitnessList getWitnessList() {
    List<Witness> witnessAll = storeAPI.getWitnessAll();
    WitnessList witnessList = WitnessList.newBuilder().addAllWitnesses(witnessAll).build();
    return witnessList;
  }

  public AssetIssueList getAssetIssueList() {
    List<AssetIssueContract> assetIssueAll = storeAPI.getAssetIssueAll();
    AssetIssueList assetIssueList =
        AssetIssueList.newBuilder().addAllAssetIssue(assetIssueAll).build();
    return assetIssueList;
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
