package org.tron.core.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.protobuf.ByteString;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.tron.common.BaseMethodTest;
import org.tron.common.cron.CronExpression;
import org.tron.common.crypto.ECKey;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ReflectUtils;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.consensus.ConsensusService;
import org.tron.core.exception.UnLinkedBlockException;
import org.tron.core.exception.ValidateSignatureException;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.net.message.adv.BlockMessage;
import org.tron.core.net.messagehandler.BlockMsgHandler;
import org.tron.core.net.messagehandler.PbftDataSyncHandler;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.service.adv.AdvService;
import org.tron.core.net.service.sync.SyncService;
import org.tron.core.services.WitnessProductBlockService;
import org.tron.core.store.AccountStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.ReasonCode;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.contract.AccountContract.AccountPermissionUpdateContract;

public class ManagerWitnessSignatureTest extends BaseMethodTest {

  private final ECKey owner = ECKey.fromPrivate(BigInteger.valueOf(101));
  private final ECKey signer = ECKey.fromPrivate(BigInteger.valueOf(102));
  private final ECKey replacement = ECKey.fromPrivate(BigInteger.valueOf(103));
  private ByteString witness;

  @Override
  protected void afterInit() {
    context.getBean(ConsensusService.class).start();
    witness = ByteString.copyFrom(owner.getAddress());
    chainBaseManager.getDynamicPropertiesStore().saveAllowMultiSign(1);
    chainBaseManager.getAccountStore().put(witness.toByteArray(), new AccountCapsule(
        Account.newBuilder().setAddress(witness).setIsWitness(true).setBalance(1_000_000_000_000L)
            .setOwnerPermission(AccountCapsule.createDefaultOwnerPermission(witness))
            .addActivePermission(AccountCapsule.createDefaultActivePermission(witness,
                chainBaseManager.getDynamicPropertiesStore()))
            .setWitnessPermission(AccountCapsule.createDefaultWitnessPermission(
                ByteString.copyFrom(signer.getAddress()))).build()));
    chainBaseManager.getWitnessStore().put(witness.toByteArray(), new WitnessCapsule(witness));
    chainBaseManager.getWitnessScheduleStore()
        .saveActiveWitnesses(Collections.singletonList(witness));
  }

  @Test
  public void acceptsCommittedWitnessPermission() throws Exception {
    BlockCapsule block = nextBlock(signer);
    context.getBean(TronNetDelegate.class).validSignature(block);

    dbManager.pushBlock(block);

    assertEquals(block.getBlockId(), chainBaseManager.getHeadBlockId());
    assertEquals(block.getBlockId(), chainBaseManager.getKhaosDb().getHeadId());
    assertTrue(chainBaseManager.getBlockStore().has(block.getBlockId().getBytes()));
  }

  @Test
  public void rejectsWitnessRevokedWhilePushWaitsForManagerLock() throws Exception {
    BlockCapsule block = spy(nextBlock(signer));
    AtomicInteger validations = new AtomicInteger();
    doAnswer(invocation -> {
      if (validations.incrementAndGet() > 1) {
        assertTrue(Thread.holdsLock(dbManager));
      }
      return invocation.callRealMethod();
    }).when(block).validateSignature(any(DynamicPropertiesStore.class), any(AccountStore.class));
    CountDownLatch ingressValidated = new CountDownLatch(1);
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Future<?> pushed;
    try {
      synchronized (dbManager) {
        pushed = executor.submit(() -> {
          context.getBean(TronNetDelegate.class).validSignature(block);
          ingressValidated.countDown();
          return assertThrows(ValidateSignatureException.class, () -> dbManager.pushBlock(block));
        });
        assertTrue(ingressValidated.await(10, TimeUnit.SECONDS));
        setWitnessPermission(replacement);
      }
      pushed.get(10, TimeUnit.SECONDS);
      assertRejected(block);
    } finally {
      executor.shutdownNow();
      assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    }
  }

  @Test
  public void rejectsWitnessAuthorizedOnlyByPendingSession() throws Exception {
    setWitnessPermission(replacement);
    dbManager.getSession().setValue(dbManager.getRevokingStore().buildSession());
    setWitnessPermission(signer);
    BlockCapsule block = nextBlock(signer);
    context.getBean(TronNetDelegate.class).validSignature(block);

    assertThrows(ValidateSignatureException.class, () -> dbManager.pushBlock(block));

    assertRejected(block);
    assertFalse(dbManager.getSession().valid());
    assertEquals(ByteString.copyFrom(replacement.getAddress()),
        ByteString.copyFrom(chainBaseManager.getAccountStore().get(witness.toByteArray())
            .getWitnessPermissionAddress()));
  }

