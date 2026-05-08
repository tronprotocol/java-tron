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

public class ProposalApproveServletTest extends BaseHttpTest {

  private ProposalApproveServlet servlet;
  private final String ownerAddr = ByteArray.toHexString(new ECKey().getAddress());

  @Override
  protected void setUpMocks() throws Exception {
    servlet = new ProposalApproveServlet();
    injectWallet(servlet);
    when(wallet.createTransactionCapsule(
        any(ProposalContract.ProposalApproveContract.class),
        eq(Protocol.Transaction.Contract.ContractType.ProposalApproveContract)))
        .thenReturn(new TransactionCapsule(MINIMAL_TX));
  }

  @Test
  public void testProposalApprove() throws Exception {
    String jsonParam = "{"
        + "\"owner_address\": \"" + ownerAddr + "\","
        + "\"proposal_id\": 1,"
        + "\"is_add_approval\": true"
        + "}";
    MockHttpServletRequest request = postRequest(jsonParam);

    MockHttpServletResponse response = newResponse();
    servlet.doPost(request, response);
    verify(wallet).createTransactionCapsule(
        argThat(c -> c instanceof ProposalContract.ProposalApproveContract
            && ((ProposalContract.ProposalApproveContract) c).getProposalId() == 1
            && ((ProposalContract.ProposalApproveContract) c).getIsAddApproval()
            && addressEquals(((ProposalContract.ProposalApproveContract) c)
                .getOwnerAddress(), ownerAddr)),
        eq(Protocol.Transaction.Contract.ContractType.ProposalApproveContract));
    assertTransactionResponse(response);
  }
}
