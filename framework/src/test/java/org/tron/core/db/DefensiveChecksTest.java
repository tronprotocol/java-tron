package org.tron.core.db;

import static org.junit.Assert.assertThrows;

import com.google.protobuf.ByteString;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.TestConstants;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.store.StoreFactory;
import org.tron.core.vm.repository.RepositoryImpl;
import org.tron.protos.Protocol.AccountType;

/**
 * Verifies that assert statements replaced with explicit if-throw checks
 * in PR-1 (Commit 1) correctly throw exceptions when invariants are violated.
 */
public class DefensiveChecksTest extends BaseTest {

  static {
    Args.setParam(new String[]{"--output-directory", dbPath()}, TestConstants.TEST_CONF);
  }

  // -------------------------------------------------------------------------
  // EnergyProcessor.calculateGlobalEnergyLimit()
  // -------------------------------------------------------------------------

  @Test
  public void testEnergyProcessorZeroWeightThrowsWhenNewRewardDisabled() {
    // Arrange: new-reward feature off, energy weight = 0, account with enough frozen balance
    dbManager.getDynamicPropertiesStore().saveUnfreezeDelayDays(0L);
    dbManager.getDynamicPropertiesStore().saveAllowNewReward(0L);
    dbManager.getDynamicPropertiesStore().saveTotalEnergyWeight(0L);

    AccountCapsule account = new AccountCapsule(
        ByteString.copyFromUtf8("test"),
        ByteString.copyFrom(ByteArray.fromHexString(
            "548794500882809695a8a687866e76d4271a1abc")),
        AccountType.Normal,
        0L);
    account.setFrozenForEnergy(1_000_000L, 0L);

    EnergyProcessor processor = new EnergyProcessor(
        dbManager.getDynamicPropertiesStore(), dbManager.getAccountStore());

    assertThrows(IllegalStateException.class,
        () -> processor.calculateGlobalEnergyLimit(account));
  }

  @Test
  public void testEnergyProcessorZeroWeightReturnsZeroWhenNewRewardEnabled() {
    // When allowNewReward is on, totalEnergyWeight == 0 should return 0 (not throw)
    dbManager.getDynamicPropertiesStore().saveUnfreezeDelayDays(0L);
    dbManager.getDynamicPropertiesStore().saveAllowNewReward(1L);
    dbManager.getDynamicPropertiesStore().saveTotalEnergyWeight(0L);

    AccountCapsule account = new AccountCapsule(
        ByteString.copyFromUtf8("test"),
        ByteString.copyFrom(ByteArray.fromHexString(
            "548794500882809695a8a687866e76d4271a1abc")),
        AccountType.Normal,
        0L);
    account.setFrozenForEnergy(1_000_000L, 0L);

    EnergyProcessor processor = new EnergyProcessor(
        dbManager.getDynamicPropertiesStore(), dbManager.getAccountStore());

    long result = processor.calculateGlobalEnergyLimit(account);
    org.junit.Assert.assertEquals(0L, result);
  }

  // -------------------------------------------------------------------------
  // ResourceProcessor.increase() — tested via EnergyProcessor.useEnergy()
  // -------------------------------------------------------------------------

  @Test
  public void testResourceProcessor_increase_timeBackwards_throwsIllegalArgument() {
    // Arrange: no freeze delay (so old increase() path is used), account with
    // latestConsumeTime = 1000 and caller supplies now = 500 (< lastTime)
    dbManager.getDynamicPropertiesStore().saveUnfreezeDelayDays(0L);

    AccountCapsule account = new AccountCapsule(
        ByteString.copyFromUtf8("test2"),
        ByteString.copyFrom(ByteArray.fromHexString(
            "abd4b9367799eaa3197fecb144eb71de1e049abc")),
        AccountType.Normal,
        0L);
    account.setLatestConsumeTimeForEnergy(1000L);
    // frozeBalance = 0 < TRX_PRECISION → calculateGlobalEnergyLimit() returns 0 safely

    EnergyProcessor processor = new EnergyProcessor(
        dbManager.getDynamicPropertiesStore(), dbManager.getAccountStore());

    assertThrows(IllegalArgumentException.class,
        () -> processor.useEnergy(account, 100L, 500L));
  }

  // -------------------------------------------------------------------------
  // RepositoryImpl.calculateGlobalEnergyLimit()
  // -------------------------------------------------------------------------

  @Test
  public void testRepositoryImpl_calculateGlobalEnergyLimit_zeroWeight_throws() {
    dbManager.getDynamicPropertiesStore().saveTotalEnergyWeight(0L);

    AccountCapsule account = new AccountCapsule(
        ByteString.copyFromUtf8("test3"),
        ByteString.copyFrom(ByteArray.fromHexString(
            "548794500882809695a8a687866e76d4271a1abc")),
        AccountType.Normal,
        0L);
    account.setFrozenForEnergy(1_000_000L, 0L);

    RepositoryImpl repository = RepositoryImpl.createRoot(StoreFactory.getInstance());

    assertThrows(IllegalStateException.class,
        () -> repository.calculateGlobalEnergyLimit(account));
  }
}
