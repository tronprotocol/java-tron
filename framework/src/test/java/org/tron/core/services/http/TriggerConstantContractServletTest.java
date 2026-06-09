package org.tron.core.services.http;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.TransactionCapsule;

public class TriggerConstantContractServletTest extends BaseHttpTest {

  private TriggerConstantContractServlet servlet;

  @Override
  protected void setUpMocks() throws Exception {
    servlet = new TriggerConstantContractServlet();
    injectWallet(servlet);
  }

  @Test
  public void testManyFlatFieldsDoesNotOverflowStack() throws Exception {
    String owner = ByteArray.toHexString(new ECKey().getAddress());
    String contract = ByteArray.toHexString(new ECKey().getAddress());

    StringBuilder body = new StringBuilder(256 * 1024)
        .append("{\"owner_address\":\"").append(owner).append('"')
        .append(",\"contract_address\":\"").append(contract).append('"')
        .append(",\"data\":\"00\"");
    for (int i = 0; i < 20_000; i++) {
      body.append(",\"x").append(i).append("\":1");
    }
    body.append('}');

    when(wallet.createTransactionCapsule(any(), any()))
        .thenReturn(new TransactionCapsule(MINIMAL_TX));
    when(wallet.triggerConstantContract(any(), any(), any(), any()))
        .thenReturn(MINIMAL_TX);

    MockHttpServletResponse response = newResponse();
    servlet.doPost(postRequest(body.toString()), response);

    assertEquals(200, response.getStatus());
    verify(wallet).triggerConstantContract(any(), any(), any(), any());
  }
}
