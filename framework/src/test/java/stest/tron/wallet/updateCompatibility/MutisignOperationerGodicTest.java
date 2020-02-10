package stest.tron.wallet.updateCompatibility;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.ExchangeList;
import org.tron.api.GrpcAPI.Note;
import org.tron.api.GrpcAPI.ProposalList;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.core.config.args.Args;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.ChainParameters;
import org.tron.protos.Protocol.Exchange;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.PublicMethedForMutiSign;
import stest.tron.wallet.common.client.utils.ShieldAddressInfo;

@Slf4j
public class MutisignOperationerGodicTest {

  final String updateName = Long.toString(System.currentTimeMillis());
  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final String operations = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.operations");
  String[] permissionKeyString = new String[2];
  String[] ownerKeyString = new String[2];
  String accountPermissionJson = "";
  String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");
  Optional<ShieldAddressInfo> shieldAddressInfo;
  String shieldAddress;
  List<Note> shieldOutList = new ArrayList<>();
  Account firstAccount;
  ByteString assetAccountId1;
  ByteString assetAccountId2;
  Optional<ExchangeList> listExchange;
  Optional<Exchange> exchangeIdInfo;
  Integer exchangeId = 0;
  Integer exchangeRate = 10;
  Long firstTokenInitialBalance = 10000L;
  Long secondTokenInitialBalance = firstTokenInitialBalance * exchangeRate;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] manager1Address = ecKey1.getAddress();
  String manager1Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] manager2Address = ecKey2.getAddress();
  String manager2Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] mutisignAccountAddress = ecKey3.getAddress();
  String mutisignAccountKey = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
  ECKey ecKey4 = new ECKey(Utils.getRandom());
  byte[] newAddress = ecKey4.getAddress();
  String newKey = ByteArray.toHexString(ecKey4.getPrivKeyBytes());
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private String foundationZenTokenKey = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.zenTokenOwnerKey");
  byte[] foundationZenTokenAddress = PublicMethed.getFinalAddress(foundationZenTokenKey);
  private String zenTokenId = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.zenTokenId");
  private byte[] tokenId = zenTokenId.getBytes();
  private Long zenTokenFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.zenTokenFee");
  private long multiSignFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.multiSignFee");
  private Long costTokenAmount = 8 * zenTokenFee;
  private Long sendTokenAmount = 3 * zenTokenFee;

  /**
   * constructor.
   */
  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    if (PublicMethed.queryAccount(foundationZenTokenKey, blockingStubFull).getCreateTime() == 0) {
      PublicMethed.sendcoin(foundationZenTokenAddress, 20480000000000L, fromAddress,
          testKey002, blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      String name = "shieldToken";
      Long start = System.currentTimeMillis() + 20000;
      Long end = System.currentTimeMillis() + 10000000000L;
      Long totalSupply = 15000000000000001L;
      String description = "This asset issue is use for exchange transaction stress";
      String url = "This asset issue is use for exchange transaction stress";
      PublicMethed.createAssetIssue(foundationZenTokenAddress, name, totalSupply, 1, 1,
          start, end, 1, description, url, 1000L, 1000L,
          1L, 1L, foundationZenTokenKey, blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      Account getAssetIdFromThisAccount =
          PublicMethed.queryAccount(foundationZenTokenAddress, blockingStubFull);
      ByteString assetAccountId = getAssetIdFromThisAccount.getAssetIssuedID();
      logger.info("AssetId:" + assetAccountId.toString());
    }
  }

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    Assert.assertTrue(PublicMethed.sendcoin(mutisignAccountAddress, 1000_000_000_000L, fromAddress,
        testKey002, blockingStubFull));
    //updatepermission权限，账户交易所需钱等前置条件写在这
    permissionKeyString[0] = manager1Key;
    permissionKeyString[1] = manager2Key;
    ownerKeyString[0] = manager1Key;
    ownerKeyString[1] = manager2Key;
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":2,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(manager2Key)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":2,"
            + "\"operations\":\"" + operations + "\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(manager2Key) + "\",\"weight\":1}"
            + "]}]}";

    logger.info(accountPermissionJson);
    Assert.assertTrue(PublicMethedForMutiSign
        .accountPermissionUpdate(accountPermissionJson, mutisignAccountAddress, mutisignAccountKey,
            blockingStubFull, new String[]{mutisignAccountKey}));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true)
  public void test001MutiSignGodicAccountTypeTransaction() {
    Assert.assertTrue(
        PublicMethedForMutiSign.setAccountId1(("" + System.currentTimeMillis()).getBytes(),
            mutisignAccountAddress, mutisignAccountKey, 2, blockingStubFull,
            permissionKeyString));
    Assert.assertTrue(PublicMethedForMutiSign.createAccountWhtiPermissionId(
        mutisignAccountAddress, newAddress, mutisignAccountKey, blockingStubFull,
        2, permissionKeyString));
    Assert.assertTrue(PublicMethedForMutiSign.sendcoinWithPermissionId(
        newAddress, 100L, mutisignAccountAddress, 2,
        mutisignAccountKey, blockingStubFull, permissionKeyString));
    Assert.assertTrue(PublicMethedForMutiSign.freezeBalanceWithPermissionId(
        mutisignAccountAddress, 1000000L, 0, 2,
        mutisignAccountKey, blockingStubFull, permissionKeyString));
    Assert.assertTrue(PublicMethedForMutiSign.freezeBalanceGetEnergyWithPermissionId(
        mutisignAccountAddress, 1000000L, 0, 1,
        mutisignAccountKey, blockingStubFull, 2, permissionKeyString));
    Assert.assertTrue(PublicMethedForMutiSign.freezeBalanceForReceiverWithPermissionId(
        mutisignAccountAddress, 1000000L, 0, 0,
        ByteString.copyFrom(newAddress),
        mutisignAccountKey, blockingStubFull, 2, permissionKeyString));
    Assert.assertTrue(PublicMethedForMutiSign.unFreezeBalanceWithPermissionId(
        mutisignAccountAddress, mutisignAccountKey, 0, null,
        2, blockingStubFull, permissionKeyString));
    Assert.assertTrue(PublicMethedForMutiSign.unFreezeBalanceWithPermissionId(
        mutisignAccountAddress, mutisignAccountKey, 0, newAddress,
        2, blockingStubFull, permissionKeyString));
    Assert.assertTrue(PublicMethedForMutiSign.updateAccountWithPermissionId(
        mutisignAccountAddress, updateName.getBytes(), mutisignAccountKey, blockingStubFull,
        2, permissionKeyString));
  }

  @Test(enabled = true)
  public void test002MutiSignGodicContractTypeTransaction() {
    Long maxFeeLimit = 1000000000L;
    //String contractName = "StorageAndCpu" + Integer.toString(randNum);
    String filePath = "./src/test/resources/soliditycode/walletTestMutiSign004.sol";
    String contractName = "timeoutTest";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    byte[] contractAddress = PublicMethedForMutiSign.deployContractWithPermissionId(
        contractName, abi, code, "", maxFeeLimit,
        0L, 100, maxFeeLimit, "0", 0L, null,
        mutisignAccountKey, mutisignAccountAddress, blockingStubFull, permissionKeyString, 2);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContract smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);
    Assert.assertTrue(smartContract.getAbi().toString() != null);
    String txid;
    String initParmes = "\"" + "930" + "\"";
    txid = PublicMethedForMutiSign.triggerContractWithPermissionId(contractAddress,
        "testUseCpu(uint256)", initParmes, false,
        0, maxFeeLimit, "0", 0L, mutisignAccountAddress,
        mutisignAccountKey, blockingStubFull, permissionKeyString, 2);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Assert.assertTrue(
        PublicMethedForMutiSign.updateSettingWithPermissionId(
            contractAddress, 50, mutisignAccountKey,
            mutisignAccountAddress, 2, blockingStubFull, permissionKeyString));
    Assert.assertTrue(
        PublicMethedForMutiSign.updateEnergyLimitWithPermissionId(
            contractAddress, 50, mutisignAccountKey,
            mutisignAccountAddress, 2, blockingStubFull, permissionKeyString));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Assert.assertTrue(PublicMethedForMutiSign
        .clearContractAbi(contractAddress, mutisignAccountAddress, mutisignAccountKey,
            blockingStubFull, 2, permissionKeyString));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

  }

  @Test(enabled = true)
  public void test003MutiSignGodicTokenTypeTransaction() {

    long now = System.currentTimeMillis();
    String name = "MutiSign001_" + Long.toString(now);
    long totalSupply = now;
    Long start = System.currentTimeMillis() + 5000;
    Long end = System.currentTimeMillis() + 1000000000;
    logger.info("try create asset issue");

    Assert.assertTrue(PublicMethedForMutiSign
        .createAssetIssueWithpermissionId(mutisignAccountAddress, name, totalSupply, 1,
            1, start, end, 1, description, url, 2000L, 2000L,
            1L, 1L, mutisignAccountKey, blockingStubFull, 2, permissionKeyString));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    //Assert.assertTrue(PublicMethedForMutiSign.unFreezeAsset(mutisignAccountAddress,
    //    mutisignAccountKey,2,ownerKeyString,blockingStubFull));

    Account getAssetIdFromOwnerAccount;
    getAssetIdFromOwnerAccount = PublicMethed.queryAccount(
        mutisignAccountAddress, blockingStubFull);
    assetAccountId1 = getAssetIdFromOwnerAccount.getAssetIssuedID();

    Assert.assertTrue(PublicMethedForMutiSign.transferAssetWithpermissionId(manager1Address,
        assetAccountId1.toByteArray(), 10, mutisignAccountAddress,
        mutisignAccountKey, blockingStubFull, 2, permissionKeyString));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Assert.assertTrue(PublicMethedForMutiSign
        .updateAssetWithPermissionId(mutisignAccountAddress, description.getBytes(), url.getBytes(),
            100L, 100L, mutisignAccountKey,
            2, blockingStubFull, permissionKeyString));
    PublicMethed.waitProduceNextBlock(blockingStubFull);


  }

  @Test(enabled = true)
  public void test004MutiSignGodicExchangeTypeTransaction() {

    ECKey ecKey22 = new ECKey(Utils.getRandom());
    byte[] secondExchange001Address = ecKey22.getAddress();
    String secondExchange001Key = ByteArray.toHexString(ecKey22.getPrivKeyBytes());
    Long secondTransferAssetToFirstAccountNum = 100000000L;

    long now = System.currentTimeMillis();
    String name2 = "exchange001_2_" + Long.toString(now);
    String name1 = "exchange001_1_" + Long.toString(now);
    final long totalSupply = 1000000001L;

    org.junit.Assert
        .assertTrue(PublicMethed.sendcoin(secondExchange001Address, 10240000000L, fromAddress,
            testKey002, blockingStubFull));
    org.junit.Assert.assertTrue(PublicMethed
        .freezeBalanceForReceiver(fromAddress, 100000000000L, 0, 0,
            ByteString.copyFrom(secondExchange001Address),
            testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Long start = System.currentTimeMillis() + 5000L;
    Long end = System.currentTimeMillis() + 5000000L;
    org.junit.Assert
        .assertTrue(PublicMethed.createAssetIssue(secondExchange001Address, name2, totalSupply, 1,
            1, start, end, 1, description, url, 10000L, 10000L,
            1L, 1L, secondExchange001Key, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    listExchange = PublicMethed.getExchangeList(blockingStubFull);
    exchangeId = listExchange.get().getExchangesCount();

    Account getAssetIdFromThisAccount;
    getAssetIdFromThisAccount = PublicMethed.queryAccount(mutisignAccountAddress, blockingStubFull);
    assetAccountId1 = getAssetIdFromThisAccount.getAssetIssuedID();

    getAssetIdFromThisAccount = PublicMethed
        .queryAccount(secondExchange001Address, blockingStubFull);
    assetAccountId2 = getAssetIdFromThisAccount.getAssetIssuedID();

    firstAccount = PublicMethed.queryAccount(mutisignAccountAddress, blockingStubFull);
    org.junit.Assert.assertTrue(PublicMethed.transferAsset(
        mutisignAccountAddress, assetAccountId2.toByteArray(),
        secondTransferAssetToFirstAccountNum, secondExchange001Address,
        secondExchange001Key, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    org.junit.Assert.assertTrue(
        PublicMethedForMutiSign.exchangeCreate1(
            assetAccountId1.toByteArray(), firstTokenInitialBalance,
            assetAccountId2.toByteArray(), secondTokenInitialBalance, mutisignAccountAddress,
            mutisignAccountKey, blockingStubFull, 2, permissionKeyString));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    listExchange = PublicMethed.getExchangeList(blockingStubFull);
    exchangeId = listExchange.get().getExchangesCount();

    org.junit.Assert.assertTrue(
        PublicMethedForMutiSign.injectExchange1(
            exchangeId, assetAccountId1.toByteArray(), 100,
            mutisignAccountAddress, mutisignAccountKey, blockingStubFull, 2, permissionKeyString));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    org.junit.Assert.assertTrue(
        PublicMethedForMutiSign.exchangeWithdraw1(
            exchangeId, assetAccountId1.toByteArray(), 200,
            mutisignAccountAddress, mutisignAccountKey, blockingStubFull, 2, permissionKeyString));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    firstAccount = PublicMethed.queryAccount(mutisignAccountAddress, blockingStubFull);

    org.junit.Assert.assertTrue(
        PublicMethedForMutiSign
            .exchangeTransaction1(exchangeId, assetAccountId1.toByteArray(), 50, 1,
                mutisignAccountAddress, mutisignAccountKey, blockingStubFull,
                2, permissionKeyString));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    firstAccount = PublicMethed.queryAccount(mutisignAccountAddress, blockingStubFull);

    Assert.assertTrue(PublicMethedForMutiSign.participateAssetIssueWithPermissionId(
        secondExchange001Address,
        assetAccountId2.toByteArray(), 1, mutisignAccountAddress, mutisignAccountKey, 2,
        blockingStubFull, ownerKeyString));

  }

  @Test(enabled = true)
  public void test005MutiSignGodicShieldTransaction() {

    Assert.assertTrue(PublicMethed.transferAsset(mutisignAccountAddress, tokenId,
        costTokenAmount, foundationZenTokenAddress, foundationZenTokenKey, blockingStubFull));
    Args.setFullNodeAllowShieldedTransaction(true);
    shieldAddressInfo = PublicMethed.generateShieldAddress();
    shieldAddress = shieldAddressInfo.get().getAddress();
    logger.info("shieldAddress:" + shieldAddress);
    final Long beforeAssetBalance = PublicMethed.getAssetIssueValue(mutisignAccountAddress,
        PublicMethed.queryAccount(foundationZenTokenKey, blockingStubFull).getAssetIssuedID(),
        blockingStubFull);
    final Long beforeBalance = PublicMethed
        .queryAccount(mutisignAccountAddress, blockingStubFull).getBalance();
    final Long beforeNetUsed = PublicMethed
        .getAccountResource(mutisignAccountAddress, blockingStubFull).getFreeNetUsed();

    String memo = "aaaaaaa";

    shieldOutList = PublicMethed.addShieldOutputList(shieldOutList, shieldAddress,
        "" + (sendTokenAmount - zenTokenFee), memo);

    Assert.assertTrue(PublicMethedForMutiSign.sendShieldCoin(
        mutisignAccountAddress, sendTokenAmount,
        null, null,
        shieldOutList,
        null, 0,
        mutisignAccountKey, blockingStubFull, 2, permissionKeyString));

    PublicMethed.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true)
  public void test006MutiSignGodicWitnessTransaction() {
    permissionKeyString[0] = manager1Key;
    permissionKeyString[1] = manager2Key;
    ownerKeyString[0] = manager1Key;
    ownerKeyString[1] = manager2Key;
    PublicMethed.printAddress(newKey);
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":2,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(manager2Key)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":2,"
            + "\"operations\":\"" + operations + "\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(manager2Key) + "\",\"weight\":1}"
            + "]}]}";

    Assert.assertTrue(PublicMethed.sendcoin(newAddress, 1000000_000_000L, fromAddress,
        testKey002, blockingStubFull));
    logger.info(accountPermissionJson);
    Assert.assertTrue(PublicMethedForMutiSign
        .accountPermissionUpdate(accountPermissionJson, newAddress, newKey,
            blockingStubFull, new String[]{newKey}));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    long now = System.currentTimeMillis();
    String url = "MutiSign001_" + Long.toString(now) + ".com";
    Assert.assertTrue(PublicMethedForMutiSign.createWitness(url, newAddress,
        newKey, 2, permissionKeyString, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert
        .assertTrue(PublicMethedForMutiSign.updateWitness2(newAddress, "newWitness.com".getBytes(),
            newKey, 2, permissionKeyString, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String voteStr = Base58.encode58Check(newAddress);
    HashMap<String, String> smallVoteMap = new HashMap<String, String>();
    smallVoteMap.put(voteStr, "1");
    Assert.assertTrue(PublicMethedForMutiSign.voteWitnessWithPermissionId(
        smallVoteMap, mutisignAccountAddress, mutisignAccountKey, blockingStubFull,
        2, permissionKeyString));


  }

  @Test(enabled = true)
  public void test007MutiSignGodicProposalTypeTransaction() {

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    HashMap<Long, Long> proposalMap = new HashMap<Long, Long>();
    proposalMap.put(0L, 81000L);
    Assert.assertTrue(
        PublicMethedForMutiSign.createProposalWithPermissionId(newAddress, newKey,
            proposalMap, 2, blockingStubFull, permissionKeyString));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    //Get proposal list
    ProposalList proposalList = blockingStubFull.listProposals(EmptyMessage.newBuilder().build());
    Optional<ProposalList> listProposals = Optional.ofNullable(proposalList);
    final Integer proposalId = listProposals.get().getProposalsCount();
    logger.info(Integer.toString(proposalId));

    Assert.assertTrue(PublicMethedForMutiSign.approveProposalWithPermission(
        newAddress, newKey, proposalId,
        true, 2, blockingStubFull, permissionKeyString));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    //Delete proposal list after approve
    Assert.assertTrue(PublicMethedForMutiSign.deleteProposalWithPermissionId(
        newAddress, newKey, proposalId, 2, blockingStubFull, permissionKeyString));
  }

  @Test(enabled = true)
  public void test008MutiSignGodicWithdrawBanlanceTransaction() {
    long MaintenanceTimeInterval = -1L;
    ChainParameters chainParameters = blockingStubFull
        .getChainParameters(EmptyMessage.newBuilder().build());
    Optional<ChainParameters> getChainParameters = Optional.ofNullable(chainParameters);
    logger.info(Long.toString(getChainParameters.get().getChainParameterCount()));
    for (Integer i = 0; i < getChainParameters.get().getChainParameterCount(); i++) {
      logger.info("Index is:" + i);
      logger.info(getChainParameters.get().getChainParameter(i).getKey());
      logger.info(Long.toString(getChainParameters.get().getChainParameter(i).getValue()));
      if (getChainParameters.get().getChainParameter(i).getKey()
          .equals("getMaintenanceTimeInterval")) {
        MaintenanceTimeInterval = getChainParameters.get().getChainParameter(i).getValue();
        break;
      }
    }

    try {
      Thread.sleep(MaintenanceTimeInterval);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    Assert.assertTrue(PublicMethedForMutiSign.withdrawBalance(newAddress, newKey,
        2, permissionKeyString, blockingStubFull));
  }


  /**
   * constructor.
   */
  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


