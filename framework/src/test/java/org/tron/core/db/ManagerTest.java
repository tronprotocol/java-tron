package org.tron.core.db;

import static org.tron.common.utils.Commons.adjustAssetBalanceV2;
import static org.tron.common.utils.Commons.adjustBalance;
import static org.tron.common.utils.Commons.adjustTotalShieldedPoolValue;
import static org.tron.common.utils.Commons.getExchangeStoreFinal;

import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;
import java.io.File;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.Sha256Hash;
import org.tron.common.utils.StringUtil;
import org.tron.common.utils.Utils;
import org.tron.consensus.dpos.DposSlot;
import org.tron.core.ChainBaseManager;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.consensus.ConsensusService;
import org.tron.core.exception.AccountResourceInsufficientException;
import org.tron.core.exception.BadBlockException;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.BadNumberBlockException;
import org.tron.core.exception.BalanceInsufficientException;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.DupTransactionException;
import org.tron.core.exception.HeaderNotFound;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.exception.NonCommonBlockException;
import org.tron.core.exception.ReceiptCheckErrException;
import org.tron.core.exception.TaposException;
import org.tron.core.exception.TooBigTransactionException;
import org.tron.core.exception.TooBigTransactionResultException;
import org.tron.core.exception.TransactionExpirationException;
import org.tron.core.exception.UnLinkedBlockException;
import org.tron.core.exception.VMIllegalException;
import org.tron.core.exception.ValidateScheduleException;
import org.tron.core.exception.ValidateSignatureException;
import org.tron.core.exception.ZksnarkException;
import org.tron.core.store.CodeStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.ExchangeStore;
import org.tron.core.store.ExchangeV2Store;
import org.tron.core.store.IncrementalMerkleTreeStore;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.contract.AssetIssueContractOuterClass;
import org.tron.protos.contract.BalanceContract.TransferContract;
import org.tron.protos.contract.ShieldContract;


@Slf4j
public class ManagerTest extends BlockGenerate {

  private static final int SHIELDED_TRANS_IN_BLOCK_COUNTS = 1;
  private static Manager dbManager;
  private static ChainBaseManager chainManager;
  private static ConsensusService consensusService;
  private static DposSlot dposSlot;
  private static TronApplicationContext context;
  private static BlockCapsule blockCapsule2;
  private static String dbPath = "output_manager_test";
  private static AtomicInteger port = new AtomicInteger(0);
  private static String accountAddress =
      Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";

  @Before
  public void init() {
    Args.setParam(new String[]{"-d", dbPath, "-w"}, Constant.TEST_CONF);
    Args.getInstance().setNodeListenPort(10000 + port.incrementAndGet());
    context = new TronApplicationContext(DefaultConfig.class);

    dbManager = context.getBean(Manager.class);
    setManager(dbManager);
    dposSlot = context.getBean(DposSlot.class);
    consensusService = context.getBean(ConsensusService.class);
    consensusService.start();
    chainManager = dbManager.getChainBaseManager();
    blockCapsule2 =
        new BlockCapsule(
            1,
            Sha256Hash.wrap(ByteString.copyFrom(
                ByteArray.fromHexString(
                    "0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b81"))),
            0,
            ByteString.copyFrom(
                ECKey.fromPrivate(
                    ByteArray.fromHexString(
                        Args.getLocalWitnesses().getPrivateKey()))
                    .getAddress()));
    blockCapsule2.setMerkleRoot();
    blockCapsule2.sign(
        ByteArray.fromHexString(Args.getLocalWitnesses().getPrivateKey()));
  }

  @After
  public void removeDb() {
    Args.clearParam();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }

