package org.tron.core.services.http;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.protobuf.ByteString;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.tron.api.GrpcAPI.AssetIssueList;
import org.tron.common.utils.ByteArray;

public class GetAssetIssueListByNameServletTest extends BaseHttpTest {

  private GetAssetIssueListByNameServlet servlet;
  private final ByteString data = ByteString.copyFrom(ByteArray.fromHexString("74657374"));

  @Override
  protected void setUpMocks() throws Exception {
    servlet = new GetAssetIssueListByNameServlet();
    injectWallet(servlet);
    when(wallet.getAssetIssueListByName(eq(data)))
        .thenReturn(AssetIssueList.getDefaultInstance());
  }

  @Test
  public void testPostNoMatchingAssets() throws Exception {
    String jsonParam = "{\"value\": \"74657374\"}";
    MockHttpServletRequest request = postRequest(jsonParam);

    MockHttpServletResponse response = newResponse();
    servlet.doPost(request, response);
    verify(wallet).getAssetIssueListByName(eq(data));
    assertEquals("{}" + System.lineSeparator(), response.getContentAsString());
  }

  @Test
  public void testGetNoMatchingAssets() throws Exception {
    MockHttpServletRequest request = getRequest("value", "74657374");

    MockHttpServletResponse response = newResponse();
    servlet.doGet(request, response);
    verify(wallet).getAssetIssueListByName(eq(data));
    assertEquals("{}" + System.lineSeparator(), response.getContentAsString());
  }

  @Test
  public void testPostEmptyNameReturnsEmptyObject() throws Exception {
    when(wallet.getAssetIssueListByName(eq(ByteString.EMPTY))).thenReturn(null);
    MockHttpServletResponse response = newResponse();

    servlet.doPost(postRequest("{\"value\":\"\"}"), response);

    verify(wallet).getAssetIssueListByName(eq(ByteString.EMPTY));
    assertEquals("{}" + System.lineSeparator(), response.getContentAsString());
  }

  @Test
  public void testGetEmptyNameReturnsEmptyObject() throws Exception {
    when(wallet.getAssetIssueListByName(eq(ByteString.EMPTY))).thenReturn(null);
    MockHttpServletResponse response = newResponse();

    servlet.doGet(getRequest("value", ""), response);

    verify(wallet).getAssetIssueListByName(eq(ByteString.EMPTY));
    assertEquals("{}" + System.lineSeparator(), response.getContentAsString());
  }
}
