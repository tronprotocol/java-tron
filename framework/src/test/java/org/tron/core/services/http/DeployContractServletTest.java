package org.tron.core.services.http;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.protos.Protocol;
import org.tron.protos.contract.SmartContractOuterClass.CreateSmartContract;

public class DeployContractServletTest extends BaseHttpTest {

  private DeployContractServlet servlet;

  @Override
  protected void setUpMocks() throws Exception {
    servlet = new DeployContractServlet();
    injectWallet(servlet);
    when(wallet.createTransactionCapsule(
            any(), eq(Protocol.Transaction.Contract.ContractType.CreateSmartContract)))
        .thenReturn(new TransactionCapsule(MINIMAL_TX));
  }

  @Test
  public void testDeployContract() throws Exception {
    String jsonParam = "{"
        + "\"owner_address\": \"4199357684BC659F5166046B56C95A0E99F1265CD1\","
        + "\"name\": \"TestContract\","
        + "\"abi\": \"[{\\\"inputs\\\":[],\\\"name\\\":\\\"test\\\","
        + "\\\"outputs\\\":[],\\\"type\\\":\\\"function\\\"}]\","
        + "\"bytecode\": \"608060405234801561001057600080fd5b50\","
        + "\"fee_limit\": 1000000000,"
        + "\"call_value\": 0,"
        + "\"consume_user_resource_percent\": 100,"
        + "\"origin_energy_limit\": 10000000"
        + "}";
    MockHttpServletRequest request = postRequest(jsonParam);

    MockHttpServletResponse response = newResponse();
    servlet.doPost(request, response);
    assertEquals(200, response.getStatus());
    verify(wallet).createTransactionCapsule(
        argThat(c -> c instanceof CreateSmartContract
            && addressEquals(((CreateSmartContract) c).getOwnerAddress(),
                "4199357684bc659f5166046b56c95a0e99f1265cd1")
            && ((CreateSmartContract) c).getNewContract().getName().equals("TestContract")
            && ((CreateSmartContract) c).getNewContract()
                .getOriginEnergyLimit() == 10000000),
        eq(Protocol.Transaction.Contract.ContractType.CreateSmartContract));
    assertTransactionResponse(response);
  }
}
