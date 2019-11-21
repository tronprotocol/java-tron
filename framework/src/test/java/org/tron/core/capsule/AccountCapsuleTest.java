package org.tron.core.capsule;

import com.google.protobuf.ByteString;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Key;
import org.tron.protos.Protocol.Permission;
import org.tron.protos.Protocol.Vote;
import org.tron.protos.contract.AssetIssueContractOuterClass.AssetIssueContract;

@Ignore
public class AccountCapsuleTest {

  private static final String dbPath = "output_accountCapsule_test";
  private static final Manager dbManager;
  private static final TronApplicationContext context;
  private static final String OWNER_ADDRESS;
  private static final String ASSET_NAME = "trx";
  private static final long TOTAL_SUPPLY = 10000L;
  private static final int TRX_NUM = 10;
  private static final int NUM = 1;
  private static final long START_TIME = 1;
  private static final long END_TIME = 2;
  private static final int VOTE_SCORE = 2;
  private static final String DESCRIPTION = "TRX";
  private static final String URL = "https://tron.network";


  static AccountCapsule accountCapsuleTest;
  static AccountCapsule accountCapsule;

  static {
    Args.setParam(new String[]{"-d", dbPath, "-w"}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    dbManager = context.getBean(Manager.class);

    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "a06a17a49648a8ad32055c06f60fa14ae46df91234";
  }


  @BeforeClass
  public static void init() {
    ByteString accountName = ByteString.copyFrom(AccountCapsuleTest.randomBytes(16));
    ByteString address = ByteString.copyFrom(AccountCapsuleTest.randomBytes(32));
    AccountType accountType = AccountType.forNumber(1);
    accountCapsuleTest = new AccountCapsule(accountName, address, accountType);
    byte[] accountByte = accountCapsuleTest.getData();
    accountCapsule = new AccountCapsule(accountByte);
    accountCapsuleTest.setBalance(1111L);
  }

  @AfterClass
  public static void removeDb() {
    Args.clearParam();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }

  public static byte[] randomBytes(int length) {
    //generate the random number
    byte[] result = new byte[length];
    new Random().nextBytes(result);
    return result;
  }

  @Test
  public void getDataTest() {
    //test AccountCapsule onstructed function
    Assert.assertEquals(accountCapsule.getInstance().getAccountName(),
        accountCapsuleTest.getInstance().getAccountName());
    Assert.assertEquals(accountCapsule.getInstance().getType(),
        accountCapsuleTest.getInstance().getType());
    Assert.assertEquals(1111, accountCapsuleTest.getBalance());
  }

  @Test
  public void addVotesTest() {
    //test addVote and getVotesList function
    ByteString voteAddress = ByteString.copyFrom(AccountCapsuleTest.randomBytes(32));
    long voteAdd = 10L;
    accountCapsuleTest.addVotes(voteAddress, voteAdd);
    List<Vote> votesList = accountCapsuleTest.getVotesList();
    for (Vote vote :
        votesList) {
      Assert.assertEquals(voteAddress, vote.getVoteAddress());
      Assert.assertEquals(voteAdd, vote.getVoteCount());
    }
  }

  @Test
  public void AssetAmountTest() {
    //test AssetAmount ,addAsset and reduceAssetAmount function

    String nameAdd = "TokenX";
    long amountAdd = 222L;
    boolean addBoolean = accountCapsuleTest
        .addAssetAmount(nameAdd.getBytes(), amountAdd);

    Assert.assertTrue(addBoolean);

    Map<String, Long> assetMap = accountCapsuleTest.getAssetMap();
    for (Map.Entry<String, Long> entry : assetMap.entrySet()) {
      Assert.assertEquals(nameAdd, entry.getKey());
      Assert.assertEquals(amountAdd, entry.getValue().longValue());
    }
    long amountReduce = 22L;

    boolean reduceBoolean = accountCapsuleTest
        .reduceAssetAmount(ByteArray.fromString("TokenX"), amountReduce);
    Assert.assertTrue(reduceBoolean);

    Map<String, Long> assetMapAfter = accountCapsuleTest.getAssetMap();
    for (Map.Entry<String, Long> entry : assetMapAfter.entrySet()) {
      Assert.assertEquals(nameAdd, entry.getKey());
      Assert.assertEquals(amountAdd - amountReduce, entry.getValue().longValue());
    }
    String key = nameAdd;
    long value = 11L;
    boolean addAsssetBoolean = accountCapsuleTest.addAsset(key.getBytes(), value);
    Assert.assertFalse(addAsssetBoolean);

    String keyName = "TokenTest";
    long amountValue = 33L;
    boolean addAsssetTrue = accountCapsuleTest.addAsset(keyName.getBytes(), amountValue);
    Assert.assertTrue(addAsssetTrue);
  }

  /**
   * SameTokenName close, test assert amountV2 function
   */
  @Test
  public void sameTokenNameCloseAssertAmountV2test() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    long id = dbManager.getDynamicPropertiesStore().getTokenIdNum() + 1;
    dbManager.getDynamicPropertiesStore().saveTokenIdNum(id);

    AssetIssueContract assetIssueContract =
        AssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setName(ByteString.copyFrom(ByteArray.fromString(ASSET_NAME)))
            .setId(Long.toString(id))
            .setTotalSupply(TOTAL_SUPPLY)
            .setTrxNum(TRX_NUM)
            .setNum(NUM)
            .setStartTime(START_TIME)
            .setEndTime(END_TIME)
            .setVoteScore(VOTE_SCORE)
            .setDescription(ByteString.copyFrom(ByteArray.fromString(DESCRIPTION)))
            .setUrl(ByteString.copyFrom(ByteArray.fromString(URL)))
            .build();
    AssetIssueCapsule assetIssueCapsule = new AssetIssueCapsule(assetIssueContract);
    dbManager.getAssetIssueStore().put(assetIssueCapsule.createDbKey(), assetIssueCapsule);

    AssetIssueContract assetIssueContract2 =
        AssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setName(ByteString.copyFrom(ByteArray.fromString("abc")))
            .setId(Long.toString(id + 1))
            .setTotalSupply(TOTAL_SUPPLY)
            .setTrxNum(TRX_NUM)
            .setNum(NUM)
            .setStartTime(START_TIME)
            .setEndTime(END_TIME)
            .setVoteScore(VOTE_SCORE)
            .setDescription(ByteString.copyFrom(ByteArray.fromString(DESCRIPTION)))
            .setUrl(ByteString.copyFrom(ByteArray.fromString(URL)))
            .build();
    AssetIssueCapsule assetIssueCapsule2 = new AssetIssueCapsule(assetIssueContract2);
    dbManager.getAssetIssueStore().put(assetIssueCapsule2.createDbKey(), assetIssueCapsule2);

    AccountCapsule accountCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8("owner"),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            AccountType.Normal,
            10000);
    accountCapsule.addAsset(ByteArray.fromString(ASSET_NAME), 1000L);
    dbManager.getAccountStore().put(accountCapsule.getAddress().toByteArray(), accountCapsule);

