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
  private Class clazz;

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
    appTest.shutdownServices();
    appTest.shutdown();
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
    clazz = NodeManager.class;
    Constructor<NodeManager> constructor = clazz.getConstructor(Manager.class);
    manager = context.getBean(Manager.class);
    nodeManager = constructor.newInstance(manager);
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
  public void trimTableTest() throws Exception {
    //put 3001 nodes in nodeHandlerMap
    int ipPart3 = 0;
    int ipPart4 = 1;
    for (int i = 0; i < 3002; i++) {
      StringBuilder stringBuilder = new StringBuilder("128.0.");
      byte[] bytes = new byte[64];
      bytes[0] = (byte) i;
      stringBuilder.append(ipPart3);
      stringBuilder.append(".");
      stringBuilder.append(ipPart4);
      ipPart4++;
      if (ipPart4 == 256) {
        ipPart3++;
        ipPart4 = 1;
      }
      Node node = new Node(bytes, stringBuilder.toString(), 18888, 18888);
      nodeManager.getNodeHandler(node);
    }
    Field declaredField = clazz.getDeclaredField("nodeHandlerMap");
    declaredField.setAccessible(true);
    Method method = clazz.getDeclaredMethod("trimTable");
    method.setAccessible(true);
    method.invoke(nodeManager);
    Map<String,NodeHandler> nodeHandlerMap = (ConcurrentHashMap)declaredField.get(nodeManager);
    Assert.assertEquals(2001, nodeHandlerMap.size());
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
