package org.tron.core.db;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import org.tron.common.cron.CronExpression;
import org.tron.common.logsfilter.EventPluginLoader;
import org.tron.common.logsfilter.trigger.ContractLogTrigger;
import org.tron.common.logsfilter.trigger.ContractTrigger;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.runtime.ProgramResult;
import org.tron.common.runtime.vm.LogInfo;
import org.tron.common.utils.Pair;
import org.tron.common.utils.Sha256Hash;
import org.tron.consensus.Consensus;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.TransactionInfoCapsule;
import org.tron.core.capsule.utils.TransactionUtil;
import org.tron.core.config.args.Args;
import org.tron.core.db2.ISession;
import org.tron.core.exception.ContractSizeNotEqualToOneException;
import org.tron.core.exception.DupTransactionException;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.exception.ReceiptCheckErrException;
import org.tron.core.exception.TaposException;
import org.tron.core.exception.TooBigTransactionException;
import org.tron.core.exception.TooBigTransactionResultException;
import org.tron.core.exception.TransactionExpirationException;
import org.tron.core.exception.ValidateSignatureException;
import org.tron.core.store.AccountStore;
import org.tron.core.store.BalanceTraceStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.protos.Protocol;
import org.tron.protos.contract.BalanceContract;

@Slf4j
public class ManagerMockTest {
  @After
  public void  clearMocks() {
    Mockito.framework().clearInlineMocks();
  }

  @Test
  public void processTransactionCostTimeMoreThan100() throws Exception {
    ProgramResult result = new ProgramResult();
    result.setResultCode(Protocol.Transaction.Result.contractResult.SUCCESS);
    try (MockedConstruction<TransactionTrace> traces
             = mockConstruction(TransactionTrace.class, (trace, context) ->
                 when(trace.getRuntimeResult()).thenReturn(result));
         MockedStatic<TransactionUtil> mockedStatic = mockStatic(TransactionUtil.class)) {
      Manager dbManager = spy(new Manager());
      BalanceContract.TransferContract transferContract =
          BalanceContract.TransferContract.newBuilder()
              .setAmount(10)
              .setOwnerAddress(ByteString.copyFromUtf8("aaa"))
              .setToAddress(ByteString.copyFromUtf8("bbb"))
              .build();
      Protocol.Transaction transaction = Protocol.Transaction.newBuilder().setRawData(
          Protocol.Transaction.raw.newBuilder()
              .addContract(
                  Protocol.Transaction.Contract.newBuilder()
                      .setParameter(Any.pack(transferContract))
                      .setType(Protocol.Transaction.Contract.ContractType.TransferContract)))
          .build();
      TransactionCapsule trxCap = spy(new TransactionCapsule(transaction));
      Sha256Hash transactionId = trxCap.getTransactionId();

      ChainBaseManager chainBaseManagerMock = mock(ChainBaseManager.class);
      BalanceTraceStore balanceTraceStoreMock = mock(BalanceTraceStore.class);
      TransactionStore transactionStoreMock = mock(TransactionStore.class);
      Protocol.TransactionInfo transactionInfo = Protocol.TransactionInfo.newBuilder()
          .setId(transactionId.getByteString()).setFee(100L).build();

      Field field = Manager.class.getDeclaredField("chainBaseManager");
      field.setAccessible(true);
      field.set(dbManager, chainBaseManagerMock);

      BlockCapsule blockCapMock = Mockito.mock(BlockCapsule.class);

      mockedStatic.when(() -> TransactionUtil.buildTransactionInfoInstance(
          eq(trxCap), eq(blockCapMock), any(TransactionTrace.class)))
          .thenReturn(new TransactionInfoCapsule(transactionInfo));
      when(blockCapMock.isMerkleRootEmpty()).thenAnswer(invocation -> {
        Thread.sleep(110);
        return true;
      });

      when(chainBaseManagerMock.getBalanceTraceStore()).thenReturn(balanceTraceStoreMock);
      when(chainBaseManagerMock.getAccountStore()).thenReturn(mock(AccountStore.class));
      when(chainBaseManagerMock.getDynamicPropertiesStore())
          .thenReturn(mock(DynamicPropertiesStore.class));
      when(chainBaseManagerMock.getTransactionStore()).thenReturn(transactionStoreMock);
      doReturn(true).when(trxCap).validateSignature(
          any(AccountStore.class), any(DynamicPropertiesStore.class));
      doNothing().when(dbManager).validateTapos(trxCap);
      doNothing().when(dbManager).validateCommon(trxCap);
      doNothing().when(dbManager).validateDup(trxCap);
      doNothing().when(dbManager).consumeBandwidth(eq(trxCap), any(TransactionTrace.class));

      Assert.assertEquals(transactionInfo, dbManager.processTransaction(trxCap, blockCapMock));

      Assert.assertEquals(1, traces.constructed().size());
      TransactionTrace trace = traces.constructed().get(0);
      verify(trace).init(blockCapMock, false);
      verify(trace).exec();
      verify(trace).finalization();
      // The second call selects the log label only after processing exceeds 100 ms.
      verify(blockCapMock, times(2)).hasWitnessSignature();
      verify(transactionStoreMock).put(transactionId.getBytes(), trxCap);
      verify(balanceTraceStoreMock).initCurrentTransactionBalanceTrace(trxCap);
      verify(balanceTraceStoreMock).updateCurrentTransactionStatus("SUCCESS");
      verify(balanceTraceStoreMock).resetCurrentTransactionTrace();
      assertTrue(trxCap.isInBlock());
      Assert.assertEquals(100L, trxCap.getOrder());
      Assert.assertNull(trxCap.getTrxTrace());
    }
  }

