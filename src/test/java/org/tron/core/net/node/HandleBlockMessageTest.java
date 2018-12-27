package org.tron.core.net.node;

import com.google.protobuf.ByteString;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.crypto.ECKey;
import org.tron.common.overlay.discover.node.Node;
import org.tron.common.overlay.server.Channel;
import org.tron.common.overlay.server.ChannelManager;
import org.tron.common.overlay.server.SyncPool;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.ReflectUtils;
import org.tron.core.Constant;
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
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.BlockHeader;
import org.tron.protos.Protocol.Inventory.InventoryType;

@Slf4j
public class HandleBlockMessageTest {

  private static TronApplicationContext context;
  private static NodeImpl node;
  private RpcApiService rpcApiService;
  private static PeerClientTest peerClient;
  private ChannelManager channelManager;
  private SyncPool pool;
  private static Application appT;
  private Manager dbManager;
  private Node nodeEntity;
  private static HandshakeHandlerTest handshakeHandlerTest;

  private static final String dbPath = "output-HandleBlockMessageTest";
  private static final String dbDirectory = "db_HandleBlockMessage_test";
  private static final String indexDirectory = "index_HandleBlockMessage_test";

  @Test
  public void testHandleBlockMessage() throws Exception {
    List<PeerConnection> activePeers = ReflectUtils.getFieldValue(pool, "activePeers");
    Thread.sleep(5000);
    if (activePeers.size() < 1) {
      return;
    }
    PeerConnection peer = activePeers.get(0);

    //receive a sync block
    BlockCapsule headBlockCapsule = dbManager.getHead();
    BlockCapsule syncblockCapsule = generateOneBlockCapsule(headBlockCapsule);
    BlockMessage blockMessage = new BlockMessage(syncblockCapsule);
    peer.getSyncBlockRequested().put(blockMessage.getBlockId(), System.currentTimeMillis());
    node.onMessage(peer, blockMessage);
    Assert.assertEquals(peer.getSyncBlockRequested().isEmpty(), true);

    //receive a advertise block
    BlockCapsule advblockCapsule = generateOneBlockCapsule(headBlockCapsule);
    BlockMessage advblockMessage = new BlockMessage(advblockCapsule);
    peer.getAdvObjWeRequested().put(new Item(advblockMessage.getBlockId(), InventoryType.BLOCK),
        System.currentTimeMillis());
    node.onMessage(peer, advblockMessage);
    Assert.assertEquals(peer.getAdvObjWeRequested().size(), 0);

    //receive a sync block but not requested
    BlockCapsule blockCapsule = generateOneBlockCapsule(headBlockCapsule);
    blockMessage = new BlockMessage(blockCapsule);
    BlockCapsule blockCapsuleOther = generateOneBlockCapsule(blockCapsule);
    BlockMessage blockMessageOther = new BlockMessage(blockCapsuleOther);

    peer.getSyncBlockRequested().put(blockMessage.getBlockId(), System.currentTimeMillis());
    node.onMessage(peer, blockMessageOther);
    Assert.assertEquals(peer.getSyncBlockRequested().isEmpty(), false);
  }

  // generate ong block by parent block
  private BlockCapsule generateOneBlockCapsule(BlockCapsule parentCapsule) {
    ByteString witnessAddress = ByteString.copyFrom(
        ECKey.fromPrivate(
            ByteArray.fromHexString(
                Args.getInstance().getLocalWitnesses().getPrivateKey()))
            .getAddress());
    BlockHeader.raw raw = BlockHeader.raw.newBuilder()
        .setTimestamp(System.currentTimeMillis())
        .setParentHash(parentCapsule.getBlockId().getByteString())
        .setNumber(parentCapsule.getNum() + 1)
        .setWitnessAddress(witnessAddress)
        .setWitnessId(1).build();
    BlockHeader blockHeader = BlockHeader.newBuilder()
        .setRawData(raw)
        .build();

    Block block = Block.newBuilder().setBlockHeader(blockHeader).build();

    BlockCapsule blockCapsule = new BlockCapsule(block);
    blockCapsule.setMerkleRoot();
    blockCapsule.sign(
        ByteArray.fromHexString(Args.getInstance().getLocalWitnesses().getPrivateKey()));
    blockCapsule.setMerkleRoot();
    blockCapsule.sign(
        ByteArray.fromHexString(Args.getInstance().getLocalWitnesses().getPrivateKey()));

    return blockCapsule;
  }

  private static boolean go = false;

  @Before
  public void init() {
    nodeEntity = new Node(
        "enode://e437a4836b77ad9d9ffe73ee782ef2614e6d8370fcf62191a6e488276e23717147073a7ce0b444d485fff5a0c34c4577251a7a990cf80d8542e21b95aa8c5e6c@127.0.0.1:17894");

    new Thread(new Runnable() {
      @Override
      public void run() {
        logger.info("Full node running.");
        Args.setParam(
            new String[]{
                "--output-directory", dbPath,
                "--storage-db-directory", dbDirectory,
                "--storage-index-directory", indexDirectory
            },
            Constant.TEST_CONF
        );
        Args cfgArgs = Args.getInstance();
        cfgArgs.setNodeListenPort(17894);
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
        dbManager = context.getBean(Manager.class);
        handshakeHandlerTest = context.getBean(HandshakeHandlerTest.class);
        handshakeHandlerTest.setNode(nodeEntity);
        NodeDelegate nodeDelegate = new NodeDelegateImpl(dbManager);
        node.setNodeDelegate(nodeDelegate);
        pool.init(node);
        prepare();
        rpcApiService.blockUntilShutdown();
      }
    }).start();
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
    Collection<PeerConnection> peerConnections = ReflectUtils.invokeMethod(node, "getActivePeer");
    for (PeerConnection peer : peerConnections) {
      peer.close();
    }
    handshakeHandlerTest.close();
    appT.shutdownServices();
    appT.shutdown();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }
}