  @Test
  public void acceptsCommittedPermissionAfterPendingRevocationReset() throws Exception {
    BlockCapsule block = nextBlock(signer);
    context.getBean(TronNetDelegate.class).validSignature(block);
    dbManager.getSession().setValue(dbManager.getRevokingStore().buildSession());
    setWitnessPermission(replacement);
    assertFalse(block.validateSignature(dbManager.getDynamicPropertiesStore(),
        dbManager.getAccountStore()));

    dbManager.pushBlock(block);

    assertEquals(block.getBlockId(), chainBaseManager.getHeadBlockId());
    assertEquals(ByteString.copyFrom(signer.getAddress()),
        ByteString.copyFrom(chainBaseManager.getAccountStore().get(witness.toByteArray())
            .getWitnessPermissionAddress()));
  }

  @Test
  public void rejectsWellFormedUnauthorizedWitness() throws Exception {
    BlockCapsule block = nextBlock(replacement);
    assertFalse(block.validateSignature(dbManager.getDynamicPropertiesStore(),
        dbManager.getAccountStore()));

    assertThrows(ValidateSignatureException.class, () -> dbManager.pushBlock(block));

    assertRejected(block);
  }

  @Test
  public void rejectsMalformedWitnessSignature() throws Exception {
    chainBaseManager.getDynamicPropertiesStore().saveAllowStrictEcdsaValidation(1);
    BlockCapsule signed = nextBlock(signer);
    BlockCapsule block = new BlockCapsule(signed.getInstance().toBuilder()
        .setBlockHeader(signed.getInstance().getBlockHeader().toBuilder()
            .setWitnessSignature(ByteString.copyFrom(new byte[64]))).build());

    assertThrows(ValidateSignatureException.class, () -> dbManager.pushBlock(block));

    assertRejected(block);
  }

  @Test
  public void preservesTrustedGeneratedBlockBypassAndDuplicateHandling() throws Exception {
    BlockCapsule block = nextBlock(replacement);
    block.generatedByMyself = true;
    setMatchingShutdownTime(block);
    dbManager.pushBlock(block);
    assertEquals(block.getNum(), dbManager.getLatestSolidityNumShutDown());
    setWitnessPermission(owner);

    dbManager.pushBlock(new BlockCapsule(block.getData()));

    assertEquals(block.getBlockId(), chainBaseManager.getHeadBlockId());
    assertEquals(block.getBlockId(), chainBaseManager.getKhaosDb().getHeadId());
  }

  @Test
  public void rejectsForkWitnessNotAuthorizedAtCurrentHead() throws Exception {
    BlockCapsule parent = nextBlock(signer);
    dbManager.pushBlock(parent);
    BlockCapsule oldHead = blockRevokingWitness(parent);
    BlockCapsule fork = signedBlock(parent.getNum() + 1, parent.getBlockId(),
        parent.getTimeStamp() + 3001, signer);

    assertThrows(ValidateSignatureException.class, () -> dbManager.pushBlock(fork));

    assertEquals(oldHead.getBlockId(), chainBaseManager.getHeadBlockId());
    assertEquals(oldHead.getBlockId(), chainBaseManager.getKhaosDb().getHeadId());
    assertFalse(chainBaseManager.getKhaosDb().containBlockInMiniStore(fork.getBlockId()));
    assertFalse(chainBaseManager.getBlockStore().has(fork.getBlockId().getBytes()));
  }

