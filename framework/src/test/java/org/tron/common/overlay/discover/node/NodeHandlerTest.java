package org.tron.common.overlay.discover.node;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.FileUtil;
import org.tron.core.ChainBaseManager;
import org.tron.core.Constant;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;


public class NodeHandlerTest {

  private static final Logger logger = LoggerFactory.getLogger("Test");
  // private static Manager dbManager;
  private static TronApplicationContext context;
  // private Application appTest;
  // private CommonParameter argsTest;
  private static Node currNode;
  private static Node oldNode;
  private static Node replaceNode;
  private static NodeHandler currHandler;
  private static NodeHandler oldHandler;
  private static NodeHandler replaceHandler;
  private static NodeManager nodeManager;
  private static String dbPath = "NodeHandlerTest";

  static {
    Args.setParam(new String[]{"-d", dbPath}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
  }

  /**
   * init the application.
   */
  @BeforeClass
  public static void init() {
    initNodes();
  }

  /**
   * destroy the context.
   */
  @AfterClass
  public static void destroy() {
    Args.clearParam();
    context.destroy();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
  }

  /**
   * init nodes.
   */
  public static void initNodes() {
    // dbManager = context.getBean(Manager.class);
    nodeManager = new NodeManager(context.getBean(ChainBaseManager.class));
    String currNodeId = "74c11ffad1d59d7b1a56691a0b84a53f0791c92361357364f1d2537"
        + "898407ef0249bbbf5a4ce8cff9e34e2fdf8bac883540e026d1e5d6ebf536414bdde81198e";
    String oldNodeId = "74c11ffad1d59d7b2c56691a0b84a53f0791c92361357364f1d2537898407e"
        + "f0249bbbf5a4ce8cff9e34e2fdf8bac883540e026d1e5d6ebf536414bdde81198e";
    String replaceNodeId = "74c11ffad1d59d7b1a56691a0b84a53f0791c92361357364f1d2537"
        + "837407ef0249bbbf5a4ce8cff9e34e2fdf8bac883540e026d1e5d6ebf536414bdde81198e";
    currNode = new Node(currNodeId.getBytes(), "47.95.206.44", 18885, 18888);
    oldNode = new Node(oldNodeId.getBytes(), "36.95.165.44", 18885, 18888);
    replaceNode = new Node(replaceNodeId.getBytes(), "47.29.177.44", 18885, 18888);
    currHandler = new NodeHandler(currNode, nodeManager);
    oldHandler = new NodeHandler(oldNode, nodeManager);
    replaceHandler = new NodeHandler(replaceNode, nodeManager);
  }

  @Test
  public void stateNonActiveTest() throws Exception {
    Class clazz = NodeHandler.class;
    Constructor<NodeHandler> cn = clazz.getDeclaredConstructor(Node.class, NodeManager.class);
    NodeHandler nh = cn.newInstance(oldNode, nodeManager);
    Field declaredField = clazz.getDeclaredField("replaceCandidate");
    declaredField.setAccessible(true);
    declaredField.set(nh, replaceHandler);

    nodeManager.getTable().addNode(oldNode);
    nh.changeState(NodeHandler.State.EVICTCANDIDATE);
    nh.changeState(NodeHandler.State.NONACTIVE);
    replaceHandler.changeState(NodeHandler.State.ALIVE);

    Assert.assertFalse(nodeManager.getTable().contains(oldNode));
    Assert.assertTrue(nodeManager.getTable().contains(replaceNode));
  }
}
