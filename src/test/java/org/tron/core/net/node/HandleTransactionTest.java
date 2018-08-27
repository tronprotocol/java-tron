package org.tron.core.net.node;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.overlay.client.PeerClient;
import org.tron.common.overlay.discover.node.Node;
import org.tron.common.overlay.server.Channel;
import org.tron.common.overlay.server.ChannelManager;
import org.tron.common.overlay.server.SyncPool;
import org.tron.common.utils.ReflectUtils;
import org.tron.core.Constant;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.ByteArrayWrapper;
import org.tron.core.db.Manager;
import org.tron.core.exception.TraitorPeerException;
import org.tron.core.net.message.TransactionMessage;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.services.RpcApiService;
import org.tron.core.services.WitnessService;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Inventory.InventoryType;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ExecutorService;


@Slf4j
public class HandleTransactionTest {

    private static TronApplicationContext context;
    private NodeImpl node;
    RpcApiService rpcApiService;
    PeerClient peerClient;
    ChannelManager channelManager;
    SyncPool pool;
    Application appT;
    Manager dbManager;

    private static final String dbPath = "output-HandleTransactionTest";
    private static final String dbDirectory = "db_HandleTransaction_test";
    private static final String indexDirectory = "index_HandleTransaction_test";

    private static Boolean deleteFolder(File index) {
        if (!index.isDirectory() || index.listFiles().length <= 0) {
            return index.delete();
        }
        for (File file : index.listFiles()) {
            if (null != file && !deleteFolder(file)) {
                return false;
            }
        }
        return index.delete();
    }

    @Test
    public void testHandleTransactionMessage() throws TraitorPeerException {
        PeerConnection peer = new PeerConnection();
        Protocol.Transaction transaction = Protocol.Transaction.getDefaultInstance();
        TransactionMessage transactionMessage = new TransactionMessage(transaction);

        //没有向peer广播请求过交易信息
        peer.getAdvObjWeRequested().clear();
        peer.setSyncFlag(true);
        //nodeImpl.onMessage(peer, transactionMessage);
        //Assert.assertEquals(peer.getSyncFlag(), false);

        //向peer广播请求过交易信息
        peer.getAdvObjWeRequested().put(new Item(transactionMessage.getMessageId(), InventoryType.TRX), System.currentTimeMillis());
        peer.setSyncFlag(true);
        node.onMessage(peer, transactionMessage);
        //Assert.assertEquals(peer.getAdvObjWeRequested().isEmpty(), true);
        //ConcurrentHashMap<Sha256Hash, InventoryType> advObjToSpread = ReflectUtils.getFieldValue(nodeImpl, "advObjToSpread");
        //Assert.assertEquals(advObjToSpread.contains(transactionMessage.getMessageId()), true);
    }

    private static boolean go = false;

    @Before
    public void init() {
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
                cfgArgs.setNodeListenPort(17891);
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
                peerClient = context.getBean(PeerClient.class);
                channelManager = context.getBean(ChannelManager.class);
                pool = context.getBean(SyncPool.class);
                dbManager = context.getBean(Manager.class);
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

            ReflectUtils.setFieldValue(node, "isAdvertiseActive", false);
            ReflectUtils.setFieldValue(node, "isFetchActive", false);

            Node node = new Node(
                    "enode://e437a4836b77ad9d9ffe73ee782ef2614e6d8370fcf62191a6e488276e23717147073a7ce0b444d485fff5a0c34c4577251a7a990cf80d8542e21b95aa8c5e6c@127.0.0.1:17891");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    peerClient.connect(node.getHost(), node.getPort(), node.getHexId());
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

    @After
    public void removeDb() {
        Args.clearParam();

        File dbFolder = new File(dbPath);
        if (deleteFolder(dbFolder)) {
            logger.info("Release resources successful.");
        } else {
            logger.info("Release resources failure.");
        }
        context.destroy();
        dbManager.getSession().reset();
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
