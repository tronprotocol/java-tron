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
import org.tron.protos.contract.Common.ResourceCode;

public class UnFreezeBalanceV2ServletTest extends BaseHttpTest {

  private UnFreezeBalanceV2Servlet servlet;
  private final String ownerAddr = ByteArray.toHexString(new ECKey().getAddress());

  @Override
  protected void setUpMocks() throws Exception {
    servlet = new UnFreezeBalanceV2Servlet();
    injectWallet(servlet);
    when(wallet.createTransactionCapsule(
        any(BalanceContract.UnfreezeBalanceV2Contract.class),
        eq(Protocol.Transaction.Contract.ContractType.UnfreezeBalanceV2Contract)))
        .thenReturn(new TransactionCapsule(MINIMAL_TX));
  }

  @Test
  public void testUnFreezeBalanceV2() throws Exception {
    String jsonParam = "{"
        + "\"owner_address\": \"" + ownerAddr + "\","
        + "\"unfreeze_balance\": 1000000,"
        + "\"resource\": \"ENERGY\""
        + "}";
    MockHttpServletRequest request = postRequest(jsonParam);

    MockHttpServletResponse response = newResponse();
    servlet.doPost(request, response);
    verify(wallet).createTransactionCapsule(
        argThat(c -> c instanceof BalanceContract.UnfreezeBalanceV2Contract
            && addressEquals(((BalanceContract.UnfreezeBalanceV2Contract) c)
                .getOwnerAddress(), ownerAddr)
            && ((BalanceContract.UnfreezeBalanceV2Contract) c).getUnfreezeBalance() == 1000000
            && ((BalanceContract.UnfreezeBalanceV2Contract) c)
                .getResource() == ResourceCode.ENERGY),
        eq(Protocol.Transaction.Contract.ContractType.UnfreezeBalanceV2Contract));
    assertTransactionResponse(response);
  }
}
