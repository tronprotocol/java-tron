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

import com.google.common.primitives.Longs;
import com.google.protobuf.ByteString;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.AccountNetMessage;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.GrpcAPI.Address;
import org.tron.api.GrpcAPI.AssetIssueList;
import org.tron.api.GrpcAPI.BlockList;
import org.tron.api.GrpcAPI.ExchangeList;
import org.tron.api.GrpcAPI.Node;
import org.tron.api.GrpcAPI.NodeList;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.api.GrpcAPI.ProposalList;
import org.tron.api.GrpcAPI.Return;
import org.tron.api.GrpcAPI.Return.response_code;
import org.tron.api.GrpcAPI.TransactionExtention.Builder;
import org.tron.api.GrpcAPI.WitnessList;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.Hash;
import org.tron.common.overlay.discover.node.NodeHandler;
import org.tron.common.overlay.discover.node.NodeManager;
import org.tron.common.overlay.message.Message;
import org.tron.common.runtime.Runtime;
import org.tron.common.runtime.vm.program.ProgramResult;
import org.tron.common.runtime.vm.program.invoke.ProgramInvokeFactoryImpl;
import org.tron.common.storage.DepositImpl;
import org.tron.common.utils.Base58;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.common.utils.Utils;
import org.tron.core.actuator.Actuator;
import org.tron.core.actuator.ActuatorFactory;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.ContractCapsule;
import org.tron.core.capsule.ExchangeCapsule;
import org.tron.core.capsule.ProposalCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.config.Parameter.ChainConstant;
import org.tron.core.config.Parameter.ChainParameters;
import org.tron.core.db.AccountIdIndexStore;
import org.tron.core.db.AccountStore;
import org.tron.core.db.BandwidthProcessor;
import org.tron.core.db.ContractStore;
import org.tron.core.db.DynamicPropertiesStore;
import org.tron.core.db.EnergyProcessor;
import org.tron.core.db.Manager;
import org.tron.core.db.PendingManager;
import org.tron.core.exception.AccountResourceInsufficientException;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.DupTransactionException;
import org.tron.core.exception.HeaderNotFound;
import org.tron.core.exception.StoreException;
import org.tron.core.exception.TaposException;
import org.tron.core.exception.TooBigTransactionException;
import org.tron.core.exception.TransactionExpirationException;
import org.tron.core.exception.ValidateSignatureException;
import org.tron.core.net.message.TransactionMessage;
import org.tron.core.net.node.NodeImpl;
import org.tron.protos.Contract.AssetIssueContract;
import org.tron.protos.Contract.CreateSmartContract;
import org.tron.protos.Contract.TransferContract;
import org.tron.protos.Contract.TriggerSmartContract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Exchange;
import org.tron.protos.Protocol.Proposal;
import org.tron.protos.Protocol.SmartContract;
import org.tron.protos.Protocol.SmartContract.ABI;
import org.tron.protos.Protocol.SmartContract.ABI.Entry.StateMutabilityType;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Transaction.Result.code;
import org.tron.protos.Protocol.TransactionSign;

@Slf4j
@Component
public class Wallet {

  @Getter
  private final ECKey ecKey;
  @Autowired
  private NodeImpl p2pNode;
  @Autowired
  private Manager dbManager;
  @Autowired
  private NodeManager nodeManager;
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

  public static boolean isConstant(ABI abi, TriggerSmartContract triggerSmartContract) {
    try {
      return isConstant(abi, getSelector(triggerSmartContract.getData().toByteArray()));
    } catch (Exception e) {
      return false;
    }
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
    byte[] hash0 = Sha256Hash.hash(input);
    byte[] hash1 = Sha256Hash.hash(hash0);
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
    byte[] hash0 = Sha256Hash.hash(decodeData);
    byte[] hash1 = Sha256Hash.hash(hash0);
    if (hash1[0] == decodeCheck[decodeData.length] &&
        hash1[1] == decodeCheck[decodeData.length + 1] &&
        hash1[2] == decodeCheck[decodeData.length + 2] &&
        hash1[3] == decodeCheck[decodeData.length + 3]) {
      return decodeData;
    }
    return null;
  }

