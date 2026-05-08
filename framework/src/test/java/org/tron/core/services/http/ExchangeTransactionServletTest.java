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
import org.tron.protos.contract.ExchangeContract;

public class ExchangeTransactionServletTest extends BaseHttpTest {

  private ExchangeTransactionServlet servlet;
  private final String ownerAddr = ByteArray.toHexString(new ECKey().getAddress());

  @Override
  protected void setUpMocks() throws Exception {
    servlet = new ExchangeTransactionServlet();
    injectWallet(servlet);
    when(wallet.createTransactionCapsule(
        any(ExchangeContract.ExchangeTransactionContract.class),
        eq(Protocol.Transaction.Contract.ContractType.ExchangeTransactionContract)))
        .thenReturn(new TransactionCapsule(MINIMAL_TX));
  }

  @Test
  public void testExchangeTransaction() throws Exception {
    String jsonParam = "{"
        + "\"owner_address\": \"" + ownerAddr + "\","
        + "\"exchange_id\": 1,"
        + "\"token_id\": \"5f\","
        + "\"quant\": 100,"
        + "\"expected\": 10"
        + "}";
    MockHttpServletRequest request = postRequest(jsonParam);

    MockHttpServletResponse response = newResponse();
    servlet.doPost(request, response);
    verify(wallet).createTransactionCapsule(
        argThat(c -> c instanceof ExchangeContract.ExchangeTransactionContract
            && addressEquals(((ExchangeContract.ExchangeTransactionContract) c)
                .getOwnerAddress(), ownerAddr)
            && ((ExchangeContract.ExchangeTransactionContract) c).getExchangeId() == 1
            && ((ExchangeContract.ExchangeTransactionContract) c).getQuant() == 100
            && ((ExchangeContract.ExchangeTransactionContract) c).getExpected() == 10
            && ((ExchangeContract.ExchangeTransactionContract) c)
                .getTokenId().toStringUtf8().equals("_")),
        eq(Protocol.Transaction.Contract.ContractType.ExchangeTransactionContract));
    assertTransactionResponse(response);
  }
}
