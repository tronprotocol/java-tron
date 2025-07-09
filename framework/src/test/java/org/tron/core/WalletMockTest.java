package org.tron.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.LazyStringArrayList;
import com.google.protobuf.ProtocolStringList;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.tron.api.GrpcAPI;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ByteUtil;
import org.tron.common.utils.Sha256Hash;
import org.tron.common.utils.client.WalletClient;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.ContractCapsule;
import org.tron.core.capsule.ContractStateCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.db.TransactionStore;
import org.tron.core.exception.AccountResourceInsufficientException;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.DupTransactionException;
import org.tron.core.exception.HeaderNotFound;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.exception.TaposException;
import org.tron.core.exception.TooBigTransactionException;
import org.tron.core.exception.TronException;
import org.tron.core.exception.VMIllegalException;
import org.tron.core.exception.ValidateSignatureException;
import org.tron.core.exception.ZksnarkException;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.net.message.adv.TransactionMessage;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.store.AbiStore;
import org.tron.core.store.AccountStore;
import org.tron.core.store.CodeStore;
import org.tron.core.store.ContractStateStore;
import org.tron.core.store.ContractStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.TransactionHistoryStore;
import org.tron.core.store.TransactionRetStore;
import org.tron.core.zen.ShieldedTRC20ParametersBuilder;
import org.tron.core.zen.address.DiversifierT;
import org.tron.core.zen.address.ExpandedSpendingKey;
import org.tron.core.zen.address.KeyIo;
import org.tron.core.zen.address.PaymentAddress;
import org.tron.protos.Protocol;
import org.tron.protos.contract.BalanceContract;
import org.tron.protos.contract.ShieldContract;
import org.tron.protos.contract.SmartContractOuterClass;


public class WalletMockTest {

  @Before
  public void init() {
    CommonParameter.PARAMETER.setMinEffectiveConnection(0);
  }

  @After
  public void  clearMocks() {
    Mockito.clearAllCaches();
  }

  @Test
  public void testSetTransactionNullException() throws Exception {
    Wallet wallet = new Wallet();
    TransactionCapsule transactionCapsuleMock
        = mock(TransactionCapsule.class);

    Method privateMethod = Wallet.class.getDeclaredMethod(
        "setTransaction", TransactionCapsule.class);
    privateMethod.setAccessible(true);
    privateMethod.invoke(wallet, transactionCapsuleMock);
  }

  @Test
  public void testCreateTransactionCapsuleWithoutValidateWithTimeoutNullException()
      throws Exception {
    Wallet wallet = new Wallet();
    com.google.protobuf.Message message =
        mock(com.google.protobuf.Message.class);
    Protocol.Transaction.Contract.ContractType contractType =
        mock(Protocol.Transaction.Contract.ContractType.class);
    long timeout = 100L;

    try (MockedConstruction<TransactionCapsule> mocked = mockConstruction(TransactionCapsule.class,
        (mock, context) -> {
          when(mock.getInstance()).thenReturn(Protocol.Transaction.newBuilder().build());
        })) {
      Method privateMethod = Wallet.class.getDeclaredMethod(
            "createTransactionCapsuleWithoutValidateWithTimeout",
            com.google.protobuf.Message.class,
            Protocol.Transaction.Contract.ContractType.class,
            long.class);
      privateMethod.setAccessible(true);
      privateMethod.invoke(wallet, message, contractType, timeout);
    }
  }

  @Test
  public void testCreateTransactionCapsuleWithoutValidateWithTimeout()
      throws Exception {
    Wallet wallet = new Wallet();
    com.google.protobuf.Message message =
        mock(com.google.protobuf.Message.class);
    Protocol.Transaction.Contract.ContractType contractType =
        mock(Protocol.Transaction.Contract.ContractType.class);
    long timeout = 100L;
    BlockCapsule.BlockId blockId = new BlockCapsule.BlockId();

    try (MockedConstruction<TransactionCapsule> mocked = mockConstruction(TransactionCapsule.class,
        (mock, context) -> {
          when(mock.getInstance()).thenReturn(Protocol.Transaction.newBuilder().build());
        })) {
      ChainBaseManager chainBaseManagerMock = mock(ChainBaseManager.class);

      Field field = wallet.getClass().getDeclaredField("chainBaseManager");
      field.setAccessible(true);
      field.set(wallet, chainBaseManagerMock);

      when(chainBaseManagerMock.getHeadBlockId()).thenReturn(blockId);

      Method privateMethod = Wallet.class.getDeclaredMethod(
          "createTransactionCapsuleWithoutValidateWithTimeout",
          com.google.protobuf.Message.class,
          Protocol.Transaction.Contract.ContractType.class,
          long.class);
      privateMethod.setAccessible(true);
      privateMethod.invoke(wallet, message, contractType, timeout);
    }
  }


  @Test
  public void testBroadcastTransactionBlockUnsolidified() throws Exception {
    Wallet wallet = new Wallet();
    Protocol.Transaction transaction = Protocol.Transaction.newBuilder().build();

    TronNetDelegate tronNetDelegateMock = mock(TronNetDelegate.class);
    when(tronNetDelegateMock.isBlockUnsolidified()).thenReturn(true);

    Field field = wallet.getClass().getDeclaredField("tronNetDelegate");
    field.setAccessible(true);
    field.set(wallet, tronNetDelegateMock);

    GrpcAPI.Return ret = wallet.broadcastTransaction(transaction);

    assertEquals(GrpcAPI.Return.response_code.BLOCK_UNSOLIDIFIED, ret.getCode());
  }

  @Test
  public void testBroadcastTransactionNoConnection() throws Exception {
    Wallet wallet = new Wallet();
    Protocol.Transaction transaction = Protocol.Transaction.newBuilder().build();
    List<PeerConnection> peerConnections = new ArrayList<>();

    TronNetDelegate tronNetDelegateMock = mock(TronNetDelegate.class);
    when(tronNetDelegateMock.isBlockUnsolidified()).thenReturn(false);

    Field field = wallet.getClass().getDeclaredField("tronNetDelegate");
    field.setAccessible(true);
    field.set(wallet, tronNetDelegateMock);

    Field field2 = wallet.getClass().getDeclaredField("minEffectiveConnection");
    field2.setAccessible(true);
    field2.set(wallet, 10);

    when(tronNetDelegateMock.getActivePeer()).thenReturn(peerConnections);

    GrpcAPI.Return ret = wallet.broadcastTransaction(transaction);

    assertEquals(GrpcAPI.Return.response_code.NO_CONNECTION, ret.getCode());
  }

