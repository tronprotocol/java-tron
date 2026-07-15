package org.tron.core.services.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;

public class ValidateAddressServletTest extends BaseHttpTest {

  private ValidateAddressServlet servlet;
  private final String validAddr = ByteArray.toHexString(new ECKey().getAddress());

  @Override
  protected void setUpMocks() throws Exception {
    servlet = new ValidateAddressServlet();
  }

  @Test
  public void testValidateAddressGet() throws Exception {
    MockHttpServletRequest request = getRequest("address", validAddr);

    MockHttpServletResponse response = newResponse();
    servlet.doGet(request, response);
    assertEquals(200, response.getStatus());
    String content = response.getContentAsString();
    assertTrue("Should report valid", content.contains("\"result\":true"));
    assertFalse("Should not contain Error", content.contains("\"Error\""));
  }

  @Test
  public void testValidateAddressPost() throws Exception {
    String json = "{\"address\": \"" + validAddr + "\"}";
    MockHttpServletRequest request = postRequest(json);

    MockHttpServletResponse response = newResponse();
    servlet.doPost(request, response);
    assertEquals(200, response.getStatus());
    String content = response.getContentAsString();
    assertTrue("Should report valid", content.contains("\"result\":true"));
  }

  @Test
  public void testValidateInvalidAddress() throws Exception {
    MockHttpServletRequest request = getRequest("address", "invalid_address");

    MockHttpServletResponse response = newResponse();
    servlet.doGet(request, response);
    assertEquals(200, response.getStatus());
    String content = response.getContentAsString();
    assertTrue("Should report invalid", content.contains("\"result\":false"));
    assertFalse("Should not report valid", content.contains("\"result\":true"));
  }
}