  private void initMockEnv(Manager dbManager, long headNum, long headTime,
                           long exitHeight, long exitCount, String blockTime)
      throws Exception {
    ChainBaseManager chainBaseManagerMock = mock(ChainBaseManager.class);
    CommonParameter parameter = new CommonParameter();
    parameter.setShutdownBlockHeight(exitHeight);
    parameter.setShutdownBlockCount(exitCount);
    parameter.setShutdownBlockTime(blockTime == null ? null : new CronExpression(blockTime));
    when(CommonParameter.getInstance()).thenReturn(parameter);

    when(chainBaseManagerMock.getHeadBlockNum()).thenReturn(headNum);
    when(chainBaseManagerMock.getHeadBlockTimeStamp()).thenReturn(headTime);

    Field field = Manager.class.getDeclaredField("chainBaseManager");
    field.setAccessible(true);
    field.set(dbManager, chainBaseManagerMock);
  }

  @Test
  public void testInitAutoStop() throws Exception {
    Manager dbManager = new Manager();
    try (MockedStatic<CommonParameter> methodTestMockedStatic
             = mockStatic(CommonParameter.class)) {
      initMockEnv(dbManager, 100L, 12345L,
          10L, -1L, null);

      InvocationTargetException thrown = assertThrows(
          InvocationTargetException.class,
          () -> {
            Method privateMethod = Manager.class.getDeclaredMethod(
                "initAutoStop");
            privateMethod.setAccessible(true);
            privateMethod.invoke(dbManager);
          }
      );
      Assert.assertEquals(IllegalArgumentException.class, thrown.getCause().getClass());
      Assert.assertEquals("shutDownBlockHeight 10 is less than headNum 100",
          thrown.getCause().getMessage());
    }

  }

  @Test
  public void testInitAutoStop1() throws Exception {
    Manager dbManager = new Manager();
    try (MockedStatic<CommonParameter> methodTestMockedStatic
             = mockStatic(CommonParameter.class)) {
      initMockEnv(dbManager,10L, 12345L,
          0L, 0L, null);

      InvocationTargetException thrown = assertThrows(
          InvocationTargetException.class,
          () -> {
            Method privateMethod = Manager.class.getDeclaredMethod(
                "initAutoStop");
            privateMethod.setAccessible(true);
            privateMethod.invoke(dbManager);
          }
      );
      Assert.assertEquals(IllegalArgumentException.class, thrown.getCause().getClass());
      Assert.assertEquals("shutDownBlockCount 0 is less than 1", thrown.getCause().getMessage());
    }
  }

  @Test
  public void testInitAutoStop2() throws Exception {
    Manager dbManager = new Manager();
    try (MockedStatic<CommonParameter> methodTestMockedStatic
             = mockStatic(CommonParameter.class)) {
      // The scheduled time in 2020 has passed at this head timestamp (2021-01-01 UTC).
      initMockEnv(dbManager, 10L, 1609459200000L,
          0L, -1L, "0 0 12 1 1 ? 2020");

      InvocationTargetException thrown = assertThrows(
          InvocationTargetException.class,
          () -> {
            Method privateMethod = Manager.class.getDeclaredMethod(
                "initAutoStop");
            privateMethod.setAccessible(true);
            privateMethod.invoke(dbManager);
          }
      );
      Assert.assertEquals(IllegalArgumentException.class, thrown.getCause().getClass());
      Assert.assertEquals("shutDownBlockTime 0 0 12 1 1 ? 2020 is illegal",
          thrown.getCause().getMessage());
    }

  }

