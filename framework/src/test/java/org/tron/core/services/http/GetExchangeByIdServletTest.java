package org.tron.core.services.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.google.protobuf.ByteString;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.tron.protos.Protocol.Exchange;

public class GetExchangeByIdServletTest extends BaseHttpTest {

  private GetExchangeByIdServlet servlet;
  private final ByteString exchangeId =
      ByteString.copyFrom(org.tron.common.utils.ByteArray.fromLong(1L));

  @Override
  protected void setUpMocks() throws Exception {
    servlet = new GetExchangeByIdServlet();
    injectWallet(servlet);
    when(wallet.getExchangeById(eq(exchangeId)))
        .thenReturn(Exchange.newBuilder().setExchangeId(1L).build());
  }

  @Test
  public void testPost() throws Exception {
    String jsonParam = "{\"id\": 1}";
    MockHttpServletRequest request = postRequest(jsonParam);

    MockHttpServletResponse response = newResponse();
    servlet.doPost(request, response);
    verify(wallet).getExchangeById(eq(exchangeId));
    assertEquals(200, response.getStatus());
    assertTrue(response.getContentAsString().contains("exchange_id"));
  }

  @Test
  public void testGet() throws Exception {
    MockHttpServletRequest request = getRequest("id", "1");

    MockHttpServletResponse response = newResponse();
    servlet.doGet(request, response);
    verify(wallet).getExchangeById(eq(exchangeId));
    assertEquals(200, response.getStatus());
    assertTrue(response.getContentAsString().contains("exchange_id"));
  }

  @Test
  public void oversizedQuotedIdIsRejectedBeforeWalletCall() throws Exception {
    String oversized = "99999999999999999999999999999999999999999999999999999999999999999";
    MockHttpServletResponse response = newResponse();

    servlet.doPost(postRequest("{\"id\":\"" + oversized + "\"}"), response);

    verifyNoInteractions(wallet);
    assertTrue(response.getContentAsString().contains("id"));
    assertTrue(response.getContentAsString().contains("64 characters"));
    assertFalse(response.getContentAsString().contains(oversized));
  }
}
