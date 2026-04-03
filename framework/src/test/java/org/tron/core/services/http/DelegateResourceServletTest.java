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

public class DelegateResourceServletTest extends BaseHttpTest {

  private DelegateResourceServlet servlet;
  private final String ownerAddr = ByteArray.toHexString(new ECKey().getAddress());
  private final String receiverAddr = ByteArray.toHexString(new ECKey().getAddress());

  @Override
  protected void setUpMocks() throws Exception {
    servlet = new DelegateResourceServlet();
    injectWallet(servlet);
    when(wallet.createTransactionCapsule(
        any(BalanceContract.DelegateResourceContract.class),
        eq(Protocol.Transaction.Contract.ContractType.DelegateResourceContract)))
        .thenReturn(new TransactionCapsule(MINIMAL_TX));
  }

  @Test
  public void testDelegateResource() throws Exception {
    String jsonParam = "{"
        + "\"owner_address\": \"" + ownerAddr + "\","
        + "\"receiver_address\": \"" + receiverAddr + "\","
        + "\"balance\": 1000000,"
        + "\"resource\": \"ENERGY\""
        + "}";
    MockHttpServletRequest request = postRequest(jsonParam);

    MockHttpServletResponse response = newResponse();
    servlet.doPost(request, response);
    verify(wallet).createTransactionCapsule(
        argThat(c -> c instanceof BalanceContract.DelegateResourceContract
            && addressEquals(((BalanceContract.DelegateResourceContract) c)
                .getOwnerAddress(), ownerAddr)
            && addressEquals(((BalanceContract.DelegateResourceContract) c)
                .getReceiverAddress(), receiverAddr)
            && ((BalanceContract.DelegateResourceContract) c).getBalance() == 1000000
            && ((BalanceContract.DelegateResourceContract) c)
                .getResource() == ResourceCode.ENERGY),
        eq(Protocol.Transaction.Contract.ContractType.DelegateResourceContract));
    assertTransactionResponse(response);
  }
}
