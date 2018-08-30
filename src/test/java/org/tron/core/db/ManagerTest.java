package org.tron.core.db;

import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;
import java.io.File;
import java.util.Map;
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
import org.tron.common.utils.Utils;
import org.tron.core.Constant;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.exception.AccountResourceInsufficientException;
import org.tron.core.exception.BadBlockException;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.BadNumberBlockException;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.DupTransactionException;
import org.tron.core.exception.HeaderNotFound;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.exception.NonCommonBlockException;
import org.tron.core.exception.ReceiptCheckErrException;
import org.tron.core.exception.ReceiptException;
import org.tron.core.exception.TaposException;
import org.tron.core.exception.TooBigTransactionException;
import org.tron.core.exception.TooBigTransactionResultException;
import org.tron.core.exception.TransactionExpirationException;
import org.tron.core.exception.TransactionTraceException;
import org.tron.core.exception.UnLinkedBlockException;
import org.tron.core.exception.UnsupportVMException;
import org.tron.core.exception.ValidateScheduleException;
import org.tron.core.exception.ValidateSignatureException;
import org.tron.core.witness.WitnessController;
import org.tron.protos.Contract.TransferContract;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;

@Slf4j
public class ManagerTest {

  private static Manager dbManager;
  private static TronApplicationContext context;
  private static BlockCapsule blockCapsule2;
  private static String dbPath = "output_manager_test";

