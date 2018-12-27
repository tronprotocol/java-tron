package org.tron.core;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.BandwidthProcessor;
import org.tron.core.db.Manager;
import org.tron.core.db.TransactionTrace;
import org.tron.core.exception.AccountResourceInsufficientException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.TooBigTransactionResultException;
import org.tron.protos.Contract;
import org.tron.protos.Contract.AssetIssueContract;
import org.tron.protos.Contract.TransferAssetContract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.AccountType;

@Slf4j
public class BandwidthProcessorTest {

  private static Manager dbManager;
  private static final String dbPath = "output_bandwidth_test";
  private static TronApplicationContext context;
  private static final String ASSET_NAME;
  private static final String ASSET_NAME_V2;
  private static final String OWNER_ADDRESS;
  private static final String ASSET_ADDRESS;
  private static final String ASSET_ADDRESS_V2;
  private static final String TO_ADDRESS;
  private static final long TOTAL_SUPPLY = 10000000000000L;
  private static final int TRX_NUM = 2;
  private static final int NUM = 2147483647;
  private static final int VOTE_SCORE = 2;
  private static final String DESCRIPTION = "TRX";
  private static final String URL = "https://tron.network";
  private static long START_TIME;
  private static long END_TIME;


  static {
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    ASSET_NAME = "test_token";
    ASSET_NAME_V2 = "2";
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
    TO_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    ASSET_ADDRESS = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a3456";
    ASSET_ADDRESS_V2 = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a7890";
    START_TIME = DateTime.now().minusDays(1).getMillis();
    END_TIME = DateTime.now().getMillis();
  }

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
  }

  /**
   * Release resources.
   */
  @AfterClass
  public static void destroy() {
    Args.clearParam();
    context.destroy();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
  }

  /**
   * create temp Capsule test need.
   */
  @Before
  public void createCapsule() {
    AssetIssueCapsule assetIssueCapsule = new AssetIssueCapsule(getAssetIssueContract());
    assetIssueCapsule.setId("1");
    dbManager
        .getAssetIssueStore()
        .put(
            ByteArray.fromString(ASSET_NAME),
            assetIssueCapsule);
    dbManager
        .getAssetIssueV2Store()
        .put(
            ByteArray.fromString("1"),
            assetIssueCapsule);

    AssetIssueCapsule assetIssueCapsuleV2 = new AssetIssueCapsule(getAssetIssueV2Contract());
    dbManager
        .getAssetIssueV2Store()
        .put(
            ByteArray.fromString(ASSET_NAME_V2),
            assetIssueCapsuleV2);


    AccountCapsule ownerCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8("owner"),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            AccountType.Normal,
            0L);
    ownerCapsule.addAsset(ASSET_NAME.getBytes(), 100L);
    ownerCapsule.addAsset(ASSET_NAME_V2.getBytes(), 100L);

    AccountCapsule toAccountCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8("toAccount"),
            ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)),
            AccountType.Normal,
            0L);

    AccountCapsule assetCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8("asset"),
            ByteString.copyFrom(ByteArray.fromHexString(ASSET_ADDRESS)),
            AccountType.AssetIssue,
            dbManager.getDynamicPropertiesStore().getAssetIssueFee());

    AccountCapsule assetCapsule2 =
        new AccountCapsule(
            ByteString.copyFromUtf8("asset2"),
            ByteString.copyFrom(ByteArray.fromHexString(ASSET_ADDRESS_V2)),
            AccountType.AssetIssue,
            dbManager.getDynamicPropertiesStore().getAssetIssueFee());

    dbManager.getAccountStore().reset();
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);
    dbManager.getAccountStore().put(toAccountCapsule.getAddress().toByteArray(), toAccountCapsule);
    dbManager.getAccountStore().put(assetCapsule.getAddress().toByteArray(), assetCapsule);
    dbManager.getAccountStore().put(assetCapsule2.getAddress().toByteArray(), assetCapsule2);

  }

  private TransferAssetContract getTransferAssetContract() {
    return Contract.TransferAssetContract.newBuilder()
        .setAssetName(ByteString.copyFrom(ByteArray.fromString(ASSET_NAME)))
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
        .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
        .setAmount(100L)
        .build();
  }

  private TransferAssetContract getTransferAssetV2Contract() {
    return Contract.TransferAssetContract.newBuilder()
        .setAssetName(ByteString.copyFrom(ByteArray.fromString(ASSET_NAME_V2)))
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
        .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
        .setAmount(100L)
        .build();
  }

  private AssetIssueContract getAssetIssueContract() {
    return Contract.AssetIssueContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ASSET_ADDRESS)))
        .setName(ByteString.copyFromUtf8(ASSET_NAME))
        .setFreeAssetNetLimit(1000L)
        .setPublicFreeAssetNetLimit(1000L)
        .build();
  }

  private AssetIssueContract getAssetIssueV2Contract() {
    return Contract.AssetIssueContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ASSET_ADDRESS_V2)))
        .setName(ByteString.copyFromUtf8(ASSET_NAME_V2))
        .setId(ASSET_NAME_V2)
        .setFreeAssetNetLimit(1000L)
        .setPublicFreeAssetNetLimit(1000L)
        .build();
  }

  private void initAssetIssue(long startTimestmp, long endTimestmp, String assetName) {
    long id = dbManager.getDynamicPropertiesStore().getTokenIdNum() + 1;
    dbManager.getDynamicPropertiesStore().saveTokenIdNum(id);
    AssetIssueContract assetIssueContract =
            AssetIssueContract.newBuilder()
                    .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
                    .setName(ByteString.copyFrom(ByteArray.fromString(assetName)))
                    .setId(Long.toString(id))
                    .setTotalSupply(TOTAL_SUPPLY)
                    .setTrxNum(TRX_NUM)
                    .setNum(NUM)
                    .setStartTime(startTimestmp)
                    .setEndTime(endTimestmp)
                    .setVoteScore(VOTE_SCORE)
                    .setDescription(ByteString.copyFrom(ByteArray.fromString(DESCRIPTION)))
                    .setUrl(ByteString.copyFrom(ByteArray.fromString(URL)))
                    .build();
    AssetIssueCapsule assetIssueCapsule = new AssetIssueCapsule(assetIssueContract);
    AccountCapsule toAccountCapsule = dbManager.getAccountStore()
            .get(ByteArray.fromHexString(TO_ADDRESS));
    if(dbManager.getDynamicPropertiesStore().getAllowSameTokenName() == 1 ) {
      dbManager.getAssetIssueV2Store()
              .put(assetIssueCapsule.createDbV2Key(), assetIssueCapsule);
      toAccountCapsule.addAssetV2(ByteArray.fromString(String.valueOf(id)), TOTAL_SUPPLY);
    } else {
      dbManager.getAssetIssueStore()
              .put(assetIssueCapsule.createDbKey(), assetIssueCapsule);
      toAccountCapsule.addAsset(assetName.getBytes(), TOTAL_SUPPLY);
    }
    dbManager.getAccountStore().put(toAccountCapsule.getAddress().toByteArray(), toAccountCapsule);
  }




  //@Test
  public void testCreateNewAccount() throws Exception {
    BandwidthProcessor processor = new BandwidthProcessor(dbManager);
    TransferAssetContract transferAssetContract = getTransferAssetContract();
    TransactionCapsule trx = new TransactionCapsule(transferAssetContract);

    String NOT_EXISTS_ADDRESS =
        Wallet.getAddressPreFixString() + "008794500882809695a8a687866e76d4271a1abc";
    transferAssetContract = transferAssetContract.toBuilder()
        .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(NOT_EXISTS_ADDRESS))).build();

    org.tron.protos.Protocol.Transaction.Contract contract = org.tron.protos.Protocol.Transaction.Contract
        .newBuilder()
        .setType(Protocol.Transaction.Contract.ContractType.TransferAssetContract).setParameter(
            Any.pack(transferAssetContract)).build();

    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1526647838000L);
    dbManager.getDynamicPropertiesStore()
        .saveTotalNetWeight(10_000_000L);//only owner has frozen balance

    AccountCapsule ownerCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    ownerCapsule.setFrozen(10_000_000L, 0L);

    Assert.assertEquals(true, processor.contractCreateNewAccount(contract));
    long bytes = trx.getSerializedSize();
    processor.consumeBandwidthForCreateNewAccount(ownerCapsule, bytes, 1526647838000L);

    AccountCapsule ownerCapsuleNew = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    Assert.assertEquals(122L, ownerCapsuleNew.getNetUsage());

  }


  @Test
  public void testFree() throws Exception {

    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1526647838000L);
    TransferAssetContract contract = getTransferAssetContract();
    TransactionCapsule trx = new TransactionCapsule(contract);

    AccountCapsule ownerCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);

    TransactionResultCapsule ret = new TransactionResultCapsule();
    TransactionTrace trace = new TransactionTrace(trx, dbManager);

    dbManager.consumeBandwidth(trx, trace);

    AccountCapsule ownerCapsuleNew = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));

    Assert.assertEquals(122L + (dbManager.getDynamicPropertiesStore().supportVM()
            ? Constant.MAX_RESULT_SIZE_IN_TX : 0),
        ownerCapsuleNew.getFreeNetUsage());
    Assert.assertEquals(508882612L, ownerCapsuleNew.getLatestConsumeFreeTime());//slot
    Assert.assertEquals(1526647838000L, ownerCapsuleNew.getLatestOperationTime());
    Assert.assertEquals(122L + (dbManager.getDynamicPropertiesStore().supportVM() ? Constant.MAX_RESULT_SIZE_IN_TX : 0),
        dbManager.getDynamicPropertiesStore().getPublicNetUsage());
    Assert.assertEquals(508882612L, dbManager.getDynamicPropertiesStore().getPublicNetTime());
    Assert.assertEquals(0L, ret.getFee());

    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1526691038000L); // + 12h

    dbManager.consumeBandwidth(trx, trace);
    ownerCapsuleNew = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));

    Assert.assertEquals(61L + 122 + (dbManager.getDynamicPropertiesStore().supportVM() ?
            Constant.MAX_RESULT_SIZE_IN_TX / 2 * 3 : 0),
        ownerCapsuleNew.getFreeNetUsage());
    Assert.assertEquals(508897012L,
        ownerCapsuleNew.getLatestConsumeFreeTime()); // 508882612L + 28800L/2
    Assert.assertEquals(1526691038000L, ownerCapsuleNew.getLatestOperationTime());
    Assert.assertEquals(61L + 122L + (dbManager.getDynamicPropertiesStore().supportVM() ?
            Constant.MAX_RESULT_SIZE_IN_TX / 2 * 3 : 0),
        dbManager.getDynamicPropertiesStore().getPublicNetUsage());
    Assert.assertEquals(508897012L, dbManager.getDynamicPropertiesStore().getPublicNetTime());
    Assert.assertEquals(0L, ret.getFee());
  }


  @Test
  public void testConsumeAssetAccount() throws Exception {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1526647838000L);
    dbManager.getDynamicPropertiesStore()
        .saveTotalNetWeight(10_000_000L);//only assetAccount has frozen balance

    TransferAssetContract contract = getTransferAssetContract();
    TransactionCapsule trx = new TransactionCapsule(contract);

    AccountCapsule assetCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(ASSET_ADDRESS));
    assetCapsule.setFrozen(10_000_000L, 0L);
    dbManager.getAccountStore().put(assetCapsule.getAddress().toByteArray(), assetCapsule);

    TransactionResultCapsule ret = new TransactionResultCapsule();
    TransactionTrace trace = new TransactionTrace(trx, dbManager);
    dbManager.consumeBandwidth(trx, trace);

    AccountCapsule ownerCapsuleNew = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    AccountCapsule assetCapsuleNew = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(ASSET_ADDRESS));

    Assert.assertEquals(508882612L, assetCapsuleNew.getLatestConsumeTime());
    Assert.assertEquals(1526647838000L, ownerCapsuleNew.getLatestOperationTime());
    Assert.assertEquals(508882612L, ownerCapsuleNew.getLatestAssetOperationTime(ASSET_NAME));
    Assert.assertEquals(122L + (dbManager.getDynamicPropertiesStore().supportVM() ? Constant.MAX_RESULT_SIZE_IN_TX : 0),
        ownerCapsuleNew.getFreeAssetNetUsage(ASSET_NAME));
    Assert.assertEquals(
        122L + (dbManager.getDynamicPropertiesStore().supportVM() ? Constant.MAX_RESULT_SIZE_IN_TX
            : 0),
        ownerCapsuleNew.getFreeAssetNetUsageV2("1"));
    Assert.assertEquals(
        122L + (dbManager.getDynamicPropertiesStore().supportVM() ? Constant.MAX_RESULT_SIZE_IN_TX
            : 0),
        assetCapsuleNew.getNetUsage());

    Assert.assertEquals(0L, ret.getFee());

    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1526691038000L); // + 12h

    dbManager.consumeBandwidth(trx, trace);

    ownerCapsuleNew = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    assetCapsuleNew = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(ASSET_ADDRESS));

    Assert.assertEquals(508897012L, assetCapsuleNew.getLatestConsumeTime());
    Assert.assertEquals(1526691038000L, ownerCapsuleNew.getLatestOperationTime());
    Assert.assertEquals(508897012L, ownerCapsuleNew.getLatestAssetOperationTime(ASSET_NAME));
    Assert.assertEquals(61L + 122L + (dbManager.getDynamicPropertiesStore().supportVM()
            ? Constant.MAX_RESULT_SIZE_IN_TX / 2 * 3 : 0),
        ownerCapsuleNew.getFreeAssetNetUsage(ASSET_NAME));
    Assert.assertEquals(61L + 122L + (dbManager.getDynamicPropertiesStore().supportVM()
            ? Constant.MAX_RESULT_SIZE_IN_TX / 2 * 3 : 0),
        ownerCapsuleNew.getFreeAssetNetUsageV2("1"));
    Assert.assertEquals(61L + 122L + (dbManager.getDynamicPropertiesStore().supportVM() ?
            Constant.MAX_RESULT_SIZE_IN_TX / 2 * 3 : 0),
        assetCapsuleNew.getNetUsage());
    Assert.assertEquals(0L, ret.getFee());

  }

  @Test
  public void testConsumeAssetAccountV2() throws Exception {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1526647838000L);
    dbManager.getDynamicPropertiesStore()
        .saveTotalNetWeight(10_000_000L);//only assetAccount has frozen balance

    TransferAssetContract contract = getTransferAssetV2Contract();
    TransactionCapsule trx = new TransactionCapsule(contract);

    // issuer freeze balance for bandwidth
    AccountCapsule issuerCapsuleV2 = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(ASSET_ADDRESS_V2));
    issuerCapsuleV2.setFrozen(10_000_000L, 0L);
    dbManager.getAccountStore().put(issuerCapsuleV2.getAddress().toByteArray(), issuerCapsuleV2);

    TransactionResultCapsule ret = new TransactionResultCapsule();
    TransactionTrace trace = new TransactionTrace(trx, dbManager);
    dbManager.consumeBandwidth(trx, trace);

    AccountCapsule ownerCapsuleNew = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    AccountCapsule issuerCapsuleNew = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(ASSET_ADDRESS_V2));

    Assert.assertEquals(508882612L, issuerCapsuleNew.getLatestConsumeTime());
    Assert.assertEquals(1526647838000L, ownerCapsuleNew.getLatestOperationTime());
    Assert.assertEquals(508882612L,
        ownerCapsuleNew.getLatestAssetOperationTimeV2(ASSET_NAME_V2));
    Assert.assertEquals(
        113L + (dbManager.getDynamicPropertiesStore().supportVM()
            ? Constant.MAX_RESULT_SIZE_IN_TX : 0),
        issuerCapsuleNew.getNetUsage());
    Assert.assertEquals(
        113L + (dbManager.getDynamicPropertiesStore().supportVM()
            ? Constant.MAX_RESULT_SIZE_IN_TX : 0),
        ownerCapsuleNew.getFreeAssetNetUsageV2(ASSET_NAME_V2));

    Assert.assertEquals(508882612L, ownerCapsuleNew.getLatestAssetOperationTimeV2 (ASSET_NAME_V2));
    Assert.assertEquals(0L, ret.getFee());

    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1526691038000L); // + 12h

    dbManager.consumeBandwidth(trx, trace);

    ownerCapsuleNew = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    issuerCapsuleNew = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(ASSET_ADDRESS_V2));

    Assert.assertEquals(508897012L, issuerCapsuleNew.getLatestConsumeTime());
    Assert.assertEquals(1526691038000L, ownerCapsuleNew.getLatestOperationTime());
    Assert.assertEquals(508897012L,
        ownerCapsuleNew.getLatestAssetOperationTimeV2(ASSET_NAME_V2));
    Assert.assertEquals(56L + 113L + (dbManager.getDynamicPropertiesStore().supportVM() ?
            Constant.MAX_RESULT_SIZE_IN_TX / 2 * 3 : 0),
        ownerCapsuleNew.getFreeAssetNetUsageV2(ASSET_NAME_V2));
    Assert.assertEquals(56L + 113L + (dbManager.getDynamicPropertiesStore().supportVM() ?
            Constant.MAX_RESULT_SIZE_IN_TX / 2 * 3 : 0),
        issuerCapsuleNew.getNetUsage());
    Assert.assertEquals(0L, ret.getFee());

  }

  @Test
  public void testConsumeOwner() throws Exception {
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1526647838000L);
    dbManager.getDynamicPropertiesStore()
        .saveTotalNetWeight(10_000_000L);//only owner has frozen balance

    TransferAssetContract contract = getTransferAssetContract();
    TransactionCapsule trx = new TransactionCapsule(contract);

    AccountCapsule ownerCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    ownerCapsule.setFrozen(10_000_000L, 0L);

    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);

    TransactionResultCapsule ret = new TransactionResultCapsule();
    TransactionTrace trace = new TransactionTrace(trx, dbManager);
    dbManager.consumeBandwidth(trx, trace);

    AccountCapsule ownerCapsuleNew = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));

    AccountCapsule assetCapsuleNew = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(ASSET_ADDRESS));

    Assert.assertEquals(122L + (dbManager.getDynamicPropertiesStore().supportVM() ? Constant.MAX_RESULT_SIZE_IN_TX : 0),
        ownerCapsuleNew.getNetUsage());
    Assert.assertEquals(1526647838000L, ownerCapsuleNew.getLatestOperationTime());
    Assert.assertEquals(508882612L, ownerCapsuleNew.getLatestConsumeTime());
    Assert.assertEquals(0L, ret.getFee());

    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1526691038000L); // + 12h

    dbManager.consumeBandwidth(trx, trace);

    ownerCapsuleNew = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));

    Assert.assertEquals(61L + 122L + (dbManager.getDynamicPropertiesStore().supportVM() ? Constant.MAX_RESULT_SIZE_IN_TX / 2 * 3 : 0),
        ownerCapsuleNew.getNetUsage());
    Assert.assertEquals(1526691038000L, ownerCapsuleNew.getLatestOperationTime());
    Assert.assertEquals(508897012L, ownerCapsuleNew.getLatestConsumeTime());
    Assert.assertEquals(0L, ret.getFee());

  }


  @Test
  public void testUsingFee() throws Exception {

    Args.getInstance().getGenesisBlock().getAssets().forEach(account -> {
      AccountCapsule capsule =
          new AccountCapsule(
              ByteString.copyFromUtf8(""),
              ByteString.copyFrom(account.getAddress()),
              AccountType.AssetIssue,
              100L);
      dbManager.getAccountStore().put(account.getAddress(), capsule);
    });

    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1526647838000L);
    dbManager.getDynamicPropertiesStore().saveFreeNetLimit(0L);

    TransferAssetContract contract = getTransferAssetContract();
    TransactionCapsule trx = new TransactionCapsule(contract);

    AccountCapsule ownerCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    ownerCapsule.setBalance(10_000_000L);

    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);

    TransactionResultCapsule ret = new TransactionResultCapsule();
    TransactionTrace trace = new TransactionTrace(trx, dbManager);
    dbManager.consumeBandwidth(trx, trace);

    AccountCapsule ownerCapsuleNew = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));

    long transactionFee =
        (122L + (dbManager.getDynamicPropertiesStore().supportVM() ? Constant.MAX_RESULT_SIZE_IN_TX : 0)) * dbManager
            .getDynamicPropertiesStore().getTransactionFee();
    Assert.assertEquals(transactionFee,
        dbManager.getDynamicPropertiesStore().getTotalTransactionCost());
    Assert.assertEquals(
        10_000_000L - transactionFee,
        ownerCapsuleNew.getBalance());
    Assert.assertEquals(transactionFee, trace.getReceipt().getNetFee());

    dbManager.getAccountStore().delete(ByteArray.fromHexString(TO_ADDRESS));
    dbManager.consumeBandwidth(trx, trace);

