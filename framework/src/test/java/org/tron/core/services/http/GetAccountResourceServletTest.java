package org.tron.core.services.http;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.protobuf.ByteString;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;

public class GetAccountResourceServletTest extends BaseHttpTest {

  private GetAccountResourceServlet servlet;
  private final String addrStr = ByteArray.toHexString(new ECKey().getAddress());

  @Override
  protected void setUpMocks() throws Exception {
    servlet = new GetAccountResourceServlet();
    injectWallet(servlet);
    when(wallet.getAccountResource(any()))
        .thenReturn(AccountResourceMessage.newBuilder().setFreeNetUsed(1L).build());
  }

  @Test
  public void testGetAccountResourcePost() throws Exception {
    String jsonParam = "{\"address\": \"" + addrStr + "\"}";
    MockHttpServletRequest request = postRequest(jsonParam);

    MockHttpServletResponse response = newResponse();
    servlet.doPost(request, response);
    verify(wallet).getAccountResource(eq(ByteString.copyFrom(ByteArray.fromHexString(addrStr))));
    String content = response.getContentAsString();
    assertFalse("Should not contain error", content.contains("\"Error\""));
    assertTrue("Should contain freeNetUsed", content.contains("freeNetUsed"));
  }

  @Test
  public void testGetAccountResourceGet() throws Exception {
    MockHttpServletRequest request = getRequest("address", addrStr);

    MockHttpServletResponse response = newResponse();
    servlet.doGet(request, response);
    verify(wallet).getAccountResource(eq(ByteString.copyFrom(ByteArray.fromHexString(addrStr))));
    String content = response.getContentAsString();
    assertFalse("Should not contain error", content.contains("\"Error\""));
    assertTrue("Should contain freeNetUsed", content.contains("freeNetUsed"));
  }
}