  public static byte[] generateContractAddress(Transaction trx) {

    CreateSmartContract contract = ContractCapsule.getSmartContractFromTransaction(trx);
    byte[] ownerAddress = contract.getOwnerAddress().toByteArray();
    TransactionCapsule trxCap = new TransactionCapsule(trx);
    byte[] txRawDataHash = trxCap.getTransactionId().getBytes();

    byte[] combined = new byte[txRawDataHash.length + ownerAddress.length];
    System.arraycopy(txRawDataHash, 0, combined, 0, txRawDataHash.length);
    System.arraycopy(ownerAddress, 0, combined, txRawDataHash.length, ownerAddress.length);

    return Hash.sha3omit12(combined);

  }

  public static byte[] generateContractAddress(byte[] ownerAddress, byte[] txRawDataHash) {

    byte[] combined = new byte[txRawDataHash.length + ownerAddress.length];
    System.arraycopy(txRawDataHash, 0, combined, 0, txRawDataHash.length);
    System.arraycopy(ownerAddress, 0, combined, txRawDataHash.length, ownerAddress.length);

    return Hash.sha3omit12(combined);

  }

  public static byte[] generateContractAddress(byte[] transactionRootId, long nonce) {
    byte[] nonceBytes = Longs.toByteArray(nonce);
    byte[] combined = new byte[transactionRootId.length + nonceBytes.length];
    System.arraycopy(transactionRootId, 0, combined, 0, transactionRootId.length);
    System.arraycopy(nonceBytes, 0, combined, transactionRootId.length, nonceBytes.length);

    return Hash.sha3omit12(combined);
  }

  public static byte[] decodeFromBase58Check(String addressBase58) {
    if (StringUtils.isEmpty(addressBase58)) {
      logger.warn("Warning: Address is empty !!");
      return null;
    }
    byte[] address = decode58Check(addressBase58);
    if (address == null) {
      return null;
    }

    if (!addressValid(address)) {
      return null;
    }

    return address;
  }


  public Account getAccount(Account account) {
    AccountStore accountStore = dbManager.getAccountStore();
    AccountCapsule accountCapsule = accountStore.get(account.getAddress().toByteArray());
    if (accountCapsule == null) {
      return null;
    }
    BandwidthProcessor processor = new BandwidthProcessor(dbManager);
    processor.updateUsage(accountCapsule);

    EnergyProcessor energyProcessor = new EnergyProcessor(dbManager);
    energyProcessor.updateUsage(accountCapsule);

    long genesisTimeStamp = dbManager.getGenesisBlock().getTimeStamp();
    accountCapsule.setLatestConsumeTime(genesisTimeStamp
        + ChainConstant.BLOCK_PRODUCED_INTERVAL * accountCapsule.getLatestConsumeTime());
    accountCapsule.setLatestConsumeFreeTime(genesisTimeStamp
        + ChainConstant.BLOCK_PRODUCED_INTERVAL * accountCapsule.getLatestConsumeFreeTime());
    accountCapsule.setLatestConsumeTimeForEnergy(genesisTimeStamp
        + ChainConstant.BLOCK_PRODUCED_INTERVAL * accountCapsule.getLatestConsumeTimeForEnergy());

    return accountCapsule.getInstance();
  }


