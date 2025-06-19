package org.tron.core.db;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.tron.common.cron.CronExpression;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.runtime.ProgramResult;
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

@Slf4j
public class ManagerMockTest {
  @After
  public void  clearMocks() {
    Mockito.framework().clearInlineMocks();
  }

  @Test
  public void processTransactionCostTimeMoreThan100() throws Exception {
    TransactionTrace traceMock = mock(TransactionTrace.class);
    BandwidthProcessor bandwidthProcessorMock = mock(BandwidthProcessor.class);
    try (MockedConstruction<TransactionTrace> mockedConstruction2
             = mockConstruction(TransactionTrace.class,(mock, context) -> {
               when(mock).thenReturn(traceMock); });
         MockedConstruction<BandwidthProcessor> mockedConstruction3
             = mockConstruction(BandwidthProcessor.class,(mock, context) -> {
               when(mock).thenReturn(bandwidthProcessorMock);
             });
         MockedStatic<TransactionUtil> mockedStatic = mockStatic(TransactionUtil.class)) {
      Manager dbManager = mock(Manager.class);
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

      ChainBaseManager chainBaseManagerMock = mock(ChainBaseManager.class);
      BalanceTraceStore balanceTraceStoreMock = mock(BalanceTraceStore.class);
      TransactionStore transactionStoreMock = mock(TransactionStore.class);
      TransactionInfoCapsule transactionInfoCapsuleMock = mock(TransactionInfoCapsule.class);
      Protocol.TransactionInfo transactionInfo = Protocol.TransactionInfo.newBuilder().build();

      Field field = dbManager.getClass().getDeclaredField("chainBaseManager");
      field.setAccessible(true);
      field.set(dbManager, chainBaseManagerMock);

      BlockCapsule blockCapMock = Mockito.mock(BlockCapsule.class);

      when(TransactionUtil
          .buildTransactionInfoInstance(trxCapMock, blockCapMock, traceMock))
          .thenReturn(transactionInfoCapsuleMock);

      // this make cost > 100 cond is true
      when(blockCapMock.isMerkleRootEmpty()).thenAnswer(new Answer<Boolean>() {
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
      when(trxCapMock.getInstance()).thenReturn(trxCap.getInstance());
      when(trxCapMock.validatePubSignature(
          Mockito.any(AccountStore.class),
          Mockito.any(DynamicPropertiesStore.class))).thenReturn(true);
      when(trxCapMock.validateSignature(
          Mockito.any(AccountStore.class),
          Mockito.any(DynamicPropertiesStore.class))).thenReturn(true);

      doNothing().when(dbManager).validateTapos(trxCapMock);
      doNothing().when(dbManager).validateCommon(trxCapMock);
      doNothing().when(dbManager).validateDup(trxCapMock);


      doNothing().when(transactionStoreMock).put(transactionId.getBytes(), trxCapMock);
      doNothing().when(bandwidthProcessorMock).consume(trxCapMock, traceMock);
      doNothing().when(dbManager).consumeBandwidth(trxCapMock, traceMock);
      doNothing().when(balanceTraceStoreMock).initCurrentTransactionBalanceTrace(trxCapMock);
      doNothing().when(balanceTraceStoreMock).updateCurrentTransactionStatus(anyString());
      doNothing().when(balanceTraceStoreMock).resetCurrentTransactionTrace();


      assertNotNull(
          when(dbManager.processTransaction(trxCapMock, blockCapMock)).thenCallRealMethod()
      );
    }
  }

  private void initMockEnv(Manager dbManager, long headNum, long headTime,
                           long exitHeight, long exitCount, String blockTime)
      throws Exception {
    ChainBaseManager chainBaseManagerMock = mock(ChainBaseManager.class);
    Args argsMock = mock(Args.class);

    when(Args.getInstance()).thenReturn(argsMock);

    when(chainBaseManagerMock.getHeadBlockNum()).thenReturn(headNum);
    when(chainBaseManagerMock.getHeadBlockTimeStamp()).thenReturn(headTime);

    when(argsMock.getShutdownBlockHeight()).thenReturn(exitHeight);
    when(argsMock.getShutdownBlockCount()).thenReturn(exitCount);
    when(argsMock.isP2pDisable()).thenReturn(false);
    when(argsMock.getShutdownBlockTime())
        .thenReturn(new CronExpression(blockTime));  //"0 0 12 * * ?"

    Field field = dbManager.getClass().getDeclaredField("chainBaseManager");
    field.setAccessible(true);
    field.set(dbManager, chainBaseManagerMock);
  }

  @Test
  public void testInitAutoStop() throws Exception {
    Manager dbManager = spy(new Manager());
    try (MockedStatic<CommonParameter> methodTestMockedStatic
             = mockStatic(CommonParameter.class)) {
      initMockEnv(dbManager, 100L, 12345L,
          10L, 0L, "0 0 12 * * ?");

      assertThrows(
          "shutDownBlockHeight 10 is less than headNum 100",
          Exception.class,
          () -> {
            Method privateMethod = Manager.class.getDeclaredMethod(
                "initAutoStop");
            privateMethod.setAccessible(true);
            privateMethod.invoke(dbManager);
          }
      );
    }

  }

  @Test
  public void testInitAutoStop1() throws Exception {
    Manager dbManager = spy(new Manager());
    try (MockedStatic<CommonParameter> methodTestMockedStatic
             = mockStatic(CommonParameter.class)) {
      initMockEnv(dbManager,10L, 12345L,
          100L, 0L, "0 0 12 * * ?");

      assertThrows(
          "shutDownBlockCount 0 is less than 1",
          Exception.class,
          () -> {
            Method privateMethod = Manager.class.getDeclaredMethod(
                "initAutoStop");
            privateMethod.setAccessible(true);
            privateMethod.invoke(dbManager);
          }
      );
    }
  }

  @Test
  public void testInitAutoStop2() throws Exception {
    Manager dbManager = spy(new Manager());
    try (MockedStatic<CommonParameter> methodTestMockedStatic
             = mockStatic(CommonParameter.class)) {
      initMockEnv(dbManager,10L, 99726143865000L,
          100L, 1L, "0 0 12 * * ?");

      assertThrows(
          "shutDownBlockTime 0 0 12 * * ? is illegal",
          Exception.class,
          () -> {
            Method privateMethod = Manager.class.getDeclaredMethod(
                "initAutoStop");
            privateMethod.setAccessible(true);
            privateMethod.invoke(dbManager);
          }
      );
    }

  }

  @Test
  public void testInitAutoStop3() throws Exception {
    Manager dbManager = spy(new Manager());
    try (MockedStatic<CommonParameter> methodTestMockedStatic
             = mockStatic(CommonParameter.class)) {
      initMockEnv(dbManager,10L, 12345L,
          100L, 1L, "0 0 12 * * ?");

      assertThrows(
          "shutDownBlockHeight 100 and shutDownBlockCount 1 set both",
          Exception.class,
          () -> {
            Method privateMethod = Manager.class.getDeclaredMethod(
                "initAutoStop");
            privateMethod.setAccessible(true);
            privateMethod.invoke(dbManager);
          }
      );
    }

  }

  @Test
  public void testInitAutoStop4() throws Exception {
    Manager dbManager = spy(new Manager());
    try (MockedStatic<CommonParameter> methodTestMockedStatic
             = mockStatic(CommonParameter.class)) {
      initMockEnv(dbManager, 10L, 12345L,
          100L, -1L, "0 0 12 * * ?");

      assertThrows(
          "shutDownBlockHeight 100 and shutDownBlockTime 0 0 12 * * ? set both",
          Exception.class,
          () -> {
            Method privateMethod = Manager.class.getDeclaredMethod(
                "initAutoStop");
            privateMethod.setAccessible(true);
            privateMethod.invoke(dbManager);
          }
      );
    }

  }

  @Test
  public void testInitAutoStop5() throws Exception {
    Manager dbManager = spy(new Manager());
    try (MockedStatic<CommonParameter> methodTestMockedStatic
             = mockStatic(CommonParameter.class)) {
      initMockEnv(dbManager,10L, 12345L,
          0L, 1L, "0 0 12 * * ?");

      assertThrows(
          "shutDownBlockCount 1 and shutDownBlockTime 0 0 12 * * ? set both",
          Exception.class,
          () -> {
            Method privateMethod = Manager.class.getDeclaredMethod(
                "initAutoStop");
            privateMethod.setAccessible(true);
            privateMethod.invoke(dbManager);
          }
      );
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
    Protocol.Transaction transaction = Protocol.Transaction.newBuilder().build();
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

}