  @Test
  public void testBroadcastTransactionConnectionNotEnough() throws Exception {
    Wallet wallet = new Wallet();
    Protocol.Transaction transaction = Protocol.Transaction.newBuilder().build();
    List<PeerConnection> peerConnections = new ArrayList<>();
    PeerConnection p1 = new PeerConnection();
    PeerConnection p2 = new PeerConnection();
    peerConnections.add(p1);
    peerConnections.add(p2);

    TronNetDelegate tronNetDelegateMock = mock(TronNetDelegate.class);
    when(tronNetDelegateMock.isBlockUnsolidified()).thenReturn(false);

    Field field = wallet.getClass().getDeclaredField("tronNetDelegate");
    field.setAccessible(true);
    field.set(wallet, tronNetDelegateMock);

    Field field2 = wallet.getClass().getDeclaredField("minEffectiveConnection");
    field2.setAccessible(true);
    field2.set(wallet, 10);
    when(tronNetDelegateMock.getActivePeer()).thenReturn(peerConnections);

    GrpcAPI.Return ret = wallet.broadcastTransaction(transaction);

    assertEquals(GrpcAPI.Return.response_code.NOT_ENOUGH_EFFECTIVE_CONNECTION,
        ret.getCode());
  }

  @Test
  public void testBroadcastTransactionTooManyPending() throws Exception {
    Wallet wallet = new Wallet();
    Protocol.Transaction transaction = Protocol.Transaction.newBuilder().build();

    TronNetDelegate tronNetDelegateMock = mock(TronNetDelegate.class);
    Manager managerMock = mock(Manager.class);
    when(tronNetDelegateMock.isBlockUnsolidified()).thenReturn(false);
    when(managerMock.isTooManyPending()).thenReturn(true);

    Field field = wallet.getClass().getDeclaredField("tronNetDelegate");
    field.setAccessible(true);
    field.set(wallet, tronNetDelegateMock);

    Field field2 = wallet.getClass().getDeclaredField("dbManager");
    field2.setAccessible(true);
    field2.set(wallet, managerMock);

    GrpcAPI.Return ret = wallet.broadcastTransaction(transaction);

    assertEquals(GrpcAPI.Return.response_code.SERVER_BUSY, ret.getCode());
  }

  @Test
  public void testBroadcastTransactionAlreadyExists() throws Exception {
    Wallet wallet = new Wallet();
    Protocol.Transaction transaction = Protocol.Transaction.newBuilder().build();
    TransactionCapsule trx = new TransactionCapsule(transaction);
    trx.setTime(System.currentTimeMillis());
    Sha256Hash txID = trx.getTransactionId();

    Cache<Sha256Hash, Boolean> transactionIdCache = CacheBuilder
        .newBuilder().maximumSize(10)
        .expireAfterWrite(1, TimeUnit.HOURS).recordStats().build();
    transactionIdCache.put(txID, true);

    TronNetDelegate tronNetDelegateMock = mock(TronNetDelegate.class);
    Manager managerMock = mock(Manager.class);
    when(tronNetDelegateMock.isBlockUnsolidified()).thenReturn(false);
    when(managerMock.isTooManyPending()).thenReturn(false);
    when(managerMock.getTransactionIdCache()).thenReturn(transactionIdCache);

    Field field = wallet.getClass().getDeclaredField("tronNetDelegate");
    field.setAccessible(true);
    field.set(wallet, tronNetDelegateMock);

    Field field2 = wallet.getClass().getDeclaredField("dbManager");
    field2.setAccessible(true);
    field2.set(wallet, managerMock);

    Field field3 = wallet.getClass().getDeclaredField("trxCacheEnable");
    field3.setAccessible(true);
    field3.set(wallet, true);

    GrpcAPI.Return ret = wallet.broadcastTransaction(transaction);

    assertEquals(GrpcAPI.Return.response_code.DUP_TRANSACTION_ERROR,
        ret.getCode());
  }


  @Test
  public void testBroadcastTransactionNoContract() throws Exception {
    Wallet wallet = new Wallet();
    Protocol.Transaction transaction = Protocol.Transaction.newBuilder().build();

    TronNetDelegate tronNetDelegateMock = mock(TronNetDelegate.class);
    Manager managerMock = mock(Manager.class);
    ChainBaseManager chainBaseManagerMock = mock(ChainBaseManager.class);
    DynamicPropertiesStore dynamicPropertiesStoreMock
        = mock(DynamicPropertiesStore.class);
    when(tronNetDelegateMock.isBlockUnsolidified()).thenReturn(false);
    when(managerMock.isTooManyPending()).thenReturn(false);
    when(chainBaseManagerMock.getDynamicPropertiesStore())
        .thenReturn(dynamicPropertiesStoreMock);
    when(dynamicPropertiesStoreMock.supportVM()).thenReturn(false);

    Field field = wallet.getClass().getDeclaredField("tronNetDelegate");
    field.setAccessible(true);
    field.set(wallet, tronNetDelegateMock);

    Field field2 = wallet.getClass().getDeclaredField("dbManager");
    field2.setAccessible(true);
    field2.set(wallet, managerMock);

    Field field4 = wallet.getClass().getDeclaredField("chainBaseManager");
    field4.setAccessible(true);
    field4.set(wallet, chainBaseManagerMock);

    Field field3 = wallet.getClass().getDeclaredField("trxCacheEnable");
    field3.setAccessible(true);
    field3.set(wallet, false);

    GrpcAPI.Return ret = wallet.broadcastTransaction(transaction);

    assertEquals(GrpcAPI.Return.response_code.CONTRACT_VALIDATE_ERROR,
        ret.getCode());
  }

  @Test
  public void testBroadcastTransactionOtherException() throws Exception {
    Wallet wallet = new Wallet();
    Protocol.Transaction transaction = getExampleTrans();

    TronNetDelegate tronNetDelegateMock = mock(TronNetDelegate.class);
    Manager managerMock = mock(Manager.class);
    ChainBaseManager chainBaseManagerMock = mock(ChainBaseManager.class);
    DynamicPropertiesStore dynamicPropertiesStoreMock
        = mock(DynamicPropertiesStore.class);
    when(tronNetDelegateMock.isBlockUnsolidified()).thenReturn(false);
    when(managerMock.isTooManyPending()).thenReturn(false);
    when(chainBaseManagerMock.getDynamicPropertiesStore())
        .thenReturn(dynamicPropertiesStoreMock);
    when(dynamicPropertiesStoreMock.supportVM()).thenReturn(false);

    Field field = wallet.getClass().getDeclaredField("tronNetDelegate");
    field.setAccessible(true);
    field.set(wallet, tronNetDelegateMock);

    Field field2 = wallet.getClass().getDeclaredField("dbManager");
    field2.setAccessible(true);
    field2.set(wallet, managerMock);

    Field field4 = wallet.getClass().getDeclaredField("chainBaseManager");
    field4.setAccessible(true);
    field4.set(wallet, chainBaseManagerMock);

    Field field3 = wallet.getClass().getDeclaredField("trxCacheEnable");
    field3.setAccessible(true);
    field3.set(wallet, false);

    GrpcAPI.Return ret = wallet.broadcastTransaction(transaction);

    assertEquals(GrpcAPI.Return.response_code.OTHER_ERROR, ret.getCode());
  }

