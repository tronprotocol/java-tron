package org.tron.core.services.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.protobuf.ByteString;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.StringUtil;
import org.tron.protos.Protocol.Account;

public class GetAccountServletTest extends BaseHttpTest {

  private GetAccountServlet servlet;
  private final byte[] address = new ECKey().getAddress();
  private final String addrStr = ByteArray.toHexString(address);
  private final ByteString addr = ByteString.copyFrom(address);

  @Override
  protected void setUpMocks() throws Exception {
    servlet = new GetAccountServlet();
    injectWallet(servlet);
    when(wallet.getAccount(any(Account.class))).thenReturn(
        Account.newBuilder().setAddress(addr).build());
  }

  @Test
  public void testGetAccountPost() throws Exception {
    String jsonParam = "{\"address\": \"" + addrStr + "\"}";
    MockHttpServletRequest request = postRequest(jsonParam);

    MockHttpServletResponse response = newResponse();
    servlet.doPost(request, response);
    assertEquals(200, response.getStatus());
    verify(wallet).getAccount(argThat(req -> req != null && req.getAddress().equals(addr)));
    String content = response.getContentAsString();
    assertFalse("Should not contain error", content.contains("\"Error\""));
    assertTrue("Should contain address", content.contains("address"));
  }

  @Test
  public void testGetAccountGet() throws Exception {
    MockHttpServletRequest request = getRequest("address", addrStr);

    MockHttpServletResponse response = newResponse();
    servlet.doGet(request, response);
    assertEquals(200, response.getStatus());
    verify(wallet).getAccount(argThat(req -> req != null && req.getAddress().equals(addr)));
    String content = response.getContentAsString();
    assertFalse("Should not contain error", content.contains("\"Error\""));
    assertTrue("Should contain address", content.contains("address"));
  }

  @Test
  public void testGetAccountPostOversizedBase58Address() throws Exception {
    String oversized = addrStr + "0";
    String jsonParam = "{\"address\": \"" + oversized + "\", \"visible\": true}";
    MockHttpServletRequest request = postRequest(jsonParam);
    MockHttpServletResponse response = newResponse();
    servlet.doPost(request, response);
    String content = response.getContentAsString();
    // null from decodeFromBase58Check now surfaces a clear "invalid address" error instead of a
    // bare NullPointerException (JsonFormat checks for null before ByteString.copyFrom).
    assertTrue("oversized address should surface a clear invalid-address error: " + content,
        content.contains("invalid address"));
  }

  @Test
  public void testGetAccountPostVisibleBase58Address() throws Exception {
    // visible=true happy path: a valid 34-char Base58 address decodes back to the same 21 bytes
    // (decodeFromBase58Check success path) and returns the account with no error.
    String base58Addr = StringUtil.encode58Check(address);
    String jsonParam = "{\"address\": \"" + base58Addr + "\", \"visible\": true}";
    MockHttpServletRequest request = postRequest(jsonParam);

    MockHttpServletResponse response = newResponse();
    servlet.doPost(request, response);
    assertEquals(200, response.getStatus());
    verify(wallet).getAccount(argThat(req -> req != null && req.getAddress().equals(addr)));
    String content = response.getContentAsString();
    assertFalse("Should not contain error", content.contains("\"Error\""));
    assertTrue("Should contain address", content.contains("address"));
  }
}
