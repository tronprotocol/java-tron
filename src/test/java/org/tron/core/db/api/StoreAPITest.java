package org.tron.core.db.api;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
import java.util.List;
import org.joda.time.DateTime;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.NonUniqueObjectException;
import org.tron.protos.Contract.AssetIssueContract;
import org.tron.protos.Contract.TransferContract;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.BlockHeader;
import org.tron.protos.Protocol.BlockHeader.raw;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Witness;


public class StoreAPITest {

  public static final String ACCOUNT_ADDRESS_ONE = "121212a9cf";
  public static final String ACCOUNT_ADDRESS_TWO = "232323a9cf";
  public static final String ACCOUNT_ADDRESS_THREE = "343434a9cf";
  public static final String ACCOUNT_ADDRESS_FOUR = "454545a9cf";
  public static final String ACCOUNT_NAME_ONE = "account12";
  public static final String ACCOUNT_NAME_TWO = "account23";
  public static final long BLOCK_NUM_ONE = 10;
  public static final long BLOCK_NUM_TWO = 11;
  public static final long BLOCK_NUM_THREE = 12;
  public static final long BLOCK_TIMESTAMP_ONE = DateTime.now().minusDays(2).getMillis();
  public static final long BLOCK_TIMESTAMP_TWO = DateTime.now().minusDays(1).getMillis();
  public static final long BLOCK_TIMESTAMP_THREE = DateTime.now().getMillis();
  public static final long BLOCK_WITNESS_ONE = 12;
  public static final long BLOCK_WITNESS_TWO = 13;
  public static final long BLOCK_WITNESS_THREE = 14;
  public static final long TRANSACTION_TIMESTAMP_ONE = DateTime.now().minusDays(2).getMillis();
  public static final long TRANSACTION_TIMESTAMP_TWO = DateTime.now().minusDays(1).getMillis();
  public static final long TRANSACTION_TIMESTAMP_THREE = DateTime.now().getMillis();
  public static final String WITNESS_PUB_K_ONE = "989898a9cf";
  public static final String WITNESS_PUB_K_TWO = "878787a9cf";
  public static final String WITNESS_PUB_K_THREE = "767676a9cf";
  public static final String WITNESS_PUB_K_FOUR = "656565a9cf";
  public static final String WITNESS_URL_ONE = "www.tron.cn";
  public static final String WITNESS_URL_TWO = "www.tron-super.cn";
  public static final String WITNESS_URL_THREE = "www.tron-plus.cn";
  public static final String WITNESS_URL_FOUR = "www.tron-hk.cn";
  public static final long WITNESS_COUNT_ONE = 100;
  public static final long WITNESS_COUNT_TWO = 200;
  public static final long WITNESS_COUNT_THREE = 300;
  public static final long WITNESS_COUNT_FOUR = 400;
  public static final String ASSETISSUE_NAME_ONE = "www.tron.cn";
  public static final String ASSETISSUE_NAME_TWO = "www.tron-super.cn";
  public static final String ASSETISSUE_NAME_THREE = "www.tron-plus.cn";
  public static final String ASSETISSUE_NAME_FOUR = "www.tron-hk.cn";
  public static final long ASSETISSUE_START_ONE = DateTime.now().minusDays(2).getMillis();
  public static final long ASSETISSUE_END_ONE = DateTime.now().minusDays(1).getMillis();
  public static final long ASSETISSUE_START_TWO = DateTime.now().minusDays(1).getMillis();
  public static final long ASSETISSUE_END_TWO = DateTime.now().plusDays(1).getMillis();
  public static final long ASSETISSUE_START_THREE = DateTime.now().getMillis();
  public static final long ASSETISSUE_END_THREE = DateTime.now().plusDays(1).getMillis();
  public static final long ASSETISSUE_START_FOUR = DateTime.now().plusDays(1).getMillis();
  public static final long ASSETISSUE_END_FOUR = DateTime.now().plusDays(2).getMillis();
  public static Account account1;
  public static Account account2;
  public static Block block1;
  public static Block block2;
  public static Block block3;
  public static Witness witness1;
  public static Witness witness2;
  public static Witness witness3;
  public static Witness witness4;
  public static Transaction transaction1;
  public static Transaction transaction2;
  public static Transaction transaction3;
  public static Transaction transaction4;
  public static Transaction transaction5;
  public static Transaction transaction6;
  public static AssetIssueContract assetIssue1;
  public static AssetIssueContract assetIssue2;
  public static AssetIssueContract assetIssue3;
  public static AssetIssueContract assetIssue4;
  private static Manager dbManager;
  private static StoreAPI storeAPI;
  private static AnnotationConfigApplicationContext context;
  private static String dbPath = "output_StoreAPI_test";

