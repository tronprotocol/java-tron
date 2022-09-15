package org.tron.core.services.http;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.overlay.discover.node.Node;
import org.tron.common.overlay.discover.node.NodeHandler;
import org.tron.common.overlay.discover.node.NodeManager;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.services.jsonrpc.TronJsonRpcImpl;

@Slf4j
public class ListNodesServletTest {
  private static String dbPath = "service_test_" + RandomStringUtils.randomAlphanumeric(10);
  private static TronApplicationContext context;
  private ListNodesServlet listNodesServlet;
  private HttpServletRequest request;
  private HttpServletResponse response;

  static {
    Args.setParam(new String[] {"--output-directory", dbPath, "--debug"}, Constant.TEST_CONF);
    // 启服务，具体的端口号啥的在DefaultConfig.class里写死的
    context = new TronApplicationContext(DefaultConfig.class);
  }

  /** . */
  @AfterClass
  public static void removeDb() {
    Args.clearParam();
    context.destroy();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
  }

  /** Init. */
  @Before
  public void setUp() throws InterruptedException {
    listNodesServlet = context.getBean(ListNodesServlet.class);
    this.request = mock(HttpServletRequest.class);
    this.response = mock(HttpServletResponse.class);

    Node node1 = new Node(new byte[64], "128.0.0.1", 18889, 18889);
    Node node2 = new Node(new byte[64], "128.0.0.2", 18889, 18889);
    NodeManager nodeManager = context.getBean(NodeManager.class);
    NodeHandler nodeHandler1 = nodeManager.getNodeHandler(node1);
    NodeHandler nodeHandler2 = nodeManager.getNodeHandler(node2);

    nodeHandler1.changeState(NodeHandler.State.ALIVE);
    nodeHandler2.changeState(NodeHandler.State.ACTIVE);

    context.getBean(NodeManager.class).dumpActiveNodes().set(0, nodeHandler1);
    context.getBean(NodeManager.class).dumpActiveNodes().set(1, nodeHandler2);

    nodeManager.dumpActiveNodes().set(0, nodeHandler1);
    nodeManager.dumpActiveNodes().set(1, nodeHandler2);
  }

  /** . */
  @After
  public void tearDown() {
    if (FileUtil.deleteDir(new File("temp.txt"))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
  }

  @Test
  public void testDoGet() {
    String result = "";
    try {
      PrintWriter writer = new PrintWriter("temp.txt");
      when(response.getWriter()).thenReturn(writer);
      listNodesServlet.doGet(request, response);
      writer.close();
      FileInputStream fileInputStream = new FileInputStream("temp.txt");
      InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
      BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

      StringBuffer sb = new StringBuffer();
      String text = null;
      while ((text = bufferedReader.readLine()) != null) {
        sb.append(text);
      }
      fileInputStream.close();
      inputStreamReader.close();
      bufferedReader.close();
      Assert.assertTrue(sb.toString().contains("3132382e302e302e31"));
    } catch (Exception e) {
      Assert.fail();
    }
  }

  @Test
  public void testDoPost() {
    String result = "";
    try {
      PrintWriter writer = new PrintWriter("temp.txt");
      when(response.getWriter()).thenReturn(writer);
      listNodesServlet.doPost(request, response);
      writer.close();
      FileInputStream fileInputStream = new FileInputStream("temp.txt");
      InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
      BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

      StringBuffer sb = new StringBuffer();
      String text = null;
      while ((text = bufferedReader.readLine()) != null) {
        sb.append(text);
      }
      fileInputStream.close();
      inputStreamReader.close();
      bufferedReader.close();
      Assert.assertTrue(sb.toString().contains("3132382e302e302e32"));
    } catch (Exception e) {
      Assert.fail();
    }
  }
}