package stest.tron.wallet.common.client.utils;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonObject;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.CrossChainAuctionConfigDetailList;
import org.tron.api.GrpcAPI.CrossChainVoteDetailList;
import org.tron.api.GrpcAPI.CrossChainVotePaginated;
import org.tron.api.GrpcAPI.CrossChainVoteSummaryList;
import org.tron.api.GrpcAPI.CrossChainVoteSummaryPaginated;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.api.GrpcAPI.PaginatedMessage;
import org.tron.api.GrpcAPI.ParaChainList;
import org.tron.api.GrpcAPI.RegisterCrossChainList;
import org.tron.api.GrpcAPI.Return;
import org.tron.api.GrpcAPI.RoundMessage;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.runtime.TvmTestUtils;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Commons;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.core.exception.CancelException;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.CrossMessage;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.contract.BalanceContract;
import org.tron.protos.contract.CrossChain;
import org.tron.protos.contract.SmartContractOuterClass;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;



@Slf4j
public class CrossChainBase {

  public static final String foundationKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  public static final byte[] foundationAddress = PublicMethed.getFinalAddress(foundationKey);
  public static final String zenTrc20TokenOwnerKey = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.zenTrc20TokenOwnerKey");
  public static ManagedChannel channelFull = null;
  public static ManagedChannel crossChannelFull = null;
  public static ManagedChannel channelSolidity = null;
  public static WalletGrpc.WalletBlockingStub blockingStubFull = null;
  public static WalletGrpc.WalletBlockingStub crossBlockingStubFull = null;
  public static String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  public static String crossFullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  public static ByteString chainId;
  public static ByteString crossChainId;
  public static ByteString parentHash;
  public static ByteString crossParentHash;
  public static Long voteAmount = 110000000L;
  public static String round = "6";
  public static String crossRound = "6";
  public static Long startSynBlockNum = 12L;
  public static Long startSynTimeStamp;
  public static Long crossStartSynTimeStamp;
  public static long registerNum = 1;


  public static String registerAccountKey
      = "7400E3D0727F8A61041A8E8BF86599FE5597CE19DE451E59AED07D60967A5E25";
  public static byte[] registerAccountAddress = PublicMethed.getFinalAddress(registerAccountKey);

  public static ECKey ecKey1 = new ECKey(Utils.getRandom());
  public static String trc10TokenAccountKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  public static final byte[] trc10TokenAccountAddress
      = PublicMethed.getFinalAddress(trc10TokenAccountKey);

  public static ECKey ecKey2 = new ECKey(Utils.getRandom());
  public static String mutisignTestKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  public static final byte[] mutisignTestAddress
      = PublicMethed.getFinalAddress(mutisignTestKey);


  public static long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");

  public static final String witnessKey001 = Configuration.getByPath("testng.conf")
      .getString("witness.key1");
  public static final String witnessKey002 = Configuration.getByPath("testng.conf")
      .getString("witness.key2");
  public static final byte[] witness001Address = PublicMethed.getFinalAddress(witnessKey001);
  public static final byte[] witness002Address = PublicMethed.getFinalAddress(witnessKey002);
  public static String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  public static String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");
  public static final long now = System.currentTimeMillis();
  public static final long totalSupply = now;
  public static String name1 = "chain1_" + Long.toString(now);
  public static String name2 = "chain2_" + Long.toString(now);
  public static ByteString assetAccountId1;
  public static ByteString assetAccountId2;
  public static ByteString assetAccountIdCrossChain;
  public static ByteString mutisignAssetAccountId1;
  public static ByteString mutisignAssetAccountId2;
  public static byte[] contractAddress;
  public static byte[] crossContractAddress;
  public static final long getChainIdBlockNum = 1L;
  public static ECKey ecKey3 = new ECKey(Utils.getRandom());
  public static byte[] manager1Address = ecKey3.getAddress();
  public static String manager1Key = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
  public static ECKey ecKey4 = new ECKey(Utils.getRandom());
  public static byte[] manager2Address = ecKey4.getAddress();
  public static String manager2Key = ByteArray.toHexString(ecKey4.getPrivKeyBytes());
  public static String[] permissionKeyString = new String[2];
  public static String[] ownerKeyString = new String[3];
  public static String accountPermissionJson = "";

  static HttpResponse response;
  static String transactionString;
  static String transactionSignString;

  /**
   * constructor.
   */
  @BeforeSuite(enabled = true, description = "Prepare env for cross chain")
  public void deployCrossChainNeeded() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    crossChannelFull = ManagedChannelBuilder.forTarget(crossFullnode)
        .usePlaintext(true)
        .build();
    crossBlockingStubFull = WalletGrpc.newBlockingStub(crossChannelFull);

    logger.info("trc10TokenAccount :");
    PublicMethed.printAddress(trc10TokenAccountKey);


