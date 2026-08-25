package org.tron.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.config.args.Args;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.contract.ShieldContract;

public class WalletShieldedDescriptionValidationTest {

  private static Wallet wallet;

  @BeforeClass
  public static void setUpClass() {
    wallet = new Wallet();
  }

  private static void triggerInput(GrpcAPI.ShieldedTRC20Parameters parameters,
      String amount, int signatureCount) throws Exception {
    GrpcAPI.ShieldedTRC20TriggerContractParameters.Builder request =
        GrpcAPI.ShieldedTRC20TriggerContractParameters.newBuilder()
            .setAmount(amount)
            .setShieldedTRC20Parameters(parameters);
    for (int i = 0; i < signatureCount; i++) {
      request.addSpendAuthoritySignature(BytesMessage.getDefaultInstance());
    }

    CommonParameter commonParameter = mock(Args.class);
    try (MockedStatic<CommonParameter> mocked = mockStatic(CommonParameter.class)) {
      when(CommonParameter.getInstance()).thenReturn(commonParameter);
      when(commonParameter.isAllowShieldedTransactionApi()).thenReturn(true);
      wallet.getTriggerInputForShieldedTRC20Contract(request.build());
    }
  }

  @Test
  public void transferDescriptionCardinalityIsReportedAsValidationFailure() {
    GrpcAPI.ShieldedTRC20Parameters.Builder parameters =
        GrpcAPI.ShieldedTRC20Parameters.newBuilder()
            .setParameterType("transfer")
            .addSpendDescription(ShieldContract.SpendDescription.getDefaultInstance());
    for (int i = 0; i < 3; i++) {
      parameters.addReceiveDescription(ShieldContract.ReceiveDescription.getDefaultInstance());
    }

    ContractValidateException exception = assertThrows(ContractValidateException.class,
        () -> triggerInput(parameters.build(), "0", 1));
    assertEquals("invalid shielded TRC-20 trigger input: invalid transfer output number",
        exception.getMessage());
  }

  @Test
  public void mintDescriptionCardinalityIsReportedAsValidationFailure() {
    GrpcAPI.ShieldedTRC20Parameters parameters =
        GrpcAPI.ShieldedTRC20Parameters.newBuilder()
            .setParameterType("mint")
            .addReceiveDescription(ShieldContract.ReceiveDescription.getDefaultInstance())
            .addReceiveDescription(ShieldContract.ReceiveDescription.getDefaultInstance())
            .build();

    ContractValidateException exception = assertThrows(ContractValidateException.class,
        () -> triggerInput(parameters, "1", 0));
    assertEquals("invalid shielded TRC-20 trigger input: invalid mint description number",
        exception.getMessage());
  }
}
