package org.tron.core.ibc.spv;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.cache.Cache;
import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.crypto.ECKey;
import org.tron.common.overlay.server.SyncPool;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.ReflectUtils;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.ChainBaseManager;
import org.tron.core.Constant;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.BlockHeaderCapsule;
import org.tron.core.capsule.PbftSignCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.BadBlockException;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.exception.ValidateSignatureException;
import org.tron.core.ibc.common.Utils;
import org.tron.core.ibc.spv.message.BlockHeaderInventoryMesasge;
import org.tron.core.ibc.spv.message.BlockHeaderRequestMessage;
import org.tron.core.ibc.spv.message.BlockHeaderUpdatedNoticeMessage;
import org.tron.core.net.peer.PeerConnection;
import org.tron.protos.Protocol;



@Slf4j
public class CrossHeaderMsgProcessTest {

  private static TronApplicationContext context;
  private static Manager dbManager;
  private ChainBaseManager chainBaseManager;
  private CrossHeaderMsgProcess crossHeaderMsgProcess;
  private PeerConnection peer;
  private SyncPool mockSyncPool;

  private static String CHAIN_ID =
      "00000000000000007adbf8dc20423f587a5f3f8ea83e2877e2129c5128c12d11";

  private static String KEY_11 =
      "1111111111111111111111111111111111111111111111111111111111111111";
  private static String KEY_12 =
      "1212121212121212121212121212121212121212121212121212121212121212";

  /**
   * init static var
   */
  static {
    Args.setParam(new String[]{"--output-directory", "dbPath-crossHeaderMsgProcessTest"},
        Constant.TEST_CONF);
  }

  /**
   * release resources.
   */
  @AfterClass
  public static void destroy() {
    Args.clearParam();
    context.destroy();
    FileUtil.deleteDir(new File("dbPath-crossHeaderMsgProcessTest"));
  }

  @BeforeClass
  public static void init() {
    context = new TronApplicationContext(DefaultConfig.class);
    dbManager = context.getBean(Manager.class);
  }

  @Before
  public void initParas() {
    CommonParameter.getInstance().setShouldRegister(false);
    chainBaseManager = dbManager.getChainBaseManager();
    crossHeaderMsgProcess = context.getBean(CrossHeaderMsgProcess.class);
    peer = context.getBean(PeerConnection.class);
    mockSyncPool = mock(SyncPool.class);
  }

  private Protocol.SignedBlockHeader buildSignedBlockHeader() {
    BlockCapsule blockCapsule = Utils.buildBlockCapsule(chainBaseManager);
    BlockHeaderCapsule blockHeaderCapsule = new BlockHeaderCapsule(
        blockCapsule.getInstance().getBlockHeader());
    Protocol.SignedBlockHeader.Builder builder = Protocol.SignedBlockHeader.newBuilder();
    builder.setBlockHeader(blockHeaderCapsule.getInstance());
    return builder.build();
  }

  @Test
  public void handleCrossUpdatedNotice() throws NoSuchFieldException,
      IllegalAccessException, BadBlockException, InvalidProtocolBufferException,
      ValidateSignatureException {
    Protocol.SignedBlockHeader signedBlockHeader = buildSignedBlockHeader();
    Protocol.BlockHeaderUpdatedNotice blockHeaderUpdatedNotice = Protocol
        .BlockHeaderUpdatedNotice.newBuilder()
        .setChainId(Sha256Hash.wrap(ByteArray
            .fromHexString(CHAIN_ID))
            .getByteString())
        .setSignedBlockHeader(signedBlockHeader)
        .build();
    chainBaseManager.getCommonDataBase().saveLatestHeaderBlockNum(CHAIN_ID,
        100, true);

    Class<CrossHeaderMsgProcess> clazz = CrossHeaderMsgProcess.class;
    Field field = clazz.getDeclaredField("syncPool");
    field.setAccessible(true);
    field.set(crossHeaderMsgProcess, mockSyncPool);
    field.setAccessible(false);
    when(mockSyncPool.getActivePeers()).thenReturn(new ArrayList<>());

    HeaderManager mockHeaderManager = mock(HeaderManager.class);
    Field field2 = clazz.getDeclaredField("headerManager");
    field2.setAccessible(true);
    field2.set(crossHeaderMsgProcess, mockHeaderManager);
    field2.setAccessible(false);

    when(mockHeaderManager.isExist(any(),any())).thenReturn(false);
    crossHeaderMsgProcess.handleCrossUpdatedNotice(peer,
        new BlockHeaderUpdatedNoticeMessage(blockHeaderUpdatedNotice));

    Map<String, Boolean> syncDisabledMap = ReflectUtils.getFieldValue(crossHeaderMsgProcess,
        "syncDisabledMap");
    Assert.assertTrue(syncDisabledMap.get(CHAIN_ID));

    Cache<String, Long> sendHeaderNumCache = ReflectUtils.getFieldValue(crossHeaderMsgProcess,
        "sendHeaderNumCache");
    Assert.assertNull(sendHeaderNumCache.getIfPresent(CHAIN_ID));

    Map<String, Long> syncBlockHeaderMap = ReflectUtils.getFieldValue(crossHeaderMsgProcess,
        "syncBlockHeaderMap");
    Assert.assertEquals((long) syncBlockHeaderMap.get(CHAIN_ID), 100L);

    Map<String, Long> missBlockHeaderMap = ReflectUtils.getFieldValue(crossHeaderMsgProcess,
        "missBlockHeaderMap");
    Assert.assertEquals((long) missBlockHeaderMap.get(CHAIN_ID), 100L);
  }

