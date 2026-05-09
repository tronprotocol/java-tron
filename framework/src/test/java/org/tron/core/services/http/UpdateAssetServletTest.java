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

public class UpdateAssetServletTest extends BaseHttpTest {

  private UpdateAssetServlet servlet;
  private final String ownerAddr = ByteArray.toHexString(new ECKey().getAddress());

  @Override
  protected void setUpMocks() throws Exception {
    servlet = new UpdateAssetServlet();
    injectWallet(servlet);
    when(wallet.createTransactionCapsule(
        any(AssetIssueContractOuterClass.UpdateAssetContract.class),
        eq(Protocol.Transaction.Contract.ContractType.UpdateAssetContract)))
        .thenReturn(new TransactionCapsule(MINIMAL_TX));
  }

  @Test
  public void testUpdateAsset() throws Exception {
    String jsonParam = "{"
        + "\"owner_address\": \"" + ownerAddr + "\","
        + "\"description\": \"746573745f64657363\","
        + "\"url\": \"746573745f75726c\","
        + "\"new_limit\": 100,"
        + "\"new_public_limit\": 200"
        + "}";
    MockHttpServletRequest request = postRequest(jsonParam);

    MockHttpServletResponse response = newResponse();
    servlet.doPost(request, response);
    verify(wallet).createTransactionCapsule(
        argThat(c -> c instanceof AssetIssueContractOuterClass.UpdateAssetContract
            && addressEquals(((AssetIssueContractOuterClass.UpdateAssetContract) c)
                .getOwnerAddress(), ownerAddr)
            && ((AssetIssueContractOuterClass.UpdateAssetContract) c).getNewLimit() == 100
            && ((AssetIssueContractOuterClass.UpdateAssetContract) c).getNewPublicLimit() == 200
            && ((AssetIssueContractOuterClass.UpdateAssetContract) c)
                .getDescription().toStringUtf8().equals("test_desc")
            && ((AssetIssueContractOuterClass.UpdateAssetContract) c)
                .getUrl().toStringUtf8().equals("test_url")),
        eq(Protocol.Transaction.Contract.ContractType.UpdateAssetContract));
    assertTransactionResponse(response);
  }
}
