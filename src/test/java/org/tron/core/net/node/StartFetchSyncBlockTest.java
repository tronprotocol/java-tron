package org.tron.core.net.node;

import com.google.common.cache.Cache;
import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.junit.*;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.overlay.client.PeerClient;
import org.tron.common.overlay.discover.node.Node;
import org.tron.common.overlay.server.Channel;
import org.tron.common.overlay.server.ChannelManager;
import org.tron.common.overlay.server.SyncPool;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.ReflectUtils;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.ByteArrayWrapper;
import org.tron.core.db.Manager;
import org.tron.core.net.message.BlockMessage;
import org.tron.core.net.node.override.HandshakeHandlerTest;
import org.tron.core.net.node.override.PeerClientTest;
import org.tron.core.net.node.override.TronChannelInitializerTest;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.services.RpcApiService;
import org.tron.core.services.WitnessService;
import org.tron.protos.Protocol;

@Slf4j
public class StartFetchSyncBlockTest {

  private static TronApplicationContext context;
  private NodeImpl node;
  private RpcApiService rpcApiService;
  private static PeerClientTest peerClient;
  private ChannelManager channelManager;
  private SyncPool pool;
  private static Application appT;
  private Node nodeEntity;
  private static HandshakeHandlerTest handshakeHandlerTest;
  private static final String dbPath = "output-nodeImplTest-startFetchSyncBlockTest";
  private static final String dbDirectory = "db_StartFetchSyncBlock_test";
  private static final String indexDirectory = "index_StartFetchSyncBlock_test";

  private class Condition {

    private Sha256Hash blockId;

    public Condition(Sha256Hash blockId) {
      this.blockId = blockId;
    }

    public Sha256Hash getBlockId() {
      return blockId;
    }

  }

  private Sha256Hash testBlockBroad() {
    Protocol.Block block = Protocol.Block.getDefaultInstance();
    BlockMessage blockMessage = new BlockMessage(new BlockCapsule(block));
    node.broadcast(blockMessage);
    ConcurrentHashMap<Sha256Hash, Protocol.Inventory.InventoryType> advObjToSpread = ReflectUtils
        .getFieldValue(node, "advObjToSpread");
    Assert.assertEquals(advObjToSpread.get(blockMessage.getMessageId()),
        Protocol.Inventory.InventoryType.BLOCK);
    return blockMessage.getMessageId();
  }

  private BlockMessage removeTheBlock(Sha256Hash blockId) {
    Cache<Sha256Hash, BlockMessage> blockCache = ReflectUtils.getFieldValue(node, "BlockCache");
    BlockMessage blockMessage = blockCache.getIfPresent(blockId);
    if (blockMessage != null) {
      blockCache.invalidate(blockId);
    }
    return blockMessage;
  }

  private void addTheBlock(BlockMessage blockMessag) {
    Cache<Sha256Hash, BlockMessage> blockCache = ReflectUtils.getFieldValue(node, "BlockCache");
    blockCache.put(blockMessag.getMessageId(), blockMessag);
  }

  private Condition testConsumerAdvObjToSpread() {
    Sha256Hash blockId = testBlockBroad();
    //remove the block
    BlockMessage blockMessage = removeTheBlock(blockId);
    ReflectUtils.invokeMethod(node, "consumerAdvObjToSpread");
    Collection<PeerConnection> activePeers = ReflectUtils.invokeMethod(node, "getActivePeer");

    boolean result = true;
    for (PeerConnection peerConnection : activePeers) {
      if (!peerConnection.getAdvObjWeSpread().containsKey(blockId)) {
        result &= false;
      }
    }
    for (PeerConnection peerConnection : activePeers) {
      peerConnection.getAdvObjWeSpread().clear();
    }
    Assert.assertTrue(result);
    return new Condition(blockId);
  }

  @Test
  public void testStartFetchSyncBlock() throws InterruptedException {
    testConsumerAdvObjToSpread();
    Collection<PeerConnection> activePeers = ReflectUtils.invokeMethod(node, "getActivePeer");
    Thread.sleep(1000);
    ReflectUtils.setFieldValue(activePeers.iterator().next(), "needSyncFromPeer", true);
    // construct a block
    Protocol.Block block = Protocol.Block.getDefaultInstance();
    BlockMessage blockMessage = new BlockMessage(new BlockCapsule(block));
    // push the block to syncBlockToFetch
    activePeers.iterator().next().getSyncBlockToFetch().push(blockMessage.getBlockId());
    // invoke testing method
    addTheBlock(blockMessage);
    ReflectUtils.invokeMethod(node, "startFetchSyncBlock");
    Cache syncBlockIdWeRequested = ReflectUtils
        .getFieldValue(node, "syncBlockIdWeRequested");
    Assert.assertTrue(syncBlockIdWeRequested.size() == 1);
  }


