package org.tron.core.net.nodehandler;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.overlay.discover.node.Node;
import org.tron.common.overlay.discover.node.NodeHandler;
import org.tron.common.overlay.discover.node.NodeManager;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;


public class NodeHandlerTest {
    private static final Logger logger = LoggerFactory.getLogger("Test");
    private Manager dbManager;
    private TronApplicationContext context;
    private Application AppT;
    private Args args;
    private Node currNode;
    private Node oldNode;
    private Node replaceNode;
    private NodeHandler currHandler;
    private NodeHandler oldHandler;
    private NodeHandler replaceHandler;
    private NodeManager nodeManager;

    @Before
    public void init() {
        args = Args.getInstance();
        Args.setParam(new String[]{"--output-directory", "output-directory", "--debug"}, Constant.TEST_CONF);
        context = new TronApplicationContext(DefaultConfig.class); //使用这个TronApplicationContext类，不要使用AnnotationConfigApplicationContext类
        AppT = ApplicationFactory.create(context);
        AppT.initServices(args);
        AppT.startServices();
        AppT.startup();
    }

    @After
    public void destroy() {
        Args.clearParam();
        AppT.shutdownServices();
        AppT.shutdown();
        context.destroy(); //context.destroy()调用必须在FileUtil.deleteDir()前执行，否则数据库文件删除不成功。

        if (FileUtil.deleteDir(new File("output-directory"))) {
            logger.info("Release resources successful.");
        } else {
            logger.info("Release resources failure.");
        }
    }

    @Before
    public void initNodes() {
        dbManager = context.getBean(Manager.class);
        nodeManager = new NodeManager(dbManager);
        String currNodeId = "74c11ffad1d59d7b1a56691a0b84a53f0791c92361357364f1d2537898407ef0249bbbf5a4ce8cff9e34e2fdf8bac883540e026d1e5d6ebf536414bdde81198e";
        String oldNodeId = "74c11ffad1d59d7b2c56691a0b84a53f0791c92361357364f1d2537898407ef0249bbbf5a4ce8cff9e34e2fdf8bac883540e026d1e5d6ebf536414bdde81198e";
        String replaceNodeId = "74c11ffad1d59d7b1a56691a0b84a53f0791c92361357364f1d2537837407ef0249bbbf5a4ce8cff9e34e2fdf8bac883540e026d1e5d6ebf536414bdde81198e";
        currNode = new Node(currNodeId.getBytes(),"47.95.206.44",18885,18888);
        oldNode = new Node(oldNodeId.getBytes(),"36.95.165.44",18885,18888);
        replaceNode = new Node(replaceNodeId.getBytes(), "47.29.177.44", 18885,18888);
        currHandler = new NodeHandler(currNode, nodeManager);
        oldHandler = new NodeHandler(oldNode, nodeManager);
        replaceHandler = new NodeHandler(replaceNode, nodeManager);
    }

    @Test
    public void stateNonActiveTest() throws Exception{
        Class clazz = NodeHandler.class;
        Constructor<NodeHandler> cn = clazz.getDeclaredConstructor(Node.class, NodeManager.class);
        NodeHandler nh = cn.newInstance(oldNode,nodeManager);
        Field declaredField = clazz.getDeclaredField("replaceCandidate");
        declaredField.setAccessible(true);
        declaredField.set(nh, replaceHandler);

        nodeManager.getTable().addNode(oldNode);
        nh.changeState(NodeHandler.State.EVICTCANDIDATE);
        nh.changeState(NodeHandler.State.NONACTIVE);
        replaceHandler.changeState(NodeHandler.State.ALIVE);

        Assert.assertTrue(!nodeManager.getTable().contains(oldNode));
        Assert.assertTrue(nodeManager.getTable().contains(replaceNode));
    }
}
