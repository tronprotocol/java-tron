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

public class FreezeBalanceV2ServletTest extends BaseHttpTest {

  private FreezeBalanceV2Servlet servlet;
  private final String ownerAddr = ByteArray.toHexString(new ECKey().getAddress());

  @Override
  protected void setUpMocks() throws Exception {
    servlet = new FreezeBalanceV2Servlet();
    injectWallet(servlet);
    when(wallet.createTransactionCapsule(
        any(BalanceContract.FreezeBalanceV2Contract.class),
        eq(Protocol.Transaction.Contract.ContractType.FreezeBalanceV2Contract)))
        .thenReturn(new TransactionCapsule(MINIMAL_TX));
  }

  @Test
  public void testFreezeBalanceV2() throws Exception {
    String jsonParam = "{"
        + "\"owner_address\": \"" + ownerAddr + "\","
        + "\"frozen_balance\": 1000000,"
        + "\"resource\": \"ENERGY\""
        + "}";
    MockHttpServletRequest request = postRequest(jsonParam);

    MockHttpServletResponse response = newResponse();
    servlet.doPost(request, response);
    verify(wallet).createTransactionCapsule(
        argThat(c -> c instanceof BalanceContract.FreezeBalanceV2Contract
            && addressEquals(((BalanceContract.FreezeBalanceV2Contract) c)
                .getOwnerAddress(), ownerAddr)
            && ((BalanceContract.FreezeBalanceV2Contract) c).getFrozenBalance() == 1000000
            && ((BalanceContract.FreezeBalanceV2Contract) c)
                .getResource() == ResourceCode.ENERGY),
        eq(Protocol.Transaction.Contract.ContractType.FreezeBalanceV2Contract));
    assertTransactionResponse(response);
  }
}
