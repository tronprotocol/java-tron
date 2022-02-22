package stest.tron.wallet.common.client.utils;

import static org.tron.common.crypto.Hash.sha3omit12;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.api.GrpcAPI.DecryptNotes.NoteTx;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.ExchangeList;
import org.tron.api.GrpcAPI.Note;
import org.tron.api.GrpcAPI.PrivateParameters;
import org.tron.api.GrpcAPI.ReceiveNote;
import org.tron.api.GrpcAPI.Return;
import org.tron.api.GrpcAPI.SpendNote;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.GrpcAPI.TransactionSignWeight;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletGrpc.WalletBlockingStub;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.ECKey.ECDSASignature;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.Base58;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ByteUtil;
import org.tron.common.utils.Commons;
import org.tron.core.Wallet;
import org.tron.core.exception.CancelException;
import org.tron.core.zen.address.ExpandedSpendingKey;
import org.tron.core.zen.address.SpendingKey;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Key;
import org.tron.protos.Protocol.Permission;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Transaction.Result;
import org.tron.protos.Protocol.Transaction.raw;
import org.tron.protos.contract.AccountContract.AccountCreateContract;
import org.tron.protos.contract.AccountContract.AccountPermissionUpdateContract;
import org.tron.protos.contract.AccountContract.AccountUpdateContract;
import org.tron.protos.contract.AccountContract.SetAccountIdContract;
import org.tron.protos.contract.AssetIssueContractOuterClass.AssetIssueContract;
import org.tron.protos.contract.AssetIssueContractOuterClass.ParticipateAssetIssueContract;
import org.tron.protos.contract.AssetIssueContractOuterClass.TransferAssetContract;
import org.tron.protos.contract.AssetIssueContractOuterClass.UnfreezeAssetContract;
import org.tron.protos.contract.AssetIssueContractOuterClass.UpdateAssetContract;
import org.tron.protos.contract.BalanceContract.FreezeBalanceContract;
import org.tron.protos.contract.BalanceContract.TransferContract;
import org.tron.protos.contract.BalanceContract.UnfreezeBalanceContract;
import org.tron.protos.contract.BalanceContract.WithdrawBalanceContract;
import org.tron.protos.contract.ExchangeContract.ExchangeCreateContract;
import org.tron.protos.contract.ExchangeContract.ExchangeInjectContract;
import org.tron.protos.contract.ExchangeContract.ExchangeTransactionContract;
import org.tron.protos.contract.ExchangeContract.ExchangeWithdrawContract;
import org.tron.protos.contract.MarketContract;
import org.tron.protos.contract.ProposalContract.ProposalApproveContract;
import org.tron.protos.contract.ProposalContract.ProposalCreateContract;
import org.tron.protos.contract.ProposalContract.ProposalDeleteContract;
import org.tron.protos.contract.ShieldContract;
import org.tron.protos.contract.ShieldContract.IncrementalMerkleVoucherInfo;
import org.tron.protos.contract.ShieldContract.OutputPoint;
import org.tron.protos.contract.ShieldContract.OutputPointInfo;
import org.tron.protos.contract.SmartContractOuterClass.ClearABIContract;
import org.tron.protos.contract.SmartContractOuterClass.CreateSmartContract;
import org.tron.protos.contract.SmartContractOuterClass.CreateSmartContract.Builder;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import org.tron.protos.contract.SmartContractOuterClass.TriggerSmartContract;
import org.tron.protos.contract.SmartContractOuterClass.UpdateEnergyLimitContract;
import org.tron.protos.contract.SmartContractOuterClass.UpdateSettingContract;
import org.tron.protos.contract.StorageContract.UpdateBrokerageContract;
import org.tron.protos.contract.WitnessContract.VoteWitnessContract;
import org.tron.protos.contract.WitnessContract.WitnessCreateContract;
import org.tron.protos.contract.WitnessContract.WitnessUpdateContract;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.WalletClient;


public class PublicMethedForMutiSign {

  private static final Logger logger = LoggerFactory.getLogger("TestLogger");
  Wallet wallet = new Wallet();

  /**
   * constructor.
   */

  public static Boolean createAssetIssue(byte[] address, String name, Long totalSupply,
      Integer trxNum, Integer icoNum, Long startTime, Long endTime, Integer voteScore,
      String description, String url, Long freeAssetNetLimit, Long publicFreeAssetNetLimit,
      Long fronzenAmount, Long frozenDay, String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull, String[] permissionKeyString) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;
    try {
      AssetIssueContract.Builder builder = AssetIssueContract.newBuilder();
      builder.setOwnerAddress(ByteString.copyFrom(address));
      builder.setName(ByteString.copyFrom(name.getBytes()));
      builder.setTotalSupply(totalSupply);
      builder.setTrxNum(trxNum);
      builder.setNum(icoNum);
      builder.setStartTime(startTime);
      builder.setEndTime(endTime);
      builder.setVoteScore(voteScore);
      builder.setDescription(ByteString.copyFrom(description.getBytes()));
      builder.setUrl(ByteString.copyFrom(url.getBytes()));
      builder.setFreeAssetNetLimit(freeAssetNetLimit);
      builder.setPublicFreeAssetNetLimit(publicFreeAssetNetLimit);
      AssetIssueContract.FrozenSupply.Builder frozenBuilder = AssetIssueContract.FrozenSupply
          .newBuilder();
      frozenBuilder.setFrozenAmount(fronzenAmount);
      frozenBuilder.setFrozenDays(frozenDay);
      builder.addFrozenSupply(0, frozenBuilder);

      TransactionExtention transactionExtention = blockingStubFull
          .createAssetIssue2(builder.build());
      Return ret = transactionExtention.getResult();
      if (!ret.getResult()) {
        System.out.println("Code = " + ret.getCode());
        System.out.println("Message = " + ret.getMessage().toStringUtf8());
        return false;
      }

      Transaction transaction = transactionExtention.getTransaction();
      if (transaction == null || transaction.getRawData().getContractCount() == 0) {
        System.out.println("Transaction is empty");
        return false;
      }
      transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);

