package org.tron.core.services.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.protobuf.ByteString;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;

public class GetMarketOrderByAccountServletTest extends BaseHttpTest {

  private GetMarketOrderByAccountServlet servlet;
  private final byte[] address = new ECKey().getAddress();
  private final String addrStr = ByteArray.toHexString(address);
  private final ByteString addrBytes = ByteString.copyFrom(address);

  @Override
  protected void setUpMocks() throws Exception {
    servlet = new GetMarketOrderByAccountServlet();
    injectWallet(servlet);
    when(wallet.getMarketOrderByAccount(any())).thenReturn(null);
  }

  @Test
  public void testPost() throws Exception {
    String jsonParam = "{\"value\": \"" + addrStr + "\"}";
    MockHttpServletRequest request = postRequest(jsonParam);

    MockHttpServletResponse response = newResponse();
    servlet.doPost(request, response);
    verify(wallet).getMarketOrderByAccount(eq(addrBytes));
    String content = response.getContentAsString();
    assertFalse("Should not contain error", content.contains("\"Error\""));
    assertEquals("{}" + System.lineSeparator(), content);
  }

  @Test
  public void testGet() throws Exception {
    MockHttpServletRequest request = getRequest("value", addrStr);

    MockHttpServletResponse response = newResponse();
    servlet.doGet(request, response);
    verify(wallet).getMarketOrderByAccount(eq(addrBytes));
    String content = response.getContentAsString();
    assertFalse("Should not contain error", content.contains("\"Error\""));
    assertEquals("{}" + System.lineSeparator(), content);
  }
}
