package org.tron.core.services.http.solidity;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.google.protobuf.ByteString;
import java.util.Arrays;
import java.util.Collection;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.core.config.args.Args;
import org.tron.json.JSONObject;
import org.tron.protos.Protocol.TransactionInfo;

@RunWith(Parameterized.class)
public class GetTransactionInfoByIdSolidityServletTest {

  private static final String TRANSACTION_ID =
      "309b6fa3d01353e46f57dd8a8f27611f98e392b50d035cef213f2c55225a8bd2";
  private static final ByteString TRANSACTION_ID_BYTES =
      ByteString.copyFrom(ByteArray.fromHexString(TRANSACTION_ID));

  @Parameter
  public String method;

  private GetTransactionInfoByIdSolidityServlet servlet;
  private Wallet wallet;
  private long savedMaxMessageSize;

  @Parameters(name = "{0}")
  public static Collection<Object[]> methods() {
    return Arrays.asList(new Object[][] {{"GET"}, {"POST"}});
  }

  @Before
  public void setUp() {
    savedMaxMessageSize = Args.getInstance().getHttpMaxMessageSize();
    Args.getInstance().setHttpMaxMessageSize(1024);
    servlet = new GetTransactionInfoByIdSolidityServlet();
    wallet = mock(Wallet.class);
    ReflectionTestUtils.setField(servlet, "wallet", wallet);
  }

  @After
  public void tearDown() {
    Args.getInstance().setHttpMaxMessageSize(savedMaxMessageSize);
  }

  @Test
  public void walletFailureReturnsSanitizedJson() throws Exception {
    when(wallet.getTransactionInfoById(TRANSACTION_ID_BYTES))
        .thenThrow(new NullPointerException("internal transaction store detail"));

    MockHttpServletResponse response = request(TRANSACTION_ID);

    assertEquals("internal server error", errorMessage(response));
    verify(wallet).getTransactionInfoById(TRANSACTION_ID_BYTES);
  }

  @Test
  public void invalidHexReturnsJsonWithoutCallingWallet() throws Exception {
    MockHttpServletResponse response = request("zz");

    String message = errorMessage(response);
    if ("GET".equals(method)) {
      assertEquals("internal server error", message);
    } else {
      assertTrue(message.matches("\\d+:\\d+: INVALID hex String"));
    }
    verifyNoInteractions(wallet);
  }

  @Test
  public void missingTransactionKeepsEmptyObject() throws Exception {
    MockHttpServletResponse response = request(TRANSACTION_ID);

    assertEquals(200, response.getStatus());
    assertEquals("{}", response.getContentAsString().trim());
    verify(wallet).getTransactionInfoById(TRANSACTION_ID_BYTES);
  }

  @Test
  public void successfulLookupKeepsTransactionInfo() throws Exception {
    TransactionInfo info = TransactionInfo.newBuilder()
        .setId(TRANSACTION_ID_BYTES).setFee(7).setBlockNumber(123).build();
    when(wallet.getTransactionInfoById(TRANSACTION_ID_BYTES)).thenReturn(info);

    MockHttpServletResponse response = request(TRANSACTION_ID);

    assertEquals(200, response.getStatus());
    JSONObject body = JSONObject.parseObject(response.getContentAsString());
    assertEquals(3, body.size());
    assertEquals(TRANSACTION_ID, body.getString("id"));
    assertEquals(7L, body.getLongValue("fee"));
    assertEquals(123L, body.getLongValue("blockNumber"));
    verify(wallet).getTransactionInfoById(TRANSACTION_ID_BYTES);
  }

  private MockHttpServletResponse request(String value) throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest(method,
        "/walletsolidity/gettransactioninfobyid");
    MockHttpServletResponse response = new MockHttpServletResponse();
    if ("GET".equals(method)) {
      request.setParameter("value", value);
      servlet.doGet(request, response);
    } else {
      request.setContentType("application/json");
      request.setContent(("{\"value\":\"" + value + "\"}").getBytes(UTF_8));
      servlet.doPost(request, response);
    }
    return response;
  }

  private static String errorMessage(MockHttpServletResponse response) throws Exception {
    assertEquals(200, response.getStatus());
    JSONObject body = JSONObject.parseObject(response.getContentAsString());
    assertEquals(1, body.size());
    return body.getString("Error");
  }
}
