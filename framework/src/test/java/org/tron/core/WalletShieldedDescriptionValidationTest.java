package org.tron.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.google.protobuf.ByteString;
import java.math.BigInteger;
import java.util.Arrays;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ByteUtil;
import org.tron.core.config.args.Args;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.zen.note.NoteEncryption.Encryption;
import org.tron.protos.contract.ShieldContract;

public class WalletShieldedDescriptionValidationTest {

  private static Wallet wallet;

  @BeforeClass
  public static void setUpClass() {
    wallet = new Wallet();
  }

  private static BytesMessage triggerInput(GrpcAPI.ShieldedTRC20Parameters parameters,
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
      return wallet.getTriggerInputForShieldedTRC20Contract(request.build());
    }
  }

  private static GrpcAPI.ShieldedTRC20Parameters withReceives(
      GrpcAPI.ShieldedTRC20Parameters.Builder parameters, int receives) {
    for (int i = 0; i < receives; i++) {
      parameters.addReceiveDescription(ShieldContract.ReceiveDescription.getDefaultInstance());
    }
    return parameters.build();
  }

  /**
   * A burn whose v2 record carries the marker and the nonce bound to (nullifier, amount, zero
   * transparent address), so the request clears the existing record checks.
   */
  private static GrpcAPI.ShieldedTRC20Parameters burn(int receives, String amount) {
    byte[] nullifier = new byte[32];
    Arrays.fill(nullifier, (byte) 1);
    byte[] addr21 = new byte[21];
    addr21[0] = Wallet.getAddressPreFixByte();
    byte[] nonce = Encryption.deriveBurnNonce(nullifier,
        ByteUtil.bigIntegerToBytes(new BigInteger(amount), 32), addr21);
    byte[] record = new byte[Encryption.BURN_CIPHER_RECORD_SIZE];
    System.arraycopy(nonce, 0, record, Encryption.BURN_NONCE_OFFSET, Encryption.BURN_NONCE_LEN);
    System.arraycopy(Encryption.getBurnRecordV2Marker(), 0, record,
        Encryption.BURN_RESERVED_OFFSET, Encryption.BURN_RESERVED_LEN);
    return withReceives(GrpcAPI.ShieldedTRC20Parameters.newBuilder()
        .setParameterType("burn")
        .setTriggerContractInput(ByteArray.toHexString(record))
        .addSpendDescription(ShieldContract.SpendDescription.newBuilder()
            .setNullifier(ByteString.copyFrom(nullifier))), receives);
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
  public void mintRequiresExactlyOneReceiveDescription() throws Exception {
    for (int receives : new int[]{0, 2}) {
      GrpcAPI.ShieldedTRC20Parameters parameters = withReceives(
          GrpcAPI.ShieldedTRC20Parameters.newBuilder().setParameterType("mint"), receives);
      ContractValidateException exception = assertThrows(ContractValidateException.class,
          () -> triggerInput(parameters, "1", 0));
      assertEquals("invalid shielded TRC-20 trigger input: invalid mint output number",
          exception.getMessage());
    }
    assertFalse(triggerInput(withReceives(
        GrpcAPI.ShieldedTRC20Parameters.newBuilder().setParameterType("mint"), 1), "1", 0)
        .getValue().isEmpty());
  }

  @Test
  public void burnAcceptsAtMostOneReceiveDescription() throws Exception {
    assertFalse(triggerInput(burn(0, "1"), "1", 1).getValue().isEmpty());
    assertFalse(triggerInput(burn(1, "1"), "1", 1).getValue().isEmpty());

    ContractValidateException exception = assertThrows(ContractValidateException.class,
        () -> triggerInput(burn(2, "1"), "1", 1));
    assertEquals("invalid shielded TRC-20 trigger input: invalid burn output number",
        exception.getMessage());
  }
}
