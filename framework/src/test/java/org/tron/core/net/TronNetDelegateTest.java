package org.tron.core.net;

import static org.mockito.Mockito.mock;

import com.google.protobuf.ByteString;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.tron.common.TestConstants;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.ChainBaseManager;
import org.tron.core.Wallet;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.BadBlockException;
import org.tron.core.exception.P2pException;
import org.tron.core.exception.P2pException.TypeEnum;
import org.tron.core.exception.ValidateScheduleException;
import org.tron.core.exception.ValidateSignatureException;
import org.tron.core.metrics.MetricsService;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.store.AccountStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.WitnessScheduleStore;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.contract.BalanceContract.TransferContract;

public class TronNetDelegateTest {

  @Test
  public void test() throws Exception {
    Args.setParam(new String[] {}, TestConstants.TEST_CONF);
    CommonParameter parameter = Args.getInstance();
    Args.logConfig();
    parameter.setUnsolidifiedBlockCheck(true);

    BlockCapsule.BlockId blockId = new BlockCapsule.BlockId(Sha256Hash.ZERO_HASH, 10000L);

    TronNetDelegate tronNetDelegate = new TronNetDelegate();

    ChainBaseManager chainBaseManager = mock(ChainBaseManager.class);
    Mockito.when(chainBaseManager.getHeadBlockNum()).thenReturn(10000L);
    Mockito.when(chainBaseManager.getSolidBlockId()).thenReturn(blockId);

    Field field = tronNetDelegate.getClass().getDeclaredField("chainBaseManager");
    field.setAccessible(true);
    field.set(tronNetDelegate, chainBaseManager);

    Assert.assertTrue(!tronNetDelegate.isBlockUnsolidified());

    blockId = new BlockCapsule.BlockId(Sha256Hash.ZERO_HASH, 1L);
    Mockito.when(chainBaseManager.getSolidBlockId()).thenReturn(blockId);
    Assert.assertTrue(tronNetDelegate.isBlockUnsolidified());

    parameter.setUnsolidifiedBlockCheck(false);
    tronNetDelegate = new TronNetDelegate();

    field = tronNetDelegate.getClass().getDeclaredField("unsolidifiedBlockCheck");
    field.setAccessible(true);
    field.set(tronNetDelegate, false);

    Assert.assertTrue(!tronNetDelegate.isBlockUnsolidified());
  }

  // ── pushVerifiedBlock tests ───────────────────────────────────────────────────

  /**
   * When hitDown is already true, processBlock returns immediately without
   * calling pushBlock and pushVerifiedBlock must not throw.
   */
  @Test
  public void testPushVerifiedBlockSkipsWhenHitDown() throws Exception {
    Args.setParam(new String[] {}, TestConstants.TEST_CONF);
    TronNetDelegate tronNetDelegate = new TronNetDelegate();
    setField(tronNetDelegate, "hitDown", true);

    BlockCapsule block = new BlockCapsule(1, Sha256Hash.ZERO_HASH, 0L, ByteString.EMPTY);
    tronNetDelegate.pushVerifiedBlock(block);

    Assert.assertTrue(block.generatedByMyself);
    Assert.assertTrue(tronNetDelegate.isHitDown());
  }

  /**
   * When the conditional-shutdown threshold is reached, processBlock must set
   * hitDown=true and return without calling pushBlock.
   */
  @Test
  public void testPushVerifiedBlockTriggersShutdown() throws Exception {
    Args.setParam(new String[] {}, TestConstants.TEST_CONF);
    TronNetDelegate tronNetDelegate = new TronNetDelegate();
    tronNetDelegate.init();
    tronNetDelegate.setExit(false); // prevent System.exit(0) in hit-thread

    Manager dbManager = Mockito.mock(Manager.class);
    Mockito.when(dbManager.getLatestSolidityNumShutDown()).thenReturn(50L);
    DynamicPropertiesStore store = Mockito.mock(DynamicPropertiesStore.class);
    Mockito.when(store.getLatestBlockHeaderNumberFromDB()).thenReturn(50L);
    Mockito.when(dbManager.getDynamicPropertiesStore()).thenReturn(store);
    setField(tronNetDelegate, "dbManager", dbManager);

    BlockCapsule block = new BlockCapsule(1, Sha256Hash.ZERO_HASH, 0L, ByteString.EMPTY);
    try {
      tronNetDelegate.pushVerifiedBlock(block);
    } finally {
      tronNetDelegate.close();
    }

    Assert.assertTrue(tronNetDelegate.isHitDown());
    Mockito.verify(dbManager, Mockito.never()).pushBlock(Mockito.any());
  }