    PublicMethed.sendcoin(registerAccountAddress, 2048000000L,
        foundationAddress, foundationKey, blockingStubFull);
    PublicMethed.sendcoin(registerAccountAddress, 2048000000L,
        foundationAddress, foundationKey, crossBlockingStubFull);
    PublicMethed.sendcoin(trc10TokenAccountAddress, 2048000000L,
        foundationAddress, foundationKey, blockingStubFull);
    PublicMethed.sendcoin(trc10TokenAccountAddress, 2048000000L,
        foundationAddress, foundationKey, crossBlockingStubFull);
    PublicMethed.sendcoin(mutisignTestAddress, 2048000000L,
        foundationAddress, foundationKey, blockingStubFull);
    PublicMethed.sendcoin(mutisignTestAddress, 2048000000L,
        foundationAddress, foundationKey, crossBlockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Long start = System.currentTimeMillis() + 50000;
    Long end = System.currentTimeMillis() + 1000000000;

    //Create a new AssetIssue success.
    Assert.assertTrue(PublicMethed.createAssetIssue(trc10TokenAccountAddress, name1,
        totalSupply * 10000, 1,
        100, 6, start, end, 1, description, url, 10000L, 10000L,
        1L, 1L, trc10TokenAccountKey, blockingStubFull));
    Assert.assertTrue(PublicMethed.createAssetIssue(trc10TokenAccountAddress, name2,
        totalSupply * 10000, 1, 100, 6, start, end, 1, description, url,
        10000L, 10000L,
        1L, 1L, trc10TokenAccountKey, crossBlockingStubFull));

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(crossBlockingStubFull);

    Account getAssetIdFromThisAccount;
    getAssetIdFromThisAccount = PublicMethed
        .queryAccount(trc10TokenAccountAddress, blockingStubFull);
    assetAccountId1 = getAssetIdFromThisAccount.getAssetIssuedID();
    getAssetIdFromThisAccount = PublicMethed
        .queryAccount(trc10TokenAccountAddress, crossBlockingStubFull);
    assetAccountIdCrossChain = getAssetIdFromThisAccount.getAssetIssuedID();
    GrpcAPI.AssetIssueList assetIssueList = crossBlockingStubFull
        .getAssetIssueList(EmptyMessage.newBuilder().build());
    assetAccountId2 = ByteString.copyFromUtf8(String
        .valueOf(1000002L + assetIssueList.getAssetIssueCount()));

    start = System.currentTimeMillis() + 50000;
    end = System.currentTimeMillis() + 1000000000;
    Assert.assertTrue(PublicMethed.createAssetIssue(mutisignTestAddress, name1, totalSupply, 1,
        100, 6, start, end, 1, description, url, 10000L, 10000L,
        1L, 1L, mutisignTestKey, blockingStubFull));
    Assert.assertTrue(PublicMethed.createAssetIssue(mutisignTestAddress, name2,
        totalSupply, 1, 100, 6, start, end, 1, description, url,
        10000L, 10000L,
        1L, 1L, mutisignTestKey, crossBlockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(crossBlockingStubFull);


    getAssetIdFromThisAccount = PublicMethed
        .queryAccount(mutisignTestAddress, blockingStubFull);
    mutisignAssetAccountId1 = getAssetIdFromThisAccount.getAssetIssuedID();
    mutisignAssetAccountId2 = ByteString.copyFromUtf8(String
        .valueOf(1000003L + assetIssueList.getAssetIssueCount()));







    chainId = PublicMethed.getBlock(getChainIdBlockNum, blockingStubFull)
        .getBlockHeader().getRawData().getParentHash();
    crossChainId = PublicMethed.getBlock(getChainIdBlockNum, crossBlockingStubFull)
        .getBlockHeader().getRawData().getParentHash();
    parentHash = PublicMethed.getBlock(startSynBlockNum, blockingStubFull)
        .getBlockHeader().getRawData().getParentHash();
    crossParentHash = PublicMethed.getBlock(startSynBlockNum, crossBlockingStubFull)
        .getBlockHeader().getRawData().getParentHash();
    startSynTimeStamp = PublicMethed.getBlock(startSynBlockNum, blockingStubFull)
        .getBlockHeader().getRawData().getTimestamp();
    crossStartSynTimeStamp = PublicMethed.getBlock(startSynBlockNum, crossBlockingStubFull)
        .getBlockHeader().getRawData().getTimestamp();


    PublicMethed.sendcoin(witness001Address, 5000000L,
        foundationAddress, foundationKey, blockingStubFull);
    PublicMethed.sendcoin(witness002Address, 5000000L,
        foundationAddress, foundationKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    PublicMethed.sendcoin(witness001Address, 5000000L,
        foundationAddress, foundationKey, crossBlockingStubFull);
    PublicMethed.sendcoin(witness002Address, 5000000L,
        foundationAddress, foundationKey, crossBlockingStubFull);
    PublicMethed.waitProduceNextBlock(crossBlockingStubFull);



    //Create trc20 token in two chain.
    String filePath = "./src/test/resources/soliditycode/crossContractA.sol";
    String contractName = "crossContractA";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    contractAddress = PublicMethed
        .deployContract(contractName, abi, code, null,
            maxFeeLimit, 0L, 100, null,
            trc10TokenAccountKey, trc10TokenAccountAddress, blockingStubFull);

    filePath = "./src/test/resources/soliditycode/crossContractB.sol";
    contractName = "crossContractB";
    retMap = PublicMethed.getBycodeAbi(filePath, contractName);

    code = retMap.get("byteCode").toString();
    abi = retMap.get("abI").toString();
    crossContractAddress = PublicMethed
        .deployContract(contractName, abi, code, null,
            maxFeeLimit, 0L, 100, null,
            trc10TokenAccountKey, trc10TokenAccountAddress, crossBlockingStubFull);


    PublicMethed.waitProduceNextBlock(blockingStubFull);


    permissionKeyString[0] = manager1Key;
    permissionKeyString[1] = manager2Key;
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    ownerKeyString[0] = mutisignTestKey;
    ownerKeyString[1] = manager1Key;
    ownerKeyString[2] = manager2Key;
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":3,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(manager2Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(mutisignTestKey)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":2,"
            + "\"operations\":\"000000c00000c007000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(manager2Key) + "\",\"weight\":1}"
            + "]}]}";
    logger.info(accountPermissionJson);
    PublicMethedForMutiSign.accountPermissionUpdate(
        accountPermissionJson, mutisignTestAddress, mutisignTestKey,
        blockingStubFull, ownerKeyString);

    PublicMethedForMutiSign.accountPermissionUpdate(
        accountPermissionJson, mutisignTestAddress, mutisignTestKey,
        crossBlockingStubFull, ownerKeyString);


  }

  /**
   * constructor.
   */
  @AfterSuite(enabled = true)
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  /**
   * constructor.
   */
  public static String getTxidFromTransactionExtention(TransactionExtention transactionExtention,
      ECKey ecKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
    return getTxidFromTransactionExtention(transactionExtention, ecKey,
        -1, null, blockingStubFull);


  }


  /**
   * constructor.
   */
  public static String getTxidFromTransactionExtention(TransactionExtention transactionExtention,
      ECKey ecKey, int permissionId, String[] permissionKeyString,
      WalletGrpc.WalletBlockingStub blockingStubFull) {

    if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
      System.out.println("RPC create trx failed!");
      if (transactionExtention != null) {
        System.out.println("Code = " + transactionExtention.getResult().getCode());
        System.out
            .println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
      }
      return null;
    }

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
    if (permissionId == -1) {
      transaction = PublicMethed.signTransaction(ecKey, transaction);
    } else {
      try {
        transaction = PublicMethedForMutiSign.setPermissionId(transaction, permissionId);
      } catch (CancelException e) {
        e.printStackTrace();
      }
      transaction = PublicMethedForMutiSign.signTransaction(transaction,
          blockingStubFull, permissionKeyString);
    }

    String txid = ByteArray.toHexString(Sha256Hash
        .hash(CommonParameter.getInstance().isECKeyCryptoEngine(),
            transaction.getRawData().toByteArray()));
    System.out.println("txid = " + txid);
    GrpcAPI.Return response = PublicMethed.broadcastTransaction(transaction, blockingStubFull);
    if (response.getResult() == false) {
      return null;
    } else {
      return txid;
    }
  }

  /**
   * constructor.
   */
  public static String registerCrossChainGetTxid(byte[] ownerAddress, byte[] proxyAddress,
      Long registerNum, ByteString chainId, List<ByteString> srList, Long beginSyncHeight,
      Long maintenanceTimeInterval, ByteString parentBlockHash, Long blockTime, String priKey,
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
    BalanceContract.CrossChainInfo.Builder build = BalanceContract.CrossChainInfo.newBuilder();
    build.setOwnerAddress(ByteString.copyFrom(ownerAddress));
    build.setProxyAddress(ByteString.copyFrom(proxyAddress));
    build.setChainId(chainId);
    build.addAllSrList(srList);
    build.setBeginSyncHeight(beginSyncHeight);
    build.setMaintenanceTimeInterval(maintenanceTimeInterval);
    build.setParentBlockHash(parentBlockHash);
    build.setBlockTime(blockTime);
    build.setRegisterNum(registerNum);
    TransactionExtention transactionExtention = blockingStubFull.registerCrossChain(build.build());
    return getTxidFromTransactionExtention(transactionExtention, ecKey, blockingStubFull);
  }

  /**
   * constructor.
   */
  public static String updateCrossChainGetTxid(byte[] ownerAddress, byte[] proxyAddress,
      ByteString chainId, List<ByteString> srList, Long beginSyncHeight,
      Long maintenanceTimeInterval, String parentBlockHash, Long blockTime, String priKey,
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
    BalanceContract.CrossChainInfo.Builder build = BalanceContract.CrossChainInfo.newBuilder();
    build.setOwnerAddress(ByteString.copyFrom(ownerAddress));
    build.setProxyAddress(ByteString.copyFrom(proxyAddress));
    build.setChainId(chainId);
    build.addAllSrList(srList);
    build.setBeginSyncHeight(beginSyncHeight);
    build.setMaintenanceTimeInterval(maintenanceTimeInterval);
    build.setParentBlockHash(ByteString.copyFrom(ByteArray.fromHexString(parentBlockHash)));
    build.setBlockTime(blockTime);
    TransactionExtention transactionExtention = blockingStubFull.updateCrossChain(build.build());
    return getTxidFromTransactionExtention(transactionExtention, ecKey, blockingStubFull);
  }


  /**
   * constructor.
   */
  public static String voteCrossChainGetTxid(byte[] ownerAddress, Long registerNum, Long amount,
      Integer round, String priKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;
    CrossChain.VoteCrossChainContract.Builder build =
        CrossChain.VoteCrossChainContract.newBuilder();
    build.setAmount(amount);
    build.setRegisterNum(registerNum);
    build.setOwnerAddress(ByteString.copyFrom(ownerAddress));
    build.setRound(round);
    TransactionExtention transactionExtention = blockingStubFull.voteCrossChain(build.build());
    return getTxidFromTransactionExtention(transactionExtention, ecKey, blockingStubFull);
  }

  /**
   * constructor.
   */
  public static String unVoteCrossChainGetTxid(byte[] ownerAddress, Long registerNum,
      Integer round, String priKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;
    CrossChain.UnvoteCrossChainContract.Builder build =
        CrossChain.UnvoteCrossChainContract.newBuilder();
    build.setRegisterNum(registerNum);
    build.setOwnerAddress(ByteString.copyFrom(ownerAddress));
    build.setRound(round);
    TransactionExtention transactionExtention = blockingStubFull.unvoteCrossChain(build.build());
    return getTxidFromTransactionExtention(transactionExtention, ecKey, blockingStubFull);
  }

  /**
   * constructor.
   */
  public static String createTriggerContractForCrossByHttp(byte[] ownerAddress, byte[] proxyAddress,
      byte[] contractAddress, byte[] crossContractAddress, String method, String argsStr,
      ByteString chainId, ByteString crossChainId, String priKey,
      String httpNode, WalletGrpc.WalletBlockingStub blockingStubFull) throws Exception {
    httpNode = "http://" + httpNode;
    byte[] data =  Hex.decode(AbiUtil.parseMethod(method, argsStr, false));
    SmartContractOuterClass.TriggerSmartContract triggerSmartContractSource
        = TvmTestUtils.buildTriggerSmartContract(
        ownerAddress, contractAddress, data, 0);
    Protocol.Transaction.raw.Builder transactionBuilder1 = Protocol.Transaction.raw.newBuilder().addContract(
        Protocol.Transaction.Contract.newBuilder()
            .setType(Protocol.Transaction.Contract.ContractType.TriggerSmartContract)
            .setParameter(Any.pack(triggerSmartContractSource))
            .build())
        .setFeeLimit(100000000L);
    Protocol.Transaction transactionSource = Protocol.Transaction.newBuilder()
        .setRawData(transactionBuilder1.build())
        .build();

    transactionSource = PublicMethed.addTransactionSign(transactionSource, priKey, blockingStubFull);

    SmartContractOuterClass.TriggerSmartContract triggerSmartContractDest = TvmTestUtils.buildTriggerSmartContract(
        proxyAddress, crossContractAddress, data, 0);
    Protocol.Transaction.raw.Builder transactionBuilder2 = Protocol.Transaction.raw.newBuilder().addContract(
        Protocol.Transaction.Contract.newBuilder()
            .setType(Protocol.Transaction.Contract.ContractType.TriggerSmartContract)
            .setParameter(Any.pack(triggerSmartContractDest))
            .build())
        .setFeeLimit(100000000L);
    Protocol.Transaction transactionDest = Protocol.Transaction.newBuilder().setRawData(transactionBuilder2.build()).build();

    BalanceContract.ContractTrigger contractTrigger = BalanceContract.ContractTrigger.newBuilder()
        .setSource(transactionSource)
        .setDest(transactionDest)
        .build();

    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty("owner_address", ByteArray.toHexString(ownerAddress));
    jsonObject.addProperty("owner_chainId", ByteArray.toHexString(chainId.toByteArray()));
    jsonObject.addProperty("to_address", ByteArray.toHexString(ownerAddress));
    jsonObject.addProperty("to_chainId", ByteArray.toHexString(crossChainId.toByteArray()));
    jsonObject.addProperty("data", ByteArray.toHexString(contractTrigger.toByteString().toByteArray()));
    jsonObject.addProperty("type", 1);
    jsonObject.addProperty("contractType", "CrossContract");
    HttpResponse response = HttpMethed.createConnect(httpNode + "/wallet/createCommonTransaction", jsonObject);
    JSONObject responseContent = HttpMethed.parseResponseContent(response);
    String rawDataHex = responseContent.getString("raw_data_hex");
    Protocol.Transaction transaction = Protocol.Transaction.newBuilder().setRawData(Protocol.Transaction.raw.parseFrom(ByteArray.fromHexString(rawDataHex))).build();
    transaction = TransactionUtils.sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(priKey)));
    String hex = Hex.toHexString(transaction.toByteArray());
    JsonObject jsonObjectNew = new JsonObject();
    jsonObjectNew.addProperty("transaction", hex);
    HttpMethed.createConnect(httpNode + "/wallet/broadcasthex", jsonObjectNew);
    String txid = ByteArray.toHexString(Sha256Hash
        .hash(CommonParameter.getInstance().isECKeyCryptoEngine(),
            transaction.getRawData().toByteArray()));
    return txid;

  }