  private static boolean go = false;

  @Before
  public void init() {
    nodeEntity = new Node(
        "enode://e437a4836b77ad9d9ffe73ee782ef2614e6d8370fcf62191a6e488276e23717147073a7ce0b444d485fff5a0c34c4577251a7a990cf80d8542e21b95aa8c5e6c@127.0.0.1:17890");

    Thread thread = new Thread(new Runnable() {
      @Override
      public void run() {
        logger.info("Full node running.");
        Args.setParam(
            new String[]{
                "--output-directory", dbPath,
                "--storage-db-directory", dbDirectory,
                "--storage-index-directory", indexDirectory
            },
            "config.conf"
        );
        Args cfgArgs = Args.getInstance();
        cfgArgs.setNodeListenPort(17890);
        cfgArgs.setNodeDiscoveryEnable(false);
        cfgArgs.getSeedNode().getIpList().clear();
        cfgArgs.setNeedSyncCheck(false);
        cfgArgs.setNodeExternalIp("127.0.0.1");

        context = new TronApplicationContext(DefaultConfig.class);

        if (cfgArgs.isHelp()) {
          logger.info("Here is the help message.");
          return;
        }
        appT = ApplicationFactory.create(context);
        rpcApiService = context.getBean(RpcApiService.class);
        appT.addService(rpcApiService);
        if (cfgArgs.isWitness()) {
          appT.addService(new WitnessService(appT, context));
        }
//        appT.initServices(cfgArgs);
//        appT.startServices();
//        appT.startup();
        node = context.getBean(NodeImpl.class);
        peerClient = context.getBean(PeerClientTest.class);
        channelManager = context.getBean(ChannelManager.class);
        pool = context.getBean(SyncPool.class);
        Manager dbManager = context.getBean(Manager.class);
        handshakeHandlerTest = context.getBean(HandshakeHandlerTest.class);
        handshakeHandlerTest.setNode(nodeEntity);
        NodeDelegate nodeDelegate = new NodeDelegateImpl(dbManager);
        node.setNodeDelegate(nodeDelegate);
        pool.init(node);
        prepare();
        rpcApiService.blockUntilShutdown();
      }
    });
    thread.start();
    try {
      thread.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    int tryTimes = 0;
    while (tryTimes < 10 && (node == null || peerClient == null
        || channelManager == null || pool == null || !go)) {
      try {
        logger.info("node:{},peerClient:{},channelManager:{},pool:{},{}", node, peerClient,
            channelManager, pool, go);
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      } finally {
        ++tryTimes;
      }
    }
  }

  private void prepare() {
    try {
      ExecutorService advertiseLoopThread = ReflectUtils.getFieldValue(node, "broadPool");
      advertiseLoopThread.shutdownNow();

      peerClient.prepare(nodeEntity.getHexId());

      ReflectUtils.setFieldValue(node, "isAdvertiseActive", false);
      ReflectUtils.setFieldValue(node, "isFetchActive", false);

      TronChannelInitializerTest tronChannelInitializer = ReflectUtils
          .getFieldValue(peerClient, "tronChannelInitializer");
      tronChannelInitializer.prepare();
      Channel channel = ReflectUtils.getFieldValue(tronChannelInitializer, "channel");
      ReflectUtils.setFieldValue(channel, "handshakeHandler", handshakeHandlerTest);

      new Thread(new Runnable() {
        @Override
        public void run() {
          peerClient.connect(nodeEntity.getHost(), nodeEntity.getPort(), nodeEntity.getHexId());
        }
      }).start();
      Thread.sleep(1000);
      Map<ByteArrayWrapper, Channel> activePeers = ReflectUtils
          .getFieldValue(channelManager, "activePeers");
      int tryTimes = 0;
      while (MapUtils.isEmpty(activePeers) && ++tryTimes < 10) {
        Thread.sleep(1000);
      }
      go = true;
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @AfterClass
  public static void destroy() {
    Args.clearParam();
    peerClient.close();
    handshakeHandlerTest.close();
    context.destroy();
    appT.shutdownServices();
    appT.shutdown();
    FileUtil.deleteDir(new File(dbPath));
  }
}
