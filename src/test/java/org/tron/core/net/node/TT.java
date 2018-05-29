package org.tron.core.net.node;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.testng.collections.Lists;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.overlay.discover.Node;
import org.tron.common.overlay.discover.NodeManager;
import org.tron.common.overlay.discover.RefreshTask;
import org.tron.common.overlay.discover.message.*;
import org.tron.common.overlay.server.ChannelManager;
import org.tron.core.Constant;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.services.RpcApiService;
import org.tron.core.services.WitnessService;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class TT {

    ApplicationContext context;

    @Before
    public void before(){

        // start full node
        new Thread(()->{
            logger.info("Full node running.");
            Args.setParam(new String[0], Constant.TESTNET_CONF);
            Args cfgArgs = Args.getInstance();
            cfgArgs.getSeedNode().setIpList(Lists.newArrayList());
            cfgArgs.setNodeP2pVersion(100);
            cfgArgs.setNodeListenPort(10001);

            context = new AnnotationConfigApplicationContext(DefaultConfig.class);
            Application appT = ApplicationFactory.create(context);
            Runtime.getRuntime().addShutdownHook(new Thread(appT::shutdown));

            RpcApiService rpcApiService = context.getBean(RpcApiService.class);
            appT.addService(rpcApiService);
            if (cfgArgs.isWitness()) {
                appT.addService(new WitnessService(appT));
            }
            appT.initServices(cfgArgs);
            appT.startServices();
            appT.startup();
            rpcApiService.blockUntilShutdown();
        }).start();
    }

    @Test
    public void test() throws Exception{
        try{
            Thread.sleep(10000);

            InetAddress server = InetAddress.getByName("127.0.0.1");

            Node from = Node.instanceOf("127.0.0.1:10002");
            Node peer1 = Node.instanceOf("127.0.0.1:10003");
            Node peer2 = Node.instanceOf("127.0.0.1:10004");

            NodeManager nodeManager = context.getBean(NodeManager.class);

            nodeManager.hasNodeHandler(from);

            System.out.println(nodeManager.hasNodeHandler(peer1));
            System.out.println(nodeManager.hasNodeHandler(peer2));
            System.out.println(nodeManager.nodeHandlerMap.size());
            System.out.println(nodeManager.getTable().getAllNodes().size());
            Assert.assertTrue(!nodeManager.hasNodeHandler(peer1));
            Assert.assertTrue(!nodeManager.hasNodeHandler(peer2));
            Assert.assertTrue(nodeManager.nodeHandlerMap.size() == 0);
            System.out.println(nodeManager.getTable().getAllNodes().size());

            PingMessage pingMessage = new PingMessage(from, nodeManager.getPublicHomeNode());

            PongMessage pongMessage = new PongMessage(from);

            FindNodeMessage findNodeMessage = new FindNodeMessage(from, RefreshTask.getNodeId());

            List<Node> peers = Lists.newArrayList(peer1, peer2);
            NeighborsMessage neighborsMessage = new NeighborsMessage(from, peers);

            DatagramSocket socket = new DatagramSocket();

            DatagramPacket pingPacket = new DatagramPacket(pingMessage.getSendData(),
                    pingMessage.getSendData().length, server, 10001);

            DatagramPacket pongPacket = new DatagramPacket(pongMessage.getSendData(),
                    pongMessage.getSendData().length, server, 10001);

            DatagramPacket findNodePacket = new DatagramPacket(findNodeMessage.getSendData(),
                    findNodeMessage.getSendData().length, server, 10001);

            DatagramPacket neighborsPacket = new DatagramPacket(neighborsMessage.getSendData(),
                    neighborsMessage.getSendData().length, server, 10001);

            // send ping msg
            socket.send(pingPacket);
            byte[] data = new byte[1024];
            DatagramPacket packet = new DatagramPacket(data, data.length);

            boolean pingFlag = false;
            boolean pongFlag = false;
            boolean findNodeFlag = false;
            boolean neighborsFlag = false;
            while (true){
                socket.receive(packet);
                byte[] bytes = Arrays.copyOfRange(data, 0, packet.getLength());
                Message msg = Message.parse(bytes);
                System.out.println("============================");
                System.out.println(msg);
                System.out.println(msg.getNodeId().length);
                Assert.assertTrue(Arrays.equals(msg.getNodeId(), nodeManager.getPublicHomeNode().getId()));
                if (!pingFlag){
                    pingFlag = true;
                    Assert.assertTrue(msg instanceof PingMessage);
                    socket.send(pongPacket);
                }else if (!pongFlag){
                    pongFlag = true;
                    Assert.assertTrue(msg instanceof PongMessage);
                }else if (!findNodeFlag){
                    findNodeFlag = true;
                    Assert.assertTrue(msg instanceof FindNodeMessage);
                    socket.send(neighborsPacket);
                    socket.send(findNodePacket);
                }else if (!neighborsFlag){
                    Assert.assertTrue(msg instanceof NeighborsMessage);
                    break;
                }
            }
            System.out.println(nodeManager.hasNodeHandler(peer1));
            System.out.println(nodeManager.hasNodeHandler(peer2));
            System.out.println(nodeManager.nodeHandlerMap.size());
            System.out.println(nodeManager.getTable().getAllNodes().size());
            System.out.println("finish UDP test.");

            socket.close();
        }catch (Exception e){
            System.out.println(e);
            e.printStackTrace();
        }
    }
}
