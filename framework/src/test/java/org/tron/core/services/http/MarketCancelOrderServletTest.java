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
import org.tron.protos.contract.MarketContract.MarketCancelOrderContract;

public class MarketCancelOrderServletTest extends BaseHttpTest {

  private MarketCancelOrderServlet servlet;
  private final String ownerAddr = ByteArray.toHexString(new ECKey().getAddress());

  @Override
  protected void setUpMocks() throws Exception {
    servlet = new MarketCancelOrderServlet();
    injectWallet(servlet);
    when(wallet.createTransactionCapsule(
        any(MarketCancelOrderContract.class), eq(ContractType.MarketCancelOrderContract)))
        .thenReturn(new TransactionCapsule(MINIMAL_TX));
  }

  @Test
  public void testMarketCancelOrder() throws Exception {
    String jsonParam = "{"
        + "\"owner_address\": \"" + ownerAddr + "\","
        + "\"order_id\": \"0000000000000000000000000000000000000000000000000000000000000001\""
        + "}";
    MockHttpServletRequest request = postRequest(jsonParam);

    MockHttpServletResponse response = newResponse();
    servlet.doPost(request, response);
    verify(wallet).createTransactionCapsule(
        argThat(c -> c instanceof MarketCancelOrderContract
            && addressEquals(((MarketCancelOrderContract) c)
                .getOwnerAddress(), ownerAddr)
            && ((MarketCancelOrderContract) c).getOrderId().size() == 32),
        eq(ContractType.MarketCancelOrderContract));
    assertTransactionResponse(response);
  }
}
