package org.tron.core.ibc.communicate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import com.google.protobuf.ByteString;

import java.io.File;
import java.lang.reflect.Field;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Commons;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.ReflectUtils;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.ChainBaseManager;
import org.tron.core.Constant;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.BlockIndexStore;
import org.tron.core.db.BlockStore;
import org.tron.core.db.CrossStore;
import org.tron.core.db.Manager;

import org.tron.core.event.entity.PbftBlockCommitEvent;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.ibc.common.Utils;

import org.tron.protos.Protocol;
import org.tron.protos.contract.BalanceContract;



public class PbftBlockListenerTest {

  private static TronApplicationContext context;
  private static Manager dbManager;
  private ChainBaseManager chainBaseManager;
  private CrossStore crossStore;
  private BlockStore blockStore;
  private BlockIndexStore blockIndexStore;

  private PbftBlockListener pbftBlockListener;
  private CommunicateService mockCommunicateService;

  public static ByteString owner = ByteString
      .copyFrom(Commons.decodeFromBase58Check("TCWHANtDDdkZCTo2T2peyEq3Eg9c2XB7ut"));

  private static String KEY_11 = "1111111111111111111111111111111111111111111111111111111111111111";
  private static String KEY_12 = "1212121212121212121212121212121212121212121212121212121212121212";

  private static String FROMCHAINID = "";
  private static String TOCHAINID = "";

  /**
   * init static var
   */
  static {
    Args.setParam(new String[]{"--output-directory",
        "dbPath-PbftBlockListenserTest"}, Constant.TEST_CONF);
    FROMCHAINID =
        "00000000000000007adbf8dc20423f587a5f3f8ea83e2877e2129c5128c12d11";
    TOCHAINID =
        "00000000000000007adbf8dc20433f587a5f3f8ea83e2877e2129c5128c12d12";

  }

  @BeforeClass
  public static void init() {
    context = new TronApplicationContext(DefaultConfig.class);
    dbManager = context.getBean(Manager.class);

  }

  /**
   * release resources.
   */
  @AfterClass
  public static void destroy() {
    Args.clearParam();
    context.destroy();
    FileUtil.deleteDir(new File("dbPath-PbftBlockListenserTest"));
  }

  /**
   * init paras.
   */
  @Before
  public void initParas() {
    chainBaseManager = dbManager.getChainBaseManager();
    crossStore = chainBaseManager.getCrossStore();
    pbftBlockListener = context.getBean(PbftBlockListener.class);
    blockStore = chainBaseManager.getBlockStore();
    blockIndexStore = chainBaseManager.getBlockIndexStore();
    mockCommunicateService = mock(CommunicateService.class);
  }


  public static BalanceContract.CrossContract buildCrossContract(int amount) {
    BalanceContract.CrossToken.Builder crossToken = BalanceContract.CrossToken.newBuilder();
    crossToken.setAmount(amount).setTokenId(ByteString.copyFrom(ByteArray.fromString("1000001")))
        .setTokenName(ByteString.copyFrom(ByteArray.fromString("testCross"))).setPrecision(0)
        .setChainId(Sha256Hash.wrap(ByteArray
            .fromHexString(
                "000000000000000019b59068c6058ff466ccf59f2c38a0df1c330b9b7e8dcc4c"))
            .getByteString());

    BalanceContract.CrossContract crossContract = BalanceContract.CrossContract.newBuilder()
        .setOwnerAddress(owner)
        .setOwnerChainId(Sha256Hash.wrap(ByteArray
            .fromHexString(
                "000000000000000019b59068c6058ff466ccf59f2c38a0df1c330b9b7e8dcc4c"))
            .getByteString())
        .setToAddress(owner)
        .setToChainId(Sha256Hash.wrap(
            ByteArray.fromHexString(
                "0000000000000000d4b7cf850c78c1c779d19446edeafdfeb30875060e5dcee8"))
            .getByteString())
        .setType(BalanceContract.CrossContract.CrossDataType.TOKEN)
        .setData(crossToken.build().toByteString())
        .build();
    return crossContract;
  }


  private Protocol.CrossMessage buildCrossMessage(TransactionCapsule trx) {
    Protocol.CrossMessage crossMessage = Protocol.CrossMessage.newBuilder()
        .setType(Protocol.CrossMessage.Type.DATA)
        .setTransaction(trx.getInstance())
        .setRootHeight(1)
        .setFromChainId(Sha256Hash.wrap(ByteArray
            .fromHexString(FROMCHAINID))
            .getByteString())
        .setToChainId(Sha256Hash.wrap(ByteArray
            .fromHexString(TOCHAINID))
            .getByteString())
        .setRouteChainId(Sha256Hash.wrap(ByteArray
            .fromHexString(TOCHAINID))
            .getByteString())
        .build();
    return crossMessage;
  }

