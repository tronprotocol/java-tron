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
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.contract.ExchangeContract.ExchangeCreateContract;

public class ExchangeCreateServletTest extends BaseHttpTest {

  private ExchangeCreateServlet servlet;
  private final String ownerAddr = ByteArray.toHexString(new ECKey().getAddress());

  @Override
  protected void setUpMocks() throws Exception {
    servlet = new ExchangeCreateServlet();
    injectWallet(servlet);
    when(wallet.createTransactionCapsule(
        any(ExchangeCreateContract.class), eq(ContractType.ExchangeCreateContract)))
        .thenReturn(new TransactionCapsule(MINIMAL_TX));
  }

  @Test
  public void testExchangeCreate() throws Exception {
    String jsonParam = "{"
        + "\"owner_address\": \"" + ownerAddr + "\","
        + "\"first_token_id\": \"5f\","
        + "\"first_token_balance\": 100,"
        + "\"second_token_id\": \"61\","
        + "\"second_token_balance\": 200"
        + "}";
    MockHttpServletRequest request = postRequest(jsonParam);

    MockHttpServletResponse response = newResponse();
    servlet.doPost(request, response);
    verify(wallet).createTransactionCapsule(
        argThat(c -> c instanceof ExchangeCreateContract
            && addressEquals(((ExchangeCreateContract) c)
                .getOwnerAddress(), ownerAddr)
            && ((ExchangeCreateContract) c).getFirstTokenBalance() == 100
            && ((ExchangeCreateContract) c).getSecondTokenBalance() == 200
            && ((ExchangeCreateContract) c)
                .getFirstTokenId().toStringUtf8().equals("_")
            && ((ExchangeCreateContract) c)
                .getSecondTokenId().toStringUtf8().equals("a")),
        eq(ContractType.ExchangeCreateContract));
    assertTransactionResponse(response);
  }
}