  /**
   * On the normal (non-shutdown) path pushBlock must be called exactly once.
   */
  @Test
  public void testPushVerifiedBlockPushesBlock() throws Exception {
    Args.setParam(new String[] {}, TestConstants.TEST_CONF);
    TronNetDelegate tronNetDelegate = new TronNetDelegate();

    Manager dbManager = Mockito.mock(Manager.class);
    Mockito.when(dbManager.getLatestSolidityNumShutDown()).thenReturn(0L);
    Mockito.when(dbManager.getBlockedTimer()).thenReturn(new ThreadLocal<>());

    ChainBaseManager chainBaseManager = Mockito.mock(ChainBaseManager.class);
    Mockito.when(chainBaseManager.getHeadBlockId())
        .thenReturn(new BlockCapsule.BlockId(Sha256Hash.ZERO_HASH, 0L));

    setField(tronNetDelegate, "dbManager", dbManager);
    setField(tronNetDelegate, "chainBaseManager", chainBaseManager);

    BlockCapsule block = new BlockCapsule(1, Sha256Hash.ZERO_HASH, 0L, ByteString.EMPTY);
    tronNetDelegate.pushVerifiedBlock(block);

    Assert.assertTrue(block.generatedByMyself);
    Mockito.verify(dbManager, Mockito.times(1)).pushBlock(Mockito.any());
  }

  @Test
  public void testPushTransactionReturnsAdmissionResult() throws Exception {
    TronNetDelegate tronNetDelegate = new TronNetDelegate();
    Manager dbManager = Mockito.mock(Manager.class);
    TransactionCapsule transaction = new TransactionCapsule(
        TransferContract.getDefaultInstance(), ContractType.TransferContract);
    setField(tronNetDelegate, "dbManager", dbManager);

    Mockito.when(dbManager.pushTransaction(transaction)).thenReturn(false, true);

    Assert.assertFalse(tronNetDelegate.pushTransaction(transaction));
    Assert.assertTrue(tronNetDelegate.pushTransaction(transaction));
  }

  private static void setField(Object obj, String name, Object value) throws Exception {
    Field f = obj.getClass().getDeclaredField(name);
    f.setAccessible(true);
    f.set(obj, value);
  }