    accountCapsule.addAssetV2(ByteArray.fromString(String.valueOf(id)), 1000L);
    Assert.assertEquals(accountCapsule.getAssetMap().get(ASSET_NAME).longValue(), 1000L);
    Assert.assertEquals(accountCapsule.getAssetMapV2().get(String.valueOf(id)).longValue(),
        1000L);

    //assetBalanceEnoughV2
    Assert.assertTrue(accountCapsule.assetBalanceEnoughV2(ByteArray.fromString(ASSET_NAME),
        999, dbManager.getDynamicPropertiesStore()));
    Assert.assertFalse(accountCapsule.assetBalanceEnoughV2(ByteArray.fromString(ASSET_NAME),
        1001, dbManager.getDynamicPropertiesStore()));

    //reduceAssetAmountV2
    Assert.assertTrue(accountCapsule.reduceAssetAmountV2(ByteArray.fromString(ASSET_NAME),
        999, dbManager.getDynamicPropertiesStore(), dbManager.getAssetIssueStore()));
    Assert.assertFalse(accountCapsule.reduceAssetAmountV2(ByteArray.fromString(ASSET_NAME),
        0, dbManager.getDynamicPropertiesStore(), dbManager.getAssetIssueStore()));
    Assert.assertFalse(accountCapsule.reduceAssetAmountV2(ByteArray.fromString(ASSET_NAME),
        1001, dbManager.getDynamicPropertiesStore(), dbManager.getAssetIssueStore()));
    Assert.assertFalse(accountCapsule.reduceAssetAmountV2(ByteArray.fromString("abc"),
        1001, dbManager.getDynamicPropertiesStore(), dbManager.getAssetIssueStore()));