  /**
   * constructor.
   */
  public static String createTriggerContractForCross(byte[] ownerAddress, byte[] proxyAddress,
      byte[] contractAddress, byte[] crossContractAddress, String method, String argsStr,
      ByteString chainId, ByteString crossChainId, String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return createTriggerContractForCross(ownerAddress, proxyAddress, contractAddress,
        crossContractAddress, method, argsStr, chainId, crossChainId, priKey, -1,
        null, blockingStubFull);

  }

  /**
   * constructor.
   */
  public static String createTriggerContractForCross(byte[] ownerAddress, byte[] proxyAddress,
      byte[] contractAddress, byte[] crossContractAddress, String method, String argsStr,
      ByteString chainId, ByteString crossChainId, String priKey, int permissionId,
      String[] permissionKeyString, WalletGrpc.WalletBlockingStub blockingStubFull) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    //byte[] data = TvmTestUtils.parseAbi(method,argsStr);

    byte[] data =  Hex.decode(AbiUtil.parseMethod(method, argsStr, false));


    SmartContractOuterClass.TriggerSmartContract triggerSmartContractSource
        = TvmTestUtils.buildTriggerSmartContract(
        ownerAddress, contractAddress, data, 0);
    Protocol.Transaction.raw.Builder transactionBuilder1
        = Protocol.Transaction.raw.newBuilder().addContract(
        Protocol.Transaction.Contract.newBuilder()
            .setType(Protocol.Transaction.Contract.ContractType.TriggerSmartContract)
            .setParameter(Any.pack(triggerSmartContractSource))
            .build())
        .setFeeLimit(100000000L);
    Protocol.Transaction transactionSource = Protocol.Transaction.newBuilder()
        .setRawData(transactionBuilder1.build())
        .build();

