package org.tron.core.services.http;

import static org.junit.Assert.assertEquals;
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
import org.tron.common.utils.ByteArray;
import org.tron.protos.contract.AssetIssueContractOuterClass.AssetIssueContract;

public class GetAssetIssueByNameServletTest extends BaseHttpTest {

  private GetAssetIssueByNameServlet servlet;
  private final ByteString data =
      ByteString.copyFrom(ByteArray.fromHexString("74657374"));

  @Override
  protected void setUpMocks() throws Exception {
    servlet = new GetAssetIssueByNameServlet();
    injectWallet(servlet);
    when(wallet.getAssetIssueByName(any())).thenReturn(null);
  }

  @Test
  public void testPost() throws Exception {
    String jsonParam = "{\"value\": \"74657374\"}";
    MockHttpServletRequest request = postRequest(jsonParam);

    MockHttpServletResponse response = newResponse();
    servlet.doPost(request, response);
    verify(wallet).getAssetIssueByName(eq(data));
    String content = response.getContentAsString();
    assertFalse("Should not contain error", content.contains("\"Error\""));
    assertEquals("{}" + System.lineSeparator(), content);
  }

  @Test
  public void testGet() throws Exception {
    MockHttpServletRequest request = getRequest("value", "74657374");

    MockHttpServletResponse response = newResponse();
    servlet.doGet(request, response);
    verify(wallet).getAssetIssueByName(eq(data));
    String content = response.getContentAsString();
    assertFalse("Should not contain error", content.contains("\"Error\""));
    assertEquals("{}" + System.lineSeparator(), content);
  }

  @Test
  public void testPostReturnsAssetWhenFound() throws Exception {
    AssetIssueContract asset = AssetIssueContract.newBuilder()
        .setName(data)
        .setTotalSupply(5000L)
        .build();
    when(wallet.getAssetIssueByName(eq(data))).thenReturn(asset);

    MockHttpServletRequest request = postRequest("{\"value\": \"74657374\"}");
    MockHttpServletResponse response = newResponse();
    servlet.doPost(request, response);
    assertEquals(200, response.getStatus());
    String content = response.getContentAsString();
    assertTrue("Should contain total_supply", content.contains("total_supply"));
  }
}