  @Test
  public void testInitAutoStop3() throws Exception {
    Manager dbManager = new Manager();
    try (MockedStatic<CommonParameter> methodTestMockedStatic
             = mockStatic(CommonParameter.class)) {
      initMockEnv(dbManager,10L, 12345L,
          100L, 1L, null);

      InvocationTargetException thrown = assertThrows(
          InvocationTargetException.class,
          () -> {
            Method privateMethod = Manager.class.getDeclaredMethod(
                "initAutoStop");
            privateMethod.setAccessible(true);
            privateMethod.invoke(dbManager);
          }
      );
      Assert.assertEquals(IllegalArgumentException.class, thrown.getCause().getClass());
      Assert.assertEquals("shutDownBlockHeight 100 and shutDownBlockCount 1 set both",
          thrown.getCause().getMessage());
    }

  }

  @Test
  public void testInitAutoStop4() throws Exception {
    Manager dbManager = new Manager();
    try (MockedStatic<CommonParameter> methodTestMockedStatic
             = mockStatic(CommonParameter.class)) {
      initMockEnv(dbManager, 10L, 12345L,
          100L, -1L, "0 0 12 * * ?");

      InvocationTargetException thrown = assertThrows(
          InvocationTargetException.class,
          () -> {
            Method privateMethod = Manager.class.getDeclaredMethod(
                "initAutoStop");
            privateMethod.setAccessible(true);
            privateMethod.invoke(dbManager);
          }
      );
      Assert.assertEquals(IllegalArgumentException.class, thrown.getCause().getClass());
      Assert.assertEquals("shutDownBlockHeight 100 and shutDownBlockTime 0 0 12 * * ? set both",
          thrown.getCause().getMessage());
    }

  }

  @Test
  public void testInitAutoStop5() throws Exception {
    Manager dbManager = new Manager();
    try (MockedStatic<CommonParameter> methodTestMockedStatic
             = mockStatic(CommonParameter.class)) {
      initMockEnv(dbManager,10L, 12345L,
          0L, 1L, "0 0 12 * * ?");

      InvocationTargetException thrown = assertThrows(
          InvocationTargetException.class,
          () -> {
            Method privateMethod = Manager.class.getDeclaredMethod(
                "initAutoStop");
            privateMethod.setAccessible(true);
            privateMethod.invoke(dbManager);
          }
      );
      Assert.assertEquals(IllegalArgumentException.class, thrown.getCause().getClass());
      Assert.assertEquals("shutDownBlockCount 1 and shutDownBlockTime 0 0 12 * * ? set both",
          thrown.getCause().getMessage());
    }

  }

  @Test
  public void testProcessTransaction() throws Exception {
    Manager dbManager = spy(new Manager());
    TransactionCapsule transactionCapsuleMock = null;
    BlockCapsule blockCapsuleMock = mock(BlockCapsule.class);

    Method privateMethod = Manager.class.getDeclaredMethod(
        "processTransaction",
        TransactionCapsule.class, BlockCapsule.class);
    privateMethod.setAccessible(true);
    privateMethod.invoke(dbManager, transactionCapsuleMock, blockCapsuleMock);
  }

  @Test
  public void testProcessTransaction1() {
    Manager dbManager = spy(new Manager());
    Protocol.Transaction transaction = Protocol.Transaction.newBuilder().setRawData(
        Protocol.Transaction.raw.newBuilder()
            .setData(ByteString.copyFrom("sb.toString()".getBytes(StandardCharsets.UTF_8))))
        .build();
    TransactionCapsule trxCap = new TransactionCapsule(transaction);

    BlockCapsule blockCapsuleMock = mock(BlockCapsule.class);

    Throwable thrown = assertThrows(InvocationTargetException.class, () -> {
      Method privateMethod = Manager.class.getDeclaredMethod(
          "processTransaction",
          TransactionCapsule.class, BlockCapsule.class);
      privateMethod.setAccessible(true);
      privateMethod.invoke(dbManager, trxCap, blockCapsuleMock);
    });
    Throwable cause = thrown.getCause();
    assertTrue(cause instanceof ContractSizeNotEqualToOneException);
  }

