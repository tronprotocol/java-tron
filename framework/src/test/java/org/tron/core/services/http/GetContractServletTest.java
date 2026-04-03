package org.tron.core.services.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.protobuf.ByteString;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.tron.api.GrpcAPI;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;

public class GetContractServletTest extends BaseHttpTest {

  private final byte[] address = new ECKey().getAddress();
  private final String addrStr = ByteArray.toHexString(address);
  private final GrpcAPI.BytesMessage expectedRequest = GrpcAPI.BytesMessage.newBuilder()
      .setValue(ByteString.copyFrom(address))
      .build();

  private GetContractServlet servlet;

  @Override
  protected void setUpMocks() throws Exception {
    servlet = new GetContractServlet();
    injectWallet(servlet);
  }

  @Test
  public void testPostFound() throws Exception {
    when(wallet.getContract(eq(expectedRequest))).thenReturn(
        SmartContract.newBuilder().setName("TestContract").build());

    MockHttpServletResponse response = newResponse();
    servlet.doPost(postRequest("{\"value\": \"" + addrStr + "\"}"), response);
    verify(wallet).getContract(eq(expectedRequest));
    String content = response.getContentAsString();
    assertFalse("Should not contain error", content.contains("\"Error\""));
    assertTrue("Should contain contract name", content.contains("TestContract"));
  }

  @Test
  public void testPostNotFound() throws Exception {
    when(wallet.getContract(eq(expectedRequest))).thenReturn(null);

    MockHttpServletResponse response = newResponse();
    servlet.doPost(postRequest("{\"value\": \"" + addrStr + "\"}"), response);
    verify(wallet).getContract(eq(expectedRequest));
    assertEquals(200, response.getStatus());
    assertEquals("{}" + System.lineSeparator(), response.getContentAsString());
    assertEquals("{}", response.getContentAsString().trim());
  }

  @Test
  public void testGetNotFound() throws Exception {
    when(wallet.getContract(eq(expectedRequest))).thenReturn(null);

    MockHttpServletResponse response = newResponse();
    servlet.doGet(getRequest("value", addrStr), response);
    verify(wallet).getContract(eq(expectedRequest));
    assertEquals(200, response.getStatus());
    assertEquals("{}" + System.lineSeparator(), response.getContentAsString());
    assertEquals("{}", response.getContentAsString().trim());

  }
}