  @Test
  public void testValidBlockMerkleRoot() throws Exception {
    Args.setParam(new String[] {}, TestConstants.TEST_CONF);

    String parentHash = "9938a342238077182498b464ac0292229938a342238077182498b464ac029222";
    BlockCapsule block = new BlockCapsule(1,
        Sha256Hash.wrap(ByteString.copyFrom(ByteArray.fromHexString(parentHash))),
        System.currentTimeMillis(),
        ByteString.copyFrom("witness".getBytes()));
    block.setMerkleRoot();

    // Add a transaction after setMerkleRoot, making the stored merkle root stale.
    TransferContract transferContract = TransferContract.newBuilder()
        .setAmount(1L)
        .setOwnerAddress(ByteString.copyFrom("0x0000000000000000000".getBytes()))
        .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(
            Wallet.getAddressPreFixString() + "A389132D6639FBDA4FBC8B659264E6B7C90DB086")))
        .build();
    block.addTransaction(
        new TransactionCapsule(transferContract, ContractType.TransferContract));

    BlockCapsule tampered = new BlockCapsule(block.getInstance());

    TronNetDelegate tronNetDelegate = new TronNetDelegate();
    try {
      tronNetDelegate.validBlock(tampered);
      Assert.fail("Expected P2pException for tampered merkle root");
    } catch (P2pException e) {
      Assert.assertEquals(TypeEnum.BLOCK_MERKLE_INVALID, e.getType());
    }
    PeerConnection peer = new PeerConnection();
    WitnessScheduleStore schedule = mock(WitnessScheduleStore.class);
    setField(tronNetDelegate, "witnessScheduleStore", schedule);
    Mockito.when(schedule.getActiveWitnesses())
        .thenReturn(Collections.singletonList(tampered.getWitnessAddress()));
    try {
      tronNetDelegate.validAdvBlock(peer, tampered);
      Assert.fail("Invalid Merkle root must still be rejected for an active witness");
    } catch (P2pException e) {
      Assert.assertEquals(TypeEnum.BLOCK_MERKLE_INVALID, e.getType());
    }
    Assert.assertEquals(0, peer.getLastInactiveWitnessBlockTime());
  }

  @Test
  public void testValidBlockDoesNotCheckWitnessActivity() throws Exception {
    TronNetDelegate delegate = Mockito.spy(new TronNetDelegate());
    WitnessScheduleStore schedule = mock(WitnessScheduleStore.class);
    setField(delegate, "witnessScheduleStore", schedule);
    BlockCapsule block = new BlockCapsule(1, Sha256Hash.ZERO_HASH, 0,
        ByteString.copyFromUtf8("witness"));
    block.setMerkleRoot();
    Mockito.doNothing().when(delegate).validSignature(block);
    delegate.validBlock(block);
    Mockito.verify(schedule, Mockito.never()).getActiveWitnesses();
  }

  @Test
  public void testValidAdvBlockChecksSignatureAfterWitnessActivity() throws Exception {
    TronNetDelegate delegate = Mockito.spy(new TronNetDelegate());
    WitnessScheduleStore schedule = mock(WitnessScheduleStore.class);
    setField(delegate, "witnessScheduleStore", schedule);
    Mockito.when(schedule.getActiveWitnesses()).thenReturn(Collections.emptyList());
    Mockito.doReturn(21_600_000L).when(delegate).getMaintenanceTimeInterval();
    BlockCapsule block = new BlockCapsule(1, Sha256Hash.ZERO_HASH, 0, ByteString.EMPTY);
    block.setMerkleRoot();
    P2pException failure = new P2pException(TypeEnum.BLOCK_SIGN_INVALID, "invalid signature");
    Mockito.doThrow(failure).when(delegate).validSignature(block);
    PeerConnection peer = Mockito.spy(new PeerConnection());
    Mockito.doReturn(new InetSocketAddress("127.0.0.1", 18888))
        .when(peer).getInetSocketAddress();
    try {
      delegate.validAdvBlock(peer, block);
      Assert.fail("Invalid signature must still be rejected after witness activity handling");
    } catch (P2pException e) {
      Assert.assertSame(failure, e);
    }
    Assert.assertTrue(peer.getLastInactiveWitnessBlockTime() > 0);
  }

  @Test
  public void testRepeatedInactiveWitnessIsRejectedBeforeBasicValidation() throws Exception {
    TronNetDelegate delegate = Mockito.spy(new TronNetDelegate());
    WitnessScheduleStore schedule = mock(WitnessScheduleStore.class);
    setField(delegate, "witnessScheduleStore", schedule);
    Mockito.when(schedule.getActiveWitnesses()).thenReturn(Collections.emptyList());
    Mockito.doReturn(21_600_000L).when(delegate).getMaintenanceTimeInterval();
    BlockCapsule block = new BlockCapsule(1, Sha256Hash.ZERO_HASH, 0, ByteString.EMPTY);
    PeerConnection peer = Mockito.spy(new PeerConnection());
    Mockito.doReturn(new InetSocketAddress("127.0.0.1", 18888))
        .when(peer).getInetSocketAddress();
    peer.setLastInactiveWitnessBlockTime(System.currentTimeMillis());

    try {
      delegate.validAdvBlock(peer, block);
      Assert.fail("Repeated inactive witness must be rejected before basic validation");
    } catch (P2pException e) {
      Assert.assertEquals(TypeEnum.BAD_BLOCK, e.getType());
    }
    Mockito.verify(delegate, Mockito.never()).validBlock(block);
  }

  @Test
  public void testValidAdvBlockAcceptsActiveWitnessWithoutChangingHistory() throws Exception {
    TronNetDelegate delegate = Mockito.spy(new TronNetDelegate());
    WitnessScheduleStore schedule = mock(WitnessScheduleStore.class);
    setField(delegate, "witnessScheduleStore", schedule);
    BlockCapsule block = new BlockCapsule(1, Sha256Hash.ZERO_HASH, 0,
        ByteString.copyFromUtf8("witness"));
    block.setMerkleRoot();
    Mockito.doNothing().when(delegate).validSignature(block);
    Mockito.when(schedule.getActiveWitnesses())
        .thenReturn(Collections.singletonList(block.getWitnessAddress()));
    PeerConnection peer = new PeerConnection();
    peer.setLastInactiveWitnessBlockTime(86_400_000L);

    Assert.assertTrue(delegate.validAdvBlock(peer, block));

    Mockito.verify(delegate).validSignature(block);
    Assert.assertEquals(86_400_000L, peer.getLastInactiveWitnessBlockTime());
  }

  @Test
  public void testValidBlockRejectsInvalidContractCounts() throws Exception {
    for (int count : new int[]{0, 2}) {
      for (boolean laterTransaction : new boolean[]{false, true}) {
        TronNetDelegate delegate = signatureDelegate();
        BlockCapsule block = Mockito.spy(new BlockCapsule(1, Sha256Hash.ZERO_HASH, 0,
            ByteString.copyFromUtf8("witness")));
        if (laterTransaction) {
          block.addTransaction(contractCountTransaction(1));
        }
        block.addTransaction(contractCountTransaction(count));
        block.setMerkleRoot();
        Mockito.doReturn(true).when(block).validateSignature(Mockito.any(), Mockito.any());
        WitnessScheduleStore schedule = mock(WitnessScheduleStore.class);
        Mockito.doReturn(Collections.singletonList(block.getWitnessAddress()))
            .when(schedule).getActiveWitnesses();
        setField(delegate, "witnessScheduleStore", schedule);

        P2pException error = Assert.assertThrows(P2pException.class,
            () -> delegate.validBlock(block));

        Assert.assertEquals(TypeEnum.BAD_BLOCK, error.getType());
        Assert.assertTrue(error.getMessage().contains(block.getBlockId().getString()));
        Assert.assertTrue(error.getMessage().contains("count: " + count));
        Mockito.verify(block, Mockito.never()).validateSignature(Mockito.any(), Mockito.any());
      }
    }
  }

  @Test
  public void testValidBlockAcceptsEmptyAndSingleContractTransactions() throws Exception {
    for (int transactions : new int[]{0, 1, 2}) {
      TronNetDelegate delegate = signatureDelegate();
      BlockCapsule block = Mockito.spy(new BlockCapsule(1, Sha256Hash.ZERO_HASH, 0,
          ByteString.copyFromUtf8("witness")));
      for (int i = 0; i < transactions; i++) {
        block.addTransaction(contractCountTransaction(1));
      }
      block.setMerkleRoot();
      Mockito.doReturn(true).when(block).validateSignature(Mockito.any(), Mockito.any());
      WitnessScheduleStore schedule = mock(WitnessScheduleStore.class);
      Mockito.doReturn(Collections.singletonList(block.getWitnessAddress()))
          .when(schedule).getActiveWitnesses();
      setField(delegate, "witnessScheduleStore", schedule);

      delegate.validBlock(block);

      Mockito.verify(block).validateSignature(Mockito.any(), Mockito.any());
    }
  }

  private TransactionCapsule contractCountTransaction(int count) {
    Transaction.raw.Builder raw = Transaction.raw.newBuilder();
    for (int i = 0; i < count; i++) {
      raw.addContract(Transaction.Contract.newBuilder().setType(ContractType.TransferContract));
    }
    return new TransactionCapsule(Transaction.newBuilder().setRawData(raw).build());
  }

  @Test
  public void testFutureTimeCutoffAppliesToCommonAndAdvertisedBlocks() throws Exception {
    TronNetDelegate delegate = signatureDelegate();
    BlockCapsule block = Mockito.spy(new BlockCapsule(1, Sha256Hash.ZERO_HASH,
        System.currentTimeMillis() + 60000, ByteString.copyFromUtf8("witness")));
    block.setMerkleRoot();
    Mockito.doReturn(true).when(block).validateSignature(Mockito.any(), Mockito.any());
    WitnessScheduleStore schedule = mock(WitnessScheduleStore.class);
    Mockito.doReturn(Collections.singletonList(block.getWitnessAddress()))
        .when(schedule).getActiveWitnesses();
    setField(delegate, "witnessScheduleStore", schedule);

    P2pException commonError = Assert.assertThrows(P2pException.class,
        () -> delegate.validBlock(block));
    P2pException advertisedError = Assert.assertThrows(P2pException.class,
        () -> delegate.validAdvBlock(new PeerConnection(), block));

    Assert.assertEquals(TypeEnum.BAD_BLOCK, commonError.getType());
    Assert.assertEquals(TypeEnum.BAD_BLOCK, advertisedError.getType());
    Assert.assertTrue(commonError.getMessage().contains("block time:"));
    Assert.assertTrue(advertisedError.getMessage().contains("block time:"));
    Mockito.verify(block, Mockito.never()).validateSignature(Mockito.any(), Mockito.any());
  }

  @Test
  public void testValidBlockRejectsFalseSignature() throws Exception {
    TronNetDelegate delegate = signatureDelegate();
    BlockCapsule block = Mockito.spy(new BlockCapsule(1, Sha256Hash.ZERO_HASH, 0,
        ByteString.copyFromUtf8("witness")));
    block.setMerkleRoot();
    Mockito.doReturn(false).when(block).validateSignature(Mockito.any(), Mockito.any());

    P2pException error = Assert.assertThrows(P2pException.class,
        () -> delegate.validBlock(block));

    Assert.assertEquals(TypeEnum.BLOCK_SIGN_INVALID, error.getType());
    Assert.assertEquals("valid signature failed.", error.getMessage());
  }

  @Test
  public void testValidBlockWrapsSignatureFailureWithCause() throws Exception {
    for (Exception failure : new Exception[]{new ValidateSignatureException("invalid signature"),
        new IllegalStateException("signature validation unavailable")}) {
      TronNetDelegate delegate = signatureDelegate();
      BlockCapsule block = Mockito.spy(new BlockCapsule(1, Sha256Hash.ZERO_HASH, 0,
          ByteString.copyFromUtf8("witness")));
      block.setMerkleRoot();
      Mockito.doThrow(failure).when(block).validateSignature(Mockito.any(), Mockito.any());

      P2pException error = Assert.assertThrows(P2pException.class,
          () -> delegate.validBlock(block));

      Assert.assertEquals(TypeEnum.BLOCK_SIGN_INVALID, error.getType());
      Assert.assertSame(failure, error.getCause());
      Assert.assertEquals(failure.toString(), error.getMessage());
    }
  }

  private TronNetDelegate signatureDelegate() throws Exception {
    Manager manager = mock(Manager.class);
    Mockito.when(manager.getDynamicPropertiesStore())
        .thenReturn(mock(DynamicPropertiesStore.class));
    Mockito.when(manager.getAccountStore()).thenReturn(mock(AccountStore.class));
    TronNetDelegate delegate = Mockito.spy(new TronNetDelegate());
    setField(delegate, "dbManager", manager);
    return delegate;
  }

  @Test
  public void testProcessBlockWrapsScheduleFailureWithCause() throws Exception {
    assertProcessBlockFailure(new ValidateScheduleException("invalid scheduled witness"));
  }

  @Test
  public void testProcessBlockWrapsMerkleFailureWithCause() throws Exception {
    assertProcessBlockFailure(new BadBlockException(
        BadBlockException.TypeEnum.CALC_MERKLE_ROOT_FAILED, "invalid merkle root"));
  }

  private void assertProcessBlockFailure(Exception failure) throws Exception {
    TronNetDelegate delegate = new TronNetDelegate();
    Manager manager = mock(Manager.class);
    ChainBaseManager chainManager = mock(ChainBaseManager.class);
    setField(delegate, "dbManager", manager);
    setField(delegate, "chainBaseManager", chainManager);
    setField(delegate, "metricsService", mock(MetricsService.class));
    Mockito.when(manager.getBlockedTimer()).thenReturn(new ThreadLocal<>());
    Mockito.when(chainManager.getHeadBlockId())
        .thenReturn(new BlockCapsule.BlockId(Sha256Hash.ZERO_HASH, 0));
    BlockCapsule block = new BlockCapsule(1, Sha256Hash.ZERO_HASH, 0, ByteString.EMPTY);
    Mockito.doThrow(failure).when(manager).pushBlock(block);
    try {
      delegate.processBlock(block, true);
      Assert.fail("Expected block validation failure");
    } catch (P2pException e) {
      Assert.assertEquals(TypeEnum.BAD_BLOCK, e.getType());
      Assert.assertSame(failure, e.getCause());
    }
  }
}
