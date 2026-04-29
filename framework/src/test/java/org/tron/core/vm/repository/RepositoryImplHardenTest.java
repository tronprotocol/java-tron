package org.tron.core.vm.repository;

import com.google.protobuf.ByteString;
import java.lang.reflect.Method;
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
import org.tron.core.store.StoreFactory;
import org.tron.core.vm.config.VMConfig;
import org.tron.protos.Protocol.AccountType;

@Slf4j
public class RepositoryImplHardenTest extends BaseTest {

  static {
    Args.setParam(new String[]{"--output-directory", dbPath()}, TestConstants.TEST_CONF);
  }

  private RepositoryImpl repository;
  private Method increaseMethod;
  private Method getUsageMethod;
  private Method usageToBalanceMethod;

  @Before
  public void setUp() throws Exception {
    repository = RepositoryImpl.createRoot(StoreFactory.getInstance());

    increaseMethod = RepositoryImpl.class.getDeclaredMethod(
        "increase", long.class, long.class, long.class, long.class, long.class);
    increaseMethod.setAccessible(true);

    getUsageMethod = RepositoryImpl.class.getDeclaredMethod(
        "getUsage", long.class, long.class);
    getUsageMethod.setAccessible(true);

    usageToBalanceMethod = RepositoryImpl.class.getDeclaredMethod(
        "usageToBalance", long.class, long.class, long.class);
    usageToBalanceMethod.setAccessible(true);
  }

  @After
  public void tearDown() {
    VMConfig.initAllowHardenResourceCalculation(0);
  }

  private long invokeIncrease(long lastUsage, long usage, long lastTime,
                              long now, long windowSize) throws Exception {
    try {
      return (long) increaseMethod.invoke(
          repository, lastUsage, usage, lastTime, now, windowSize);
    } catch (java.lang.reflect.InvocationTargetException e) {
      if (e.getCause() instanceof RuntimeException) {
        throw (RuntimeException) e.getCause();
      }
      throw e;
    }
  }

  private long invokeGetUsage(long usage, long windowSize) throws Exception {
    try {
      return (long) getUsageMethod.invoke(repository, usage, windowSize);
    } catch (java.lang.reflect.InvocationTargetException e) {
      if (e.getCause() instanceof RuntimeException) {
        throw (RuntimeException) e.getCause();
      }
      throw e;
    }
  }

  private long invokeUsageToBalance(long usage, long totalWeight, long totalLimit)
      throws Exception {
    try {
      return (long) usageToBalanceMethod.invoke(
          repository, usage, totalWeight, totalLimit);
    } catch (java.lang.reflect.InvocationTargetException e) {
      if (e.getCause() instanceof RuntimeException) {
        throw (RuntimeException) e.getCause();
      }
      throw e;
    }
  }

  @Test
  public void testIncreaseNormalValuesParity() throws Exception {
    long lastUsage = 1_000L;
    long usage = 500L;
    long lastTime = 9990L;
    long now = 9995L;
    long windowSize = 28800L;

    VMConfig.initAllowHardenResourceCalculation(0);
    long resultOld = invokeIncrease(lastUsage, usage, lastTime, now, windowSize);

    VMConfig.initAllowHardenResourceCalculation(1);
    long resultNew = invokeIncrease(lastUsage, usage, lastTime, now, windowSize);

    Assert.assertEquals(resultOld, resultNew);
  }

  @Test
  public void testGetUsageNormalValuesParity() throws Exception {
    long usage = 100_000L;
    long windowSize = 28800L;

    VMConfig.initAllowHardenResourceCalculation(0);
    long resultOld = invokeGetUsage(usage, windowSize);

    VMConfig.initAllowHardenResourceCalculation(1);
    long resultNew = invokeGetUsage(usage, windowSize);

    Assert.assertEquals(resultOld, resultNew);
  }

  @Test
  public void testIncreaseOverflowDetectedWithHardening() {
    long lastUsage = Long.MAX_VALUE / 10; // ~9.2e17
    long usage = 1L;
    long lastTime = 9990L;
    long now = 9995L;
    long windowSize = 28800L;

    VMConfig.initAllowHardenResourceCalculation(1);

    Assert.assertThrows(ArithmeticException.class,
        () -> invokeIncrease(lastUsage, usage, lastTime, now, windowSize));
  }

  @Test
  public void testIncreaseOverflowSilentWithoutHardening() throws Exception {
    long lastUsage = Long.MAX_VALUE / 10;
    long usage = 1L;
    long lastTime = 9990L;
    long now = 9995L;
    long windowSize = 28800L;

    VMConfig.initAllowHardenResourceCalculation(0);
    invokeIncrease(lastUsage, usage, lastTime, now, windowSize);
  }

  @Test
  public void testGetUsageCorrectAcrossOverflowBoundary() throws Exception {
    long usage = Long.MAX_VALUE / 1000; // ~9.2e15
    long windowSize = 28800L;

    long expected = java.math.BigInteger.valueOf(usage)
        .multiply(java.math.BigInteger.valueOf(windowSize))
        .divide(java.math.BigInteger.valueOf(1_000_000L))
        .longValueExact();

    VMConfig.initAllowHardenResourceCalculation(1);
    long actual = invokeGetUsage(usage, windowSize);
    Assert.assertEquals(expected, actual);

    VMConfig.initAllowHardenResourceCalculation(0);
    long wrapped = invokeGetUsage(usage, windowSize);
    Assert.assertNotEquals(expected, wrapped);
  }

