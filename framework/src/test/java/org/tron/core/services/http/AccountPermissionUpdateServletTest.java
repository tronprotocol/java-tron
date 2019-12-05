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
public class AccountPermissionUpdateServletTest {
  private AccountPermissionUpdateServlet accountPermissionUpdateServlet;
  private HttpServletRequest request;
  private HttpServletResponse response;

  /**
   * init.
   */
  @Before
  public void setUp() {
    accountPermissionUpdateServlet = new AccountPermissionUpdateServlet();
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
    URL url = new URL("http://127.0.0.1:8090/wallet/accountpermissionupdate");
    String postData = "{\n"
            + "  \"owner_address\": \"TRGhNNfnmgLegT4zHNjEqDSADjgmnHvubJ\",\n"
            + "  \"owner\": {\n"
            + "      \"type\": 0,\n"
            + "      \"permission_name\": \"owner\",\n"
            + "      \"threshold\": 1,\n"
            + "      \"keys\": [{\n"
            + "          \"address\": \"TRGhNNfnmgLegT4zHNjEqDSADjgmnHvubJ\",\n"
            + "          \"weight\": 1\n"
            + "      }]\n"
            + "  },\n"
            + "  \"witness\": {\n"
            + "      \"type\": 1,\n"
            + "      \"permission_name\": \"witness\",\n"
            + "      \"threshold\": 1,\n"
            + "      \"keys\": [{\n"
            + "          \"address\": \"TRGhNNfnmgLegT4zHNjEqDSADjgmnHvubJ\",\n"
            + "          \"weight\": 1\n"
            + "      }]\n"
            + "  },\n"
            + "  \"actives\": [{\n"
            + "      \"type\": 2,\n"
            + "      \"permission_name\": \"active12323\",\n"
            + "      \"threshold\": 2,\n"
            + "      \"operations\": \"7fff1fc0033e000000000000000000000000000000"
            + "0000000000000000000000\",\n"
            + "      \"keys\": [{\n"
            + "          \"address\": \"TNhXo1GbRNCuorvYu5JFWN3m2NYr9QQpVR\",\n"
            + "          \"weight\": 1\n"
            + "      }, {\n"
            + "          \"address\": \"TKwhcDup8L2PH5r6hxp5CQvQzZqJLmKvZP\",\n"
            + "          \"weight\": 1\n"
            + "      }]\n"
            + "  }],\n"
            + "  \"visible\": true\n"
            + "\t\n"
            + "}\n";
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

    accountPermissionUpdateServlet.doPost(request, response);
    //Get Response Body
    String line = "";
    String result = "";
    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
    while ((line = in.readLine()) != null) {
      result += line + "\n";
    }
    in.close();
    logger.info(result);
    Assert.assertTrue(result.contains("multi sign is not allowed, need to be opened"));
    writer.flush();
    conn.disconnect();
  }
}

