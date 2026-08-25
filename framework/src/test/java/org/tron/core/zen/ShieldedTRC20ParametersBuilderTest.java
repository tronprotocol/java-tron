package org.tron.core.zen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mockStatic;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.api.GrpcAPI.ShieldedTRC20Parameters;
import org.tron.common.utils.ByteUtil;
import org.tron.protos.contract.ShieldContract;

public class ShieldedTRC20ParametersBuilderTest {

  private static final String GOLDEN_ONE_TO_ONE =
      "00000000000000000000000000000000000000000000000000000000000000c0"
          + "0000000000000000000000000000000000000000000000000000000000000220"
          + "0000000000000000000000000000000000000000000000000000000000000280"
          + "00000000000000000000000000000000000000000000000000000000000003c0"
          + "0000000000000000000000000000000000000000000000000000000000000001"
          + "0000000000000000000000000000000000000000000000000000000000000001"
          + "0000000000000000000000000000000000000000000000000000000000000001"
          + "0000000000000000000000000000000000000000000000000000000000000001"
          + "000000000000000000000000";

  private static final String GOLDEN_TWO_TO_TWO =
      "00000000000000000000000000000000000000000000000000000000000000c0"
          + "0000000000000000000000000000000000000000000000000000000000000360"
          + "0000000000000000000000000000000000000000000000000000000000000400"
          + "0000000000000000000000000000000000000000000000000000000000000660"
          + "0000000000000000000000000000000000000000000000000000000000000002"
          + "0000000000000000000000000000000000000000000000000000000000000002"
          + "0000000000000000000000000000000000000000000000000000000000000002"
          + "0000000000000000000000000000000000000000000000000000000000000002"
          + "000000000000000000000000000000000000000000000000";

  private static final String GOLDEN_ONE_TO_TWO =
      "00000000000000000000000000000000000000000000000000000000000000c0"
          + "0000000000000000000000000000000000000000000000000000000000000220"
          + "0000000000000000000000000000000000000000000000000000000000000280"
          + "00000000000000000000000000000000000000000000000000000000000004e0"
          + "0000000000000000000000000000000000000000000000000000000000000001"
          + "0000000000000000000000000000000000000000000000000000000000000001"
          + "0000000000000000000000000000000000000000000000000000000000000002"
          + "0000000000000000000000000000000000000000000000000000000000000002"
          + "000000000000000000000000000000000000000000000000";

  private static final String GOLDEN_TWO_TO_ONE =
      "00000000000000000000000000000000000000000000000000000000000000c0"
          + "0000000000000000000000000000000000000000000000000000000000000360"
          + "0000000000000000000000000000000000000000000000000000000000000400"
          + "0000000000000000000000000000000000000000000000000000000000000540"
          + "0000000000000000000000000000000000000000000000000000000000000002"
          + "0000000000000000000000000000000000000000000000000000000000000002"
          + "0000000000000000000000000000000000000000000000000000000000000001"
          + "0000000000000000000000000000000000000000000000000000000000000001"
          + "000000000000000000000000";

  private static ShieldedTRC20ParametersBuilder transferBuilder() throws Exception {
    return new ShieldedTRC20ParametersBuilder("transfer");
  }

  private static ShieldedTRC20Parameters params(int spends, int receives) {
    ShieldedTRC20Parameters.Builder parameters = ShieldedTRC20Parameters.newBuilder();
    for (int i = 0; i < spends; i++) {
      parameters.addSpendDescription(ShieldContract.SpendDescription.getDefaultInstance());
    }
    for (int i = 0; i < receives; i++) {
      parameters.addReceiveDescription(ShieldContract.ReceiveDescription.getDefaultInstance());
    }
    return parameters.build();
  }

  private static String run(int spends, int receives) throws Exception {
    return run(spends, receives, Collections.emptyList(), true);
  }

  private static String run(int spends, int receives, List<BytesMessage> signatures,
      boolean withAsk) throws Exception {
    return transferBuilder().getTriggerContractInput(
        params(spends, receives), signatures, BigInteger.ZERO, withAsk, new byte[21]);
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
            () -> run((int) counts[0], (int) counts[1]));
        assertEquals(counts[2], exception.getMessage());
      }
      byteUtil.verifyNoInteractions();
    }
  }

  /**
   * The asymmetric shapes are the ones that pin the arithmetic: both offsets mix the two counts,
   * so with spendCount == recvCount a transposition of the two produces identical bytes and the
   * symmetric vectors alone would pass under it.
   */
  @Test
  public void validOneAndTwoEntryOutputsRemainByteForByteCompatible() throws Exception {
    assertEquals(GOLDEN_ONE_TO_ONE, run(1, 1));
    assertEquals(GOLDEN_TWO_TO_TWO, run(2, 2));
    assertEquals(GOLDEN_ONE_TO_TWO, run(1, 2));
    assertEquals(GOLDEN_TWO_TO_ONE, run(2, 1));
  }
}
