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
import org.tron.protos.contract.ProposalContract;

public class ProposalCreateServletTest extends BaseHttpTest {

  private ProposalCreateServlet servlet;
  private final String ownerAddr = ByteArray.toHexString(new ECKey().getAddress());

  @Override
  protected void setUpMocks() throws Exception {
    servlet = new ProposalCreateServlet();
    injectWallet(servlet);
    when(wallet.createTransactionCapsule(
        any(ProposalContract.ProposalCreateContract.class),
        eq(Protocol.Transaction.Contract.ContractType.ProposalCreateContract)))
        .thenReturn(new TransactionCapsule(MINIMAL_TX));
  }

  @Test
  public void testProposalCreate() throws Exception {
    String jsonParam = "{"
        + "\"owner_address\": \"" + ownerAddr + "\","
        + "\"parameters\": [{\"key\": 0, \"value\": 100000}]"
        + "}";
    MockHttpServletRequest request = postRequest(jsonParam);

    MockHttpServletResponse response = newResponse();
    servlet.doPost(request, response);
    verify(wallet).createTransactionCapsule(
        argThat(c -> c instanceof ProposalContract.ProposalCreateContract
            && addressEquals(((ProposalContract.ProposalCreateContract) c)
                .getOwnerAddress(), ownerAddr)
            && ((ProposalContract.ProposalCreateContract) c).getParametersMap().size() == 1
            && ((ProposalContract.ProposalCreateContract) c)
                .getParametersMap().get(0L) == 100000),
        eq(Protocol.Transaction.Contract.ContractType.ProposalCreateContract));
    assertTransactionResponse(response);
  }
}
