package org.tron.core.net;

import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.tron.common.application.TronApplicationContext;
import org.testng.collections.Lists;
import org.tron.common.net.udp.message.discover.FindNodeMessage;
import org.tron.common.net.udp.message.Message;
import org.tron.common.net.udp.message.discover.NeighborsMessage;
import org.tron.common.net.udp.message.discover.PingMessage;
import org.tron.common.net.udp.message.discover.PongMessage;
import org.tron.common.overlay.discover.node.Node;
import org.tron.common.overlay.discover.node.NodeManager;
import org.tron.common.overlay.discover.RefreshTask;
import org.tron.common.utils.FileUtil;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class UdpTest {

  ApplicationContext context;

  NodeManager nodeManager;

  //@Before
  public void before(){
    new Thread(() -> {
      Args.setParam(
          new String[]{ "--output-directory", "udp_test", "--storage-db-directory", "database",
              "--storage-index-directory", "index"},"config.conf"
      );
      Args cfgArgs = Args.getInstance();
      cfgArgs.getSeedNode().setIpList(Lists.newArrayList());
      cfgArgs.setNodeP2pVersion(100);
      cfgArgs.setNodeListenPort(10001);
      context = new TronApplicationContext(DefaultConfig.class);
    }).start();
  }

  @Test
  public void test() {}

  //@Test
  public void udpTest() throws Exception {

    Thread.sleep(10000);

    nodeManager = context.getBean(NodeManager.class);

    InetAddress server = InetAddress.getByName("127.0.0.1");

    Node from = Node.instanceOf("127.0.0.1:10002");
    Node peer1 = Node.instanceOf("127.0.0.1:10003");
    Node peer2 = Node.instanceOf("127.0.0.1:10004");

    Assert.assertTrue(!nodeManager.hasNodeHandler(peer1));
    Assert.assertTrue(!nodeManager.hasNodeHandler(peer2));
    Assert.assertTrue(nodeManager.getTable().getAllNodes().isEmpty());

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
    while (true) {
      socket.receive(packet);
      byte[] bytes = Arrays.copyOfRange(data, 0, packet.getLength());
      Message msg = Message.parse(bytes);
      Assert.assertTrue(Arrays.equals(msg.getFrom().getId(), nodeManager.getPublicHomeNode().getId()));
      if (!pingFlag) {
        pingFlag = true;
        Assert.assertTrue(msg instanceof PingMessage);
        Assert.assertTrue(Arrays.equals(((PingMessage) msg).getTo().getId(), from.getId()));
        socket.send(pongPacket);
      } else if (!pongFlag) {
        pongFlag = true;
        Assert.assertTrue(msg instanceof PongMessage);
      } else if (!findNodeFlag) {
        findNodeFlag = true;
        Assert.assertTrue(msg instanceof FindNodeMessage);
        socket.send(neighborsPacket);
        socket.send(findNodePacket);
      } else if (!neighborsFlag) {
        Assert.assertTrue(msg instanceof NeighborsMessage);
        break;
      }
    }

    Assert.assertTrue(nodeManager.hasNodeHandler(peer1));
    Assert.assertTrue(nodeManager.hasNodeHandler(peer2));
    Assert.assertTrue(nodeManager.getTable().getAllNodes().size() == 1);

    socket.close();
  }

  //@After
  public void after() {
    FileUtil.deleteDir(new File("udp_test"));
  }
}