  private Protocol.Transaction getExampleTrans() {
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
    return Protocol.Transaction.newBuilder().setRawData(
        Protocol.Transaction.raw.newBuilder()
            .setData(ByteString.copyFrom(sb.toString().getBytes(StandardCharsets.UTF_8)))
            .addContract(
                Protocol.Transaction.Contract.newBuilder()
                    .setParameter(Any.pack(transferContract))
                    .setType(Protocol.Transaction.Contract.ContractType.TransferContract)))
        .build();
  }

  private void mockEnv(Wallet wallet, TronException tronException) throws Exception {
    TronNetDelegate tronNetDelegateMock = mock(TronNetDelegate.class);
    Manager managerMock = mock(Manager.class);
    ChainBaseManager chainBaseManagerMock
        = mock(ChainBaseManager.class);
    DynamicPropertiesStore dynamicPropertiesStoreMock
        = mock(DynamicPropertiesStore.class);

    when(tronNetDelegateMock.isBlockUnsolidified()).thenReturn(false);
    when(managerMock.isTooManyPending()).thenReturn(false);
    when(chainBaseManagerMock.getDynamicPropertiesStore())
        .thenReturn(dynamicPropertiesStoreMock);
    when(dynamicPropertiesStoreMock.supportVM()).thenReturn(false);

    doThrow(tronException).when(managerMock).pushTransaction(any());

    Field field = wallet.getClass().getDeclaredField("tronNetDelegate");
    field.setAccessible(true);
    field.set(wallet, tronNetDelegateMock);

    Field field2 = wallet.getClass().getDeclaredField("dbManager");
    field2.setAccessible(true);
    field2.set(wallet, managerMock);

    Field field4 = wallet.getClass().getDeclaredField("chainBaseManager");
    field4.setAccessible(true);
    field4.set(wallet, chainBaseManagerMock);

    Field field3 = wallet.getClass().getDeclaredField("trxCacheEnable");
    field3.setAccessible(true);
    field3.set(wallet, false);
  }

  @Test
  public void testBroadcastTransactionValidateSignatureException() throws Exception {
    try (MockedConstruction<TransactionMessage> mocked = mockConstruction(TransactionMessage.class,
        (mock, context) -> {

        })) {
      Wallet wallet = new Wallet();
      Protocol.Transaction transaction = getExampleTrans();
      mockEnv(wallet, new ValidateSignatureException());
      GrpcAPI.Return ret = wallet.broadcastTransaction(transaction);
      assertEquals(GrpcAPI.Return.response_code.SIGERROR, ret.getCode());
    }

  }

  @Test
  public void testBroadcastTransactionValidateContractExeException() throws Exception {
    try (MockedConstruction<TransactionMessage> mocked = mockConstruction(TransactionMessage.class,
        (mock, context) -> {

        })) {
      Wallet wallet = new Wallet();
      Protocol.Transaction transaction = getExampleTrans();
      mockEnv(wallet, new ContractExeException());
      GrpcAPI.Return ret = wallet.broadcastTransaction(transaction);
      assertEquals(GrpcAPI.Return.response_code.CONTRACT_EXE_ERROR, ret.getCode());
    }

  }

  @Test
  public void testBroadcastTransactionValidateAccountResourceInsufficientException()
      throws Exception {
    try (MockedConstruction<TransactionMessage> mocked = mockConstruction(TransactionMessage.class,
        (mock, context) -> {

        })) {
      Wallet wallet = new Wallet();
      Protocol.Transaction transaction = getExampleTrans();
      mockEnv(wallet, new AccountResourceInsufficientException(""));
      GrpcAPI.Return ret = wallet.broadcastTransaction(transaction);
      assertEquals(GrpcAPI.Return.response_code.BANDWITH_ERROR, ret.getCode());
    }

  }

  @Test
  public void testBroadcastTransactionValidateDupTransactionException()
      throws Exception {
    try (MockedConstruction<TransactionMessage> mocked = mockConstruction(TransactionMessage.class,
        (mock, context) -> {

        })) {
      Wallet wallet = new Wallet();
      Protocol.Transaction transaction = getExampleTrans();
      mockEnv(wallet, new DupTransactionException(""));
      GrpcAPI.Return ret = wallet.broadcastTransaction(transaction);
      assertEquals(GrpcAPI.Return.response_code.DUP_TRANSACTION_ERROR, ret.getCode());
    }

  }

  @Test
  public void testBroadcastTransactionValidateTaposException() throws Exception {
    try (MockedConstruction<TransactionMessage> mocked = mockConstruction(TransactionMessage.class,
        (mock, context) -> {

        })) {
      Wallet wallet = new Wallet();
      Protocol.Transaction transaction = getExampleTrans();
      mockEnv(wallet, new TaposException(""));
      GrpcAPI.Return ret = wallet.broadcastTransaction(transaction);
      assertEquals(GrpcAPI.Return.response_code.TAPOS_ERROR, ret.getCode());
    }

  }

  @Test
  public void testBroadcastTransactionValidateTooBigTransactionException()
      throws Exception {
    try (MockedConstruction<TransactionMessage> mocked = mockConstruction(TransactionMessage.class,
        (mock, context) -> {

        })) {
      Wallet wallet = new Wallet();
      Protocol.Transaction transaction = getExampleTrans();
      mockEnv(wallet, new TooBigTransactionException(""));

      GrpcAPI.Return ret = wallet.broadcastTransaction(transaction);
      assertEquals(GrpcAPI.Return.response_code.TOO_BIG_TRANSACTION_ERROR, ret.getCode());
    }

  }

  @Test
  public void testGetBlockByNum() throws Exception {
    Wallet wallet = new Wallet();
    ChainBaseManager chainBaseManagerMock = mock(ChainBaseManager.class);

    Field field = wallet.getClass().getDeclaredField("chainBaseManager");
    field.setAccessible(true);
    field.set(wallet, chainBaseManagerMock);

    doThrow(new ItemNotFoundException()).when(chainBaseManagerMock).getBlockByNum(anyLong());

    Protocol.Block block = wallet.getBlockByNum(0L);
    assertNull(block);
  }

  @Test
  public void testGetBlockCapsuleByNum() throws Exception {
    Wallet wallet = new Wallet();
    ChainBaseManager chainBaseManagerMock = mock(ChainBaseManager.class);
    Field field = wallet.getClass().getDeclaredField("chainBaseManager");
    field.setAccessible(true);
    field.set(wallet, chainBaseManagerMock);
    doThrow(new ItemNotFoundException()).when(chainBaseManagerMock).getBlockByNum(anyLong());

    BlockCapsule blockCapsule = wallet.getBlockCapsuleByNum(0L);
    assertNull(blockCapsule);
  }

  @Test
  public void testGetTransactionCountByBlockNum() throws Exception {
    Wallet wallet = new Wallet();
    ChainBaseManager chainBaseManagerMock = mock(ChainBaseManager.class);
    Field field = wallet.getClass().getDeclaredField("chainBaseManager");
    field.setAccessible(true);
    field.set(wallet, chainBaseManagerMock);
    doThrow(new ItemNotFoundException()).when(chainBaseManagerMock).getBlockByNum(anyLong());

    long count = wallet.getTransactionCountByBlockNum(0L);
    assertEquals(count, 0L);
  }

