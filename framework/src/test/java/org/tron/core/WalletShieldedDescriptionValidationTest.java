package org.tron.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.google.protobuf.ByteString;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.util.Arrays;
import javax.servlet.http.HttpServletResponse;
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
import org.tron.core.exception.ZksnarkException;
import org.tron.core.services.http.servlets.Util;
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
      request.addSpendAuthoritySignature(BytesMessage.newBuilder()
          .setValue(ByteString.copyFrom(new byte[64])));
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
      parameters.addReceiveDescription(ShieldContract.ReceiveDescription.newBuilder()
          .setNoteCommitment(ByteString.copyFrom(new byte[32]))
          .setValueCommitment(ByteString.copyFrom(new byte[32]))
          .setEpk(ByteString.copyFrom(new byte[32]))
          .setZkproof(ByteString.copyFrom(new byte[192]))
          .setCEnc(ByteString.copyFrom(new byte[580]))
          .setCOut(ByteString.copyFrom(new byte[80])));
    }
    return parameters.setBindingSignature(ByteString.copyFrom(new byte[64])).build();
  }

  private static ShieldContract.SpendDescription.Builder spend() {
    return ShieldContract.SpendDescription.newBuilder()
        .setNullifier(ByteString.copyFrom(new byte[32]))
        .setAnchor(ByteString.copyFrom(new byte[32]))
        .setValueCommitment(ByteString.copyFrom(new byte[32]))
        .setRk(ByteString.copyFrom(new byte[32]))
        .setZkproof(ByteString.copyFrom(new byte[192]));
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
        .addSpendDescription(spend()
            .setNullifier(ByteString.copyFrom(nullifier))), receives);
  }

  @Test
  public void transferDescriptionCardinalityIsReportedAsValidationFailure() {
    GrpcAPI.ShieldedTRC20Parameters.Builder parameters =
        GrpcAPI.ShieldedTRC20Parameters.newBuilder()
            .setParameterType("transfer")
            .setBindingSignature(ByteString.copyFrom(new byte[64]))
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
  public void invalidBindingSignatureLengthsAreReportedAsValidationFailures() {
    GrpcAPI.ShieldedTRC20Parameters[] parameters = {
        withReceives(GrpcAPI.ShieldedTRC20Parameters.newBuilder().setParameterType("mint"), 1),
        withReceives(GrpcAPI.ShieldedTRC20Parameters.newBuilder().setParameterType("transfer")
            .addSpendDescription(spend()), 1),
        burn(0, "1")
    };
    for (GrpcAPI.ShieldedTRC20Parameters parameter : parameters) {
      for (int length : new int[]{0, 63, 65}) {
        GrpcAPI.ShieldedTRC20Parameters invalid = parameter.toBuilder()
            .setBindingSignature(ByteString.copyFrom(new byte[length])).build();
        ContractValidateException exception = assertThrows(ContractValidateException.class,
            () -> triggerInput(invalid, "1", invalid.getSpendDescriptionCount()));
        assertEquals("invalid shielded TRC-20 trigger input: binding_signature must be 64 bytes",
            exception.getMessage());
      }
    }
  }

  @Test
  public void signatureCountValidationRetainsWalletError() {
    GrpcAPI.ShieldedTRC20Parameters[] parameters = {
        withReceives(GrpcAPI.ShieldedTRC20Parameters.newBuilder().setParameterType("transfer")
            .addSpendDescription(spend()), 1),
        burn(0, "1")
    };
    for (GrpcAPI.ShieldedTRC20Parameters parameter : parameters) {
      for (int count : new int[]{0, 2}) {
        ZksnarkException exception = assertThrows(ZksnarkException.class,
            () -> triggerInput(parameter, "1", count));
        assertEquals("the number of spendDescription and spendAuthoritySignature is not equal",
            exception.getMessage());
      }
    }
  }

  @Test
  public void malformedBurnHexIsReportedAsValidationFailure() throws Exception {
    GrpcAPI.ShieldedTRC20Parameters valid = burn(0, "1");
    for (String hex : new String[]{"gg", "0xgg", "g", "ĀĀ"}) {
      GrpcAPI.ShieldedTRC20Parameters invalid = valid.toBuilder()
          .setTriggerContractInput(hex).build();
      ContractValidateException error = assertThrows(ContractValidateException.class,
          () -> triggerInput(invalid, "1", 1));
      assertEquals("invalid shielded TRC-20 trigger input: burn record must be valid hex",
          error.getMessage());

      HttpServletResponse response = mock(HttpServletResponse.class);
      StringWriter output = new StringWriter();
      when(response.getWriter()).thenReturn(new PrintWriter(output));
      Util.processError(error, response);
      assertEquals("{\"Error\":\"invalid shielded TRC-20 trigger input: "
          + "burn record must be valid hex\"}", output.toString().trim());
    }
  }

  @Test
  public void burnHexPrefixesAndOddLengthsPreserveTriggerInput() throws Exception {
    GrpcAPI.ShieldedTRC20Parameters valid = burn(0, "1");
    String hex = valid.getTriggerContractInput();
    assertEquals('0', hex.charAt(0));
    BytesMessage expected = triggerInput(valid, "1", 1);
    for (String compatible : new String[]{"0x" + hex, hex.substring(1),
        "0x" + hex.substring(1)}) {
      assertEquals(expected, triggerInput(valid.toBuilder()
          .setTriggerContractInput(compatible).build(), "1", 1));
    }
  }

  @Test
  public void burnInputValidationRetainsWalletErrors() {
    GrpcAPI.ShieldedTRC20Parameters valid = burn(0, "1");
    for (int count : new int[]{0, 2}) {
      GrpcAPI.ShieldedTRC20Parameters.Builder parameters = valid.toBuilder()
          .clearSpendDescription();
      for (int i = 0; i < count; i++) {
        parameters.addSpendDescription(valid.getSpendDescription(0));
      }
      ZksnarkException exception = assertThrows(ZksnarkException.class,
          () -> triggerInput(parameters.build(), "1", count));
      assertEquals("burn trigger input requires exactly one spendDescription for nf-bound nonce",
          exception.getMessage());
    }
    for (int length : new int[]{0, 31, 33}) {
      GrpcAPI.ShieldedTRC20Parameters.Builder parameters = valid.toBuilder();
      parameters.getSpendDescriptionBuilder(0).setNullifier(ByteString.copyFrom(new byte[length]));
      ZksnarkException exception = assertThrows(ZksnarkException.class,
          () -> triggerInput(parameters.build(), "1", 1));
      assertEquals("burn trigger input requires 32-byte spendDescription.nullifier",
          exception.getMessage());
    }
  }

  @Test
  public void malformedDescriptionsAreReportedAsValidationFailures() {
    GrpcAPI.ShieldedTRC20Parameters valid = withReceives(
        GrpcAPI.ShieldedTRC20Parameters.newBuilder().setParameterType("transfer")
            .addSpendDescription(spend()), 1);
    GrpcAPI.ShieldedTRC20Parameters.Builder invalidSpend = valid.toBuilder();
    invalidSpend.getSpendDescriptionBuilder(0).setZkproof(ByteString.copyFrom(new byte[191]));
    ContractValidateException spendError = assertThrows(ContractValidateException.class,
        () -> triggerInput(invalidSpend.build(), "0", 1));
    assertEquals("invalid shielded TRC-20 trigger input: "
            + "spend_description.zkproof must be 192 bytes",
        spendError.getMessage());

    GrpcAPI.ShieldedTRC20Parameters.Builder invalidReceive = valid.toBuilder();
    invalidReceive.getReceiveDescriptionBuilder(0).setCEnc(ByteString.copyFrom(new byte[579]));
    ContractValidateException receiveError = assertThrows(ContractValidateException.class,
        () -> triggerInput(invalidReceive.build(), "0", 1));
    assertEquals("invalid shielded TRC-20 trigger input: "
            + "receive_description.c_enc must be 580 bytes",
        receiveError.getMessage());
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
