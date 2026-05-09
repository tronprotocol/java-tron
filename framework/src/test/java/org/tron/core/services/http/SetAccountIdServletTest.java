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
import org.tron.protos.contract.AccountContract;

public class SetAccountIdServletTest extends BaseHttpTest {

  private SetAccountIdServlet servlet;
  private final String ownerAddr = ByteArray.toHexString(new ECKey().getAddress());

  @Override
  protected void setUpMocks() throws Exception {
    servlet = new SetAccountIdServlet();
    injectWallet(servlet);
    when(wallet.createTransactionCapsule(any(AccountContract.SetAccountIdContract.class), eq(
        Protocol.Transaction.Contract.ContractType.SetAccountIdContract)))
        .thenReturn(new TransactionCapsule(MINIMAL_TX));
  }

  @Test
  public void testSetAccountId() throws Exception {
    String jsonParam = "{"
        + "\"owner_address\": \"" + ownerAddr + "\","
        + "\"account_id\": \"6161616162626262\""
        + "}";
    MockHttpServletRequest request = postRequest(jsonParam);

    MockHttpServletResponse response = newResponse();
    servlet.doPost(request, response);
    verify(wallet).createTransactionCapsule(
        argThat(c -> c instanceof AccountContract.SetAccountIdContract
            && addressEquals(((AccountContract.SetAccountIdContract) c)
                .getOwnerAddress(), ownerAddr)
            && ((AccountContract.SetAccountIdContract) c)
                .getAccountId().toStringUtf8().equals("aaaabbbb")),
        eq(Protocol.Transaction.Contract.ContractType.SetAccountIdContract));
    assertTransactionResponse(response);
  }
}