  @Test
  public void testGetTransactionById() throws Exception {
    Wallet wallet = new Wallet();
    ByteString transactionId = null;
    Protocol.Transaction transaction = wallet.getTransactionById(transactionId);
    assertNull(transaction);
  }

  @Test
  public void testGetTransactionById2() throws Exception {
    Wallet wallet = new Wallet();
    ByteString transactionId = ByteString.empty();
    ChainBaseManager chainBaseManagerMock = mock(ChainBaseManager.class);
    TransactionStore transactionStoreMock = mock(TransactionStore.class);

    when(chainBaseManagerMock.getTransactionStore()).thenReturn(transactionStoreMock);
    Field field = wallet.getClass().getDeclaredField("chainBaseManager");
    field.setAccessible(true);
    field.set(wallet, chainBaseManagerMock);
    doThrow(new BadItemException()).when(transactionStoreMock).get(any());

    Protocol.Transaction transaction = wallet.getTransactionById(transactionId);
    assertNull(transaction);
  }

  @Test
  public void testGetTransactionById3() throws Exception {
    Wallet wallet = new Wallet();
    ByteString transactionId = ByteString.empty();
    ChainBaseManager chainBaseManagerMock = mock(ChainBaseManager.class);
    TransactionStore transactionStoreMock = mock(TransactionStore.class);
    TransactionCapsule transactionCapsuleMock = mock(TransactionCapsule.class);
    Protocol.Transaction transaction = Protocol.Transaction.newBuilder().build();

    when(chainBaseManagerMock.getTransactionStore()).thenReturn(transactionStoreMock);
    Field field = wallet.getClass().getDeclaredField("chainBaseManager");
    field.setAccessible(true);
    field.set(wallet, chainBaseManagerMock);
    when(transactionStoreMock.get(any())).thenReturn(transactionCapsuleMock);
    when(transactionCapsuleMock.getInstance()).thenReturn(transaction);

    Protocol.Transaction transactionRet = wallet.getTransactionById(transactionId);
    assertEquals(transaction, transactionRet);
  }

  @Test
  public void testGetTransactionCapsuleById() throws Exception {
    Wallet wallet = new Wallet();
    ByteString transactionId = null;
    TransactionCapsule transactionCapsule = wallet.getTransactionCapsuleById(transactionId);
    assertNull(transactionCapsule);
  }

  @Test
  public void testGetTransactionCapsuleById1() throws Exception {
    Wallet wallet = new Wallet();
    ByteString transactionId = ByteString.empty();
    ChainBaseManager chainBaseManagerMock = mock(ChainBaseManager.class);
    TransactionStore transactionStoreMock = mock(TransactionStore.class);

    when(chainBaseManagerMock.getTransactionStore()).thenReturn(transactionStoreMock);
    Field field = wallet.getClass().getDeclaredField("chainBaseManager");
    field.setAccessible(true);
    field.set(wallet, chainBaseManagerMock);
    doThrow(new BadItemException()).when(transactionStoreMock).get(any());

    TransactionCapsule transactionCapsule = wallet.getTransactionCapsuleById(transactionId);
    assertNull(transactionCapsule);
  }

  @Test
  public void testGetTransactionInfoById() throws Exception {
    Wallet wallet = new Wallet();
    ByteString transactionId = null;
    Protocol.TransactionInfo transactionInfo = wallet.getTransactionInfoById(transactionId);
    assertNull(transactionInfo);
  }

  @Test
  public void testGetTransactionInfoById1() throws Exception {
    Wallet wallet = new Wallet();
    ByteString transactionId = ByteString.empty();
    ChainBaseManager chainBaseManagerMock = mock(ChainBaseManager.class);
    TransactionRetStore transactionRetStoreMock = mock(TransactionRetStore.class);

    when(chainBaseManagerMock.getTransactionRetStore()).thenReturn(transactionRetStoreMock);
    Field field = wallet.getClass().getDeclaredField("chainBaseManager");
    field.setAccessible(true);
    field.set(wallet, chainBaseManagerMock);
    doThrow(new BadItemException()).when(transactionRetStoreMock).getTransactionInfo(any());

    Protocol.TransactionInfo transactionInfo = wallet.getTransactionInfoById(transactionId);
    assertNull(transactionInfo);
  }

  @Test
  public void testGetTransactionInfoById2() throws Exception {
    Wallet wallet = new Wallet();
    ByteString transactionId = ByteString.empty();
    ChainBaseManager chainBaseManagerMock = mock(ChainBaseManager.class);
    TransactionRetStore transactionRetStoreMock = mock(TransactionRetStore.class);
    TransactionHistoryStore transactionHistoryStoreMock =
        mock(TransactionHistoryStore.class);

    when(chainBaseManagerMock.getTransactionRetStore())
        .thenReturn(transactionRetStoreMock);
    when(chainBaseManagerMock.getTransactionHistoryStore())
        .thenReturn(transactionHistoryStoreMock);
    Field field = wallet.getClass().getDeclaredField("chainBaseManager");
    field.setAccessible(true);
    field.set(wallet, chainBaseManagerMock);
    when(transactionRetStoreMock.getTransactionInfo(any())).thenReturn(null);
    doThrow(new BadItemException()).when(transactionHistoryStoreMock).get(any());

    Protocol.TransactionInfo transactionInfo = wallet.getTransactionInfoById(transactionId);
    assertNull(transactionInfo);
  }

  @Test
  public void testGetProposalById() throws Exception {
    Wallet wallet = new Wallet();
    ByteString proposalId = null;
    Protocol.Proposal proposal = wallet.getProposalById(proposalId);
    assertNull(proposal);
  }

  @Test
  public void testGetMemoFeePrices() throws Exception {
    Wallet wallet = new Wallet();
    ChainBaseManager chainBaseManagerMock = mock(ChainBaseManager.class);
    DynamicPropertiesStore dynamicPropertiesStoreMock =
        mock(DynamicPropertiesStore.class);

    when(chainBaseManagerMock.getDynamicPropertiesStore()).thenReturn(dynamicPropertiesStoreMock);
    doThrow(new IllegalArgumentException("not found MEMO_FEE_HISTORY"))
        .when(dynamicPropertiesStoreMock).getMemoFeeHistory();

    Field field = wallet.getClass().getDeclaredField("chainBaseManager");
    field.setAccessible(true);
    field.set(wallet, chainBaseManagerMock);

    GrpcAPI.PricesResponseMessage responseMessage = wallet.getMemoFeePrices();
    assertNull(responseMessage);
  }

