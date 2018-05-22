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

import com.google.protobuf.ByteString;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.AssetIssueList;
import org.tron.api.GrpcAPI.BlockList;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.api.GrpcAPI.Return.response_code;
import org.tron.api.GrpcAPI.WitnessList;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.Hash;
import org.tron.common.overlay.message.Message;
import org.tron.common.utils.Base58;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.db.AccountStore;
import org.tron.core.db.Manager;
import org.tron.core.db.PendingManager;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.DupTransactionException;
import org.tron.core.exception.StoreException;
import org.tron.core.exception.TaposException;
import org.tron.core.exception.TooBigTransactionException;
import org.tron.core.exception.TransactionExpirationException;
import org.tron.core.exception.ValidateBandwidthException;
import org.tron.core.exception.ValidateSignatureException;
import org.tron.core.net.message.TransactionMessage;
import org.tron.core.net.node.NodeImpl;
import org.tron.protos.Contract.AssetIssueContract;
import org.tron.protos.Contract.TransferContract;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;


@Slf4j
@Component
public class Wallet {

  @Getter
  private final ECKey ecKey;
  @Autowired
  private NodeImpl p2pNode;
  @Autowired
  private Manager dbManager;
  private static String addressPreFixString = Constant.ADD_PRE_FIX_STRING_TESTNET;  //default testnet
  private static byte addressPreFixByte = Constant.ADD_PRE_FIX_BYTE_TESTNET;

  /**
   * Creates a new Wallet with a random ECKey.
   */
  public Wallet() {
    this.ecKey = new ECKey(Utils.getRandom());
  }

  /**
   * Creates a Wallet with an existing ECKey.
   */
  public Wallet(final ECKey ecKey) {
    this.ecKey = ecKey;
    logger.info("wallet address: {}", ByteArray.toHexString(this.ecKey.getAddress()));
  }

  public byte[] getAddress() {
    return ecKey.getAddress();
  }

  public static String getAddressPreFixString() {
    return addressPreFixString;
  }

  public static void setAddressPreFixString(String addressPreFixString) {
    Wallet.addressPreFixString = addressPreFixString;
  }

  public static byte getAddressPreFixByte() {
    return addressPreFixByte;
  }

  public static void setAddressPreFixByte(byte addressPreFixByte) {
    Wallet.addressPreFixByte = addressPreFixByte;
  }

  public static boolean addressValid(byte[] address) {
    if (ArrayUtils.isEmpty(address)) {
      logger.warn("Warning: Address is empty !!");
      return false;
    }
    if (address.length != Constant.ADDRESS_SIZE / 2) {
      logger.warn(
          "Warning: Address length need " + Constant.ADDRESS_SIZE + " but " + address.length
              + " !!");
      return false;
    }
    if (address[0] != addressPreFixByte) {
      logger.warn("Warning: Address need prefix with " + addressPreFixByte + " but "
          + address[0] + " !!");
      return false;
    }
    //Other rule;
    return true;
  }

  public static String encode58Check(byte[] input) {
    byte[] hash0 = Hash.sha256(input);
    byte[] hash1 = Hash.sha256(hash0);
    byte[] inputCheck = new byte[input.length + 4];
    System.arraycopy(input, 0, inputCheck, 0, input.length);
    System.arraycopy(hash1, 0, inputCheck, input.length, 4);
    return Base58.encode(inputCheck);
  }

  private static byte[] decode58Check(String input) {
    byte[] decodeCheck = Base58.decode(input);
    if (decodeCheck.length <= 4) {
      return null;
    }
    byte[] decodeData = new byte[decodeCheck.length - 4];
    System.arraycopy(decodeCheck, 0, decodeData, 0, decodeData.length);
    byte[] hash0 = Hash.sha256(decodeData);
    byte[] hash1 = Hash.sha256(hash0);
    if (hash1[0] == decodeCheck[decodeData.length] &&
        hash1[1] == decodeCheck[decodeData.length + 1] &&
        hash1[2] == decodeCheck[decodeData.length + 2] &&
        hash1[3] == decodeCheck[decodeData.length + 3]) {
      return decodeData;
    }
    return null;
  }

