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

public class ExchangeWithdrawServletTest extends BaseHttpTest {

  private ExchangeWithdrawServlet servlet;
  private final String ownerAddr = ByteArray.toHexString(new ECKey().getAddress());

  @Override
  protected void setUpMocks() throws Exception {
    servlet = new ExchangeWithdrawServlet();
    injectWallet(servlet);
    when(wallet.createTransactionCapsule(
        any(ExchangeContract.ExchangeWithdrawContract.class),
        eq(Protocol.Transaction.Contract.ContractType.ExchangeWithdrawContract)))
        .thenReturn(new TransactionCapsule(MINIMAL_TX));
  }

  @Test
  public void testExchangeWithdraw() throws Exception {
    String jsonParam = "{"
        + "\"owner_address\": \"" + ownerAddr + "\","
        + "\"exchange_id\": 1,"
        + "\"token_id\": \"5f\","
        + "\"quant\": 50"
        + "}";
    MockHttpServletRequest request = postRequest(jsonParam);

    MockHttpServletResponse response = newResponse();
    servlet.doPost(request, response);
    verify(wallet).createTransactionCapsule(
        argThat(c -> c instanceof ExchangeContract.ExchangeWithdrawContract
            && addressEquals(((ExchangeContract.ExchangeWithdrawContract) c)
                .getOwnerAddress(), ownerAddr)
            && ((ExchangeContract.ExchangeWithdrawContract) c).getExchangeId() == 1
            && ((ExchangeContract.ExchangeWithdrawContract) c).getQuant() == 50
            && ((ExchangeContract.ExchangeWithdrawContract) c)
                .getTokenId().toStringUtf8().equals("_")),
        eq(Protocol.Transaction.Contract.ContractType.ExchangeWithdrawContract));
    assertTransactionResponse(response);
  }
}
