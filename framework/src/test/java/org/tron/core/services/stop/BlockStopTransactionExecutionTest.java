package org.tron.core.services.stop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.google.protobuf.ByteString;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.springframework.test.util.AopTestUtils;
import org.tron.api.GrpcAPI;
import org.tron.common.crypto.ECKey;
import org.tron.common.es.ExecutorServiceManager;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ReflectUtils;
import org.tron.common.utils.Sha256Hash;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.net.TronNetService;
import org.tron.protos.Protocol;
import org.tron.protos.contract.BalanceContract;

public class BlockStopTransactionExecutionTest extends ConditionallyStopTest {

  private static final long HEIGHT = 10;
  private static final int MAX_BLOCKS_PER_STOP = 120;

  private final ECKey owner = new ECKey(Utils.getRandom());
  private final ECKey receiver = new ECKey(Utils.getRandom());
  private long amount = 1;

  @Override
  protected void initParameter(CommonParameter parameter) {
    parameter.setShutdownBlockHeight(HEIGHT);
    parameter.getStorage().setMaxFlushCount(1);
    parameter.setMinEffectiveConnection(0);
  }

  @Override
  protected void check() {
    assertEquals(HEIGHT, persistedHeight());
  }

  @Test(timeout = 180_000)
  public void sparseTransactionsDoNotSkipTheTarget() throws Exception {
    fundAccounts();
    for (int spacing : new int[] {2, 3, 5, 10, 20}) {
      for (int phase = 0; phase < spacing; phase++) {
        long target = persistedHeight() + 25 + phase;
        ReflectUtils.setFieldValue(dbManager, "latestSolidityNumShutDown", target);
        for (int i = 0; i < MAX_BLOCKS_PER_STOP && !tronNetDelegate.isHitDown(); i++) {
          generateBlock();
          if (!tronNetDelegate.isHitDown() && (i + phase) % spacing == 0) {
            dbManager.pushTransaction(validTransaction());
          }
        }
        assertTrue("spacing " + spacing + " phase " + phase, tronNetDelegate.isHitDown());
        assertEquals(target, persistedHeight());
        ReflectUtils.setFieldValue(tronNetDelegate, "hitDown", false);
      }
    }
  }

  @Test(timeout = 60_000)
  public void transactionsRunWhileHeadIsPastTheTargetAndSolidIsNot() throws Exception {
    fundAccounts();
    ReflectUtils.setFieldValue(dbManager, "latestSolidityNumShutDown", 100L);
    while (headHeight() < 40) {
      generateBlock();
    }
    ReflectUtils.setFieldValue(dbManager, "latestSolidityNumShutDown", solidHeight() + 1);

    dbManager.pushTransaction(validTransaction());

    assertEquals(1, dbManager.getPendingTransactions().size());
    assertEquals(solidHeight(), persistedHeight());
  }

  @Test(timeout = 60_000)
  public void transactionAtTheSolidTargetFlushesOnlyUpToIt() throws Exception {
    fundAccounts();
    ReflectUtils.setFieldValue(dbManager, "latestSolidityNumShutDown", 100L);
    while (headHeight() < 40) {
      generateBlock();
    }
    long target = solidHeight();
    ReflectUtils.setFieldValue(dbManager, "latestSolidityNumShutDown", target);
    assertTrue(persistedHeight() < target);

    dbManager.pushTransaction(validTransaction());

    assertEquals(1, dbManager.getPendingTransactions().size());
    assertEquals(target, persistedHeight());
  }

  @Test(timeout = 60_000)
  public void transactionAfterABusyStopSignalKeepsThePersistedHeight() throws Exception {
    fundAccounts();
    for (int i = 0; i < MAX_BLOCKS_PER_STOP && !tronNetDelegate.isHitDown(); i++) {
      generateBlock();
      if (!tronNetDelegate.isHitDown()) {
        dbManager.pushTransaction(validTransaction());
      }
    }
    assertTrue(tronNetDelegate.isHitDown());
    assertEquals(HEIGHT, solidHeight());

    dbManager.pushTransaction(validTransaction());

    assertEquals(HEIGHT, persistedHeight());
  }

  @Test(timeout = 60_000)
  public void transactionsRunAgainOnceThePersistedHeightIsPastTheTarget() throws Exception {
    fundAccounts();
    ReflectUtils.setFieldValue(dbManager, "latestSolidityNumShutDown", 100L);
    while (headHeight() < 40) {
      generateBlock();
    }
    ReflectUtils.setFieldValue(dbManager, "latestSolidityNumShutDown", persistedHeight() - 1);

    assertTrue(dbManager.pushTransaction(validTransaction()));

    assertEquals(1, dbManager.getPendingTransactions().size());
  }

  @Test(timeout = 60_000)
  public void forkBelowAHeightTargetKeepsIt() throws Exception {
    while (headHeight() < 5) {
      generateBlock();
    }
    long height = headHeight() + 1;
    long slotTime = chainManager.getNextBlockSlotTime();
    ByteString parent = chainManager.getDynamicPropertiesStore().getLatestBlockHeaderHash()
        .getByteString();
    BlockCapsule losing = createTestBlockCapsule(slotTime + 3000, height, parent);
    BlockCapsule winning = createTestBlockCapsule(slotTime, height, parent);
    BlockCapsule next = createTestBlockCapsule(slotTime + 6000, height + 1,
        winning.getBlockId().getByteString());

    tronNetDelegate.processBlock(losing, false);
    tronNetDelegate.processBlock(winning, false);
    tronNetDelegate.processBlock(next, false);

    assertEquals(next.getBlockId(), chainManager.getHeadBlockId());
    assertEquals(HEIGHT, dbManager.getLatestSolidityNumShutDown());
  }