  @Test
  public void setBlockReference()
      throws ContractExeException, UnLinkedBlockException, ValidateScheduleException,
      BadBlockException, ContractValidateException, ValidateSignatureException,
      AccountResourceInsufficientException, TransactionExpirationException,
      TooBigTransactionException, DupTransactionException, TaposException, BadNumberBlockException,
      NonCommonBlockException, ReceiptCheckErrException, VMIllegalException,
      TooBigTransactionResultException, ZksnarkException {

    BlockCapsule blockCapsule =
        new BlockCapsule(
            1,
            Sha256Hash.wrap(chainManager.getGenesisBlockId().getByteString()),
            1,
            ByteString.copyFrom(
                ECKey.fromPrivate(
                    ByteArray.fromHexString(
                        Args.getLocalWitnesses().getPrivateKey()))
                    .getAddress()));
    blockCapsule.setMerkleRoot();
    blockCapsule.sign(
        ByteArray.fromHexString(Args.getLocalWitnesses().getPrivateKey()));

    TransferContract tc =
        TransferContract.newBuilder()
            .setAmount(10)
            .setOwnerAddress(ByteString.copyFromUtf8("aaa"))
            .setToAddress(ByteString.copyFromUtf8("bbb"))
            .build();
    TransactionCapsule trx = new TransactionCapsule(tc, ContractType.TransferContract);
    if (chainManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber() == 0) {
      dbManager.pushBlock(blockCapsule);
      Assert.assertEquals(1,
          chainManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber());
      chainManager.setBlockReference(trx);
      Assert.assertEquals(1,
          ByteArray.toInt(trx.getInstance().getRawData().getRefBlockBytes().toByteArray()));
    }

    while (chainManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber() > 0) {
      dbManager.eraseBlock();
    }

    dbManager.pushBlock(blockCapsule);
    Assert.assertEquals(1,
        chainManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber());
    chainManager.setBlockReference(trx);
    Assert.assertEquals(1,
        ByteArray.toInt(trx.getInstance().getRawData().getRefBlockBytes().toByteArray()));
  }

  @Test
  public void pushBlock() {
    boolean isUnlinked = false;
    try {
      dbManager.pushBlock(blockCapsule2);
    } catch (UnLinkedBlockException e) {
      isUnlinked = true;
    } catch (Exception e) {
      Assert.assertTrue("pushBlock is error", false);
    }

    if (isUnlinked) {
      Assert.assertEquals("getBlockIdByNum is error",
          0, chainManager.getHeadBlockNum());
    } else {
      try {
        Assert.assertEquals(
            "getBlockIdByNum is error",
            blockCapsule2.getBlockId().toString(),
            chainManager.getBlockIdByNum(1).toString());
      } catch (ItemNotFoundException e) {
        e.printStackTrace();
      }
    }

    Assert.assertTrue("hasBlocks is error", chainManager.hasBlocks());
  }

  @Test
  public void GetterInstanceTest() {

    Assert.assertTrue(chainManager.getTransactionStore() instanceof TransactionStore);
    Assert.assertTrue(chainManager.getDynamicPropertiesStore() instanceof DynamicPropertiesStore);
    Assert.assertTrue(chainManager.getMerkleTreeStore() instanceof IncrementalMerkleTreeStore);
    Assert.assertTrue(chainManager.getBlockIndexStore() instanceof BlockIndexStore);
    Assert.assertTrue(chainManager.getCodeStore() instanceof CodeStore);
    Assert.assertTrue(chainManager.getCodeStore() instanceof CodeStore);
    Assert.assertTrue(chainManager.getBlockIndexStore() instanceof BlockIndexStore);
    Assert.assertTrue(chainManager.getExchangeV2Store() instanceof ExchangeV2Store);
    Assert.assertTrue(chainManager.getExchangeStore() instanceof ExchangeStore);
    chainManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    Assert.assertTrue(getExchangeStoreFinal(chainManager.getDynamicPropertiesStore(),
        chainManager.getExchangeStore(),
        chainManager.getExchangeV2Store()) instanceof ExchangeStore);
    chainManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    Assert.assertTrue(getExchangeStoreFinal(chainManager.getDynamicPropertiesStore(),
        chainManager.getExchangeStore(),
        chainManager.getExchangeV2Store()) instanceof ExchangeV2Store);

  }


  @Test
  public void pushBlockInvalidSignature() {
    // invalid witness address cause invalid signature
    String invalidWitness = "bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f";
    blockCapsule2.setWitness(invalidWitness);
    try {
      dbManager.pushBlock(blockCapsule2);
      Assert.assertTrue(false);
    } catch (BadBlockException e) {
      Assert.assertEquals("The signature is not validated", e.getMessage());
    } catch (Exception e) {
      Assert.assertFalse(e instanceof Exception);
    }
  }


  @Test
  public void getHeadTest() {
    try {
      BlockCapsule head = chainManager.getHead();
      Assert.assertTrue(head instanceof BlockCapsule);  // successfully
    } catch (HeaderNotFound e) {
      Assert.assertFalse(e instanceof HeaderNotFound);
    }

    chainManager.getBlockStore().reset();

    try {
      chainManager.getHead();
      Assert.assertTrue(false);
    } catch (HeaderNotFound e) {
      logger.info(e.getMessage());
      Assert.assertTrue(e instanceof HeaderNotFound);
    }
  }

