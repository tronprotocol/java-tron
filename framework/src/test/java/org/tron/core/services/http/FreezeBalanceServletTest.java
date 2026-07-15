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

public class FreezeBalanceServletTest extends BaseHttpTest {

  private FreezeBalanceServlet servlet;
  private final String ownerAddr = ByteArray.toHexString(new ECKey().getAddress());

  @Override
  protected void setUpMocks() throws Exception {
    servlet = new FreezeBalanceServlet();
    injectWallet(servlet);
    when(wallet.createTransactionCapsule(
        any(BalanceContract.FreezeBalanceContract.class),
        eq(Protocol.Transaction.Contract.ContractType.FreezeBalanceContract)))
        .thenReturn(new TransactionCapsule(MINIMAL_TX));
  }

  @Test
  public void testFreezeBalance() throws Exception {
    String jsonParam = "{"
        + "\"owner_address\": \"" + ownerAddr + "\","
        + "\"frozen_balance\": 1000000,"
        + "\"frozen_duration\": 3"
        + "}";
    MockHttpServletRequest request = postRequest(jsonParam);

    MockHttpServletResponse response = newResponse();
    servlet.doPost(request, response);
    verify(wallet).createTransactionCapsule(
        argThat(c -> c instanceof BalanceContract.FreezeBalanceContract
            && addressEquals(((BalanceContract.FreezeBalanceContract) c)
                .getOwnerAddress(), ownerAddr)
            && ((BalanceContract.FreezeBalanceContract) c).getFrozenBalance() == 1000000
            && ((BalanceContract.FreezeBalanceContract) c).getFrozenDuration() == 3),
        eq(Protocol.Transaction.Contract.ContractType.FreezeBalanceContract));
    assertTransactionResponse(response);
  }
}