  @Test
  public void testGetEnergyFeeByTime() throws Exception {
    Wallet wallet = new Wallet();
    ChainBaseManager chainBaseManagerMock = mock(ChainBaseManager.class);
    DynamicPropertiesStore dynamicPropertiesStoreMock =
        mock(DynamicPropertiesStore.class);
    long now = System.currentTimeMillis();

    when(chainBaseManagerMock.getDynamicPropertiesStore()).thenReturn(dynamicPropertiesStoreMock);
    doThrow(new IllegalArgumentException("not found ENERGY_PRICE_HISTORY"))
        .when(dynamicPropertiesStoreMock).getEnergyPriceHistory();
    when(dynamicPropertiesStoreMock.getEnergyFee()).thenReturn(10L);

    Field field = wallet.getClass().getDeclaredField("chainBaseManager");
    field.setAccessible(true);
    field.set(wallet, chainBaseManagerMock);

    long energyFee = wallet.getEnergyFee(now);
    assertEquals(energyFee, 10L);
  }

  @Test
  public void testGetEnergyPrices() throws Exception {
    Wallet wallet = new Wallet();
    ChainBaseManager chainBaseManagerMock = mock(ChainBaseManager.class);
    DynamicPropertiesStore dynamicPropertiesStoreMock =
        mock(DynamicPropertiesStore.class);

    when(chainBaseManagerMock.getDynamicPropertiesStore()).thenReturn(dynamicPropertiesStoreMock);
    doThrow(new IllegalArgumentException("not found ENERGY_PRICE_HISTORY"))
        .when(dynamicPropertiesStoreMock).getEnergyPriceHistory();

    Field field = wallet.getClass().getDeclaredField("chainBaseManager");
    field.setAccessible(true);
    field.set(wallet, chainBaseManagerMock);

    GrpcAPI.PricesResponseMessage pricesResponseMessage = wallet.getEnergyPrices();
    assertNull(pricesResponseMessage);
  }

  @Test
  public void testGetBandwidthPrices() throws Exception {
    Wallet wallet = new Wallet();
    ChainBaseManager chainBaseManagerMock = mock(ChainBaseManager.class);
    DynamicPropertiesStore dynamicPropertiesStoreMock =
        mock(DynamicPropertiesStore.class);

    when(chainBaseManagerMock.getDynamicPropertiesStore()).thenReturn(dynamicPropertiesStoreMock);
    doThrow(new IllegalArgumentException("not found BANDWIDTH_PRICE_HISTORY"))
        .when(dynamicPropertiesStoreMock).getBandwidthPriceHistory();

    Field field = wallet.getClass().getDeclaredField("chainBaseManager");
    field.setAccessible(true);
    field.set(wallet, chainBaseManagerMock);

    GrpcAPI.PricesResponseMessage pricesResponseMessage = wallet.getBandwidthPrices();
    assertNull(pricesResponseMessage);
  }

  @Test
  public void testCheckBlockIdentifier() {
    Wallet wallet = new Wallet();
    BalanceContract.BlockBalanceTrace.BlockIdentifier blockIdentifier =
        BalanceContract.BlockBalanceTrace.BlockIdentifier.newBuilder()
            .build();
    blockIdentifier = blockIdentifier.getDefaultInstanceForType();

    BalanceContract.BlockBalanceTrace.BlockIdentifier blockIdentifier1 =
        blockIdentifier;

    assertThrows(IllegalArgumentException.class, () -> {
      wallet.checkBlockIdentifier(blockIdentifier1);
    });

    BalanceContract.BlockBalanceTrace.BlockIdentifier blockIdentifier2 =
        BalanceContract.BlockBalanceTrace.BlockIdentifier.newBuilder()
            .setNumber(-1L)
            .build();

    assertThrows(IllegalArgumentException.class, () -> {
      wallet.checkBlockIdentifier(blockIdentifier2);
    });

    BalanceContract.BlockBalanceTrace.BlockIdentifier blockIdentifier3 =
        BalanceContract.BlockBalanceTrace.BlockIdentifier.newBuilder()
            .setHash(ByteString.copyFrom("".getBytes(StandardCharsets.UTF_8)))
            .build();

    assertThrows(IllegalArgumentException.class, () -> {
      wallet.checkBlockIdentifier(blockIdentifier3);
    });
  }

  @Test
  public void testCheckAccountIdentifier() {
    Wallet wallet = new Wallet();
    BalanceContract.AccountIdentifier accountIdentifier =
        BalanceContract.AccountIdentifier.newBuilder().build();
    accountIdentifier = accountIdentifier.getDefaultInstanceForType();

    BalanceContract.AccountIdentifier accountIdentifier2 = accountIdentifier;

    assertThrows(IllegalArgumentException.class, () -> {
      wallet.checkAccountIdentifier(accountIdentifier2);
    });

    BalanceContract.AccountIdentifier accountIdentifier1
        = BalanceContract.AccountIdentifier.newBuilder().build();

    assertThrows(IllegalArgumentException.class, () -> {
      wallet.checkAccountIdentifier(accountIdentifier1);
    });
  }

  @Test
  public void testGetTriggerInputForShieldedTRC20Contract()  {
    Wallet wallet = new Wallet();
    GrpcAPI.ShieldedTRC20TriggerContractParameters.Builder triggerParam =
        GrpcAPI.ShieldedTRC20TriggerContractParameters
            .newBuilder();
    GrpcAPI.ShieldedTRC20Parameters shieldedTRC20Parameters =
        GrpcAPI.ShieldedTRC20Parameters.newBuilder().build();
    GrpcAPI.BytesMessage bytesMessage =
        GrpcAPI.BytesMessage.newBuilder().build();

    triggerParam.setShieldedTRC20Parameters(shieldedTRC20Parameters);
    triggerParam.addSpendAuthoritySignature(bytesMessage);

    CommonParameter commonParameterMock = mock(Args.class);
    try (MockedStatic<CommonParameter> mockedStatic = mockStatic(CommonParameter.class)) {
      when(CommonParameter.getInstance()).thenReturn(commonParameterMock);
      when(commonParameterMock.isAllowShieldedTransactionApi()).thenReturn(true);

      assertThrows(ZksnarkException.class, () -> {
        wallet.getTriggerInputForShieldedTRC20Contract(triggerParam.build());
      });
    }
  }

  @Test
  public void testGetTriggerInputForShieldedTRC20Contract1()
      throws ZksnarkException, ContractValidateException {
    Wallet wallet = new Wallet();
    ShieldContract.SpendDescription spendDescription =
        ShieldContract.SpendDescription.newBuilder().build();
    GrpcAPI.ShieldedTRC20TriggerContractParameters.Builder triggerParam =
        GrpcAPI.ShieldedTRC20TriggerContractParameters
            .newBuilder();
    GrpcAPI.ShieldedTRC20Parameters shieldedTRC20Parameters =
        GrpcAPI.ShieldedTRC20Parameters.newBuilder()
            .addSpendDescription(spendDescription)
            .setParameterType("transfer")
            .build();
    GrpcAPI.BytesMessage bytesMessage =
        GrpcAPI.BytesMessage.newBuilder().build();

    triggerParam.setShieldedTRC20Parameters(shieldedTRC20Parameters);
    triggerParam.addSpendAuthoritySignature(bytesMessage);

    CommonParameter commonParameterMock = mock(Args.class);
    try (MockedStatic<CommonParameter> mockedStatic = mockStatic(CommonParameter.class)) {
      when(CommonParameter.getInstance()).thenReturn(commonParameterMock);
      when(commonParameterMock.isAllowShieldedTransactionApi()).thenReturn(true);

      GrpcAPI.BytesMessage reponse =
          wallet.getTriggerInputForShieldedTRC20Contract(triggerParam.build());
      assertNotNull(reponse);
    }

  }