  private BlockCapsule buildBlockCapsuleWithMultiTx() {
    BlockCapsule blockCapsule = Utils.buildBlockCapsule(chainBaseManager);
    TransactionCapsule trx1 = new TransactionCapsule(buildCrossContract(100),
        Protocol.Transaction.Contract.ContractType.CrossContract);
    blockCapsule.addTransaction(trx1);
    trx1.setBlockNum(blockCapsule.getNum());
    Protocol.CrossMessage crossMessage1 = buildCrossMessage(trx1);
    blockCapsule.addCrossMessage(crossMessage1);

    TransactionCapsule trx2 = new TransactionCapsule(buildCrossContract(101),
        Protocol.Transaction.Contract.ContractType.CrossContract);
    blockCapsule.addTransaction(trx2);
    trx2.setBlockNum(blockCapsule.getNum());
    Protocol.CrossMessage crossMessage2 = buildCrossMessage(trx2);
    blockCapsule.addCrossMessage(crossMessage2);

    TransactionCapsule trx3 = new TransactionCapsule(buildCrossContract(102),
        Protocol.Transaction.Contract.ContractType.CrossContract);
    blockCapsule.addTransaction(trx3);
    trx3.setBlockNum(blockCapsule.getNum());
    Protocol.CrossMessage crossMessage3 = buildCrossMessage(trx3);
    blockCapsule.addCrossMessage(crossMessage3);

    TransactionCapsule trx4 = new TransactionCapsule(buildCrossContract(103),
        Protocol.Transaction.Contract.ContractType.CrossContract);
    blockCapsule.addTransaction(trx4);
    trx4.setBlockNum(blockCapsule.getNum());
    Protocol.CrossMessage crossMessage4 = buildCrossMessage(trx4);
    blockCapsule.addCrossMessage(crossMessage4);
    return blockCapsule;
  }

  /**
   * method specific initiation
   */
  @Test
  public void listenerBlockCommitEvent() throws NoSuchFieldException, IllegalAccessException {

    //check when not store block
    try {
      PbftBlockCommitEvent event1 = new PbftBlockCommitEvent(-1, ByteString.EMPTY);
      pbftBlockListener.listener(event1);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof ItemNotFoundException);
    }

    //check when currentBlockNum is 0
    BlockCapsule blockCapsule = buildBlockCapsuleWithMultiTx();
    blockStore.put(blockCapsule.getBlockId().getBytes(), blockCapsule);
    blockIndexStore.put(blockCapsule.getBlockId());
    PbftBlockCommitEvent event2 = new PbftBlockCommitEvent(100, ByteString.EMPTY);
    pbftBlockListener.listener(event2);
    long updatedCurrentBlockNum = ReflectUtils.getFieldValue(pbftBlockListener, "currentBlockNum");
    Assert.assertEquals(updatedCurrentBlockNum, 99L);

    //check when currentBlockNum < index and callBackTx is empty
    Class<PbftBlockListener> clazz = PbftBlockListener.class;
    Field field = clazz.getDeclaredField("currentBlockNum");
    field.setAccessible(true);
    field.set(pbftBlockListener, 91);
    field.setAccessible(false);
    PbftBlockCommitEvent event3 = new PbftBlockCommitEvent(100, ByteString.EMPTY);
    pbftBlockListener.listener(event3);
    long updatedCurrentBlockNum2 = ReflectUtils.getFieldValue(pbftBlockListener, "currentBlockNum");
    Assert.assertEquals(updatedCurrentBlockNum2, 95L);


    //check listenerBlockCommitEvent work correctly
    addCallBackTx();
    //set currentNum
    field.setAccessible(true);
    field.set(pbftBlockListener, 91);
    field.setAccessible(false);
    when(mockCommunicateService.checkCommit(any())).thenReturn(true);
    when(mockCommunicateService.getRouteChainId()).thenReturn(Sha256Hash.wrap(ByteArray
        .fromHexString(TOCHAINID))
        .getByteString());
    when(mockCommunicateService.getLocalChainId()).thenReturn(Sha256Hash.wrap(ByteArray
        .fromHexString(FROMCHAINID))
        .getByteString());
    when(mockCommunicateService.getHeight(any())).thenReturn(99999L);
    pbftBlockListener.listener(event3);
    //检查sendCrossMessage是否被调用了2次
    Mockito.verify(mockCommunicateService, times(1)).sendCrossMessage(any(),anyBoolean());

  }





  @Test
  public void addCallBackTx() throws NoSuchFieldException, IllegalAccessException {

    //when isSyncFinish is false
    Class<PbftBlockListener> clazz = PbftBlockListener.class;
    Field field = clazz.getDeclaredField("communicateService");
    field.setAccessible(true);
    field.set(pbftBlockListener, mockCommunicateService);
    field.setAccessible(false);
    when(mockCommunicateService.isSyncFinish()).thenReturn(false);
    boolean result1 = pbftBlockListener.addCallBackTx(null, 1, null);
    Assert.assertFalse(result1);

    //check contract type is wrong
    long blockNum = 92L;
    BalanceContract.CrossContract crossContract = buildCrossContract(100);
    TransactionCapsule trx = new TransactionCapsule(crossContract,
        Protocol.Transaction.Contract.ContractType.UnvoteCrossChainContract);
    when(mockCommunicateService.isSyncFinish()).thenReturn(true);
    boolean result2 = pbftBlockListener.addCallBackTx(chainBaseManager, blockNum, trx);
    Assert.assertFalse(result2);

    //check isSource true
    trx.setSource(true);
    TransactionCapsule trx1 = new TransactionCapsule(crossContract,
        Protocol.Transaction.Contract.ContractType.CrossContract);
    trx1.setBlockNum(92);
    when(mockCommunicateService.isSyncFinish()).thenReturn(true);
    boolean result3 = pbftBlockListener.addCallBackTx(chainBaseManager, blockNum, trx1);
    Assert.assertTrue(result3);


    //check contract type is TOKEN
    TransactionCapsule trx2 = new TransactionCapsule(crossContract,
        Protocol.Transaction.Contract.ContractType.CrossContract);
    trx2.setSource(false);
    trx2.setBlockNum(92);
    Protocol.CrossMessage crossMessage = buildCrossMessage(trx2);
    crossStore.saveReceiveCrossMsg(trx2.getTransactionId(), crossMessage);
    boolean result4 = pbftBlockListener.addCallBackTx(chainBaseManager, blockNum, trx2);
    Assert.assertTrue(result4);

  }

}
