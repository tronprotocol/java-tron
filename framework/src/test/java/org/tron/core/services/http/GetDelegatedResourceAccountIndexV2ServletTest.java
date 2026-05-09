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
import org.tron.common.utils.ByteArray;
import org.tron.protos.Protocol.DelegatedResourceAccountIndex;

public class GetDelegatedResourceAccountIndexV2ServletTest extends BaseHttpTest {

  private GetDelegatedResourceAccountIndexV2Servlet servlet;
  ByteString expectedAddress = ByteString.copyFrom(
      ByteArray.fromHexString(
          Util.getHexAddress("TBxSocpujP6UGKV5ydXNVTDQz7fAgdmoaB")));

  @Override
  protected void setUpMocks() throws Exception {
    servlet = new GetDelegatedResourceAccountIndexV2Servlet();
    injectWallet(servlet);
    when(wallet.getDelegatedResourceAccountIndexV2(any()))
        .thenReturn(DelegatedResourceAccountIndex.getDefaultInstance());
  }

  @Test
  public void testPost() throws Exception {
    String jsonParam = "{\"visible\": true, \"value\": \"TBxSocpujP6UGKV5ydXNVTDQz7fAgdmoaB\"}";
    MockHttpServletRequest request = postRequest(jsonParam);

    MockHttpServletResponse response = newResponse();
    servlet.doPost(request, response);

    verify(wallet).getDelegatedResourceAccountIndexV2(eq(expectedAddress));
    String postContent = response.getContentAsString();
    assertFalse(postContent.contains("\"Error\""));
    assertFalse(postContent.trim().isEmpty());
  }

  @Test
  public void testGet() throws Exception {
    MockHttpServletRequest request =
        getRequest("visible", "true", "value", "TBxSocpujP6UGKV5ydXNVTDQz7fAgdmoaB");

    MockHttpServletResponse response = newResponse();
    servlet.doGet(request, response);
    verify(wallet).getDelegatedResourceAccountIndexV2(eq(expectedAddress));
    String getContent = response.getContentAsString();
    assertFalse(getContent.contains("\"Error\""));
    assertFalse(getContent.trim().isEmpty());
  }
}