  @Test
  public void revalidatesAdmittedForkAgainstRewoundParentPermission() throws Exception {
    BlockCapsule parent = nextBlock(signer);
    dbManager.pushBlock(parent);
    BlockCapsule oldHead = blockRevokingWitness(parent);
    BlockCapsule fork = signedBlock(parent.getNum() + 1, parent.getBlockId(),
        parent.getTimeStamp() + 3001, replacement);
    dbManager.pushBlock(fork);
    assertTrue(chainBaseManager.getKhaosDb().containBlockInMiniStore(fork.getBlockId()));
    BlockCapsule forkHead = signedBlock(fork.getNum() + 1, fork.getBlockId(),
        parent.getTimeStamp() + 6000, replacement);

    synchronized (dbManager) {
      assertThrows(ValidateSignatureException.class, () -> dbManager.pushBlock(forkHead));

      assertEquals(oldHead.getBlockId(), chainBaseManager.getHeadBlockId());
      assertEquals(oldHead.getBlockId(), chainBaseManager.getKhaosDb().getHeadId());
      assertFalse(chainBaseManager.getKhaosDb().containBlockInMiniStore(fork.getBlockId()));
      assertFalse(chainBaseManager.getKhaosDb().containBlockInMiniStore(forkHead.getBlockId()));
      assertEquals(ByteString.copyFrom(replacement.getAddress()),
          ByteString.copyFrom(chainBaseManager.getAccountStore().get(witness.toByteArray())
              .getWitnessPermissionAddress()));
    }
  }

  @Test
  public void syncValidatesWitnessOnceUnderManagerLock() throws Exception {
    List<Boolean> validationLocks = new ArrayList<>();
    BlockCapsule block = recordValidationLocks(nextBlock(signer), validationLocks);

    processSyncBlock(block);

    assertEquals(Collections.singletonList(true), validationLocks);
    assertEquals(block.getBlockId(), chainBaseManager.getHeadBlockId());
  }

  @Test
  public void syncRejectsUnauthorizedWitnessBeforeChainOrKhaosAdmission() throws Exception {
    List<Boolean> validationLocks = new ArrayList<>();
    BlockCapsule block = recordValidationLocks(nextBlock(replacement), validationLocks);

    PeerConnection peer = processSyncBlock(block);

    assertEquals(Collections.singletonList(true), validationLocks);
    verify(peer).disconnect(ReasonCode.BAD_BLOCK);
    assertRejected(block);
  }

  @Test
  public void invalidSyncWitnessCannotChangeMatchingShutdownTarget() throws Exception {
    BlockCapsule block = nextBlock(replacement);
    setMatchingShutdownTime(block);
    long shutdownTarget = dbManager.getLatestSolidityNumShutDown();

    PeerConnection peer = processSyncBlock(block);

    assertEquals(shutdownTarget, dbManager.getLatestSolidityNumShutDown());
    verify(peer).disconnect(ReasonCode.BAD_BLOCK);
    assertRejected(block);
  }

  @Test
  public void validSyncWitnessUpdatesMatchingShutdownTarget() throws Exception {
    BlockCapsule block = nextBlock(signer);
    setMatchingShutdownTime(block);

    processSyncBlock(block);

    assertEquals(block.getNum(), dbManager.getLatestSolidityNumShutDown());
    assertEquals(block.getBlockId(), chainBaseManager.getHeadBlockId());
  }

  @Test
  public void authorizedBlockRetainsShutdownSchedulingBeforeLaterAdmissionFailure()
      throws Exception {
    BlockCapsule block = signedBlock(1, Sha256Hash.of(new byte[]{1}), 1533529947000L, signer);
    setMatchingShutdownTime(block);

    assertThrows(UnLinkedBlockException.class, () -> dbManager.pushBlock(block));

    assertEquals(block.getNum(), dbManager.getLatestSolidityNumShutDown());
    assertRejected(block);
  }

  @Test
  public void advertisedBlockValidatesBeforeBroadcastAndAgainUnderManagerLock() throws Exception {
    List<Boolean> validationLocks = new ArrayList<>();
    BlockCapsule block = recordValidationLocks(nextBlock(signer), validationLocks);
    AdvService adv = mock(AdvService.class);
    doAnswer(invocation -> {
      assertEquals(Collections.singletonList(false), validationLocks);
      assertEquals(0, chainBaseManager.getHeadBlockNum());
      return null;
    }).when(adv).broadcast(any(BlockMessage.class));
    BlockMsgHandler handler = new BlockMsgHandler();
    ReflectUtils.setFieldValue(handler, "tronNetDelegate", context.getBean(TronNetDelegate.class));
    ReflectUtils.setFieldValue(handler, "advService", adv);
    ReflectUtils.setFieldValue(handler, "witnessProductBlockService",
        mock(WitnessProductBlockService.class));
    Method process = BlockMsgHandler.class.getDeclaredMethod("processBlock",
        PeerConnection.class, BlockCapsule.class);
    process.setAccessible(true);

    process.invoke(handler, new PeerConnection(), block);

    assertEquals(Arrays.asList(false, true), validationLocks);
    verify(adv).broadcast(any(BlockMessage.class));
    assertEquals(block.getBlockId(), chainBaseManager.getHeadBlockId());
  }