  @Before
  public void init() {
    Args.setParam(new String[]{"-d", dbPath, "-w"}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);

    dbManager = context.getBean(Manager.class);

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
                        Args.getInstance().getLocalWitnesses().getPrivateKey()))
                    .getAddress()));
    blockCapsule2.setMerkleRoot();
    blockCapsule2.sign(
        ByteArray.fromHexString(Args.getInstance().getLocalWitnesses().getPrivateKey()));
  }

  @After
  public void removeDb() {
    Args.clearParam();
    FileUtil.deleteDir(new File(dbPath));
    context.destroy();
  }

  @Test
  public void setBlockReference()
      throws ContractExeException, UnLinkedBlockException, ValidateScheduleException, BadBlockException,
      ContractValidateException, ValidateSignatureException, BadItemException, ItemNotFoundException, AccountResourceInsufficientException, TransactionExpirationException, TooBigTransactionException, DupTransactionException, TaposException, BadNumberBlockException, NonCommonBlockException, ReceiptException, TransactionTraceException, ReceiptCheckErrException, UnsupportVMException, TooBigTransactionResultException {

    BlockCapsule blockCapsule =
        new BlockCapsule(
            1,
            Sha256Hash.wrap(dbManager.getGenesisBlockId().getByteString()),
            1,
            ByteString.copyFrom(
                ECKey.fromPrivate(
                    ByteArray.fromHexString(
                        Args.getInstance().getLocalWitnesses().getPrivateKey()))
                    .getAddress()));
    blockCapsule.setMerkleRoot();
    blockCapsule.sign(
        ByteArray.fromHexString(Args.getInstance().getLocalWitnesses().getPrivateKey()));

    TransferContract tc =
        TransferContract.newBuilder()
            .setAmount(10)
            .setOwnerAddress(ByteString.copyFromUtf8("aaa"))
            .setToAddress(ByteString.copyFromUtf8("bbb"))
            .build();
    TransactionCapsule trx = new TransactionCapsule(tc, ContractType.TransferContract);
    if (dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber() == 0) {
      dbManager.pushBlock(blockCapsule);
      Assert.assertEquals(1, dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber());
      dbManager.setBlockReference(trx);
      Assert.assertEquals(1,
          ByteArray.toInt(trx.getInstance().getRawData().getRefBlockBytes().toByteArray()));
    }

    while (dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber() > 0) {
      dbManager.eraseBlock();
    }

    dbManager.pushBlock(blockCapsule);
    Assert.assertEquals(1, dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber());
    dbManager.setBlockReference(trx);
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

//    Assert.assertTrue(
//        "containBlock is error",
//        dbManager.containBlock(
//            Sha256Hash.wrap(ByteArray.fromHexString(blockCapsule2.getBlockId().toString()))));

    if (isUnlinked) {
      Assert.assertEquals("getBlockIdByNum is error", dbManager.getHeadBlockNum(), 0);
    } else {
      try {
        Assert.assertEquals(
            "getBlockIdByNum is error",
            blockCapsule2.getBlockId().toString(),
            dbManager.getBlockIdByNum(1).toString());
      } catch (ItemNotFoundException e) {
        e.printStackTrace();
      }
    }

    Assert.assertTrue("hasBlocks is error", dbManager.hasBlocks());
  }

  //    @Test
  public void updateWits() {
    int sizePrv = dbManager.getWitnesses().size();
    dbManager
        .getWitnesses()
        .forEach(
            witnessAddress -> {
              logger.info(
                  "witness address is {}", ByteArray.toHexString(witnessAddress.toByteArray()));
            });
    logger.info("------------");
    WitnessCapsule witnessCapsulef =
        new WitnessCapsule(
            ByteString.copyFrom(ByteArray.fromHexString("0x0011")), "www.tron.net/first");
    witnessCapsulef.setIsJobs(true);
    WitnessCapsule witnessCapsules =
        new WitnessCapsule(
            ByteString.copyFrom(ByteArray.fromHexString("0x0012")), "www.tron.net/second");
    witnessCapsules.setIsJobs(true);
    WitnessCapsule witnessCapsulet =
        new WitnessCapsule(
            ByteString.copyFrom(ByteArray.fromHexString("0x0013")), "www.tron.net/three");
    witnessCapsulet.setIsJobs(false);

    dbManager
        .getWitnesses()
        .forEach(
            witnessAddress -> {
              logger.info(
                  "witness address is {}", ByteArray.toHexString(witnessAddress.toByteArray()));
            });
    logger.info("---------");
    dbManager.getWitnessStore().put(witnessCapsulef.getAddress().toByteArray(), witnessCapsulef);
    dbManager.getWitnessStore().put(witnessCapsules.getAddress().toByteArray(), witnessCapsules);
    dbManager.getWitnessStore().put(witnessCapsulet.getAddress().toByteArray(), witnessCapsulet);
    dbManager.getWitnessController().initWits();
    dbManager
        .getWitnesses()
        .forEach(
            witnessAddress -> {
              logger.info(
                  "witness address is {}", ByteArray.toHexString(witnessAddress.toByteArray()));
            });
    int sizeTis = dbManager.getWitnesses().size();
    Assert.assertEquals("update add witness size is ", 2, sizeTis - sizePrv);
  }

  @Test
  public void fork()
      throws ValidateSignatureException, ContractValidateException, ContractExeException,
      UnLinkedBlockException, ValidateScheduleException, BadItemException, ReceiptException,
      ItemNotFoundException, HeaderNotFound, AccountResourceInsufficientException,
      TransactionExpirationException, TooBigTransactionException, DupTransactionException,
      BadBlockException, TaposException, BadNumberBlockException, NonCommonBlockException,
      TransactionTraceException, ReceiptCheckErrException, UnsupportVMException, TooBigTransactionResultException {
    Args.setParam(new String[]{"--witness"}, Constant.TEST_CONF);
    long size = dbManager.getBlockStore().size();
    System.out.print("block store size:" + size + "\n");
    String key = "f31db24bfbd1a2ef19beddca0a0fa37632eded9ac666a05d3bd925f01dde1f62";
    byte[] privateKey = ByteArray.fromHexString(key);
    final ECKey ecKey = ECKey.fromPrivate(privateKey);
    byte[] address = ecKey.getAddress();
    WitnessCapsule witnessCapsule = new WitnessCapsule(ByteString.copyFrom(address));
    dbManager.addWitness(ByteString.copyFrom(address));
    dbManager.generateBlock(witnessCapsule, 1533529947843L, privateKey);

    Map<ByteString, String> addressToProvateKeys = addTestWitnessAndAccount();

    long num = dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber();
    BlockCapsule blockCapsule0 =
        createTestBlockCapsule(
            1533529947843L + 3000,
            num + 1,
            dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash().getByteString(),
            addressToProvateKeys);

    BlockCapsule blockCapsule1 =
        createTestBlockCapsule(
            1533529947843L + 3000,
            num + 1,
            dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash().getByteString(),
            addressToProvateKeys);

    dbManager.pushBlock(blockCapsule0);
    dbManager.pushBlock(blockCapsule1);

    BlockCapsule blockCapsule2 =
        createTestBlockCapsule(
            1533529947843L + 6000,
            num + 2, blockCapsule1.getBlockId().getByteString(), addressToProvateKeys);

    dbManager.pushBlock(blockCapsule2);

    Assert.assertNotNull(dbManager.getBlockStore().get(blockCapsule1.getBlockId().getBytes()));
    Assert.assertNotNull(dbManager.getBlockStore().get(blockCapsule2.getBlockId().getBytes()));

    Assert.assertEquals(
        dbManager.getBlockStore().get(blockCapsule2.getBlockId().getBytes()).getParentHash(),
        blockCapsule1.getBlockId());

    Assert.assertEquals(dbManager.getBlockStore().size(), size + 3);

    Assert.assertEquals(
        dbManager.getBlockIdByNum(dbManager.getHead().getNum() - 1), blockCapsule1.getBlockId());
    Assert.assertEquals(
        dbManager.getBlockIdByNum(dbManager.getHead().getNum() - 2), blockCapsule1.getParentHash());

    Assert.assertEquals(
        blockCapsule2.getBlockId(),
        dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash());
    Assert.assertEquals(
        dbManager.getHead().getBlockId(),
        dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash());
  }

  @Test
  public void doNotSwitch()
      throws ValidateSignatureException, ContractValidateException, ContractExeException,
      UnLinkedBlockException, ValidateScheduleException, BadItemException, ReceiptException,
      ItemNotFoundException, HeaderNotFound, AccountResourceInsufficientException,
      TransactionExpirationException, TooBigTransactionException,
      DupTransactionException, BadBlockException,
      TaposException, BadNumberBlockException, NonCommonBlockException, TransactionTraceException,
      ReceiptCheckErrException, UnsupportVMException, TooBigTransactionResultException {
    Args.setParam(new String[]{"--witness"}, Constant.TEST_CONF);
    long size = dbManager.getBlockStore().size();
    System.out.print("block store size:" + size + "\n");
    String key = "f31db24bfbd1a2ef19beddca0a0fa37632eded9ac666a05d3bd925f01dde1f62";
    byte[] privateKey = ByteArray.fromHexString(key);
    final ECKey ecKey = ECKey.fromPrivate(privateKey);
    byte[] address = ecKey.getAddress();
    WitnessCapsule witnessCapsule = new WitnessCapsule(ByteString.copyFrom(address));
    dbManager.addWitness(ByteString.copyFrom(address));
    dbManager.generateBlock(witnessCapsule, 1533529947843L, privateKey);

    Map<ByteString, String> addressToProvateKeys = addTestWitnessAndAccount();

    long num = dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber();
    BlockCapsule blockCapsule0 =
        createTestBlockCapsule(
            1533529947843L + 3000,
            num + 1,
            dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash().getByteString(),
            addressToProvateKeys);

    BlockCapsule blockCapsule1 =
        createTestBlockCapsule(
            1533529947843L + 3001,
            num + 1,
            dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash().getByteString(),
            addressToProvateKeys);

    logger.info("******block0:" + blockCapsule0);
    logger.info("******block1:" + blockCapsule1);

    dbManager.pushBlock(blockCapsule0);
    dbManager.pushBlock(blockCapsule1);
    context.getBean(KhaosDatabase.class).removeBlk(dbManager.getBlockIdByNum(num));
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
      Assert.assertNotNull(dbManager.getBlockStore().get(blockCapsule0.getBlockId().getBytes()));
      Assert.assertEquals(blockCapsule0.getBlockId(),
          dbManager.getBlockStore().get(blockCapsule0.getBlockId().getBytes()).getBlockId());
      Assert.assertEquals(blockCapsule0.getBlockId(),
          dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash());
      exception = e;
    }

    if (exception == null) {
      throw new IllegalStateException();
    }

    BlockCapsule blockCapsule3 =
        createTestBlockCapsule(1533529947843L + 9000,
            dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber() + 1,
            dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash().getByteString(),
            addressToProvateKeys);
    logger.info("******block3:" + blockCapsule3);
    dbManager.pushBlock(blockCapsule3);

    Assert.assertEquals(blockCapsule3.getBlockId(),
        dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash());
    Assert.assertEquals(blockCapsule3.getBlockId(),
        dbManager.getBlockStore()
            .get(dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash().getBytes())
            .getBlockId());

    BlockCapsule blockCapsule4 =
        createTestBlockCapsule(1533529947843L + 12000,
            dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber() + 1,
            blockCapsule3.getBlockId().getByteString(), addressToProvateKeys);
    logger.info("******block4:" + blockCapsule4);
    dbManager.pushBlock(blockCapsule4);

    Assert.assertEquals(blockCapsule4.getBlockId(),
        dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash());
    Assert.assertEquals(blockCapsule4.getBlockId(),
        dbManager.getBlockStore()
            .get(dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash().getBytes())
            .getBlockId());
  }

  @Test
  public void switchBack()
      throws ValidateSignatureException, ContractValidateException, ContractExeException,
      UnLinkedBlockException, ValidateScheduleException, BadItemException, ReceiptException,
      ItemNotFoundException, HeaderNotFound, AccountResourceInsufficientException,
      TransactionExpirationException, TooBigTransactionException, DupTransactionException,
      BadBlockException, TaposException, BadNumberBlockException, NonCommonBlockException,
      TransactionTraceException, ReceiptCheckErrException, UnsupportVMException, TooBigTransactionResultException {
    Args.setParam(new String[]{"--witness"}, Constant.TEST_CONF);
    long size = dbManager.getBlockStore().size();
    System.out.print("block store size:" + size + "\n");
    String key = "f31db24bfbd1a2ef19beddca0a0fa37632eded9ac666a05d3bd925f01dde1f62";
    byte[] privateKey = ByteArray.fromHexString(key);
    final ECKey ecKey = ECKey.fromPrivate(privateKey);
    byte[] address = ecKey.getAddress();
    WitnessCapsule witnessCapsule = new WitnessCapsule(ByteString.copyFrom(address));
    dbManager.addWitness(ByteString.copyFrom(address));
    dbManager.generateBlock(witnessCapsule, 1533529947843L, privateKey);

    Map<ByteString, String> addressToProvateKeys = addTestWitnessAndAccount();

    long num = dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber();
    BlockCapsule blockCapsule0 =
        createTestBlockCapsule(
            1533529947843L + 3000,
            num + 1,
            dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash().getByteString(),
            addressToProvateKeys);

    BlockCapsule blockCapsule1 =
        createTestBlockCapsule(
            1533529947843L + 3000,
            num + 1,
            dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash().getByteString(),
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

    Assert.assertNotNull(dbManager.getBlockStore().get(blockCapsule0.getBlockId().getBytes()));
    Assert.assertEquals(blockCapsule0.getBlockId(),
        dbManager.getBlockStore().get(blockCapsule0.getBlockId().getBytes()).getBlockId());

    BlockCapsule blockCapsule3 =
        createTestBlockCapsule(
            1533529947843L + 9000,
            dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber() + 1,
            blockCapsule0.getBlockId().getByteString(), addressToProvateKeys);
    dbManager.pushBlock(blockCapsule3);

    Assert.assertEquals(blockCapsule3.getBlockId(),
        dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash());
    Assert.assertEquals(blockCapsule3.getBlockId(),
        dbManager.getBlockStore()
            .get(dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash().getBytes())
            .getBlockId());

    BlockCapsule blockCapsule4 =
        createTestBlockCapsule(
            1533529947843L + 12000,
            dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber() + 1,
            blockCapsule3.getBlockId().getByteString(), addressToProvateKeys);
    dbManager.pushBlock(blockCapsule4);

    Assert.assertEquals(blockCapsule4.getBlockId(),
        dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash());
    Assert.assertEquals(blockCapsule4.getBlockId(),
        dbManager.getBlockStore()
            .get(dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash().getBytes())
            .getBlockId());
  }

  private Map<ByteString, String> addTestWitnessAndAccount() {
    dbManager.getWitnesses().clear();
    return IntStream.range(0, 2)
        .mapToObj(
            i -> {
              ECKey ecKey = new ECKey(Utils.getRandom());
              String privateKey = ByteArray.toHexString(ecKey.getPrivKey().toByteArray());
              ByteString address = ByteString.copyFrom(ecKey.getAddress());

              WitnessCapsule witnessCapsule = new WitnessCapsule(address);
              dbManager.getWitnessStore().put(address.toByteArray(), witnessCapsule);
              dbManager.getWitnessController().addWitness(address);

              AccountCapsule accountCapsule =
                  new AccountCapsule(Account.newBuilder().setAddress(address).build());
              dbManager.getAccountStore().put(address.toByteArray(), accountCapsule);

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
      long number, ByteString hash, Map<ByteString, String> addressToProvateKeys) {
    WitnessController witnessController = dbManager.getWitnessController();
    ByteString witnessAddress =
        witnessController.getScheduledWitness(witnessController.getSlotAtTime(time));
    BlockCapsule blockCapsule = new BlockCapsule(number, Sha256Hash.wrap(hash), time,
        witnessAddress);
    blockCapsule.generatedByMyself = true;
    blockCapsule.setMerkleRoot();
    blockCapsule.sign(ByteArray.fromHexString(addressToProvateKeys.get(witnessAddress)));
    return blockCapsule;
  }

  private BlockCapsule createTestBlockCapsuleError(
      long number, ByteString hash, Map<ByteString, String> addressToProvateKeys) {
    long time = System.currentTimeMillis();
    return createTestBlockCapsuleError(time, number, hash, addressToProvateKeys);
  }

  private BlockCapsule createTestBlockCapsuleError(long time,
      long number, ByteString hash, Map<ByteString, String> addressToProvateKeys) {
    WitnessController witnessController = dbManager.getWitnessController();
    ByteString witnessAddress =
        witnessController.getScheduledWitness(witnessController.getSlotAtTime(time));
    BlockCapsule blockCapsule = new BlockCapsule(number, Sha256Hash.wrap(hash), time,
        ByteString.copyFromUtf8("onlyTest"));
    blockCapsule.generatedByMyself = true;
    blockCapsule.setMerkleRoot();
    blockCapsule.sign(ByteArray.fromHexString(addressToProvateKeys.get(witnessAddress)));
    return blockCapsule;
  }
}