  public static byte[] decodeFromBase58Check(String addressBase58) {
    if (StringUtils.isEmpty(addressBase58)) {
      logger.warn("Warning: Address is empty !!");
      return null;
    }
    if (addressBase58.length() != Constant.BASE58CHECK_ADDRESS_SIZE) {
      logger.warn(
          "Warning: Base58 address length need " + Constant.BASE58CHECK_ADDRESS_SIZE + " but "
              + addressBase58.length()
              + " !!");
      return null;
    }
    byte[] address = decode58Check(addressBase58);
    if (!addressValid(address)) {
      return null;
    }
    return address;
  }


  public Account getBalance(Account account) {
    AccountStore accountStore = dbManager.getAccountStore();
    AccountCapsule accountCapsule = accountStore.get(account.getAddress().toByteArray());
    return accountCapsule == null ? null : accountCapsule.getInstance();
  }

  /**
   * Create a transaction.
   */
  /*public Transaction createTransaction(byte[] address, String to, long amount) {
    long balance = getBalance(address);
    return new TransactionCapsule(address, to, amount, balance, utxoStore).getInstance();
  } */

  /**
   * Create a transaction by contract.
   */
  @Deprecated
  public Transaction createTransaction(TransferContract contract) {
    AccountStore accountStore = dbManager.getAccountStore();
    return new TransactionCapsule(contract, accountStore).getInstance();
  }

  /**
   * Broadcast a transaction.
   */
  public GrpcAPI.Return broadcastTransaction(Transaction signaturedTransaction) {
    GrpcAPI.Return.Builder builder = GrpcAPI.Return.newBuilder();
    TransactionCapsule trx = new TransactionCapsule(signaturedTransaction);
    try {
      Message message = new TransactionMessage(signaturedTransaction);
      if (dbManager.isTooManyPending()) {
        logger.debug(
            "Manager is busy, pending transaction count:{}, discard the new coming transaction",
            (dbManager.getPendingTransactions().size() + PendingManager.getTmpTransactions()
                .size()));
        return builder.setResult(false).setCode(response_code.SERVER_BUSY).build();
      } else if (dbManager.isGeneratingBlock()) {
        logger.debug("Manager is generating block, discard the new coming transaction");
        return builder.setResult(false).setCode(response_code.SERVER_BUSY).build();
      } else {
        dbManager.pushTransactions(trx);
        p2pNode.broadcast(message);
        return builder.setResult(true).setCode(response_code.SUCCESS).build();
      }
    } catch (ValidateSignatureException e) {
      logger.error(e.getMessage(), e);
      return builder.setResult(false).setCode(response_code.SIGERROR)
          .setMessage(ByteString.copyFromUtf8("validate signature error"))
          .build();
    } catch (ContractValidateException e) {
      logger.warn(e.getMessage(), e);
      return builder.setResult(false).setCode(response_code.CONTRACT_VALIDATE_ERROR)
          .setMessage(ByteString.copyFromUtf8("contract validate error"))
          .build();
    } catch (ContractExeException e) {
      logger.error(e.getMessage(), e);
      return builder.setResult(false).setCode(response_code.CONTRACT_EXE_ERROR)
          .setMessage(ByteString.copyFromUtf8("contract execute error"))
          .build();
    } catch (ValidateBandwidthException e) {
      logger.warn("high freq", e);
      return builder.setResult(false).setCode(response_code.BANDWITH_ERROR)
          .setMessage(ByteString.copyFromUtf8("high freq error"))
          .build();
    } catch (DupTransactionException e) {
      logger.error("dup trans", e);
      return builder.setResult(false).setCode(response_code.DUP_TRANSACTION_ERROR)
          .setMessage(ByteString.copyFromUtf8("dup transaction"))
          .build();
    } catch (TaposException e) {
      logger.debug("tapos error", e);
      return builder.setResult(false).setCode(response_code.TAPOS_ERROR)
          .setMessage(ByteString.copyFromUtf8("Tapos check error"))
          .build();
    } catch (TooBigTransactionException e) {
      logger.debug("transaction error", e);
      return builder.setResult(false).setCode(response_code.TOO_BIG_TRANSACTION_ERROR)
          .setMessage(ByteString.copyFromUtf8("transaction size is too big"))
          .build();
    } catch (TransactionExpirationException e) {
      logger.debug("transaction expired", e);
      return builder.setResult(false).setCode(response_code.TRANSACTION_EXPIRATION_ERROR)
          .setMessage(ByteString.copyFromUtf8("transaction expired"))
          .build();
    } catch (Exception e) {
      logger.error("exception caught", e);
      return builder.setResult(false).setCode(response_code.OTHER_ERROR)
          .setMessage(ByteString.copyFromUtf8("other error"))
          .build();
    }
  }

