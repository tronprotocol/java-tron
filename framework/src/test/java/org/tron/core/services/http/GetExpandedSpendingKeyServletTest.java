package org.tron.core.services.http;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.utils.FileUtil;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
public class GetExpandedSpendingKeyServletTest {
  private GetExpandedSpendingKeyServlet getExpandedSpendingKeyServlet;
  private HttpServletRequest request;
  private HttpServletResponse response;

  /**
   * init.
   */
  @Before
  public void setUp() {
    getExpandedSpendingKeyServlet = new GetExpandedSpendingKeyServlet();
    request = mock(HttpServletRequest.class);
    response = mock(HttpServletResponse.class);
  }

  /**
   * release resource.
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
    URL url = new URL("http://127.0.0.1:8090/wallet/getexpandedspendingkey");
    String postData = "{\"value\": \"06b02aaa00f230b0887ff57a6609d76691369972ac3ba"
            + "568fe7a8a0897fce7c4\"}";
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("POST");
    conn.setRequestProperty("Content-Type", "application/json");
    conn.setRequestProperty("Connection", "Keep-Alive");
    conn.setUseCaches(false);
    conn.setDoOutput(true);
    conn.setRequestProperty("Content-Length", "" + postData.length());
    OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
    out.write(postData);
    out.flush();
    out.close();
    PrintWriter writer = new PrintWriter("temp.txt");
    when(response.getWriter()).thenReturn(writer);

    getExpandedSpendingKeyServlet.doPost(request, response);
    //Get Response Body
    String line = "";
    String result = "";
    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
    while ((line = in.readLine()) != null) {
      result += line + "\n";
    }
    in.close();
    logger.info(result);
    Assert.assertTrue(result.contains("ask"));
    Assert.assertTrue(result.contains("3123f308343cda89604fafae517"
            + "994ce5c78397346659b51b7dba8f50a08a703"));
    Assert.assertTrue(result.contains("nsk"));
    Assert.assertTrue(result.contains("873fc387c24fa4b0eb5208778e6b"
            + "99d01bd909da64b584e646fa54e5945fd207"));
    Assert.assertTrue(result.contains("ovk"));
    Assert.assertTrue(result.contains("3063b5fa5929bc1e4342d5a02459"
            + "3d7c2457efc33071a8460e933ce272cadd9f"));
    writer.flush();
    conn.disconnect();
  }
}
