package org.tron.core.services.http.solidity;

import lombok.extern.slf4j.Slf4j;
import org.junit.*;
import org.tron.common.utils.FileUtil;
import org.tron.core.services.http.solidity.mockito.HttpUrlStreamHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandlerFactory;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@Slf4j
public class GetTransactionByIdSolidityServletTest_1 {
  private GetTransactionByIdSolidityServlet getTransactionByIdSolidityServlet;
  private HttpServletRequest request;
  private HttpServletResponse response;
  private HttpURLConnection httpURLConnection;
  private OutputStreamWriter outputStreamWriter;
  private URL url;


  private static HttpUrlStreamHandler httpUrlStreamHandler;

  @BeforeClass
  public static void setupURLStreamHandlerFactory() {
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
    this.httpURLConnection = mock(HttpURLConnection.class);
    this.outputStreamWriter = mock(OutputStreamWriter.class);
//    this.url = mock(URL.class);
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
    String postData = "{\"value\": \"309b6fa3d01353e46f57dd8a8f27611f98e392b50d035cef21"
            + "3f2c55225a8bd2\"}";
    final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    System.setOut(new PrintStream(outContent));


    String href = "http://127.0.0.1:8091/walletsolidity/gettransactioninfobyid";

    httpUrlStreamHandler.addConnection(new URL(href), httpURLConnection);

    httpURLConnection.setRequestMethod("POST");
    httpURLConnection.setRequestProperty("Content-Type", "application/json");
    httpURLConnection.setRequestProperty("Connection", "Keep-Alive");
    httpURLConnection.setUseCaches(false);
    httpURLConnection.setDoOutput(true);
    httpURLConnection.setRequestProperty("Content-Length", "" + postData.length());

    when(httpURLConnection.getOutputStream()).thenReturn(outContent);
    OutputStreamWriter out = new OutputStreamWriter(httpURLConnection.getOutputStream(), StandardCharsets.UTF_8);
    out.write(postData);
    out.flush();
    out.close();
    PrintWriter writer = new PrintWriter("temp.txt");
    when(response.getWriter()).thenReturn(writer);

    getTransactionByIdSolidityServlet.doPost(request, response);
//    Get Response Body
    String line;
    StringBuilder result = new StringBuilder();

    InputStream inputStream = mock(InputStream.class);
    byte[] buffer = new byte[1024];
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer);
    when(httpURLConnection.getInputStream()).thenReturn(byteArrayInputStream);
    BufferedReader in = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream(),
            StandardCharsets.UTF_8));

    while ((line = in.readLine()) != null) {
      result.append(line).append("\n");
    }
    in.close();
//    Assert.assertTrue(result.toString().contains("{}"));
    writer.flush();

    FileInputStream fileInputStream = new FileInputStream("temp.txt");
    InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

    StringBuffer sb = new StringBuffer();
    String text = null;
    while((text = bufferedReader.readLine()) != null){
      sb.append(text);
    }
    Assert.assertTrue(sb.toString().contains("null"));
    httpURLConnection.disconnect();
  }

  @Test
  public void doGetTest() throws IOException {

    //send Post request
    URL url = new URL("http://127.0.0.1:8091/walletsolidity/gettransactioninfobyid");
    String postData = "{\"value\": \"309b6fa3d01353e46f57dd8a8f27611f98e392b50d035cef21"
            + "3f2c55225a8bd2\"}";

    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("GET");
    conn.setRequestProperty("Content-Type", "application/json");
    conn.setRequestProperty("Connection", "Keep-Alive");
    conn.setUseCaches(false);
    conn.setDoOutput(true);
    conn.setRequestProperty("Content-Length", "" + postData.length());
    OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8);
    out.write(postData);
    out.flush();
    out.close();
    PrintWriter writer = new PrintWriter("temp.txt");
    when(response.getWriter()).thenReturn(writer);

    getTransactionByIdSolidityServlet.doPost(request, response);
    //Get Response Body
    String line;
    StringBuilder result = new StringBuilder();
    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(),
            StandardCharsets.UTF_8));
    while ((line = in.readLine()) != null) {
      result.append(line).append("\n");
    }
    in.close();

    Assert.assertTrue(result.toString().contains("{}"));
    writer.flush();
    conn.disconnect();
  }
}

