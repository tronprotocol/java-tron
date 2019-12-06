package org.tron.core.services.http;

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
public class AddTransactionSignServletTest {
  private AddTransactionSignServlet addTransactionSignServlet;
  private HttpServletRequest request;
  private HttpServletResponse response;

  /**
   * init.
   */
  @Before
  public void setUp() {
    addTransactionSignServlet = new AddTransactionSignServlet();
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
    URL url = new URL("http://127.0.0.1:8090/wallet/addtransactionsign");
    String postData = "{\n"
            + "    \"transaction\": {\n"
            + "      \"visible\": true,\n"
            + "      \"txID\": \"752cece5a68e40e30eaeeb4c5844b3f4b004d23485ccef42e0609a9a90"
            + "eeb675\",\n"
            + "      \"raw_data\": {\n"
            + "          \"contract\": [{\n"
            + "              \"parameter\": {\n"
            + "                  \"value\": {\n"
            + "                      \"data\": \"a9059cbb0000000000000000000000415a523b44989"
            + "0854c8fc460ab602df9f31fe4293f00000000000000000000000000000000000000000000000000"
            + "000000000001f4\",\n"
            + "                      \"owner_address\": \"TRGhNNfnmgLegT4zHNjEqDSADjgmnHvubJ\",\n"
            + "                      \"contract_address\": \"TVf6sdWWu8zvmtcWfZerMDefoptiVhbhXC\"\n"
            + "                  },\n"
            + "                  \"type_url\": \"type.googleapis.com/protocol"
            + ".TriggerSmartContract\"\n"
            + "              },\n"
            + "              \"type\": \"TriggerSmartContract\"\n"
            + "          }],\n"
            + "          \"ref_block_bytes\": \"0883\",\n"
            + "          \"ref_block_hash\": \"84c32fcee77f6be7\",\n"
            + "          \"expiration\": 1556449785000,\n"
            + "          \"fee_limit\": 10000,\n"
            + "          \"timestamp\": 1556449725625\n"
            + "      },\n"
            + "      \"raw_data_hex\": \"0a020883220884c32fcee77f6be740a8e98b9da62d5aae01081f12a90"
            + "10a31747970652e676f6f676c65617069732e636f6d2f70726f746f636f6c2e54726967676572536d61"
            + "7274436f6e747261637412740a1541a7d8a35b260395c14aa456297662092ba3b76fc0121541d7f5e9b"
            + "3b997006444c1646ecfae6549b5737e622244a9059cbb0000000000000000000000415a523b44989085"
            + "4c8fc460ab602df9f31fe4293f000000000000000000000000000000000000000000000000000000000"
            + "00001f470b999889da62d9001904e\"\n"
            + "  },\n"
            + "  \"privateKey\": \"950139607044677436d29ff1ea2900c940"
            + "2f783a91547cdc47cf706f1129c76a\"\n"
            + "}";
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

    addTransactionSignServlet.doPost(request, response);
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
    Assert.assertTrue(result.toString().contains("Account is not exist!"));
    writer.flush();
    conn.disconnect();
  }
}

