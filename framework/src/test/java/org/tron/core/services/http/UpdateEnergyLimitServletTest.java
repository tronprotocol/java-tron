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
import org.tron.protos.contract.SmartContractOuterClass.UpdateEnergyLimitContract;

public class UpdateEnergyLimitServletTest extends BaseHttpTest {

  private UpdateEnergyLimitServlet servlet;
  private final String ownerAddr = ByteArray.toHexString(new ECKey().getAddress());
  private final String contractAddr = ByteArray.toHexString(new ECKey().getAddress());

  @Override
  protected void setUpMocks() throws Exception {
    servlet = new UpdateEnergyLimitServlet();
    injectWallet(servlet);
    when(wallet.createTransactionCapsule(
        any(UpdateEnergyLimitContract.class), eq(ContractType.UpdateEnergyLimitContract)))
        .thenReturn(new TransactionCapsule(MINIMAL_TX));
  }

  @Test
  public void testUpdateEnergyLimit() throws Exception {
    String jsonParam = "{"
        + "\"owner_address\": \"" + ownerAddr + "\","
        + "\"contract_address\": \"" + contractAddr + "\","
        + "\"origin_energy_limit\": 10000000"
        + "}";
    MockHttpServletRequest request = postRequest(jsonParam);

    MockHttpServletResponse response = newResponse();
    servlet.doPost(request, response);
    verify(wallet).createTransactionCapsule(
        argThat(c -> c instanceof UpdateEnergyLimitContract
            && addressEquals(((UpdateEnergyLimitContract) c)
                .getOwnerAddress(), ownerAddr)
            && addressEquals(((UpdateEnergyLimitContract) c)
                .getContractAddress(), contractAddr)
            && ((UpdateEnergyLimitContract) c).getOriginEnergyLimit() == 10000000),
        eq(ContractType.UpdateEnergyLimitContract));
    assertTransactionResponse(response);
  }
}