  public Block getNowBlock() {
    List<BlockCapsule> blockList = dbManager.getBlockStore().getBlockByLatestNum(1);
    if(CollectionUtils.isEmpty(blockList)){
      return null;
    }else{
      return blockList.get(0).getInstance();
    }
  }

  public Block getBlockByNum(long blockNum) {
    try {
      return dbManager.getBlockByNum(blockNum).getInstance();
    } catch (StoreException e) {
      logger.info(e.getMessage());
      return null;
    }
  }

  public WitnessList getWitnessList() {
    WitnessList.Builder builder = WitnessList.newBuilder();
    List<WitnessCapsule> witnessCapsuleList = dbManager.getWitnessStore().getAllWitnesses();
    witnessCapsuleList
        .forEach(witnessCapsule -> builder.addWitnesses(witnessCapsule.getInstance()));
    return builder.build();
  }

  public AssetIssueList getAssetIssueList() {
    AssetIssueList.Builder builder = AssetIssueList.newBuilder();
    dbManager.getAssetIssueStore().getAllAssetIssues()
        .forEach(issueCapsule -> builder.addAssetIssue(issueCapsule.getInstance()));
    return builder.build();
  }

  public AssetIssueList getAssetIssueByAccount(ByteString accountAddress) {
    if (accountAddress == null || accountAddress.size() == 0) {
      return null;
    }
    List<AssetIssueCapsule> assetIssueCapsuleList = dbManager.getAssetIssueStore()
        .getAllAssetIssues();
    AssetIssueList.Builder builder = AssetIssueList.newBuilder();
    assetIssueCapsuleList.stream()
        .filter(assetIssueCapsule -> assetIssueCapsule.getOwnerAddress().equals(accountAddress))
        .forEach(issueCapsule -> {
          builder.addAssetIssue(issueCapsule.getInstance());
        });
    return builder.build();
  }

  public AssetIssueContract getAssetIssueByName(ByteString assetName) {
    if (assetName == null || assetName.size() == 0) {
      return null;
    }
    List<AssetIssueCapsule> assetIssueCapsuleList = dbManager.getAssetIssueStore()
        .getAllAssetIssues();
    for (AssetIssueCapsule assetIssueCapsule : assetIssueCapsuleList) {
      if (assetName.equals(assetIssueCapsule.getName())) {
        return assetIssueCapsule.getInstance();
      }
    }
    return null;
  }

  public NumberMessage totalTransaction() {
    NumberMessage.Builder builder = NumberMessage.newBuilder()
        .setNum(dbManager.getTransactionStore().getTotalTransactions());
    return builder.build();
  }

  public NumberMessage getNextMaintenanceTime() {
    NumberMessage.Builder builder = NumberMessage.newBuilder()
        .setNum(dbManager.getDynamicPropertiesStore().getNextMaintenanceTime());
    return builder.build();
  }

  public Block getBlockById(ByteString BlockId) {
    if (Objects.isNull(BlockId)) {
      return null;
    }
    Block block = null;
    try {
      block = dbManager.getBlockStore().get(BlockId.toByteArray()).getInstance();
    } catch (StoreException e) {
    }
    return block;
  }

  public BlockList getBlocksByLimitNext(long number, long limit) {
    if (limit <= 0) {
      return null;
    }
    BlockList.Builder blockListBuilder = BlockList.newBuilder();
    dbManager.getBlockStore().getLimitNumber(number, limit).forEach(
        blockCapsule -> blockListBuilder.addBlock(blockCapsule.getInstance()));
    return blockListBuilder.build();
  }

  public BlockList getBlockByLatestNum(long getNum) {
    BlockList.Builder blockListBuilder = BlockList.newBuilder();
    dbManager.getBlockStore().getBlockByLatestNum(getNum).forEach(
        blockCapsule -> blockListBuilder.addBlock(blockCapsule.getInstance()));
    return blockListBuilder.build();
  }

  public Transaction getTransactionById(ByteString transactionId) {
    if (Objects.isNull(transactionId)) {
      return null;
    }
    Transaction transaction = null;
    TransactionCapsule transactionCapsule = dbManager.getTransactionStore()
        .get(transactionId.toByteArray());
    if (Objects.nonNull(transactionCapsule)) {
      transaction = transactionCapsule.getInstance();
    }
    return transaction;
  }
}