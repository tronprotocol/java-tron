package org.tron.core.services.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.tron.protos.Protocol.Block;

public class GetBlockServletTest extends BaseHttpTest {

  private GetBlockServlet servlet;

  @Override
  protected void setUpMocks() throws Exception {
    servlet = new GetBlockServlet();
    injectWallet(servlet);
    when(wallet.getBlock(org.mockito.ArgumentMatchers.argThat(
        req -> req != null && "0".equals(req.getIdOrNum()) && !req.getDetail())))
        .thenReturn(Block.getDefaultInstance());
  }

  @Test
  public void testGetBlockPost() throws Exception {
    String jsonParam = "{\"id_or_num\": \"0\", \"detail\": false}";
    MockHttpServletRequest request = postRequest(jsonParam);

    MockHttpServletResponse response = newResponse();
    servlet.doPost(request, response);
    assertEquals(200, response.getStatus());
    assertTrue(response.getContentAsString().contains("blockID"));
  }

  @Test
  public void testGetBlockGet() throws Exception {
    MockHttpServletRequest request = getRequest("id_or_num", "0", "detail", "false");

    MockHttpServletResponse response = newResponse();
    servlet.doGet(request, response);
    assertEquals(200, response.getStatus());
    assertTrue(response.getContentAsString().contains("blockID"));
  }
}