  @Test
  public void handleInventory() throws NoSuchFieldException, IllegalAccessException {
    Class<CrossHeaderMsgProcess> clazz = CrossHeaderMsgProcess.class;
    Cache<String, Long> mockSendHeaderNumCache = mock(Cache.class);
    Field field = clazz.getDeclaredField("sendHeaderNumCache");
    field.setAccessible(true);
    field.set(crossHeaderMsgProcess, mockSendHeaderNumCache);
    field.setAccessible(false);
    when(mockSendHeaderNumCache.getIfPresent(any())).thenReturn(99L);

    //when blockHeaders is null
    BlockHeaderInventoryMesasge inventoryMesasgeWithoutHeaders = new BlockHeaderInventoryMesasge(
        CHAIN_ID, 1, Lists.newArrayList());
    crossHeaderMsgProcess.handleInventory(peer, inventoryMesasgeWithoutHeaders);

    Map<String, Set<PeerConnection>> syncFailPeerMap = ReflectUtils
        .getFieldValue(crossHeaderMsgProcess, "syncFailPeerMap");
    Assert.assertNotNull(syncFailPeerMap.get(CHAIN_ID));
    Assert.assertEquals(syncFailPeerMap.get(CHAIN_ID).contains(peer), true);
    Cache<String, Long> sendHeaderNumCache = ReflectUtils.getFieldValue(crossHeaderMsgProcess,
        "sendHeaderNumCache");
    Assert.assertNotNull(sendHeaderNumCache.getIfPresent(CHAIN_ID));

    //when chainIdStr not cache
    BlockHeaderInventoryMesasge inventoryMessage = buildBlockHeaderInventoryMesasge();
    crossHeaderMsgProcess.handleInventory(peer, inventoryMessage);
    Map<String, Cache<Long, Protocol.SignedBlockHeader>> chainHeaderCache = ReflectUtils
        .getFieldValue(crossHeaderMsgProcess, "chainHeaderCache");
    Assert.assertNotNull(chainHeaderCache.get(CHAIN_ID));

    //check sendHeight
    Map<String, Long> syncBlockHeaderMap = ReflectUtils.getFieldValue(crossHeaderMsgProcess,
        "syncBlockHeaderMap");
    Assert.assertEquals((long) syncBlockHeaderMap.get(CHAIN_ID), 100L);

    Cache<String, Long> sendHeaderNumCache2 = ReflectUtils.getFieldValue(crossHeaderMsgProcess,
        "sendHeaderNumCache");
    Assert.assertNotNull(sendHeaderNumCache2.getIfPresent(CHAIN_ID));


  }

  private BlockHeaderInventoryMesasge buildBlockHeaderInventoryMesasge() {
    BlockHeaderInventoryMesasge inventoryMesasge = new BlockHeaderInventoryMesasge(CHAIN_ID,
        1, Lists.newArrayList(buildSignedBlockHeader()));
    return inventoryMesasge;
  }

  private PbftSignCapsule buildPbftSignCapsule() {
    List<byte[]> prikeys = new ArrayList<>();
    prikeys.add(ByteArray.fromHexString(KEY_11));
    prikeys.add(ByteArray.fromHexString(KEY_12));

    ArrayList<ByteString> signList = new ArrayList<>();
    for (byte[] priKey : prikeys) {
      ECKey ecKey = ECKey.fromPrivate(priKey);
      byte[] hash = new byte[32];
      ECKey.ECDSASignature signature = ecKey.sign(hash);
      ByteString result = ByteString.copyFrom(signature.toByteArray());
      signList.add(result);
    }
    PbftSignCapsule pbftSignCapsule = new PbftSignCapsule(signList);
    return pbftSignCapsule;
  }

  @Test
  public void handleRequest() throws BadItemException, ItemNotFoundException {


    PbftSignCapsule pbftSignCapsule = buildPbftSignCapsule();
    chainBaseManager.getPbftSignDataStore().putBlockSignData(100, pbftSignCapsule);

    BlockCapsule blockCapsule = Utils.buildBlockCapsule(chainBaseManager);
    chainBaseManager.getBlockIndexStore().put(blockCapsule.getBlockId());
    chainBaseManager.getBlockStore().put(blockCapsule.getBlockId().getBytes(), blockCapsule);
    chainBaseManager.setGenesisBlock(blockCapsule);
    BlockHeaderRequestMessage requestMessage = buildBlockHeaderRequestMessage();

    chainBaseManager.getCommonDataBase().saveLatestPbftBlockNum(100);
    peer = mock(PeerConnection.class);
    crossHeaderMsgProcess.handleRequest(peer, requestMessage);
    peer.sendMessage(any());
    Map<String, Long> latestMaintenanceTimeMap = ReflectUtils.getFieldValue(crossHeaderMsgProcess,
        "latestMaintenanceTimeMap");
    Assert.assertEquals((long)latestMaintenanceTimeMap.get(CHAIN_ID),0L);
  }

  private BlockHeaderRequestMessage buildBlockHeaderRequestMessage() {
    BlockHeaderRequestMessage requestMessage = new BlockHeaderRequestMessage(CHAIN_ID,
        99, 64, 2);
    return requestMessage;
  }

  private BlockHeaderUpdatedNoticeMessage buildBlockHeaderUpdatedNoticeMessage() {
    Protocol.BlockHeaderUpdatedNotice blockHeaderUpdatedNotice = Protocol
        .BlockHeaderUpdatedNotice.newBuilder()
        .setChainId(Sha256Hash.wrap(ByteArray
            .fromHexString(CHAIN_ID))
            .getByteString())
        .setSignedBlockHeader(buildSignedBlockHeader())
        .build();
    return new BlockHeaderUpdatedNoticeMessage(blockHeaderUpdatedNotice);
  }


}