  @SneakyThrows
  @Test
  public void testRePush() {
    Manager dbManager = spy(new Manager());
    Protocol.Transaction transaction = Protocol.Transaction.newBuilder().build();
    TransactionCapsule trx = new TransactionCapsule(transaction);
    TransactionStore transactionStoreMock = mock(TransactionStore.class);

    ChainBaseManager chainBaseManagerMock = mock(ChainBaseManager.class);
    Field field = dbManager.getClass().getDeclaredField("chainBaseManager");
    field.setAccessible(true);
    field.set(dbManager, chainBaseManagerMock);
    when(chainBaseManagerMock.getTransactionStore()).thenReturn(transactionStoreMock);
    when(transactionStoreMock.has(any())).thenReturn(true);

    dbManager.rePush(trx);
  }

  @SneakyThrows
  @Test
  public void testRePush1() {
    Manager dbManager = spy(new Manager());
    BalanceContract.TransferContract transferContract =
        BalanceContract.TransferContract.newBuilder()
            .setOwnerAddress(ByteString.copyFromUtf8("aaa"))
            .setToAddress(ByteString.copyFromUtf8("bbb"))
            .setAmount(1)
            .build();
    Protocol.Transaction transaction = Protocol.Transaction.newBuilder()
        .setRawData(Protocol.Transaction.raw.newBuilder()
            .addContract(Protocol.Transaction.Contract.newBuilder()
                .setParameter(Any.pack(transferContract))
                .setType(Protocol.Transaction.Contract.ContractType.TransferContract)))
        .build();
    TransactionCapsule trx = new TransactionCapsule(transaction);
    TransactionStore transactionStoreMock = mock(TransactionStore.class);

    ChainBaseManager chainBaseManagerMock = mock(ChainBaseManager.class);

    Field field = dbManager.getClass().getDeclaredField("chainBaseManager");
    field.setAccessible(true);
    field.set(dbManager, chainBaseManagerMock);

    when(chainBaseManagerMock.getTransactionStore()).thenReturn(transactionStoreMock);
    when(transactionStoreMock.has(any())).thenReturn(false);

    doThrow(new ValidateSignatureException()).when(dbManager).pushTransaction(any());
    dbManager.rePush(trx);

    doThrow(new DupTransactionException()).when(dbManager).pushTransaction(any());
    dbManager.rePush(trx);

    doThrow(new TaposException()).when(dbManager).pushTransaction(any());
    dbManager.rePush(trx);

    doThrow(new TooBigTransactionException()).when(dbManager).pushTransaction(any());
    dbManager.rePush(trx);

    doThrow(new TransactionExpirationException()).when(dbManager).pushTransaction(any());
    dbManager.rePush(trx);

    doThrow(new ReceiptCheckErrException()).when(dbManager).pushTransaction(any());
    dbManager.rePush(trx);

    doThrow(new TooBigTransactionResultException()).when(dbManager).pushTransaction(any());
    dbManager.rePush(trx);
  }

  @Test
  public void testPostSolidityFilter() throws Exception {
    Manager dbManager = spy(new Manager());

    Method privateMethod = Manager.class.getDeclaredMethod(
        "postSolidityFilter", long.class, long.class);
    privateMethod.setAccessible(true);
    privateMethod.invoke(dbManager, 100L, 10L);
  }

  @Test
  public void testReOrgLogsFilter() throws Exception {
    Manager dbManager = spy(new Manager());
    CommonParameter commonParameterMock = mock(Args.class);
    mockStatic(CommonParameter.class);
    ChainBaseManager chainBaseManagerMock = mock(ChainBaseManager.class);

    when(CommonParameter.getInstance()).thenReturn(commonParameterMock);
    when(commonParameterMock.isJsonRpcHttpFullNodeEnable()).thenReturn(true);
    when(chainBaseManagerMock.getDynamicPropertiesStore())
        .thenReturn(mock(DynamicPropertiesStore.class));
    Field field = dbManager.getClass().getDeclaredField("chainBaseManager");
    field.setAccessible(true);
    field.set(dbManager, chainBaseManagerMock);
    doThrow(new ItemNotFoundException()).when(chainBaseManagerMock).getBlockById(any());

    Method privateMethod = Manager.class.getDeclaredMethod("reOrgLogsFilter");
    privateMethod.setAccessible(true);
    privateMethod.invoke(dbManager);
  }

