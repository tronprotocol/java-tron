package org.tron.core.db;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.spy;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.nio.charset.StandardCharsets;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.quartz.CronExpression;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.runtime.ProgramResult;
import org.tron.common.runtime.RuntimeImpl;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.TransactionInfoCapsule;
import org.tron.core.capsule.utils.TransactionUtil;
import org.tron.core.config.args.Args;
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

@RunWith(PowerMockRunner.class)
@PrepareForTest({Manager.class,
    CommonParameter.class,
    Args.class,
    TransactionUtil.class})
@Slf4j
public class ManagerMockTest {
  @After
  public void  clearMocks() {
    Mockito.framework().clearInlineMocks();
  }

  @Test
  public void processTransactionCostTimeMoreThan100() throws Exception {
    Manager dbManager = spy(new Manager());
    BalanceContract.TransferContract transferContract =
        BalanceContract.TransferContract.newBuilder()
            .setAmount(10)
            .setOwnerAddress(ByteString.copyFromUtf8("aaa"))
            .setToAddress(ByteString.copyFromUtf8("bbb"))
            .build();
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 6666; i++) {
      sb.append("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
    }
    Protocol.Transaction transaction = Protocol.Transaction.newBuilder().setRawData(
        Protocol.Transaction.raw.newBuilder()
            .setData(ByteString.copyFrom(sb.toString().getBytes(StandardCharsets.UTF_8)))
            .addContract(
                Protocol.Transaction.Contract.newBuilder()
                    .setParameter(Any.pack(transferContract))
                    .setType(Protocol.Transaction.Contract.ContractType.TransferContract)))
        .build();
    TransactionCapsule trxCap = new TransactionCapsule(transaction);
    ProgramResult result = new ProgramResult();
    result.setResultCode(Protocol.Transaction.Result.contractResult.SUCCESS);

    Sha256Hash transactionId = trxCap.getTransactionId();
    TransactionCapsule trxCapMock = mock(TransactionCapsule.class);
    TransactionTrace traceMock = mock(TransactionTrace.class);
    RuntimeImpl runtimeMock = mock(RuntimeImpl.class);
    BandwidthProcessor bandwidthProcessorMock = mock(BandwidthProcessor.class);
    ChainBaseManager chainBaseManagerMock = mock(ChainBaseManager.class);
    BalanceTraceStore balanceTraceStoreMock = mock(BalanceTraceStore.class);
    TransactionStore transactionStoreMock = mock(TransactionStore.class);
    TransactionInfoCapsule transactionInfoCapsuleMock = mock(TransactionInfoCapsule.class);
    Protocol.TransactionInfo transactionInfo = Protocol.TransactionInfo.newBuilder().build();

    // mock static
    PowerMockito.mockStatic(TransactionUtil.class);

    Whitebox.setInternalState(dbManager, "chainBaseManager", chainBaseManagerMock);

    BlockCapsule blockCapMock = Mockito.mock(BlockCapsule.class);

    PowerMockito.when(TransactionUtil
            .buildTransactionInfoInstance(trxCapMock, blockCapMock, traceMock))
        .thenReturn(transactionInfoCapsuleMock);

    // this make cost > 100 cond is true
    PowerMockito.when(blockCapMock.isMerkleRootEmpty()).thenAnswer(new Answer<Boolean>() {
      @Override
      public Boolean answer(InvocationOnMock invocation) throws Throwable {
        Thread.sleep(100);
        return true;
      }
    });

    when(chainBaseManagerMock.getBalanceTraceStore()).thenReturn(balanceTraceStoreMock);
    when(chainBaseManagerMock.getAccountStore()).thenReturn(mock(AccountStore.class));
    when(chainBaseManagerMock.getDynamicPropertiesStore())
        .thenReturn(mock(DynamicPropertiesStore.class));
    when(chainBaseManagerMock.getTransactionStore()).thenReturn(transactionStoreMock);
    when(trxCapMock.getTransactionId()).thenReturn(transactionId);
    when(traceMock.getRuntimeResult()).thenReturn(result);
    when(transactionInfoCapsuleMock.getId()).thenReturn(transactionId.getBytes());
    when(transactionInfoCapsuleMock.getInstance()).thenReturn(transactionInfo);

    doNothing().when(dbManager).validateTapos(trxCapMock);
    doNothing().when(dbManager).validateCommon(trxCapMock);
    doNothing().when(dbManager).validateDup(trxCapMock);

    // mock construct
    PowerMockito.whenNew(RuntimeImpl.class).withAnyArguments().thenReturn(runtimeMock);
    PowerMockito.whenNew(TransactionTrace.class).withAnyArguments().thenReturn(traceMock);
    PowerMockito.whenNew(BandwidthProcessor.class).withAnyArguments()
        .thenReturn(bandwidthProcessorMock);

    doNothing().when(transactionStoreMock).put(transactionId.getBytes(), trxCapMock);
    doNothing().when(bandwidthProcessorMock).consume(trxCapMock, traceMock);
    doNothing().when(dbManager).consumeBandwidth(trxCapMock, traceMock);
    doNothing().when(balanceTraceStoreMock).initCurrentTransactionBalanceTrace(trxCapMock);
    doNothing().when(balanceTraceStoreMock).updateCurrentTransactionStatus(anyString());
    doNothing().when(balanceTraceStoreMock).resetCurrentTransactionTrace();

    when(trxCapMock.getInstance()).thenReturn(trxCap.getInstance());
    when(trxCapMock.validatePubSignature(
        Mockito.any(AccountStore.class),
        Mockito.any(DynamicPropertiesStore.class))).thenReturn(true);
    when(trxCapMock.validateSignature(
        Mockito.any(AccountStore.class),
        Mockito.any(DynamicPropertiesStore.class))).thenReturn(true);

    assertNotNull(dbManager.processTransaction(trxCapMock, blockCapMock));
  }

