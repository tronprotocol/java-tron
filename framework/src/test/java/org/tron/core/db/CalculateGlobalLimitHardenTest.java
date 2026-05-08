package org.tron.core.db;

import com.google.protobuf.ByteString;
import java.math.BigInteger;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.TestConstants;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.config.args.Args;
import org.tron.protos.Protocol.AccountType;

@Slf4j
public class CalculateGlobalLimitHardenTest extends BaseTest {

  private static final String OWNER_ADDRESS;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath()}, TestConstants.TEST_CONF);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
  }

  private EnergyProcessor energyProcessor;
  private BandwidthProcessor bandwidthProcessor;
  private AccountCapsule ownerCapsule;

  @Before
  public void setUp() {
    ownerCapsule = new AccountCapsule(
        ByteString.copyFromUtf8("owner"),
        ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
        AccountType.Normal, 0L);
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);

    energyProcessor = new EnergyProcessor(
        dbManager.getDynamicPropertiesStore(), dbManager.getAccountStore());
    bandwidthProcessor = new BandwidthProcessor(dbManager.getChainBaseManager());
  }

  @After
  public void tearDown() {
    dbManager.getDynamicPropertiesStore().saveAllowHardenResourceCalculation(0);
    dbManager.getDynamicPropertiesStore().saveUnfreezeDelayDays(0);
  }

  @Test
  public void testGlobalEnergyLimitParity() {
    dbManager.getDynamicPropertiesStore().saveTotalEnergyCurrentLimit(50_000_000_000L);
    dbManager.getDynamicPropertiesStore().saveTotalEnergyWeight(2_000_000_000L);
    ownerCapsule.setFrozenForEnergy(10_000_000_000L, 0L);
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);

    dbManager.getDynamicPropertiesStore().saveAllowHardenResourceCalculation(0);
    long resultOld = energyProcessor.calculateGlobalEnergyLimit(ownerCapsule);

    dbManager.getDynamicPropertiesStore().saveAllowHardenResourceCalculation(1);
    long resultNew = energyProcessor.calculateGlobalEnergyLimit(ownerCapsule);

    Assert.assertEquals(resultOld, resultNew);
  }

  @Test
  public void testGlobalEnergyLimitOverflowDetectedWithHardening() {
    dbManager.getDynamicPropertiesStore().saveTotalEnergyCurrentLimit(Long.MAX_VALUE / 2);
    dbManager.getDynamicPropertiesStore().saveTotalEnergyWeight(1L);
    ownerCapsule.setFrozenForEnergy(Long.MAX_VALUE / 4, 0L);
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);

    dbManager.getDynamicPropertiesStore().saveAllowHardenResourceCalculation(1);

    Assert.assertThrows(ArithmeticException.class,
        () -> energyProcessor.calculateGlobalEnergyLimit(ownerCapsule));
  }

  @Test
  public void testGlobalEnergyLimitV2Parity() {
    dbManager.getDynamicPropertiesStore().saveTotalEnergyCurrentLimit(50_000_000_000L);
    dbManager.getDynamicPropertiesStore().saveTotalEnergyWeight(2_000_000_000L);
    long frozeBalance = 10_000_000_000L;

    dbManager.getDynamicPropertiesStore().saveAllowHardenResourceCalculation(0);
    long resultOld = energyProcessor.calculateGlobalEnergyLimitV2(frozeBalance);

    dbManager.getDynamicPropertiesStore().saveAllowHardenResourceCalculation(1);
    long resultNew = energyProcessor.calculateGlobalEnergyLimitV2(frozeBalance);

    Assert.assertEquals(resultOld, resultNew);
  }

  @Test
  public void testGlobalEnergyLimitV2CorrectVsDoublePrecisionLoss() {
    long totalEnergyLimit = 50_000_000_000L;
    long totalEnergyWeight = 1_234_567L;
    long frozeBalance = 9_876_543_210_000_000L; // ~9.8e15

    dbManager.getDynamicPropertiesStore().saveTotalEnergyCurrentLimit(totalEnergyLimit);
    dbManager.getDynamicPropertiesStore().saveTotalEnergyWeight(totalEnergyWeight);

    BigInteger expected = BigInteger.valueOf(frozeBalance)
        .multiply(BigInteger.valueOf(totalEnergyLimit))
        .divide(BigInteger.valueOf(1_000_000L)
            .multiply(BigInteger.valueOf(totalEnergyWeight)));

    dbManager.getDynamicPropertiesStore().saveAllowHardenResourceCalculation(1);
    long actual = energyProcessor.calculateGlobalEnergyLimitV2(frozeBalance);
    Assert.assertEquals(expected.longValueExact(), actual);
  }

  @Test
  public void testGlobalNetLimitParity() {
    dbManager.getDynamicPropertiesStore().saveTotalNetLimit(43_200_000_000L);
    dbManager.getDynamicPropertiesStore().saveTotalNetWeight(2_000_000_000L);
    ownerCapsule.setFrozenForBandwidth(10_000_000_000L, 0L);
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);

    dbManager.getDynamicPropertiesStore().saveAllowHardenResourceCalculation(0);
    long resultOld = bandwidthProcessor.calculateGlobalNetLimit(ownerCapsule);

    dbManager.getDynamicPropertiesStore().saveAllowHardenResourceCalculation(1);
    long resultNew = bandwidthProcessor.calculateGlobalNetLimit(ownerCapsule);

    Assert.assertEquals(resultOld, resultNew);
  }

  @Test
  public void testGlobalNetLimitOverflowDetectedWithHardening() {
    dbManager.getDynamicPropertiesStore().saveTotalNetLimit(Long.MAX_VALUE / 2);
    dbManager.getDynamicPropertiesStore().saveTotalNetWeight(1L);
    ownerCapsule.setFrozenForBandwidth(Long.MAX_VALUE / 4, 0L);
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);

    dbManager.getDynamicPropertiesStore().saveAllowHardenResourceCalculation(1);

    Assert.assertThrows(ArithmeticException.class,
        () -> bandwidthProcessor.calculateGlobalNetLimit(ownerCapsule));
  }


  @Test
  public void testGlobalNetLimitV2Parity() {
    dbManager.getDynamicPropertiesStore().saveTotalNetLimit(43_200_000_000L);
    dbManager.getDynamicPropertiesStore().saveTotalNetWeight(2_000_000_000L);
    long frozeBalance = 10_000_000_000L;

    dbManager.getDynamicPropertiesStore().saveAllowHardenResourceCalculation(0);
    long resultOld = bandwidthProcessor.calculateGlobalNetLimitV2(frozeBalance);

    dbManager.getDynamicPropertiesStore().saveAllowHardenResourceCalculation(1);
    long resultNew = bandwidthProcessor.calculateGlobalNetLimitV2(frozeBalance);

    Assert.assertEquals(resultOld, resultNew);
  }

  @Test
  public void testGlobalNetLimitV2ExactPrecision() {
    long totalNetLimit = 43_200_000_000L;
    long totalNetWeight = 1_234_567L;
    long frozeBalance = 9_876_543_210_000_000L;

    dbManager.getDynamicPropertiesStore().saveTotalNetLimit(totalNetLimit);
    dbManager.getDynamicPropertiesStore().saveTotalNetWeight(totalNetWeight);

    BigInteger expected = BigInteger.valueOf(frozeBalance)
        .multiply(BigInteger.valueOf(totalNetLimit))
        .divide(BigInteger.valueOf(1_000_000L)
            .multiply(BigInteger.valueOf(totalNetWeight)));

    dbManager.getDynamicPropertiesStore().saveAllowHardenResourceCalculation(1);
    long actual = bandwidthProcessor.calculateGlobalNetLimitV2(frozeBalance);
    Assert.assertEquals(expected.longValueExact(), actual);
  }

  @Test
  public void testGlobalEnergyLimitV2BelowTrxPrecisionMatchesDouble() {
    long totalEnergyLimit = 50_000_000_000L;
    long totalEnergyWeight = 2_000_000_000L;
    long frozeBalance = 500_000L; // < TRX_PRECISION (1_000_000)

    dbManager.getDynamicPropertiesStore().saveTotalEnergyCurrentLimit(totalEnergyLimit);
    dbManager.getDynamicPropertiesStore().saveTotalEnergyWeight(totalEnergyWeight);

    dbManager.getDynamicPropertiesStore().saveAllowHardenResourceCalculation(0);
    long resultOld = energyProcessor.calculateGlobalEnergyLimitV2(frozeBalance);

    dbManager.getDynamicPropertiesStore().saveAllowHardenResourceCalculation(1);
    long resultNew = energyProcessor.calculateGlobalEnergyLimitV2(frozeBalance);

    Assert.assertEquals(12L, resultNew);
    Assert.assertEquals(resultOld, resultNew);
  }

  @Test
  public void testGlobalNetLimitV2BelowTrxPrecisionMatchesDouble() {
    long totalNetLimit = 43_200_000_000L;
    long totalNetWeight = 2_000_000_000L;
    long frozeBalance = 500_000L; // < TRX_PRECISION

    dbManager.getDynamicPropertiesStore().saveTotalNetLimit(totalNetLimit);
    dbManager.getDynamicPropertiesStore().saveTotalNetWeight(totalNetWeight);

    dbManager.getDynamicPropertiesStore().saveAllowHardenResourceCalculation(0);
    long resultOld = bandwidthProcessor.calculateGlobalNetLimitV2(frozeBalance);

    dbManager.getDynamicPropertiesStore().saveAllowHardenResourceCalculation(1);
    long resultNew = bandwidthProcessor.calculateGlobalNetLimitV2(frozeBalance);

    Assert.assertEquals(resultOld, resultNew);
    Assert.assertTrue("non-zero proportional result expected", resultNew > 0);
  }

  @Test
  public void testGlobalEnergyLimitV1NonIntegerRatioParity() {
    dbManager.getDynamicPropertiesStore().saveUnfreezeDelayDays(0); // force V1 path
    long totalEnergyLimit = 50_000_000_000L;
    long totalEnergyWeight = 1_234_567L; // not an exact divisor of totalEnergyLimit
    dbManager.getDynamicPropertiesStore().saveTotalEnergyCurrentLimit(totalEnergyLimit);
    dbManager.getDynamicPropertiesStore().saveTotalEnergyWeight(totalEnergyWeight);
    ownerCapsule.setFrozenForEnergy(10_000_000_000L, 0L);
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);

    dbManager.getDynamicPropertiesStore().saveAllowHardenResourceCalculation(0);
    long resultOld = energyProcessor.calculateGlobalEnergyLimit(ownerCapsule);

    dbManager.getDynamicPropertiesStore().saveAllowHardenResourceCalculation(1);
    long resultNew = energyProcessor.calculateGlobalEnergyLimit(ownerCapsule);

    Assert.assertEquals(resultOld, resultNew);
  }

  @Test
  public void testV1FlooredWeightVsV2FractionalWeight() {
    dbManager.getDynamicPropertiesStore().saveTotalEnergyCurrentLimit(50_000_000_000L);
    dbManager.getDynamicPropertiesStore().saveTotalEnergyWeight(2_000_000_000L);
    long frozeBalance = 1_500_000L; // 1.5 x TRX_PRECISION

    dbManager.getDynamicPropertiesStore().saveAllowHardenResourceCalculation(1);

    // V1 path
    dbManager.getDynamicPropertiesStore().saveUnfreezeDelayDays(0);
    ownerCapsule.setFrozenForEnergy(frozeBalance, 0L);
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);
    long v1New = energyProcessor.calculateGlobalEnergyLimit(ownerCapsule);

    // Legacy V1 expectation: floor(1.5) * 25.0 = 1 * 25 = 25
    Assert.assertEquals(25L, v1New);

    // V2 path with the same balance keeps the fractional weight
    long v2New = energyProcessor.calculateGlobalEnergyLimitV2(frozeBalance);
    // Legacy V2 expectation: 1.5 * 25.0 = 37.5 -> 37
    Assert.assertEquals(37L, v2New);

    // And both must match their respective legacy doubles
    dbManager.getDynamicPropertiesStore().saveAllowHardenResourceCalculation(0);
    long v1Old = energyProcessor.calculateGlobalEnergyLimit(ownerCapsule);
    long v2Old = energyProcessor.calculateGlobalEnergyLimitV2(frozeBalance);
    Assert.assertEquals(v1Old, v1New);
    Assert.assertEquals(v2Old, v2New);
  }

  @Test
  public void testGlobalNetLimitV1UsesTotalNetWeightNotLimit() {
    dbManager.getDynamicPropertiesStore().saveUnfreezeDelayDays(0); // force V1 path
    long totalNetLimit = 43_200_000_000L;
    long totalNetWeight = 2_000_000_000L; // distinct from totalNetLimit
    dbManager.getDynamicPropertiesStore().saveTotalNetLimit(totalNetLimit);
    dbManager.getDynamicPropertiesStore().saveTotalNetWeight(totalNetWeight);
    long frozeBalance = 10_000_000_000L;
    ownerCapsule.setFrozenForBandwidth(frozeBalance, 0L);
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);

    dbManager.getDynamicPropertiesStore().saveAllowHardenResourceCalculation(1);
    long actual = bandwidthProcessor.calculateGlobalNetLimit(ownerCapsule);

    Assert.assertEquals(216_000L, actual);
    Assert.assertNotEquals(10_000L, actual);
  }

  @Test
  public void testGlobalNetLimitV2UsesTotalNetWeightNotLimit() {
    long totalNetLimit = 43_200_000_000L;
    long totalNetWeight = 2_000_000_000L;
    dbManager.getDynamicPropertiesStore().saveTotalNetLimit(totalNetLimit);
    dbManager.getDynamicPropertiesStore().saveTotalNetWeight(totalNetWeight);
    long frozeBalance = 10_000_000_000L;

    dbManager.getDynamicPropertiesStore().saveAllowHardenResourceCalculation(1);
    long actual = bandwidthProcessor.calculateGlobalNetLimitV2(frozeBalance);

    Assert.assertEquals(216_000L, actual);
    Assert.assertNotEquals(10_000L, actual);
  }


  @Test
  public void testUpdateAdaptiveTotalEnergyLimitParity() {
    dbManager.getDynamicPropertiesStore().saveTotalEnergyAverageUsage(20_000_000L);
    dbManager.getDynamicPropertiesStore().saveTotalEnergyTargetLimit(10_000_000L);
    dbManager.getDynamicPropertiesStore().saveTotalEnergyCurrentLimit(50_000_000_000L);
    dbManager.getDynamicPropertiesStore().saveTotalEnergyLimit(50_000_000_000L);
    dbManager.getDynamicPropertiesStore().saveAdaptiveResourceLimitMultiplier(1000L);

    dbManager.getDynamicPropertiesStore().saveAllowHardenResourceCalculation(0);
    energyProcessor.updateAdaptiveTotalEnergyLimit();
    long resultOld = dbManager.getDynamicPropertiesStore().getTotalEnergyCurrentLimit();

    dbManager.getDynamicPropertiesStore().saveTotalEnergyCurrentLimit(50_000_000_000L);
    dbManager.getDynamicPropertiesStore().saveAllowHardenResourceCalculation(1);
    energyProcessor.updateAdaptiveTotalEnergyLimit();
    long resultNew = dbManager.getDynamicPropertiesStore().getTotalEnergyCurrentLimit();

    Assert.assertEquals(resultOld, resultNew);
  }

  @Test
  public void testUpdateAdaptiveTotalEnergyLimitOverflowDetected() {
    dbManager.getDynamicPropertiesStore().saveTotalEnergyAverageUsage(0L);
    dbManager.getDynamicPropertiesStore().saveTotalEnergyTargetLimit(Long.MAX_VALUE);
    dbManager.getDynamicPropertiesStore().saveTotalEnergyCurrentLimit(
        10_000_000_000_000_000L);
    dbManager.getDynamicPropertiesStore().saveTotalEnergyLimit(10_000_000_000_000_000L);
    dbManager.getDynamicPropertiesStore().saveAdaptiveResourceLimitMultiplier(1000L);

    dbManager.getDynamicPropertiesStore().saveAllowHardenResourceCalculation(1);

    Assert.assertThrows(ArithmeticException.class,
        () -> energyProcessor.updateAdaptiveTotalEnergyLimit());
  }

  @Test
  public void testUpdateAdaptiveLimitMultiplierOverflowDetected() {
    dbManager.getDynamicPropertiesStore().saveTotalEnergyAverageUsage(0L);
    dbManager.getDynamicPropertiesStore().saveTotalEnergyTargetLimit(Long.MAX_VALUE);
    dbManager.getDynamicPropertiesStore().saveTotalEnergyCurrentLimit(1_000_000L);
    dbManager.getDynamicPropertiesStore().saveTotalEnergyLimit(Long.MAX_VALUE / 100);
    dbManager.getDynamicPropertiesStore().saveAdaptiveResourceLimitMultiplier(1000L);
    dbManager.getDynamicPropertiesStore().saveAllowHardenResourceCalculation(1);

    Assert.assertThrows(ArithmeticException.class,
        () -> energyProcessor.updateAdaptiveTotalEnergyLimit());
  }
}