  @Test
  public void testPostContractTriggerProcessesSync() throws Exception {
    Manager dbManager = spy(new Manager());
    Field eventLoadedField = Manager.class.getDeclaredField("eventPluginLoaded");
    eventLoadedField.setAccessible(true);
    eventLoadedField.set(dbManager, true);

    ChainBaseManager cbm = mock(ChainBaseManager.class);
    DynamicPropertiesStore dps = mock(DynamicPropertiesStore.class);
    when(dps.getLatestSolidifiedBlockNum()).thenReturn(0L);
    when(cbm.getDynamicPropertiesStore()).thenReturn(dps);
    Field cbmField = Manager.class.getDeclaredField("chainBaseManager");
    cbmField.setAccessible(true);
    cbmField.set(dbManager, cbm);

    EventPluginLoader mockLoader = mock(EventPluginLoader.class);
    when(mockLoader.isContractLogTriggerEnable()).thenReturn(false);
    when(mockLoader.isContractEventTriggerEnable()).thenReturn(false);
    when(mockLoader.isSolidityLogTriggerEnable()).thenReturn(true);
    when(mockLoader.isSolidityEventTriggerEnable()).thenReturn(false);

    Field instanceField = EventPluginLoader.class.getDeclaredField("instance");
    instanceField.setAccessible(true);
    EventPluginLoader original = (EventPluginLoader) instanceField.get(null);
    instanceField.set(null, mockLoader);

    Args.getSolidityContractLogTriggerMap().clear();

    try {
      ContractLogTrigger trigger = new ContractLogTrigger();
      trigger.setBlockNumber(200L);
      trigger.setTransactionId("tx-id");
      trigger.setContractAddress("0x01");
      trigger.setLogInfo(new LogInfo(new byte[0], new ArrayList<>(), new byte[0]));

      TransactionTrace traceMock = mock(TransactionTrace.class);
      ProgramResult resultMock = mock(ProgramResult.class);
      when(traceMock.getRuntimeResult()).thenReturn(resultMock);
      List<ContractTrigger> triggers = new ArrayList<>();
      triggers.add(trigger);
      when(resultMock.getTriggerList()).thenReturn(triggers);

      Method method = Manager.class.getDeclaredMethod("postContractTrigger",
          TransactionTrace.class, boolean.class, String.class);
      method.setAccessible(true);
      method.invoke(dbManager, traceMock, false, "blockhash");

      Assert.assertNotNull(
          "synchronous processTrigger should populate solidity log map",
          Args.getSolidityContractLogTriggerMap().get(200L));
    } finally {
      instanceField.set(null, original);
      eventLoadedField.set(dbManager, false);
      Args.getSolidityContractLogTriggerMap().clear();
    }
  }

  @Test
  public void testPostContractTriggerSwallowsThrowable() throws Exception {
    Manager dbManager = spy(new Manager());
    Field eventLoadedField = Manager.class.getDeclaredField("eventPluginLoaded");
    eventLoadedField.setAccessible(true);
    eventLoadedField.set(dbManager, true);

    ChainBaseManager cbm = mock(ChainBaseManager.class);
    DynamicPropertiesStore dps = mock(DynamicPropertiesStore.class);
    when(dps.getLatestSolidifiedBlockNum()).thenReturn(0L);
    when(cbm.getDynamicPropertiesStore()).thenReturn(dps);
    Field cbmField = Manager.class.getDeclaredField("chainBaseManager");
    cbmField.setAccessible(true);
    cbmField.set(dbManager, cbm);

    EventPluginLoader mockLoader = mock(EventPluginLoader.class);
    when(mockLoader.isContractLogTriggerEnable()).thenReturn(false);
    when(mockLoader.isContractEventTriggerEnable()).thenReturn(false);
    when(mockLoader.isSolidityLogTriggerEnable()).thenReturn(true);
    when(mockLoader.isSolidityEventTriggerEnable()).thenReturn(false);

    Field instanceField = EventPluginLoader.class.getDeclaredField("instance");
    instanceField.setAccessible(true);
    EventPluginLoader original = (EventPluginLoader) instanceField.get(null);
    instanceField.set(null, mockLoader);

    try {
      // null logInfo → processTrigger throws NPE on logInfo.getTopics()
      ContractLogTrigger trigger = new ContractLogTrigger();
      trigger.setBlockNumber(300L);
      trigger.setTransactionId("tx-id");
      trigger.setContractAddress("0x01");

      TransactionTrace traceMock = mock(TransactionTrace.class);
      ProgramResult resultMock = mock(ProgramResult.class);
      when(traceMock.getRuntimeResult()).thenReturn(resultMock);
      when(resultMock.getTriggerList())
          .thenReturn(Collections.singletonList((ContractTrigger) trigger));

      Method method = Manager.class.getDeclaredMethod("postContractTrigger",
          TransactionTrace.class, boolean.class, String.class);
      method.setAccessible(true);
      // catch (Throwable) absorbs the NPE — invocation must complete normally
      method.invoke(dbManager, traceMock, false, "blockhash");
    } finally {
      instanceField.set(null, original);
      eventLoadedField.set(dbManager, false);
    }
  }

