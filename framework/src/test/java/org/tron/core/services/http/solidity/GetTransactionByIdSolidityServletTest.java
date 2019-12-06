package org.tron.core.services.http.solidity;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.utils.FileUtil;


@Slf4j
public class GetTransactionByIdSolidityServletTest {
  private GetTransactionByIdSolidityServlet getTransactionByIdSolidityServlet;
  private HttpServletRequest request;
  private HttpServletResponse response;

  /**
   * Init.
   */
  @Before
  public void setUp() {
    getTransactionByIdSolidityServlet = new GetTransactionByIdSolidityServlet();
    request = mock(HttpServletRequest.class);
    response = mock(HttpServletResponse.class);
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
    URL url = new URL("http://127.0.0.1:8091/walletsolidity/gettransactioninfobyid");
    String postData = "{\"value\": \"309b6fa3d01353e46f57dd8a8f27611f98e392b50d035cef21"
            + "3f2c55225a8bd2\"}";

    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("POST");
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
    logger.info(result.toString());
    Assert.assertTrue(result.toString().contains("{}"));
    writer.flush();
    conn.disconnect();
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

