package org.tron.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import org.apache.commons.lang3.StringUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.tron.api.GrpcAPI;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.config.args.Args;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.ZksnarkException;

public class WalletShieldedAmountValidationTest {

  // both methods under test are pure and never touch instance state, so one wallet is enough;
  // constructing one generates a keypair, which is not worth paying for on every invocation
  private static Wallet wallet;
  private static Method bigIntegerFromString;
  private static Method checkBigIntegerRange;

  @BeforeClass
  public static void setUpClass() throws Exception {
    wallet = new Wallet();
    bigIntegerFromString =
        Wallet.class.getDeclaredMethod("getBigIntegerFromString", String.class);
    bigIntegerFromString.setAccessible(true);
    checkBigIntegerRange =
        Wallet.class.getDeclaredMethod("checkBigIntegerRange", BigInteger.class);
    checkBigIntegerRange.setAccessible(true);
  }

  private static BigInteger parse(String value) throws Exception {
    try {
      return (BigInteger) bigIntegerFromString.invoke(wallet, value);
    } catch (InvocationTargetException exception) {
      throw (Exception) exception.getCause();
    }
  }

  private static void checkRange(BigInteger value) throws Exception {
    try {
      checkBigIntegerRange.invoke(wallet, value);
    } catch (InvocationTargetException exception) {
      throw (Exception) exception.getCause();
    }
  }

  @Test
  public void amountLengthBoundaryIsAppliedAfterTrimAndBeforeParsing() throws Exception {
    assertEquals(BigInteger.ZERO, parse("   "));
    assertEquals(BigInteger.ZERO, parse("  " + StringUtils.repeat('0', 80) + "  "));

    String oversized = StringUtils.repeat('9', 81);
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> parse(oversized));
    assertEquals("invalid shielded amount", exception.getMessage());
    assertFalse(exception.getMessage().contains(oversized));
    assertThrows(IllegalArgumentException.class, () -> parse(StringUtils.repeat('0', 81)));
  }

  @Test
  public void uint256AndSignRangeChecksRemainAuthoritative() throws Exception {
    BigInteger maximum = BigInteger.ONE.shiftLeft(256).subtract(BigInteger.ONE);
    assertEquals(maximum, parse(maximum.toString()));
    checkRange(maximum);

    ContractValidateException tooLarge = assertThrows(ContractValidateException.class,
        () -> checkRange(parse(BigInteger.ONE.shiftLeft(256).toString())));
    assertTrue(tooLarge.getMessage().contains("256 bits"));

    ContractValidateException negative = assertThrows(ContractValidateException.class,
        () -> checkRange(parse("-1")));
    assertTrue(negative.getMessage().contains("non-negative"));
    assertEquals(BigInteger.valueOf(123), parse(" 123 "));
  }

  /** Runs the public entrypoint with the shielded api enabled, so the amount is reached. */
  private static void triggerInputWithAmount(String amount) throws Exception {
    GrpcAPI.ShieldedTRC20TriggerContractParameters request =
        GrpcAPI.ShieldedTRC20TriggerContractParameters.newBuilder()
            .setAmount(amount)
            .setShieldedTRC20Parameters(GrpcAPI.ShieldedTRC20Parameters.newBuilder()
                .setParameterType("transfer")
                .build())
            .build();
    CommonParameter commonParameter = mock(Args.class);
    try (MockedStatic<CommonParameter> mocked = mockStatic(CommonParameter.class)) {
      when(CommonParameter.getInstance()).thenReturn(commonParameter);
      when(commonParameter.isAllowShieldedTransactionApi()).thenReturn(true);
      wallet.getTriggerInputForShieldedTRC20Contract(request);
    }
  }

  /**
   * getBigIntegerFromString reports an overlong amount, and a malformed one, with unchecked
   * exceptions. The entrypoint declares only ZksnarkException and ContractValidateException, and
   * the http error sanitizer answers an untranslated failure with "internal server error" instead
   * of the validation error every other amount entrypoint answers with.
   */
  @Test
  public void unusableAmountsAreReportedAsValidationFailures() {
    String[] unusable = {StringUtils.repeat('9', 81), "not-a-number", "1.5"};
    for (String amount : unusable) {
      ContractValidateException exception = assertThrows(ContractValidateException.class,
          () -> triggerInputWithAmount(amount));
      assertEquals("invalid amount", exception.getMessage());
    }
  }

  /** A well-formed amount past the uint256 range still reports the range failure, not "invalid". */
  @Test
  public void outOfRangeAmountRetainsRangeValidationMessage() {
    ContractValidateException exception = assertThrows(ContractValidateException.class,
        () -> triggerInputWithAmount(BigInteger.ONE.shiftLeft(256).toString()));
    assertTrue(exception.getMessage().contains("256 bits"));
  }

  @Test
  public void disabledFeatureGateRunsBeforeAmountOrDescriptionValidation() {
    GrpcAPI.ShieldedTRC20TriggerContractParameters request =
        GrpcAPI.ShieldedTRC20TriggerContractParameters.newBuilder()
            .setAmount(StringUtils.repeat('9', 81))
            .setShieldedTRC20Parameters(GrpcAPI.ShieldedTRC20Parameters.newBuilder()
                .setParameterType("transfer")
                .build())
            .build();
    CommonParameter commonParameter = mock(Args.class);
    try (MockedStatic<CommonParameter> mocked = mockStatic(CommonParameter.class)) {
      when(CommonParameter.getInstance()).thenReturn(commonParameter);
      when(commonParameter.isAllowShieldedTransactionApi()).thenReturn(false);

      ZksnarkException exception = assertThrows(ZksnarkException.class,
          () -> wallet.getTriggerInputForShieldedTRC20Contract(request));
      assertTrue(exception.getMessage().contains("Shielded transaction API is disabled"));
    }
  }
}
