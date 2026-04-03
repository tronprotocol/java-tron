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
import org.tron.protos.contract.AssetIssueContractOuterClass;

public class ParticipateAssetIssueServletTest extends BaseHttpTest {

  private ParticipateAssetIssueServlet servlet;
  private final String ownerAddr = ByteArray.toHexString(new ECKey().getAddress());
  private final String toAddr = ByteArray.toHexString(new ECKey().getAddress());

  @Override
  protected void setUpMocks() throws Exception {
    servlet = new ParticipateAssetIssueServlet();
    injectWallet(servlet);
    when(wallet.createTransactionCapsule(
        any(AssetIssueContractOuterClass.ParticipateAssetIssueContract.class),
        eq(Protocol.Transaction.Contract.ContractType.ParticipateAssetIssueContract)))
        .thenReturn(new TransactionCapsule(MINIMAL_TX));
  }

  @Test
  public void testParticipateAssetIssue() throws Exception {
    String jsonParam = "{"
        + "\"owner_address\": \"" + ownerAddr + "\","
        + "\"to_address\": \"" + toAddr + "\","
        + "\"asset_name\": \"74657374\","
        + "\"amount\": 100"
        + "}";
    MockHttpServletRequest request = postRequest(jsonParam);

    MockHttpServletResponse response = newResponse();
    servlet.doPost(request, response);
    verify(wallet).createTransactionCapsule(
        argThat(c -> c instanceof AssetIssueContractOuterClass.ParticipateAssetIssueContract
            && addressEquals(((AssetIssueContractOuterClass.ParticipateAssetIssueContract) c)
                .getOwnerAddress(), ownerAddr)
            && addressEquals(((AssetIssueContractOuterClass.ParticipateAssetIssueContract) c)
                .getToAddress(), toAddr)
            && ((AssetIssueContractOuterClass.ParticipateAssetIssueContract) c)
                .getAmount() == 100
            && ((AssetIssueContractOuterClass.ParticipateAssetIssueContract) c)
                .getAssetName().toStringUtf8().equals("test")),
        eq(Protocol.Transaction.Contract.ContractType.ParticipateAssetIssueContract));
    assertTransactionResponse(response);
  }
}