  /**
   * Covers the fork-replay signature recheck added in this PR:
   * when a block being re-applied during switchFork fails witness signature
   * validation, the new `if (!validateSignature) throw` block must fire,
   * surfacing ValidateSignatureException through the existing catch list.
   *
   * The old branch fails schedule validation during rollback, which switchFork
   * handles without replacing the new branch's signature-validation failure.
   */
  @SneakyThrows
  @Test
  public void testSwitchForkRejectsBlockWithInvalidSignature() {
    Manager dbManager = spy(new Manager());
    Consensus consensus = mock(Consensus.class);
    setField(dbManager, "consensus", consensus);

    // chainBaseManager + stores so getDynamicPropertiesStore() / getAccountStore() resolve.
    ChainBaseManager cbm = mock(ChainBaseManager.class);
    DynamicPropertiesStore dps = mock(DynamicPropertiesStore.class);
    AccountStore accountStore = mock(AccountStore.class);
    Sha256Hash sharedHash = Sha256Hash.ZERO_HASH;
    when(cbm.getDynamicPropertiesStore()).thenReturn(dps);
    when(cbm.getAccountStore()).thenReturn(accountStore);
    when(dps.getLatestBlockHeaderHash()).thenReturn(sharedHash);
    setField(dbManager, "chainBaseManager", cbm);

    // revokingStore.buildSession() returns a no-op ISession.
    RevokingDatabase revokingStore = mock(RevokingDatabase.class);
    ISession session = mock(ISession.class);
    when(revokingStore.buildSession()).thenReturn(session);
    setField(dbManager, "revokingStore", revokingStore);

    // khaosDb.getBranch returns (first=[badBlock], value=[oldBlock]).
    // The bad block goes into the apply loop; the old block lets the while
    // loops in the rollback/switchback paths exit immediately by matching
    // parent hash to the current head hash.
    KhaosDatabase khaosDb = mock(KhaosDatabase.class);
    setField(dbManager, "khaosDb", khaosDb);

    BlockCapsule badBlock = mock(BlockCapsule.class);
    BlockCapsule.BlockId badBlockId = mock(BlockCapsule.BlockId.class);
    when(badBlock.getBlockId()).thenReturn(badBlockId);
    when(badBlock.getNum()).thenReturn(100L);
    when(badBlock.validateSignature(any(DynamicPropertiesStore.class),
        any(AccountStore.class))).thenReturn(false);

    BlockCapsule oldBlock = mock(BlockCapsule.class);
    BlockCapsule.BlockId oldBlockId = mock(BlockCapsule.BlockId.class);
    when(oldBlock.getBlockId()).thenReturn(oldBlockId);
    when(oldBlock.getParentHash()).thenReturn(sharedHash);
    when(oldBlock.setSwitch(true)).thenReturn(oldBlock);

    LinkedList<KhaosDatabase.KhaosBlock> first = new LinkedList<>();
    first.add(new KhaosDatabase.KhaosBlock(badBlock));
    LinkedList<KhaosDatabase.KhaosBlock> value = new LinkedList<>();
    value.add(new KhaosDatabase.KhaosBlock(oldBlock));
    when(khaosDb.getBranch(any(BlockCapsule.BlockId.class), any(Sha256Hash.class)))
        .thenReturn(new Pair<>(first, value));

    Method switchFork = Manager.class.getDeclaredMethod("switchFork", BlockCapsule.class);
    switchFork.setAccessible(true);

    InvocationTargetException exception = assertThrows(InvocationTargetException.class,
        () -> switchFork.invoke(dbManager, badBlock));
    Assert.assertEquals(ValidateSignatureException.class, exception.getCause().getClass());
    Assert.assertEquals("switch fork: block 100 signature invalid",
        exception.getCause().getMessage());

    // The fix's contract: validateSignature was invoked on the replayed block.
    verify(badBlock, atLeastOnce()).validateSignature(
        any(DynamicPropertiesStore.class), any(AccountStore.class));
    verify(badBlock, never()).setSwitch(true);
    verify(consensus, never()).validBlock(badBlock);
    verify(khaosDb).removeBlk(badBlockId);
  }