  @Test
  public void testGetShieldedContractScalingFactorException() throws Exception {
    Wallet walletMock = mock(Wallet.class);
    byte[] contractAddress = "".getBytes(StandardCharsets.UTF_8);
    Protocol.Transaction transaction = Protocol.Transaction.newBuilder().build();
    when(walletMock.createTransactionCapsule(any(), any()))
        .thenReturn(new TransactionCapsule(transaction));

    try {
      when(walletMock.getShieldedContractScalingFactor(contractAddress)).thenCallRealMethod();
    } catch (Exception e) {
      assertNotNull(e);
    }
  }

  @Test
  public void testGetShieldedContractScalingFactorRuntimeException()
      throws VMIllegalException, HeaderNotFound, ContractValidateException, ContractExeException {
    Wallet walletMock = mock(Wallet.class);
    byte[] contractAddress = "".getBytes(StandardCharsets.UTF_8);
    Protocol.Transaction transaction = Protocol.Transaction.newBuilder().build();
    when(walletMock.triggerConstantContract(any(),any(),any(),any())).thenReturn(transaction);
    when(walletMock.getShieldedContractScalingFactor(any())).thenCallRealMethod();

    assertThrows(ContractExeException.class, () -> {
      walletMock.getShieldedContractScalingFactor(contractAddress);
    });
  }

  @Test
  public void testGetShieldedContractScalingFactorSuccess()
      throws Exception {
    Wallet walletMock = mock(Wallet.class);
    byte[] contractAddress = "".getBytes(StandardCharsets.UTF_8);
    Protocol.Transaction transaction = Protocol.Transaction.newBuilder().build();
    when(walletMock.triggerConstantContract(any(),any(),any(),any()))
        .thenReturn(transaction);
    when(walletMock.createTransactionCapsule(any(), any()))
        .thenReturn(new TransactionCapsule(transaction));
    when(walletMock.getShieldedContractScalingFactor(any())).thenCallRealMethod();
    try {
      byte[] listBytes = walletMock.getShieldedContractScalingFactor(contractAddress);
      assertNotNull(listBytes);
    } catch (Exception e) {
      assertNull(e);
    }
  }

  @Test
  public void testGetShieldedContractScalingFactorContractExeException()
      throws Exception {
    Wallet walletMock = mock(Wallet.class);
    byte[] contractAddress = "".getBytes(StandardCharsets.UTF_8);
    Protocol.Transaction transaction = Protocol.Transaction.newBuilder().build();
    doThrow(new ContractExeException(""))
        .when(walletMock).triggerConstantContract(any(),any(),any(),any());
    when(walletMock.createTransactionCapsule(any(), any()))
        .thenReturn(new TransactionCapsule(transaction));
    when(walletMock.getShieldedContractScalingFactor(any())).thenCallRealMethod();

    assertThrows(ContractExeException.class, () -> {
      walletMock.getShieldedContractScalingFactor(contractAddress);
    });
  }

  @Test
  public void testCheckBigIntegerRange() {
    Wallet wallet = new Wallet();

    assertThrows(
        Exception.class,
        () -> {
          Method privateMethod = Wallet.class.getDeclaredMethod(
              "checkBigIntegerRange", BigInteger.class);
          privateMethod.setAccessible(true);
          privateMethod.invoke(wallet, new BigInteger("-1"));
        }
    );
  }

  @Test
  public void testCheckPublicAmount() throws ContractExeException {
    Wallet walletMock = mock(Wallet.class);

    byte[] address = "".getBytes(StandardCharsets.UTF_8);
    BigInteger fromAmount = new BigInteger("10");
    BigInteger toAmount = new BigInteger("10");
    doThrow(new ContractExeException("")).when(walletMock).getShieldedContractScalingFactor(any());

    Throwable thrown = assertThrows(InvocationTargetException.class, () -> {
      Method privateMethod = Wallet.class.getDeclaredMethod(
          "checkPublicAmount",
          byte[].class, BigInteger.class, BigInteger.class);
      privateMethod.setAccessible(true);
      privateMethod.invoke(walletMock, address, fromAmount, toAmount);
    });
    Throwable cause = thrown.getCause();
    assertTrue(cause instanceof ContractExeException);
  }

  @Test
  public void testCheckPublicAmount1() throws ContractExeException {
    Wallet walletMock = mock(Wallet.class);

    byte[] address = "".getBytes(StandardCharsets.UTF_8);
    BigInteger fromAmount = new BigInteger("300");
    BigInteger toAmount = new BigInteger("255");

    byte[] scalingFactorBytes = ByteUtil.bigIntegerToBytes(new BigInteger("-1"));

    when(walletMock.getShieldedContractScalingFactor(any())).thenReturn(scalingFactorBytes);

    Throwable thrown = assertThrows(InvocationTargetException.class, () -> {
      Method privateMethod = Wallet.class.getDeclaredMethod(
          "checkPublicAmount",
          byte[].class, BigInteger.class, BigInteger.class);
      privateMethod.setAccessible(true);
      privateMethod.invoke(walletMock, address, fromAmount, toAmount);
    });
    Throwable cause = thrown.getCause();
    assertTrue(cause instanceof ContractValidateException);
  }

  @Test
  public void testCheckPublicAmount2() throws ContractExeException {
    Wallet walletMock = mock(Wallet.class);

    byte[] address = "".getBytes(StandardCharsets.UTF_8);
    BigInteger fromAmount = new BigInteger("300");
    BigInteger toAmount = new BigInteger("255");

    byte[] scalingFactorBytes = ByteUtil.bigIntegerToBytes(new BigInteger("-1"));
    try (MockedStatic<ByteUtil> mockedStatic = mockStatic(ByteUtil.class)) {
      when(ByteUtil.bytesToBigInteger(any())).thenReturn(new BigInteger("-1"));
      when(walletMock.getShieldedContractScalingFactor(any())).thenReturn(scalingFactorBytes);

      Throwable thrown = assertThrows(InvocationTargetException.class, () -> {
        Method privateMethod = Wallet.class.getDeclaredMethod(
            "checkPublicAmount",
            byte[].class, BigInteger.class, BigInteger.class);
        privateMethod.setAccessible(true);
        privateMethod.invoke(walletMock, address, fromAmount, toAmount);
      });
      Throwable cause = thrown.getCause();
      assertTrue(cause instanceof ContractValidateException);
    }

  }

