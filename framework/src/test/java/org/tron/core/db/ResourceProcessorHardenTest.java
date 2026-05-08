package org.tron.core.db;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
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
import org.tron.protos.contract.Common;
import org.tron.protos.contract.Common.ResourceCode;

@Slf4j
public class ResourceProcessorHardenTest extends BaseTest {

  private static final String OWNER_ADDRESS;
  private static final String RECEIVER_ADDRESS;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath()}, TestConstants.TEST_CONF);
    OWNER_ADDRESS =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
    RECEIVER_ADDRESS =
        Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
  }

  private EnergyProcessor processor;
  private AccountCapsule ownerCapsule;
  private AccountCapsule receiverCapsule;

  @Before
  public void setUp() {
    ownerCapsule = new AccountCapsule(
        ByteString.copyFromUtf8("owner"),
        ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
        AccountType.Normal, 10_000_000_000L);

    receiverCapsule = new AccountCapsule(
        ByteString.copyFromUtf8("receiver"),
        ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS)),
        AccountType.Normal, 10_000_000_000L);

    dbManager.getAccountStore().put(
        ownerCapsule.getAddress().toByteArray(), ownerCapsule);
    dbManager.getAccountStore().put(
        receiverCapsule.getAddress().toByteArray(), receiverCapsule);

    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(10000L);
    dbManager.getDynamicPropertiesStore().saveTotalEnergyWeight(10_000_000L);

    processor = new EnergyProcessor(
        dbManager.getDynamicPropertiesStore(), dbManager.getAccountStore());
  }

  @Test
  public void testIncreaseNormalValuesConsistent() {
    long lastUsage = 1000L;
    long usage = 500L;
    long lastTime = 9990L;
    long now = 9995L;
    long windowSize = 28800L; // 24h in slots

    dbManager.getDynamicPropertiesStore().saveAllowHardenResourceCalculation(0);
    long resultOld = processor.increase(lastUsage, usage, lastTime, now, windowSize);

    dbManager.getDynamicPropertiesStore().saveAllowHardenResourceCalculation(1);
    long resultNew = processor.increase(lastUsage, usage, lastTime, now, windowSize);

    Assert.assertEquals(resultOld, resultNew);
  }

  @Test
  public void testIncreaseV2NormalValuesConsistent() {
    dbManager.getDynamicPropertiesStore().saveUnfreezeDelayDays(14);
    dbManager.getDynamicPropertiesStore().saveAllowCancelAllUnfreezeV2(1);

    long lastUsage = 70_000_000L;
    long usage = 2345L;
    long lastTime = 9999L;
    long now = 10000L;

    dbManager.getDynamicPropertiesStore().saveAllowHardenResourceCalculation(0);
    ownerCapsule.setNewWindowSize(Common.ResourceCode.ENERGY, 28800);
    ownerCapsule.setWindowOptimized(Common.ResourceCode.ENERGY, true);
    ownerCapsule.setLatestConsumeTimeForEnergy(lastTime);
    ownerCapsule.setEnergyUsage(lastUsage);
    long resultOld = processor.increaseV2(ownerCapsule, ResourceCode.ENERGY,
        lastUsage, usage, lastTime, now);

    dbManager.getDynamicPropertiesStore().saveAllowHardenResourceCalculation(1);
    ownerCapsule.setNewWindowSize(Common.ResourceCode.ENERGY, 28800);
    ownerCapsule.setWindowOptimized(Common.ResourceCode.ENERGY, true);
    ownerCapsule.setLatestConsumeTimeForEnergy(lastTime);
    ownerCapsule.setEnergyUsage(lastUsage);
    long resultNew = processor.increaseV2(ownerCapsule, ResourceCode.ENERGY,
        lastUsage, usage, lastTime, now);

    Assert.assertEquals(resultOld, resultNew);
  }

  @Test
  public void testIncreaseOverflowDetectedWithHardening() {
    long lastUsage = Long.MAX_VALUE / 10; // ~9.2e17
    long usage = 1L;
    long lastTime = 9990L;
    long now = 9995L;
    long windowSize = 28800L;

    dbManager.getDynamicPropertiesStore().saveAllowHardenResourceCalculation(1);

    Assert.assertThrows(ArithmeticException.class,
        () -> processor.increase(lastUsage, usage, lastTime, now, windowSize));
  }

  @Test
  public void testIncreaseOverflowSilentWithoutHardening() {
    long lastUsage = Long.MAX_VALUE / 10;
    long usage = 1L;
    long lastTime = 9990L;
    long now = 9995L;
    long windowSize = 28800L;

    dbManager.getDynamicPropertiesStore().saveAllowHardenResourceCalculation(0);
    processor.increase(lastUsage, usage, lastTime, now, windowSize);
  }

  @Test
  public void testIncreaseAcceptsIntermediateOverflowWhenResultFits() {
    long lastUsage = Long.MAX_VALUE / 100; // ~9.2e16
    long usage = 1L;
    long lastTime = 9990L;
    long now = 9995L;
    long windowSize = 28800L;

    dbManager.getDynamicPropertiesStore().saveAllowHardenResourceCalculation(1);
    long result = processor.increase(lastUsage, usage, lastTime, now, windowSize);
    Assert.assertTrue("Result should be a valid long", result >= 0);
  }

  @Test
  public void testIncreaseWithAccountCapsuleConsistent() {
    dbManager.getDynamicPropertiesStore().saveUnfreezeDelayDays(14);

    long lastUsage = 5_000_000L;
    long usage = 1_000L;
    long lastTime = 9990L;
    long now = 9995L;

    dbManager.getDynamicPropertiesStore().saveAllowHardenResourceCalculation(0);
    ownerCapsule.setNewWindowSize(ResourceCode.ENERGY, 28800);
    ownerCapsule.setLatestConsumeTimeForEnergy(lastTime);
    ownerCapsule.setEnergyUsage(lastUsage);
    long resultOld = processor.increase(ownerCapsule, ResourceCode.ENERGY,
        lastUsage, usage, lastTime, now);
    long windowOld = ownerCapsule.getWindowSize(ResourceCode.ENERGY);

    dbManager.getDynamicPropertiesStore().saveAllowHardenResourceCalculation(1);
    ownerCapsule.setNewWindowSize(ResourceCode.ENERGY, 28800);
    ownerCapsule.setLatestConsumeTimeForEnergy(lastTime);
    ownerCapsule.setEnergyUsage(lastUsage);
    long resultNew = processor.increase(ownerCapsule, ResourceCode.ENERGY,
        lastUsage, usage, lastTime, now);
    long windowNew = ownerCapsule.getWindowSize(ResourceCode.ENERGY);

    Assert.assertEquals(resultOld, resultNew);
    Assert.assertEquals(windowOld, windowNew);
  }

  @Test
  public void testUnDelegateIncreaseV2NormalValuesConsistent() {
    dbManager.getDynamicPropertiesStore().saveUnfreezeDelayDays(14);
    dbManager.getDynamicPropertiesStore().saveAllowCancelAllUnfreezeV2(1);

    long transferUsage = 1000L;
    long now = 10000L;

    dbManager.getDynamicPropertiesStore().saveAllowHardenResourceCalculation(0);
    setupForUnDelegate(now);
    processor.unDelegateIncreaseV2(ownerCapsule, receiverCapsule,
        transferUsage, ResourceCode.ENERGY, now);
    long usageOld = ownerCapsule.getUsage(ResourceCode.ENERGY);
    long windowOld = ownerCapsule.getWindowSizeV2(ResourceCode.ENERGY);

    dbManager.getDynamicPropertiesStore().saveAllowHardenResourceCalculation(1);
    setupForUnDelegate(now);
    processor.unDelegateIncreaseV2(ownerCapsule, receiverCapsule,
        transferUsage, ResourceCode.ENERGY, now);
    long usageNew = ownerCapsule.getUsage(ResourceCode.ENERGY);
    long windowNew = ownerCapsule.getWindowSizeV2(ResourceCode.ENERGY);

    Assert.assertEquals(usageOld, usageNew);
    Assert.assertEquals(windowOld, windowNew);
  }

  @Test
  public void testUnDelegateIncreaseV2ConsistentWithHardening() {
    dbManager.getDynamicPropertiesStore().saveUnfreezeDelayDays(14);
    dbManager.getDynamicPropertiesStore().saveAllowCancelAllUnfreezeV2(1);

    long transferUsage = 5_000_000L;
    long now = 10000L;

    dbManager.getDynamicPropertiesStore().saveAllowHardenResourceCalculation(0);
    setupForUnDelegateWithUsage(now, 2_000_000L, 3_000_000L);
    processor.unDelegateIncreaseV2(ownerCapsule, receiverCapsule,
        transferUsage, ResourceCode.ENERGY, now);
    long usageOld = ownerCapsule.getUsage(ResourceCode.ENERGY);
    long windowOld = ownerCapsule.getWindowSizeV2(ResourceCode.ENERGY);

    dbManager.getDynamicPropertiesStore().saveAllowHardenResourceCalculation(1);
    setupForUnDelegateWithUsage(now, 2_000_000L, 3_000_000L);
    processor.unDelegateIncreaseV2(ownerCapsule, receiverCapsule,
        transferUsage, ResourceCode.ENERGY, now);
    long usageNew = ownerCapsule.getUsage(ResourceCode.ENERGY);
    long windowNew = ownerCapsule.getWindowSizeV2(ResourceCode.ENERGY);

    Assert.assertEquals(usageOld, usageNew);
    Assert.assertEquals(windowOld, windowNew);
  }

  @Test
  public void testIncreaseV2OverflowDetected() {
    dbManager.getDynamicPropertiesStore().saveUnfreezeDelayDays(14);
    dbManager.getDynamicPropertiesStore().saveAllowCancelAllUnfreezeV2(1);
    dbManager.getDynamicPropertiesStore().saveAllowHardenResourceCalculation(1);

    long lastUsage = Long.MAX_VALUE / 10; // ~9.2e17, above threshold
    long usage = 1000L;
    long lastTime = 9999L;
    long now = 10000L;

    ownerCapsule.setNewWindowSize(ResourceCode.ENERGY, 28800);
    ownerCapsule.setWindowOptimized(ResourceCode.ENERGY, true);
    ownerCapsule.setLatestConsumeTimeForEnergy(lastTime);
    ownerCapsule.setEnergyUsage(lastUsage);
    dbManager.getAccountStore().put(
        ownerCapsule.getAddress().toByteArray(), ownerCapsule);

    Assert.assertThrows(ArithmeticException.class,
        () -> processor.increaseV2(ownerCapsule, ResourceCode.ENERGY,
            lastUsage, usage, lastTime, now));
  }

  @Test
  public void testLargeButSafeValuesWithHardening() {
    long lastUsage = 300_000_000_000L; // 300 billion
    long usage = 100L;
    long lastTime = 9990L;
    long now = 9995L;
    long windowSize = 28800L;

    dbManager.getDynamicPropertiesStore().saveAllowHardenResourceCalculation(1);
    long result = processor.increase(lastUsage, usage, lastTime, now, windowSize);
    Assert.assertTrue("Result should be positive", result > 0);
  }

  private void setupForUnDelegate(long now) {
    setupForUnDelegateWithUsage(now, 5_000_000L, 3_000_000L);
  }

  private void setupForUnDelegateWithUsage(long now, long ownerUsage, long receiverUsage) {
    ownerCapsule.setLatestConsumeTimeForEnergy(now);
    ownerCapsule.setEnergyUsage(ownerUsage);
    ownerCapsule.setNewWindowSize(ResourceCode.ENERGY, 28800);
    ownerCapsule.setWindowOptimized(ResourceCode.ENERGY, true);
    dbManager.getAccountStore().put(
        ownerCapsule.getAddress().toByteArray(), ownerCapsule);

    receiverCapsule.setLatestConsumeTimeForEnergy(now - 100);
    receiverCapsule.setEnergyUsage(receiverUsage);
    receiverCapsule.setNewWindowSize(ResourceCode.ENERGY, 28800);
    receiverCapsule.setWindowOptimized(ResourceCode.ENERGY, true);
    dbManager.getAccountStore().put(
        receiverCapsule.getAddress().toByteArray(), receiverCapsule);
  }
}