  /**
   * Symmetric "happy path" coverage: when validateSignature returns true, the
   * throw is skipped and execution continues to applyBlock. Pins that the
   * new check correctly inverts the boolean (no off-by-one in the `!`).
   */
  @SneakyThrows
  @Test
  public void testSwitchForkPassesValidSignatureBlockToApply() {
    Manager dbManager = spy(new Manager());
    Consensus consensus = mock(Consensus.class);
    setField(dbManager, "consensus", consensus);

    ChainBaseManager cbm = mock(ChainBaseManager.class);
    DynamicPropertiesStore dps = mock(DynamicPropertiesStore.class);
    AccountStore accountStore = mock(AccountStore.class);
    Sha256Hash sharedHash = Sha256Hash.ZERO_HASH;
    when(cbm.getDynamicPropertiesStore()).thenReturn(dps);
    when(cbm.getAccountStore()).thenReturn(accountStore);
    when(dps.getLatestBlockHeaderHash()).thenReturn(sharedHash);
    setField(dbManager, "chainBaseManager", cbm);

    RevokingDatabase revokingStore = mock(RevokingDatabase.class);
    ISession session = mock(ISession.class);
    when(revokingStore.buildSession()).thenReturn(session);
    setField(dbManager, "revokingStore", revokingStore);

    KhaosDatabase khaosDb = mock(KhaosDatabase.class);
    setField(dbManager, "khaosDb", khaosDb);

    BlockCapsule goodBlock = mock(BlockCapsule.class);
    BlockCapsule.BlockId goodBlockId = mock(BlockCapsule.BlockId.class);
    when(goodBlock.getBlockId()).thenReturn(goodBlockId);
    when(goodBlock.getNum()).thenReturn(100L);
    when(goodBlock.validateSignature(any(DynamicPropertiesStore.class),
        any(AccountStore.class))).thenReturn(true);
    // setSwitch returns self for chained call from applyBlock argument expression.
    when(goodBlock.setSwitch(true)).thenReturn(goodBlock);
    RuntimeException reachedApply = new RuntimeException("reached block application");
    when(consensus.validBlock(goodBlock)).thenThrow(reachedApply);

    LinkedList<KhaosDatabase.KhaosBlock> first = new LinkedList<>();
    first.add(new KhaosDatabase.KhaosBlock(goodBlock));
    LinkedList<KhaosDatabase.KhaosBlock> value = new LinkedList<>();
    when(khaosDb.getBranch(any(BlockCapsule.BlockId.class), any(Sha256Hash.class)))
        .thenReturn(new Pair<>(first, value));

    Method switchFork = Manager.class.getDeclaredMethod("switchFork", BlockCapsule.class);
    switchFork.setAccessible(true);
    InvocationTargetException exception = assertThrows(InvocationTargetException.class,
        () -> switchFork.invoke(dbManager, goodBlock));
    Assert.assertSame(reachedApply, exception.getCause());

    // Validation ran AND setSwitch was reached — proves the `if` did not short-circuit
    // on the false branch when validateSignature returned true.
    verify(goodBlock, atLeastOnce()).validateSignature(
        any(DynamicPropertiesStore.class), any(AccountStore.class));
    verify(goodBlock, atLeastOnce()).setSwitch(true);
    verify(consensus).validBlock(goodBlock);
  }

  private static void setField(Object target, String name, Object value) throws Exception {
    Field f = target.getClass().getSuperclass() != null
        ? findField(target.getClass(), name)
        : target.getClass().getDeclaredField(name);
    f.setAccessible(true);
    f.set(target, value);
  }

  private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
    Class<?> c = cls;
    while (c != null) {
      try {
        return c.getDeclaredField(name);
      } catch (NoSuchFieldException e) {
        c = c.getSuperclass();
      }
    }
    throw new NoSuchFieldException(name);
  }

}