  @Test
  public void adjustBalanceTest() {

    byte[] ownerAddress = accountAddress.getBytes();
    AccountCapsule account =
        new AccountCapsule(Account.newBuilder()
            .setAddress(ByteString.copyFrom(accountAddress.getBytes()))
            .setBalance(10)
            .setAccountName(ByteString.copyFrom("test".getBytes()))
            .build());
    chainManager.getAccountStore().put(account.createDbKey(), account);
    try {
      adjustBalance(chainManager.getAccountStore(), accountAddress.getBytes(), 0);
      AccountCapsule copyAccount = chainManager.getAccountStore().get(ownerAddress);
      Assert.assertEquals(copyAccount.getBalance(), account.getBalance());
      Assert.assertEquals(copyAccount.getAccountName(), account.getAccountName());
    } catch (BalanceInsufficientException e) {
      Assert.assertFalse(e instanceof BalanceInsufficientException);
    }

    account.setBalance(30);
    chainManager.getAccountStore().put(account.createDbKey(), account); // update balance
    try {
      adjustBalance(chainManager.getAccountStore(), accountAddress.getBytes(), -40);
      Assert.assertTrue(false);
    } catch (BalanceInsufficientException e) {
      Assert.assertEquals(
          StringUtil.createReadableString(account.createDbKey()) + " insufficient balance",
          e.getMessage());
    }

    account.setBalance(30);
    chainManager.getAccountStore().put(account.createDbKey(), account); // update balance
    try {
      adjustBalance(chainManager.getAccountStore(), accountAddress.getBytes(), -10);
      AccountCapsule copyAccount = chainManager.getAccountStore().get(ownerAddress);
      Assert.assertEquals(copyAccount.getBalance(), account.getBalance() - 10);
      Assert.assertEquals(copyAccount.getAccountName(), account.getAccountName());
    } catch (BalanceInsufficientException e) {
      Assert.assertFalse(e instanceof BalanceInsufficientException);
    }

    account.setBalance(30);
    chainManager.getAccountStore().put(account.createDbKey(), account); // update balance
    try {
      adjustBalance(chainManager.getAccountStore(), accountAddress.getBytes(), 10);
      AccountCapsule copyAccount = chainManager.getAccountStore().get(ownerAddress);
      Assert.assertEquals(copyAccount.getBalance(), account.getBalance() + 10);
      Assert.assertEquals(copyAccount.getAccountName(), account.getAccountName());
    } catch (BalanceInsufficientException e) {
      Assert.assertFalse(e instanceof BalanceInsufficientException);
    }

  }

  @Test
  public void adjustAssetBalanceV2Test() {
    String assetID = "asset1";
    byte[] ownerAddress = accountAddress.getBytes();
    AccountCapsule account =
        new AccountCapsule(Account.newBuilder()
            .setAddress(ByteString.copyFrom(accountAddress.getBytes()))
            .setBalance(10)
            .setAccountName(ByteString.copyFrom("test".getBytes()))
            .build());
    chainManager.getAccountStore().put(account.createDbKey(), account);

    String tokenId = "test1234";
    AssetIssueCapsule assetIssue = new AssetIssueCapsule(
        AssetIssueContractOuterClass.AssetIssueContract.newBuilder()
            .setId(tokenId)
            .setOwnerAddress(ByteString.copyFrom(accountAddress.getBytes()))
            .setAbbr(ByteString.copyFrom(accountAddress.getBytes()))
            .build());
    chainManager.getAssetIssueStore().put(assetID.getBytes(), assetIssue);
    try {
      adjustAssetBalanceV2(accountAddress.getBytes(), assetID, -20,
          chainManager.getAccountStore(), chainManager.getAssetIssueStore(),
          chainManager.getDynamicPropertiesStore());
      Assert.assertTrue(false);
    } catch (BalanceInsufficientException e) {
      Assert.assertTrue(e instanceof BalanceInsufficientException);
      Assert.assertEquals(
          "reduceAssetAmount failed !", e.getMessage());
    }

    account.setBalance(30);
    chainManager.getAccountStore().put(account.createDbKey(), account); // update balance

    try {
      adjustAssetBalanceV2(accountAddress.getBytes(), assetID, 10,
          chainManager.getAccountStore(), chainManager.getAssetIssueStore(),
          chainManager.getDynamicPropertiesStore());
      AccountCapsule copyAccount = chainManager.getAccountStore().get(ownerAddress);
      Assert.assertEquals(copyAccount.getAssetMap().size(), 1);
      copyAccount.getAssetMap().forEach((k, v) -> {
        Assert.assertEquals(k, assetID);
        Assert.assertEquals(v.compareTo(10L), 0);
      });
      Assert.assertEquals(copyAccount.getBalance(), account.getBalance());
    } catch (BalanceInsufficientException e) {
      Assert.assertFalse(e instanceof BalanceInsufficientException);
    }

  }

