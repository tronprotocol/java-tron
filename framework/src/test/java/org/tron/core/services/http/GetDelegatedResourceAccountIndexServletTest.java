package org.tron.core.services.http;

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
import org.tron.protos.Protocol.DelegatedResourceAccountIndex;

public class GetDelegatedResourceAccountIndexServletTest extends BaseHttpTest {

  private GetDelegatedResourceAccountIndexServlet servlet;
  private final byte[] address = new ECKey().getAddress();
  private final String addrStr = ByteArray.toHexString(address);
  private final ByteString expectedAddress = ByteString.copyFrom(address);

  @Override
  protected void setUpMocks() throws Exception {
    servlet = new GetDelegatedResourceAccountIndexServlet();
    injectWallet(servlet);
    when(wallet.getDelegatedResourceAccountIndex(any()))
        .thenReturn(DelegatedResourceAccountIndex.getDefaultInstance());
  }

  @Test
  public void testGetDelegatedResourceAccountIndexPost() throws Exception {
    String jsonParam = "{\"value\": \"" + addrStr + "\"}";
    MockHttpServletRequest request = postRequest(jsonParam);

    MockHttpServletResponse response = newResponse();
    servlet.doPost(request, response);
    verify(wallet).getDelegatedResourceAccountIndex(eq(expectedAddress));
    String content = response.getContentAsString();
    assertFalse("Should not be empty", content.trim().isEmpty());
    assertFalse("Should not contain error", content.contains("\"Error\""));
  }

  @Test
  public void testGetDelegatedResourceAccountIndexGet() throws Exception {
    MockHttpServletRequest request = getRequest("value", addrStr);

    MockHttpServletResponse response = newResponse();
    servlet.doGet(request, response);
    verify(wallet).getDelegatedResourceAccountIndex(eq(expectedAddress));
    String content = response.getContentAsString();
    assertFalse("Should not be empty", content.trim().isEmpty());
    assertFalse("Should not contain error", content.contains("\"Error\""));
  }
}
