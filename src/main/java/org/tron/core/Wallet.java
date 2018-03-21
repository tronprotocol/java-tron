/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.core;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.tron.api.GrpcAPI.AccountList;
import org.tron.api.GrpcAPI.AssetIssueList;
import org.tron.api.GrpcAPI.WitnessList;
import org.tron.common.application.Application;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.common.utils.Utils;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.db.AccountStore;
import org.tron.core.db.BlockStore;
import org.tron.core.db.Manager;
import org.tron.core.db.UtxoStore;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.ValidateSignatureException;
import org.tron.core.net.message.Message;
import org.tron.core.net.message.TransactionMessage;
import org.tron.core.net.node.Node;
import org.tron.protos.Contract.AccountCreateContract;
import org.tron.protos.Contract.AssetIssueContract;
import org.tron.protos.Contract.ParticipateAssetIssueContract;
import org.tron.protos.Contract.TransferAssetContract;
import org.tron.protos.Contract.TransferContract;
import org.tron.protos.Contract.VoteWitnessContract;
import org.tron.protos.Contract.WitnessCreateContract;
import org.tron.protos.Contract.WitnessUpdateContract;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.TXOutput;
import org.tron.protos.Protocol.Transaction;

@Slf4j
public class Wallet {
  private BlockStore db;
  private final ECKey ecKey;
  @Getter
  private UtxoStore utxoStore;
  private Application app;
  private Node p2pnode;
  private Manager dbManager;

  /**
   * Creates a new Wallet with a random ECKey.
   */
  public Wallet() {
    this.ecKey = new ECKey(Utils.getRandom());
  }


  /**
   * constructor.
   */
  public Wallet(Application app) {
    this.app = app;
    this.p2pnode = app.getP2pNode();
    this.db = app.getBlockStoreS();
    utxoStore = app.getDbManager().getUtxoStore();
    dbManager = app.getDbManager();
    this.ecKey = new ECKey(Utils.getRandom());
  }

  /**
   * Creates a Wallet with an existing ECKey.
   */
  public Wallet(final ECKey ecKey) {
    this.ecKey = ecKey;
    logger.info("wallet address: {}", ByteArray.toHexString(this.ecKey.getAddress()));
  }

  public ECKey getEcKey() {
    return ecKey;
  }

  public byte[] getAddress() {
    return ecKey.getAddress();
  }

  /**
   * Get balance by address.
   */
  public long getBalance(byte[] address) {

    ArrayList<TXOutput> utxos = utxoStore.findUtxo(address);
    long balance = 0;

    for (TXOutput txOutput : utxos) {
      balance += txOutput.getValue();
    }

    logger.info("balance = {}", balance);
    return balance;
  }

  public Account getBalance(Account account) {
    AccountStore accountStore = dbManager.getAccountStore();
    AccountCapsule accountCapsule = accountStore.get(account.getAddress().toByteArray());
    if (accountCapsule == null) {
      return null;
    }
    return accountCapsule.getInstance();
  }


  /**
   * Create a transaction.
   */
  public Transaction createTransaction(byte[] address, String to, long amount) {
    long balance = getBalance(address);
    TransactionCapsule transactionCapsule = new TransactionCapsule(address, to, amount, balance,
        utxoStore);
    return transactionCapsule.getInstance();
  }


  /**
   * Create a transaction by contract.
   */
  public Transaction createTransaction(TransferContract contract) {
    AccountStore accountStore = dbManager.getAccountStore();
    TransactionCapsule transactionCapsule = new TransactionCapsule(contract, accountStore);
    return transactionCapsule.getInstance();
  }

  /**
   * Broadcast a transaction.
   */
  public boolean broadcastTransaction(Transaction signaturedTransaction) {

    TransactionCapsule trx = new TransactionCapsule(signaturedTransaction);
    try {
      if (trx.validateSignature()) {
        Message message = new TransactionMessage(signaturedTransaction);
        dbManager.pushTransactions(trx);
        p2pnode.broadcast(message);
        return true;
      }
    } catch (ValidateSignatureException e) {
      e.printStackTrace();
    } catch (ContractValidateException e) {
      e.printStackTrace();
    } catch (ContractExeException e) {
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }

  public Transaction createAccount(AccountCreateContract contract) {
    AccountStore accountStore = dbManager.getAccountStore();
    TransactionCapsule transactionCapsule = new TransactionCapsule(contract, accountStore);
    return transactionCapsule.getInstance();
  }

  public Transaction createTransaction(VoteWitnessContract voteWitnessContract) {
    TransactionCapsule transactionCapsule = new TransactionCapsule(voteWitnessContract);
    return transactionCapsule.getInstance();
  }

  public Transaction createTransaction(AssetIssueContract assetIssueContract) {
    TransactionCapsule transactionCapsule = new TransactionCapsule(assetIssueContract);
    return transactionCapsule.getInstance();
  }

  public Transaction createTransaction(WitnessCreateContract witnessCreateContract) {
    TransactionCapsule transactionCapsule = new TransactionCapsule(witnessCreateContract);
    return transactionCapsule.getInstance();
  }

  public Transaction createTransaction(WitnessUpdateContract witnessUpdateContract) {
    TransactionCapsule transactionCapsule = new TransactionCapsule(witnessUpdateContract);
    return transactionCapsule.getInstance();
  }

  public Block getNowBlock() {
    Sha256Hash headBlockId = dbManager.getHeadBlockId();
    Block block = dbManager.getBlockById(headBlockId).getInstance();
    return block;
  }

  public Block getBlockByNum(long blockNum) {
    Sha256Hash headBlockId = dbManager.getBlockIdByNum(blockNum);
    Block block = dbManager.getBlockById(headBlockId).getInstance();
    return block;
  }


  public AccountList getAllAccounts() {
    List<AccountCapsule> allAccounts = dbManager.getAccountStore().getAllAccounts();
    AccountList.Builder builder = AccountList.newBuilder();
    allAccounts.forEach(accountCapsule -> {
      builder.addAccounts(accountCapsule.getInstance());
    });
    return builder.build();
  }

  public WitnessList getWitnessList() {
    List<WitnessCapsule> witnessCapsules = dbManager.getWitnessStore().getAllWitnesses();
    WitnessList.Builder builder = WitnessList.newBuilder();
    witnessCapsules.forEach(witnessCapsule -> {
      builder.addWitnesses(witnessCapsule.getInstance());
    });
    return builder.build();
  }

  public Transaction createTransaction(TransferAssetContract transferAssetContract) {
    TransactionCapsule transactionCapsule = new TransactionCapsule(transferAssetContract);
    return transactionCapsule.getInstance();
  }

  public Transaction createTransaction(
      ParticipateAssetIssueContract participateAssetIssueContract) {
    TransactionCapsule transactionCapsule = new TransactionCapsule(participateAssetIssueContract);
    return transactionCapsule.getInstance();
  }

  public AssetIssueList getAssetIssueList() {
    List<AssetIssueCapsule> assetIssueCapsuleList = dbManager.getAssetIssueStore()
        .getAllAssetIssues();
    AssetIssueList.Builder builder = AssetIssueList.newBuilder();
    assetIssueCapsuleList.forEach(witnessCapsule -> {
      builder.addAssetIssue(witnessCapsule.getInstance());
    });
    return builder.build();
  }
}
