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
import org.tron.protos.contract.BalanceContract;

public class TransferServletTest extends BaseHttpTest {

  private TransferServlet servlet;
  private final String ownerAddr = ByteArray.toHexString(new ECKey().getAddress());
  private final String toAddr = ByteArray.toHexString(new ECKey().getAddress());

  @Override
  protected void setUpMocks() throws Exception {
    servlet = new TransferServlet();
    injectWallet(servlet);
    when(wallet.createTransactionCapsule(
        any(BalanceContract.TransferContract.class),
        eq(Protocol.Transaction.Contract.ContractType.TransferContract)))
        .thenReturn(new TransactionCapsule(MINIMAL_TX));
  }

  @Test
  public void testTransfer() throws Exception {
    String jsonParam = "{"
        + "\"owner_address\": \"" + ownerAddr + "\","
        + "\"to_address\": \"" + toAddr + "\","
        + "\"amount\": 100"
        + "}";
    MockHttpServletRequest request = postRequest(jsonParam);

    MockHttpServletResponse response = newResponse();
    servlet.doPost(request, response);
    verify(wallet).createTransactionCapsule(
        argThat(c -> c instanceof BalanceContract.TransferContract
            && addressEquals(((BalanceContract.TransferContract) c)
                .getOwnerAddress(), ownerAddr)
            && addressEquals(((BalanceContract.TransferContract) c)
                .getToAddress(), toAddr)
            && ((BalanceContract.TransferContract) c).getAmount() == 100),
        eq(Protocol.Transaction.Contract.ContractType.TransferContract));
    assertTransactionResponse(response);
  }
}
