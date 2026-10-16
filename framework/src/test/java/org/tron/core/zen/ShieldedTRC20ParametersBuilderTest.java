package org.tron.core.zen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mockStatic;

import com.google.protobuf.ByteString;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.api.GrpcAPI.ShieldedTRC20Parameters;
import org.tron.common.utils.ByteUtil;
import org.tron.core.utils.ZenChainParams;
import org.tron.protos.contract.ShieldContract;

public class ShieldedTRC20ParametersBuilderTest {

  /** SHA-256 of {@link #encode} per spends-receives-withAsk key; each output is several KB. */
  private static final String[][] BASE_DIGESTS = {
      {"1-1-true",
          "da4c52cbf2903d98f9eba9c2b537328448d767bee8c495d3c22087e272976238"},
      {"1-1-false",
          "b8fa61b5b5230092591d45fa61c65e5bfea2995441cdb373547e378c7670f8df"},
      {"1-2-true",
          "77a62c96cf7f03a0ff092661861a53596b9ef57b9f397efcc661482b0ecc1d6b"},
      {"1-2-false",
          "b998f3d1fc5e309560476b25e38fdb1f4cb2ec9223ada4734f0c086f0013c29d"},
      {"2-1-true",
          "7fb3c1231f29a606aa2509d3d6bc9a0c58d851a8710c21f453d556edcdf41675"},
      {"2-1-false",
          "25d93b0be6e8e387d86932b2a2b87f39bea516b543c646d0ce55023250b591ce"},
      {"2-2-true",
          "2d4525842ecef92b52c0b3a0dc8689d461e1be08788f29d03a150639f05fe9c6"},
      {"2-2-false",
          "aa8cd6ebd1208a53e642d9aa796967d5ebe66a7aca7615965dccd9d727f7b624"},
  };

  private static ByteString fill(int length, int seed) {
    byte[] value = new byte[length];
    for (int i = 0; i < length; i++) {
      value[i] = (byte) (seed * 31 + i);
    }
    return ByteString.copyFrom(value);
  }

  /** Every field has its protocol size and its own content, so a misplaced field shows. */
  static ShieldedTRC20Parameters populated(int spends, int receives) {
    ShieldedTRC20Parameters.Builder parameters = ShieldedTRC20Parameters.newBuilder();
    for (int i = 0; i < spends; i++) {
      int seed = 16 * i;
      parameters.addSpendDescription(ShieldContract.SpendDescription.newBuilder()
          .setNullifier(fill(32, seed + 1))
          .setAnchor(fill(32, seed + 2))
          .setValueCommitment(fill(32, seed + 3))
          .setRk(fill(32, seed + 4))
          .setZkproof(fill(192, seed + 5))
          .setSpendAuthoritySignature(fill(64, seed + 6)));
    }
    for (int i = 0; i < receives; i++) {
      int seed = 16 * i;
      parameters.addReceiveDescription(ShieldContract.ReceiveDescription.newBuilder()
          .setNoteCommitment(fill(32, seed + 7))
          .setValueCommitment(fill(32, seed + 8))
          .setEpk(fill(32, seed + 9))
          .setZkproof(fill(192, seed + 10))
          .setCEnc(fill(ZenChainParams.ZC_ENCCIPHERTEXT_SIZE, seed + 11))
          .setCOut(fill(ZenChainParams.ZC_OUTCIPHERTEXT_SIZE, seed + 12)));
    }
    return parameters.setBindingSignature(fill(64, 13)).build();
  }

  /** Differs from the signatures inside the descriptions, so the two paths give distinct bytes. */
  static List<BytesMessage> requestSignatures(int count) {
    List<BytesMessage> signatures = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      signatures.add(BytesMessage.newBuilder().setValue(fill(64, 16 * i + 14)).build());
    }
    return signatures;
  }

  static String encode(int spends, int receives, boolean withAsk) throws Exception {
    return new ShieldedTRC20ParametersBuilder("transfer").getTriggerContractInput(
        populated(spends, receives), requestSignatures(spends), BigInteger.ZERO, withAsk,
        new byte[21]);
  }

  static String digest(String triggerInput) throws Exception {
    return Hex.toHexString(MessageDigest.getInstance("SHA-256").digest(Hex.decode(triggerInput)));
  }

  private static String runEmpty(int spends, int receives) throws Exception {
    ShieldedTRC20Parameters.Builder parameters = ShieldedTRC20Parameters.newBuilder();
    for (int i = 0; i < spends; i++) {
      parameters.addSpendDescription(ShieldContract.SpendDescription.getDefaultInstance());
    }
    for (int i = 0; i < receives; i++) {
      parameters.addReceiveDescription(ShieldContract.ReceiveDescription.getDefaultInstance());
    }
    return new ShieldedTRC20ParametersBuilder("transfer").getTriggerContractInput(
        parameters.build(), Collections.emptyList(), BigInteger.ZERO, true, new byte[21]);
  }

  @Test
  public void invalidCountsAreRejectedBeforeAnyMerge() throws Exception {
    Object[][] invalidCounts = {
        {0, 1, "invalid transfer input number"},
        {3, 1, "invalid transfer input number"},
        {1, 0, "invalid transfer output number"},
        {1, 3, "invalid transfer output number"},
        {1, 10_000, "invalid transfer output number"},
    };
    try (MockedStatic<ByteUtil> byteUtil = mockStatic(ByteUtil.class)) {
      for (Object[] counts : invalidCounts) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> runEmpty((int) counts[0], (int) counts[1]));
        assertEquals(counts[2], exception.getMessage());
      }
      byteUtil.verifyNoInteractions();
    }
  }

  @Test
  public void populatedOutputsMatchTheBaseForEveryCountAndSignaturePath() throws Exception {
    for (String[] vector : BASE_DIGESTS) {
      String[] key = vector[0].split("-");
      String triggerInput = encode(Integer.parseInt(key[0]), Integer.parseInt(key[1]),
          Boolean.parseBoolean(key[2]));
      assertEquals(vector[0], vector[1], digest(triggerInput));
    }
  }
}
