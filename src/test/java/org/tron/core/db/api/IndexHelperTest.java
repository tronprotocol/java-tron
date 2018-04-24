package org.tron.core.db.api;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.ByteString;
import com.googlecode.cqengine.IndexedCollection;
import java.io.File;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
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
import org.tron.protos.Contract.AssetIssueContract;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.BlockHeader;
import org.tron.protos.Protocol.BlockHeader.raw;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Witness;

@Ignore
public class IndexHelperTest {

  private static Manager dbManager;
  private static IndexHelper indexHelper;
  private static AnnotationConfigApplicationContext context;
  private static String dbPath = "output_IndexHelper_test";

  static {
    Args.setParam(new String[]{"-d", dbPath, "-w"}, "config-test-index.conf");
    context = new AnnotationConfigApplicationContext(DefaultConfig.class);
  }

  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
    AccountCapsule accountCapsule =
        new AccountCapsule(
            Account.newBuilder()
                .setAddress(ByteString.copyFrom(ByteArray.fromHexString("121212abc")))
                .build());
    dbManager.getAccountStore().put(ByteArray.fromHexString("121212abc"), accountCapsule);
    BlockCapsule blockCapsule =
        new BlockCapsule(
            Block.newBuilder()
                .setBlockHeader(
                    BlockHeader.newBuilder()
                        .setRawData(raw.newBuilder().setNumber(4).build())
                        .build())
                .build());
    dbManager.getBlockStore().put(blockCapsule.getBlockId().getBytes(), blockCapsule);
    WitnessCapsule witnessCapsule =
        new WitnessCapsule(
            Witness.newBuilder()
                .setAddress(ByteString.copyFrom(ByteArray.fromHexString("121212abc")))
                .build());
    dbManager.getWitnessStore().put(ByteArray.fromHexString("121212abc"), witnessCapsule);
    TransactionCapsule transactionCapsule =
        new TransactionCapsule(
            Transaction.newBuilder()
                .setRawData(
                    Transaction.raw
                        .newBuilder()
                        .setData(ByteString.copyFrom("i am trans".getBytes()))
                        .build())
                .build());
    dbManager
        .getTransactionStore()
        .put(transactionCapsule.getTransactionId().getBytes(), transactionCapsule);
    AssetIssueCapsule assetIssueCapsule =
        new AssetIssueCapsule(
            AssetIssueContract.newBuilder()
                .setName(ByteString.copyFrom("assetIssueName".getBytes()))
                .setNum(12581)
                .build());
    dbManager.getAssetIssueStore().put("assetIssueName".getBytes(), assetIssueCapsule);
    indexHelper = context.getBean(IndexHelper.class);
  }

  @AfterClass
  public static void removeDb() {
    Args.clearParam();
    FileUtil.deleteDir(new File(dbPath));
    context.destroy();
  }

  @Test
  public void initTest() {

    int sizeOfAccount = getIndexSizeOfAccount();
    Assert.assertEquals("account index num", 1, sizeOfAccount);

    int sizeOfBlock = getIndexSizeOfBlock();
    Assert.assertEquals("block index num", 2, sizeOfBlock);

    int sizeOfWitness = getIndexSizeOfWitness();
    Assert.assertEquals("witness index num", 1, sizeOfWitness);

    int sizeOfTransaction = getIndexSizeOfTransaction();
    Assert.assertEquals("transaction index num", 1, sizeOfTransaction);

    int sizeOfAssetIssue = getIndexSizeOfAssetIssue();
    Assert.assertEquals("assetIssue index num", 1, sizeOfAssetIssue);
  }

  @Test
  public void addAndRemoveAccount() {
    AccountCapsule accountCapsule =
        new AccountCapsule(
            Account.newBuilder()
                .setAddress(ByteString.copyFrom(ByteArray.fromHexString("232323abc")))
                .build());
    indexHelper.add(accountCapsule.getInstance());
    int size = getIndexSizeOfAccount();
    Assert.assertEquals("account index add", 2, size);
    indexHelper.remove(accountCapsule.getInstance());
    size = getIndexSizeOfAccount();
    Assert.assertEquals("account index remove", 1, size);
  }

  private int getIndexSizeOfAccount() {
    IndexedCollection<Account> accountIndex = indexHelper.getAccountIndex();
    ImmutableList<Account> accountImmutableList = ImmutableList.copyOf(accountIndex);
    return accountImmutableList.size();
  }

  @Test
  public void addAndRemoveBlock() {
    BlockCapsule blockCapsule =
        new BlockCapsule(
            Block.newBuilder()
                .setBlockHeader(
                    BlockHeader.newBuilder()
                        .setRawData(raw.newBuilder().setNumber(6).build())
                        .build())
                .build());
    indexHelper.add(blockCapsule.getInstance());
    int size = getIndexSizeOfBlock();
    Assert.assertEquals("block index add", 3, size);
    indexHelper.remove(blockCapsule.getInstance());
    size = getIndexSizeOfBlock();
    Assert.assertEquals("block index remove", 2, size);
  }

  private int getIndexSizeOfBlock() {
    IndexedCollection<Block> blockIndex = indexHelper.getBlockIndex();
    ImmutableList<Block> accountImmutableList = ImmutableList.copyOf(blockIndex);
    return accountImmutableList.size();
  }

  @Test
  public void addAndRemoveWitness() {
    WitnessCapsule witnessCapsule =
        new WitnessCapsule(
            Witness.newBuilder()
                .setAddress(ByteString.copyFrom(ByteArray.fromHexString("343434abc")))
                .build());
    indexHelper.add(witnessCapsule.getInstance());
    int size = getIndexSizeOfWitness();
    Assert.assertEquals("witness index add", 2, size);
    indexHelper.remove(witnessCapsule.getInstance());
    size = getIndexSizeOfWitness();
    Assert.assertEquals("witness index remove", 1, size);
  }

  private int getIndexSizeOfWitness() {
    IndexedCollection<Witness> witnessIndex = indexHelper.getWitnessIndex();
    ImmutableList<Witness> wtinessImmutableList = ImmutableList.copyOf(witnessIndex);
    return wtinessImmutableList.size();
  }

  @Test
  public void addAndRemoveTransaction() {
    TransactionCapsule transactionCapsule =
        new TransactionCapsule(
            Transaction.newBuilder()
                .setRawData(
                    Transaction.raw
                        .newBuilder()
                        .setData(ByteString.copyFrom("i am trans".getBytes()))
                        .build())
                .build());
    indexHelper.add(transactionCapsule.getInstance());
    int size = getIndexSizeOfTransaction();
    Assert.assertEquals("account index add", 1, size);
    indexHelper.remove(transactionCapsule.getInstance());
    size = getIndexSizeOfTransaction();
    Assert.assertEquals("account index remove", 0, size);
  }

  private int getIndexSizeOfTransaction() {
    IndexedCollection<Transaction> transactionIndex = indexHelper.getTransactionIndex();
    ImmutableList<Transaction> accountImmutableList = ImmutableList.copyOf(transactionIndex);
    return accountImmutableList.size();
  }

  @Test
  public void addAndRemoveAssetIssue() {
    AssetIssueCapsule assetIssueCapsule =
        new AssetIssueCapsule(
            AssetIssueContract.newBuilder()
                .setName(ByteString.copyFrom("assetIssueName".getBytes()))
                .setNum(12581)
                .build());
    indexHelper.add(assetIssueCapsule.getInstance());
    int size = getIndexSizeOfAssetIssue();
    Assert.assertEquals("account index add", 1, size);
    indexHelper.remove(assetIssueCapsule.getInstance());
    size = getIndexSizeOfAssetIssue();
    Assert.assertEquals("account index remove", 0, size);
  }

  private int getIndexSizeOfAssetIssue() {
    IndexedCollection<AssetIssueContract> assetIssueContractIndex =
        indexHelper.getAssetIssueIndex();
    ImmutableList<AssetIssueContract> accountImmutableList =
        ImmutableList.copyOf(assetIssueContractIndex);
    return accountImmutableList.size();
  }
}
