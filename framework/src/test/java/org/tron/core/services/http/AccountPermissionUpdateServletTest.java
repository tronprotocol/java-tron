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

public class AccountPermissionUpdateServletTest extends BaseHttpTest {

  private AccountPermissionUpdateServlet servlet;
  private final String ownerAddr = ByteArray.toHexString(new ECKey().getAddress());

  @Override
  protected void setUpMocks() throws Exception {
    servlet = new AccountPermissionUpdateServlet();
    injectWallet(servlet);
    when(wallet.createTransactionCapsule(
        any(AccountContract.AccountPermissionUpdateContract.class),
        eq(Protocol.Transaction.Contract.ContractType.AccountPermissionUpdateContract)))
        .thenReturn(new TransactionCapsule(MINIMAL_TX));
  }

  @Test
  public void testAccountPermissionUpdate() throws Exception {
    String jsonParam = "{"
        + "\"owner_address\": \"" + ownerAddr + "\","
        + "\"owner\": {\"type\": 0, \"permission_name\": \"owner\", \"threshold\": 1,"
        + " \"keys\": [{\"address\": \"" + ownerAddr + "\","
        + " \"weight\": 1}]},"
        + "\"actives\": [{\"type\": 2, \"permission_name\": \"active\", \"threshold\": 1,"
        + " \"operations\": \"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
        + " \"keys\": [{\"address\": \"" + ownerAddr + "\","
        + " \"weight\": 1}]}]"
        + "}";
    MockHttpServletRequest request = postRequest(jsonParam);

    MockHttpServletResponse response = newResponse();
    servlet.doPost(request, response);
    verify(wallet).createTransactionCapsule(
        argThat(c -> c instanceof AccountContract.AccountPermissionUpdateContract
            && addressEquals(((AccountContract.AccountPermissionUpdateContract) c)
                .getOwnerAddress(), ownerAddr)
            && ((AccountContract.AccountPermissionUpdateContract) c)
                .getOwner().getThreshold() == 1
            && ((AccountContract.AccountPermissionUpdateContract) c)
                .getActivesCount() == 1),
        eq(Protocol.Transaction.Contract.ContractType.AccountPermissionUpdateContract));
    assertTransactionResponse(response);
  }
}
