package org.tron.core.services.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.protobuf.ByteString;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.tron.api.GrpcAPI;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import org.tron.protos.contract.SmartContractOuterClass.SmartContractDataWrapper;

public class GetContractInfoServletTest extends BaseHttpTest {

  private GetContractInfoServlet servlet;
  private final byte[] address = new ECKey().getAddress();
  private final String addrStr = ByteArray.toHexString(address);
  private final GrpcAPI.BytesMessage expectedRequest = GrpcAPI.BytesMessage.newBuilder()
      .setValue(ByteString.copyFrom(address))
      .build();

  @Override
  protected void setUpMocks() throws Exception {
    servlet = new GetContractInfoServlet();
    injectWallet(servlet);
  }

  @Test
  public void testGetContractInfoPost() throws Exception {
    when(wallet.getContractInfo(eq(expectedRequest))).thenReturn(
        SmartContractDataWrapper.newBuilder()
            .setSmartContract(SmartContract.newBuilder().setName("TestContract").build())
            .build());
    String jsonParam = "{\"value\": \"" + addrStr + "\"}";
    MockHttpServletRequest request = postRequest(jsonParam);

    MockHttpServletResponse response = newResponse();
    servlet.doPost(request, response);
    assertEquals(200, response.getStatus());
    assertTrue(response.getContentAsString().contains("TestContract"));
  }

  @Test
  public void testGetContractInfoGet() throws Exception {
    when(wallet.getContractInfo(eq(expectedRequest))).thenReturn(null);
    MockHttpServletRequest request = getRequest("value", addrStr);

    MockHttpServletResponse response = newResponse();
    servlet.doGet(request, response);
    verify(wallet).getContractInfo(eq(expectedRequest));
    assertEquals(200, response.getStatus());
    assertEquals("{}", response.getContentAsString().trim());
  }
}