  @Test(timeout = 60_000)
  public void transactionWaitingForTheLockIsNotExecuted() throws Exception {
    fundAccounts();
    ReflectUtils.setFieldValue(dbManager, "latestSolidityNumShutDown", 100L);
    while (headHeight() < 40) {
      generateBlock();
    }
    TransactionCapsule trx = validTransaction();
    long persisted = persistedHeight();
    AtomicReference<Thread> worker = new AtomicReference<>();
    ExecutorService executor = ExecutorServiceManager.newSingleThreadExecutor("waiting-tx");
    try {
      Future<Boolean> task;
      synchronized (dbManager) {
        task = executor.submit(() -> {
          worker.set(Thread.currentThread());
          return dbManager.pushTransaction(trx);
        });
        while (worker.get() == null || worker.get().getState() != Thread.State.BLOCKED) {
          assertFalse("the transaction did not wait for the lock", task.isDone());
          Thread.sleep(1);
        }
        ReflectUtils.setFieldValue(dbManager, "latestSolidityNumShutDown", solidHeight() - 1);
      }
      assertFalse(task.get());
    } finally {
      ExecutorServiceManager.shutdownAndAwaitTermination(executor, "waiting-tx");
    }

    assertEquals(0, dbManager.getPendingTransactions().size());
    assertEquals(persisted, persistedHeight());
  }

  @Test(timeout = 60_000)
  public void apiTransactionAfterTheStopSignalIsRejected() throws Exception {
    fundAccounts();
    generateUntilStopSignal();

    Wallet wallet = AopTestUtils.getTargetObject(context.getBean(Wallet.class));
    TronNetService net = mock(TronNetService.class);
    ReflectUtils.setFieldValue(wallet, "tronNetService", net);

    GrpcAPI.Return result = wallet.broadcastTransaction(validTransaction().getInstance());

    assertEquals(GrpcAPI.Return.response_code.SERVER_BUSY, result.getCode());
    verify(net, never()).fastBroadcastTransaction(any());
    assertEquals(0, dbManager.getPendingTransactions().size());
    assertEquals(HEIGHT, persistedHeight());
  }

  @Test(timeout = 60_000)
  public void peerTransactionAfterTheStopSignalIsRejected() throws Exception {
    fundAccounts();
    generateUntilStopSignal();

    assertFalse(tronNetDelegate.pushTransaction(validTransaction()));

    assertEquals(0, dbManager.getPendingTransactions().size());
    assertEquals(HEIGHT, persistedHeight());
  }

  @Test(timeout = 60_000)
  public void invalidTransactionAfterTheStopSignalIsNotExecuted() throws Exception {
    fundAccounts();
    generateUntilStopSignal();

    TransactionCapsule trx = transfer();
    trx.setReference(1, new byte[32]);
    trx.setExpiration(chainManager.getHeadBlockTimeStamp() + 60_000);
    trx.sign(owner.getPrivKeyBytes());

    assertFalse(dbManager.pushTransaction(trx));
    assertEquals(0, dbManager.getPendingTransactions().size());
    assertEquals(HEIGHT, persistedHeight());
  }

  private void fundAccounts() {
    for (ECKey key : new ECKey[] {owner, receiver}) {
      AccountCapsule account = new AccountCapsule(Protocol.Account.newBuilder()
          .setAddress(ByteString.copyFrom(key.getAddress()))
          .setBalance(1_000_000_000_000L)
          .build());
      chainManager.getAccountStore().put(key.getAddress(), account);
    }
  }

  private void generateUntilStopSignal() throws Exception {
    for (int i = 0; i < MAX_BLOCKS_PER_STOP && !tronNetDelegate.isHitDown(); i++) {
      generateBlock();
    }
    assertTrue(tronNetDelegate.isHitDown());
    assertEquals(HEIGHT, persistedHeight());
  }

  private TransactionCapsule validTransaction() {
    TransactionCapsule trx = transfer();
    Sha256Hash headHash = chainManager.getDynamicPropertiesStore().getLatestBlockHeaderHash();
    trx.setReference(headHeight(), headHash.getBytes());
    trx.setExpiration(chainManager.getHeadBlockTimeStamp() + 60_000);
    trx.setTimestamp();
    trx.sign(owner.getPrivKeyBytes());
    trx.setTime(System.currentTimeMillis());
    return trx;
  }

  private TransactionCapsule transfer() {
    BalanceContract.TransferContract contract = BalanceContract.TransferContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(owner.getAddress()))
        .setToAddress(ByteString.copyFrom(receiver.getAddress()))
        .setAmount(amount++)
        .build();
    return new TransactionCapsule(contract,
        Protocol.Transaction.Contract.ContractType.TransferContract);
  }

  private long persistedHeight() {
    return dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumberFromDB();
  }

  private long solidHeight() {
    return dbManager.getDynamicPropertiesStore().getLatestSolidifiedBlockNum();
  }

  private long headHeight() {
    return dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber();
  }
}