  private void initMockEnv(Manager dbManager, long headNum, long headTime,
                           long exitHeight, long exitCount, String blockTime)
      throws Exception {
    ChainBaseManager chainBaseManagerMock = PowerMockito.mock(ChainBaseManager.class);
    Args argsMock = PowerMockito.mock(Args.class);

    PowerMockito.mockStatic(CommonParameter.class);
    PowerMockito.mockStatic(Args.class);
    PowerMockito.when(Args.getInstance()).thenReturn(argsMock);

    when(chainBaseManagerMock.getHeadBlockNum()).thenReturn(headNum);
    when(chainBaseManagerMock.getHeadBlockTimeStamp()).thenReturn(headTime);

    when(argsMock.getShutdownBlockHeight()).thenReturn(exitHeight);
    when(argsMock.getShutdownBlockCount()).thenReturn(exitCount);
    when(argsMock.isP2pDisable()).thenReturn(false);
    when(argsMock.getShutdownBlockTime())
        .thenReturn(new CronExpression(blockTime));  //"0 0 12 * * ?"

    Whitebox.setInternalState(dbManager,
        "chainBaseManager", chainBaseManagerMock);
  }

  @Test
  public void testInitAutoStop() throws Exception {
    Manager dbManager = spy(new Manager());
    initMockEnv(dbManager, 100L, 12345L,
        10L, 0L, "0 0 12 * * ?");

    assertThrows(
        "shutDownBlockHeight 10 is less than headNum 100",
        Exception.class,
        () -> {
          Whitebox.invokeMethod(dbManager, "initAutoStop");
        }
    );
  }

  @Test
  public void testInitAutoStop1() throws Exception {
    Manager dbManager = spy(new Manager());
    initMockEnv(dbManager,10L, 12345L,
        100L, 0L, "0 0 12 * * ?");

    assertThrows(
        "shutDownBlockCount 0 is less than 1",
        Exception.class,
        () -> {
          Whitebox.invokeMethod(dbManager, "initAutoStop");
        }
    );
  }

  @Test
  public void testInitAutoStop2() throws Exception {
    Manager dbManager = spy(new Manager());
    initMockEnv(dbManager,10L, 99726143865000L,
        100L, 1L, "0 0 12 * * ?");

    assertThrows(
        "shutDownBlockTime 0 0 12 * * ? is illegal",
        Exception.class,
        () -> {
          Whitebox.invokeMethod(dbManager, "initAutoStop");
        }
    );
  }

  @Test
  public void testInitAutoStop3() throws Exception {
    Manager dbManager = spy(new Manager());
    initMockEnv(dbManager,10L, 12345L,
        100L, 1L, "0 0 12 * * ?");

    assertThrows(
        "shutDownBlockHeight 100 and shutDownBlockCount 1 set both",
        Exception.class,
        () -> {
          Whitebox.invokeMethod(dbManager, "initAutoStop");
        }
    );
  }