  @Test
  public void testGetShieldedTRC20Nullifier() {
    Wallet wallet = new Wallet();
    GrpcAPI.Note note = GrpcAPI.Note.newBuilder()
        .setValue(100)
        .setPaymentAddress("address")
        .setRcm(ByteString.copyFrom("rcm".getBytes(StandardCharsets.UTF_8)))
        .setMemo(ByteString.copyFrom("memo".getBytes(StandardCharsets.UTF_8)))
        .build();
    long pos = 100L;
    byte[] ak = "ak".getBytes(StandardCharsets.UTF_8);
    byte[] nk = "nk".getBytes(StandardCharsets.UTF_8);
    try (MockedStatic<KeyIo> keyIoMockedStatic = mockStatic(KeyIo.class)) {
      when(KeyIo.decodePaymentAddress(any())).thenReturn(null);

      Throwable thrown = assertThrows(InvocationTargetException.class, () -> {
        Method privateMethod = Wallet.class.getDeclaredMethod(
            "getShieldedTRC20Nullifier",
            GrpcAPI.Note.class, long.class, byte[].class,
            byte[].class);
        privateMethod.setAccessible(true);
        privateMethod.invoke(wallet,
            note, pos, ak, nk);
      });
      Throwable cause = thrown.getCause();
      assertTrue(cause instanceof ZksnarkException);
    }
  }

  @Test
  public void testGetShieldedTRC20LogType() {
    Wallet wallet = new Wallet();
    Protocol.TransactionInfo.Log log = Protocol.TransactionInfo.Log.newBuilder().build();
    byte[] contractAddress = "contractAddress".getBytes(StandardCharsets.UTF_8);
    LazyStringArrayList topicsList = new LazyStringArrayList();

    Throwable thrown = assertThrows(InvocationTargetException.class, () -> {
      Method privateMethod = Wallet.class.getDeclaredMethod(
          "getShieldedTRC20LogType",
          Protocol.TransactionInfo.Log.class,
          byte[].class,
          ProtocolStringList.class);
      privateMethod.setAccessible(true);
      privateMethod.invoke(wallet,
          log,
          contractAddress,
          topicsList);
    });
    Throwable cause = thrown.getCause();
    assertTrue(cause instanceof ZksnarkException);
  }

  @Test
  public void testGetShieldedTRC20LogType1() {
    Wallet wallet = new Wallet();
    final String SHIELDED_CONTRACT_ADDRESS_STR = "TGAmX5AqVUoXCf8MoHxbuhQPmhGfWTnEgA";
    byte[] contractAddress = WalletClient.decodeFromBase58Check(SHIELDED_CONTRACT_ADDRESS_STR);

    byte[] addressWithoutPrefix = new byte[20];
    System.arraycopy(contractAddress, 1, addressWithoutPrefix, 0, 20);
    Protocol.TransactionInfo.Log log = Protocol.TransactionInfo.Log.newBuilder()
        .setAddress(ByteString.copyFrom(addressWithoutPrefix))
        .build();

    LazyStringArrayList topicsList = new LazyStringArrayList();
    try {
      Method privateMethod = Wallet.class.getDeclaredMethod(
          "getShieldedTRC20LogType",
          Protocol.TransactionInfo.Log.class,
          byte[].class,
          ProtocolStringList.class);
      privateMethod.setAccessible(true);
      privateMethod.invoke(wallet,
          log,
          contractAddress,
          topicsList);
    } catch (Exception e) {
      assertTrue(false);
    }
  }


  @Test
  public void testGetShieldedTRC20LogType2() {
    Wallet wallet = new Wallet();
    final String SHIELDED_CONTRACT_ADDRESS_STR = "TGAmX5AqVUoXCf8MoHxbuhQPmhGfWTnEgA";
    byte[] contractAddress = WalletClient.decodeFromBase58Check(SHIELDED_CONTRACT_ADDRESS_STR);

    byte[] addressWithoutPrefix = new byte[20];
    System.arraycopy(contractAddress, 1, addressWithoutPrefix, 0, 20);
    Protocol.TransactionInfo.Log log = Protocol.TransactionInfo.Log.newBuilder()
        .setAddress(ByteString.copyFrom(addressWithoutPrefix))
        .addTopics(ByteString.copyFrom("topic".getBytes()))
        .build();

    LazyStringArrayList topicsList = new LazyStringArrayList();
    topicsList.add("topic");
    try {
      Method privateMethod = Wallet.class.getDeclaredMethod(
          "getShieldedTRC20LogType",
          Protocol.TransactionInfo.Log.class,
          byte[].class,
          ProtocolStringList.class);
      privateMethod.setAccessible(true);
      privateMethod.invoke(wallet,
          log,
          contractAddress,
          topicsList);
    } catch (Exception e) {
      assertTrue(false);
    }
  }

  @Test
  public void testBuildShieldedTRC20InputWithAK() throws ZksnarkException {
    Wallet wallet = new Wallet();
    ShieldedTRC20ParametersBuilder builder =  new ShieldedTRC20ParametersBuilder("transfer");
    GrpcAPI.Note note = GrpcAPI.Note.newBuilder()
        .setValue(100)
        .setPaymentAddress("address")
        .setRcm(ByteString.copyFrom("rcm".getBytes(StandardCharsets.UTF_8)))
        .setMemo(ByteString.copyFrom("memo".getBytes(StandardCharsets.UTF_8)))
        .build();
    GrpcAPI.SpendNoteTRC20 spendNote = GrpcAPI.SpendNoteTRC20.newBuilder()
        .setNote(note)
        .setAlpha(ByteString.copyFrom("alpha".getBytes()))
        .setRoot(ByteString.copyFrom("root".getBytes()))
        .setPath(ByteString.copyFrom("path".getBytes()))
        .setPos(0L)
        .build();
    byte[] ak = "ak".getBytes(StandardCharsets.UTF_8);
    byte[] nk = "nk".getBytes(StandardCharsets.UTF_8);

    try (MockedStatic<KeyIo> keyIoMockedStatic = mockStatic(KeyIo.class)) {
      when(KeyIo.decodePaymentAddress(any())).thenReturn(null);

      Throwable thrown = assertThrows(InvocationTargetException.class, () -> {
        Method privateMethod = Wallet.class.getDeclaredMethod(
            "buildShieldedTRC20InputWithAK",
            ShieldedTRC20ParametersBuilder.class,
            GrpcAPI.SpendNoteTRC20.class,
            byte[].class, byte[].class);
        privateMethod.setAccessible(true);
        privateMethod.invoke(wallet,
            builder,
            spendNote,
            ak, nk);
      });
      Throwable cause = thrown.getCause();
      assertTrue(cause instanceof ZksnarkException);
    }

  }