  @Test
  public void pushBlockInvalidMerkelRoot() {
    Transaction trx = Transaction.newBuilder().build();
    TransactionCapsule moreTrans = new TransactionCapsule(trx);
    blockCapsule2.addTransaction(moreTrans);  // add one more transaction will change merkroot
    blockCapsule2.sign(ByteArray.fromHexString(Args.getLocalWitnesses()
        .getPrivateKey()));
    try {
      dbManager.pushBlock(blockCapsule2);
      Assert.assertTrue(false);
    } catch (BadBlockException e) {
      Assert.assertTrue(e instanceof BadBlockException);
      Assert.assertEquals("The merkle hash is not validated", e.getMessage());
    } catch (Exception e) {
      Assert.assertFalse(e instanceof Exception);
    }
  }

  @Test
  public void adjustTotalShieldPoolValueTest() {
    long valueBalance = chainManager.getDynamicPropertiesStore().getTotalShieldedPoolValue() + 1;
    try {
      adjustTotalShieldedPoolValue(valueBalance, chainManager.getDynamicPropertiesStore());
      Assert.assertTrue(false);
    } catch (BalanceInsufficientException e) {
      Assert.assertTrue(e instanceof BalanceInsufficientException);
      Assert.assertEquals("Total shielded pool value can not below 0", e.getMessage());
    }

    long beforeTotalShieldValue = chainManager.getDynamicPropertiesStore()
        .getTotalShieldedPoolValue();
    valueBalance = beforeTotalShieldValue - 1;
    try {
      adjustTotalShieldedPoolValue(valueBalance, chainManager.getDynamicPropertiesStore());
      long expectValue = beforeTotalShieldValue - valueBalance;
      Assert.assertEquals(chainManager.getDynamicPropertiesStore().getTotalShieldedPoolValue(),
          expectValue);
    } catch (BalanceInsufficientException e) {
      Assert.assertFalse(e instanceof BalanceInsufficientException);
    }

  }

  @Test
  public void pushBlockTooMuchShieldedTransactions() {
    ShieldContract.ShieldedTransferContract trx1 = ShieldContract.ShieldedTransferContract
        .newBuilder()
        .setFromAmount(10)
        .setToAmount(10)
        .build();
    ShieldContract.ShieldedTransferContract trx2 = ShieldContract.ShieldedTransferContract
        .newBuilder()
        .setFromAmount(20)
        .setToAmount(20)
        .build();
    TransactionCapsule trans1 = new TransactionCapsule(trx1, ContractType.ShieldedTransferContract);
    TransactionCapsule trans2 = new TransactionCapsule(trx2, ContractType.ShieldedTransferContract);
    blockCapsule2.addTransaction(trans1);  // addShield transaction
    blockCapsule2.addTransaction(trans2);  //  add Shield transaction
    blockCapsule2.setMerkleRoot();
    blockCapsule2.sign(ByteArray.fromHexString(Args.getLocalWitnesses()
        .getPrivateKey()));
    try {
      dbManager.pushBlock(blockCapsule2);
      Assert.assertTrue(false);
    } catch (BadBlockException e) {
      Assert.assertTrue(e instanceof BadBlockException);
      Assert.assertEquals("shielded transaction count > " + SHIELDED_TRANS_IN_BLOCK_COUNTS,
          e.getMessage());
    } catch (Exception e) {
      Assert.assertFalse(e instanceof Exception);
    }
  }