  public Account getAccountById(Account account) {
    AccountStore accountStore = dbManager.getAccountStore();
    AccountIdIndexStore accountIdIndexStore = dbManager.getAccountIdIndexStore();
    byte[] address = accountIdIndexStore.get(account.getAccountId());
    if (address == null) {
      return null;
    }
    AccountCapsule accountCapsule = accountStore.get(address);
    if (accountCapsule == null) {
      return null;
    }
    BandwidthProcessor processor = new BandwidthProcessor(dbManager);
    processor.updateUsage(accountCapsule);

    EnergyProcessor energyProcessor = new EnergyProcessor(dbManager);
    energyProcessor.updateUsage(accountCapsule);

    return accountCapsule.getInstance();
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


  public TransactionCapsule createTransactionCapsule(com.google.protobuf.Message message,
      ContractType contractType) throws ContractValidateException {
    TransactionCapsule trx = new TransactionCapsule(message, contractType);
    if (contractType != ContractType.CreateSmartContract
        && contractType != ContractType.TriggerSmartContract) {
      List<Actuator> actList = ActuatorFactory.createActuator(trx, dbManager);
      for (Actuator act : actList) {
        act.validate();
      }
    }

    if (contractType == ContractType.CreateSmartContract) {

      CreateSmartContract contract = ContractCapsule
          .getSmartContractFromTransaction(trx.getInstance());
      long percent = contract.getNewContract().getConsumeUserResourcePercent();
      if (percent < 0 || percent > 100) {
        throw new ContractValidateException("percent must be >= 0 and <= 100");
      }
    }

    try {
      BlockCapsule headBlock = null;
      List<BlockCapsule> blockList = dbManager.getBlockStore().getBlockByLatestNum(1);
      if (CollectionUtils.isEmpty(blockList)) {
        throw new HeaderNotFound("latest block not found");
      } else {
        headBlock = blockList.get(0);
      }
      trx.setReference(headBlock.getNum(), headBlock.getBlockId().getBytes());
      long expiration = headBlock.getTimeStamp() + Constant.TRANSACTION_DEFAULT_EXPIRATION_TIME;
      trx.setExpiration(expiration);
      trx.setTimestamp();
    } catch (HeaderNotFound headerNotFound) {
      headerNotFound.printStackTrace();
    }
    return trx;
  }

  /**
   * Broadcast a transaction.
   */
  public GrpcAPI.Return broadcastTransaction(Transaction signaturedTransaction) {
    GrpcAPI.Return.Builder builder = GrpcAPI.Return.newBuilder();

    try {
      TransactionCapsule trx = new TransactionCapsule(signaturedTransaction);
      Message message = new TransactionMessage(signaturedTransaction);

      if (dbManager.isTooManyPending()) {
        logger.debug(
            "Manager is busy, pending transaction count:{}, discard the new coming transaction",
            (dbManager.getPendingTransactions().size() + PendingManager.getTmpTransactions()
                .size()));
        return builder.setResult(false).setCode(response_code.SERVER_BUSY).build();
      }

      if (dbManager.isGeneratingBlock()) {
        logger.debug("Manager is generating block, discard the new coming transaction");
        return builder.setResult(false).setCode(response_code.SERVER_BUSY).build();
      }

      if (dbManager.getTransactionIdCache().getIfPresent(trx.getTransactionId()) != null) {
        logger.debug("This transaction has been processed, discard the transaction");
        return builder.setResult(false).setCode(response_code.DUP_TRANSACTION_ERROR).build();
      } else {
        dbManager.getTransactionIdCache().put(trx.getTransactionId(), true);
      }

      if (dbManager.getForkController().forkOrNot(trx)) {
        dbManager.pushTransaction(trx);
        p2pNode.broadcast(message);
      }

      return builder.setResult(true).setCode(response_code.SUCCESS).build();
    } catch (ValidateSignatureException e) {
      logger.info(e.getMessage());
      return builder.setResult(false).setCode(response_code.SIGERROR)
          .setMessage(ByteString.copyFromUtf8("validate signature error"))
          .build();
    } catch (ContractValidateException e) {
      logger.info(e.getMessage());
      return builder.setResult(false).setCode(response_code.CONTRACT_VALIDATE_ERROR)
          .setMessage(ByteString.copyFromUtf8("contract validate error : " + e.getMessage()))
          .build();
    } catch (ContractExeException e) {
      logger.info(e.getMessage());
      return builder.setResult(false).setCode(response_code.CONTRACT_EXE_ERROR)
          .setMessage(ByteString.copyFromUtf8("contract execute error : " + e.getMessage()))
          .build();
    } catch (AccountResourceInsufficientException e) {
      logger.info(e.getMessage());
      return builder.setResult(false).setCode(response_code.BANDWITH_ERROR)
          .setMessage(ByteString.copyFromUtf8("AccountResourceInsufficient error"))
          .build();
    } catch (DupTransactionException e) {
      logger.info("dup trans" + e.getMessage());
      return builder.setResult(false).setCode(response_code.DUP_TRANSACTION_ERROR)
          .setMessage(ByteString.copyFromUtf8("dup transaction"))
          .build();
    } catch (TaposException e) {
      logger.info("tapos error" + e.getMessage());
      return builder.setResult(false).setCode(response_code.TAPOS_ERROR)
          .setMessage(ByteString.copyFromUtf8("Tapos check error"))
          .build();
    } catch (TooBigTransactionException e) {
      logger.info("transaction error" + e.getMessage());
      return builder.setResult(false).setCode(response_code.TOO_BIG_TRANSACTION_ERROR)
          .setMessage(ByteString.copyFromUtf8("transaction size is too big"))
          .build();
    } catch (TransactionExpirationException e) {
      logger.info("transaction expired" + e.getMessage());
      return builder.setResult(false).setCode(response_code.TRANSACTION_EXPIRATION_ERROR)
          .setMessage(ByteString.copyFromUtf8("transaction expired"))
          .build();
    } catch (Exception e) {
      logger.info("exception caught" + e.getMessage());
      return builder.setResult(false).setCode(response_code.OTHER_ERROR)
          .setMessage(ByteString.copyFromUtf8("other error : " + e.getMessage()))
          .build();
    }
  }

  public TransactionCapsule getTransactionSign(TransactionSign transactionSign) {
    byte[] privateKey = transactionSign.getPrivateKey().toByteArray();
    TransactionCapsule trx = new TransactionCapsule(transactionSign.getTransaction());
    trx.sign(privateKey);
    return trx;
  }

  public byte[] pass2Key(byte[] passPhrase) {
    return Sha256Hash.hash(passPhrase);
  }

  public byte[] createAdresss(byte[] passPhrase) {
    byte[] privateKey = pass2Key(passPhrase);
    ECKey ecKey = ECKey.fromPrivate(privateKey);
    return ecKey.getAddress();
  }

  public Block getNowBlock() {
    List<BlockCapsule> blockList = dbManager.getBlockStore().getBlockByLatestNum(1);
    if (CollectionUtils.isEmpty(blockList)) {
      return null;
    } else {
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

  public ProposalList getProposalList() {
    ProposalList.Builder builder = ProposalList.newBuilder();
    List<ProposalCapsule> proposalCapsuleList = dbManager.getProposalStore().getAllProposals();
    proposalCapsuleList
        .forEach(proposalCapsule -> builder.addProposals(proposalCapsule.getInstance()));
    return builder.build();
  }

  public ExchangeList getExchangeList() {
    ExchangeList.Builder builder = ExchangeList.newBuilder();
    List<ExchangeCapsule> exchangeCapsuleList = dbManager.getExchangeStore().getAllExchanges();
    exchangeCapsuleList
        .forEach(exchangeCapsule -> builder.addExchanges(exchangeCapsule.getInstance()));
    return builder.build();
  }

  public Protocol.ChainParameters getChainParameters() {
    Protocol.ChainParameters.Builder builder = Protocol.ChainParameters.newBuilder();
    DynamicPropertiesStore dynamicPropertiesStore = dbManager.getDynamicPropertiesStore();

    Protocol.ChainParameters.ChainParameter.Builder builder1
        = Protocol.ChainParameters.ChainParameter.newBuilder();

    builder.addChainParameter(builder1
        .setKey(ChainParameters.MAINTENANCE_TIME_INTERVAL.name())
        .setValue(
            dynamicPropertiesStore.getMaintenanceTimeInterval())
        .build());
    builder.addChainParameter(builder1
        .setKey(ChainParameters.ACCOUNT_UPGRADE_COST.name())
        .setValue(
            dynamicPropertiesStore.getAccountUpgradeCost())
        .build());
    builder.addChainParameter(builder1
        .setKey(ChainParameters.CREATE_ACCOUNT_FEE.name())
        .setValue(
            dynamicPropertiesStore.getCreateAccountFee())
        .build());
    builder.addChainParameter(builder1
        .setKey(ChainParameters.TRANSACTION_FEE.name())
        .setValue(
            dynamicPropertiesStore.getTransactionFee())
        .build());
    builder.addChainParameter(builder1
        .setKey(ChainParameters.ASSET_ISSUE_FEE.name())
        .setValue(
            dynamicPropertiesStore.getAssetIssueFee())
        .build());
    builder.addChainParameter(builder1
        .setKey(ChainParameters.WITNESS_PAY_PER_BLOCK.name())
        .setValue(
            dynamicPropertiesStore.getWitnessPayPerBlock())
        .build());
    builder.addChainParameter(builder1
        .setKey(ChainParameters.WITNESS_STANDBY_ALLOWANCE.name())
        .setValue(
            dynamicPropertiesStore.getWitnessStandbyAllowance())
        .build());
    builder.addChainParameter(builder1
        .setKey(ChainParameters.CREATE_NEW_ACCOUNT_FEE_IN_SYSTEM_CONTRACT.name())
        .setValue(
            dynamicPropertiesStore.getCreateNewAccountFeeInSystemContract())
        .build());
    builder.addChainParameter(builder1
        .setKey(ChainParameters.CREATE_NEW_ACCOUNT_BANDWIDTH_RATE.name())
        .setValue(
            dynamicPropertiesStore.getCreateNewAccountBandwidthRate())
        .build());

    builder.addChainParameter(builder1
        .setKey(ChainParameters.ALLOW_CREATION_OF_CONTRACTS.name())
        .setValue(
            dynamicPropertiesStore.getAllowCreationOfContracts())
        .build());

    return builder.build();
  }

  public AssetIssueList getAssetIssueList() {
    AssetIssueList.Builder builder = AssetIssueList.newBuilder();
    dbManager.getAssetIssueStore().getAllAssetIssues()
        .forEach(issueCapsule -> builder.addAssetIssue(issueCapsule.getInstance()));
    return builder.build();
  }


  public AssetIssueList getAssetIssueList(long offset, long limit) {
    AssetIssueList.Builder builder = AssetIssueList.newBuilder();
    List<AssetIssueCapsule> assetIssueList = dbManager.getAssetIssueStore()
        .getAssetIssuesPaginated(offset, limit);

    if (CollectionUtils.isEmpty(assetIssueList)) {
      return null;
    }

    assetIssueList.forEach(issueCapsule -> builder.addAssetIssue(issueCapsule.getInstance()));
    return builder.build();
  }

  public AssetIssueList getAssetIssueByAccount(ByteString accountAddress) {
    if (accountAddress == null || accountAddress.isEmpty()) {
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

  public AccountNetMessage getAccountNet(ByteString accountAddress) {
    if (accountAddress == null || accountAddress.isEmpty()) {
      return null;
    }
    AccountNetMessage.Builder builder = AccountNetMessage.newBuilder();
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(accountAddress.toByteArray());
    if (accountCapsule == null) {
      return null;
    }

    BandwidthProcessor processor = new BandwidthProcessor(dbManager);
    processor.updateUsage(accountCapsule);

    long netLimit = processor.calculateGlobalNetLimit(accountCapsule.getFrozenBalance());
    long freeNetLimit = dbManager.getDynamicPropertiesStore().getFreeNetLimit();
    long totalNetLimit = dbManager.getDynamicPropertiesStore().getTotalNetLimit();
    long totalNetWeight = dbManager.getDynamicPropertiesStore().getTotalNetWeight();

    Map<String, Long> assetNetLimitMap = new HashMap<>();
    accountCapsule.getAllFreeAssetNetUsage().keySet().forEach(asset -> {
      byte[] key = ByteArray.fromString(asset);
      assetNetLimitMap.put(asset, dbManager.getAssetIssueStore().get(key).getFreeAssetNetLimit());
    });

    builder.setFreeNetUsed(accountCapsule.getFreeNetUsage())
        .setFreeNetLimit(freeNetLimit)
        .setNetUsed(accountCapsule.getNetUsage())
        .setNetLimit(netLimit)
        .setTotalNetLimit(totalNetLimit)
        .setTotalNetWeight(totalNetWeight)
        .putAllAssetNetUsed(accountCapsule.getAllFreeAssetNetUsage())
        .putAllAssetNetLimit(assetNetLimitMap);
    return builder.build();
  }

  public AccountResourceMessage getAccountResource(ByteString accountAddress) {
    if (accountAddress == null || accountAddress.isEmpty()) {
      return null;
    }
    AccountResourceMessage.Builder builder = AccountResourceMessage.newBuilder();
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(accountAddress.toByteArray());
    if (accountCapsule == null) {
      return null;
    }

    BandwidthProcessor processor = new BandwidthProcessor(dbManager);
    processor.updateUsage(accountCapsule);

    EnergyProcessor energyProcessor = new EnergyProcessor(dbManager);
    energyProcessor.updateUsage(accountCapsule);

    long netLimit = processor.calculateGlobalNetLimit(accountCapsule.getFrozenBalance());
    long freeNetLimit = dbManager.getDynamicPropertiesStore().getFreeNetLimit();
    long totalNetLimit = dbManager.getDynamicPropertiesStore().getTotalNetLimit();
    long totalNetWeight = dbManager.getDynamicPropertiesStore().getTotalNetWeight();
    long energyLimit = energyProcessor
        .calculateGlobalEnergyLimit(accountCapsule.getEnergyFrozenBalance());
    long totalEnergyLimit = dbManager.getDynamicPropertiesStore().getTotalEnergyLimit();
    long totalEnergyWeight = dbManager.getDynamicPropertiesStore().getTotalEnergyWeight();

    long storageLimit = accountCapsule.getAccountResource().getStorageLimit();
    long storageUsage = accountCapsule.getAccountResource().getStorageUsage();

    Map<String, Long> assetNetLimitMap = new HashMap<>();
    accountCapsule.getAllFreeAssetNetUsage().keySet().forEach(asset -> {
      byte[] key = ByteArray.fromString(asset);
      assetNetLimitMap.put(asset, dbManager.getAssetIssueStore().get(key).getFreeAssetNetLimit());
    });

    builder.setFreeNetUsed(accountCapsule.getFreeNetUsage())
        .setFreeNetLimit(freeNetLimit)
        .setNetUsed(accountCapsule.getNetUsage())
        .setNetLimit(netLimit)
        .setTotalNetLimit(totalNetLimit)
        .setTotalNetWeight(totalNetWeight)
        .setEnergyLimit(energyLimit)
        .setEnergyUsed(accountCapsule.getAccountResource().getEnergyUsage())
        .setTotalEnergyLimit(totalEnergyLimit)
        .setTotalEnergyWeight(totalEnergyWeight)
        .setStorageLimit(storageLimit)
        .setStorageUsed(storageUsage)
        .putAllAssetNetUsed(accountCapsule.getAllFreeAssetNetUsage())
        .putAllAssetNetLimit(assetNetLimitMap);
    return builder.build();
  }

  public AssetIssueContract getAssetIssueByName(ByteString assetName) {
    if (assetName == null || assetName.isEmpty()) {
      return null;
    }
    AssetIssueCapsule assetIssueCapsule = dbManager.getAssetIssueStore()
        .get(assetName.toByteArray());
    return assetIssueCapsule != null ? assetIssueCapsule.getInstance() : null;
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
    TransactionCapsule transactionCapsule = null;
    try {
      transactionCapsule = dbManager.getTransactionStore()
          .get(transactionId.toByteArray());
    } catch (StoreException e) {
    }
    if (transactionCapsule != null) {
      return transactionCapsule.getInstance();
    }
    return null;
  }

  public Proposal getProposalById(ByteString proposalId) {
    if (Objects.isNull(proposalId)) {
      return null;
    }
    ProposalCapsule proposalCapsule = null;
    try {
      proposalCapsule = dbManager.getProposalStore()
          .get(proposalId.toByteArray());
    } catch (StoreException e) {
    }
    if (proposalCapsule != null) {
      return proposalCapsule.getInstance();
    }
    return null;
  }

  public Exchange getExchangeById(ByteString exchangeId) {
    if (Objects.isNull(exchangeId)) {
      return null;
    }
    ExchangeCapsule exchangeCapsule = null;
    try {
      exchangeCapsule = dbManager.getExchangeStore()
          .get(exchangeId.toByteArray());
    } catch (StoreException e) {
    }
    if (exchangeCapsule != null) {
      return exchangeCapsule.getInstance();
    }
    return null;
  }


  public NodeList listNodes() {
    List<NodeHandler> handlerList = nodeManager.dumpActiveNodes();

    Map<String, NodeHandler> nodeHandlerMap = new HashMap<>();
    for (NodeHandler handler : handlerList) {
      String key = handler.getNode().getHexId() + handler.getNode().getHost();
      nodeHandlerMap.put(key, handler);
    }

    NodeList.Builder nodeListBuilder = NodeList.newBuilder();

    nodeHandlerMap.entrySet().stream()
        .forEach(v -> {
          org.tron.common.overlay.discover.node.Node node = v.getValue().getNode();
          nodeListBuilder.addNodes(Node.newBuilder().setAddress(
              Address.newBuilder()
                  .setHost(ByteString.copyFrom(ByteArray.fromString(node.getHost())))
                  .setPort(node.getPort())));
        });
    return nodeListBuilder.build();
  }

  public Transaction deployContract(CreateSmartContract createSmartContract,
      TransactionCapsule trxCap) {

    // do nothing, so can add some useful function later
    // trxcap contract para cacheUnpackValue has value
    return trxCap.getInstance();
  }

  public Transaction triggerContract(TriggerSmartContract triggerSmartContract,
      TransactionCapsule trxCap, Builder builder,
      Return.Builder retBuilder) {

    ContractStore contractStore = dbManager.getContractStore();
    byte[] contractAddress = triggerSmartContract.getContractAddress().toByteArray();
    SmartContract.ABI abi = contractStore.getABI(contractAddress);
    if (abi == null) {
      // FIXME
      return null;
    }

    try {
      byte[] selector = getSelector(triggerSmartContract.getData().toByteArray());
      if (selector == null) {
        // FIXME
        return null;
      }

      if (!isConstant(abi, selector)) {
        return trxCap.getInstance();
      } else {
        DepositImpl deposit = DepositImpl.createRoot(dbManager);

        Block headBlock;
        List<BlockCapsule> blockCapsuleList = dbManager.getBlockStore().getBlockByLatestNum(1);
        if (CollectionUtils.isEmpty(blockCapsuleList)) {
          throw new HeaderNotFound("latest block not found");
        } else {
          headBlock = blockCapsuleList.get(0).getInstance();
        }

        Runtime runtime = new Runtime(trxCap.getInstance(), headBlock, deposit,
            new ProgramInvokeFactoryImpl());
        runtime.init();
        runtime.execute();
        runtime.go();
        runtime.finalization();
        // TODO exception
        if (runtime.getResult().getException() != null) {
//          runtime.getResult().getException().printStackTrace();
          throw new RuntimeException("Runtime exe failed!");
        }

        ProgramResult result = runtime.getResult();
        TransactionResultCapsule ret = new TransactionResultCapsule();

        builder.addConstantResult(ByteString.copyFrom(result.getHReturn()));
        ret.setStatus(0, code.SUCESS);
        if (StringUtils.isNoneEmpty(runtime.getRuntimeError())) {
          ret.setStatus(0, code.FAILED);
          retBuilder.setMessage(ByteString.copyFromUtf8(runtime.getRuntimeError())).build();
        }
        trxCap.setResult(ret);
        return trxCap.getInstance();
      }
    } catch (Exception e) {
      logger.error(e.getMessage());
      return null;
    }
  }

  public SmartContract getContract(GrpcAPI.BytesMessage bytesMessage) {
    byte[] address = bytesMessage.getValue().toByteArray();
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(address);
    if (accountCapsule == null) {
      logger.error(
          "Get contract failed, the account is not exist or the account does not have code hash!");
      return null;
    }

    ContractCapsule contractCapsule = dbManager.getContractStore()
        .get(bytesMessage.getValue().toByteArray());
    if (Objects.nonNull(contractCapsule)) {
      return contractCapsule.getInstance();
    }
    return null;
  }

  private static byte[] getSelector(byte[] data) {
    if (data == null ||
        data.length < 4) {
      return null;
    }

    byte[] ret = new byte[4];
    System.arraycopy(data, 0, ret, 0, 4);
    return ret;
  }

  private static boolean isConstant(SmartContract.ABI abi, byte[] selector) throws Exception {

    if (abi.getEntrysList().size() == 0) {
      return false;
    }
    if (selector == null || selector.length != 4) {
      throw new Exception("Selector's length or selector itself is invalid");
    }

    for (int i = 0; i < abi.getEntrysCount(); i++) {
      ABI.Entry entry = abi.getEntrys(i);
      if (entry.getType() != ABI.Entry.EntryType.Function) {
        continue;
      }

      int inputCount = entry.getInputsCount();
      StringBuffer sb = new StringBuffer();
      sb.append(entry.getName());
      sb.append("(");
      for (int k = 0; k < inputCount; k++) {
        ABI.Entry.Param param = entry.getInputs(k);
        sb.append(param.getType());
        if (k + 1 < inputCount) {
          sb.append(",");
        }
      }
      sb.append(")");

      byte[] funcSelector = new byte[4];
      System.arraycopy(Hash.sha3(sb.toString().getBytes()), 0, funcSelector, 0, 4);
      if (Arrays.equals(funcSelector, selector)) {
        if (entry.getConstant() == true || entry.getStateMutability()
            .equals(StateMutabilityType.View)) {
          return true;
        } else {
          return false;
        }
      }
    }

    throw new Exception("There is no the selector!");
  }

}