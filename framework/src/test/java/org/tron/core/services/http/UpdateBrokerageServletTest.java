package org.tron.core.services.http;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.protos.Protocol;
import org.tron.protos.contract.StorageContract;

public class UpdateBrokerageServletTest extends BaseHttpTest {

  private UpdateBrokerageServlet servlet;
  private final String ownerAddr = ByteArray.toHexString(new ECKey().getAddress());

  @Override
  protected void setUpMocks() throws Exception {
    servlet = new UpdateBrokerageServlet();
    injectWallet(servlet);
    when(wallet.createTransactionCapsule(
        any(StorageContract.UpdateBrokerageContract.class),
        eq(Protocol.Transaction.Contract.ContractType.UpdateBrokerageContract)))
        .thenReturn(new TransactionCapsule(MINIMAL_TX));
  }

  @Test
  public void testUpdateBrokerage() throws Exception {
    String jsonParam = "{"
        + "\"owner_address\": \"" + ownerAddr + "\","
        + "\"brokerage\": 20"
        + "}";
    MockHttpServletRequest request = postRequest(jsonParam);

    MockHttpServletResponse response = newResponse();
    servlet.doPost(request, response);
    verify(wallet).createTransactionCapsule(
        argThat(c -> c instanceof StorageContract.UpdateBrokerageContract
            && addressEquals(((StorageContract.UpdateBrokerageContract) c)
                .getOwnerAddress(), ownerAddr)
            && ((StorageContract.UpdateBrokerageContract) c).getBrokerage() == 20),
        eq(Protocol.Transaction.Contract.ContractType.UpdateBrokerageContract));
    assertTransactionResponse(response);
  }
}