  @Test
  public void testInitAutoStop4() throws Exception {
    Manager dbManager = spy(new Manager());
    initMockEnv(dbManager, 10L, 12345L,
        100L, -1L, "0 0 12 * * ?");

    assertThrows(
        "shutDownBlockHeight 100 and shutDownBlockTime 0 0 12 * * ? set both",
        Exception.class,
        () -> {
          Whitebox.invokeMethod(dbManager, "initAutoStop");
        }
    );
  }

  @Test
  public void testInitAutoStop5() throws Exception {
    Manager dbManager = spy(new Manager());
    initMockEnv(dbManager,10L, 12345L,
        0L, 1L, "0 0 12 * * ?");

    assertThrows(
        "shutDownBlockCount 1 and shutDownBlockTime 0 0 12 * * ? set both",
        Exception.class,
        () -> {
          Whitebox.invokeMethod(dbManager, "initAutoStop");
        }
    );
  }

  @Test
  public void testProcessTransaction() throws Exception {
    Manager dbManager = spy(new Manager());
    TransactionCapsule transactionCapsuleMock = null;
    BlockCapsule blockCapsuleMock = PowerMockito.mock(BlockCapsule.class);
    Whitebox.invokeMethod(dbManager, "processTransaction",
        transactionCapsuleMock, blockCapsuleMock);
    assertTrue(true);
  }

  @Test
  public void testProcessTransaction1() {
    Manager dbManager = spy(new Manager());
    Protocol.Transaction transaction = Protocol.Transaction.newBuilder().setRawData(
        Protocol.Transaction.raw.newBuilder()
            .setData(ByteString.copyFrom("sb.toString()".getBytes(StandardCharsets.UTF_8))))
        .build();
    TransactionCapsule trxCap = new TransactionCapsule(transaction);

    BlockCapsule blockCapsuleMock = PowerMockito.mock(BlockCapsule.class);

    assertThrows(
        ContractSizeNotEqualToOneException.class,
        () -> {
          Whitebox.invokeMethod(dbManager, "processTransaction",
              trxCap, blockCapsuleMock);
        }
    );
  }

  @SneakyThrows
  @Test
  public void testRePush() {
    Manager dbManager = spy(new Manager());
    Protocol.Transaction transaction = Protocol.Transaction.newBuilder().build();
    TransactionCapsule trx = new TransactionCapsule(transaction);
    TransactionStore transactionStoreMock = mock(TransactionStore.class);

    ChainBaseManager chainBaseManagerMock = PowerMockito.mock(ChainBaseManager.class);
    Whitebox.setInternalState(dbManager, "chainBaseManager", chainBaseManagerMock);
    when(chainBaseManagerMock.getTransactionStore()).thenReturn(transactionStoreMock);
    when(transactionStoreMock.has(any())).thenReturn(true);

    dbManager.rePush(trx);
    assertTrue(true);
  }

  @SneakyThrows
  @Test
  public void testRePush1() {
    Manager dbManager = spy(new Manager());
    Protocol.Transaction transaction = Protocol.Transaction.newBuilder().build();
    TransactionCapsule trx = new TransactionCapsule(transaction);
    TransactionStore transactionStoreMock = mock(TransactionStore.class);

    ChainBaseManager chainBaseManagerMock = PowerMockito.mock(ChainBaseManager.class);
    Whitebox.setInternalState(dbManager, "chainBaseManager", chainBaseManagerMock);
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
    assertTrue(true);
  }

  @Test
  public void testPostSolidityFilter() throws Exception {
    Manager dbManager = spy(new Manager());
    Whitebox.invokeMethod(dbManager, "postSolidityFilter",
        100L, 10L);
    assertTrue(true);
  }

  @Test
  public void testReOrgLogsFilter() throws Exception {
    Manager dbManager = spy(new Manager());
    CommonParameter commonParameterMock = PowerMockito.mock(Args.class);
    PowerMockito.mockStatic(CommonParameter.class);
    ChainBaseManager chainBaseManagerMock = PowerMockito.mock(ChainBaseManager.class);

    PowerMockito.when(CommonParameter.getInstance()).thenReturn(commonParameterMock);
    when(commonParameterMock.isJsonRpcHttpFullNodeEnable()).thenReturn(true);
    when(chainBaseManagerMock.getDynamicPropertiesStore())
        .thenReturn(mock(DynamicPropertiesStore.class));
    Whitebox.setInternalState(dbManager, "chainBaseManager", chainBaseManagerMock);
    doThrow(new ItemNotFoundException()).when(chainBaseManagerMock).getBlockById(any());

    Whitebox.invokeMethod(dbManager, "reOrgLogsFilter");
    assertTrue(true);
  }

}
