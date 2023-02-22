package org.tron.common.storage.prune;

import com.google.common.primitives.Longs;
import com.google.protobuf.ByteString;
import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;
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
import org.tron.core.ChainBaseManager;
import org.tron.core.Constant;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.TransactionInfoCapsule;
import org.tron.core.capsule.TransactionRetCapsule;
import org.tron.core.capsule.utils.BlockUtil;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.consensus.ConsensusService;
import org.tron.core.db.BlockIndexStore;
import org.tron.core.db.BlockStore;
import org.tron.core.db.Manager;
import org.tron.core.db.TransactionStore;
import org.tron.core.db2.core.Chainbase;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.TransactionRetStore;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.contract.BalanceContract;



@Slf4j
public class ChainDataPrunerTest {

  private static Manager dbManager;
  private static ChainDataPruner chainDataPruner;
  private static ChainBaseManager chainBaseManager;
  private static ConsensusService consensusService;
  private static TronApplicationContext context;
  private static BlockCapsule blockCapsule1;
  private static BlockCapsule blockCapsule2;
  private static String dbPath = "output_pruner_test";
  private static AtomicInteger port = new AtomicInteger(0);

  @Before
  public void init() {
    Args.setParam(new String[] {"-d", dbPath, "-w"}, Constant.TEST_CONF);
    Args.getInstance().setNodeListenPort(10000 + port.incrementAndGet());
    context = new TronApplicationContext(DefaultConfig.class);

    dbManager = context.getBean(Manager.class);
    chainDataPruner = context.getBean(ChainDataPruner.class);
    consensusService = context.getBean(ConsensusService.class);
    consensusService.start();
    chainBaseManager = dbManager.getChainBaseManager();
  }

  @Test
  public void pruneTest() {
    try {
      blockCapsule1 = BlockUtil.newGenesisBlockCapsule();
      blockCapsule2 =
              new BlockCapsule(
                      2,
                      Sha256Hash.wrap(ByteString.copyFrom(
                              ByteArray.fromHexString(
                                      blockCapsule1.getBlockId().toString()))),
                      0,
                      ByteString.copyFrom(
                              ECKey.fromPrivate(
                                      ByteArray.fromHexString(
                                              Args.getLocalWitnesses().getPrivateKey()))
                                      .getAddress()));
      final BlockStore blockStore = chainBaseManager.getBlockStore();
      final TransactionStore trxStore = chainBaseManager.getTransactionStore();
      final BlockIndexStore blockIndexStore = chainBaseManager.getBlockIndexStore();
      final TransactionRetStore transactionRetStore = chainBaseManager.getTransactionRetStore();
      final DynamicPropertiesStore dynamicPropertiesStore =
          chainBaseManager.getDynamicPropertiesStore();
      // save in database with block number
      BalanceContract.TransferContract tc =
              BalanceContract.TransferContract.newBuilder()
                      .setAmount(10)
                      .setOwnerAddress(ByteString.copyFromUtf8("aaa"))
                      .setToAddress(ByteString.copyFromUtf8("bbb"))
                      .build();
      TransactionCapsule trx = new TransactionCapsule(tc, ContractType.TransferContract);
      trx.setBlockNum(blockCapsule2.getNum());
      TransactionInfoCapsule transactionInfoCapsule = new TransactionInfoCapsule();
      transactionInfoCapsule.setId(trx.getTransactionId().getBytes());
      transactionInfoCapsule.setFee(1000L);
      transactionInfoCapsule.setBlockNumber(blockCapsule2.getNum());
      transactionInfoCapsule.setBlockTimeStamp(200L);
      TransactionRetCapsule transactionRetCapsule = new TransactionRetCapsule();
      transactionRetCapsule.addTransactionInfo(transactionInfoCapsule.getInstance());
      blockCapsule2.addTransaction(trx);
      blockCapsule2.setMerkleRoot();
      blockCapsule2.sign(
              ByteArray.fromHexString(Args.getLocalWitnesses().getPrivateKey()));
      trxStore.put(trx.getTransactionId().getBytes(), trx);
      blockStore.put(blockCapsule1.getBlockId().getBytes(), blockCapsule1);
      blockStore.put(blockCapsule2.getBlockId().getBytes(), blockCapsule2);
      blockIndexStore.put(blockCapsule1.getBlockId());
      blockIndexStore.put(blockCapsule2.getBlockId());
      transactionRetStore.put(Longs.toByteArray(blockCapsule2.getNum()), transactionRetCapsule);
      transactionRetStore.put(trx.getTransactionId().getBytes(), transactionRetCapsule);
      dynamicPropertiesStore.saveLatestBlockHeaderNumber(65538L);
      chainDataPruner.onBlockAdd();
      Thread.sleep(1000);
      Assert.assertTrue(chainBaseManager.getBlockStore()
          .getFromRoot(blockCapsule1.getBlockId().getBytes()) != null);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @After
  public void removeDb() {
    Args.clearParam();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }

}