  static {
    Args.setParam(new String[]{"-d", dbPath, "-w"}, "config-test-index.conf");
    Args.getInstance().setSolidityNode(true);
    context = new AnnotationConfigApplicationContext(DefaultConfig.class);
  }

  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
    initAccount();
    initTransaction();
    initWitness();
    initAssetIssue();
    initBlock();
    storeAPI = context.getBean(StoreAPI.class);
  }

  @AfterClass
  public static void removeDb() {
    Args.clearParam();
    FileUtil.deleteDir(new File(dbPath));
    context.destroy();
  }

  /**
   * initAssetIssue.
   */
  private static void initAssetIssue() {
    assetIssue1 =
        getBuildAssetIssueContract(
            ASSETISSUE_NAME_ONE, ACCOUNT_ADDRESS_ONE, ASSETISSUE_START_ONE, ASSETISSUE_END_ONE);
    addAssetIssueToStore(assetIssue1);
    assetIssue2 =
        getBuildAssetIssueContract(
            ASSETISSUE_NAME_TWO, ACCOUNT_ADDRESS_TWO, ASSETISSUE_START_TWO, ASSETISSUE_END_TWO);
    addAssetIssueToStore(assetIssue2);
    assetIssue3 =
        getBuildAssetIssueContract(
            ASSETISSUE_NAME_THREE,
            ACCOUNT_ADDRESS_THREE,
            ASSETISSUE_START_THREE,
            ASSETISSUE_END_THREE);
    addAssetIssueToStore(assetIssue3);
    assetIssue4 =
        getBuildAssetIssueContract(
            ASSETISSUE_NAME_FOUR, ACCOUNT_ADDRESS_ONE, ASSETISSUE_START_FOUR, ASSETISSUE_END_FOUR);
    addAssetIssueToStore(assetIssue4);
    dbManager.getAssetIssueStore().delete(ASSETISSUE_NAME_FOUR.getBytes());
    dbManager.getAssetIssueStore().delete(ASSETISSUE_NAME_THREE.getBytes());
  }

  private static void addAssetIssueToStore(AssetIssueContract assetIssueContract) {
    AssetIssueCapsule assetIssueCapsule = new AssetIssueCapsule(assetIssueContract);
    dbManager
        .getAssetIssueStore()
        .put(assetIssueCapsule.getName().toByteArray(), assetIssueCapsule);
  }

  private static AssetIssueContract getBuildAssetIssueContract(
      String name, String address, long start, long end) {
    return AssetIssueContract.newBuilder()
        .setName(ByteString.copyFrom(name.getBytes()))
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(address)))
        .setStartTime(start)
        .setEndTime(end)
        .build();
  }

  /**
   * initTransaction.
   */
  private static void initTransaction() {
    transaction1 =
        getBuildTransaction(
            getBuildTransferContract(ACCOUNT_ADDRESS_ONE, ACCOUNT_ADDRESS_TWO),
            TRANSACTION_TIMESTAMP_ONE);
    addTransactionToStore(transaction1);
    transaction2 =
        getBuildTransaction(
            getBuildTransferContract(ACCOUNT_ADDRESS_TWO, ACCOUNT_ADDRESS_THREE),
            TRANSACTION_TIMESTAMP_TWO);
    addTransactionToStore(transaction2);
  }

  private static void addTransactionToStore(Transaction transaction) {
    TransactionCapsule transactionCapsule = new TransactionCapsule(transaction);
    dbManager
        .getTransactionStore()
        .put(transactionCapsule.getTransactionId().getBytes(), transactionCapsule);
  }

  private static Transaction getBuildTransaction(
      TransferContract transferContract, long transactionTimestamp) {
    return Transaction.newBuilder()
        .setRawData(
            Transaction.raw
                .newBuilder()
                .setTimestamp(transactionTimestamp)
                .addContract(
                    Contract.newBuilder()
                        .setType(ContractType.TransferContract)
                        .setParameter(Any.pack(transferContract))
                        .build())
                .build())
        .build();
  }

  private static TransferContract getBuildTransferContract(String ownerAddress, String toAddress) {
    return TransferContract.newBuilder()
        .setAmount(10)
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
        .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(toAddress)))
        .build();
  }

  /**
   * initWitness.
   */
  private static void initWitness() {
    witness1 =
        getBuildWitness(
            true, ACCOUNT_ADDRESS_ONE, WITNESS_PUB_K_ONE, WITNESS_URL_ONE, WITNESS_COUNT_ONE);
    addWitnessToStore(witness1);
    witness2 =
        getBuildWitness(
            true, ACCOUNT_ADDRESS_TWO, WITNESS_PUB_K_TWO, WITNESS_URL_TWO, WITNESS_COUNT_ONE);
    addWitnessToStore(witness2);
    witness3 =
        getBuildWitness(
            true, ACCOUNT_ADDRESS_THREE, WITNESS_PUB_K_THREE, WITNESS_URL_THREE, WITNESS_COUNT_ONE);
    addWitnessToStore(witness3);
    witness4 =
        getBuildWitness(
            false, ACCOUNT_ADDRESS_FOUR, WITNESS_PUB_K_FOUR, WITNESS_URL_FOUR, WITNESS_COUNT_ONE);
    addWitnessToStore(witness4);
  }

  private static void addWitnessToStore(Witness transaction) {
    WitnessCapsule witnessCapsule = new WitnessCapsule(transaction);
    dbManager.getWitnessStore().put(witnessCapsule.getAddress().toByteArray(), witnessCapsule);
  }

  private static Witness getBuildWitness(
      boolean job, String address, String pubKey, String url, long count) {
    return Witness.newBuilder()
        .setIsJobs(job)
        .setAddress(ByteString.copyFrom(ByteArray.fromHexString(address)))
        .setPubKey(ByteString.copyFrom(ByteArray.fromHexString(pubKey)))
        .setUrl(url)
        .setVoteCount(count)
        .build();
  }

  /**
   * initBlock.
   */
  private static void initBlock() {
    block1 =
        getBuildBlock(
            BLOCK_TIMESTAMP_ONE,
            BLOCK_NUM_ONE,
            BLOCK_WITNESS_ONE,
            ACCOUNT_ADDRESS_ONE,
            transaction1,
            transaction2);
    addBlockToStore(block1);
  }

  private static void addBlockToStore(Block block) {
    BlockCapsule blockCapsule = new BlockCapsule(block);
    dbManager.getBlockStore().put(blockCapsule.getBlockId().getBytes(), blockCapsule);
  }

  private static Block getBuildBlock(
      long timestamp,
      long num,
      long witnessId,
      String witnessAddress,
      Transaction transaction,
      Transaction transactionNext) {
    return Block.newBuilder()
        .setBlockHeader(
            BlockHeader.newBuilder()
                .setRawData(
                    raw.newBuilder()
                        .setTimestamp(timestamp)
                        .setNumber(num)
                        .setWitnessId(witnessId)
                        .setWitnessAddress(
                            ByteString.copyFrom(ByteArray.fromHexString(witnessAddress)))
                        .build())
                .build())
        .addTransactions(transaction)
        .addTransactions(transactionNext)
        .build();
  }

  /**
   * initAccount.
   */
  private static void initAccount() {
    account1 = getBuildAccount(ACCOUNT_ADDRESS_ONE, ACCOUNT_NAME_ONE);
    addAccountToStore(account1);
    account2 = getBuildAccount(ACCOUNT_ADDRESS_TWO, ACCOUNT_NAME_TWO);
    addAccountToStore(account2);
  }

  private static void addAccountToStore(Account account) {
    AccountCapsule accountCapsule = new AccountCapsule(account);
    dbManager.getAccountStore().put(account.getAddress().toByteArray(), accountCapsule);
  }

  private static Account getBuildAccount(String address, String name) {
    return Account.newBuilder()
        .setAddress(ByteString.copyFrom(ByteArray.fromHexString(address)))
        .setAccountName(ByteString.copyFrom(name.getBytes()))
        .build();
  }

  @Test
  public void getAccountAll() {
    List<Account> accountAll = storeAPI.getAccountAll();
    Assert.assertTrue("accountAll1", accountAll.contains(account1));
    Assert.assertTrue("accountAll2", accountAll.contains(account2));
  }

  @Test
  public void getAccountByAddress() {
    try {
      Account account = storeAPI.getAccountByAddress(ACCOUNT_ADDRESS_ONE);
      Assert.assertEquals("getAccountByAddress1", account1, account);
      account = storeAPI.getAccountByAddress(ACCOUNT_ADDRESS_TWO);
      Assert.assertEquals("getAccountByAddress2", account2, account);
      account = storeAPI.getAccountByAddress(null);
      Assert.assertNull("getAccountByAddress2", account);
    } catch (NonUniqueObjectException e) {
      Assert.fail("Exception " + e);
    }
  }

  @Test
  public void getAccountCount() {
    long accountCount = storeAPI.getAccountCount();
    Assert.assertEquals("accountAll1", 2, accountCount);
  }

  @Test
  public void getBlockCount() {
    long blockCount = storeAPI.getBlockCount();
    Assert.assertEquals("BlockCount1", 2, blockCount);
  }

  @Test
  public void getBlockByNumber() {
    try {
      Block block = storeAPI.getBlockByNumber(BLOCK_NUM_ONE);
      Assert.assertEquals("BlockByNumber1", block1, block);
    } catch (NonUniqueObjectException e) {
      Assert.fail("Exception " + e);
    }
  }

  @Test
  public void getBlockByTransactionId() {
    try {
      Block block =
          storeAPI.getBlockByTransactionId(
              new TransactionCapsule(transaction1).getTransactionId().toString());
      Assert.assertEquals("BlockByNumber1", block1, block);
      block = storeAPI.getBlockByTransactionId("");
      Assert.assertNull("BlockByNumber1", block);
      block = storeAPI.getBlockByTransactionId(null);
      Assert.assertNull("BlockByNumber1", block);
    } catch (NonUniqueObjectException e) {
      Assert.fail("Exception " + e);
    }
  }

  @Test
  public void getBlocksRelatedToAccount() {
    List<Block> blockList = storeAPI.getBlocksRelatedToAccount(ACCOUNT_ADDRESS_THREE);
    Assert.assertEquals("BlockByNumber1", block1, blockList.get(0));
    blockList = storeAPI.getBlocksRelatedToAccount(null);
    Assert.assertEquals("BlockByNumber2", 0, blockList.size());
    blockList = storeAPI.getBlocksRelatedToAccount("");
    Assert.assertEquals("BlockByNumber3", 0, blockList.size());
  }

  @Test
  public void getBlocksByWitnessAddress() {
    List<Block> blockList = storeAPI.getBlocksByWitnessAddress(ACCOUNT_ADDRESS_ONE);
    Assert.assertEquals("BlockByNumber1", block1, blockList.get(0));
    blockList = storeAPI.getBlocksByWitnessAddress(null);
    Assert.assertEquals("BlockByNumber2", 0, blockList.size());
    blockList = storeAPI.getBlocksByWitnessAddress("");
    Assert.assertEquals("BlockByNumber3", 0, blockList.size());
  }

  @Test
  public void getBlocksByWitnessId() {
    List<Block> blockList = storeAPI.getBlocksByWitnessId(BLOCK_WITNESS_ONE);
    Assert.assertEquals("BlockByNumber1", block1, blockList.get(0));
    blockList = storeAPI.getBlocksByWitnessId(0L);
    Assert.assertEquals("BlockByNumber2", 1, blockList.size());
    blockList = storeAPI.getBlocksByWitnessId(-1L);
    Assert.assertEquals("BlockByNumber3", 0, blockList.size());
  }

  @Test
  public void getLatestBlocks() {
    List<Block> blockList = storeAPI.getLatestBlocks(1);
    Assert.assertEquals("BlockByNumber1", block1, blockList.get(0));
  }

  @Test
  public void getTransactionCount() {
    long transactionCount = storeAPI.getTransactionCount();
    Assert.assertEquals("TransactionCount1", 2, transactionCount);
  }

  @Test
  public void getTransactionById() {
    try {
      Transaction transaction =
          storeAPI.getTransactionById(
              new TransactionCapsule(transaction1).getTransactionId().toString());
      Assert.assertEquals("TransactionById1", transaction1, transaction);
      transaction =
          storeAPI.getTransactionById(
              new TransactionCapsule(transaction2).getTransactionId().toString());
      Assert.assertEquals("TransactionById2", transaction2, transaction);
      transaction = storeAPI.getTransactionById(null);
      Assert.assertNull("TransactionById3", transaction);
      transaction = storeAPI.getTransactionById("");
      Assert.assertNull("TransactionById4", transaction);
    } catch (NonUniqueObjectException e) {
      Assert.fail("Exception " + e);
    }
  }

  @Test
  public void getTransactionsFromThis() {
    List<Transaction> transactionList = storeAPI.getTransactionsFromThis(ACCOUNT_ADDRESS_ONE,0,1000);
    Assert.assertEquals("TransactionsFromThis1", transaction1, transactionList.get(0));
    transactionList = storeAPI.getTransactionsFromThis(ACCOUNT_ADDRESS_TWO,0,1000);
    Assert.assertEquals("TransactionsFromThis2", transaction2, transactionList.get(0));
    transactionList = storeAPI.getTransactionsFromThis(null,0,1000);
    Assert.assertEquals("TransactionsFromThis3", 0, transactionList.size());
    transactionList = storeAPI.getTransactionsFromThis("",0,1000);
    Assert.assertEquals("TransactionsFromThis4", 0, transactionList.size());
  }

  @Test
  public void getTransactionsToThis() {

    List<Transaction> transactionList = storeAPI.getTransactionsToThis(ACCOUNT_ADDRESS_TWO,0,1000);
    Assert.assertEquals("TransactionsToThis1", transaction1, transactionList.get(0));
    transactionList = storeAPI.getTransactionsToThis(ACCOUNT_ADDRESS_THREE,0,1000);
    Assert.assertEquals("TransactionsToThis2", transaction2, transactionList.get(0));
    transactionList = storeAPI.getTransactionsToThis(null,0,1000);
    Assert.assertEquals("TransactionsToThis3", 0, transactionList.size());
    transactionList = storeAPI.getTransactionsToThis("",0,1000);
    Assert.assertEquals("TransactionsToThis4", 0, transactionList.size());
  }

  @Test
  public void getTransactionsRelatedToAccount() {

    List<Transaction> transactionList =
        storeAPI.getTransactionsRelatedToAccount(ACCOUNT_ADDRESS_ONE);
    Assert.assertEquals("TransactionsRelatedToAccount1", 1, transactionList.size());
    transactionList = storeAPI.getTransactionsRelatedToAccount(ACCOUNT_ADDRESS_TWO);
    Assert.assertEquals("TransactionsRelatedToAccount2", 2, transactionList.size());
    transactionList = storeAPI.getTransactionsRelatedToAccount(ACCOUNT_ADDRESS_THREE);
    Assert.assertEquals("TransactionsRelatedToAccount3", 1, transactionList.size());
    transactionList = storeAPI.getTransactionsRelatedToAccount(null);
    Assert.assertEquals("TransactionsRelatedToAccount4", 0, transactionList.size());
    transactionList = storeAPI.getTransactionsRelatedToAccount("");
    Assert.assertEquals("TransactionsRelatedToAccount5", 0, transactionList.size());
  }

  @Test
  public void getTransactionsByTimestamp() {
    long time1 = DateTime.now().minusDays(3).getMillis();
    long time2 = DateTime.now().minusDays(2).getMillis();
    long time3 = DateTime.now().minusDays(1).getMillis();
    long time4 = DateTime.now().getMillis();
    List<Transaction> transactionList = storeAPI.getTransactionsByTimestamp(time1, time2, 0, 1000);
    Assert.assertEquals("TransactionsByTimestamp1", 1, transactionList.size());
    transactionList = storeAPI.getTransactionsByTimestamp(time1, time3, 0 ,1000);
    Assert.assertEquals("TransactionsByTimestamp2", 2, transactionList.size());
    transactionList = storeAPI.getTransactionsByTimestamp(time1, time4, 0 ,1000);
    Assert.assertEquals("TransactionsByTimestamp3", 2, transactionList.size());
    transactionList = storeAPI.getTransactionsByTimestamp(time2, time4, 0 ,1000);
    Assert.assertEquals("TransactionsByTimestamp4", 1, transactionList.size());
    transactionList = storeAPI.getTransactionsByTimestamp(time3, time4, 0 ,1000);
    Assert.assertEquals("TransactionsByTimestamp5", 0, transactionList.size());
    transactionList = storeAPI.getTransactionsByTimestamp(time4, time3, 0 ,1000);
    Assert.assertEquals("TransactionsByTimestamp5", 0, transactionList.size());
    transactionList = storeAPI.getTransactionsByTimestamp(0, 0, 0 ,1000);
    Assert.assertEquals("TransactionsByTimestamp6", 0, transactionList.size());
    transactionList = storeAPI.getTransactionsByTimestamp(-1, 0,0 , 1000);
    Assert.assertEquals("TransactionsByTimestamp7", 0, transactionList.size());
  }

  @Test
  public void getLatestTransactions() {
    List<Transaction> latestTransactions = storeAPI.getLatestTransactions(1);
    Assert.assertEquals("LatestTransactions1", transaction2, latestTransactions.get(0));
  }

  @Test
  public void getWitnessByAddress() {
    try {
      Witness witness = storeAPI.getWitnessByAddress(ACCOUNT_ADDRESS_ONE);
      Assert.assertEquals("WitnessByAddress1", witness1, witness);
      witness = storeAPI.getWitnessByAddress(ACCOUNT_ADDRESS_TWO);
      Assert.assertEquals("WitnessByAddress2", witness2, witness);
      witness = storeAPI.getWitnessByAddress(ACCOUNT_ADDRESS_THREE);
      Assert.assertEquals("WitnessByAddress3", witness3, witness);
      witness = storeAPI.getWitnessByAddress(ACCOUNT_ADDRESS_FOUR);
      Assert.assertEquals("WitnessByAddress4", witness4, witness);
      witness = storeAPI.getWitnessByAddress(null);
      Assert.assertNull("WitnessByAddress5", witness);
      witness = storeAPI.getWitnessByAddress("");
      Assert.assertNull("WitnessByAddress6", witness);
    } catch (NonUniqueObjectException e) {
      Assert.fail("Exception " + e);
    }
  }

  @Test
  public void getWitnessByUrl() {
    try {
      Witness witness = storeAPI.getWitnessByUrl(WITNESS_URL_ONE);
      Assert.assertEquals("WitnessByUrl1", witness1, witness);
      witness = storeAPI.getWitnessByUrl(WITNESS_URL_TWO);
      Assert.assertEquals("WitnessByUrl2", witness2, witness);
      witness = storeAPI.getWitnessByUrl(WITNESS_URL_THREE);
      Assert.assertEquals("WitnessByUrl3", witness3, witness);
      witness = storeAPI.getWitnessByUrl(WITNESS_URL_FOUR);
      Assert.assertEquals("WitnessByUrl4", witness4, witness);
      witness = storeAPI.getWitnessByUrl(null);
      Assert.assertNull("WitnessByUrl5", witness);
      witness = storeAPI.getWitnessByUrl("");
      Assert.assertNull("WitnessByUrl6", witness);
    } catch (NonUniqueObjectException e) {
      Assert.fail("Exception " + e);
    }
  }

  @Test
  public void getWitnessByPublicKey() {
    try {
      Witness witness = storeAPI.getWitnessByPublicKey(WITNESS_PUB_K_ONE);
      Assert.assertEquals("WitnessByPublicKey2", witness1, witness);
      witness = storeAPI.getWitnessByPublicKey(WITNESS_PUB_K_TWO);
      Assert.assertEquals("WitnessByPublicKey2", witness2, witness);
      witness = storeAPI.getWitnessByPublicKey(WITNESS_PUB_K_THREE);
      Assert.assertEquals("WitnessByPublicKey3", witness3, witness);
      witness = storeAPI.getWitnessByPublicKey(WITNESS_PUB_K_FOUR);
      Assert.assertEquals("WitnessByPublicKey4", witness4, witness);
      witness = storeAPI.getWitnessByPublicKey(null);
      Assert.assertNull("WitnessByPublicKey5", witness);
      witness = storeAPI.getWitnessByPublicKey("");
      Assert.assertNull("WitnessByPublicKey6", witness);
    } catch (NonUniqueObjectException e) {
      Assert.fail("Exception " + e);
    }
  }

  @Test
  public void getWitnessCount() {
    long count = storeAPI.getWitnessCount();
    Assert.assertEquals("WitnessCount1", 4, count);
  }

  @Test
  public void getAssetIssueAll() {
    List<AssetIssueContract> assetIssueAll = storeAPI.getAssetIssueAll();
    Assert.assertTrue("AssetIssueAll1", assetIssueAll.contains(assetIssue1));
    Assert.assertTrue("AssetIssueAll2", assetIssueAll.contains(assetIssue2));
    Assert.assertFalse("AssetIssueAll3", assetIssueAll.contains(assetIssue3));
    Assert.assertFalse("AssetIssueAll4", assetIssueAll.contains(assetIssue4));
  }

  @Test
  public void getAssetIssueByTime() {
    List<AssetIssueContract> assetIssueList =
        storeAPI.getAssetIssueByTime(DateTime.now().getMillis());
    Assert.assertEquals("AssetIssueByTime1", 1, assetIssueList.size());
    assetIssueList = storeAPI.getAssetIssueByTime(0);
    Assert.assertEquals("AssetIssueByTime2", 0, assetIssueList.size());
    assetIssueList = storeAPI.getAssetIssueByTime(DateTime.now().plusDays(2).getMillis());
    Assert.assertEquals("AssetIssueByTime3", 0, assetIssueList.size());
    assetIssueList = storeAPI.getAssetIssueByTime(-1);
    Assert.assertEquals("AssetIssueByTime4", 0, assetIssueList.size());
  }

  @Test
  public void getAssetIssueByName() {
    try {
      AssetIssueContract assetIssueByName = storeAPI.getAssetIssueByName(ASSETISSUE_NAME_ONE);
      Assert.assertEquals("AssetIssueByName1", assetIssue1, assetIssueByName);
      assetIssueByName = storeAPI.getAssetIssueByName(ASSETISSUE_NAME_TWO);
      Assert.assertEquals("AssetIssueByName2", assetIssue2, assetIssueByName);
      assetIssueByName = storeAPI.getAssetIssueByName(ASSETISSUE_NAME_THREE);
      Assert.assertNull("AssetIssueByName3", assetIssueByName);
      assetIssueByName = storeAPI.getAssetIssueByName(ASSETISSUE_NAME_FOUR);
      Assert.assertNull("AssetIssueByName4", assetIssueByName);
      assetIssueByName = storeAPI.getAssetIssueByName(null);
      Assert.assertNull("AssetIssueByName5", assetIssueByName);
      assetIssueByName = storeAPI.getAssetIssueByName("");
      Assert.assertNull("AssetIssueByName6", assetIssueByName);
    } catch (NonUniqueObjectException e) {
      Assert.fail("Exception " + e);
    }
  }

  @Test
  public void getAssetIssueByOwnerAddress() {
    List<AssetIssueContract> assetIssueList =
        storeAPI.getAssetIssueByOwnerAddress(ACCOUNT_ADDRESS_ONE);
    Assert.assertEquals("AssetIssueByOwnerAddress1", 1, assetIssueList.size());
    assetIssueList = storeAPI.getAssetIssueByOwnerAddress(ACCOUNT_ADDRESS_TWO);
    Assert.assertEquals("AssetIssueByOwnerAddress2", 1, assetIssueList.size());
    assetIssueList = storeAPI.getAssetIssueByOwnerAddress(ACCOUNT_ADDRESS_THREE);
    Assert.assertEquals("AssetIssueByOwnerAddress3", 0, assetIssueList.size());
    assetIssueList = storeAPI.getAssetIssueByOwnerAddress(ACCOUNT_ADDRESS_FOUR);
    Assert.assertEquals("AssetIssueByOwnerAddress4", 0, assetIssueList.size());
    assetIssueList = storeAPI.getAssetIssueByOwnerAddress(null);
    Assert.assertEquals("AssetIssueByOwnerAddress5", 0, assetIssueList.size());
    assetIssueList = storeAPI.getAssetIssueByOwnerAddress("");
    Assert.assertEquals("AssetIssueByOwnerAddress6", 0, assetIssueList.size());
  }
}
