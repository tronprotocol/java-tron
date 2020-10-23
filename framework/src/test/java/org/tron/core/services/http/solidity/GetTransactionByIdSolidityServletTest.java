package org.tron.core.services.http.solidity;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLStreamHandlerFactory;
import java.nio.charset.StandardCharsets;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.utils.FileUtil;
import org.tron.core.services.http.solidity.mockito.HttpUrlStreamHandler;


@Slf4j
public class GetTransactionByIdSolidityServletTest {

  private static HttpUrlStreamHandler httpUrlStreamHandler;
  private GetTransactionByIdSolidityServlet getTransactionByIdSolidityServlet;
  private HttpServletRequest request;
  private HttpServletResponse response;
  private HttpURLConnection httpUrlConnection;
  private OutputStreamWriter outputStreamWriter;
  private URL url;

  /**
   * .
   */
  @BeforeClass
  public static void init() {
    // Allows for mocking URL connections
    URLStreamHandlerFactory urlStreamHandlerFactory = mock(URLStreamHandlerFactory.class);
    URL.setURLStreamHandlerFactory(urlStreamHandlerFactory);

    httpUrlStreamHandler = new HttpUrlStreamHandler();
    given(urlStreamHandlerFactory.createURLStreamHandler("http")).willReturn(httpUrlStreamHandler);
  }

  /**
   * Init.
   */

  @Before
  public void setUp() throws InterruptedException {
    getTransactionByIdSolidityServlet = new GetTransactionByIdSolidityServlet();
    this.request = mock(HttpServletRequest.class);
    this.response = mock(HttpServletResponse.class);
    this.httpUrlConnection = mock(HttpURLConnection.class);
    this.outputStreamWriter = mock(OutputStreamWriter.class);
    httpUrlStreamHandler.resetConnections();
  }

  /**
   * Release Resource.
   */
  @After
  public void tearDown() {
    if (FileUtil.deleteDir(new File("temp.txt"))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
  }

  @Test
  public void doPostTest() throws IOException {

    //send Post request

    final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    System.setOut(new PrintStream(outContent));
    String href = "http://127.0.0.1:8091/walletsolidity/gettransactioninfobyid";
    httpUrlStreamHandler.addConnection(new URL(href), httpUrlConnection);
    httpUrlConnection.setRequestMethod("POST");
    httpUrlConnection.setRequestProperty("Content-Type", "application/json");
    httpUrlConnection.setRequestProperty("Connection", "Keep-Alive");
    httpUrlConnection.setUseCaches(false);
    httpUrlConnection.setDoOutput(true);
    String postData = "{\"value\": \"309b6fa3d01353e46f57dd8a8f27611f98e392b50d035cef21"
        + "3f2c55225a8bd2\"}";
    httpUrlConnection.setRequestProperty("Content-Length", "" + postData.length());

    when(httpUrlConnection.getOutputStream()).thenReturn(outContent);
    OutputStreamWriter out = new OutputStreamWriter(httpUrlConnection.getOutputStream(),
        StandardCharsets.UTF_8);
    out.write(postData);
    out.flush();
    out.close();
    PrintWriter writer = new PrintWriter("temp.txt");
    when(response.getWriter()).thenReturn(writer);

    getTransactionByIdSolidityServlet.doPost(request, response);
    //    Get Response Body
    String line;
    StringBuilder result = new StringBuilder();

    byte[] buffer = new byte[1024];
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer);
    when(httpUrlConnection.getInputStream()).thenReturn(byteArrayInputStream);
    BufferedReader in = new BufferedReader(new InputStreamReader(httpUrlConnection.getInputStream(),
        StandardCharsets.UTF_8));

    while ((line = in.readLine()) != null) {
      result.append(line).append("\n");
    }
    in.close();
    writer.flush();
    FileInputStream fileInputStream = new FileInputStream("temp.txt");
    InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

    StringBuffer sb = new StringBuffer();
    String text = null;
    while ((text = bufferedReader.readLine()) != null) {
      sb.append(text);
    }
    Assert.assertTrue(sb.toString().contains("null"));
    httpUrlConnection.disconnect();
  }

  @Test
  public void doGetTest() throws IOException {

    final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    System.setOut(new PrintStream(outContent));
    String href = "http://127.0.0.1:8091/walletsolidity/gettransactioninfobyid";
    httpUrlStreamHandler.addConnection(new URL(href), httpUrlConnection);
    httpUrlConnection.setRequestMethod("GET");
    httpUrlConnection.setRequestProperty("Content-Type", "application/json");
    httpUrlConnection.setRequestProperty("Connection", "Keep-Alive");
    httpUrlConnection.setUseCaches(false);
    httpUrlConnection.setDoOutput(true);
    String postData = "{\"value\": \"309b6fa3d01353e46f57dd8a8f27611f98e392b50d035cef21"
        + "3f2c55225a8bd2\"}";
    httpUrlConnection.setRequestProperty("Content-Length", "" + postData.length());

    when(httpUrlConnection.getOutputStream()).thenReturn(outContent);
    OutputStreamWriter out = new OutputStreamWriter(httpUrlConnection.getOutputStream(),
        StandardCharsets.UTF_8);
    out.write(postData);
    out.flush();
    out.close();
    PrintWriter writer = new PrintWriter("temp.txt");
    when(response.getWriter()).thenReturn(writer);

    getTransactionByIdSolidityServlet.doPost(request, response);
    //    Get Response Body
    String line;
    StringBuilder result = new StringBuilder();

    byte[] buffer = new byte[1024];
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer);
    when(httpUrlConnection.getInputStream()).thenReturn(byteArrayInputStream);
    BufferedReader in = new BufferedReader(new InputStreamReader(httpUrlConnection.getInputStream(),
        StandardCharsets.UTF_8));

    while ((line = in.readLine()) != null) {
      result.append(line).append("\n");
    }
    in.close();
    writer.flush();
    FileInputStream fileInputStream = new FileInputStream("temp.txt");
    InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

    StringBuffer sb = new StringBuffer();
    String text = null;
    while ((text = bufferedReader.readLine()) != null) {
      sb.append(text);
    }
    Assert.assertTrue(sb.toString().contains("null"));
    httpUrlConnection.disconnect();
  }
}

