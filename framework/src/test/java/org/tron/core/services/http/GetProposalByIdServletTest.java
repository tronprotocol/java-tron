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
import org.tron.protos.Protocol.Proposal;

public class GetProposalByIdServletTest extends BaseHttpTest {

  private GetProposalByIdServlet servlet;
  private final ByteString proposalId =
      ByteString.copyFrom(ByteArray.fromLong(1L));

  @Override
  protected void setUpMocks() throws Exception {
    servlet = new GetProposalByIdServlet();
    injectWallet(servlet);
    when(wallet.getProposalById(any())).thenReturn(null);
  }

  @Test
  public void testPost() throws Exception {
    String jsonParam = "{\"id\": 1}";
    MockHttpServletRequest request = postRequest(jsonParam);

    MockHttpServletResponse response = newResponse();
    servlet.doPost(request, response);
    verify(wallet).getProposalById(eq(proposalId));
    assertEquals(200, response.getStatus());
    String content = response.getContentAsString();
    assertFalse("Should not contain error", content.contains("\"Error\""));
    assertEquals("{}" + System.lineSeparator(), content);
  }

  @Test
  public void testGet() throws Exception {
    MockHttpServletRequest request = getRequest("id", "1");

    MockHttpServletResponse response = newResponse();
    servlet.doGet(request, response);
    verify(wallet).getProposalById(eq(proposalId));
    assertEquals(200, response.getStatus());
    String content = response.getContentAsString();
    assertFalse("Should not contain error", content.contains("\"Error\""));
    assertEquals("{}" + System.lineSeparator(), content);
  }

  @Test
  public void testPostReturnsProposalWhenFound() throws Exception {
    Proposal proposal = Proposal.newBuilder().setProposalId(1L).build();
    when(wallet.getProposalById(eq(proposalId))).thenReturn(proposal);

    MockHttpServletRequest request = postRequest("{\"id\": 1}");
    MockHttpServletResponse response = newResponse();
    servlet.doPost(request, response);
    assertEquals(200, response.getStatus());
    String content = response.getContentAsString();
    assertTrue("Should contain proposal_id", content.contains("proposal_id"));
  }
}