  private void setMatchingShutdownTime(BlockCapsule block) throws Exception {
    CronExpression schedule = new CronExpression("* * * * * ?");
    assertTrue(schedule.isSatisfiedBy(new Date(block.getTimeStamp())));
    CommonParameter.getInstance().setShutdownBlockTime(schedule);
  }

  private PeerConnection processSyncBlock(BlockCapsule block) throws Exception {
    SyncService service = context.getBean(SyncService.class);
    ReflectUtils.setFieldValue(service, "pbftDataSyncHandler", mock(PbftDataSyncHandler.class));
    PeerConnection peer = mock(PeerConnection.class);
    when(peer.getInetSocketAddress()).thenReturn(new InetSocketAddress("127.0.0.2", 10001));
    Method process = SyncService.class.getDeclaredMethod("processSyncBlock",
        BlockCapsule.class, PeerConnection.class);
    process.setAccessible(true);
    process.invoke(service, block, peer);
    return peer;
  }

  private BlockCapsule recordValidationLocks(BlockCapsule original, List<Boolean> locks)
      throws Exception {
    BlockCapsule block = spy(original);
    doAnswer(invocation -> {
      locks.add(Thread.holdsLock(dbManager));
      return invocation.callRealMethod();
    }).when(block).validateSignature(any(DynamicPropertiesStore.class), any(AccountStore.class));
    return block;
  }

  private BlockCapsule blockRevokingWitness(BlockCapsule parent) throws Exception {
    AccountPermissionUpdateContract contract = AccountPermissionUpdateContract.newBuilder()
        .setOwnerAddress(witness)
        .setOwner(AccountCapsule.createDefaultOwnerPermission(witness))
        .setWitness(AccountCapsule.createDefaultWitnessPermission(
            ByteString.copyFrom(replacement.getAddress())))
        .addActives(AccountCapsule.createDefaultActivePermission(witness,
            chainBaseManager.getDynamicPropertiesStore())).build();
    TransactionCapsule update = new TransactionCapsule(contract,
        ContractType.AccountPermissionUpdateContract);
    update.setReference(parent.getNum(), parent.getBlockId().getBytes());
    update.setExpiration(parent.getTimeStamp() + 1_000_000L);
    update.sign(owner.getPrivKeyBytes());
    BlockCapsule oldHead = nextBlock(signer);
    oldHead.addTransaction(update);
    oldHead.setMerkleRoot();
    oldHead.sign(signer.getPrivKeyBytes());
    dbManager.pushBlock(oldHead);
    return oldHead;
  }

  private void setWitnessPermission(ECKey key) {
    Account account = chainBaseManager.getAccountStore().get(witness.toByteArray()).getInstance();
    chainBaseManager.getAccountStore().put(witness.toByteArray(), new AccountCapsule(account
        .toBuilder().setWitnessPermission(AccountCapsule.createDefaultWitnessPermission(
            ByteString.copyFrom(key.getAddress()))).build()));
  }

  private BlockCapsule nextBlock(ECKey key) {
    long time = chainBaseManager.getHeadBlockNum() == 0 ? 1533529947000L
        : chainBaseManager.getHeadBlockTimeStamp() + 3000;
    return signedBlock(chainBaseManager.getHeadBlockNum() + 1,
        chainBaseManager.getHeadBlockId(), time, key);
  }

  private BlockCapsule signedBlock(long number, Sha256Hash parent, long time, ECKey key) {
    BlockCapsule block = new BlockCapsule(number, parent, time, witness);
    block.setMerkleRoot();
    block.sign(key.getPrivKeyBytes());
    return block;
  }

  private void assertRejected(BlockCapsule block) {
    assertEquals(0, chainBaseManager.getHeadBlockNum());
    assertEquals(chainBaseManager.getGenesisBlockId(), chainBaseManager.getHeadBlockId());
    assertEquals(chainBaseManager.getGenesisBlockId(), chainBaseManager.getKhaosDb().getHeadId());
    assertFalse(chainBaseManager.getKhaosDb().containBlockInMiniStore(block.getBlockId()));
    assertFalse(chainBaseManager.getBlockStore().has(block.getBlockId().getBytes()));
  }
}