    if (permissionId == -1) {
      transactionSource = PublicMethed.addTransactionSign(transactionSource,
          priKey, blockingStubFull);
    } else {
      for (String key : permissionKeyString) {
        transactionSource = PublicMethedForMutiSign
            .addTransactionSignWithPermissionId(transactionSource, key, permissionId,
                blockingStubFull);
      }
    }




    SmartContractOuterClass.TriggerSmartContract triggerSmartContractDest
        = TvmTestUtils.buildTriggerSmartContract(
        proxyAddress, crossContractAddress, data, 0);

    Protocol.Transaction.raw.Builder transactionBuilder2
        = Protocol.Transaction.raw.newBuilder().addContract(
        Protocol.Transaction.Contract.newBuilder()
            .setType(Protocol.Transaction.Contract.ContractType.TriggerSmartContract)
            .setParameter(Any.pack(triggerSmartContractDest))
            .build())
        .setFeeLimit(100000000L);
    Protocol.Transaction transactionDest
        = Protocol.Transaction.newBuilder().setRawData(transactionBuilder2.build()).build();


    BalanceContract.ContractTrigger contractTrigger = BalanceContract.ContractTrigger.newBuilder()
        .setSource(transactionSource)
        .setDest(transactionDest)
        .build();
    BalanceContract.CrossContract crossContract = BalanceContract.CrossContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ownerAddress))
        .setOwnerChainId(Sha256Hash.wrap(
            chainId).getByteString())
        .setToAddress(ByteString.copyFrom(ownerAddress))
        .setToChainId(Sha256Hash.wrap(
            crossChainId).getByteString())
        .setData(contractTrigger.toByteString())
        .setType(BalanceContract.CrossContract.CrossDataType.CONTRACT)
        .build();
    Protocol.Transaction.Builder transaction = Protocol.Transaction.newBuilder();
    Protocol.Transaction.raw.Builder raw = Protocol.Transaction.raw.newBuilder();
    Protocol.Transaction.Contract.Builder contract = Protocol.Transaction.Contract.newBuilder();
    contract.setType(Protocol.Transaction.Contract.ContractType.CrossContract)
        .setParameter(Any.pack(crossContract));
    raw.addContract(contract.build());
    transaction.setRawData(raw.build());
    GrpcAPI.TransactionExtention transactionExtention = blockingStubFull
        .createCommonTransaction(transaction.build());
    return getTxidFromTransactionExtention(transactionExtention, ecKey,
        permissionId, permissionKeyString, blockingStubFull);
  }

  /**
   * constructor.
   */
  public static String createCrossTrc10TransferByHttp(byte[] ownerAddress, byte[] toAddress,
      ByteString tokenId,ByteString tokenChainId,Integer precision, Long amount,
      String tokenName,ByteString chainId,
      ByteString paraChainId,String priKey, String httpNode) throws Exception {
    httpNode = "http://" + httpNode;

    BalanceContract.CrossToken crossToken = BalanceContract.CrossToken.newBuilder()
        .setTokenId(tokenId)
        .setAmount(amount)
        .setTokenName(ByteString.copyFrom(ByteArray.fromString(tokenName)))
        .setPrecision(precision)
        .setChainId(tokenChainId)
        .build();

    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty("owner_address", ByteArray.toHexString(ownerAddress));
    jsonObject.addProperty("owner_chainId", ByteArray.toHexString(chainId.toByteArray()));
    jsonObject.addProperty("to_address", ByteArray.toHexString(toAddress));
    jsonObject.addProperty("to_chainId", ByteArray.toHexString(paraChainId.toByteArray()));
    jsonObject.addProperty("data", ByteArray.toHexString(crossToken.toByteString().toByteArray()));
    jsonObject.addProperty("type", 0);
    jsonObject.addProperty("contractType", "CrossContract");

    HttpResponse response = HttpMethed
        .createConnect(httpNode + "/wallet/createCommonTransaction", jsonObject);
    JSONObject responseContent = HttpMethed.parseResponseContent(response);
    String rawDataHex = responseContent.getString("raw_data_hex");

    Protocol.Transaction transaction = Protocol.Transaction.newBuilder()
        .setRawData(Protocol.Transaction.raw
            .parseFrom(ByteArray.fromHexString(rawDataHex))).build();
    transaction = TransactionUtils.sign(transaction, ECKey
        .fromPrivate(ByteArray.fromHexString(priKey)));

    String hex = Hex.toHexString(transaction.toByteArray());
    JsonObject jsonObjectNew = new JsonObject();
    jsonObjectNew.addProperty("transaction", hex);
    response = HttpMethed.createConnect(httpNode + "/wallet/broadcasthex", jsonObjectNew);
    String txid = ByteArray.toHexString(Sha256Hash
        .hash(CommonParameter.getInstance().isECKeyCryptoEngine(),
            transaction.getRawData().toByteArray()));
    return txid;



  }


  /**
   * constructor.
   */
  public static String createCrossTrc10Transfer(byte[] ownerAddress, byte[] toAddress,
      ByteString tokenId, ByteString tokenChainId, Integer precision, Long amount,
      String tokenName, ByteString chainId,
      ByteString paraChainId, String priKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
    return createCrossTrc10Transfer(ownerAddress, toAddress, tokenId, tokenChainId, precision,
        amount, tokenName,
        chainId, paraChainId, priKey, -1, null, blockingStubFull);

  }


  /**
   * constructor.
   */
  public static String createCrossTrc10Transfer(byte[] ownerAddress, byte[] toAddress,
      ByteString tokenId, ByteString tokenChainId, Integer precision, Long amount, String tokenName,
      ByteString chainId,
      ByteString paraChainId, String priKey, int permissionId, String[] permissionKeyString,
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
    BalanceContract.CrossToken crossToken = BalanceContract.CrossToken.newBuilder()
        .setTokenId(tokenId)
        .setAmount(amount)
        .setTokenName(ByteString.copyFrom(ByteArray.fromString(tokenName)))
        .setPrecision(precision)
        .setChainId(Sha256Hash.wrap(
            tokenChainId.toByteArray()).getByteString())
        .build();


    BalanceContract.CrossContract crossContract = BalanceContract.CrossContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ownerAddress))
        .setOwnerChainId(Sha256Hash.wrap(
            chainId.toByteArray()).getByteString())
        .setToAddress(ByteString.copyFrom(toAddress))
        .setToChainId(Sha256Hash.wrap(
            paraChainId.toByteArray()).getByteString())
        .setData(crossToken.toByteString())
        .setType(BalanceContract.CrossContract.CrossDataType.TOKEN)
        .build();

    Protocol.Transaction.Builder transaction = Protocol.Transaction.newBuilder();
    Protocol.Transaction.raw.Builder raw = Protocol.Transaction.raw.newBuilder();
    Protocol.Transaction.Contract.Builder contract = Protocol.Transaction.Contract.newBuilder();
    contract.setType(Protocol.Transaction.Contract.ContractType.CrossContract)
        .setParameter(Any.pack(crossContract));
    raw.addContract(contract.build());
    transaction.setRawData(raw.build());

    TransactionExtention transactionExtention
        = blockingStubFull.createCommonTransaction(transaction.build());

    return getTxidFromTransactionExtention(transactionExtention, ecKey,
        permissionId, permissionKeyString, blockingStubFull);
  }


  /**
   * constructor.
   */
  public static Optional<CrossChainVoteSummaryList> getCrossChainVoteSummaryList(Integer round,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    CrossChainVoteSummaryPaginated request = CrossChainVoteSummaryPaginated.newBuilder()
        .setLimit(10).setOffset(0).setRound(round).build();
    CrossChainVoteSummaryList crossChainVoteSummaryList
        = blockingStubFull.getCrossChainVoteSummaryList(request);
    return Optional.ofNullable(crossChainVoteSummaryList);
  }

  /**
   * constructor.
   */
  public static Optional<RegisterCrossChainList> getRegisterCrossChainList(Integer limit,
      Integer offset, WalletGrpc.WalletBlockingStub blockingStubFull) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    PaginatedMessage request = PaginatedMessage.newBuilder().build().newBuilder().build()
        .newBuilder()
        .setLimit(limit).setOffset(offset).build();
    RegisterCrossChainList registerCrossChainList
        = blockingStubFull.getRegisterCrossChainList(request);
    return Optional.ofNullable(registerCrossChainList);
  }

  /**
   * constructor.
   */
  public static HttpResponse httpUnvoteCrossChain(String httpNode, String ownerAddress,
                                                  int registerNum, int round, String key) {
    try {

      JSONObject rawBody = new JSONObject();
      rawBody.put("register_num", registerNum);
      rawBody.put("owner_address", ownerAddress);
      rawBody.put("round", round);
      String requestUrl = "http://" + httpNode + "/wallet/unvotecrosschain";
      response = HttpMethed.createConnect1(requestUrl, rawBody);
      transactionString = EntityUtils.toString(response.getEntity());
      transactionSignString = HttpMethed.gettransactionsign(httpNode, transactionString, key);
      logger.info(transactionString);
      logger.info(transactionSignString);
      response = HttpMethed.broadcastTransaction(httpNode, transactionSignString);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
    return response;
  }

  /**
   * constructor.
   */
  public static Optional<ParaChainList> getParaChainList(Integer round,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    RoundMessage request = RoundMessage.newBuilder().setRound(round).build();
    ParaChainList paraChainList = blockingStubFull.getParaChainList(request);
    return Optional.ofNullable(paraChainList);
  }

  /**
   * constructor.
   */
  public static Optional<CrossChainVoteDetailList> getCrossChainVoteDetailList(Integer round,
      Long registerNum, WalletGrpc.WalletBlockingStub blockingStubFull) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    CrossChainVotePaginated request = CrossChainVotePaginated.newBuilder()
        .setLimit(10).setOffset(0).setRound(round).setRegisterNum(registerNum).build();

    CrossChainVoteDetailList crossChainVoteDetailList
        = blockingStubFull.getCrossChainVoteDetailList(request);
    return Optional.ofNullable(crossChainVoteDetailList);
  }

  /**
   * constructor.
   */
  public static Optional<CrossChainAuctionConfigDetailList>
      getAuctionConfigList(WalletGrpc.WalletBlockingStub blockingStubFull) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    CrossChainAuctionConfigDetailList auctionConfigDetailList =
        blockingStubFull.getCrossChainAuctionConfigDetailList(EmptyMessage.newBuilder().build());
    return Optional.ofNullable(auctionConfigDetailList);
  }

  /**
   * constructor.
   */
  public static HttpResponse
        httpRegisterCrossChainGetTxid(String httpNode, String ownerAddress,
                                                           String proxyAddress, String chainId,
                                      List<String> srList, Long beginSyncHeight,
                                      Long maintenanceTimeInterval, String parentBlockHash,
                                                           Long blockTime, int registerNum,
                                      String key) {
    try {
      JSONObject rawBody = new JSONObject();
      rawBody.put("owner_address", ownerAddress);
      rawBody.put("proxy_address", proxyAddress);
      rawBody.put("chain_id", chainId);
      rawBody.put("sr_list", srList);
      rawBody.put("begin_sync_height", beginSyncHeight);
      rawBody.put("maintenance_time_interval", maintenanceTimeInterval);
      rawBody.put("parent_block_hash", parentBlockHash);
      rawBody.put("block_time", blockTime);
      rawBody.put("register_num", registerNum);
      rawBody.put("visible", true);
      String requestUrl = "http://" + httpNode + "/wallet/registercrosschain";
      response = HttpMethed.createConnect1(requestUrl, rawBody);
      transactionString = EntityUtils.toString(response.getEntity());
      transactionSignString = HttpMethed.gettransactionsign(httpNode, transactionString, key);
      logger.info(transactionString);
      logger.info(transactionSignString);
      response = HttpMethed.broadcastTransaction(httpNode, transactionSignString);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
    return response;
  }

  /**
   * constructor.
   */
  public static HttpResponse httpGetRegisterCrossChainList(String httpNode, int offset,
                                                           int limit) {
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getregistercrosschainlist";
      JSONObject rawBody = new JSONObject();
      rawBody.put("offset", offset);
      rawBody.put("limit", limit);
      System.out.println("requestUrl: " + requestUrl);
      System.out.println("rawBody: " + rawBody.toJSONString());
      response = HttpMethed.createConnect1(requestUrl, rawBody);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
    return response;
  }

  /**
   * constructor.
   */
  public static HttpResponse httpVoteCrossChain(String httpNode, String ownerAddress,
                                                int registerNum, Long amount,
                                                int round, String key) {
    try {
      JSONObject rawBody = new JSONObject();
      rawBody.put("register_num", registerNum);
      rawBody.put("owner_address", ownerAddress);
      rawBody.put("amount", amount);
      rawBody.put("round", round);
      rawBody.put("visible", true);
      String requestUrl = "http://" + httpNode + "/wallet/votecrosschain";
      response = HttpMethed.createConnect1(requestUrl, rawBody);
      transactionString = EntityUtils.toString(response.getEntity());
      transactionSignString = HttpMethed.gettransactionsign(httpNode, transactionString, key);
      logger.info(transactionString);
      logger.info(transactionSignString);
      response = HttpMethed.broadcastTransaction(httpNode, transactionSignString);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
    return response;
  }

  /**
   * constructor.
   */
  public static List<String> getCrossTransactionListFromTargetRange(Long startNum, Long endNum,
                                                                    WalletGrpc.WalletBlockingStub
                                                                        blockingStubFull) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    List<String> transactionList = new ArrayList<>();
    for (Long i = startNum; i <= endNum; i++) {
      NumberMessage.Builder builder = NumberMessage.newBuilder();
      builder.setNum(i);
      Block block = blockingStubFull.getBlockByNum(builder.build());
      for (CrossMessage crossMessage : block.getCrossMessageList()) {
        transactionList.add(ByteArray.toHexString(Sha256Hash.hash(CommonParameter.getInstance()
            .isECKeyCryptoEngine(), crossMessage.getTransaction().getRawData().toByteArray())));
      }
    }
    return transactionList;
  }

  /**
   * constructor.
   */
  public static List<CrossMessage> getCrossMessageListFromTargetRange(Long startNum, Long endNum,
                                                                      WalletGrpc.WalletBlockingStub
                                                                          blockingStubFull) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    List<CrossMessage> crossMessageList = new ArrayList<>();
    for (Long i = startNum; i <= endNum; i++) {
      NumberMessage.Builder builder = NumberMessage.newBuilder();
      builder.setNum(i);
      Block block = blockingStubFull.getBlockByNum(builder.build());
      for (CrossMessage crossMessage : block.getCrossMessageList()) {
        crossMessageList.add(crossMessage);
      }
    }
    return crossMessageList;
  }

  /**
   * constructor.
   */
  public static HttpResponse httpUpdateCrossChain(String httpNode,
                                                  String ownerAddress, String proxyAddress,
                                                  String chainId, List<String> srList,
                                                  Long beginSyncHeight,
                                                  Long maintenanceTimeInterval,
                                                  String parentBlockHash,
                                                  Long blockTime, int registerNum, String key) {
    try {
      JSONObject rawBody = new JSONObject();
      rawBody.put("owner_address", ownerAddress);
      rawBody.put("proxy_address", proxyAddress);
      rawBody.put("chain_id", chainId);
      rawBody.put("sr_lis", srList);
      rawBody.put("begin_sync_height", beginSyncHeight);
      rawBody.put("maintenance_time_interval", maintenanceTimeInterval);
      rawBody.put("parent_block_hash", parentBlockHash);
      rawBody.put("block_time", blockTime);
      rawBody.put("register_num", registerNum);
      rawBody.put("visible", true);
      String requestUrl = "http://" + httpNode + "/wallet/updatecrosschain";
      response = HttpMethed.createConnect1(requestUrl, rawBody);
      response = HttpMethed.createConnect1(requestUrl, rawBody);
      transactionString = EntityUtils.toString(response.getEntity());
      transactionSignString = HttpMethed.gettransactionsign(httpNode, transactionString, key);
      logger.info(transactionString);
      logger.info(transactionSignString);
      response = HttpMethed.broadcastTransaction(httpNode, transactionSignString);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
    return response;
  }

  /**
   * constructor.
   */
  public static HttpResponse httpGetcrosschainvotesummarylist(String httpNode,
                                                              int offset, int limit, int round) {
    try {
      JSONObject rawBody = new JSONObject();
      rawBody.put("offset", offset);
      rawBody.put("limit", limit);
      rawBody.put("round", limit);
      rawBody.put("visible", true);
      String requestUrl = "http://" + httpNode + "/wallet/getcrosschainvotesummarylist";
      response = HttpMethed.createConnect1(requestUrl, rawBody);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
    return response;
  }

  /**
   * constructor.
   */
  public static HttpResponse httpGetparachainlist(String httpNode, int round) {
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getparachainlist";
      JSONObject rawBody = new JSONObject();
      rawBody.put("round", round);
      rawBody.put("visible", true);
      response = HttpMethed.createConnect1(requestUrl, rawBody);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
    return response;
  }



  /**
   * constructor.
   */
  public static HttpResponse httpGetcrosschainvotedetaillist(String httpNode,
                                                             int registerNum,
                                                             int offset, int limit, int round) {
    try {
      JSONObject rawBody = new JSONObject();
      rawBody.put("register_num", registerNum);
      rawBody.put("offset", offset);
      rawBody.put("limit", limit);
      rawBody.put("round", round);
      rawBody.put("visible", true);
      String requestUrl = "http://" + httpNode + "/wallet/getcrosschainvotedetaillist";
      response = HttpMethed.createConnect1(requestUrl, rawBody);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
    return response;
  }

  /**
   * constructor.
   */
  public static HttpResponse httpGetauctionconfiglist(String httpNode) {
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getauctionconfiglist";
      response = HttpMethed.createConnect(requestUrl);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
    return response;
  }
}