      return broadcastTransaction(transaction, blockingStubFull);
    } catch (Exception ex) {
      ex.printStackTrace();
      return false;
    }
  }


  /**
   * constructor.
   */

  public static Boolean createAssetIssueWithpermissionId(byte[] address, String name,
      Long totalSupply, Integer trxNum, Integer icoNum, Long startTime, Long endTime,
      Integer voteScore, String description, String url, Long freeAssetNetLimit,
      Long publicFreeAssetNetLimit, Long fronzenAmount, Long frozenDay, String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull, int permissionId,
      String[] permissionKeyString) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;
    try {
      AssetIssueContract.Builder builder = AssetIssueContract.newBuilder();
      builder.setOwnerAddress(ByteString.copyFrom(address));
      builder.setName(ByteString.copyFrom(name.getBytes()));
      builder.setTotalSupply(totalSupply);
      builder.setTrxNum(trxNum);
      builder.setNum(icoNum);
      builder.setStartTime(startTime);
      builder.setEndTime(endTime);
      builder.setVoteScore(voteScore);
      builder.setDescription(ByteString.copyFrom(description.getBytes()));
      builder.setUrl(ByteString.copyFrom(url.getBytes()));
      builder.setFreeAssetNetLimit(freeAssetNetLimit);
      builder.setPublicFreeAssetNetLimit(publicFreeAssetNetLimit);
      AssetIssueContract.FrozenSupply.Builder frozenBuilder = AssetIssueContract.FrozenSupply
          .newBuilder();
      frozenBuilder.setFrozenAmount(fronzenAmount);
      frozenBuilder.setFrozenDays(frozenDay);
      builder.addFrozenSupply(0, frozenBuilder);

      TransactionExtention transactionExtention = blockingStubFull
          .createAssetIssue2(builder.build());
      Return ret = transactionExtention.getResult();
      if (transactionExtention == null) {
        return false;
      }
      if (!ret.getResult()) {
        System.out.println("Code = " + ret.getCode());
        System.out.println("Message = " + ret.getMessage().toStringUtf8());
        return false;
      }
      Transaction transaction = transactionExtention.getTransaction();
      if (transaction == null || transaction.getRawData().getContractCount() == 0) {
        System.out.println("Transaction is empty");
        return false;
      }
      try {
        transaction = setPermissionId(transaction, permissionId);
      } catch (CancelException e) {
        e.printStackTrace();
      }
      System.out.println(
          "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));
      transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);

      return broadcastTransaction(transaction, blockingStubFull);
    } catch (Exception ex) {
      ex.printStackTrace();
      return false;
    }
  }

  /**
   * constructor.
   */

  public static String createAssetIssueForTransactionId(byte[] address, String name,
      Long totalSupply, Integer trxNum, Integer icoNum, Long startTime, Long endTime,
      Integer voteScore, String description, String url, Long freeAssetNetLimit,
      Long publicFreeAssetNetLimit, Long fronzenAmount, Long frozenDay, String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull, String[] permissionKeyString) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;
    try {
      AssetIssueContract.Builder builder = AssetIssueContract.newBuilder();
      builder.setOwnerAddress(ByteString.copyFrom(address));
      builder.setName(ByteString.copyFrom(name.getBytes()));
      builder.setTotalSupply(totalSupply);
      builder.setTrxNum(trxNum);
      builder.setNum(icoNum);
      builder.setStartTime(startTime);
      builder.setEndTime(endTime);
      builder.setVoteScore(voteScore);
      builder.setDescription(ByteString.copyFrom(description.getBytes()));
      builder.setUrl(ByteString.copyFrom(url.getBytes()));
      builder.setFreeAssetNetLimit(freeAssetNetLimit);
      builder.setPublicFreeAssetNetLimit(publicFreeAssetNetLimit);
      AssetIssueContract.FrozenSupply.Builder frozenBuilder = AssetIssueContract.FrozenSupply
          .newBuilder();
      frozenBuilder.setFrozenAmount(fronzenAmount);
      frozenBuilder.setFrozenDays(frozenDay);
      builder.addFrozenSupply(0, frozenBuilder);

      Transaction transaction = blockingStubFull.createAssetIssue(builder.build());
      if (transaction == null || transaction.getRawData().getContractCount() == 0) {
        logger.info("transaction == null");
        return null;
      }
      transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);

      boolean result = broadcastTransaction(transaction, blockingStubFull);
      if (!result) {
        return null;
      } else {
        return ByteArray.toHexString(Sha256Hash.hash(CommonParameter.getInstance()
            .isECKeyCryptoEngine(), transaction.getRawData().toByteArray()));
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      return null;
    }
  }

  /**
   * constructor.
   */
  public static boolean broadcastTransaction(Transaction transaction,
      WalletGrpc.WalletBlockingStub blockingStubFull) {

    Return response = PublicMethed.broadcastTransaction(transaction, blockingStubFull);
    return response.getResult();
  }


  /**
   * constructor.
   */
  public static Account queryAccount(byte[] address,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ByteString addressBs = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBs).build();
    return blockingStubFull.getAccount(request);
  }

  /**
   * constructor.
   */

  public static Account queryAccount(byte[] address,
      WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFull) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ByteString addressBs = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBs).build();
    return blockingStubFull.getAccount(request);
  }


  /**
   * constructor.
   */

  public static Account queryAccount(String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    byte[] address;
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;
    if (ecKey == null) {
      String pubKey = loadPubKey(); //04 PubKey[128]
      if (StringUtils.isEmpty(pubKey)) {
        logger.warn("Warning: QueryAccount failed, no wallet address !!");
        return null;
      }
      byte[] pubKeyAsc = pubKey.getBytes();
      byte[] pubKeyHex = Hex.decode(pubKeyAsc);
      ecKey = ECKey.fromPublicOnly(pubKeyHex);
    }
    return grpcQueryAccount(ecKey.getAddress(), blockingStubFull);
  }

  /**
   * constructor.
   */

  public static String loadPubKey() {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    char[] buf = new char[0x100];
    return String.valueOf(buf, 32, 130);
  }

  /**
   * constructor.
   */

  public static byte[] getAddress(ECKey ecKey) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);

    return ecKey.getAddress();
  }

  /**
   * constructor.
   */

  public static Account grpcQueryAccount(byte[] address,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ByteString addressBs = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBs).build();
    return blockingStubFull.getAccount(request);
  }

  /**
   * constructor.
   */

  public static Block getBlock(long blockNum, WalletGrpc.WalletBlockingStub blockingStubFull) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    GrpcAPI.NumberMessage.Builder builder = GrpcAPI.NumberMessage.newBuilder();
    builder.setNum(blockNum);
    return blockingStubFull.getBlockByNum(builder.build());
  }

  /**
   * constructor.
   */

  public static Transaction signTransaction(ECKey ecKey, Transaction transaction) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    if (ecKey == null || ecKey.getPrivKey() == null) {
      return null;
    }
    transaction = TransactionUtils.setTimestamp(transaction);
    return TransactionUtils.sign(transaction, ecKey);
  }

  /**
   * constructor.
   */
  private static Transaction signTransaction(Transaction transaction,
      WalletGrpc.WalletBlockingStub blockingStubFull, String[] priKeys) {
    if (transaction.getRawData().getTimestamp() == 0) {
      transaction = TransactionUtils.setTimestamp(transaction);
    }

    long currentTime = System.currentTimeMillis();//*1000000 + System.nanoTime()%1000000;
    Transaction.Builder builder = transaction.toBuilder();
    org.tron.protos.Protocol.Transaction.raw.Builder rowBuilder = transaction.getRawData()
        .toBuilder();
    rowBuilder.setTimestamp(currentTime);
    builder.setRawData(rowBuilder.build());
    transaction = builder.build();

    for (int i = 0; i < priKeys.length; i += 1) {
      String priKey = priKeys[i];
      ECKey temKey = null;
      try {
        BigInteger priK = new BigInteger(priKey, 16);
        temKey = ECKey.fromPrivate(priK);
      } catch (Exception ex) {
        ex.printStackTrace();
      }
      ECKey ecKey = temKey;

      transaction = TransactionUtils.sign(transaction, ecKey);
      TransactionSignWeight weight = blockingStubFull.getTransactionSignWeight(transaction);
      if (weight.getResult().getCode()
          == TransactionSignWeight.Result.response_code.ENOUGH_PERMISSION) {
        break;
      }
      if (weight.getResult().getCode()
          == TransactionSignWeight.Result.response_code.NOT_ENOUGH_PERMISSION) {
        continue;
      }
    }
    return transaction;
  }


  /**
   * constructor.
   */
  public static boolean participateAssetIssue(byte[] to, byte[] assertName, long amount,
      byte[] from, String priKey, WalletGrpc.WalletBlockingStub blockingStubFull,
      String[] permissionKeyString) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    ParticipateAssetIssueContract.Builder builder = ParticipateAssetIssueContract.newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsName = ByteString.copyFrom(assertName);
    ByteString bsOwner = ByteString.copyFrom(from);
    builder.setToAddress(bsTo);
    builder.setAssetName(bsName);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);
    ParticipateAssetIssueContract contract = builder.build();
    Transaction transaction = blockingStubFull.participateAssetIssue(contract);
    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);

    return broadcastTransaction(transaction, blockingStubFull);
  }

  /**
   * constructor.
   */
  public static boolean participateAssetIssueWithPermissionId(byte[] to, byte[] assertName,
      long amount, byte[] from, String priKey, int permissionId,
      WalletGrpc.WalletBlockingStub blockingStubFull, String[] permissionKeyString) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    ParticipateAssetIssueContract.Builder builder = ParticipateAssetIssueContract.newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsName = ByteString.copyFrom(assertName);
    ByteString bsOwner = ByteString.copyFrom(from);
    builder.setToAddress(bsTo);
    builder.setAssetName(bsName);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);
    ParticipateAssetIssueContract contract = builder.build();
    Transaction transaction = blockingStubFull.participateAssetIssue(contract);
    try {
      transaction = setPermissionId(transaction, permissionId);
    } catch (CancelException e) {
      e.printStackTrace();
    }
    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);

    return broadcastTransaction(transaction, blockingStubFull);
  }

  /**
   * constructor.
   */
  public static String participateAssetIssueForTransactionId(byte[] to, byte[] assertName,
      long amount, byte[] from, String priKey, int permissionId,
      WalletGrpc.WalletBlockingStub blockingStubFull, String[] permissionKeyString) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    ParticipateAssetIssueContract.Builder builder = ParticipateAssetIssueContract.newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsName = ByteString.copyFrom(assertName);
    ByteString bsOwner = ByteString.copyFrom(from);
    builder.setToAddress(bsTo);
    builder.setAssetName(bsName);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);
    ParticipateAssetIssueContract contract = builder.build();
    Transaction transaction = blockingStubFull.participateAssetIssue(contract);
    try {
      transaction = setPermissionId(transaction, permissionId);
    } catch (CancelException e) {
      e.printStackTrace();
    }
    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);

    boolean result = broadcastTransaction(transaction, blockingStubFull);
    if (!result) {
      return null;
    }
    return ByteArray.toHexString(Sha256Hash.hash(CommonParameter.getInstance()
        .isECKeyCryptoEngine(), transaction.getRawData().toByteArray()));
  }

  /**
   * constructor.
   */

  public static Boolean freezeBalance(byte[] addRess, long freezeBalance, long freezeDuration,
      String priKey, WalletGrpc.WalletBlockingStub blockingStubFull, String[] permissionKeyString) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    byte[] address = addRess;
    long frozenBalance = freezeBalance;
    long frozenDuration = freezeDuration;
    //String priKey = testKey002;
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;
    Block currentBlock = blockingStubFull.getNowBlock(EmptyMessage.newBuilder().build());
    final Long beforeBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    Account beforeFronzen = queryAccount(priKey, blockingStubFull);
    Long beforeFrozenBalance = 0L;
    //Long beforeBandwidth     = beforeFronzen.getBandwidth();
    if (beforeFronzen.getFrozenCount() != 0) {
      beforeFrozenBalance = beforeFronzen.getFrozen(0).getFrozenBalance();
      //beforeBandwidth     = beforeFronzen.getBandwidth();
      //logger.info(Long.toString(beforeFronzen.getBandwidth()));
      logger.info(Long.toString(beforeFronzen.getFrozen(0).getFrozenBalance()));
    }

    FreezeBalanceContract.Builder builder = FreezeBalanceContract.newBuilder();
    ByteString byteAddreess = ByteString.copyFrom(address);

    builder.setOwnerAddress(byteAddreess).setFrozenBalance(frozenBalance)
        .setFrozenDuration(frozenDuration);

    FreezeBalanceContract contract = builder.build();
    Transaction transaction = blockingStubFull.freezeBalance(contract);

    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction = null");
      return null;
    }

    transaction = TransactionUtils.setTimestamp(transaction);
    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);

    return broadcastTransaction(transaction, blockingStubFull);

  }

  /**
   * constructor.
   */

  public static Boolean freezeBalanceWithPermissionId(byte[] addRess, long freezeBalance,
      long freezeDuration, int permissionId, String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull, String[] permissionKeyString) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    byte[] address = addRess;
    long frozenBalance = freezeBalance;
    long frozenDuration = freezeDuration;
    //String priKey = testKey002;
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;
    Block currentBlock = blockingStubFull.getNowBlock(EmptyMessage.newBuilder().build());
    final Long beforeBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    Account beforeFronzen = queryAccount(priKey, blockingStubFull);
    Long beforeFrozenBalance = 0L;
    //Long beforeBandwidth     = beforeFronzen.getBandwidth();
    if (beforeFronzen.getFrozenCount() != 0) {
      beforeFrozenBalance = beforeFronzen.getFrozen(0).getFrozenBalance();
      //beforeBandwidth     = beforeFronzen.getBandwidth();
      //logger.info(Long.toString(beforeFronzen.getBandwidth()));
      logger.info(Long.toString(beforeFronzen.getFrozen(0).getFrozenBalance()));
    }

    FreezeBalanceContract.Builder builder = FreezeBalanceContract.newBuilder();
    ByteString byteAddreess = ByteString.copyFrom(address);

    builder.setOwnerAddress(byteAddreess).setFrozenBalance(frozenBalance)
        .setFrozenDuration(frozenDuration);

    FreezeBalanceContract contract = builder.build();
    Transaction transaction = blockingStubFull.freezeBalance(contract);

    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction = null");
      return null;
    }

    try {
      transaction = setPermissionId(transaction, permissionId);
    } catch (CancelException e) {
      e.printStackTrace();
    }

    transaction = TransactionUtils.setTimestamp(transaction);
    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);

    return broadcastTransaction(transaction, blockingStubFull);

  }

  /**
   * constructor.
   */

  public static Boolean unFreezeBalanceWithPermissionId(byte[] address, String priKey,
      int resourceCode, byte[] receiverAddress, int permissionId,
      WalletGrpc.WalletBlockingStub blockingStubFull, String[] permissionKeyString) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;
    UnfreezeBalanceContract.Builder builder = UnfreezeBalanceContract.newBuilder();
    ByteString byteAddreess = ByteString.copyFrom(address);
    builder.setOwnerAddress(byteAddreess).setResourceValue(resourceCode);
    if (receiverAddress != null) {
      ByteString receiverAddressBytes = ByteString.copyFrom(receiverAddress);
      builder.setReceiverAddress(receiverAddressBytes);
    }

    UnfreezeBalanceContract contract = builder.build();
    Transaction transaction = blockingStubFull.unfreezeBalance(contract);
    try {
      transaction = setPermissionId(transaction, permissionId);
    } catch (CancelException e) {
      e.printStackTrace();
    }
    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);

    return broadcastTransaction(transaction, blockingStubFull);
  }

  /**
   * constructor.
   */

  public static Boolean unFreezeBalance(byte[] address, String priKey, int resourceCode,
      byte[] receiverAddress, WalletGrpc.WalletBlockingStub blockingStubFull,
      String[] permissionKeyString) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;
    UnfreezeBalanceContract.Builder builder = UnfreezeBalanceContract.newBuilder();
    ByteString byteAddreess = ByteString.copyFrom(address);
    builder.setOwnerAddress(byteAddreess).setResourceValue(resourceCode);
    if (receiverAddress != null) {
      ByteString receiverAddressBytes = ByteString.copyFrom(receiverAddress);
      builder.setReceiverAddress(receiverAddressBytes);
    }

    UnfreezeBalanceContract contract = builder.build();
    Transaction transaction = blockingStubFull.unfreezeBalance(contract);
    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);

    return broadcastTransaction(transaction, blockingStubFull);
  }

  /**
   * constructor.
   */

  public static Boolean sendcoin(byte[] to, long amount, byte[] owner, String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull, String[] permissionKeyString) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    //String priKey = testKey002;
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;
    //Protocol.Account search = queryAccount(priKey, blockingStubFull);

    TransferContract.Builder builder = TransferContract.newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsOwner = ByteString.copyFrom(owner);
    builder.setToAddress(bsTo);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);

    TransferContract contract = builder.build();
    Transaction transaction = blockingStubFull.createTransaction(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction ==null");
      return null;
    }
    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);

    return broadcastTransaction(transaction, blockingStubFull);

  }

  /**
   * constructor.
   */

  public static String sendcoinGetTransactionHex(byte[] to, long amount, byte[] owner,
      String priKey, WalletGrpc.WalletBlockingStub blockingStubFull, String[] permissionKeyString) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    //String priKey = testKey002;
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;
    //Protocol.Account search = queryAccount(priKey, blockingStubFull);

    TransferContract.Builder builder = TransferContract.newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsOwner = ByteString.copyFrom(owner);
    builder.setToAddress(bsTo);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);

    TransferContract contract = builder.build();
    Transaction transaction = blockingStubFull.createTransaction(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction ==null");
      return null;
    }
    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);

    logger.info("HEX transaction is : " + "transaction hex string is " + ByteArray
        .toHexString(transaction.toByteArray()));
    return ByteArray.toHexString(transaction.toByteArray());

  }


  /**
   * constructor.
   */
  public static boolean updateAsset(byte[] address, byte[] description, byte[] url, long newLimit,
      long newPublicLimit, String priKey, WalletGrpc.WalletBlockingStub blockingStubFull,
      String[] permissionKeyString) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;
    UpdateAssetContract.Builder builder = UpdateAssetContract.newBuilder();
    ByteString basAddreess = ByteString.copyFrom(address);
    builder.setDescription(ByteString.copyFrom(description));
    builder.setUrl(ByteString.copyFrom(url));
    builder.setNewLimit(newLimit);
    builder.setNewPublicLimit(newPublicLimit);
    builder.setOwnerAddress(basAddreess);

    UpdateAssetContract contract = builder.build();
    Transaction transaction = blockingStubFull.updateAsset(contract);

    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }

    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);

    return broadcastTransaction(transaction, blockingStubFull);
  }

  /**
   * constructor.
   */
  public static boolean updateAssetWithPermissionId(byte[] address, byte[] description, byte[] url,
      long newLimit, long newPublicLimit, String priKey, int permissionId,
      WalletGrpc.WalletBlockingStub blockingStubFull, String[] permissionKeyString) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;
    UpdateAssetContract.Builder builder = UpdateAssetContract.newBuilder();
    ByteString basAddreess = ByteString.copyFrom(address);
    builder.setDescription(ByteString.copyFrom(description));
    builder.setUrl(ByteString.copyFrom(url));
    builder.setNewLimit(newLimit);
    builder.setNewPublicLimit(newPublicLimit);
    builder.setOwnerAddress(basAddreess);

    UpdateAssetContract contract = builder.build();
    Transaction transaction = blockingStubFull.updateAsset(contract);

    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }

    try {
      transaction = setPermissionId(transaction, permissionId);
    } catch (CancelException e) {
      e.printStackTrace();
    }

    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);

    return broadcastTransaction(transaction, blockingStubFull);
  }

  /**
   * constructor.
   */
  public static String updateAssetForTransactionId(byte[] address, byte[] description, byte[] url,
      long newLimit, long newPublicLimit, String priKey, int permissionId,
      WalletGrpc.WalletBlockingStub blockingStubFull, String[] permissionKeyString) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;
    UpdateAssetContract.Builder builder = UpdateAssetContract.newBuilder();
    ByteString basAddreess = ByteString.copyFrom(address);
    builder.setDescription(ByteString.copyFrom(description));
    builder.setUrl(ByteString.copyFrom(url));
    builder.setNewLimit(newLimit);
    builder.setNewPublicLimit(newPublicLimit);
    builder.setOwnerAddress(basAddreess);

    UpdateAssetContract contract = builder.build();
    Transaction transaction = blockingStubFull.updateAsset(contract);

    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return null;
    }

    try {
      transaction = setPermissionId(transaction, permissionId);
    } catch (CancelException e) {
      e.printStackTrace();
    }

    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);

    boolean result = broadcastTransaction(transaction, blockingStubFull);
    if (!result) {
      return null;
    } else {
      return ByteArray.toHexString(Sha256Hash.hash(CommonParameter.getInstance()
          .isECKeyCryptoEngine(), transaction.getRawData().toByteArray()));
    }
  }

  /**
   * constructor.
   */

  public static boolean transferAsset(byte[] to, byte[] assertName, long amount, byte[] address,
      String priKey, WalletGrpc.WalletBlockingStub blockingStubFull, String[] permissionKeyString) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    TransferAssetContract.Builder builder = TransferAssetContract.newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsName = ByteString.copyFrom(assertName);
    ByteString bsOwner = ByteString.copyFrom(address);
    builder.setToAddress(bsTo);
    builder.setAssetName(bsName);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);

    TransferAssetContract contract = builder.build();
    Transaction transaction = blockingStubFull.transferAsset(contract);

    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      if (transaction == null) {
        logger.info("transaction == null");
      } else {
        logger.info("transaction.getRawData().getContractCount() == 0");
      }
      return false;
    }
    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);

    return broadcastTransaction(transaction, blockingStubFull);
  }

  /**
   * constructor.
   */

  public static boolean transferAssetWithpermissionId(byte[] to, byte[] assertName, long amount,
      byte[] address, String priKey, WalletGrpc.WalletBlockingStub blockingStubFull,
      int permissionId, String[] permissionKeyString) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    TransferAssetContract.Builder builder = TransferAssetContract.newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsName = ByteString.copyFrom(assertName);
    ByteString bsOwner = ByteString.copyFrom(address);
    builder.setToAddress(bsTo);
    builder.setAssetName(bsName);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);

    TransferAssetContract contract = builder.build();
    TransactionExtention transactionExtention = blockingStubFull.transferAsset2(contract);
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return false;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return false;
    }
    try {
      transaction = setPermissionId(transaction, permissionId);
    } catch (CancelException e) {
      e.printStackTrace();
    }
    System.out.println(
        "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));
    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);

    return broadcastTransaction(transaction, blockingStubFull);
  }

  /**
   * constructor.
   */

  public static String transferAssetForTransactionId(byte[] to, byte[] assertName, long amount,
      byte[] address, String priKey, WalletGrpc.WalletBlockingStub blockingStubFull,
      String[] permissionKeyString) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    TransferAssetContract.Builder builder = TransferAssetContract.newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsName = ByteString.copyFrom(assertName);
    ByteString bsOwner = ByteString.copyFrom(address);
    builder.setToAddress(bsTo);
    builder.setAssetName(bsName);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);

    TransferAssetContract contract = builder.build();
    Transaction transaction = blockingStubFull.transferAsset(contract);

    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      if (transaction == null) {
        logger.info("transaction == null");
      } else {
        logger.info("transaction.getRawData().getContractCount() == 0");
      }
      return null;
    }
    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);

    boolean result = broadcastTransaction(transaction, blockingStubFull);
    if (!result) {
      return null;
    } else {
      return ByteArray.toHexString(Sha256Hash.hash(CommonParameter.getInstance()
          .isECKeyCryptoEngine(), transaction.getRawData().toByteArray()));
    }
  }

  /**
   * constructor.
   */

  public static boolean updateAccount(byte[] addressBytes, byte[] accountNameBytes, String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull, String[] permissionKeyString) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    AccountUpdateContract.Builder builder = AccountUpdateContract.newBuilder();
    ByteString basAddreess = ByteString.copyFrom(addressBytes);
    ByteString bsAccountName = ByteString.copyFrom(accountNameBytes);

    builder.setAccountName(bsAccountName);
    builder.setOwnerAddress(basAddreess);

    AccountUpdateContract contract = builder.build();
    Transaction transaction = blockingStubFull.updateAccount(contract);

    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("Please check!!! transaction == null");
      return false;
    }
    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);

    return broadcastTransaction(transaction, blockingStubFull);
  }

  /**
   * constructor.
   */

  public static boolean waitProduceNextBlock(WalletGrpc.WalletBlockingStub blockingStubFull) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    Block currentBlock = blockingStubFull.getNowBlock(EmptyMessage.newBuilder().build());
    final Long currentNum = currentBlock.getBlockHeader().getRawData().getNumber();

    Block nextBlock = blockingStubFull.getNowBlock(EmptyMessage.newBuilder().build());
    Long nextNum = nextBlock.getBlockHeader().getRawData().getNumber();

    Integer wait = 0;
    logger.info(
        "Block num is " + Long.toString(currentBlock.getBlockHeader().getRawData().getNumber()));
    while (nextNum <= currentNum + 1 && wait <= 15) {
      try {
        Thread.sleep(3000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      logger.info("Wait to produce next block");
      nextBlock = blockingStubFull.getNowBlock(EmptyMessage.newBuilder().build());
      nextNum = nextBlock.getBlockHeader().getRawData().getNumber();
      if (wait == 15) {
        logger.info("These 45 second didn't produce a block,please check.");
        return false;
      }
      wait++;
    }
    logger.info("quit normally");
    return true;
  }

  /**
   * constructor.
   */

  public static boolean createAccount(byte[] ownerAddress, byte[] newAddress, String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull, String[] permissionKeyString) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    byte[] owner = ownerAddress;
    AccountCreateContract.Builder builder = AccountCreateContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setAccountAddress(ByteString.copyFrom(newAddress));
    AccountCreateContract contract = builder.build();
    Transaction transaction = blockingStubFull.createAccount(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction == null");
    }
    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);

    return broadcastTransaction(transaction, blockingStubFull);

  }

  /**
   * constructor.
   */

  public static boolean createAccountWhtiPermissionId(byte[] ownerAddress, byte[] newAddress,
      String priKey, WalletGrpc.WalletBlockingStub blockingStubFull, int permissionId,
      String[] permissionKeyString) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    byte[] owner = ownerAddress;
    AccountCreateContract.Builder builder = AccountCreateContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setAccountAddress(ByteString.copyFrom(newAddress));
    AccountCreateContract contract = builder.build();
    TransactionExtention transactionExtention = blockingStubFull.createAccount2(contract);
    if (transactionExtention == null) {
      return false;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return false;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return false;
    }
    try {
      transaction = setPermissionId(transaction, permissionId);
    } catch (CancelException e) {
      e.printStackTrace();
    }
    System.out.println(
        "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));
    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);

    return broadcastTransaction(transaction, blockingStubFull);

  }

  /**
   * constructor.
   */

  public static boolean createProposal(byte[] ownerAddress, String priKey,
      HashMap<Long, Long> parametersMap, WalletGrpc.WalletBlockingStub blockingStubFull,
      String[] permissionKeyString) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    byte[] owner = ownerAddress;
    ProposalCreateContract.Builder builder = ProposalCreateContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.putAllParameters(parametersMap);

    ProposalCreateContract contract = builder.build();
    TransactionExtention transactionExtention = blockingStubFull.proposalCreate(contract);
    if (transactionExtention == null) {
      return false;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return false;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return false;
    }
    System.out.println(
        "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));
    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);

    return broadcastTransaction(transaction, blockingStubFull);
  }

  /**
   * constructor.
   */

  public static boolean createProposalWithPermissionId(byte[] ownerAddress, String priKey,
      HashMap<Long, Long> parametersMap, int permissionId,
      WalletGrpc.WalletBlockingStub blockingStubFull, String[] permissionKeyString) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    byte[] owner = ownerAddress;
    ProposalCreateContract.Builder builder = ProposalCreateContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.putAllParameters(parametersMap);

    ProposalCreateContract contract = builder.build();
    TransactionExtention transactionExtention = blockingStubFull.proposalCreate(contract);
    if (transactionExtention == null) {
      return false;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return false;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return false;
    }
    try {
      transaction = setPermissionId(transaction, permissionId);
    } catch (CancelException e) {
      e.printStackTrace();
    }
    System.out.println(
        "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));
    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);

    return broadcastTransaction(transaction, blockingStubFull);
  }

  /**
   * constructor.
   */

  public static boolean approveProposal(byte[] ownerAddress, String priKey, long id,
      boolean isAddApproval, WalletGrpc.WalletBlockingStub blockingStubFull,
      String[] permissionKeyString) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    byte[] owner = ownerAddress;
    ProposalApproveContract.Builder builder = ProposalApproveContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setProposalId(id);
    builder.setIsAddApproval(isAddApproval);
    ProposalApproveContract contract = builder.build();
    TransactionExtention transactionExtention = blockingStubFull.proposalApprove(contract);
    if (transactionExtention == null) {
      return false;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return false;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return false;
    }
    System.out.println(
        "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));
    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);

    return broadcastTransaction(transaction, blockingStubFull);
  }

  /**
   * constructor.
   */

  public static boolean approveProposalWithPermission(byte[] ownerAddress, String priKey, long id,
      boolean isAddApproval, int permissionId, WalletGrpc.WalletBlockingStub blockingStubFull,
      String[] permissionKeyString) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    byte[] owner = ownerAddress;
    ProposalApproveContract.Builder builder = ProposalApproveContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setProposalId(id);
    builder.setIsAddApproval(isAddApproval);
    ProposalApproveContract contract = builder.build();
    TransactionExtention transactionExtention = blockingStubFull.proposalApprove(contract);
    if (transactionExtention == null) {
      return false;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return false;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return false;
    }
    try {
      transaction = setPermissionId(transaction, permissionId);
    } catch (CancelException e) {
      e.printStackTrace();
    }
    System.out.println(
        "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));
    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);

    return broadcastTransaction(transaction, blockingStubFull);
  }

  /**
   * constructor.
   */

  public static boolean deleteProposal(byte[] ownerAddress, String priKey, long id,
      WalletGrpc.WalletBlockingStub blockingStubFull, String[] permissionKeyString) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    byte[] owner = ownerAddress;
    ProposalDeleteContract.Builder builder = ProposalDeleteContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setProposalId(id);

    ProposalDeleteContract contract = builder.build();
    TransactionExtention transactionExtention = blockingStubFull.proposalDelete(contract);
    if (transactionExtention == null) {
      return false;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return false;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return false;
    }
    System.out.println(
        "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));
    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);

    return broadcastTransaction(transaction, blockingStubFull);
  }

  /**
   * constructor.
   */

  public static boolean deleteProposalWithPermissionId(byte[] ownerAddress, String priKey, long id,
      int permissionId, WalletGrpc.WalletBlockingStub blockingStubFull,
      String[] permissionKeyString) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    byte[] owner = ownerAddress;
    ProposalDeleteContract.Builder builder = ProposalDeleteContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setProposalId(id);

    ProposalDeleteContract contract = builder.build();
    TransactionExtention transactionExtention = blockingStubFull.proposalDelete(contract);
    if (transactionExtention == null) {
      return false;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return false;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return false;
    }
    try {
      transaction = setPermissionId(transaction, permissionId);
    } catch (CancelException e) {
      e.printStackTrace();
    }
    System.out.println(
        "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));
    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);

    return broadcastTransaction(transaction, blockingStubFull);
  }

  /**
   * constructor.
   */

  public static boolean setAccountId(byte[] accountIdBytes, byte[] ownerAddress, String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    byte[] owner = ownerAddress;
    SetAccountIdContract.Builder builder = SetAccountIdContract.newBuilder();
    ByteString bsAddress = ByteString.copyFrom(owner);
    ByteString bsAccountId = ByteString.copyFrom(accountIdBytes);
    builder.setAccountId(bsAccountId);
    builder.setOwnerAddress(bsAddress);
    SetAccountIdContract contract = builder.build();
    Transaction transaction = blockingStubFull.setAccountId(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction == null");
    }
    transaction = signTransaction(ecKey, transaction);
    Return response = broadcastTransaction1(transaction, blockingStubFull);
    return response.getResult();
  }

  /**
   * constructor.
   */

  public static Boolean freezeBalanceGetEnergy(byte[] addRess, long freezeBalance,
      long freezeDuration, int resourceCode, String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull, String[] permissionKeyString) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    byte[] address = addRess;
    long frozenBalance = freezeBalance;
    long frozenDuration = freezeDuration;
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    FreezeBalanceContract.Builder builder = FreezeBalanceContract.newBuilder();
    ByteString byteAddreess = ByteString.copyFrom(address);

    builder.setOwnerAddress(byteAddreess).setFrozenBalance(frozenBalance)
        .setFrozenDuration(frozenDuration).setResourceValue(resourceCode);

    FreezeBalanceContract contract = builder.build();
    Transaction transaction = blockingStubFull.freezeBalance(contract);

    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction = null");
      return false;
    }
    transaction = TransactionUtils.setTimestamp(transaction);
    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);

    return broadcastTransaction(transaction, blockingStubFull);
  }

  /**
   * constructor.
   */
  public static byte[] deployContract(String contractName, String abiString, String code,
      String data, Long feeLimit, long value, long consumeUserResourcePercent,
      String libraryAddress, String priKey, byte[] ownerAddress,
      WalletGrpc.WalletBlockingStub blockingStubFull, String[] permissionKeyString) {
    return deployContract(contractName, abiString, code, data, feeLimit, value,
        consumeUserResourcePercent, 1000L, "0", 0L, libraryAddress, priKey, ownerAddress,
        blockingStubFull, permissionKeyString);
  }

  /**
   * constructor.
   */

  public static byte[] deployContract(String contractName, String abiString, String code,
      String data, Long feeLimit, long value, long consumeUserResourcePercent,
      long originEnergyLimit, String tokenId, long tokenValue, String libraryAddress, String priKey,
      byte[] ownerAddress, WalletGrpc.WalletBlockingStub blockingStubFull,
      String[] permissionKeyString) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    byte[] owner = ownerAddress;
    SmartContract.ABI abi = jsonStr2Abi(abiString);
    if (abi == null) {
      logger.error("abi is null");
      return null;
    }
    //byte[] codeBytes = Hex.decode(code);
    SmartContract.Builder builder = SmartContract.newBuilder();
    builder.setName(contractName);
    builder.setOriginAddress(ByteString.copyFrom(owner));
    builder.setAbi(abi);
    builder.setConsumeUserResourcePercent(consumeUserResourcePercent);
    builder.setOriginEnergyLimit(originEnergyLimit);

    if (value != 0) {

      builder.setCallValue(value);
    }

    byte[] byteCode;
    if (null != libraryAddress) {
      byteCode = replaceLibraryAddress(code, libraryAddress);
    } else {
      byteCode = Hex.decode(code);
    }
    builder.setBytecode(ByteString.copyFrom(byteCode));

    Builder contractBuilder = CreateSmartContract.newBuilder();
    contractBuilder.setOwnerAddress(ByteString.copyFrom(owner));
    contractBuilder.setCallTokenValue(tokenValue);
    contractBuilder.setTokenId(Long.parseLong(tokenId));
    CreateSmartContract contractDeployContract = contractBuilder.setNewContract(builder.build())
        .build();

    TransactionExtention transactionExtention = blockingStubFull
        .deployContract(contractDeployContract);
    if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
      System.out.println("RPC create trx failed!");
      if (transactionExtention != null) {
        System.out.println("Code = " + transactionExtention.getResult().getCode());
        System.out
            .println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
      }
      return null;
    }

    final TransactionExtention.Builder texBuilder = TransactionExtention.newBuilder();
    Transaction.Builder transBuilder = Transaction.newBuilder();
    Transaction.raw.Builder rawBuilder = transactionExtention.getTransaction().getRawData()
        .toBuilder();
    rawBuilder.setFeeLimit(feeLimit);
    transBuilder.setRawData(rawBuilder);
    for (int i = 0; i < transactionExtention.getTransaction().getSignatureCount(); i++) {
      ByteString s = transactionExtention.getTransaction().getSignature(i);
      transBuilder.setSignature(i, s);
    }
    for (int i = 0; i < transactionExtention.getTransaction().getRetCount(); i++) {
      Result r = transactionExtention.getTransaction().getRet(i);
      transBuilder.setRet(i, r);
    }
    texBuilder.setTransaction(transBuilder);
    texBuilder.setResult(transactionExtention.getResult());
    texBuilder.setTxid(transactionExtention.getTxid());
    transactionExtention = texBuilder.build();

    byte[] contractAddress = PublicMethed
        .generateContractAddress(transactionExtention.getTransaction(), owner);
    System.out.println(
        "Your smart contract address will be: " + WalletClient.encode58Check(contractAddress));
    if (transactionExtention == null) {
      return null;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return null;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return null;
    }
    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);

    System.out.println(
        "txid = " + ByteArray.toHexString(Sha256Hash.hash(CommonParameter.getInstance()
            .isECKeyCryptoEngine(), transaction.getRawData().toByteArray())));
    contractAddress = PublicMethed.generateContractAddress(transaction, owner);
    System.out.println(
        "Your smart contract address will be: " + WalletClient.encode58Check(contractAddress));
    broadcastTransaction(transaction, blockingStubFull);
    return contractAddress;
  }

  /**
   * constructor.
   */

  public static String deployContractAndGetTransactionInfoById(String contractName,
      String abiString, String code, String data, Long feeLimit, long value,
      long consumeUserResourcePercent, String libraryAddress, String priKey, byte[] ownerAddress,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return deployContractAndGetTransactionInfoById(contractName, abiString, code, data, feeLimit,
        value, consumeUserResourcePercent, 1000L, "0", 0L, libraryAddress, priKey, ownerAddress,
        blockingStubFull);
  }

  /**
   * constructor.
   */

  public static String deployContractAndGetTransactionInfoById(String contractName,
      String abiString, String code, String data, Long feeLimit, long value,
      long consumeUserResourcePercent, long originEnergyLimit, String tokenId, long tokenValue,
      String libraryAddress, String priKey, byte[] ownerAddress,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    byte[] owner = ownerAddress;
    SmartContract.ABI abi = jsonStr2Abi(abiString);
    if (abi == null) {
      logger.error("abi is null");
      return null;
    }
    //byte[] codeBytes = Hex.decode(code);
    SmartContract.Builder builder = SmartContract.newBuilder();
    builder.setName(contractName);
    builder.setOriginAddress(ByteString.copyFrom(owner));
    builder.setAbi(abi);
    builder.setConsumeUserResourcePercent(consumeUserResourcePercent);
    builder.setOriginEnergyLimit(originEnergyLimit);

    if (value != 0) {

      builder.setCallValue(value);
    }

    byte[] byteCode;
    if (null != libraryAddress) {
      byteCode = replaceLibraryAddress(code, libraryAddress);
    } else {
      byteCode = Hex.decode(code);
    }
    builder.setBytecode(ByteString.copyFrom(byteCode));

    Builder contractBuilder = CreateSmartContract.newBuilder();
    contractBuilder.setOwnerAddress(ByteString.copyFrom(owner));
    contractBuilder.setCallTokenValue(tokenValue);
    contractBuilder.setTokenId(Long.parseLong(tokenId));
    CreateSmartContract contractDeployContract = contractBuilder.setNewContract(builder.build())
        .build();

    TransactionExtention transactionExtention = blockingStubFull
        .deployContract(contractDeployContract);
    if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
      System.out.println("RPC create trx failed!");
      if (transactionExtention != null) {
        System.out.println("Code = " + transactionExtention.getResult().getCode());
        System.out
            .println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
      }
      return null;
    }

    final TransactionExtention.Builder texBuilder = TransactionExtention.newBuilder();
    Transaction.Builder transBuilder = Transaction.newBuilder();
    Transaction.raw.Builder rawBuilder = transactionExtention.getTransaction().getRawData()
        .toBuilder();
    rawBuilder.setFeeLimit(feeLimit);
    transBuilder.setRawData(rawBuilder);
    for (int i = 0; i < transactionExtention.getTransaction().getSignatureCount(); i++) {
      ByteString s = transactionExtention.getTransaction().getSignature(i);
      transBuilder.setSignature(i, s);
    }
    for (int i = 0; i < transactionExtention.getTransaction().getRetCount(); i++) {
      Result r = transactionExtention.getTransaction().getRet(i);
      transBuilder.setRet(i, r);
    }
    texBuilder.setTransaction(transBuilder);
    texBuilder.setResult(transactionExtention.getResult());
    texBuilder.setTxid(transactionExtention.getTxid());
    transactionExtention = texBuilder.build();

    if (transactionExtention == null) {
      return null;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return null;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return null;
    }
    transaction = signTransaction(ecKey, transaction);
    System.out.println(
        "txid = " + ByteArray.toHexString(Sha256Hash.hash(CommonParameter.getInstance()
            .isECKeyCryptoEngine(), transaction.getRawData().toByteArray())));
    byte[] contractAddress = PublicMethed.generateContractAddress(transaction, owner);
    System.out.println(
        "Your smart contract address will be: " + WalletClient.encode58Check(contractAddress));
    Return response = broadcastTransaction1(transaction, blockingStubFull);
    if (response.getResult() == false) {
      return null;
    } else {
      return ByteArray.toHexString(Sha256Hash.hash(CommonParameter.getInstance()
          .isECKeyCryptoEngine(), transaction.getRawData().toByteArray()));
    }
  }

  /**
   * constructor.
   */

  public static SmartContract.ABI jsonStr2Abi(String jsonStr) {
    if (jsonStr == null) {
      return null;
    }

    JsonParser jsonParser = new JsonParser();
    JsonElement jsonElementRoot = jsonParser.parse(jsonStr);
    JsonArray jsonRoot = jsonElementRoot.getAsJsonArray();
    SmartContract.ABI.Builder abiBuilder = SmartContract.ABI.newBuilder();
    for (int index = 0; index < jsonRoot.size(); index++) {
      JsonElement abiItem = jsonRoot.get(index);
      boolean anonymous =
          abiItem.getAsJsonObject().get("anonymous") != null ? abiItem.getAsJsonObject()
              .get("anonymous").getAsBoolean() : false;
      final boolean constant =
          abiItem.getAsJsonObject().get("constant") != null ? abiItem.getAsJsonObject()
              .get("constant").getAsBoolean() : false;
      final String name =
          abiItem.getAsJsonObject().get("name") != null ? abiItem.getAsJsonObject().get("name")
              .getAsString() : null;
      JsonArray inputs =
          abiItem.getAsJsonObject().get("inputs") != null ? abiItem.getAsJsonObject().get("inputs")
              .getAsJsonArray() : null;
      final JsonArray outputs =
          abiItem.getAsJsonObject().get("outputs") != null ? abiItem.getAsJsonObject()
              .get("outputs").getAsJsonArray() : null;
      String type =
          abiItem.getAsJsonObject().get("type") != null ? abiItem.getAsJsonObject().get("type")
              .getAsString() : null;
      final boolean payable =
          abiItem.getAsJsonObject().get("payable") != null ? abiItem.getAsJsonObject()
              .get("payable").getAsBoolean() : false;
      final String stateMutability =
          abiItem.getAsJsonObject().get("stateMutability") != null ? abiItem.getAsJsonObject()
              .get("stateMutability").getAsString() : null;
      if (type == null) {
        logger.error("No type!");
        return null;
      }
      if (!type.equalsIgnoreCase("fallback") && null == inputs) {
        logger.error("No inputs!");
        return null;
      }

      SmartContract.ABI.Entry.Builder entryBuilder = SmartContract.ABI.Entry.newBuilder();
      entryBuilder.setAnonymous(anonymous);
      entryBuilder.setConstant(constant);
      if (name != null) {
        entryBuilder.setName(name);
      }

      /* { inputs : optional } since fallback function not requires inputs*/
      if (inputs != null) {
        for (int j = 0; j < inputs.size(); j++) {
          JsonElement inputItem = inputs.get(j);
          if (inputItem.getAsJsonObject().get("name") == null
              || inputItem.getAsJsonObject().get("type") == null) {
            logger.error("Input argument invalid due to no name or no type!");
            return null;
          }
          String inputName = inputItem.getAsJsonObject().get("name").getAsString();
          String inputType = inputItem.getAsJsonObject().get("type").getAsString();
          SmartContract.ABI.Entry.Param.Builder paramBuilder = SmartContract.ABI.Entry.Param
              .newBuilder();
          paramBuilder.setIndexed(false);
          paramBuilder.setName(inputName);
          paramBuilder.setType(inputType);
          entryBuilder.addInputs(paramBuilder.build());
        }
      }

      /* { outputs : optional } */
      if (outputs != null) {
        for (int k = 0; k < outputs.size(); k++) {
          JsonElement outputItem = outputs.get(k);
          if (outputItem.getAsJsonObject().get("name") == null
              || outputItem.getAsJsonObject().get("type") == null) {
            logger.error("Output argument invalid due to no name or no type!");
            return null;
          }
          String outputName = outputItem.getAsJsonObject().get("name").getAsString();
          String outputType = outputItem.getAsJsonObject().get("type").getAsString();
          SmartContract.ABI.Entry.Param.Builder paramBuilder = SmartContract.ABI.Entry.Param
              .newBuilder();
          paramBuilder.setIndexed(false);
          paramBuilder.setName(outputName);
          paramBuilder.setType(outputType);
          entryBuilder.addOutputs(paramBuilder.build());
        }
      }

      entryBuilder.setType(getEntryType(type));
      entryBuilder.setPayable(payable);
      if (stateMutability != null) {
        entryBuilder.setStateMutability(getStateMutability(stateMutability));
      }

      abiBuilder.addEntrys(entryBuilder.build());
    }

    return abiBuilder.build();
  }

  /**
   * constructor.
   */

  public static SmartContract.ABI.Entry.EntryType getEntryType(String type) {
    switch (type) {
      case "constructor":
        return SmartContract.ABI.Entry.EntryType.Constructor;
      case "function":
        return SmartContract.ABI.Entry.EntryType.Function;
      case "event":
        return SmartContract.ABI.Entry.EntryType.Event;
      case "fallback":
        return SmartContract.ABI.Entry.EntryType.Fallback;
      default:
        return SmartContract.ABI.Entry.EntryType.UNRECOGNIZED;
    }
  }

  /**
   * constructor.
   */

  public static SmartContract.ABI.Entry.StateMutabilityType getStateMutability(
      String stateMutability) {
    switch (stateMutability) {
      case "pure":
        return SmartContract.ABI.Entry.StateMutabilityType.Pure;
      case "view":
        return SmartContract.ABI.Entry.StateMutabilityType.View;
      case "nonpayable":
        return SmartContract.ABI.Entry.StateMutabilityType.Nonpayable;
      case "payable":
        return SmartContract.ABI.Entry.StateMutabilityType.Payable;
      default:
        return SmartContract.ABI.Entry.StateMutabilityType.UNRECOGNIZED;
    }
  }

  /**
   * constructor.
   */

  public static SmartContract getContract(byte[] address,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ByteString byteString = ByteString.copyFrom(address);
    BytesMessage bytesMessage = BytesMessage.newBuilder().setValue(byteString).build();
    Integer i = 0;
    while (blockingStubFull.getContract(bytesMessage).getName().isEmpty() && i++ < 4) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    logger.info("contract name is " + blockingStubFull.getContract(bytesMessage).getName());
    logger.info("contract address is " + WalletClient.encode58Check(address));
    return blockingStubFull.getContract(bytesMessage);
  }

  private static byte[] replaceLibraryAddress(String code, String libraryAddressPair) {

    String[] libraryAddressList = libraryAddressPair.split("[,]");

    for (int i = 0; i < libraryAddressList.length; i++) {
      String cur = libraryAddressList[i];

      int lastPosition = cur.lastIndexOf(":");
      if (-1 == lastPosition) {
        throw new RuntimeException("libraryAddress delimit by ':'");
      }
      String libraryName = cur.substring(0, lastPosition);
      String addr = cur.substring(lastPosition + 1);
      String libraryAddressHex = ByteArray.toHexString(Commons.decodeFromBase58Check(addr))
          .substring(2);

      String repeated = new String(new char[40 - libraryName.length() - 2]).replace("\0", "_");
      String beReplaced = "__" + libraryName + repeated;
      Matcher m = Pattern.compile(beReplaced).matcher(code);
      code = m.replaceAll(libraryAddressHex);
    }

    return Hex.decode(code);
  }

  private static byte[] replaceLibraryAddress_1(String code, byte[] libraryAddress) {

    String libraryAddressHex = ByteArray.toHexString(libraryAddress).substring(2);

    Matcher m = Pattern.compile("__.*__").matcher(code);
    code = m.replaceAll(libraryAddressHex);
    return Hex.decode(code);
  }

  /**
   * constructor.
   */

  public static boolean updateSetting(byte[] contractAddress, long consumeUserResourcePercent,
      String priKey, byte[] ownerAddress, WalletGrpc.WalletBlockingStub blockingStubFull,
      String[] permissionKeyString) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    byte[] owner = ownerAddress;
    UpdateSettingContract.Builder builder = UpdateSettingContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setContractAddress(ByteString.copyFrom(contractAddress));
    builder.setConsumeUserResourcePercent(consumeUserResourcePercent);

    UpdateSettingContract updateSettingContract = builder.build();
    TransactionExtention transactionExtention = blockingStubFull
        .updateSetting(updateSettingContract);
    if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
      System.out.println("RPC create trx failed!");
      if (transactionExtention != null) {
        System.out.println("Code = " + transactionExtention.getResult().getCode());
        System.out
            .println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
      }
      return false;
    }
    if (transactionExtention == null) {
      return false;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return false;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return false;
    }
    System.out.println(
        "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));
    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);

    return broadcastTransaction(transaction, blockingStubFull);
  }

  /**
   * constructor.
   */

  public static boolean updateSettingWithPermissionId(byte[] contractAddress,
      long consumeUserResourcePercent, String priKey, byte[] ownerAddress, int permissionId,
      WalletGrpc.WalletBlockingStub blockingStubFull, String[] permissionKeyString) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    byte[] owner = ownerAddress;
    UpdateSettingContract.Builder builder = UpdateSettingContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setContractAddress(ByteString.copyFrom(contractAddress));
    builder.setConsumeUserResourcePercent(consumeUserResourcePercent);

    UpdateSettingContract updateSettingContract = builder.build();
    TransactionExtention transactionExtention = blockingStubFull
        .updateSetting(updateSettingContract);
    if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
      System.out.println("RPC create trx failed!");
      if (transactionExtention != null) {
        System.out.println("Code = " + transactionExtention.getResult().getCode());
        System.out
            .println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
      }
      return false;
    }
    if (transactionExtention == null) {
      return false;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return false;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return false;
    }
    try {
      transaction = setPermissionId(transaction, permissionId);
    } catch (CancelException e) {
      e.printStackTrace();
    }
    System.out.println(
        "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));
    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);

    return broadcastTransaction(transaction, blockingStubFull);
  }

  /**
   * constructor.
   */

  public static boolean updateEnergyLimitWithPermissionId(byte[] contractAddress,
      long originEnergyLimit, String priKey, byte[] ownerAddress, int permissionId,
      WalletGrpc.WalletBlockingStub blockingStubFull, String[] permissionKeyString) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    byte[] owner = ownerAddress;
    UpdateEnergyLimitContract.Builder builder = UpdateEnergyLimitContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setContractAddress(ByteString.copyFrom(contractAddress));
    builder.setOriginEnergyLimit(originEnergyLimit);

    UpdateEnergyLimitContract updateEnergyLimitContract = builder.build();
    TransactionExtention transactionExtention = blockingStubFull
        .updateEnergyLimit(updateEnergyLimitContract);
    if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
      System.out.println("RPC create trx failed!");
      if (transactionExtention != null) {
        System.out.println("Code = " + transactionExtention.getResult().getCode());
        System.out
            .println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
      }
      return false;
    }
    if (transactionExtention == null) {
      return false;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return false;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return false;
    }
    try {
      transaction = setPermissionId(transaction, permissionId);
    } catch (CancelException e) {
      e.printStackTrace();
    }
    System.out.println(
        "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));
    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);

    return broadcastTransaction(transaction, blockingStubFull);
  }

  /**
   * constructor.
   */
  public static String triggerContract(byte[] contractAddress, String method, String argsStr,
      Boolean isHex, long callValue, long feeLimit, byte[] ownerAddress, String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull, String[] permissionKeyString) {
    return triggerContract(contractAddress, method, argsStr, isHex, callValue, feeLimit, "0", 0,
        ownerAddress, priKey, blockingStubFull, permissionKeyString);
  }

  /**
   * constructor.
   */

  public static String triggerContract(byte[] contractAddress, String method, String argsStr,
      Boolean isHex, long callValue, long feeLimit, String tokenId, long tokenValue,
      byte[] ownerAddress, String priKey, WalletGrpc.WalletBlockingStub blockingStubFull,
      String[] permissionKeyString) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;
    if (argsStr.equalsIgnoreCase("#")) {
      logger.info("argsstr is #");
      argsStr = "";
    }

    byte[] owner = ownerAddress;
    byte[] input = Hex.decode(AbiUtil.parseMethod(method, argsStr, isHex));

    TriggerSmartContract.Builder builder = TriggerSmartContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setContractAddress(ByteString.copyFrom(contractAddress));
    builder.setData(ByteString.copyFrom(input));
    builder.setCallValue(callValue);
    builder.setTokenId(Long.parseLong(tokenId));
    builder.setCallTokenValue(tokenValue);
    TriggerSmartContract triggerContract = builder.build();

    TransactionExtention transactionExtention = blockingStubFull.triggerContract(triggerContract);
    if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
      System.out.println("RPC create call trx failed!");
      System.out.println("Code = " + transactionExtention.getResult().getCode());
      System.out
          .println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
      return null;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction.getRetCount() != 0 && transactionExtention.getConstantResult(0) != null
        && transactionExtention.getResult() != null) {
      byte[] result = transactionExtention.getConstantResult(0).toByteArray();
      System.out.println("message:" + transaction.getRet(0).getRet());
      System.out.println(
          ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));
      System.out.println("Result:" + Hex.toHexString(result));
      return null;
    }

    final TransactionExtention.Builder texBuilder = TransactionExtention.newBuilder();
    Transaction.Builder transBuilder = Transaction.newBuilder();
    Transaction.raw.Builder rawBuilder = transactionExtention.getTransaction().getRawData()
        .toBuilder();
    rawBuilder.setFeeLimit(feeLimit);
    transBuilder.setRawData(rawBuilder);
    for (int i = 0; i < transactionExtention.getTransaction().getSignatureCount(); i++) {
      ByteString s = transactionExtention.getTransaction().getSignature(i);
      transBuilder.setSignature(i, s);
    }
    for (int i = 0; i < transactionExtention.getTransaction().getRetCount(); i++) {
      Result r = transactionExtention.getTransaction().getRet(i);
      transBuilder.setRet(i, r);
    }
    texBuilder.setTransaction(transBuilder);
    texBuilder.setResult(transactionExtention.getResult());
    texBuilder.setTxid(transactionExtention.getTxid());
    transactionExtention = texBuilder.build();
    if (transactionExtention == null) {
      return null;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return null;
    }
    transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return null;
    }
    System.out.println("trigger txid = " + ByteArray
        .toHexString(Sha256Hash.hash(CommonParameter.getInstance()
            .isECKeyCryptoEngine(), transaction.getRawData().toByteArray())));
    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);

    broadcastTransaction(transaction, blockingStubFull);
    return ByteArray.toHexString(Sha256Hash.hash(CommonParameter.getInstance()
        .isECKeyCryptoEngine(), transaction.getRawData().toByteArray()));
  }

  /**
   * constructor.
   */

  public static Boolean exchangeCreate(byte[] firstTokenId, long firstTokenBalance,
      byte[] secondTokenId, long secondTokenBalance, byte[] ownerAddress, String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull, String[] permissionKeyString) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    byte[] owner = ownerAddress;

    ExchangeCreateContract.Builder builder = ExchangeCreateContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner))
        .setFirstTokenId(ByteString.copyFrom(firstTokenId)).setFirstTokenBalance(firstTokenBalance)
        .setSecondTokenId(ByteString.copyFrom(secondTokenId))
        .setSecondTokenBalance(secondTokenBalance);
    ExchangeCreateContract contract = builder.build();
    TransactionExtention transactionExtention = blockingStubFull.exchangeCreate(contract);
    if (transactionExtention == null) {
      return false;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return false;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return false;
    }
    System.out.println(
        "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));
    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);

    return broadcastTransaction(transaction, blockingStubFull);
  }

  /**
   * constructor.
   */

  public static Boolean injectExchange(long exchangeId, byte[] tokenId, long quant,
      byte[] ownerAddress, String priKey, WalletGrpc.WalletBlockingStub blockingStubFull,
      String[] permissionKeyString) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    byte[] owner = ownerAddress;

    ExchangeInjectContract.Builder builder = ExchangeInjectContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner)).setExchangeId(exchangeId)
        .setTokenId(ByteString.copyFrom(tokenId)).setQuant(quant);
    ExchangeInjectContract contract = builder.build();
    TransactionExtention transactionExtention = blockingStubFull.exchangeInject(contract);
    if (transactionExtention == null) {
      return false;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return false;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return false;
    }
    System.out.println(
        "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));
    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);

    return broadcastTransaction(transaction, blockingStubFull);
  }

  public static Optional<ExchangeList> getExchangeList(
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    ExchangeList exchangeList = blockingStubFull.listExchanges(EmptyMessage.newBuilder().build());
    return Optional.ofNullable(exchangeList);
  }

  /**
   * constructor.
   */

  public static boolean exchangeWithdraw(long exchangeId, byte[] tokenId, long quant,
      byte[] ownerAddress, String priKey, WalletGrpc.WalletBlockingStub blockingStubFull,
      String[] permissionKeyString) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;
    byte[] owner = ownerAddress;

    ExchangeWithdrawContract.Builder builder = ExchangeWithdrawContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner)).setExchangeId(exchangeId)
        .setTokenId(ByteString.copyFrom(tokenId)).setQuant(quant);
    ExchangeWithdrawContract contract = builder.build();
    TransactionExtention transactionExtention = blockingStubFull.exchangeWithdraw(contract);
    if (transactionExtention == null) {
      return false;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return false;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return false;
    }
    System.out.println(
        "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));
    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);

    return broadcastTransaction(transaction, blockingStubFull);
  }

  /**
   * constructor.
   */

  public static boolean exchangeTransaction(long exchangeId, byte[] tokenId, long quant,
      long expected, byte[] ownerAddress, String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull, String[] permissionKeyString) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;
    byte[] owner = ownerAddress;

    ExchangeTransactionContract.Builder builder = ExchangeTransactionContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner)).setExchangeId(exchangeId)
        .setTokenId(ByteString.copyFrom(tokenId)).setQuant(quant).setExpected(expected);
    ExchangeTransactionContract contract = builder.build();
    TransactionExtention transactionExtention = blockingStubFull.exchangeTransaction(contract);
    if (transactionExtention == null) {
      return false;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return false;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return false;
    }
    System.out.println(
        "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));
    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);

    return broadcastTransaction(transaction, blockingStubFull);
  }

  /**
   * constructor.
   */

  public static String deployContractWithConstantParame(String contractName, String abiString,
      String code, String constructorStr, String argsStr, String data, Long feeLimit, long value,
      long consumeUserResourcePercent, String libraryAddress, String priKey, byte[] ownerAddress,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return deployContractWithConstantParame(contractName, abiString, code, constructorStr, argsStr,
        data, feeLimit, value, consumeUserResourcePercent, 1000L, "0", 0L, libraryAddress, priKey,
        ownerAddress, blockingStubFull);
  }

  /**
   * constructor.
   */

  public static String deployContractWithConstantParame(String contractName, String abiString,
      String code, String constructorStr, String argsStr, String data, Long feeLimit, long value,
      long consumeUserResourcePercent, long originEnergyLimit, String tokenId, long tokenValue,
      String libraryAddress, String priKey, byte[] ownerAddress,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    SmartContract.ABI abi = jsonStr2Abi(abiString);
    if (abi == null) {
      logger.error("abi is null");
      return null;
    }

    code += Hex.toHexString(AbiUtil.encodeInput(constructorStr, argsStr));
    byte[] owner = ownerAddress;
    SmartContract.Builder builder = SmartContract.newBuilder();
    builder.setName(contractName);
    builder.setOriginAddress(ByteString.copyFrom(owner));
    builder.setAbi(abi);
    builder.setConsumeUserResourcePercent(consumeUserResourcePercent);
    builder.setOriginEnergyLimit(originEnergyLimit);

    if (value != 0) {

      builder.setCallValue(value);
    }

    byte[] byteCode;
    if (null != libraryAddress) {
      byteCode = replaceLibraryAddress(code, libraryAddress);
    } else {
      byteCode = Hex.decode(code);
    }
    builder.setBytecode(ByteString.copyFrom(byteCode));

    Builder contractBuilder = CreateSmartContract.newBuilder();
    contractBuilder.setOwnerAddress(ByteString.copyFrom(owner));
    contractBuilder.setCallTokenValue(tokenValue);
    contractBuilder.setTokenId(Long.parseLong(tokenId));
    CreateSmartContract contractDeployContract = contractBuilder.setNewContract(builder.build())
        .build();

    TransactionExtention transactionExtention = blockingStubFull
        .deployContract(contractDeployContract);
    if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
      System.out.println("RPC create trx failed!");
      if (transactionExtention != null) {
        System.out.println("Code = " + transactionExtention.getResult().getCode());
        System.out
            .println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
      }
      return null;
    }

    final TransactionExtention.Builder texBuilder = TransactionExtention.newBuilder();
    Transaction.Builder transBuilder = Transaction.newBuilder();
    Transaction.raw.Builder rawBuilder = transactionExtention.getTransaction().getRawData()
        .toBuilder();
    rawBuilder.setFeeLimit(feeLimit);
    transBuilder.setRawData(rawBuilder);
    for (int i = 0; i < transactionExtention.getTransaction().getSignatureCount(); i++) {
      ByteString s = transactionExtention.getTransaction().getSignature(i);
      transBuilder.setSignature(i, s);
    }
    for (int i = 0; i < transactionExtention.getTransaction().getRetCount(); i++) {
      Result r = transactionExtention.getTransaction().getRet(i);
      transBuilder.setRet(i, r);
    }
    texBuilder.setTransaction(transBuilder);
    texBuilder.setResult(transactionExtention.getResult());
    texBuilder.setTxid(transactionExtention.getTxid());
    transactionExtention = texBuilder.build();

    if (transactionExtention == null) {
      return null;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return null;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return null;
    }
    transaction = signTransaction(ecKey, transaction);
    System.out.println(
        "txid = " + ByteArray.toHexString(Sha256Hash.hash(CommonParameter.getInstance()
            .isECKeyCryptoEngine(), transaction.getRawData().toByteArray())));
    byte[] contractAddress = PublicMethed.generateContractAddress(transaction, owner);
    System.out.println(
        "Your smart contract address will be: " + WalletClient.encode58Check(contractAddress));
    Return response = broadcastTransaction1(transaction, blockingStubFull);
    if (response.getResult() == false) {
      return null;
    } else {
      //logger.info("brodacast succesfully");
      return ByteArray.toHexString(Sha256Hash.hash(CommonParameter.getInstance()
          .isECKeyCryptoEngine(), transaction.getRawData().toByteArray()));
    }
  }

  /**
   * constructor.
   */

  public static Boolean freezeBalanceForReceiver(byte[] addRess, long freezeBalance,
      long freezeDuration, int resourceCode, ByteString receiverAddressBytes, String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull, String[] permissionKeyString) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    byte[] address = addRess;
    long frozenBalance = freezeBalance;
    long frozenDuration = freezeDuration;
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    FreezeBalanceContract.Builder builder = FreezeBalanceContract.newBuilder();
    ByteString byteAddreess = ByteString.copyFrom(address);

    builder.setOwnerAddress(byteAddreess).setFrozenBalance(frozenBalance)
        .setFrozenDuration(frozenDuration).setResourceValue(resourceCode);
    builder.setReceiverAddress(receiverAddressBytes);
    FreezeBalanceContract contract = builder.build();
    Transaction transaction = blockingStubFull.freezeBalance(contract);

    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction = null");
      return false;
    }
    transaction = TransactionUtils.setTimestamp(transaction);
    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);

    return broadcastTransaction(transaction, blockingStubFull);
  }

  private static Permission json2Permission(JSONObject json) {
    Permission.Builder permissionBuilder = Permission.newBuilder();
    if (json.containsKey("type")) {
      int type = json.getInteger("type");
      permissionBuilder.setTypeValue(type);
    }
    if (json.containsKey("permission_name")) {
      String permissionName = json.getString("permission_name");
      permissionBuilder.setPermissionName(permissionName);
    }
    if (json.containsKey("threshold")) {
      //      long threshold = json.getLong("threshold");
      long threshold = Long.parseLong(json.getString("threshold"));
      permissionBuilder.setThreshold(threshold);
    }
    if (json.containsKey("parent_id")) {
      int parentId = json.getInteger("parent_id");
      permissionBuilder.setParentId(parentId);
    }
    if (json.containsKey("operations")) {
      byte[] operations = ByteArray.fromHexString(json.getString("operations"));
      permissionBuilder.setOperations(ByteString.copyFrom(operations));
    }
    if (json.containsKey("keys")) {
      JSONArray keys = json.getJSONArray("keys");
      List<Key> keyList = new ArrayList<>();
      for (int i = 0; i < keys.size(); i++) {
        Key.Builder keyBuilder = Key.newBuilder();
        JSONObject key = keys.getJSONObject(i);
        String address = key.getString("address");
        //        long weight = key.getLong("weight");
        long weight = Long.parseLong(key.getString("weight"));
        keyBuilder.setAddress(ByteString.copyFrom(WalletClient.decodeFromBase58Check(address)));
        keyBuilder.setWeight(weight);
        keyList.add(keyBuilder.build());
      }
      permissionBuilder.addAllKeys(keyList);
    }
    return permissionBuilder.build();
  }

  /**
   * constructor.
   */

  public static boolean accountPermissionUpdate(String permissionJson, byte[] owner, String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull, String[] priKeys) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;

    AccountPermissionUpdateContract.Builder builder = AccountPermissionUpdateContract.newBuilder();

    JSONObject permissions = JSONObject.parseObject(permissionJson);
    JSONObject ownersPermission = permissions.getJSONObject("owner_permission");
    JSONObject witnesssPermission = permissions.getJSONObject("witness_permission");
    JSONArray activesPermissions = permissions.getJSONArray("active_permissions");

    if (ownersPermission != null) {
      Permission ownerPermission = json2Permission(ownersPermission);
      builder.setOwner(ownerPermission);
    }
    if (witnesssPermission != null) {
      Permission witnessPermission = json2Permission(witnesssPermission);
      builder.setWitness(witnessPermission);
    }
    if (activesPermissions != null) {
      List<Permission> activePermissionList = new ArrayList<>();
      for (int j = 0; j < activesPermissions.size(); j++) {
        JSONObject permission = activesPermissions.getJSONObject(j);
        activePermissionList.add(json2Permission(permission));
      }
      builder.addAllActives(activePermissionList);
    }
    builder.setOwnerAddress(ByteString.copyFrom(owner));

    AccountPermissionUpdateContract contract = builder.build();

    TransactionExtention transactionExtention = blockingStubFull.accountPermissionUpdate(contract);
    if (transactionExtention == null) {
      return false;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return false;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return false;
    }

    transaction = signTransaction(transaction, blockingStubFull, priKeys);
    System.out.println("trigger txid = " + ByteArray
        .toHexString(Sha256Hash.hash(CommonParameter.getInstance()
            .isECKeyCryptoEngine(), transaction.getRawData().toByteArray())));
    Return response = broadcastTransaction1(transaction, blockingStubFull);
    return response.getResult();
  }

  /**
   * constructor.
   */
  public static String accountPermissionUpdateForTransactionId(String permissionJson, byte[] owner,
      String priKey, WalletGrpc.WalletBlockingStub blockingStubFull, String[] priKeys) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;

    AccountPermissionUpdateContract.Builder builder = AccountPermissionUpdateContract.newBuilder();

    JSONObject permissions = JSONObject.parseObject(permissionJson);
    JSONObject ownerpermission = permissions.getJSONObject("owner_permission");
    JSONObject witnesspermission = permissions.getJSONObject("witness_permission");
    JSONArray activepermissions = permissions.getJSONArray("active_permissions");

    if (ownerpermission != null) {
      Permission ownerPermission = json2Permission(ownerpermission);
      builder.setOwner(ownerPermission);
    }
    if (witnesspermission != null) {
      Permission witnessPermission = json2Permission(witnesspermission);
      builder.setWitness(witnessPermission);
    }
    if (activepermissions != null) {
      List<Permission> activePermissionList = new ArrayList<>();
      for (int j = 0; j < activepermissions.size(); j++) {
        JSONObject permission = activepermissions.getJSONObject(j);
        activePermissionList.add(json2Permission(permission));
      }
      builder.addAllActives(activePermissionList);
    }
    builder.setOwnerAddress(ByteString.copyFrom(owner));

    AccountPermissionUpdateContract contract = builder.build();

    TransactionExtention transactionExtention = blockingStubFull.accountPermissionUpdate(contract);
    if (transactionExtention == null) {
      return null;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return null;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return null;
    }
    System.out.println(
        "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));

    transaction = signTransaction(transaction, blockingStubFull, priKeys);
    Return response = broadcastTransaction1(transaction, blockingStubFull);
    if (response.getResult() == false) {
      return null;
    } else {
      return ByteArray.toHexString(Sha256Hash.hash(CommonParameter.getInstance()
          .isECKeyCryptoEngine(), transaction.getRawData().toByteArray()));
    }
  }


  /**
   * constructor.
   */
  public static String accountPermissionUpdateForTransactionId1(String permissionJson, byte[] owner,
      String priKey, WalletGrpc.WalletBlockingStub blockingStubFull, int permissionId,
      String[] priKeys) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;

    AccountPermissionUpdateContract.Builder builder = AccountPermissionUpdateContract.newBuilder();

    JSONObject permissions = JSONObject.parseObject(permissionJson);
    JSONObject ownerpermission = permissions.getJSONObject("owner_permission");
    JSONObject witnesspermission = permissions.getJSONObject("witness_permission");
    JSONArray activepermissions = permissions.getJSONArray("active_permissions");

    if (ownerpermission != null) {
      Permission ownerPermission = json2Permission(ownerpermission);
      builder.setOwner(ownerPermission);
    }
    if (witnesspermission != null) {
      Permission witnessPermission = json2Permission(witnesspermission);
      builder.setWitness(witnessPermission);
    }
    if (activepermissions != null) {
      List<Permission> activePermissionList = new ArrayList<>();
      for (int j = 0; j < activepermissions.size(); j++) {
        JSONObject permission = activepermissions.getJSONObject(j);
        activePermissionList.add(json2Permission(permission));
      }
      builder.addAllActives(activePermissionList);
    }
    builder.setOwnerAddress(ByteString.copyFrom(owner));

    AccountPermissionUpdateContract contract = builder.build();

    TransactionExtention transactionExtention = blockingStubFull.accountPermissionUpdate(contract);
    if (transactionExtention == null) {
      return null;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return null;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return null;
    }
    try {
      transaction = setPermissionId(transaction, permissionId);
    } catch (CancelException e) {
      e.printStackTrace();
    }
    System.out.println(
        "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));

    transaction = signTransaction(transaction, blockingStubFull, priKeys);
    Return response = broadcastTransaction1(transaction, blockingStubFull);
    if (response.getResult() == false) {
      return null;
    } else {
      return ByteArray.toHexString(Sha256Hash.hash(CommonParameter.getInstance()
          .isECKeyCryptoEngine(), transaction.getRawData().toByteArray()));
    }
  }

  /**
   * constructor.
   */
  public static Transaction addTransactionSign(Transaction transaction, String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;

    Transaction.Builder transactionBuilderSigned = transaction.toBuilder();
    byte[] hash = Sha256Hash.hash(CommonParameter.getInstance()
        .isECKeyCryptoEngine(), transaction.getRawData().toByteArray());

    ECDSASignature signature = ecKey.sign(hash);
    ByteString bsSign = ByteString.copyFrom(signature.toByteArray());
    transactionBuilderSigned.addSignature(bsSign);
    transaction = transactionBuilderSigned.build();
    return transaction;
  }

  /**
   * constructor.
   */
  public static Boolean voteWitness(HashMap<String, String> witness, byte[] addRess, String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull, String[] permissionKeyString) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);

    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;

    VoteWitnessContract.Builder builder = VoteWitnessContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(addRess));
    for (String addressBase58 : witness.keySet()) {
      String value = witness.get(addressBase58);
      final long count = Long.parseLong(value);
      VoteWitnessContract.Vote.Builder voteBuilder = VoteWitnessContract.Vote.newBuilder();
      byte[] address = WalletClient.decodeFromBase58Check(addressBase58);
      if (address == null) {
        continue;
      }
      voteBuilder.setVoteAddress(ByteString.copyFrom(address));
      voteBuilder.setVoteCount(count);
      builder.addVotes(voteBuilder.build());
    }

    VoteWitnessContract contract = builder.build();

    Transaction transaction = blockingStubFull.voteWitnessAccount(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction == null");
      return false;
    }
    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);

    return broadcastTransaction(transaction, blockingStubFull);
  }

  /**
   * constructor.
   */
  public static void printPermissionList(List<Permission> permissionList) {
    String result = "\n";
    result += "[";
    result += "\n";
    int i = 0;
    for (Permission permission : permissionList) {
      result += "permission " + i + " :::";
      result += "\n";
      result += "{";
      result += "\n";
      result += printPermission(permission);
      result += "\n";
      result += "}";
      result += "\n";
      i++;
    }
    result += "]";
    System.out.println(result);
  }

  /**
   * constructor.
   */
  public static String printPermission(Permission permission) {
    StringBuffer result = new StringBuffer();
    result.append("permission_type: ");
    result.append(permission.getType());
    result.append("\n");
    result.append("permission_id: ");
    result.append(permission.getId());
    result.append("\n");
    result.append("permission_name: ");
    result.append(permission.getPermissionName());
    result.append("\n");
    result.append("threshold: ");
    result.append(permission.getThreshold());
    result.append("\n");
    result.append("parent_id: ");
    result.append(permission.getParentId());
    result.append("\n");
    result.append("operations: ");
    result.append(ByteArray.toHexString(permission.getOperations().toByteArray()));
    result.append("\n");
    if (permission.getKeysCount() > 0) {
      result.append("keys:");
      result.append("\n");
      result.append("[");
      result.append("\n");
      for (Key key : permission.getKeysList()) {
        result.append(printKey(key));
      }
      result.append("]");
      result.append("\n");
    }
    return result.toString();
  }

  /**
   * constructor.
   */
  public static String printKey(Key key) {
    StringBuffer result = new StringBuffer();
    result.append("address: ");
    result.append(encode58Check(key.getAddress().toByteArray()));
    result.append("\n");
    result.append("weight: ");
    result.append(key.getWeight());
    result.append("\n");
    return result.toString();
  }

  /**
   * constructor.
   */
  public static String encode58Check(byte[] input) {
    byte[] hash0 = Sha256Hash.hash(CommonParameter.getInstance()
        .isECKeyCryptoEngine(), input);
    byte[] hash1 = Sha256Hash.hash(CommonParameter.getInstance()
        .isECKeyCryptoEngine(), hash0);
    byte[] inputCheck = new byte[input.length + 4];
    System.arraycopy(input, 0, inputCheck, 0, input.length);
    System.arraycopy(hash1, 0, inputCheck, input.length, 4);
    return Base58.encode(inputCheck);
  }

  /**
   * constructor.
   */
  public static Transaction sendcoinWithPermissionIdNotSign(byte[] to, long amount, byte[] owner,
      int permissionId, String priKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    //String priKey = testKey002;
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;
    //Protocol.Account search = queryAccount(priKey, blockingStubFull);

    TransferContract.Builder builder = TransferContract.newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsOwner = ByteString.copyFrom(owner);
    builder.setToAddress(bsTo);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);

    TransferContract contract = builder.build();
    TransactionExtention transactionExtention = blockingStubFull.createTransaction2(contract);

    Transaction transaction = transactionExtention.getTransaction();
    raw rawData = transaction.getRawData();
    Transaction.Contract contract1 = transactionExtention.getTransaction().getRawData()
        .getContractList().get(0);
    contract1 = contract1.toBuilder().setPermissionId(permissionId).build();
    rawData = rawData.toBuilder().clearContract().addContract(contract1).build();
    transaction = transaction.toBuilder().setRawData(rawData).build();

    return transaction;

  }

  /**
   * constructor.
   */
  public static TransactionSignWeight getTransactionSignWeight(Transaction transaction,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return blockingStubFull.getTransactionSignWeight(transaction);
  }

  /**
   * constructor.
   */
  public static Return broadcastTransaction1(Transaction transaction,
      WalletGrpc.WalletBlockingStub blockingStubFull) {

    return PublicMethed.broadcastTransaction(transaction, blockingStubFull);
  }

  /**
   * constructor.
   */
  public static boolean accountPermissionUpdateWithPermissionId(String permissionJson, byte[] owner,
      String priKey, WalletGrpc.WalletBlockingStub blockingStubFull, int permissionId,
      String[] priKeys) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;

    AccountPermissionUpdateContract.Builder builder = AccountPermissionUpdateContract.newBuilder();

    JSONObject permissions = JSONObject.parseObject(permissionJson);
    JSONObject ownersPermission = permissions.getJSONObject("owner_permission");
    JSONObject witnesssPermission = permissions.getJSONObject("witness_permission");
    JSONArray activesPermissions = permissions.getJSONArray("active_permissions");

    if (ownersPermission != null) {
      Permission ownerPermission = json2Permission(ownersPermission);
      builder.setOwner(ownerPermission);
    }
    if (witnesssPermission != null) {
      Permission witnessPermission = json2Permission(witnesssPermission);
      builder.setWitness(witnessPermission);
    }
    if (activesPermissions != null) {
      List<Permission> activePermissionList = new ArrayList<>();
      for (int j = 0; j < activesPermissions.size(); j++) {
        JSONObject permission = activesPermissions.getJSONObject(j);
        activePermissionList.add(json2Permission(permission));
      }
      builder.addAllActives(activePermissionList);
    }
    builder.setOwnerAddress(ByteString.copyFrom(owner));

    AccountPermissionUpdateContract contract = builder.build();

    TransactionExtention transactionExtention = blockingStubFull.accountPermissionUpdate(contract);

    if (transactionExtention == null) {
      return false;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return false;
    }

    Transaction transaction = transactionExtention.getTransaction();
    raw rawData = transaction.getRawData();
    Transaction.Contract contract1 = transactionExtention.getTransaction().getRawData()
        .getContractList().get(0);
    contract1 = contract1.toBuilder().setPermissionId(permissionId).build();
    rawData = rawData.toBuilder().clearContract().addContract(contract1).build();
    transaction = transaction.toBuilder().setRawData(rawData).build();

    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return false;
    }
    System.out.println(
        "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));

    transaction = signTransaction(transaction, blockingStubFull, priKeys);
    Return response = broadcastTransaction1(transaction, blockingStubFull);
    return response.getResult();
  }

  /**
   * constructor.
   */
  public static Transaction accountPermissionUpdateWithoutSign(String permissionJson, byte[] owner,
      String priKey, WalletGrpc.WalletBlockingStub blockingStubFull, String[] priKeys) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;

    AccountPermissionUpdateContract.Builder builder = AccountPermissionUpdateContract.newBuilder();

    JSONObject permissions = JSONObject.parseObject(permissionJson);
    JSONObject ownersPermission = permissions.getJSONObject("owner_permission");
    JSONObject witnesssPermission = permissions.getJSONObject("witness_permission");
    JSONArray activesPermissions = permissions.getJSONArray("active_permissions");

    if (ownersPermission != null) {
      Permission ownerPermission = json2Permission(ownersPermission);
      builder.setOwner(ownerPermission);
    }
    if (witnesssPermission != null) {
      Permission witnessPermission = json2Permission(witnesssPermission);
      builder.setWitness(witnessPermission);
    }
    if (activesPermissions != null) {
      List<Permission> activePermissionList = new ArrayList<>();
      for (int j = 0; j < activesPermissions.size(); j++) {
        JSONObject permission = activesPermissions.getJSONObject(j);
        activePermissionList.add(json2Permission(permission));
      }
      builder.addAllActives(activePermissionList);
    }
    builder.setOwnerAddress(ByteString.copyFrom(owner));

    AccountPermissionUpdateContract contract = builder.build();

    TransactionExtention transactionExtention = blockingStubFull.accountPermissionUpdate(contract);
    if (transactionExtention == null) {
      return null;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return null;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return transaction;
    }
    System.out.println(
        "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));
    return transaction;
  }

  /**
   * constructor.
   */
  public static Transaction addTransactionSignWithPermissionId(Transaction transaction,
      String priKey, int permissionId, WalletGrpc.WalletBlockingStub blockingStubFull) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    //transaction = setPermissionId(transaction, permissionId);
    Transaction.raw.Builder raw = transaction.getRawData().toBuilder();
    Transaction.Contract.Builder contract = raw.getContract(0).toBuilder()
        .setPermissionId(permissionId);
    raw.clearContract();
    raw.addContract(contract);
    transaction = transaction.toBuilder().setRawData(raw).build();

    Transaction.Builder transactionBuilderSigned = transaction.toBuilder();
    byte[] hash = Sha256Hash.hash(CommonParameter.getInstance()
        .isECKeyCryptoEngine(), transaction.getRawData().toByteArray());
    ECKey ecKey = temKey;
    ECDSASignature signature = ecKey.sign(hash);
    ByteString bsSign = ByteString.copyFrom(signature.toByteArray());
    transactionBuilderSigned.addSignature(bsSign);
    transaction = transactionBuilderSigned.build();
    return transaction;
  }

  /**
   * constructor.
   */
  public static Transaction setPermissionId(Transaction transaction, int permissionId)
      throws CancelException {
    if (transaction.getSignatureCount() != 0
        || transaction.getRawData().getContract(0).getPermissionId() != 0) {
      return transaction;
    }
    if (permissionId < 0) {
      throw new CancelException("User cancelled");
    }
    if (permissionId != 0) {
      Transaction.raw.Builder raw = transaction.getRawData().toBuilder();
      Transaction.Contract.Builder contract = raw.getContract(0).toBuilder()
          .setPermissionId(permissionId);
      raw.clearContract();
      raw.addContract(contract);
      transaction = transaction.toBuilder().setRawData(raw).build();
    }
    return transaction;
  }

  /**
   * constructor.
   */
  public static int getActivePermissionKeyCount(List<Permission> permissionList) {
    int permissionCount = 0;
    for (Permission permission : permissionList) {
      permissionCount += permission.getKeysCount();
    }
    return permissionCount;
  }

  /**
   * constructor.
   */
  public static Boolean sendcoinWithPermissionId(byte[] to, long amount, byte[] owner,
      int permissionId, String priKey, WalletGrpc.WalletBlockingStub blockingStubFull,
      String[] permissionKeyString) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    //String priKey = testKey002;
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;
    //Protocol.Account search = queryAccount(priKey, blockingStubFull);

    TransferContract.Builder builder = TransferContract.newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsOwner = ByteString.copyFrom(owner);
    builder.setToAddress(bsTo);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);

    TransferContract contract = builder.build();
    TransactionExtention transactionExtention = blockingStubFull.createTransaction2(contract);

    Transaction transaction = transactionExtention.getTransaction();
    raw rawData = transaction.getRawData();
    Transaction.Contract contract1 = transactionExtention.getTransaction().getRawData()
        .getContractList().get(0);
    contract1 = contract1.toBuilder().setPermissionId(permissionId).build();
    rawData = rawData.toBuilder().clearContract().addContract(contract1).build();
    transaction = transaction.toBuilder().setRawData(rawData).build();
    transactionExtention = transactionExtention.toBuilder().setTransaction(transaction).build();

    if (transactionExtention == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction ==null");
      return null;
    }
    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);

    return broadcastTransaction(transaction, blockingStubFull);

  }

  /**
   * constructor.
   */
  public static void recoverWitnessPermission(String ownerKey, List<String> ownerPermissionKeys,
      WalletGrpc.WalletBlockingStub blockingStubFull) {

    PublicMethed.printAddress(ownerKey);
    byte[] ownerAddress = new WalletClient(ownerKey).getAddress();

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1}]},"
            + "\"witness_permission\":{\"type\":1,\"permission_name\":\"witness\","
            + "\"threshold\":1,\"keys\":[" + "{\"address\":\"" + PublicMethed
            .getAddressString(ownerKey) + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":1}" + "]}]}";

    Assert.assertTrue(
        accountPermissionUpdate(accountPermissionJson, ownerAddress, ownerKey, blockingStubFull,
            ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Assert.assertEquals(1, getActivePermissionKeyCount(
        PublicMethed.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(1,
        PublicMethed.queryAccount(ownerAddress, blockingStubFull).getOwnerPermission()
            .getKeysCount());

    Assert.assertEquals(1,
        PublicMethed.queryAccount(ownerAddress, blockingStubFull).getWitnessPermission()
            .getKeysCount());
  }

  /**
   * constructor.
   */
  public static String getOperations(Integer[] ints) {
    List<Integer> list = new ArrayList<>(Arrays.asList(ints));
    byte[] operations = new byte[32];
    list.forEach(e -> {
      operations[e / 8] |= (1 << e % 8);
    });

    System.out.println(ByteArray.toHexString(operations));
    return ByteArray.toHexString(operations);
  }

  /**
   * constructor.
   */
  public static GrpcAPI.Return accountPermissionUpdateForResponse(String permissionJson,
      byte[] owner, String priKey, WalletGrpc.WalletBlockingStub blockingStubFull,
      String[] priKeys) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;

    AccountPermissionUpdateContract.Builder builder = AccountPermissionUpdateContract.newBuilder();

    JSONObject permissions = JSONObject.parseObject(permissionJson);
    JSONObject ownersPermission = permissions.getJSONObject("owner_permission");
    JSONObject witnesssPermission = permissions.getJSONObject("witness_permission");
    JSONArray activesPermissions = permissions.getJSONArray("active_permissions");

    if (ownersPermission != null) {
      Permission ownerPermission = json2Permission(ownersPermission);
      builder.setOwner(ownerPermission);
    }
    if (witnesssPermission != null) {
      Permission witnessPermission = json2Permission(witnesssPermission);
      builder.setWitness(witnessPermission);
    }
    if (activesPermissions != null) {
      List<Permission> activePermissionList = new ArrayList<>();
      for (int j = 0; j < activesPermissions.size(); j++) {
        JSONObject permission = activesPermissions.getJSONObject(j);
        activePermissionList.add(json2Permission(permission));
      }
      builder.addAllActives(activePermissionList);
    }
    builder.setOwnerAddress(ByteString.copyFrom(owner));

    AccountPermissionUpdateContract contract = builder.build();

    TransactionExtention transactionExtention = blockingStubFull.accountPermissionUpdate(contract);
    if (transactionExtention == null) {
      return null;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return ret;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return ret;
    }
    System.out.println(
        "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));

    transaction = signTransaction(transaction, blockingStubFull, priKeys);
    Return response = broadcastTransaction1(transaction, blockingStubFull);
    return response;
  }

  /**
   * constructor.
   */
  public static void recoverAccountPermission(String ownerKey, List<String> ownerPermissionKeys,
      WalletGrpc.WalletBlockingStub blockingStubFull) {

    PublicMethed.printAddress(ownerKey);
    byte[] ownerAddress = new WalletClient(ownerKey).getAddress();

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[" + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":1}" + "]}]}";

    Assert.assertTrue(
        accountPermissionUpdate(accountPermissionJson, ownerAddress, ownerKey, blockingStubFull,
            ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Assert.assertEquals(1, getActivePermissionKeyCount(
        PublicMethed.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(1,
        PublicMethed.queryAccount(ownerAddress, blockingStubFull).getOwnerPermission()
            .getKeysCount());
  }

  /**
   * constructor.
   */
  public static Transaction sendcoin2(byte[] to, long amount, byte[] owner, String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    //String priKey = testKey002;
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;
    //Protocol.Account search = queryAccount(priKey, blockingStubFull);

    TransferContract.Builder builder = TransferContract.newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsOwner = ByteString.copyFrom(owner);
    builder.setToAddress(bsTo);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);

    TransferContract contract = builder.build();
    Transaction transaction = blockingStubFull.createTransaction(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction ==null");
      return null;
    }
    return transaction;

  }

  /**
   * constructor.
   */
  public static Protocol.Transaction createFakeTransaction(byte[] toAddrss, Long amount,
      byte[] fromAddress) {

    TransferContract contract = TransferContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(fromAddress))
        .setToAddress(ByteString.copyFrom(toAddrss)).setAmount(amount).build();
    Protocol.Transaction transaction = createTransaction(contract, ContractType.TransferContract);

    return transaction;
  }

  /**
   * constructor.
   */
  private static Transaction setReference(Transaction transaction, long blockNum,
      byte[] blockHash) {
    byte[] refBlockNum = ByteArray.fromLong(blockNum);
    Transaction.raw rawData = transaction.getRawData().toBuilder()
        .setRefBlockHash(ByteString.copyFrom(blockHash))
        .setRefBlockBytes(ByteString.copyFrom(refBlockNum)).build();
    return transaction.toBuilder().setRawData(rawData).build();
  }

  /**
   * constructor.
   */
  public static Transaction setExpiration(Transaction transaction, long expiration) {
    Transaction.raw rawData = transaction.getRawData().toBuilder().setExpiration(expiration)
        .build();
    return transaction.toBuilder().setRawData(rawData).build();
  }

  /**
   * constructor.
   */
  public static Transaction createTransaction(com.google.protobuf.Message message,
      ContractType contractType) {
    Transaction.raw.Builder transactionBuilder = Transaction.raw.newBuilder().addContract(
        Transaction.Contract.newBuilder().setType(contractType).setParameter(Any.pack(message))
            .build());

    Transaction transaction = Transaction.newBuilder().setRawData(transactionBuilder.build())
        .build();

    long time = System.currentTimeMillis();
    AtomicLong count = new AtomicLong();
    long geTime = count.incrementAndGet() + time;
    String ref = "" + geTime;

    transaction = setReference(transaction, geTime, ByteArray.fromString(ref));

    transaction = setExpiration(transaction, geTime);

    return transaction;
  }

  /**
   * constructor.
   */

  public static String triggerContractWithPermissionId(byte[] contractAddress, String method,
      String argsStr, Boolean isHex, long callValue, long feeLimit, String tokenId, long tokenValue,
      byte[] ownerAddress, String priKey, WalletGrpc.WalletBlockingStub blockingStubFull,
      String[] permissionKeyString, int permissionId) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;
    if (argsStr.equalsIgnoreCase("#")) {
      logger.info("argsstr is #");
      argsStr = "";
    }

    byte[] owner = ownerAddress;
    byte[] input = Hex.decode(AbiUtil.parseMethod(method, argsStr, isHex));

    TriggerSmartContract.Builder builder = TriggerSmartContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setContractAddress(ByteString.copyFrom(contractAddress));
    builder.setData(ByteString.copyFrom(input));
    builder.setCallValue(callValue);
    builder.setTokenId(Long.parseLong(tokenId));
    builder.setCallTokenValue(tokenValue);
    TriggerSmartContract triggerContract = builder.build();

    TransactionExtention transactionExtention = blockingStubFull.triggerContract(triggerContract);
    if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
      System.out.println("RPC create call trx failed!");
      System.out.println("Code = " + transactionExtention.getResult().getCode());
      System.out
          .println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
      return null;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction.getRetCount() != 0 && transactionExtention.getConstantResult(0) != null
        && transactionExtention.getResult() != null) {
      byte[] result = transactionExtention.getConstantResult(0).toByteArray();
      System.out.println("message:" + transaction.getRet(0).getRet());
      System.out.println(
          ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));
      System.out.println("Result:" + Hex.toHexString(result));
      return null;
    }

    final TransactionExtention.Builder texBuilder = TransactionExtention.newBuilder();
    Transaction.Builder transBuilder = Transaction.newBuilder();
    Transaction.raw.Builder rawBuilder = transactionExtention.getTransaction().getRawData()
        .toBuilder();
    rawBuilder.setFeeLimit(feeLimit);
    transBuilder.setRawData(rawBuilder);
    for (int i = 0; i < transactionExtention.getTransaction().getSignatureCount(); i++) {
      ByteString s = transactionExtention.getTransaction().getSignature(i);
      transBuilder.setSignature(i, s);
    }
    for (int i = 0; i < transactionExtention.getTransaction().getRetCount(); i++) {
      Result r = transactionExtention.getTransaction().getRet(i);
      transBuilder.setRet(i, r);
    }
    texBuilder.setTransaction(transBuilder);
    texBuilder.setResult(transactionExtention.getResult());
    texBuilder.setTxid(transactionExtention.getTxid());
    transactionExtention = texBuilder.build();
    if (transactionExtention == null) {
      return null;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return null;
    }
    transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return null;
    }

    try {
      transaction = setPermissionId(transaction, permissionId);
    } catch (CancelException e) {
      e.printStackTrace();
    }

    System.out.println("trigger txid = " + ByteArray
        .toHexString(Sha256Hash.hash(CommonParameter.getInstance()
            .isECKeyCryptoEngine(), transaction.getRawData().toByteArray())));
    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);

    broadcastTransaction(transaction, blockingStubFull);
    return ByteArray.toHexString(Sha256Hash.hash(CommonParameter.getInstance()
        .isECKeyCryptoEngine(), transaction.getRawData().toByteArray()));
  }

  /**
   * constructor.
   */

  public static byte[] generateContractAddress(Transaction trx, byte[] owneraddress) {

    // get owner address
    // this address should be as same as the onweraddress in trx, DONNOT modify it
    byte[] ownerAddress = owneraddress;

    // get tx hash
    byte[] txRawDataHash = Sha256Hash.of(CommonParameter.getInstance()
        .isECKeyCryptoEngine(), trx.getRawData().toByteArray()).getBytes();

    // combine
    byte[] combined = new byte[txRawDataHash.length + ownerAddress.length];
    System.arraycopy(txRawDataHash, 0, combined, 0, txRawDataHash.length);
    System.arraycopy(ownerAddress, 0, combined, txRawDataHash.length, ownerAddress.length);

    return sha3omit12(combined);

  }

  /**
   * constructor.
   */

  public static byte[] deployContractWithPermissionId(String contractName, String abiString,
      String code, String data, Long feeLimit, long value, long consumeUserResourcePercent,
      long originEnergyLimit, String tokenId, long tokenValue, String libraryAddress, String priKey,
      byte[] ownerAddress, WalletGrpc.WalletBlockingStub blockingStubFull,
      String[] permissionKeyString, int permissionId) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    byte[] owner = ownerAddress;
    SmartContract.ABI abi = jsonStr2Abi(abiString);
    if (abi == null) {
      logger.error("abi is null");
      return null;
    }
    //byte[] codeBytes = Hex.decode(code);
    SmartContract.Builder builder = SmartContract.newBuilder();
    builder.setName(contractName);
    builder.setOriginAddress(ByteString.copyFrom(owner));
    builder.setAbi(abi);
    builder.setConsumeUserResourcePercent(consumeUserResourcePercent);
    builder.setOriginEnergyLimit(originEnergyLimit);

    if (value != 0) {

      builder.setCallValue(value);
    }

    byte[] byteCode;
    if (null != libraryAddress) {
      byteCode = replaceLibraryAddress(code, libraryAddress);
    } else {
      byteCode = Hex.decode(code);
    }
    builder.setBytecode(ByteString.copyFrom(byteCode));

    Builder contractBuilder = CreateSmartContract.newBuilder();
    contractBuilder.setOwnerAddress(ByteString.copyFrom(owner));
    contractBuilder.setCallTokenValue(tokenValue);
    contractBuilder.setTokenId(Long.parseLong(tokenId));
    CreateSmartContract contractDeployContract = contractBuilder.setNewContract(builder.build())
        .build();

    TransactionExtention transactionExtention = blockingStubFull
        .deployContract(contractDeployContract);
    if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
      System.out.println("RPC create trx failed!");
      if (transactionExtention != null) {
        System.out.println("Code = " + transactionExtention.getResult().getCode());
        System.out
            .println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
      }
      return null;
    }

    final TransactionExtention.Builder texBuilder = TransactionExtention.newBuilder();
    Transaction.Builder transBuilder = Transaction.newBuilder();
    Transaction.raw.Builder rawBuilder = transactionExtention.getTransaction().getRawData()
        .toBuilder();
    rawBuilder.setFeeLimit(feeLimit);
    transBuilder.setRawData(rawBuilder);
    for (int i = 0; i < transactionExtention.getTransaction().getSignatureCount(); i++) {
      ByteString s = transactionExtention.getTransaction().getSignature(i);
      transBuilder.setSignature(i, s);
    }
    for (int i = 0; i < transactionExtention.getTransaction().getRetCount(); i++) {
      Result r = transactionExtention.getTransaction().getRet(i);
      transBuilder.setRet(i, r);
    }
    texBuilder.setTransaction(transBuilder);
    texBuilder.setResult(transactionExtention.getResult());
    texBuilder.setTxid(transactionExtention.getTxid());
    transactionExtention = texBuilder.build();

    byte[] contractAddress = generateContractAddress(transactionExtention.getTransaction(), owner);
    System.out.println(
        "Your smart contract address will be: " + WalletClient.encode58Check(contractAddress));
    if (transactionExtention == null) {
      return null;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return null;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return null;
    }

    try {
      transaction = setPermissionId(transaction, permissionId);
    } catch (CancelException e) {
      e.printStackTrace();
    }

    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);

    System.out.println(
        "txid = " + ByteArray.toHexString(Sha256Hash.hash(CommonParameter.getInstance()
            .isECKeyCryptoEngine(), transaction.getRawData().toByteArray())));
    contractAddress = generateContractAddress(transaction, owner);
    System.out.println(
        "Your smart contract address will be: " + WalletClient.encode58Check(contractAddress));

    GrpcAPI.Return response = broadcastTransaction1(transaction, blockingStubFull);
    if (response.getResult() == false) {
      return null;
    } else {
      //logger.info("brodacast succesfully");
      return contractAddress;
    }
  }


  /**
   * constructor.
   */
  public static byte[] deployContract1(String contractName, String abiString, String code,
      String data, Long feeLimit, long value, long consumeUserResourcePercent,
      String libraryAddress, String priKey, byte[] ownerAddress,
      WalletGrpc.WalletBlockingStub blockingStubFull, int permissionId,
      String[] permissionKeyString) {

    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    byte[] owner = ownerAddress;
    SmartContract.ABI abi = jsonStr2Abi(abiString);
    if (abi == null) {
      logger.error("abi is null");
      return null;
    }
    //byte[] codeBytes = Hex.decode(code);
    SmartContract.Builder builder = SmartContract.newBuilder();
    builder.setName(contractName);
    builder.setOriginAddress(ByteString.copyFrom(owner));
    builder.setAbi(abi);
    builder.setConsumeUserResourcePercent(consumeUserResourcePercent);
    builder.setOriginEnergyLimit(1000L);

    if (value != 0) {

      builder.setCallValue(value);
    }

    byte[] byteCode;
    if (null != libraryAddress) {
      byteCode = replaceLibraryAddress(code, libraryAddress);
    } else {
      byteCode = Hex.decode(code);
    }
    builder.setBytecode(ByteString.copyFrom(byteCode));

    Builder contractBuilder = CreateSmartContract.newBuilder();
    contractBuilder.setOwnerAddress(ByteString.copyFrom(owner));
    contractBuilder.setCallTokenValue(0L);
    contractBuilder.setTokenId(Long.parseLong("0"));
    CreateSmartContract contractDeployContract = contractBuilder.setNewContract(builder.build())
        .build();

    TransactionExtention transactionExtention = blockingStubFull
        .deployContract(contractDeployContract);
    if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
      System.out.println("RPC create trx failed!");
      if (transactionExtention != null) {
        System.out.println("Code = " + transactionExtention.getResult().getCode());
        System.out
            .println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
      }
      return null;
    }

    final TransactionExtention.Builder texBuilder = TransactionExtention.newBuilder();
    Transaction.Builder transBuilder = Transaction.newBuilder();
    Transaction.raw.Builder rawBuilder = transactionExtention.getTransaction().getRawData()
        .toBuilder();
    rawBuilder.setFeeLimit(feeLimit);
    transBuilder.setRawData(rawBuilder);
    for (int i = 0; i < transactionExtention.getTransaction().getSignatureCount(); i++) {
      ByteString s = transactionExtention.getTransaction().getSignature(i);
      transBuilder.setSignature(i, s);
    }
    for (int i = 0; i < transactionExtention.getTransaction().getRetCount(); i++) {
      Result r = transactionExtention.getTransaction().getRet(i);
      transBuilder.setRet(i, r);
    }
    texBuilder.setTransaction(transBuilder);
    texBuilder.setResult(transactionExtention.getResult());
    texBuilder.setTxid(transactionExtention.getTxid());
    transactionExtention = texBuilder.build();

    byte[] contractAddress = PublicMethed
        .generateContractAddress(transactionExtention.getTransaction(), owner);
    System.out.println(
        "Your smart contract address will be: " + WalletClient.encode58Check(contractAddress));
    if (transactionExtention == null) {
      return null;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return null;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return null;
    }
    try {
      transaction = setPermissionId(transaction, permissionId);
    } catch (CancelException e) {
      e.printStackTrace();
    }
    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);

    System.out.println(
        "txid = " + ByteArray.toHexString(Sha256Hash.hash(CommonParameter.getInstance()
            .isECKeyCryptoEngine(), transaction.getRawData().toByteArray())));
    contractAddress = PublicMethed.generateContractAddress(transaction, owner);
    System.out.println(
        "Your smart contract address will be: " + WalletClient.encode58Check(contractAddress));
    broadcastTransaction(transaction, blockingStubFull);
    return contractAddress;
  }

  /**
   * constructor.
   */
  public static String triggerContract1(byte[] contractAddress, String method, String argsStr,
      Boolean isHex, long callValue, long feeLimit, byte[] ownerAddress, String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull, int permissionId,
      String[] permissionKeyString) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;
    if (argsStr.equalsIgnoreCase("#")) {
      logger.info("argsstr is #");
      argsStr = "";
    }

    byte[] owner = ownerAddress;
    byte[] input = Hex.decode(AbiUtil.parseMethod(method, argsStr, isHex));

    TriggerSmartContract.Builder builder = TriggerSmartContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setContractAddress(ByteString.copyFrom(contractAddress));
    builder.setData(ByteString.copyFrom(input));
    builder.setCallValue(callValue);
    builder.setTokenId(Long.parseLong("0"));
    builder.setCallTokenValue(0L);
    TriggerSmartContract triggerContract = builder.build();

    TransactionExtention transactionExtention = blockingStubFull.triggerContract(triggerContract);
    if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
      System.out.println("RPC create call trx failed!");
      System.out.println("Code = " + transactionExtention.getResult().getCode());
      System.out
          .println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
      return null;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction.getRetCount() != 0 && transactionExtention.getConstantResult(0) != null
        && transactionExtention.getResult() != null) {
      byte[] result = transactionExtention.getConstantResult(0).toByteArray();
      System.out.println("message:" + transaction.getRet(0).getRet());
      System.out.println(
          ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));
      System.out.println("Result:" + Hex.toHexString(result));
      return null;
    }

    final TransactionExtention.Builder texBuilder = TransactionExtention.newBuilder();
    Transaction.Builder transBuilder = Transaction.newBuilder();
    Transaction.raw.Builder rawBuilder = transactionExtention.getTransaction().getRawData()
        .toBuilder();
    rawBuilder.setFeeLimit(feeLimit);
    transBuilder.setRawData(rawBuilder);
    for (int i = 0; i < transactionExtention.getTransaction().getSignatureCount(); i++) {
      ByteString s = transactionExtention.getTransaction().getSignature(i);
      transBuilder.setSignature(i, s);
    }
    for (int i = 0; i < transactionExtention.getTransaction().getRetCount(); i++) {
      Result r = transactionExtention.getTransaction().getRet(i);
      transBuilder.setRet(i, r);
    }
    texBuilder.setTransaction(transBuilder);
    texBuilder.setResult(transactionExtention.getResult());
    texBuilder.setTxid(transactionExtention.getTxid());
    transactionExtention = texBuilder.build();
    if (transactionExtention == null) {
      return null;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return null;
    }
    transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return null;
    }
    try {
      transaction = setPermissionId(transaction, permissionId);
    } catch (CancelException e) {
      e.printStackTrace();
    }
    System.out.println("trigger txid = " + ByteArray
        .toHexString(Sha256Hash.hash(CommonParameter.getInstance()
            .isECKeyCryptoEngine(), transaction.getRawData().toByteArray())));
    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);

    broadcastTransaction(transaction, blockingStubFull);
    return ByteArray.toHexString(Sha256Hash.hash(CommonParameter.getInstance()
        .isECKeyCryptoEngine(), transaction.getRawData().toByteArray()));
  }

  /**
   * constructor.
   */

  public static boolean updateAccountWithPermissionId(byte[] addressBytes, byte[] accountNameBytes,
      String priKey, WalletGrpc.WalletBlockingStub blockingStubFull, int permissionId,
      String[] permissionKeyString) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    AccountUpdateContract.Builder builder = AccountUpdateContract.newBuilder();
    ByteString basAddreess = ByteString.copyFrom(addressBytes);
    ByteString bsAccountName = ByteString.copyFrom(accountNameBytes);

    builder.setAccountName(bsAccountName);
    builder.setOwnerAddress(basAddreess);

    AccountUpdateContract contract = builder.build();
    Transaction transaction = blockingStubFull.updateAccount(contract);

    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("Please check!!! transaction == null");
      return false;
    }
    try {
      transaction = setPermissionId(transaction, permissionId);
    } catch (CancelException e) {
      e.printStackTrace();
    }
    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);

    return broadcastTransaction(transaction, blockingStubFull);
  }


  /**
   * constructor.
   */

  public static String transferAssetForTransactionId1(byte[] to, byte[] assertName, long amount,
      byte[] address, String priKey, WalletGrpc.WalletBlockingStub blockingStubFull,
      int permissionId, String[] permissionKeyString) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    TransferAssetContract.Builder builder = TransferAssetContract.newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsName = ByteString.copyFrom(assertName);
    ByteString bsOwner = ByteString.copyFrom(address);
    builder.setToAddress(bsTo);
    builder.setAssetName(bsName);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);

    TransferAssetContract contract = builder.build();
    Transaction transaction = blockingStubFull.transferAsset(contract);

    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      if (transaction == null) {
        logger.info("transaction == null");
      } else {
        logger.info("transaction.getRawData().getContractCount() == 0");
      }
      return null;
    }
    try {
      transaction = setPermissionId(transaction, permissionId);
    } catch (CancelException e) {
      e.printStackTrace();
    }
    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);

    boolean result = broadcastTransaction(transaction, blockingStubFull);
    if (result == false) {
      return null;
    } else {
      return ByteArray.toHexString(Sha256Hash.hash(CommonParameter.getInstance()
          .isECKeyCryptoEngine(), transaction.getRawData().toByteArray()));
    }
  }


  /**
   * constructor.
   */

  public static Boolean freezeBalanceGetEnergyWithPermissionId(byte[] addRess, long freezeBalance,
      long freezeDuration, int resourceCode, String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull, int permissionId,
      String[] permissionKeyString) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    byte[] address = addRess;
    long frozenBalance = freezeBalance;
    long frozenDuration = freezeDuration;
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    FreezeBalanceContract.Builder builder = FreezeBalanceContract.newBuilder();
    ByteString byteAddreess = ByteString.copyFrom(address);

    builder.setOwnerAddress(byteAddreess).setFrozenBalance(frozenBalance)
        .setFrozenDuration(frozenDuration).setResourceValue(resourceCode);

    FreezeBalanceContract contract = builder.build();
    Transaction transaction = blockingStubFull.freezeBalance(contract);

    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction = null");
      return false;
    }
    try {
      transaction = setPermissionId(transaction, permissionId);
    } catch (CancelException e) {
      e.printStackTrace();
    }
    transaction = TransactionUtils.setTimestamp(transaction);

    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);

    return broadcastTransaction(transaction, blockingStubFull);
  }


  /**
   * constructor.
   */

  public static Boolean freezeBalanceForReceiverWithPermissionId(byte[] addRess, long freezeBalance,
      long freezeDuration, int resourceCode, ByteString receiverAddressBytes, String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull, int permissionId,
      String[] permissionKeyString) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    byte[] address = addRess;
    long frozenBalance = freezeBalance;
    long frozenDuration = freezeDuration;
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    FreezeBalanceContract.Builder builder = FreezeBalanceContract.newBuilder();
    ByteString byteAddreess = ByteString.copyFrom(address);

    builder.setOwnerAddress(byteAddreess).setFrozenBalance(frozenBalance)
        .setFrozenDuration(frozenDuration).setResourceValue(resourceCode);
    builder.setReceiverAddress(receiverAddressBytes);
    FreezeBalanceContract contract = builder.build();
    Transaction transaction = blockingStubFull.freezeBalance(contract);

    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction = null");
      return false;
    }
    try {
      transaction = setPermissionId(transaction, permissionId);
    } catch (CancelException e) {
      e.printStackTrace();
    }
    transaction = TransactionUtils.setTimestamp(transaction);
    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);

    return broadcastTransaction(transaction, blockingStubFull);
  }


  /**
   * constructor.
   */

  public static boolean createAccount1(byte[] ownerAddress, byte[] newAddress, String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull, int permissionId,
      String[] permissionKeyString) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    byte[] owner = ownerAddress;
    AccountCreateContract.Builder builder = AccountCreateContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setAccountAddress(ByteString.copyFrom(newAddress));
    AccountCreateContract contract = builder.build();
    Transaction transaction = blockingStubFull.createAccount(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction == null");
    }
    try {
      transaction = setPermissionId(transaction, permissionId);
    } catch (CancelException e) {
      e.printStackTrace();
    }
    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);

    return broadcastTransaction(transaction, blockingStubFull);

  }

  /**
   * constructor.
   */

  public static boolean setAccountId1(byte[] accountIdBytes, byte[] ownerAddress, String priKey,
      int permissionId, WalletGrpc.WalletBlockingStub blockingStubFull,
      String[] permissionKeyString) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    byte[] owner = ownerAddress;
    SetAccountIdContract.Builder builder = SetAccountIdContract.newBuilder();
    ByteString bsAddress = ByteString.copyFrom(owner);
    ByteString bsAccountId = ByteString.copyFrom(accountIdBytes);
    builder.setAccountId(bsAccountId);
    builder.setOwnerAddress(bsAddress);
    SetAccountIdContract contract = builder.build();
    Transaction transaction = blockingStubFull.setAccountId(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction == null");
    }
    try {
      transaction = setPermissionId(transaction, permissionId);
    } catch (CancelException e) {
      e.printStackTrace();
    }
    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);
    return broadcastTransaction(transaction, blockingStubFull);
    //Return response = broadcastTransaction1(transaction, blockingStubFull);
    //if (response.getResult() == false) {
    // logger.info(ByteArray.toStr(response.getMessage().toByteArray()));
    // return false;
    //} else {
    //  return true;
    //}
    //return response.getResult();
  }


  /**
   * constructor.
   */
  public static Boolean voteWitnessWithPermissionId(HashMap<String, String> witness, byte[] addRess,
      String priKey, WalletGrpc.WalletBlockingStub blockingStubFull, int permissionId,
      String[] permissionKeyString) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);

    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;

    VoteWitnessContract.Builder builder = VoteWitnessContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(addRess));
    for (String addressBase58 : witness.keySet()) {
      String value = witness.get(addressBase58);
      final long count = Long.parseLong(value);
      VoteWitnessContract.Vote.Builder voteBuilder = VoteWitnessContract.Vote.newBuilder();
      byte[] address = WalletClient.decodeFromBase58Check(addressBase58);
      if (address == null) {
        continue;
      }
      voteBuilder.setVoteAddress(ByteString.copyFrom(address));
      voteBuilder.setVoteCount(count);
      builder.addVotes(voteBuilder.build());
    }

    VoteWitnessContract contract = builder.build();

    TransactionExtention transactionExtention = blockingStubFull.voteWitnessAccount2(contract);
    Transaction transaction = transactionExtention.getTransaction();

    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return false;
    }

    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction == null");
      return false;
    }
    try {
      transaction = setPermissionId(transaction, permissionId);
    } catch (CancelException e) {
      e.printStackTrace();
    }
    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);

    return broadcastTransaction(transaction, blockingStubFull);
  }

  /**
   * constructor.
   */

  public static String createAssetIssueForTransactionId1(byte[] address, String name,
      Long totalSupply, Integer trxNum, Integer icoNum, Long startTime, Long endTime,
      Integer voteScore, String description, String url, Long freeAssetNetLimit,
      Long publicFreeAssetNetLimit, Long fronzenAmount, Long frozenDay, String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull, int permissionId,
      String[] permissionKeyString) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;
    try {
      AssetIssueContract.Builder builder = AssetIssueContract.newBuilder();
      builder.setOwnerAddress(ByteString.copyFrom(address));
      builder.setName(ByteString.copyFrom(name.getBytes()));
      builder.setTotalSupply(totalSupply);
      builder.setTrxNum(trxNum);
      builder.setNum(icoNum);
      builder.setStartTime(startTime);
      builder.setEndTime(endTime);
      builder.setVoteScore(voteScore);
      builder.setDescription(ByteString.copyFrom(description.getBytes()));
      builder.setUrl(ByteString.copyFrom(url.getBytes()));
      builder.setFreeAssetNetLimit(freeAssetNetLimit);
      builder.setPublicFreeAssetNetLimit(publicFreeAssetNetLimit);
      AssetIssueContract.FrozenSupply.Builder frozenBuilder = AssetIssueContract.FrozenSupply
          .newBuilder();
      frozenBuilder.setFrozenAmount(fronzenAmount);
      frozenBuilder.setFrozenDays(frozenDay);

      builder.addFrozenSupply(0, frozenBuilder);

      Transaction transaction = blockingStubFull.createAssetIssue(builder.build());
      if (transaction == null || transaction.getRawData().getContractCount() == 0) {
        logger.info("transaction == null");
        return null;
      }
      try {
        transaction = setPermissionId(transaction, permissionId);
      } catch (CancelException e) {
        e.printStackTrace();
      }
      transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);

      boolean result = broadcastTransaction(transaction, blockingStubFull);
      if (result == false) {
        return null;
      } else {
        return ByteArray.toHexString(Sha256Hash.hash(CommonParameter.getInstance()
            .isECKeyCryptoEngine(), transaction.getRawData().toByteArray()));
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      return null;
    }
  }

  /**
   * constructor.
   */

  public static Boolean injectExchange1(long exchangeId, byte[] tokenId, long quant,
      byte[] ownerAddress, String priKey, WalletGrpc.WalletBlockingStub blockingStubFull,
      int permissionId, String[] permissionKeyString) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    byte[] owner = ownerAddress;

    ExchangeInjectContract.Builder builder = ExchangeInjectContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner)).setExchangeId(exchangeId)
        .setTokenId(ByteString.copyFrom(tokenId)).setQuant(quant);
    ExchangeInjectContract contract = builder.build();
    TransactionExtention transactionExtention = blockingStubFull.exchangeInject(contract);
    if (transactionExtention == null) {
      return false;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return false;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return false;
    }
    try {
      transaction = setPermissionId(transaction, permissionId);
    } catch (CancelException e) {
      e.printStackTrace();
    }
    System.out.println(
        "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));
    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);

    return broadcastTransaction(transaction, blockingStubFull);
  }


  /**
   * constructor.
   */

  public static boolean exchangeWithdraw1(long exchangeId, byte[] tokenId, long quant,
      byte[] ownerAddress, String priKey, WalletGrpc.WalletBlockingStub blockingStubFull,
      int permissionId, String[] permissionKeyString) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;
    byte[] owner = ownerAddress;

    ExchangeWithdrawContract.Builder builder = ExchangeWithdrawContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner)).setExchangeId(exchangeId)
        .setTokenId(ByteString.copyFrom(tokenId)).setQuant(quant);
    ExchangeWithdrawContract contract = builder.build();
    TransactionExtention transactionExtention = blockingStubFull.exchangeWithdraw(contract);
    if (transactionExtention == null) {
      return false;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return false;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return false;
    }
    try {
      transaction = setPermissionId(transaction, permissionId);
    } catch (CancelException e) {
      e.printStackTrace();
    }
    System.out.println(
        "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));
    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);

    return broadcastTransaction(transaction, blockingStubFull);
  }


  /**
   * constructor.
   */

  public static boolean exchangeTransaction1(long exchangeId, byte[] tokenId, long quant,
      long expected, byte[] ownerAddress, String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull, int permissionId,
      String[] permissionKeyString) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;
    byte[] owner = ownerAddress;

    ExchangeTransactionContract.Builder builder = ExchangeTransactionContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner)).setExchangeId(exchangeId)
        .setTokenId(ByteString.copyFrom(tokenId)).setQuant(quant).setExpected(expected);
    ExchangeTransactionContract contract = builder.build();
    TransactionExtention transactionExtention = blockingStubFull.exchangeTransaction(contract);
    if (transactionExtention == null) {
      return false;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return false;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return false;
    }
    try {
      transaction = setPermissionId(transaction, permissionId);
    } catch (CancelException e) {
      e.printStackTrace();
    }
    System.out.println(
        "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));
    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);

    return broadcastTransaction(transaction, blockingStubFull);
  }

  /**
   * constructor.
   */

  public static Boolean exchangeCreate1(byte[] firstTokenId, long firstTokenBalance,
      byte[] secondTokenId, long secondTokenBalance, byte[] ownerAddress, String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull, int permissionId,
      String[] permissionKeyString) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    byte[] owner = ownerAddress;

    ExchangeCreateContract.Builder builder = ExchangeCreateContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner))
        .setFirstTokenId(ByteString.copyFrom(firstTokenId)).setFirstTokenBalance(firstTokenBalance)
        .setSecondTokenId(ByteString.copyFrom(secondTokenId))
        .setSecondTokenBalance(secondTokenBalance);
    ExchangeCreateContract contract = builder.build();
    TransactionExtention transactionExtention = blockingStubFull.exchangeCreate(contract);
    if (transactionExtention == null) {
      return false;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return false;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return false;
    }
    try {
      transaction = setPermissionId(transaction, permissionId);
    } catch (CancelException e) {
      e.printStackTrace();
    }
    System.out.println(
        "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));
    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);

    return broadcastTransaction(transaction, blockingStubFull);
  }

  /**
   * constructor.
   */
  public static boolean clearContractAbi(byte[] contractAddress, byte[] ownerAddress, String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull, int permissionId,
      String[] permissionKeyString) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    byte[] owner = ownerAddress;

    ClearABIContract.Builder builder = ClearABIContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setContractAddress(ByteString.copyFrom(contractAddress));

    ClearABIContract clearABIContract = builder.build();

    TransactionExtention transactionExtention = blockingStubFull.clearContractABI(clearABIContract);
    if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
      System.out.println("RPC create call trx failed!");
      System.out.println("Code = " + transactionExtention.getResult().getCode());
      System.out
          .println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
      return false;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction.getRetCount() != 0 && transactionExtention.getConstantResult(0) != null
        && transactionExtention.getResult() != null) {
      byte[] result = transactionExtention.getConstantResult(0).toByteArray();
      System.out.println("message:" + transaction.getRet(0).getRet());
      System.out.println(
          ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));
      System.out.println("Result:" + Hex.toHexString(result));
      return false;
    }

    final TransactionExtention.Builder texBuilder = TransactionExtention.newBuilder();
    Transaction.Builder transBuilder = Transaction.newBuilder();
    Transaction.raw.Builder rawBuilder = transactionExtention.getTransaction().getRawData()
        .toBuilder();
    transBuilder.setRawData(rawBuilder);
    for (int i = 0; i < transactionExtention.getTransaction().getSignatureCount(); i++) {
      ByteString s = transactionExtention.getTransaction().getSignature(i);
      transBuilder.setSignature(i, s);
    }
    for (int i = 0; i < transactionExtention.getTransaction().getRetCount(); i++) {
      Result r = transactionExtention.getTransaction().getRet(i);
      transBuilder.setRet(i, r);
    }
    texBuilder.setTransaction(transBuilder);
    texBuilder.setResult(transactionExtention.getResult());
    texBuilder.setTxid(transactionExtention.getTxid());
    transactionExtention = texBuilder.build();
    if (transactionExtention == null) {
      return false;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return false;
    }
    transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return false;
    }
    try {
      transaction = setPermissionId(transaction, permissionId);
    } catch (CancelException e) {
      e.printStackTrace();
    }
    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);
    System.out.println("trigger txid = " + ByteArray
        .toHexString(Sha256Hash.hash(CommonParameter.getInstance()
            .isECKeyCryptoEngine(), transaction.getRawData().toByteArray())));
    return broadcastTransaction(transaction, blockingStubFull);
  }

  /**
   * constructor.
   */
  public static boolean sendShieldCoin(byte[] publicZenTokenOwnerAddress, long fromAmount,
      ShieldAddressInfo shieldAddressInfo, NoteTx noteTx, List<GrpcAPI.Note> shieldOutputList,
      byte[] publicZenTokenToAddress, long toAmount, String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull, Integer permission_id,
      String[] permissionKeyString) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    PrivateParameters.Builder builder = PrivateParameters.newBuilder();
    if (!ByteUtil.isNullOrZeroArray(publicZenTokenOwnerAddress)) {
      builder.setTransparentFromAddress(ByteString.copyFrom(publicZenTokenOwnerAddress));
      builder.setFromAmount(fromAmount);
    }
    if (!ByteUtil.isNullOrZeroArray(publicZenTokenToAddress)) {
      builder.setTransparentToAddress(ByteString.copyFrom(publicZenTokenToAddress));
      builder.setToAmount(toAmount);
    }

    if (shieldAddressInfo != null) {
      OutputPointInfo.Builder request = OutputPointInfo.newBuilder();

      //ShieldNoteInfo noteInfo = shieldWrapper.getUtxoMapNote().get(shieldInputList.get(i));
      OutputPoint.Builder outPointBuild = OutputPoint.newBuilder();
      outPointBuild.setHash(ByteString.copyFrom(noteTx.getTxid().toByteArray()));
      outPointBuild.setIndex(noteTx.getIndex());
      request.addOutPoints(outPointBuild.build());

      //ShieldNoteInfo noteInfo = shieldWrapper.getUtxoMapNote().get(shieldInputList.get(i));

      //String shieldAddress = noteInfo.getPaymentAddress();
      //ShieldAddressInfo addressInfo =
      //    shieldWrapper.getShieldAddressInfoMap().get(shieldAddress);
      SpendingKey spendingKey = new SpendingKey(shieldAddressInfo.getSk());
      try {
        ExpandedSpendingKey expandedSpendingKey = spendingKey.expandedSpendingKey();
        builder.setAsk(ByteString.copyFrom(expandedSpendingKey.getAsk()));
        builder.setNsk(ByteString.copyFrom(expandedSpendingKey.getNsk()));
        builder.setOvk(ByteString.copyFrom(expandedSpendingKey.getOvk()));
      } catch (Exception e) {
        System.out.println(e);
      }

      Note.Builder noteBuild = Note.newBuilder();
      noteBuild.setPaymentAddress(shieldAddressInfo.getAddress());
      noteBuild.setValue(noteTx.getNote().getValue());
      noteBuild.setRcm(ByteString.copyFrom(noteTx.getNote().getRcm().toByteArray()));
      noteBuild.setMemo(ByteString.copyFrom(noteTx.getNote().getMemo().toByteArray()));

      //System.out.println("address " + noteInfo.getPaymentAddress());
      //System.out.println("value " + noteInfo.getValue());
      //System.out.println("rcm " + ByteArray.toHexString(noteInfo.getR()));
      //System.out.println("trxId " + noteInfo.getTrxId());
      //System.out.println("index " + noteInfo.getIndex());
      //System.out.println("meno " + new String(noteInfo.getMemo()));

      SpendNote.Builder spendNoteBuilder = SpendNote.newBuilder();
      spendNoteBuilder.setNote(noteBuild.build());
      try {
        spendNoteBuilder.setAlpha(ByteString.copyFrom(org.tron.core.zen.note.Note.generateR()));
      } catch (Exception e) {
        System.out.println(e);
      }

      IncrementalMerkleVoucherInfo merkleVoucherInfo = blockingStubFull
          .getMerkleTreeVoucherInfo(request.build());
      spendNoteBuilder.setVoucher(merkleVoucherInfo.getVouchers(0));
      spendNoteBuilder.setPath(merkleVoucherInfo.getPaths(0));

      builder.addShieldedSpends(spendNoteBuilder.build());

    } else {
      byte[] ovk = ByteArray
          .fromHexString("030c8c2bc59fb3eb8afb047a8ea4b028743d23e7d38c6fa30908358431e2314d");
      builder.setOvk(ByteString.copyFrom(ovk));
    }

    if (shieldOutputList.size() > 0) {
      for (int i = 0; i < shieldOutputList.size(); ++i) {
        builder
            .addShieldedReceives(ReceiveNote.newBuilder().setNote(shieldOutputList.get(i)).build());
      }
    }

    TransactionExtention transactionExtention = blockingStubFull
        .createShieldedTransaction(builder.build());
    if (transactionExtention == null) {
      return false;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return false;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return false;
    }
    System.out.println(
        "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));
    Any any = transaction.getRawData().getContract(0).getParameter();

    try {
      ShieldContract.ShieldedTransferContract shieldedTransferContract = any
          .unpack(ShieldContract.ShieldedTransferContract.class);
      if (shieldedTransferContract.getFromAmount() > 0 || fromAmount == 321321) {
        Transaction.raw.Builder raw = transaction.getRawData().toBuilder();
        Transaction.Contract.Builder contract = raw.getContract(0).toBuilder()
            .setPermissionId(permission_id);
        raw.clearContract();
        raw.addContract(contract);
        transaction = transaction.toBuilder().setRawData(raw).build();

        transaction = signTransactionForShield(transaction, blockingStubFull, permissionKeyString);
        System.out.println("trigger txid = " + ByteArray
            .toHexString(Sha256Hash.hash(CommonParameter.getInstance()
                .isECKeyCryptoEngine(), transaction.getRawData().toByteArray())));
      } else {
        System.out.println("trigger txid = " + ByteArray
            .toHexString(Sha256Hash.hash(CommonParameter.getInstance()
                .isECKeyCryptoEngine(), transaction.getRawData().toByteArray())));
      }
    } catch (Exception e) {
      System.out.println(e);
    }
    return broadcastTransaction(transaction, blockingStubFull);
  }


  /**
   * constructor.
   */
  private static Transaction signTransactionForShield(Transaction transaction,
      WalletGrpc.WalletBlockingStub blockingStubFull, String[] priKeys) {
    /*    if (transaction.getRawData().getTimestamp() == 0) {
      transaction = TransactionUtils.setTimestamp(transaction);
    }

    long currentTime = System.currentTimeMillis();//*1000000 + System.nanoTime()%1000000;
   Transaction.Builder builder = transaction.toBuilder();
    org.tron.protos.Protocol.Transaction.raw.Builder rowBuilder = transaction.getRawData()
        .toBuilder();
    rowBuilder.setTimestamp(currentTime);
    builder.setRawData(rowBuilder.build());
    transaction = builder.build();*/

    for (int i = 0; i < priKeys.length; i += 1) {
      String priKey = priKeys[i];
      ECKey temKey = null;
      try {
        BigInteger priK = new BigInteger(priKey, 16);
        temKey = ECKey.fromPrivate(priK);
      } catch (Exception ex) {
        ex.printStackTrace();
      }
      ECKey ecKey = temKey;

      transaction = TransactionUtils.sign(transaction, ecKey);
      TransactionSignWeight weight = blockingStubFull.getTransactionSignWeight(transaction);
      if (weight.getResult().getCode()
          == TransactionSignWeight.Result.response_code.ENOUGH_PERMISSION) {
        break;
      }
      if (weight.getResult().getCode()
          == TransactionSignWeight.Result.response_code.NOT_ENOUGH_PERMISSION) {
        continue;
      }
    }
    logger.info("Sign transaction:" + transaction.toString());
    return transaction;
  }

  /**
   * constructor.
   */
  public static boolean createWitness(String url, byte[] owner, String priKey, int permissionId,
      String[] permissionKeyString, WalletGrpc.WalletBlockingStub blockingStubFull) {
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    byte[] byteurl = url.getBytes();
    WitnessCreateContract.Builder builder = WitnessCreateContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setUrl(ByteString.copyFrom(byteurl));
    WitnessCreateContract contract = builder.build();

    TransactionExtention transactionExtention = blockingStubFull.createWitness2(contract);
    if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
      System.out.println("RPC create trx failed!");
      if (transactionExtention != null) {
        System.out.println("Code = " + transactionExtention.getResult().getCode());
        System.out
            .println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
      }
      return false;
    }
    if (transactionExtention == null) {
      return false;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return false;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return false;
    }
    try {
      transaction = setPermissionId(transaction, permissionId);
    } catch (CancelException e) {
      e.printStackTrace();
    }
    System.out.println(
        "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));

    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);
    return broadcastTransaction(transaction, blockingStubFull);
  }

  /**
   * constructor.
   */

  public static boolean updateWitness2(byte[] owner, byte[] url, String priKey, int permissionId,
      String[] permissionKeyString, WalletGrpc.WalletBlockingStub blockingStubFull) {
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    WitnessUpdateContract.Builder builder = WitnessUpdateContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setUpdateUrl(ByteString.copyFrom(url));
    WitnessUpdateContract contract = builder.build();

    GrpcAPI.TransactionExtention transactionExtention = blockingStubFull.updateWitness2(contract);
    if (transactionExtention == null) {
      return transactionExtention.getResult().getResult();
    }
    GrpcAPI.Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return false;
    }
    Protocol.Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return false;
    }
    try {
      transaction = setPermissionId(transaction, permissionId);
    } catch (CancelException e) {
      e.printStackTrace();
    }
    System.out.println(
        "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));

    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);
    GrpcAPI.Return response = blockingStubFull.broadcastTransaction(transaction);
    if (!response.getResult()) {
      logger.info(ByteArray.toStr(response.getMessage().toByteArray()));
      logger.info("response.getRestult() == false");
      return false;
    }
    return true;
  }

  /**
   * constructor.
   */
  public static boolean withdrawBalance(byte[] address, String priKey, int permissionId,
      String[] permissionKeyString, WalletGrpc.WalletBlockingStub blockingStubFull) {
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;

    WithdrawBalanceContract.Builder builder = WithdrawBalanceContract.newBuilder();
    ByteString byteAddreess = ByteString.copyFrom(address);
    builder.setOwnerAddress(byteAddreess);
    WithdrawBalanceContract contract = builder.build();

    Transaction transaction = blockingStubFull.withdrawBalance(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }

    transaction = signTransaction(ecKey, transaction);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return false;
    }
    try {
      transaction = setPermissionId(transaction, permissionId);
    } catch (CancelException e) {
      e.printStackTrace();
    }
    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);
    return true;
  }

  /**
   * constructor.
   */
  public static boolean unFreezeAsset(byte[] addRess, String priKey, int permissionId,
      String[] permissionKeyString, WalletGrpc.WalletBlockingStub blockingStubFull) {
    byte[] address = addRess;
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;
    UnfreezeAssetContract.Builder builder = UnfreezeAssetContract.newBuilder();
    ByteString byteAddreess = ByteString.copyFrom(address);
    builder.setOwnerAddress(byteAddreess);
    UnfreezeAssetContract contract = builder.build();
    Transaction transaction = blockingStubFull.unfreezeAsset(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }

    transaction = signTransaction(ecKey, transaction);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return false;
    }
    try {
      transaction = setPermissionId(transaction, permissionId);
    } catch (CancelException e) {
      e.printStackTrace();
    }
    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);
    return true;
  }

  /**
   * constructor.
   */

  public static Transaction sendcoinGetTransaction(byte[] to, long amount, byte[] owner,
      String priKey, WalletGrpc.WalletBlockingStub blockingStubFull, String[] permissionKeyString) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    //String priKey = testKey002;
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;
    //Protocol.Account search = queryAccount(priKey, blockingStubFull);

    TransferContract.Builder builder = TransferContract.newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsOwner = ByteString.copyFrom(owner);
    builder.setToAddress(bsTo);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);

    TransferContract contract = builder.build();
    Transaction transaction = blockingStubFull.createTransaction(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction ==null");
      return null;
    }
    transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);

    return transaction;

  }

  /**
   * constructor.
   */
  public static boolean updateBrokerage(byte[] owner, int brokerage, String priKey,
      int permissionId, String[] priKeys, WalletGrpc.WalletBlockingStub blockingStubFull) {

    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;

    UpdateBrokerageContract.Builder updateBrokerageContract = UpdateBrokerageContract.newBuilder();
    updateBrokerageContract.setOwnerAddress(ByteString.copyFrom(owner)).setBrokerage(brokerage);
    TransactionExtention transactionExtention = blockingStubFull
        .updateBrokerage(updateBrokerageContract.build());
    Protocol.Transaction transaction = transactionExtention.getTransaction();
    if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
      if (transactionExtention != null) {
        System.out.println("Code = " + transactionExtention.getResult().getCode());
        System.out
            .println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
      }
      return false;
    }
    try {
      transaction = setPermissionId(transaction, permissionId);
      transaction = signTransaction(transaction, blockingStubFull, priKeys);
    } catch (Exception e) {
      e.printStackTrace();
    }

    System.out.println("trigger txid = " + ByteArray
        .toHexString(Sha256Hash.hash(CommonParameter.getInstance()
            .isECKeyCryptoEngine(), transaction.getRawData().toByteArray())));

    return broadcastTransaction(transaction, blockingStubFull);
  }

  /**
   * constructor.
   */
  public static boolean marketSellAsset(byte[] owner,  byte[] sellTokenId,
      long sellTokenQuantity, byte[] buyTokenId, long buyTokenQuantity,
      int permissionId, String[] priKeys, WalletBlockingStub blockingStubFull) {

    MarketContract.MarketSellAssetContract.Builder builder = MarketContract.MarketSellAssetContract
        .newBuilder();
    builder
        .setOwnerAddress(ByteString.copyFrom(owner))
        .setSellTokenId(ByteString.copyFrom(sellTokenId))
        .setSellTokenQuantity(sellTokenQuantity)
        .setBuyTokenId(ByteString.copyFrom(buyTokenId))
        .setBuyTokenQuantity(buyTokenQuantity);

    TransactionExtention transactionExtention = blockingStubFull.marketSellAsset(builder.build());
    if (transactionExtention == null) {
      return false;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return false;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return false;
    }

    if (transaction.getRawData().getContract(0).getType()
        != ContractType.MarketSellAssetContract) {
      return false;
    }

    try {
      transaction = setPermissionId(transaction, permissionId);
      transaction = signTransaction(transaction, blockingStubFull, priKeys);
    } catch (CancelException e) {
      e.printStackTrace();
    }
    return broadcastTransaction(transaction, blockingStubFull);

  }

  /**
   * constructor.
   */
  public static boolean marketCancelOrder(byte[] owner, byte[] orderId,
      int permissionId, String[] priKeys,
      WalletBlockingStub blockingStubFull) {

    MarketContract.MarketCancelOrderContract.Builder builder = MarketContract
        .MarketCancelOrderContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner)).setOrderId(ByteString.copyFrom(orderId));

    TransactionExtention transactionExtention = blockingStubFull.marketCancelOrder(builder.build());

    if (transactionExtention == null) {
      return false;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return false;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return false;
    }

    if (transaction.getRawData().getContract(0).getType()
        != ContractType.MarketCancelOrderContract) {
      System.out.println("Wrong ContractType :"
          + transaction.getRawData().getContract(0).getType());
      return false;
    }

    try {
      transaction = setPermissionId(transaction, permissionId);
      transaction = signTransaction(transaction,blockingStubFull,priKeys);
    } catch (CancelException e) {
      e.printStackTrace();
    }
    return broadcastTransaction(transaction, blockingStubFull);
  }
}
