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

public class CancelAllUnfreezeV2ServletTest extends BaseHttpTest {

  private CancelAllUnfreezeV2Servlet servlet;
  private final String ownerAddr = ByteArray.toHexString(new ECKey().getAddress());

  @Override
  protected void setUpMocks() throws Exception {
    servlet = new CancelAllUnfreezeV2Servlet();
    injectWallet(servlet);
    when(wallet.createTransactionCapsule(
        any(BalanceContract.CancelAllUnfreezeV2Contract.class),
        eq(Protocol.Transaction.Contract.ContractType.CancelAllUnfreezeV2Contract)))
        .thenReturn(new TransactionCapsule(MINIMAL_TX));
  }

  @Test
  public void testCancelAllUnfreezeV2() throws Exception {
    String jsonParam = "{\"owner_address\": \"" + ownerAddr + "\"}";
    MockHttpServletRequest request = postRequest(jsonParam);

    MockHttpServletResponse response = newResponse();
    servlet.doPost(request, response);
    verify(wallet).createTransactionCapsule(
        argThat(c -> c instanceof BalanceContract.CancelAllUnfreezeV2Contract
            && addressEquals(((BalanceContract.CancelAllUnfreezeV2Contract) c)
                .getOwnerAddress(), ownerAddr)),
        eq(Protocol.Transaction.Contract.ContractType.CancelAllUnfreezeV2Contract));
    assertTransactionResponse(response);
  }
}
