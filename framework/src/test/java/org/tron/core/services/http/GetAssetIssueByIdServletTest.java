package org.tron.core.services.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.tron.protos.contract.AssetIssueContractOuterClass.AssetIssueContract;

public class GetAssetIssueByIdServletTest extends BaseHttpTest {

  private GetAssetIssueByIdServlet servlet;

  @Override
  protected void setUpMocks() throws Exception {
    servlet = new GetAssetIssueByIdServlet();
    injectWallet(servlet);
    when(wallet.getAssetIssueById(any())).thenReturn(null);
  }

  @Test
  public void testGetAssetIssueByIdPost() throws Exception {
    String jsonParam = "{\"value\": \"100001\"}";
    MockHttpServletRequest request = postRequest(jsonParam);

    MockHttpServletResponse response = newResponse();
    servlet.doPost(request, response);
    verify(wallet).getAssetIssueById(eq("100001"));
    assertEquals(200, response.getStatus());
    String content = response.getContentAsString();
    assertFalse("Should not contain error", content.contains("\"Error\""));
    assertEquals("{}" + System.lineSeparator(), content);
  }

  @Test
  public void testGetAssetIssueByIdGet() throws Exception {
    MockHttpServletRequest request = getRequest("value", "100001");

    MockHttpServletResponse response = newResponse();
    servlet.doGet(request, response);
    verify(wallet).getAssetIssueById(eq("100001"));
    assertEquals(200, response.getStatus());
    String content = response.getContentAsString();
    assertFalse("Should not contain error", content.contains("\"Error\""));
    assertEquals("{}" + System.lineSeparator(), content);
  }

  @Test
  public void testPostReturnsAssetWhenFound() throws Exception {
    AssetIssueContract asset = AssetIssueContract.newBuilder()
        .setId("100001")
        .setTotalSupply(1000L)
        .build();
    when(wallet.getAssetIssueById(eq("100001"))).thenReturn(asset);

    MockHttpServletRequest request = postRequest("{\"value\": \"100001\"}");
    MockHttpServletResponse response = newResponse();
    servlet.doPost(request, response);
    assertEquals(200, response.getStatus());
    String content = response.getContentAsString();
    assertTrue("Should contain id", content.contains("100001"));
    assertTrue("Should contain total_supply", content.contains("total_supply"));
  }
}
