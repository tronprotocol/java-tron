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
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;

@Slf4j
public class GetBandwidthPricesServletTest {
  private static String dbPath = "service_test_" + RandomStringUtils.randomAlphanumeric(10);
  private static TronApplicationContext context;
  private GetBandwidthPricesServlet getBandwidthPricesServlet;
  private HttpServletRequest request;
  private HttpServletResponse response;

  static {
    Args.setParam(new String[] {"--output-directory", dbPath, "--debug"}, Constant.TEST_CONF);
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
    getBandwidthPricesServlet = context.getBean(GetBandwidthPricesServlet.class);
    this.request = mock(HttpServletRequest.class);
    this.response = mock(HttpServletResponse.class);
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
      getBandwidthPricesServlet.doGet(request, response);
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
      Assert.assertTrue(sb.toString().contains("10"));
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
      getBandwidthPricesServlet.doPost(request, response);
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
      Assert.assertTrue(sb.toString().contains("10"));
    } catch (Exception e) {
      Assert.fail();
    }
  }
}