//    long createAccountFee = dbManager.getDynamicPropertiesStore().getCreateAccountFee();
//    ownerCapsuleNew = dbManager.getAccountStore()
//        .get(ByteArray.fromHexString(OWNER_ADDRESS));
//    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getCreateAccountFee(),
//        dbManager.getDynamicPropertiesStore().getTotalCreateAccountCost());
//    Assert.assertEquals(
//        10_000_000L - transactionFee - createAccountFee, ownerCapsuleNew.getBalance());
//    Assert.assertEquals(101220L, ret.getFee());
  }

  /**
   * sameTokenName close, consume success
   * assetIssueCapsule.getOwnerAddress() != fromAccount.getAddress())
   * contract.getType() = TransferAssetContract
   */
  @Test
  public void sameTokenNameCloseConsumeSuccess() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    dbManager.getDynamicPropertiesStore().saveTotalNetWeight(10_000_000L);

    long id = dbManager.getDynamicPropertiesStore().getTokenIdNum() + 1;
    dbManager.getDynamicPropertiesStore().saveTokenIdNum(id);
    AssetIssueContract assetIssueContract =
            AssetIssueContract.newBuilder()
                    .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
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
                    .setPublicFreeAssetNetLimit(2000)
                    .setFreeAssetNetLimit(2000)
                    .build();
    AssetIssueCapsule assetIssueCapsule = new AssetIssueCapsule(assetIssueContract);
    // V1
    dbManager.getAssetIssueStore().put(assetIssueCapsule.createDbKey(), assetIssueCapsule);
    // V2
    dbManager.getAssetIssueV2Store().put(assetIssueCapsule.createDbV2Key(), assetIssueCapsule);

    AccountCapsule ownerCapsule =
            new AccountCapsule(
                    ByteString.copyFromUtf8("owner"),
                    ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
                    AccountType.Normal,
                    dbManager.getDynamicPropertiesStore().getAssetIssueFee());
    ownerCapsule.setBalance(10_000_000L);
    long expireTime = DateTime.now().getMillis() + 6 * 86_400_000;
    ownerCapsule.setFrozenForBandwidth(2_000_000L, expireTime);
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);

    AccountCapsule toAddressCapsule =
            new AccountCapsule(
                    ByteString.copyFromUtf8("owner"),
                    ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)),
                    AccountType.Normal,
                    dbManager.getDynamicPropertiesStore().getAssetIssueFee());
    toAddressCapsule.setBalance(10_000_000L);
    long expireTime2 = DateTime.now().getMillis() + 6 * 86_400_000;
    toAddressCapsule.setFrozenForBandwidth(2_000_000L, expireTime2);
    dbManager.getAccountStore().put(toAddressCapsule.getAddress().toByteArray(), toAddressCapsule);

    TransferAssetContract contract = Contract.TransferAssetContract.newBuilder()
            .setAssetName(ByteString.copyFrom(ByteArray.fromString(ASSET_NAME)))
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
            .setAmount(100L)
            .build();

    TransactionCapsule trx = new TransactionCapsule(contract);
    TransactionTrace trace = new TransactionTrace(trx, dbManager);

    long byteSize = trx.getInstance().toBuilder().clearRet().build().getSerializedSize() +
            Constant.MAX_RESULT_SIZE_IN_TX;

    BandwidthProcessor processor = new BandwidthProcessor(dbManager);

    try {
      processor.consume(trx, trace);
      Assert.assertEquals(trace.getReceipt().getNetFee(), 0);
      Assert.assertEquals(trace.getReceipt().getNetUsage(), byteSize);
      //V1
      AssetIssueCapsule assetIssueCapsuleV1 =
              dbManager.getAssetIssueStore().get(assetIssueCapsule.createDbKey());
      Assert.assertNotNull(assetIssueCapsuleV1);
      Assert.assertEquals(assetIssueCapsuleV1.getPublicFreeAssetNetUsage(), byteSize);

      AccountCapsule fromAccount =
              dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      Assert.assertNotNull(fromAccount);
      Assert.assertEquals(fromAccount.getFreeAssetNetUsage(ASSET_NAME), byteSize);
      Assert.assertEquals(fromAccount.getFreeAssetNetUsageV2(String.valueOf(id)), byteSize);

      AccountCapsule ownerAccount =
              dbManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));
      Assert.assertNotNull(ownerAccount);
      Assert.assertEquals(ownerAccount.getNetUsage(), byteSize);

      //V2
      AssetIssueCapsule assetIssueCapsuleV2 =
              dbManager.getAssetIssueV2Store().get(assetIssueCapsule.createDbV2Key());
      Assert.assertNotNull(assetIssueCapsuleV2);
      Assert.assertEquals(assetIssueCapsuleV2.getPublicFreeAssetNetUsage(), byteSize);
      Assert.assertEquals(fromAccount.getFreeAssetNetUsage(ASSET_NAME), byteSize);
      Assert.assertEquals(fromAccount.getFreeAssetNetUsageV2(String.valueOf(id)), byteSize);
    } catch ( ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (TooBigTransactionResultException e ) {
      Assert.assertFalse(e instanceof TooBigTransactionResultException);
    } catch (AccountResourceInsufficientException e ) {
      Assert.assertFalse(e instanceof AccountResourceInsufficientException);
    } finally {
      dbManager.getAccountStore().delete(ByteArray.fromHexString(OWNER_ADDRESS));
      dbManager.getAccountStore().delete(ByteArray.fromHexString(TO_ADDRESS));
      dbManager.getAssetIssueStore().delete(assetIssueCapsule.createDbKey());
      dbManager.getAssetIssueV2Store().delete(assetIssueCapsule.createDbV2Key());
    }
  }

  /**
   * sameTokenName open, consume success
   * assetIssueCapsule.getOwnerAddress() != fromAccount.getAddress())
   * contract.getType() = TransferAssetContract
   */
  @Test
  public void sameTokenNameOpenConsumeSuccess() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    dbManager.getDynamicPropertiesStore().saveTotalNetWeight(10_000_000L);

    long id = dbManager.getDynamicPropertiesStore().getTokenIdNum() + 1;
    dbManager.getDynamicPropertiesStore().saveTokenIdNum(id);
    AssetIssueContract assetIssueContract =
            AssetIssueContract.newBuilder()
                    .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
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
                    .setPublicFreeAssetNetLimit(2000)
                    .setFreeAssetNetLimit(2000)
                    .build();
    AssetIssueCapsule assetIssueCapsule = new AssetIssueCapsule(assetIssueContract);
    // V2
    dbManager.getAssetIssueV2Store().put(assetIssueCapsule.createDbV2Key(), assetIssueCapsule);

    AccountCapsule ownerCapsule =
            new AccountCapsule(
                    ByteString.copyFromUtf8("owner"),
                    ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
                    AccountType.Normal,
                    dbManager.getDynamicPropertiesStore().getAssetIssueFee());
    ownerCapsule.setBalance(10_000_000L);
    long expireTime = DateTime.now().getMillis() + 6 * 86_400_000;
    ownerCapsule.setFrozenForBandwidth(2_000_000L, expireTime);
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);

    AccountCapsule toAddressCapsule =
            new AccountCapsule(
                    ByteString.copyFromUtf8("owner"),
                    ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)),
                    AccountType.Normal,
                    dbManager.getDynamicPropertiesStore().getAssetIssueFee());
    toAddressCapsule.setBalance(10_000_000L);
    long expireTime2 = DateTime.now().getMillis() + 6 * 86_400_000;
    toAddressCapsule.setFrozenForBandwidth(2_000_000L, expireTime2);
    dbManager.getAccountStore().put(toAddressCapsule.getAddress().toByteArray(), toAddressCapsule);

    TransferAssetContract contract = Contract.TransferAssetContract.newBuilder()
            .setAssetName(ByteString.copyFrom(ByteArray.fromString(String.valueOf(id))))
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
            .setAmount(100L)
            .build();

    TransactionCapsule trx = new TransactionCapsule(contract);
    TransactionTrace trace = new TransactionTrace(trx, dbManager);

    long byteSize = trx.getInstance().toBuilder().clearRet().build().getSerializedSize() +
            Constant.MAX_RESULT_SIZE_IN_TX;

    BandwidthProcessor processor = new BandwidthProcessor(dbManager);

    try {
      processor.consume(trx, trace);
      Assert.assertEquals(trace.getReceipt().getNetFee(), 0);
      Assert.assertEquals(trace.getReceipt().getNetUsage(), byteSize);
      AccountCapsule ownerAccount =
              dbManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));
      Assert.assertNotNull(ownerAccount);
      Assert.assertEquals(ownerAccount.getNetUsage(), byteSize);

      //V2
      AssetIssueCapsule assetIssueCapsuleV2 =
              dbManager.getAssetIssueV2Store().get(assetIssueCapsule.createDbV2Key());
      Assert.assertNotNull(assetIssueCapsuleV2);
      Assert.assertEquals(assetIssueCapsuleV2.getPublicFreeAssetNetUsage(), byteSize);

      AccountCapsule fromAccount =
              dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      Assert.assertNotNull(fromAccount);
      Assert.assertEquals(fromAccount.getFreeAssetNetUsageV2(String.valueOf(id)), byteSize);
      Assert.assertEquals(fromAccount.getFreeAssetNetUsageV2(String.valueOf(id)), byteSize);

    } catch ( ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (TooBigTransactionResultException e ) {
      Assert.assertFalse(e instanceof TooBigTransactionResultException);
    } catch (AccountResourceInsufficientException e ) {
      Assert.assertFalse(e instanceof AccountResourceInsufficientException);
    } finally {
      dbManager.getAccountStore().delete(ByteArray.fromHexString(OWNER_ADDRESS));
      dbManager.getAccountStore().delete(ByteArray.fromHexString(TO_ADDRESS));
      dbManager.getAssetIssueStore().delete(assetIssueCapsule.createDbKey());
      dbManager.getAssetIssueV2Store().delete(assetIssueCapsule.createDbV2Key());
    }
  }

  /**
   * sameTokenName close, consume success
   * contract.getType() = TransferContract
   * toAddressAccount isn't exist.
   */
  @Test
  public void sameTokenNameCloseTransferToAccountNotExist() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    dbManager.getDynamicPropertiesStore().saveTotalNetWeight(10_000_000L);

    AccountCapsule ownerCapsule =
            new AccountCapsule(
                    ByteString.copyFromUtf8("owner"),
                    ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
                    AccountType.Normal,
                    dbManager.getDynamicPropertiesStore().getAssetIssueFee());
    ownerCapsule.setBalance(10_000_000L);
    long expireTime = DateTime.now().getMillis() + 6 * 86_400_000;
    ownerCapsule.setFrozenForBandwidth(2_000_000L, expireTime);
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);

    AccountCapsule toAddressCapsule =
            new AccountCapsule(
                    ByteString.copyFromUtf8("owner"),
                    ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)),
                    AccountType.Normal,
                    dbManager.getDynamicPropertiesStore().getAssetIssueFee());
    toAddressCapsule.setBalance(10_000_000L);
    long expireTime2 = DateTime.now().getMillis() + 6 * 86_400_000;
    toAddressCapsule.setFrozenForBandwidth(2_000_000L, expireTime2);
    dbManager.getAccountStore().delete(toAddressCapsule.getAddress().toByteArray());

    Contract.TransferContract contract = Contract.TransferContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
            .setAmount(100L)
            .build();

    TransactionCapsule trx = new TransactionCapsule(contract, dbManager.getAccountStore());
    TransactionTrace trace = new TransactionTrace(trx, dbManager);

    long byteSize = trx.getInstance().toBuilder().clearRet().build().getSerializedSize() +
            Constant.MAX_RESULT_SIZE_IN_TX;

    BandwidthProcessor processor = new BandwidthProcessor(dbManager);

    try {
      processor.consume(trx, trace);

      Assert.assertEquals(trace.getReceipt().getNetFee(), 0);
      Assert.assertEquals(trace.getReceipt().getNetUsage(), byteSize);
      AccountCapsule fromAccount =
              dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      Assert.assertNotNull(fromAccount);
      Assert.assertEquals(fromAccount.getNetUsage(), byteSize);
    } catch ( ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (TooBigTransactionResultException e ) {
      Assert.assertFalse(e instanceof TooBigTransactionResultException);
    } catch (AccountResourceInsufficientException e ) {
      Assert.assertFalse(e instanceof AccountResourceInsufficientException);
    } finally {
      dbManager.getAccountStore().delete(ByteArray.fromHexString(OWNER_ADDRESS));
      dbManager.getAccountStore().delete(ByteArray.fromHexString(TO_ADDRESS));
    }
  }
}
