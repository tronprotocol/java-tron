package org.tron.core.net.node;

import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.junit.*;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.crypto.ECKey;
import org.tron.common.overlay.client.PeerClient;
import org.tron.common.overlay.discover.node.Node;
import org.tron.common.overlay.server.Channel;
import org.tron.common.overlay.server.ChannelManager;
import org.tron.common.overlay.server.SyncPool;
import org.tron.common.utils.*;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.BlockStore;
import org.tron.core.db.ByteArrayWrapper;
import org.tron.core.db.Manager;
import org.tron.core.net.node.override.HandshakeHandlerTest;
import org.tron.core.net.node.override.PeerClientTest;
import org.tron.core.net.node.override.TronChannelInitializerTest;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.services.RpcApiService;
import org.tron.core.services.WitnessService;
import org.tron.core.witness.WitnessController;
import org.tron.protos.Protocol;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class GetBlockChainSummaryTest{
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

    private static final String dbPath = "output-GetBlockChainSummary";
    private static final String dbDirectory = "db_GetBlockChainSummary_test";
    private static final String indexDirectory = "index_GetBlockChainSummary_test";

    @Test
    public void testGetBlockChainSummary() {
        NodeDelegate del = ReflectUtils.getFieldValue(node, "del");
        Collection<PeerConnection> activePeers = ReflectUtils.invokeMethod(node, "getActivePeer");
        BlockStore blkstore = dbManager.getBlockStore();

        Object[] peers = activePeers.toArray();
        PeerConnection peer_he = (PeerConnection) peers[1];
        Deque<BlockCapsule.BlockId> toFetch = new ConcurrentLinkedDeque<>();

        ArrayList<String> scenes = new ArrayList<>();
        scenes.add("genesis");
        scenes.add("genesis_fetch");
        scenes.add("nongenesis_fetch");

        long number = dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber() + 1;
        Map<ByteString, String> addressToProvateKeys = addTestWitnessAndAccount();
        BlockCapsule capsule = createTestBlockCapsule(1533529947843L,number, dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash().getByteString(),
            addressToProvateKeys);
        try {
            dbManager.pushBlock(capsule);
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (int i = 1; i < 5; i++) {
            number = dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber() + 1;
            capsule = createTestBlockCapsule(1533529947843L + 3000L * i,number, dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash().getByteString(), addressToProvateKeys);
            try {
                dbManager.pushBlock(capsule);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        BlockCapsule.BlockId commonBlockId = null;

        //the common block is genesisblock，syncBlockToFetch is empty。
        try {
            commonBlockId = del.getGenesisBlock().getBlockId();
            peer_he.getSyncBlockToFetch().clear();
            ReflectUtils.setFieldValue(peer_he, "headBlockWeBothHave", commonBlockId);
            ReflectUtils.setFieldValue(peer_he, "headBlockTimeWeBothHave", System.currentTimeMillis());
            Deque<BlockCapsule.BlockId> retSummary = del.getBlockChainSummary(peer_he.getHeadBlockWeBothHave(), peer_he.getSyncBlockToFetch());
            Assert.assertTrue(retSummary.size() == 3);
        } catch (Exception e) {
            System.out.println("exception!");
        }

        //the common block is genesisblock，syncBlockToFetch is not empty。
        peer_he.getSyncBlockToFetch().addAll(toFetch);
        try {
            toFetch.clear();
            peer_he.getSyncBlockToFetch().clear();
            for(int i=0; i<4 ;i++){
                number = dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber() + 1;
                capsule = createTestBlockCapsule(1533529947843L + 3000L * i, number, dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash().getByteString(),
                        addressToProvateKeys);
                toFetch.add(capsule.getBlockId());
            }

            commonBlockId = del.getGenesisBlock().getBlockId();
            peer_he.getSyncBlockToFetch().addAll(toFetch);
            ReflectUtils.setFieldValue(peer_he, "headBlockWeBothHave", commonBlockId);
            ReflectUtils.setFieldValue(peer_he, "headBlockTimeWeBothHave", System.currentTimeMillis());
            Deque<BlockCapsule.BlockId> retSummary = del.getBlockChainSummary(peer_he.getHeadBlockWeBothHave(), peer_he.getSyncBlockToFetch());
            Assert.assertTrue(retSummary.size() == 4);
        } catch (Exception e) {
            System.out.println("exception!");
        }

        //the common block is a normal block(not genesisblock)，syncBlockToFetc is not empty.
        try {
            toFetch.clear();
            peer_he.getSyncBlockToFetch().clear();
            number = dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber() + 1;
            BlockCapsule capsule1 = createTestBlockCapsule(1533529947843L + 3000L * 6, number, dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash().getByteString(),
                    addressToProvateKeys);
            dbManager.pushBlock(capsule1);

            number = dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber() + 1;
            BlockCapsule capsule2 = createTestBlockCapsule(1533529947843L + 3000L * 7, number, dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash().getByteString(),
                    addressToProvateKeys);
            dbManager.pushBlock(capsule2);

            for(int i=0; i<2; i++){
                number = dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber() + 1;
                capsule = createTestBlockCapsule(1533529947843L + 3000L * 8 + 3000L * i, number, dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash().getByteString(),
                        addressToProvateKeys);
                toFetch.add(capsule.getBlockId());
            }
            commonBlockId = capsule2.getBlockId();
            peer_he.getSyncBlockToFetch().addAll(toFetch);
            toFetch.forEach(block -> blkstore.delete(block.getBytes()));
            ReflectUtils.setFieldValue(peer_he, "headBlockWeBothHave", commonBlockId);
            ReflectUtils.setFieldValue(peer_he, "headBlockTimeWeBothHave", System.currentTimeMillis());
            Deque<BlockCapsule.BlockId> retSummary = del.getBlockChainSummary(peer_he.getHeadBlockWeBothHave(), peer_he.getSyncBlockToFetch());
            Assert.assertTrue(retSummary.size() == 4);
        } catch (Exception e) {
            System.out.println("exception!");
        }
        logger.info("finish1");
    }


    private static boolean go = false;
    private Map<ByteString, String> addTestWitnessAndAccount() {
        dbManager.getWitnesses().clear();
        return IntStream.range(0, 2)
                .mapToObj(
                        i -> {
                            ECKey ecKey = new ECKey(Utils.getRandom());
                            String privateKey = ByteArray.toHexString(ecKey.getPrivKey().toByteArray());
                            ByteString address = ByteString.copyFrom(ecKey.getAddress());

                            WitnessCapsule witnessCapsule = new WitnessCapsule(address);
                            dbManager.getWitnessStore().put(address.toByteArray(), witnessCapsule);
                            dbManager.getWitnessController().addWitness(address);

                            AccountCapsule accountCapsule =
                                    new AccountCapsule(Protocol.Account.newBuilder().setAddress(address).build());
                            dbManager.getAccountStore().put(address.toByteArray(), accountCapsule);

                            return Maps.immutableEntry(address, privateKey);
                        })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
    private BlockCapsule createTestBlockCapsule(
            long number, ByteString hash, Map<ByteString, String> addressToProvateKeys) {
        long time = System.currentTimeMillis();
        return createTestBlockCapsule(time,number,hash,addressToProvateKeys);
    }

    private BlockCapsule createTestBlockCapsule(long time ,
        long number, ByteString hash, Map<ByteString, String> addressToProvateKeys) {
        WitnessController witnessController = dbManager.getWitnessController();
        ByteString witnessAddress =
                witnessController.getScheduledWitness(witnessController.getSlotAtTime(time));
        BlockCapsule blockCapsule = new BlockCapsule(number, Sha256Hash.wrap(hash), time, witnessAddress);
        blockCapsule.generatedByMyself = true;
        blockCapsule.setMerkleRoot();
        blockCapsule.sign(ByteArray.fromHexString(addressToProvateKeys.get(witnessAddress)));
        return blockCapsule;
    }

    @Before
    public void init() {
        nodeEntity = new Node(
            "enode://e437a4836b77ad9d9ffe73ee782ef2614e6d8370fcf62191a6e488276e23717147073a7ce0b444d485fff5a0c34c4577251a7a990cf80d8542e21b95aa8c5e6c@127.0.0.1:17896");

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
                cfgArgs.setNodeListenPort(17896);
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
            Map<ByteArrayWrapper, io.grpc.Channel> activePeers = ReflectUtils
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
        peerClient.close();
        handshakeHandlerTest.close();
        appT.shutdownServices();
        appT.shutdown();
        context.destroy();
        FileUtil.deleteDir(new File(dbPath));
    }
}