    //addAssetAmountV2
    Assert.assertTrue(accountCapsule.addAssetAmountV2(ByteArray.fromString(ASSET_NAME),
        500, dbManager.getDynamicPropertiesStore(), dbManager.getAssetIssueStore()));
    // 1000-999 +500
    Assert.assertEquals(accountCapsule.getAssetMap().get(ASSET_NAME).longValue(), 501L);
    Assert.assertTrue(accountCapsule.addAssetAmountV2(ByteArray.fromString("abc"),
        500, dbManager.getDynamicPropertiesStore(), dbManager.getAssetIssueStore()));
    Assert.assertEquals(accountCapsule.getAssetMap().get("abc").longValue(), 500L);
  }

  /**
   * SameTokenName open, test assert amountV2 function
   */
  @Test
  public void sameTokenNameOpenAssertAmountV2test() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    long id = dbManager.getDynamicPropertiesStore().getTokenIdNum() + 1;
    dbManager.getDynamicPropertiesStore().saveTokenIdNum(id);

    AssetIssueContract assetIssueContract =
        AssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setName(ByteString.copyFrom(ByteArray.fromString(ASSET_NAME)))
            .setId(Long.toString(id))
            .setTotalSupply(TOTAL_SUPPLY)
            .setTrxNum(TRX_NUM)
            .setNum(NUM)
            .setStartTime(START_TIME)
            .setEndTime(END_TIME)
            .setVoteScore(VOTE_SCORE)
            .setDescription(ByteString.copyFrom(ByteArray.fromString(DESCRIPTION)))
            .setUrl(ByteString.copyFrom(ByteArray.fromString(URL)))
            .build();
    AssetIssueCapsule assetIssueCapsule = new AssetIssueCapsule(assetIssueContract);
    dbManager.getAssetIssueV2Store().put(assetIssueCapsule.createDbV2Key(), assetIssueCapsule);

    AssetIssueContract assetIssueContract2 =
        AssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setName(ByteString.copyFrom(ByteArray.fromString("abc")))
            .setId(Long.toString(id + 1))
            .setTotalSupply(TOTAL_SUPPLY)
            .setTrxNum(TRX_NUM)
            .setNum(NUM)
            .setStartTime(START_TIME)
            .setEndTime(END_TIME)
            .setVoteScore(VOTE_SCORE)
            .setDescription(ByteString.copyFrom(ByteArray.fromString(DESCRIPTION)))
            .setUrl(ByteString.copyFrom(ByteArray.fromString(URL)))
            .build();
    AssetIssueCapsule assetIssueCapsule2 = new AssetIssueCapsule(assetIssueContract2);
    dbManager.getAssetIssueV2Store().put(assetIssueCapsule2.createDbV2Key(), assetIssueCapsule2);

    AccountCapsule accountCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8("owner"),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            AccountType.Normal,
            10000);
    accountCapsule.addAssetV2(ByteArray.fromString(String.valueOf(id)), 1000L);
    dbManager.getAccountStore().put(accountCapsule.getAddress().toByteArray(), accountCapsule);
    Assert.assertEquals(accountCapsule.getAssetMapV2().get(String.valueOf(id)).longValue(),
        1000L);

    //assetBalanceEnoughV2
    Assert.assertTrue(accountCapsule.assetBalanceEnoughV2(ByteArray.fromString(String.valueOf(id)),
        999, dbManager.getDynamicPropertiesStore()));

    Assert.assertFalse(accountCapsule.assetBalanceEnoughV2(ByteArray.fromString(String.valueOf(id)),
        1001, dbManager.getDynamicPropertiesStore()));

    //reduceAssetAmountV2
    Assert.assertTrue(accountCapsule.reduceAssetAmountV2(ByteArray.fromString(String.valueOf(id)),
        999, dbManager.getDynamicPropertiesStore(), dbManager.getAssetIssueStore()));
    Assert.assertFalse(accountCapsule.reduceAssetAmountV2(ByteArray.fromString(String.valueOf(id)),
        0, dbManager.getDynamicPropertiesStore(), dbManager.getAssetIssueStore()));
    Assert.assertFalse(accountCapsule.reduceAssetAmountV2(ByteArray.fromString(String.valueOf(id)),
        1001, dbManager.getDynamicPropertiesStore(), dbManager.getAssetIssueStore()));
    // abc
    Assert.assertFalse(
        accountCapsule.reduceAssetAmountV2(ByteArray.fromString(String.valueOf(id + 1)),
            1001, dbManager.getDynamicPropertiesStore(), dbManager.getAssetIssueStore()));

    //addAssetAmountV2
    Assert.assertTrue(accountCapsule.addAssetAmountV2(ByteArray.fromString(String.valueOf(id)),
        500, dbManager.getDynamicPropertiesStore(), dbManager.getAssetIssueStore()));
    // 1000-999 +500
    Assert.assertEquals(accountCapsule.getAssetMapV2().get(String.valueOf(id)).longValue(),
        501L);
    //abc
    Assert.assertTrue(accountCapsule.addAssetAmountV2(ByteArray.fromString(String.valueOf(id + 1)),
        500, dbManager.getDynamicPropertiesStore(), dbManager.getAssetIssueStore()));
    Assert
        .assertEquals(accountCapsule.getAssetMapV2().get(String.valueOf(id + 1)).longValue(),
            500L);
  }

  @Test
  public void witnessPermissionTest() {
    AccountCapsule accountCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8("owner"),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            AccountType.Normal,
            10000);

    Assert.assertTrue(
        Arrays.equals(ByteArray.fromHexString(OWNER_ADDRESS),
            accountCapsule.getWitnessPermissionAddress()));

    String witnessPermissionAddress =
        Wallet.getAddressPreFixString() + "cc6a17a49648a8ad32055c06f60fa14ae46df912cc";
    accountCapsule = new AccountCapsule(accountCapsule.getInstance().toBuilder()
        .setWitnessPermission(Permission.newBuilder().addKeys(Key.newBuilder()
            .setAddress(ByteString.copyFrom(ByteArray.fromHexString(witnessPermissionAddress)))
            .build()).build()).build());

    Assert.assertTrue(
        Arrays.equals(ByteArray.fromHexString(witnessPermissionAddress),
            accountCapsule.getWitnessPermissionAddress()));
  }
}