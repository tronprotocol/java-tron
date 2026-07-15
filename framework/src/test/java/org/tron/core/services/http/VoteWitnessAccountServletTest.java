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
import org.tron.protos.contract.WitnessContract.VoteWitnessContract;

public class VoteWitnessAccountServletTest extends BaseHttpTest {

  private VoteWitnessAccountServlet servlet;
  private final String ownerAddr = ByteArray.toHexString(new ECKey().getAddress());
  private final String voteAddr = ByteArray.toHexString(new ECKey().getAddress());

  @Override
  protected void setUpMocks() throws Exception {
    servlet = new VoteWitnessAccountServlet();
    injectWallet(servlet);
    when(wallet.createTransactionCapsule(
        any(VoteWitnessContract.class), eq(ContractType.VoteWitnessContract)))
        .thenReturn(new TransactionCapsule(MINIMAL_TX));
  }

  @Test
  public void testVoteWitnessAccount() throws Exception {
    String jsonParam = "{"
        + "\"owner_address\": \"" + ownerAddr + "\","
        + "\"votes\": [{\"vote_address\": \"" + voteAddr + "\","
        + "\"vote_count\": 1}]"
        + "}";
    MockHttpServletRequest request = postRequest(jsonParam);

    MockHttpServletResponse response = newResponse();
    servlet.doPost(request, response);
    verify(wallet).createTransactionCapsule(
        argThat(c -> c instanceof VoteWitnessContract
            && addressEquals(((VoteWitnessContract) c).getOwnerAddress(), ownerAddr)
            && ((VoteWitnessContract) c).getVotesCount() == 1
            && ((VoteWitnessContract) c).getVotes(0).getVoteCount() == 1
            && addressEquals(((VoteWitnessContract) c).getVotes(0)
                .getVoteAddress(), voteAddr)),
        eq(ContractType.VoteWitnessContract));
    assertTransactionResponse(response);
  }
}