  @Test
  public void pushSwitchFork()
      throws UnLinkedBlockException, NonCommonBlockException, ContractValidateException,
      ValidateScheduleException, ZksnarkException, BadBlockException, VMIllegalException,
      BadNumberBlockException, DupTransactionException, ContractExeException,
      ValidateSignatureException, TooBigTransactionResultException, TransactionExpirationException,
      TaposException, ReceiptCheckErrException, TooBigTransactionException,
      AccountResourceInsufficientException {

    String key = "f31db24bfbd1a2ef19beddca0a0fa37632eded9ac666a05d3bd925f01dde1f62";
    byte[] privateKey = ByteArray.fromHexString(key);
    final ECKey ecKey = ECKey.fromPrivate(privateKey);
    byte[] address = ecKey.getAddress();

    WitnessCapsule witnessCapsule = new WitnessCapsule(ByteString.copyFrom(address));
    chainManager.addWitness(ByteString.copyFrom(address));

    Block block = getSignedBlock(witnessCapsule.getAddress(), 1533529947843L, privateKey);
    dbManager.pushBlock(new BlockCapsule(block));

    Map<ByteString, String> addressToProvateKeys = addTestWitnessAndAccount();

    long num = chainManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber();
    ByteString latestHeadHash =
        chainManager.getDynamicPropertiesStore().getLatestBlockHeaderHash().getByteString();
    BlockCapsule blockCapsule1 =
        createTestBlockCapsule(
            1533529947843L + 3000,
            num + 1,
            latestHeadHash,
            addressToProvateKeys);

    dbManager.pushBlock(blockCapsule1);

    BlockCapsule blockCapsule2 =
        createTestBlockCapsule(
            1533529947843L + 6000,
            num + 2,
            blockCapsule1.getBlockId().getByteString(),
            addressToProvateKeys);

    chainManager.getDynamicPropertiesStore()
        .saveLatestBlockHeaderHash(latestHeadHash);  // change lastest block head

    try {
      dbManager.pushBlock(blockCapsule2);
      Assert.assertTrue(false);
    } catch (BadBlockException e) {
      Assert.assertFalse(e instanceof BadBlockException);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof Exception);
    }
  }


  public void updateWits() {
    int sizePrv = chainManager.getWitnessScheduleStore().getActiveWitnesses().size();
    chainManager
        .getWitnessScheduleStore().getActiveWitnesses()
        .forEach(
            witnessAddress -> {
              logger.info(
                  "witness address is {}",
                  ByteArray.toHexString(witnessAddress.toByteArray()));
            });
    logger.info("------------");
    WitnessCapsule witnessCapsulef =
        new WitnessCapsule(
            ByteString.copyFrom(ByteArray.fromHexString("0x0011")), "www.tron.net/first");
    witnessCapsulef.setIsJobs(true);
    WitnessCapsule witnessCapsules =
        new WitnessCapsule(
            ByteString.copyFrom(ByteArray.fromHexString("0x0012")),
            "www.tron.net/second");
    witnessCapsules.setIsJobs(true);
    WitnessCapsule witnessCapsulet =
        new WitnessCapsule(
            ByteString.copyFrom(ByteArray.fromHexString("0x0013")), "www.tron.net/three");
    witnessCapsulet.setIsJobs(false);

    chainManager
        .getWitnessScheduleStore().getActiveWitnesses()
        .forEach(
            witnessAddress -> {
              logger.info(
                  "witness address is {}",
                  ByteArray.toHexString(witnessAddress.toByteArray()));
            });
    logger.info("---------");
    chainManager.getWitnessStore().put(witnessCapsulef.getAddress().toByteArray(), witnessCapsulef);
    chainManager.getWitnessStore().put(witnessCapsules.getAddress().toByteArray(), witnessCapsules);
    chainManager.getWitnessStore().put(witnessCapsulet.getAddress().toByteArray(), witnessCapsulet);
    chainManager
        .getWitnesses()
        .forEach(
            witnessAddress -> {
              logger.info(
                  "witness address is {}",
                  ByteArray.toHexString(witnessAddress.toByteArray()));
            });
    int sizeTis = chainManager.getWitnesses().size();
    Assert.assertEquals("update add witness size is ",
        2, sizeTis - sizePrv);
  }

  @Test
  public void fork()
      throws ValidateSignatureException, ContractValidateException, ContractExeException,
      UnLinkedBlockException, ValidateScheduleException, BadItemException,
      ItemNotFoundException, HeaderNotFound, AccountResourceInsufficientException,
      TransactionExpirationException, TooBigTransactionException, DupTransactionException,
      BadBlockException, TaposException, BadNumberBlockException, NonCommonBlockException,
      ReceiptCheckErrException, VMIllegalException, TooBigTransactionResultException,
      ZksnarkException {
    Args.setParam(new String[]{"--witness"}, Constant.TEST_CONF);
    long size = chainManager.getBlockStore().size();
    //  System.out.print("block store size:" + size + "\n");
    String key = "f31db24bfbd1a2ef19beddca0a0fa37632eded9ac666a05d3bd925f01dde1f62";
    byte[] privateKey = ByteArray.fromHexString(key);
    final ECKey ecKey = ECKey.fromPrivate(privateKey);
    byte[] address = ecKey.getAddress();
    WitnessCapsule witnessCapsule = new WitnessCapsule(ByteString.copyFrom(address));
    chainManager.addWitness(ByteString.copyFrom(address));

    Block block = getSignedBlock(witnessCapsule.getAddress(), 1533529947843L, privateKey);

    dbManager.pushBlock(new BlockCapsule(block));

    Map<ByteString, String> addressToProvateKeys = addTestWitnessAndAccount();

    long num = chainManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber();
    BlockCapsule blockCapsule0 =
        createTestBlockCapsule(
            1533529947843L + 3000,
            num + 1,
            chainManager.getDynamicPropertiesStore().getLatestBlockHeaderHash()
                .getByteString(),
            addressToProvateKeys);

    BlockCapsule blockCapsule1 =
        createTestBlockCapsule(
            1533529947843L + 3000,
            num + 1,
            chainManager.getDynamicPropertiesStore().getLatestBlockHeaderHash()
                .getByteString(),
            addressToProvateKeys);

    dbManager.pushBlock(blockCapsule0);
    dbManager.pushBlock(blockCapsule1);

    BlockCapsule blockCapsule2 =
        createTestBlockCapsule(
            1533529947843L + 6000,
            num + 2, blockCapsule1.getBlockId().getByteString(), addressToProvateKeys);

    dbManager.pushBlock(blockCapsule2);

    Assert.assertNotNull(chainManager.getBlockStore().get(blockCapsule1.getBlockId().getBytes()));
    Assert.assertNotNull(chainManager.getBlockStore().get(blockCapsule2.getBlockId().getBytes()));

    Assert.assertEquals(
        chainManager.getBlockStore().get(blockCapsule2.getBlockId().getBytes()).getParentHash(),
        blockCapsule1.getBlockId());

    Assert.assertEquals(chainManager.getBlockStore().size(), size + 3);

    Assert.assertEquals(
        chainManager.getBlockIdByNum(chainManager.getHead().getNum() - 1),
        blockCapsule1.getBlockId());
    Assert.assertEquals(
        chainManager.getBlockIdByNum(chainManager.getHead().getNum() - 2),
        blockCapsule1.getParentHash());

    Assert.assertEquals(
        blockCapsule2.getBlockId(),
        chainManager.getDynamicPropertiesStore().getLatestBlockHeaderHash());
    Assert.assertEquals(
        chainManager.getHead().getBlockId(),
        chainManager.getDynamicPropertiesStore().getLatestBlockHeaderHash());
  }

  @Test
  public void doNotSwitch()
      throws ValidateSignatureException, ContractValidateException, ContractExeException,
      UnLinkedBlockException, ValidateScheduleException, BadItemException,
      ItemNotFoundException, HeaderNotFound, AccountResourceInsufficientException,
      TransactionExpirationException, TooBigTransactionException,
      DupTransactionException, BadBlockException,
      TaposException, BadNumberBlockException, NonCommonBlockException,
      ReceiptCheckErrException, VMIllegalException, TooBigTransactionResultException,
      ZksnarkException {
    Args.setParam(new String[]{"--witness"}, Constant.TEST_CONF);
    long size = chainManager.getBlockStore().size();
    System.out.print("block store size:" + size + "\n");
    String key = "f31db24bfbd1a2ef19beddca0a0fa37632eded9ac666a05d3bd925f01dde1f62";
    byte[] privateKey = ByteArray.fromHexString(key);
    final ECKey ecKey = ECKey.fromPrivate(privateKey);
    byte[] address = ecKey.getAddress();
    WitnessCapsule witnessCapsule = new WitnessCapsule(ByteString.copyFrom(address));
    chainManager.addWitness(ByteString.copyFrom(address));

    Block block = getSignedBlock(witnessCapsule.getAddress(), 1533529947843L, privateKey);
    dbManager.pushBlock(new BlockCapsule(block));

    Map<ByteString, String> addressToProvateKeys = addTestWitnessAndAccount();

    long num = chainManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber();
    BlockCapsule blockCapsule0 =
        createTestBlockCapsule(
            1533529947843L + 3000,
            num + 1,
            chainManager.getDynamicPropertiesStore().getLatestBlockHeaderHash()
                .getByteString(),
            addressToProvateKeys);

    BlockCapsule blockCapsule1 =
        createTestBlockCapsule(
            1533529947843L + 3001,
            num + 1,
            chainManager.getDynamicPropertiesStore().getLatestBlockHeaderHash()
                .getByteString(),
            addressToProvateKeys);

    logger.info("******block0:" + blockCapsule0);
    logger.info("******block1:" + blockCapsule1);

    dbManager.pushBlock(blockCapsule0);
    dbManager.pushBlock(blockCapsule1);
    context.getBean(KhaosDatabase.class).removeBlk(chainManager.getBlockIdByNum(num));
    Exception exception = null;

    BlockCapsule blockCapsule2 =
        createTestBlockCapsule(
            1533529947843L + 6000,
            num + 2, blockCapsule1.getBlockId().getByteString(), addressToProvateKeys);
    logger.info("******block2:" + blockCapsule2);
    try {
      dbManager.pushBlock(blockCapsule2);
    } catch (NonCommonBlockException e) {
      logger.info("do not switch fork");
      Assert.assertNotNull(chainManager.getBlockStore().get(blockCapsule0.getBlockId().getBytes()));
      Assert.assertEquals(blockCapsule0.getBlockId(),
          chainManager.getBlockStore().get(blockCapsule0.getBlockId().getBytes()).getBlockId());
      Assert.assertEquals(blockCapsule0.getBlockId(),
          chainManager.getDynamicPropertiesStore().getLatestBlockHeaderHash());
      exception = e;
    }

    if (exception == null) {
      throw new IllegalStateException();
    }

    BlockCapsule blockCapsule3 =
        createTestBlockCapsule(1533529947843L + 9000,
            chainManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber() + 1,
            chainManager.getDynamicPropertiesStore().getLatestBlockHeaderHash()
                .getByteString(),
            addressToProvateKeys);
    logger.info("******block3:" + blockCapsule3);
    dbManager.pushBlock(blockCapsule3);

    Assert.assertEquals(blockCapsule3.getBlockId(),
        chainManager.getDynamicPropertiesStore().getLatestBlockHeaderHash());
    Assert.assertEquals(blockCapsule3.getBlockId(),
        chainManager.getBlockStore()
            .get(chainManager.getDynamicPropertiesStore().getLatestBlockHeaderHash()
                .getBytes())
            .getBlockId());

    BlockCapsule blockCapsule4 =
        createTestBlockCapsule(1533529947843L + 12000,
            chainManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber() + 1,
            blockCapsule3.getBlockId().getByteString(), addressToProvateKeys);
    logger.info("******block4:" + blockCapsule4);
    dbManager.pushBlock(blockCapsule4);

    Assert.assertEquals(blockCapsule4.getBlockId(),
        chainManager.getDynamicPropertiesStore().getLatestBlockHeaderHash());
    Assert.assertEquals(blockCapsule4.getBlockId(),
        chainManager.getBlockStore()
            .get(chainManager.getDynamicPropertiesStore().getLatestBlockHeaderHash()
                .getBytes())
            .getBlockId());
  }

  @Test
  public void switchBack()
      throws ValidateSignatureException, ContractValidateException, ContractExeException,
      UnLinkedBlockException, ValidateScheduleException, BadItemException,
      ItemNotFoundException, HeaderNotFound, AccountResourceInsufficientException,
      TransactionExpirationException, TooBigTransactionException, DupTransactionException,
      BadBlockException, TaposException, BadNumberBlockException, NonCommonBlockException,
      ReceiptCheckErrException, VMIllegalException, TooBigTransactionResultException,
      ZksnarkException {
    Args.setParam(new String[]{"--witness"}, Constant.TEST_CONF);
    long size = chainManager.getBlockStore().size();
    System.out.print("block store size:" + size + "\n");
    String key = "f31db24bfbd1a2ef19beddca0a0fa37632eded9ac666a05d3bd925f01dde1f62";
    byte[] privateKey = ByteArray.fromHexString(key);
    final ECKey ecKey = ECKey.fromPrivate(privateKey);
    byte[] address = ecKey.getAddress();
    WitnessCapsule witnessCapsule = new WitnessCapsule(ByteString.copyFrom(address));
    chainManager.addWitness(ByteString.copyFrom(address));

    Block block = getSignedBlock(witnessCapsule.getAddress(), 1533529947843L, privateKey);
    dbManager.pushBlock(new BlockCapsule(block));

    Map<ByteString, String> addressToProvateKeys = addTestWitnessAndAccount();

    long num = chainManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber();
    BlockCapsule blockCapsule0 =
        createTestBlockCapsule(
            1533529947843L + 3000,
            num + 1,
            chainManager.getDynamicPropertiesStore().getLatestBlockHeaderHash()
                .getByteString(),
            addressToProvateKeys);

    BlockCapsule blockCapsule1 =
        createTestBlockCapsule(
            1533529947843L + 3000,
            num + 1,
            chainManager.getDynamicPropertiesStore().getLatestBlockHeaderHash()
                .getByteString(),
            addressToProvateKeys);

    dbManager.pushBlock(blockCapsule0);
    dbManager.pushBlock(blockCapsule1);
    try {
      BlockCapsule blockCapsule2 =
          createTestBlockCapsuleError(
              1533529947843L + 6000,
              num + 2, blockCapsule1.getBlockId().getByteString(), addressToProvateKeys);

      dbManager.pushBlock(blockCapsule2);
    } catch (ValidateScheduleException e) {
      logger.info("the fork chain has error block");
    }

    Assert.assertNotNull(chainManager.getBlockStore().get(blockCapsule0.getBlockId().getBytes()));
    Assert.assertEquals(blockCapsule0.getBlockId(),
        chainManager.getBlockStore().get(blockCapsule0.getBlockId().getBytes()).getBlockId());

    BlockCapsule blockCapsule3 =
        createTestBlockCapsule(
            1533529947843L + 9000,
            chainManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber() + 1,
            blockCapsule0.getBlockId().getByteString(), addressToProvateKeys);
    dbManager.pushBlock(blockCapsule3);

    Assert.assertEquals(blockCapsule3.getBlockId(),
        chainManager.getDynamicPropertiesStore().getLatestBlockHeaderHash());
    Assert.assertEquals(blockCapsule3.getBlockId(),
        chainManager.getBlockStore()
            .get(chainManager.getDynamicPropertiesStore().getLatestBlockHeaderHash()
                .getBytes())
            .getBlockId());

    BlockCapsule blockCapsule4 =
        createTestBlockCapsule(
            1533529947843L + 12000,
            chainManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber() + 1,
            blockCapsule3.getBlockId().getByteString(), addressToProvateKeys);
    dbManager.pushBlock(blockCapsule4);

    Assert.assertEquals(blockCapsule4.getBlockId(),
        chainManager.getDynamicPropertiesStore().getLatestBlockHeaderHash());
    Assert.assertEquals(blockCapsule4.getBlockId(),
        chainManager.getBlockStore()
            .get(chainManager.getDynamicPropertiesStore().getLatestBlockHeaderHash()
                .getBytes())
            .getBlockId());
  }

  private Map<ByteString, String> addTestWitnessAndAccount() {
    chainManager.getWitnesses().clear();
    return IntStream.range(0, 2)
        .mapToObj(
            i -> {
              ECKey ecKey = new ECKey(Utils.getRandom());
              String privateKey = ByteArray.toHexString(ecKey.getPrivKey().toByteArray());
              ByteString address = ByteString.copyFrom(ecKey.getAddress());

              WitnessCapsule witnessCapsule = new WitnessCapsule(address);
              chainManager.getWitnessStore().put(address.toByteArray(), witnessCapsule);
              chainManager.addWitness(address);

              AccountCapsule accountCapsule =
                  new AccountCapsule(Account.newBuilder().setAddress(address).build());
              chainManager.getAccountStore().put(address.toByteArray(), accountCapsule);

              return Maps.immutableEntry(address, privateKey);
            })
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }


  private BlockCapsule createTestBlockCapsule(
      long number, ByteString hash, Map<ByteString, String> addressToProvateKeys) {
    long time = System.currentTimeMillis();
    return createTestBlockCapsule(time, number, hash, addressToProvateKeys);
  }

  private BlockCapsule createTestBlockCapsule(long time,
      long number, ByteString hash,
      Map<ByteString, String> addressToProvateKeys) {
    ByteString witnessAddress = dposSlot.getScheduledWitness(dposSlot.getSlot(time));
    BlockCapsule blockCapsule = new BlockCapsule(number, Sha256Hash.wrap(hash), time,
        witnessAddress);
    blockCapsule.generatedByMyself = true;
    blockCapsule.setMerkleRoot();
    blockCapsule.sign(ByteArray.fromHexString(addressToProvateKeys.get(witnessAddress)));
    return blockCapsule;
  }

  private BlockCapsule createTestBlockCapsuleError(long time,
      long number, ByteString hash,
      Map<ByteString, String> addressToProvateKeys) {
    ByteString witnessAddress = dposSlot.getScheduledWitness(dposSlot.getSlot(time));
    BlockCapsule blockCapsule = new BlockCapsule(number, Sha256Hash.wrap(hash), time,
        ByteString.copyFromUtf8("onlyTest"));
    blockCapsule.generatedByMyself = true;
    blockCapsule.setMerkleRoot();
    blockCapsule.sign(ByteArray.fromHexString(addressToProvateKeys.get(witnessAddress)));
    return blockCapsule;
  }
}
