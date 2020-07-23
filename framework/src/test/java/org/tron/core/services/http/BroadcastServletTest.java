package org.tron.core.services.http;

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
import org.testng.annotations.Test;
import org.tron.common.utils.FileUtil;
import org.tron.core.services.http.solidity.mockito.HttpUrlStreamHandler;

@Slf4j
public class BroadcastServletTest {

  private static HttpUrlStreamHandler httpUrlStreamHandler;
  private BroadcastServlet broadcastServlet;
  private HttpServletRequest request;
  private HttpServletResponse response;
  private HttpURLConnection httpUrlConnection;
  private OutputStreamWriter outputStreamWriter;
  private URL url;

  /**
   * init before class.
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
   * set up.
   *
   * @throws InterruptedException .
   */
  @Before
  public void setUp() throws InterruptedException {
    broadcastServlet = new BroadcastServlet();
    this.request = mock(HttpServletRequest.class);
    this.response = mock(HttpServletResponse.class);
    this.httpUrlConnection = mock(HttpURLConnection.class);
    this.outputStreamWriter = mock(OutputStreamWriter.class);
    httpUrlStreamHandler.resetConnections();
  }

  /**
   * after test.
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
  public void testDoPost() throws IOException {
    URLStreamHandlerFactory urlStreamHandlerFactory = mock(URLStreamHandlerFactory.class);
    URL.setURLStreamHandlerFactory(urlStreamHandlerFactory);

    httpUrlStreamHandler = new HttpUrlStreamHandler();
    given(urlStreamHandlerFactory.createURLStreamHandler("http")).willReturn(httpUrlStreamHandler);

    broadcastServlet = new BroadcastServlet();
    this.request = mock(HttpServletRequest.class);
    this.response = mock(HttpServletResponse.class);
    this.httpUrlConnection = mock(HttpURLConnection.class);
    this.outputStreamWriter = mock(OutputStreamWriter.class);
    httpUrlStreamHandler.resetConnections();

    final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    System.setOut(new PrintStream(outContent));
    String href = "http://127.0.0.1:8090/wallet/broadcasttransaction";
    httpUrlStreamHandler.addConnection(new URL(href), httpUrlConnection);
    httpUrlConnection.setRequestMethod("POST");
    httpUrlConnection.setRequestProperty("Content-Type", "application/json");
    httpUrlConnection.setRequestProperty("Connection", "Keep-Alive");
    httpUrlConnection.setUseCaches(false);
    httpUrlConnection.setDoOutput(true);
    String postData = "{\"signature\":[\"97c825b41c77de2a8bd65b3df55cd4c0df59c307c0187e"
        + "42321dcc1cc455ddba583dd9502e17cfec5945b34cad0511985a6165999092a6dec84c2bdd9"
        + "7e649fc01\"],\"txID\":\"454f156bf1256587ff6ccdbc56e64ad0c51e4f8efea5490dcbc7"
        + "20ee606bc7b8\",\"raw_data\":{\"contract\":[{\"parame"
        + "ter\":{\"value\":{\"amount\":1000,\"owner_address\":\"41e552f6"
        + "487585c2b58bc2c9bb4492bc1f17132cd0\",\"to_address\":\"41d1e7a6bc354106cb410e"
        + "65ff8b181c600ff14292\"},\"type_url\":\"type.googl"
        + "eapis.com/protocol.TransferContract\"},\"type\":\"TransferCon"
        + "tract\"}],\"ref_block_bytes\":\"267e\",\"ref_block_hash\":\"9a447d222e8"
        + "de9f2\",\"expiration\":1530893064000,\"timestamp\":1530893006233}}";
    httpUrlConnection.setRequestProperty("Content-Length", "" + postData.length());

    when(httpUrlConnection.getOutputStream()).thenReturn(outContent);
    OutputStreamWriter out = new OutputStreamWriter(httpUrlConnection.getOutputStream(),
        StandardCharsets.UTF_8);
    out.write(postData);
    out.flush();
    out.close();
    PrintWriter writer = new PrintWriter("temp.txt");
    when(response.getWriter()).thenReturn(writer);

    broadcastServlet.doPost(request, response);
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