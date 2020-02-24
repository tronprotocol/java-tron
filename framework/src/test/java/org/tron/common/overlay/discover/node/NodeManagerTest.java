package org.tron.common.overlay.discover.node;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.FileUtil;
import org.tron.core.ChainBaseManager;
import org.tron.core.Constant;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;


public class NodeManagerTest {

  private static final Logger logger = LoggerFactory.getLogger("Test");
  private Manager manager;
  private NodeManager nodeManager;
  private TronApplicationContext context;
  private CommonParameter argsTest;
  private Application appTest;
  private Class nodeManagerClazz;


  /**
   * start the application.
   */
  @Before
  public void init() {
    argsTest = Args.getInstance();
    Args.setParam(new String[]{"--output-directory", "output-directory", "--debug"},
        Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    appTest = ApplicationFactory.create(context);
    appTest.initServices(argsTest);
    appTest.startServices();
    appTest.startup();
  }

  /**
   * destroy the context.
   */
  @After
  public void destroy() {
    Args.clearParam();
    context.destroy();
    if (FileUtil.deleteDir(new File("output-directory"))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
  }

  /**
   * init the managers.
   */
  @Before
  public void initManager() throws Exception {
    nodeManagerClazz = NodeManager.class;
    Constructor<NodeManager> handlerConstructor
        = nodeManagerClazz.getConstructor(ChainBaseManager.class);
    manager = context.getBean(Manager.class);
    nodeManager = handlerConstructor.newInstance(context.getBean(ChainBaseManager.class));
  }

  @Test
  public void isNodeAliveTest() {
    Node node = new Node(new byte[64], "128.0.0.1", 18888, 18888);
    nodeManager.getTable().addNode(node);
    NodeHandler nodeHandler = new NodeHandler(node, nodeManager);
    nodeHandler.changeState(NodeHandler.State.ACTIVE);
    Assert.assertTrue(nodeManager.isNodeAlive(nodeHandler));
    nodeHandler.changeState(NodeHandler.State.ALIVE);
    Assert.assertTrue(nodeManager.isNodeAlive(nodeHandler));
    nodeHandler.changeState(NodeHandler.State.EVICTCANDIDATE);
    Assert.assertTrue(nodeManager.isNodeAlive(nodeHandler));
  }

  @Test
  public void trimTableTest_removeByReputation() throws Exception {
    //insert 3001 nodes(isConnectible = true) with threshold = 3000
    final int totalNodes = insertValues(3002);
    Assert.assertEquals(calculateTrimNodes(totalNodes, 0), getHandlerMapSize());
  }

  @Test
  public void trimTableTest_removeNotConnectibleNodes() throws Exception {
    final int totalNodes = insertValues(3000);
    insertNotConnectibleNodes();
    Method method = nodeManagerClazz.getDeclaredMethod("trimTable");
    method.setAccessible(true);
    method.invoke(nodeManager);
    Assert.assertEquals(calculateTrimNodes(totalNodes, 2), getHandlerMapSize());
  }

  /**
   * calculate nodes number after table trim.
   *
   * @param totalNodes total nodes inserted
   * @param wrongNodes isConnectable = false
   * @return nodes count after trimTable()
   */
  public int calculateTrimNodes(int totalNodes, int wrongNodes) {
    if (totalNodes + wrongNodes > 3000) {
      if (totalNodes <= 3000) {
        return totalNodes;
      } else {
        int result = 2000 + ((totalNodes + wrongNodes) % 2000 - 1001);
        return result;
      }
    }
    return totalNodes + wrongNodes;
  }

  /**
   * insert valid nodes in map.
   *
   * @param totalNodes total nodes to be inserted.
   * @return total nodes inserted.
   */
  public int insertValues(int totalNodes) throws Exception {
    //put 3001 nodes in nodeHandlerMap
    int ipPart3 = 1;
    int ipPart4 = 1;
    for (int i = 0; i < totalNodes; i++) {
      StringBuilder stringBuilder = new StringBuilder("128.0.");
      byte[] bytes = new byte[64];
      bytes[0] = (byte) (i + 1);
      stringBuilder.append(ipPart3);
      stringBuilder.append(".");
      stringBuilder.append(ipPart4);
      ipPart4++;
      if (ipPart4 == 256) {
        ipPart3++;
        ipPart4 = 1;
      }
      Class nodeClazz = Node.class;
      Constructor<Node> nodeConstructor
          = nodeClazz.getConstructor(byte[].class, String.class, int.class, int.class);
      Node node = nodeConstructor.newInstance(bytes, stringBuilder.toString(), 18888, 18888);
      Field isConnectableField = nodeClazz.getDeclaredField("p2pVersion");
      isConnectableField.setAccessible(true);
      isConnectableField.set(node, Args.getInstance().getNodeP2pVersion());
      nodeManager.getNodeHandler(node);
    }
    return totalNodes;
  }

  /**
   * insert nodes with illegal p2p version.
   */
  public void insertNotConnectibleNodes() throws Exception {
    Class nodeClazz = Node.class;
    Constructor<Node> nodeConstructor
        = nodeClazz.getConstructor(byte[].class, String.class, int.class, int.class);
    Node wrongNode1 = nodeConstructor.newInstance(new byte[64], "128.0.0.1", 1111, 18888);
    byte[] id = new byte[64];
    id[63] = 1;
    Node wrongNode2 = nodeConstructor.newInstance(id, "128.0.0.2", 1111, 18888);
    Field isConnectableField = nodeClazz.getDeclaredField("p2pVersion");
    isConnectableField.setAccessible(true);
    isConnectableField.set(wrongNode1, 999);
    isConnectableField.set(wrongNode2, 999);
    nodeManager.getNodeHandler(wrongNode1);
    nodeManager.getNodeHandler(wrongNode2);
  }


  /**
   * get the size of nodeHandlerMap.
   *
   * @return NodeManager.nodeHandlerMap
   */
  public int getHandlerMapSize() throws Exception {
    Field mapField = nodeManagerClazz.getDeclaredField("nodeHandlerMap");
    mapField.setAccessible(true);
    Map<String, NodeHandler> nodeHandlerMap = (ConcurrentHashMap) mapField.get(nodeManager);
    return nodeHandlerMap.size();
  }

  @Test
  public void dumpActiveNodesTest() {
    Node node1 = new Node(new byte[64], "128.0.0.1", 18888, 18888);
    Node node2 = new Node(new byte[64], "128.0.0.2", 18888, 18888);
    Node node3 = new Node(new byte[64], "128.0.0.3", 18888, 18888);
    NodeHandler nodeHandler1 = nodeManager.getNodeHandler(node1);
    NodeHandler nodeHandler2 = nodeManager.getNodeHandler(node2);
    NodeHandler nodeHandler3 = nodeManager.getNodeHandler(node3);
    nodeHandler1.changeState(NodeHandler.State.ALIVE);
    nodeHandler2.changeState(NodeHandler.State.ACTIVE);
    nodeHandler3.changeState(NodeHandler.State.NONACTIVE);
    int activeNodes = nodeManager.dumpActiveNodes().size();
    Assert.assertEquals(2, activeNodes);
  }
}
