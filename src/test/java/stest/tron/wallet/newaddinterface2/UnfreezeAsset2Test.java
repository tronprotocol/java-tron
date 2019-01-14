package stest.tron.wallet.newaddinterface2;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.math.BigInteger;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.api.GrpcAPI.Return;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Contract;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.TransactionUtils;

@Slf4j
public class UnfreezeAsset2Test {

  //testng001、testng002、testng003、testng004
  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");

  /*  //testng001、testng002、testng003、testng004
  private static final byte[] fromAddress = Base58
      .decodeFromBase58Check("THph9K2M2nLvkianrMGswRhz5hjSA9fuH7");
  private static final byte[] toAddress = Base58
      .decodeFromBase58Check("TV75jZpdmP2juMe1dRwGrwpV6AMU6mr1EU");*/

  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);

  //get account
  ECKey ecKey = new ECKey(Utils.getRandom());
  byte[] lowBalAddress = ecKey.getAddress();
  String lowBalTest = ByteArray.toHexString(ecKey.getPrivKeyBytes());

  //get account
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] lowBalAddress2 = ecKey2.getAddress();
  String lowBalTest2 = ByteArray.toHexString(ecKey2.getPrivKeyBytes());


  private static final long now = System.currentTimeMillis();
  private static final String name = "testAssetIssue003_" + Long.toString(now);
  private static final String shortname = "a";
  private static final String tooLongName = "qazxswedcvfrtgbnhyujmkiolpoiuytre";
  private static final String chineseAssetIssuename = "中文都名字";
  private static final String tooLongDescription =
      "1qazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqa"
          + "zxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvq"
          + "azxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcv";
  private static final String tooLongUrl =
      "qaswqaswqaswqaswqaswqaswqaswqaswqaswqaswqaswqaswqaswqasw1qazxswedcvqazxswedcv"
          + "qazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedc"
          + "vqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqaz"
          + "xswedcvqazxswedcvqazxswedcvqazxswedcv";
  private static final long totalSupply = now;
  String description = "just-test";
  String url = "https://github.com/tronprotocol/wallet-cli/";

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  @BeforeClass(enabled = true)
  public void beforeClass() {

    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);


  }

  @Test(enabled = true)
  public void testGetAllAssetIssue2() {
    Return ret1 = PublicMethed.sendcoin2(lowBalAddress, 2124500000L,
        fromAddress, testKey002, blockingStubFull);
    Assert.assertEquals(ret1.getCode(), Return.response_code.SUCCESS);
    Assert.assertEquals(ret1.getMessage().toStringUtf8(), "");

    ret1 = PublicMethed.sendcoin2(lowBalAddress2, 21240500000L,
        fromAddress, testKey002, blockingStubFull);
    Assert.assertEquals(ret1.getCode(), Return.response_code.SUCCESS);
    Assert.assertEquals(ret1.getMessage().toStringUtf8(), "");
    ByteString addressBS1 = ByteString.copyFrom(fromAddress);
    Account request1 = Account.newBuilder().setAddress(addressBS1).build();
    GrpcAPI.AssetIssueList assetIssueList1 = blockingStubFull.getAssetIssueByAccount(request1);
    Optional<GrpcAPI.AssetIssueList> queryAssetByAccount = Optional.ofNullable(assetIssueList1);

    //if (queryAssetByAccount.get().getAssetIssueCount() == 0) {
    Long start = System.currentTimeMillis() + 100000;
    Long end = System.currentTimeMillis() + 1000000000;
    //Freeze amount is large than total supply, create asset issue failed.
    ret1 = PublicMethed.createAssetIssue2(lowBalAddress, name, totalSupply, 1, 10,
        start, end, 2, description, url, 10000L, 10000L,
        9000000000000000000L, 1L, lowBalTest, blockingStubFull);
    Assert.assertEquals(ret1.getCode(), GrpcAPI.Return.response_code.CONTRACT_VALIDATE_ERROR);
    Assert.assertEquals(ret1.getMessage().toStringUtf8(),
        "contract validate error : Frozen supply cannot exceed total supply");
    //Freeze day is 0, create failed
    ret1 = PublicMethed.createAssetIssue2(lowBalAddress, name, totalSupply, 1, 10,
        start, end, 2, description, url, 10000L, 10000L,
        100L, 0L, lowBalTest, blockingStubFull);
    Assert.assertEquals(ret1.getCode(), GrpcAPI.Return.response_code.CONTRACT_VALIDATE_ERROR);
    Assert.assertEquals(ret1.getMessage().toStringUtf8(), "contract validate error : "
        + "frozenDuration must be less than 3652 days and more than 1 days");
    //Freeze amount is 0, create failed
    ret1 = PublicMethed.createAssetIssue2(lowBalAddress, name, totalSupply, 1, 10,
        start, end, 2, description, url, 10000L, 10000L,
        0L, 1L, lowBalTest, blockingStubFull);
    Assert.assertEquals(ret1.getCode(), GrpcAPI.Return.response_code.CONTRACT_VALIDATE_ERROR);
    Assert.assertEquals(ret1.getMessage().toStringUtf8(),
        "contract validate error : Frozen supply must be greater than 0!");
    //Freeze day is -1, create failed
    ret1 = PublicMethed.createAssetIssue2(lowBalAddress, name, totalSupply, 1, 10,
        start, end, 2, description, url, 1000L, 1000L,
        1000L, -1L, lowBalTest, blockingStubFull);
    Assert.assertEquals(ret1.getCode(), GrpcAPI.Return.response_code.CONTRACT_VALIDATE_ERROR);
    Assert.assertEquals(ret1.getMessage().toStringUtf8(), "contract validate error : "
        + "frozenDuration must be less than 3652 days and more than 1 days");
    //Freeze amount is -1, create failed
    ret1 = PublicMethed.createAssetIssue2(lowBalAddress, name, totalSupply, 1, 10,
        start, end, 2, description, url, 10000L, 10000L,
        -1L, 1L, lowBalTest, blockingStubFull);
    Assert.assertEquals(ret1.getCode(), GrpcAPI.Return.response_code.CONTRACT_VALIDATE_ERROR);
    Assert.assertEquals(ret1.getMessage().toStringUtf8(),
        "contract validate error : Frozen supply must be greater than 0!");
    //Freeze day is 3653(10 years + 1 day), create failed
    ret1 = PublicMethed.createAssetIssue2(lowBalAddress, name, totalSupply, 1, 10,
        start, end, 2, description, url, 10000L, 10000L,
        1L, 3653L, lowBalTest, blockingStubFull);
    Assert.assertEquals(ret1.getCode(), GrpcAPI.Return.response_code.CONTRACT_VALIDATE_ERROR);
    Assert.assertEquals(ret1.getMessage().toStringUtf8(), "contract validate error : "
        + "frozenDuration must be less than 3652 days and more than 1 days");
    //Start time is late than end time.
    ret1 = PublicMethed.createAssetIssue2(lowBalAddress, name, totalSupply, 1, 10,
        end, start, 2, description, url, 10000L, 10000L,
        1L, 2L, lowBalTest, blockingStubFull);
    Assert.assertEquals(ret1.getCode(), GrpcAPI.Return.response_code.CONTRACT_VALIDATE_ERROR);
    Assert.assertEquals(ret1.getMessage().toStringUtf8(),
        "contract validate error : End time should be greater than start time");
    //Start time is early than currently time.
    ret1 = PublicMethed.createAssetIssue2(lowBalAddress, name, totalSupply, 1, 10,
        start - 1000000L, end, 2, description, url, 10000L,
        10000L, 1L, 2L, lowBalTest, blockingStubFull);
    Assert.assertEquals(ret1.getCode(), GrpcAPI.Return.response_code.CONTRACT_VALIDATE_ERROR);
    Assert.assertEquals(ret1.getMessage().toStringUtf8(),
        "contract validate error : Start time should be greater than HeadBlockTime");
    //totalSupply is zero.
    ret1 = PublicMethed.createAssetIssue2(lowBalAddress, name, 0L, 1, 10,
        start, end, 2, description, url, 10000L, 10000L,
        1L, 3652L, lowBalTest, blockingStubFull);
    Assert.assertEquals(ret1.getCode(), GrpcAPI.Return.response_code.CONTRACT_VALIDATE_ERROR);
    Assert.assertEquals(ret1.getMessage().toStringUtf8(),
        "contract validate error : TotalSupply must greater than 0!");
    //Totalsupply is -1.
    ret1 = PublicMethed.createAssetIssue2(lowBalAddress, name, -1L, 1, 10,
        start, end, 2, description, url, 10000L, 10000L,
        1L, 3652L, lowBalTest, blockingStubFull);
    Assert.assertEquals(ret1.getCode(), GrpcAPI.Return.response_code.CONTRACT_VALIDATE_ERROR);
    Assert.assertEquals(ret1.getMessage().toStringUtf8(),
        "contract validate error : TotalSupply must greater than 0!");
    //TrxNum is zero.
    ret1 = PublicMethed.createAssetIssue2(lowBalAddress, name, totalSupply, 0, 10,
        start, end, 2, description, url, 10000L, 10000L,
        1L, 3652L, lowBalTest, blockingStubFull);
    Assert.assertEquals(ret1.getCode(), GrpcAPI.Return.response_code.CONTRACT_VALIDATE_ERROR);
    Assert.assertEquals(ret1.getMessage().toStringUtf8(),
        "contract validate error : TrxNum must greater than 0!");
    //TrxNum is -1.
    ret1 = PublicMethed.createAssetIssue2(lowBalAddress, name, totalSupply, -1, 10,
        start, end, 2, description, url, 10000L, 10000L,
        1L, 3652L, lowBalTest, blockingStubFull);
    Assert.assertEquals(ret1.getCode(), GrpcAPI.Return.response_code.CONTRACT_VALIDATE_ERROR);
    Assert.assertEquals(ret1.getMessage().toStringUtf8(),
        "contract validate error : TrxNum must greater than 0!");
    //IcoNum is 0.
    ret1 = PublicMethed.createAssetIssue2(lowBalAddress, name, totalSupply, 1, 0,
        start, end, 2, description, url, 10000L, 10000L,
        1L, 3652L, testKey002, blockingStubFull);
    Assert.assertEquals(ret1.getCode(), GrpcAPI.Return.response_code.CONTRACT_VALIDATE_ERROR);
    Assert.assertEquals(ret1.getMessage().toStringUtf8(),
        "contract validate error : Num must greater than 0!");
    //IcoNum is -1.
    ret1 = PublicMethed.createAssetIssue2(lowBalAddress, name, totalSupply, 1, -1,
        start, end, 2, description, url, 10000L, 10000L,
        1L, 3652L, lowBalTest, blockingStubFull);
    Assert.assertEquals(ret1.getCode(), GrpcAPI.Return.response_code.CONTRACT_VALIDATE_ERROR);
    Assert.assertEquals(ret1.getMessage().toStringUtf8(),
        "contract validate error : Num must greater than 0!");
    //The asset issue name is null.
    ret1 = PublicMethed.createAssetIssue2(lowBalAddress, "", totalSupply, 1, 10,
        start, end, 2, description, url, 10000L, 10000L,
        1L, 3652L, lowBalTest, blockingStubFull);
    Assert.assertEquals(ret1.getCode(), GrpcAPI.Return.response_code.CONTRACT_VALIDATE_ERROR);
    Assert.assertEquals(ret1.getMessage().toStringUtf8(),
        "contract validate error : Invalid assetName");
    //The asset issue name is large than 33 char.
    ret1 = PublicMethed.createAssetIssue2(lowBalAddress, tooLongName, totalSupply, 1, 10,
        start, end, 2, description, url, 10000L, 10000L,
        1L, 3652L, lowBalTest, blockingStubFull);
    Assert.assertEquals(ret1.getCode(), GrpcAPI.Return.response_code.CONTRACT_VALIDATE_ERROR);
    Assert.assertEquals(ret1.getMessage().toStringUtf8(),
        "contract validate error : Invalid assetName");
    //The asset issue name is chinese name.
    ret1 = PublicMethed.createAssetIssue2(lowBalAddress, chineseAssetIssuename, totalSupply, 1,
        10, start, end, 2, description, url, 10000L,
        10000L, 1L, 3652L, lowBalTest, blockingStubFull);
    Assert.assertEquals(ret1.getCode(), GrpcAPI.Return.response_code.CONTRACT_VALIDATE_ERROR);
    Assert.assertEquals(ret1.getMessage().toStringUtf8(),
        "contract validate error : Invalid assetName");
    //The URL is null.
    ret1 = PublicMethed.createAssetIssue2(lowBalAddress, name, totalSupply, 1, 10,
        start, end, 2, description, "", 10000L, 10000L,
        1L, 3652L, lowBalTest, blockingStubFull);
    Assert.assertEquals(ret1.getCode(), GrpcAPI.Return.response_code.CONTRACT_VALIDATE_ERROR);
    Assert.assertEquals(ret1.getMessage().toStringUtf8(), "contract validate error : Invalid url");
    //The URL is too long.
    ret1 = PublicMethed.createAssetIssue2(lowBalAddress, name, totalSupply, 1, 10,
        start, end, 2, description, tooLongUrl, 10000L, 10000L,
        1L, 3652L, lowBalTest, blockingStubFull);
    Assert.assertEquals(ret1.getCode(), GrpcAPI.Return.response_code.CONTRACT_VALIDATE_ERROR);
    Assert.assertEquals(ret1.getMessage().toStringUtf8(), "contract validate error : Invalid url");
    //The description is too long, create failed.
    ret1 = PublicMethed.createAssetIssue2(lowBalAddress, name, totalSupply, 1, 10,
        start, end, 2, tooLongDescription, url, 10000L,
        10000L, 1L, 3652L, lowBalTest, blockingStubFull);
    Assert.assertEquals(ret1.getCode(), GrpcAPI.Return.response_code.CONTRACT_VALIDATE_ERROR);
    Assert.assertEquals(ret1.getMessage().toStringUtf8(),
        "contract validate error : Invalid description");

    //FreezeBalance
    Assert.assertTrue(PublicMethed.freezeBalance(lowBalAddress, 10000000L, 3, lowBalTest,
        blockingStubFull));
    //Create success
    start = System.currentTimeMillis() + 6000;
    end = System.currentTimeMillis() + 1000000000;
    ret1 = PublicMethed.createAssetIssue2(lowBalAddress, name, totalSupply, 1, 10,
        start, end, 2, description, url, 10000L, 10000L,
        1L, 3652L, lowBalTest, blockingStubFull);
    Assert.assertEquals(ret1.getCode(), Return.response_code.SUCCESS);
    Assert.assertEquals(ret1.getMessage().toStringUtf8(), "");
    //Test not in the duration time, participate failed.
    ret1 = PublicMethed.participateAssetIssue2(lowBalAddress, name.getBytes(), 1L,
        toAddress, lowBalTest, blockingStubFull);
    Assert.assertEquals(ret1.getCode(), GrpcAPI.Return.response_code.CONTRACT_VALIDATE_ERROR);
    Assert.assertEquals(ret1.getMessage().toStringUtf8(),
        "contract validate error : No longer valid period!");
    //Test another address try to create the same name asset issue, create failed.
    ret1 = PublicMethed.createAssetIssue2(lowBalAddress2, name, totalSupply, 1, 10,
        start, end, 2, description, url, 10000L, 10000L,
        1L, 3652L, lowBalTest2, blockingStubFull);
    Assert.assertEquals(ret1.getCode(), Return.response_code.CONTRACT_VALIDATE_ERROR);
    Assert.assertEquals(ret1.getMessage().toStringUtf8(), "contract validate error : Token exists");

    try {
      Thread.sleep(4000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    GrpcAPI.AssetIssueList assetIssueList = blockingStubFull
        .getAssetIssueList(GrpcAPI.EmptyMessage.newBuilder().build());
    logger.info(Integer.toString(assetIssueList.getAssetIssue(0).getFrozenSupplyCount()));
    Assert.assertTrue(assetIssueList.getAssetIssue(0).getFrozenSupplyCount() == 1);
    //Assert.assertTrue(assetIssueList.getAssetIssue(j).getFrozenSupplyCount() > 0);
    Assert.assertTrue(assetIssueList.getAssetIssue(0).getFrozenSupply(0).getFrozenAmount() > 0);
    Assert.assertTrue(assetIssueList.getAssetIssue(0).getFrozenSupply(0).getFrozenDays() > 0);

    //Test one account only can create one asset issue.
    start = System.currentTimeMillis() + 3000;
    end = System.currentTimeMillis() + 1000000000;
    ret1 = PublicMethed.createAssetIssue2(lowBalAddress, shortname, totalSupply, 1, 10,
        start, end, 2, description, url, 10000L, 10000L,
        1L, 3652L, testKey002, blockingStubFull);
    Assert.assertEquals(ret1.getCode(), GrpcAPI.Return.response_code.CONTRACT_VALIDATE_ERROR);
    Assert.assertEquals(ret1.getMessage().toStringUtf8(),
        "contract validate error : An account can only issue one asset");
    logger.info("FROM ADDRESS create asset issue in this case!!!");

    assetIssueList = blockingStubFull
        .getAssetIssueList(GrpcAPI.EmptyMessage.newBuilder().build());
    Assert.assertTrue(assetIssueList.getAssetIssueCount() >= 1);
    for (Integer j = 0; j < assetIssueList.getAssetIssueCount(); j++) {
      Assert.assertFalse(assetIssueList.getAssetIssue(j).getOwnerAddress().isEmpty());
      Assert.assertFalse(assetIssueList.getAssetIssue(j).getName().isEmpty());
      Assert.assertFalse(assetIssueList.getAssetIssue(j).getUrl().isEmpty());
      Assert.assertTrue(assetIssueList.getAssetIssue(j).getTotalSupply() > 0);
      logger.info("test get all assetissue");
    }

    //Improve coverage.
    assetIssueList.equals(assetIssueList);
    assetIssueList.equals(null);
    GrpcAPI.AssetIssueList newAssetIssueList = blockingStubFull
        .getAssetIssueList(GrpcAPI.EmptyMessage.newBuilder().build());
    assetIssueList.equals(newAssetIssueList);
    assetIssueList.hashCode();
    assetIssueList.getSerializedSize();

  }

  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  public Boolean createAssetIssue(byte[] address, String name, Long totalSupply, Integer trxNum,
      Integer icoNum, Long startTime, Long endTime,
      Integer voteScore, String description, String url, Long fronzenAmount, Long frozenDay,
      String priKey) {
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;
    Account search = queryAccount(ecKey, blockingStubFull);

    try {
      Contract.AssetIssueContract.Builder builder = Contract.AssetIssueContract.newBuilder();
      builder.setOwnerAddress(ByteString.copyFrom(address));
      builder.setName(ByteString.copyFrom(name.getBytes()));
      builder.setTotalSupply(totalSupply);
      builder.setTrxNum(trxNum);
      builder.setNum(icoNum);
      builder.setStartTime(startTime);
      builder.setEndTime(endTime);
      builder.setVoteScore(voteScore);
      builder.setDescription(ByteString.copyFrom(description.getBytes()));
      builder.setFreeAssetNetLimit(10000);
      builder.setPublicFreeAssetNetLimit(10000);
      builder.setUrl(ByteString.copyFrom(url.getBytes()));
      Contract.AssetIssueContract.FrozenSupply.Builder frozenBuilder =
          Contract.AssetIssueContract.FrozenSupply
              .newBuilder();
      frozenBuilder.setFrozenAmount(fronzenAmount);
      frozenBuilder.setFrozenDays(frozenDay);
      builder.addFrozenSupply(0, frozenBuilder);

      Transaction transaction = blockingStubFull.createAssetIssue(builder.build());
      if (transaction == null || transaction.getRawData().getContractCount() == 0) {
        logger.info("transaction == null");
        return false;
      }
      transaction = signTransaction(ecKey, transaction);
      Return response = blockingStubFull.broadcastTransaction(transaction);
      if (response.getResult() == false) {
        logger.info(ByteArray.toStr(response.getMessage().toByteArray()));
        return false;
      } else {
        logger.info(name);
        return true;
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      return false;
    }
  }

  public Account queryAccount(ECKey ecKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
    byte[] address;
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

  public static String loadPubKey() {
    char[] buf = new char[0x100];
    return String.valueOf(buf, 32, 130);
  }

  public byte[] getAddress(ECKey ecKey) {
    return ecKey.getAddress();
  }

  public Account grpcQueryAccount(byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    ByteString addressBs = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBs).build();
    return blockingStubFull.getAccount(request);
  }

  public Block getBlock(long blockNum, WalletGrpc.WalletBlockingStub blockingStubFull) {
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    builder.setNum(blockNum);
    return blockingStubFull.getBlockByNum(builder.build());

  }

  private Transaction signTransaction(ECKey ecKey, Transaction transaction) {
    if (ecKey == null || ecKey.getPrivKey() == null) {
      logger.warn("Warning: Can't sign,there is no private key !!");
      return null;
    }
    transaction = TransactionUtils.setTimestamp(transaction);
    return TransactionUtils.sign(transaction, ecKey);
  }

  public boolean transferAsset(byte[] to, byte[] assertName, long amount, byte[] address,
      String priKey) {
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    Contract.TransferAssetContract.Builder builder = Contract.TransferAssetContract.newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsName = ByteString.copyFrom(assertName);
    ByteString bsOwner = ByteString.copyFrom(address);
    builder.setToAddress(bsTo);
    builder.setAssetName(bsName);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);

    Contract.TransferAssetContract contract = builder.build();
    Transaction transaction = blockingStubFull.transferAsset(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }
    transaction = signTransaction(ecKey, transaction);
    Return response = blockingStubFull.broadcastTransaction(transaction);
    if (response.getResult() == false) {
      return false;
    } else {
      Account search = queryAccount(ecKey, blockingStubFull);
      return true;
    }

  }

  public boolean unFreezeAsset(byte[] addRess, String priKey) {
    byte[] address = addRess;

    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    Contract.UnfreezeAssetContract.Builder builder = Contract.UnfreezeAssetContract
        .newBuilder();
    ByteString byteAddreess = ByteString.copyFrom(address);

    builder.setOwnerAddress(byteAddreess);

    Contract.UnfreezeAssetContract contract = builder.build();

    Transaction transaction = blockingStubFull.unfreezeAsset(contract);

    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }

    transaction = TransactionUtils.setTimestamp(transaction);
    transaction = TransactionUtils.sign(transaction, ecKey);
    Return response = blockingStubFull.broadcastTransaction(transaction);
    if (response.getResult() == false) {
      logger.info(ByteArray.toStr(response.getMessage().toByteArray()));
      return false;
    } else {
      return true;
    }
  }

  public boolean unFreezeAsset2(byte[] addRess, String priKey) {
    byte[] address = addRess;

    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    Contract.UnfreezeAssetContract.Builder builder = Contract.UnfreezeAssetContract
        .newBuilder();
    ByteString byteAddreess = ByteString.copyFrom(address);

    builder.setOwnerAddress(byteAddreess);

    Contract.UnfreezeAssetContract contract = builder.build();

    GrpcAPI.TransactionExtention transactionExtention = blockingStubFull.unfreezeAsset2(contract);
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

    transaction = TransactionUtils.setTimestamp(transaction);
    transaction = TransactionUtils.sign(transaction, ecKey);
    Return response = blockingStubFull.broadcastTransaction(transaction);
    if (response.getResult() == false) {
      logger.info(ByteArray.toStr(response.getMessage().toByteArray()));
      return false;
    } else {
      return true;
    }
  }

  public boolean participateAssetIssue(byte[] to, byte[] assertName, long amount, byte[] from,
      String priKey) {
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    Contract.ParticipateAssetIssueContract.Builder builder = Contract.ParticipateAssetIssueContract
        .newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsName = ByteString.copyFrom(assertName);
    ByteString bsOwner = ByteString.copyFrom(from);
    builder.setToAddress(bsTo);
    builder.setAssetName(bsName);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);
    Contract.ParticipateAssetIssueContract contract = builder.build();

    Transaction transaction = blockingStubFull.participateAssetIssue(contract);
    transaction = signTransaction(ecKey, transaction);
    Return response = blockingStubFull.broadcastTransaction(transaction);
    if (response.getResult() == false) {
      logger.info(ByteArray.toStr(response.getMessage().toByteArray()));
      return false;
    } else {
      logger.info(name);
      return true;
    }
  }

}


