package org.tron.core.services.http.solidity;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.protobuf.ByteString;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.tron.common.TestConstants;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.core.config.args.Args;

public class GetTransactionByIdSolidityServletTest {

  private static final String TX_ID =
      "309b6fa3d01353e46f57dd8a8f27611f98e392b50d035cef213f2c55225a8bd2";
  private GetTransactionByIdSolidityServlet servlet;
  private Wallet wallet;
  private HttpServletRequest request;
  private HttpServletResponse response;

  @Before
  public void setUp() {
    Args.setParam(new String[0], TestConstants.TEST_CONF);
    servlet = new GetTransactionByIdSolidityServlet();
    wallet = mock(Wallet.class);
    ReflectionTestUtils.setField(servlet, "wallet", wallet);
    request = mock(HttpServletRequest.class);
    response = mock(HttpServletResponse.class);
  }

  @After
  public void tearDown() {
    Args.clearParam();
  }

  @Test
  public void doPostTest() throws IOException {
    when(request.getReader()).thenReturn(new BufferedReader(
        new StringReader("{\"value\":\"" + TX_ID + "\"}")));
    assertMissingTransactionResponse(true);
  }

  @Test
  public void doGetTest() throws IOException {
    when(request.getParameter("value")).thenReturn(TX_ID);
    assertMissingTransactionResponse(false);
  }

  private void assertMissingTransactionResponse(boolean post) throws IOException {
    StringWriter body = new StringWriter();
    try (PrintWriter writer = new PrintWriter(body)) {
      when(response.getWriter()).thenReturn(writer);
      if (post) {
        servlet.doPost(request, response);
      } else {
        servlet.doGet(request, response);
      }
      writer.flush();
      Assert.assertEquals("{}", body.toString().trim());
      verify(wallet).getTransactionById(ByteString.copyFrom(ByteArray.fromHexString(TX_ID)));
    }
  }
}