  @Test
  public void testBuildShieldedTRC20InputWithAK1() throws Exception {
    Wallet wallet = new Wallet();
    ShieldedTRC20ParametersBuilder builder =  new ShieldedTRC20ParametersBuilder("transfer");
    GrpcAPI.Note note = GrpcAPI.Note.newBuilder()
        .setValue(100)
        .setPaymentAddress("address")
        .setRcm(ByteString.copyFrom("rcm".getBytes(StandardCharsets.UTF_8)))
        .setMemo(ByteString.copyFrom("memo".getBytes(StandardCharsets.UTF_8)))
        .build();
    GrpcAPI.SpendNoteTRC20 spendNote = GrpcAPI.SpendNoteTRC20.newBuilder()
        .setNote(note)
        .setAlpha(ByteString.copyFrom("alpha".getBytes()))
        .setRoot(ByteString.copyFrom("root".getBytes()))
        .setPath(ByteString.copyFrom("path".getBytes()))
        .setPos(0L)
        .build();
    byte[] ak = "ak".getBytes(StandardCharsets.UTF_8);
    byte[] nk = "nk".getBytes(StandardCharsets.UTF_8);
    PaymentAddress paymentAddress = mock(PaymentAddress.class);
    DiversifierT diversifierT = mock(DiversifierT.class);

    try (MockedStatic<KeyIo> keyIoMockedStatic = mockStatic(KeyIo.class)) {
      when(KeyIo.decodePaymentAddress(any())).thenReturn(paymentAddress);
      when(paymentAddress.getD()).thenReturn(diversifierT);
      when(paymentAddress.getPkD()).thenReturn("pkd".getBytes());

      Method privateMethod = Wallet.class.getDeclaredMethod(
          "buildShieldedTRC20InputWithAK",
          ShieldedTRC20ParametersBuilder.class,
          GrpcAPI.SpendNoteTRC20.class,
          byte[].class, byte[].class);
      privateMethod.setAccessible(true);
      privateMethod.invoke(wallet,
          builder,
          spendNote,
          ak, nk);
    }

  }

  @Test
  public void testBuildShieldedTRC20Input() throws Exception {
    Wallet wallet = new Wallet();
    ShieldedTRC20ParametersBuilder builder =  new ShieldedTRC20ParametersBuilder("transfer");
    GrpcAPI.Note note = GrpcAPI.Note.newBuilder()
        .setValue(100)
        .setPaymentAddress("address")
        .setRcm(ByteString.copyFrom("rcm".getBytes(StandardCharsets.UTF_8)))
        .setMemo(ByteString.copyFrom("memo".getBytes(StandardCharsets.UTF_8)))
        .build();
    GrpcAPI.SpendNoteTRC20 spendNote = GrpcAPI.SpendNoteTRC20.newBuilder()
        .setNote(note)
        .setAlpha(ByteString.copyFrom("alpha".getBytes()))
        .setRoot(ByteString.copyFrom("root".getBytes()))
        .setPath(ByteString.copyFrom("path".getBytes()))
        .setPos(0L)
        .build();
    ExpandedSpendingKey expandedSpendingKey = mock(ExpandedSpendingKey.class);
    PaymentAddress paymentAddress = mock(PaymentAddress.class);
    DiversifierT diversifierT = mock(DiversifierT.class);

    try (MockedStatic<KeyIo> keyIoMockedStatic = mockStatic(KeyIo.class)) {
      when(KeyIo.decodePaymentAddress(any())).thenReturn(paymentAddress);
      when(paymentAddress.getD()).thenReturn(diversifierT);
      when(paymentAddress.getPkD()).thenReturn("pkd".getBytes());
      Method privateMethod = Wallet.class.getDeclaredMethod(
          "buildShieldedTRC20Input",
          ShieldedTRC20ParametersBuilder.class,
          GrpcAPI.SpendNoteTRC20.class,
          ExpandedSpendingKey.class);
      privateMethod.setAccessible(true);
      privateMethod.invoke(wallet,
          builder,
          spendNote,
          expandedSpendingKey);
    }
  }

  @Test
  public void testGetContractInfo() throws Exception {
    Wallet wallet = new Wallet();
    GrpcAPI.BytesMessage bytesMessage = GrpcAPI.BytesMessage.newBuilder()
        .setValue(ByteString.copyFrom("test".getBytes()))
        .build();

    ChainBaseManager chainBaseManagerMock = mock(ChainBaseManager.class);
    AccountStore accountStore = mock(AccountStore.class);
    Field field = wallet.getClass().getDeclaredField("chainBaseManager");
    field.setAccessible(true);
    field.set(wallet, chainBaseManagerMock);
    when(chainBaseManagerMock.getAccountStore()).thenReturn(accountStore);
    when(accountStore.get(any())).thenReturn(null);

    SmartContractOuterClass.SmartContractDataWrapper smartContractDataWrapper =
        wallet.getContractInfo(bytesMessage);
    assertNull(smartContractDataWrapper);
  }

  @Test
  public void testGetContractInfo1() throws Exception {
    Wallet wallet = new Wallet();
    GrpcAPI.BytesMessage bytesMessage = GrpcAPI.BytesMessage.newBuilder()
        .setValue(ByteString.copyFrom("test".getBytes()))
        .build();

    ChainBaseManager chainBaseManagerMock = mock(ChainBaseManager.class);
    AccountStore accountStore = mock(AccountStore.class);
    ContractStore contractStore = mock(ContractStore.class);
    AbiStore abiStore = mock(AbiStore.class);
    CodeStore codeStore = mock(CodeStore.class);
    ContractStateStore contractStateStore = mock(ContractStateStore.class);
    DynamicPropertiesStore dynamicPropertiesStore = mock(DynamicPropertiesStore.class);

    AccountCapsule accountCapsule = mock(AccountCapsule.class);
    ContractCapsule contractCapsule = mock(ContractCapsule.class);
    ContractStateCapsule contractStateCapsule = new ContractStateCapsule(10L);

    Field field = wallet.getClass().getDeclaredField("chainBaseManager");
    field.setAccessible(true);
    field.set(wallet, chainBaseManagerMock);
    when(chainBaseManagerMock.getAccountStore()).thenReturn(accountStore);
    when(chainBaseManagerMock.getContractStore()).thenReturn(contractStore);
    when(chainBaseManagerMock.getAbiStore()).thenReturn(abiStore);
    when(chainBaseManagerMock.getCodeStore()).thenReturn(codeStore);
    when(chainBaseManagerMock.getContractStateStore()).thenReturn(contractStateStore);
    when(chainBaseManagerMock.getDynamicPropertiesStore()).thenReturn(dynamicPropertiesStore);

    when(accountStore.get(any())).thenReturn(accountCapsule);
    when(contractStore.get(any())).thenReturn(contractCapsule);
    when(contractCapsule.generateWrapper())
        .thenReturn(SmartContractOuterClass.SmartContractDataWrapper.newBuilder().build());
    when(abiStore.get(any())).thenReturn(null);
    when(codeStore.get(any())).thenReturn(null);
    when(contractStateStore.get(any())).thenReturn(contractStateCapsule);
    when(dynamicPropertiesStore.getCurrentCycleNumber()).thenReturn(100L);

    SmartContractOuterClass.SmartContractDataWrapper smartContractDataWrapper =
        wallet.getContractInfo(bytesMessage);
    assertNotNull(smartContractDataWrapper);
  }
}
