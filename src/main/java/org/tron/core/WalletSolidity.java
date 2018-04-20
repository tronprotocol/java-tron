package org.tron.core;

import com.google.protobuf.ByteString;
import java.util.List;
import org.springframework.context.ApplicationContext;
import org.tron.api.GrpcAPI.AccountList;
import org.tron.api.GrpcAPI.AssetIssueList;
import org.tron.api.GrpcAPI.WitnessList;
import org.tron.common.application.Application;
import org.tron.core.db.Manager;
import org.tron.core.db.api.StoreAPI;
import org.tron.core.exception.NonUniqueObjectException;
import org.tron.protos.Contract.AssetIssueContract;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Witness;

public class WalletSolidity {

  private Application app;
  private Manager dbManager;
  private StoreAPI storeAPI;

  /**
   * constructor.
   */
  public WalletSolidity(ApplicationContext ctx) {
    this.dbManager = ctx.getBean(Manager.class);
    this.storeAPI = ctx.getBean(StoreAPI.class);
  }

  public Account getAccount(ByteString addressBs) {
    Account accountByAddress = null;
    try {
      accountByAddress = storeAPI.getAccountByAddress(addressBs.toStringUtf8());
    } catch (NonUniqueObjectException e) {
      e.printStackTrace();
    }
    return accountByAddress;
  }

  public AccountList getAccountList() {
    List<Account> accountAll = storeAPI.getAccountAll();
    AccountList accountList = AccountList.newBuilder().addAllAccounts(accountAll).build();
    return accountList;
  }

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

  public AssetIssueList getAssetIssueListByTimestamp(long timestamp) {
    List<AssetIssueContract> assetIssueAll = storeAPI.getAssetIssueByTime(timestamp);
    AssetIssueList assetIssueList =
        AssetIssueList.newBuilder().addAllAssetIssue(assetIssueAll).build();
    return assetIssueList;
  }

  public AssetIssueList getAssetIssueByAccount(ByteString address) {
    List<AssetIssueContract> assetIssueByOwnerAddress = storeAPI
        .getAssetIssueByOwnerAddress(address.toStringUtf8());
    AssetIssueList assetIssueList =
        AssetIssueList.newBuilder().addAllAssetIssue(assetIssueByOwnerAddress).build();
    return assetIssueList;
  }
}