  @Test
  public void testGetUsageLargeButSafeWithHardening() throws Exception {
    long usage = 500_000_000_000L; // 5e11
    long windowSize = 28800L;

    VMConfig.initAllowHardenResourceCalculation(1);
    long expected = java.math.BigInteger.valueOf(usage)
        .multiply(java.math.BigInteger.valueOf(windowSize))
        .divide(java.math.BigInteger.valueOf(1_000_000L))
        .longValueExact();

    long actual = invokeGetUsage(usage, windowSize);
    Assert.assertEquals(expected, actual);
  }


  @Test
  public void testUsageToBalanceParity() throws Exception {
    long usage = 1_000_000L;
    long totalWeight = 2_000_000_000L;
    long totalLimit = 50_000_000_000L;

    VMConfig.initAllowHardenResourceCalculation(0);
    long resultOld = invokeUsageToBalance(usage, totalWeight, totalLimit);

    VMConfig.initAllowHardenResourceCalculation(1);
    long resultNew = invokeUsageToBalance(usage, totalWeight, totalLimit);

    Assert.assertEquals(resultOld, resultNew);
  }

  @Test
  public void testUsageToBalanceCorrectAcrossDoublePrecision() throws Exception {
    long usage = 100_000_000L; // 1e8
    long totalWeight = 100_000_000_000L; // 1e11 -> usage * weight = 1e19, beyond 2^53
    long totalLimit = 50_000_000_000L;

    java.math.BigInteger expected = java.math.BigInteger.valueOf(usage)
        .multiply(java.math.BigInteger.valueOf(totalWeight))
        .multiply(java.math.BigInteger.valueOf(1_000_000L))
        .divide(java.math.BigInteger.valueOf(totalLimit));

    VMConfig.initAllowHardenResourceCalculation(1);
    long actual = invokeUsageToBalance(usage, totalWeight, totalLimit);

    Assert.assertEquals(expected.longValueExact(), actual);
  }

  @Test
  public void testUsageToBalanceOverflowDetectedWithHardening() {
    long usage = 1_000_000_000L;
    long totalWeight = 1_000_000_000_000L;
    long totalLimit = 1L;

    VMConfig.initAllowHardenResourceCalculation(1);

    Assert.assertThrows(ArithmeticException.class,
        () -> invokeUsageToBalance(usage, totalWeight, totalLimit));
  }

  @Test
  public void testCalculateGlobalEnergyLimitHardenedParityWithNonIntegerRatio() {
    long totalEnergyLimit = 50_000_000_000L;
    long totalEnergyWeight = 1_234_567L;
    long frozeBalance = 10_000_000_000L;

    dbManager.getDynamicPropertiesStore().saveTotalEnergyCurrentLimit(totalEnergyLimit);
    dbManager.getDynamicPropertiesStore().saveTotalEnergyWeight(totalEnergyWeight);

    AccountCapsule account = new AccountCapsule(
        ByteString.copyFromUtf8("owner"),
        ByteString.copyFrom(ByteArray.fromHexString(
            Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc")),
        AccountType.Normal, 0L);
    account.setFrozenForEnergy(frozeBalance, 0L);

    VMConfig.initAllowHardenResourceCalculation(0);
    long resultOld = repository.calculateGlobalEnergyLimit(account);

    VMConfig.initAllowHardenResourceCalculation(1);
    long resultNew = repository.calculateGlobalEnergyLimit(account);

    long expected = java.math.BigInteger.valueOf(10000L)
        .multiply(java.math.BigInteger.valueOf(totalEnergyLimit))
        .divide(java.math.BigInteger.valueOf(totalEnergyWeight))
        .longValueExact();
    Assert.assertEquals(expected, resultNew);
    Assert.assertEquals(resultOld, resultNew);

    long buggy = 10000L * (totalEnergyLimit / totalEnergyWeight);
    Assert.assertNotEquals(buggy, resultNew);
  }

  @Test
  public void testCalculateGlobalEnergyLimitHardenedOverflowDetected() {
    long totalEnergyLimit = Long.MAX_VALUE / 2;
    long totalEnergyWeight = 1L;
    long frozeBalance = Long.MAX_VALUE / 4;

    dbManager.getDynamicPropertiesStore().saveTotalEnergyCurrentLimit(totalEnergyLimit);
    dbManager.getDynamicPropertiesStore().saveTotalEnergyWeight(totalEnergyWeight);

    AccountCapsule account = new AccountCapsule(
        ByteString.copyFromUtf8("owner"),
        ByteString.copyFrom(ByteArray.fromHexString(
            Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc")),
        AccountType.Normal, 0L);
    account.setFrozenForEnergy(frozeBalance, 0L);

    VMConfig.initAllowHardenResourceCalculation(1);
    Assert.assertThrows(ArithmeticException.class,
        () -> repository.calculateGlobalEnergyLimit(account));
  }
}
