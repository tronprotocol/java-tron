package org.tron.p2p.discover.protocol.kad;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.discover.Node;
import org.tron.p2p.discover.message.kad.PingMessage;
import org.tron.p2p.discover.message.kad.PongMessage;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;

public class NodeHandlerTest {

  private static KadService kadService;
  private static Node currNode;
  private static Node oldNode;
  private static Node replaceNode;
  private static NodeHandler currHandler;
  private static NodeHandler oldHandler;
  private static NodeHandler replaceHandler;

  @BeforeClass
  public static void init() {
    Parameter.p2pConfig = new P2pConfig();
    Parameter.p2pConfig.setDiscoverEnable(false);
    kadService = new KadService();
    kadService.init();
    KadService.setPingTimeout(300);
    currNode = new Node(new InetSocketAddress("127.0.0.1", 22222));
    oldNode = new Node(new InetSocketAddress("127.0.0.2", 22222));
    replaceNode = new Node(new InetSocketAddress("127.0.0.3", 22222));
    currHandler = new NodeHandler(currNode, kadService);
    oldHandler = new NodeHandler(oldNode, kadService);
    replaceHandler = new NodeHandler(replaceNode, kadService);
  }

  @Test
  public void test() throws InterruptedException {
    Assert.assertEquals(NodeHandler.State.DISCOVERED, currHandler.getState());
    Assert.assertEquals(NodeHandler.State.DISCOVERED, oldHandler.getState());
    Assert.assertEquals(NodeHandler.State.DISCOVERED, replaceHandler.getState());
    Thread.sleep(2000);
    Assert.assertEquals(NodeHandler.State.DEAD, currHandler.getState());
    Assert.assertEquals(NodeHandler.State.DEAD, oldHandler.getState());
    Assert.assertEquals(NodeHandler.State.DEAD, replaceHandler.getState());

    PingMessage msg = new PingMessage(currNode, kadService.getPublicHomeNode());
    currHandler.handlePing(msg);
    Assert.assertEquals(NodeHandler.State.DISCOVERED, currHandler.getState());
    PongMessage msg1 = new PongMessage(currNode);
    currHandler.handlePong(msg1);
    Assert.assertEquals(NodeHandler.State.ACTIVE, currHandler.getState());
    Assert.assertTrue(kadService.getTable().contains(currNode));
    kadService.getTable().dropNode(currNode);
  }

  @Test
  public void testChangeState() throws Exception {
    currHandler.changeState(NodeHandler.State.ALIVE);
    Assert.assertEquals(NodeHandler.State.ACTIVE, currHandler.getState());
    Assert.assertTrue(kadService.getTable().contains(currNode));

    Class<NodeHandler> clazz = NodeHandler.class;
    Constructor<NodeHandler> cn = clazz.getDeclaredConstructor(Node.class, KadService.class);
    NodeHandler nh = cn.newInstance(oldNode, kadService);
    Field declaredField = clazz.getDeclaredField("replaceCandidate");
    declaredField.setAccessible(true);
    declaredField.set(nh, replaceHandler);

    kadService.getTable().addNode(oldNode);
    nh.changeState(NodeHandler.State.EVICTCANDIDATE);
    nh.changeState(NodeHandler.State.DEAD);
    replaceHandler.changeState(NodeHandler.State.ALIVE);

    Assert.assertFalse(kadService.getTable().contains(oldNode));
    Assert.assertTrue(kadService.getTable().contains(replaceNode));
  }

  @AfterClass
  public static void destroy() {
    kadService.close();
  }
}
