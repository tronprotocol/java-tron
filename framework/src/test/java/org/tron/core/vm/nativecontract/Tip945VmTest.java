package org.tron.core.vm.nativecontract;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.tron.core.config.Parameter.ChainConstant.TRX_PRECISION;
import static org.tron.protos.contract.Common.ResourceCode.ENERGY;

import org.junit.Test;
import org.tron.core.db.TransactionTrace;
import org.tron.core.vm.config.VMConfig;

public class Tip945VmTest extends TvmOptimizationVmTestBase {
  @Override
  protected void enableOptimization() {
    VMConfig.initAllowTvmResourceOptimization(1);
    dbManager.getDynamicPropertiesStore().saveAllowTvmResourceOptimization(1);
  }

  @Test
  public void outerRevertRestoresSuccessfulChildTransferAndWeightDelta() throws Exception {
    VMConfig.initAllowTvmSelfdestructRestriction(0);
    install(SOURCE, destroy(BENEFICIARY));
    install(OUTER, outer(SOURCE, true));
    stake(SOURCE, ENERGY, 1_600_000);
    stake(BENEFICIARY, ENERGY, 2_600_000);
    legacyVotes(SOURCE, 3 * TRX_PRECISION);
    byte[] sourceBefore = account(SOURCE).getData();
    byte[] beneficiaryBefore = account(BENEFICIARY).getData();
    long[] beforeWeights = weights();
    TransactionTrace trace = execute(OUTER);
    assertTrue(trace.getRuntime().getResult().isRevert());
    assertEquals(1, returnWord(trace, 1));
    assertRejectedInternal(trace, "suicide");
    assertTrue(trace.getRuntime().getResult().getDeleteAccounts().isEmpty());
    finish(trace);
    assertArrayEquals(sourceBefore, account(SOURCE).getData());
    assertArrayEquals(beneficiaryBefore, account(BENEFICIARY).getData());
    assertArrayEquals(beforeWeights, weights());
    assertNull(dbManager.getVotesStore().get(SOURCE));
  }

  @Test
  public void physicalDeletionDoesNotApplyWeightDeltaTwice() throws Exception {
    VMConfig.initAllowTvmSelfdestructRestriction(0);
    install(SOURCE, destroy(BENEFICIARY));
    stake(SOURCE, ENERGY, 1_600_000);
    stake(BENEFICIARY, ENERGY, 2_600_000);
    legacyVotes(SOURCE, 3 * TRX_PRECISION);
    long before = weights()[1];
    TransactionTrace trace = execute(SOURCE);
    assertSuccess(trace);
    assertNotNull(account(SOURCE));
    assertEquals(0, account(SOURCE).getFrozenV2BalanceForEnergy());
    assertEquals(1, trace.getRuntime().getResult().getDeleteAccounts().size());
    assertEquals(before + 1, weights()[1]);
    finish(trace);
    assertNull(account(SOURCE));
    assertEquals(before + 1, weights()[1]);
    assertEquals(4_200_000, account(BENEFICIARY).getFrozenV2BalanceForEnergy());
  }

  @Test
  public void twoPositiveTransfersEachAddOneWeightUsingUpdatedBalances() throws Exception {
    install(SOURCE, destroy(BENEFICIARY));
    install(BENEFICIARY, destroy(THIRD));
    install(THIRD, "00");
    stake(SOURCE, ENERGY, 1_600_000);
    stake(BENEFICIARY, ENERGY, 2_600_000);
    stake(THIRD, ENERGY, 1_900_000);
    long before = weights()[1];
    TransactionTrace first = execute(SOURCE);
    assertSuccess(first);
    assertEquals(before + 1, weights()[1]);
    assertEquals(0, account(SOURCE).getFrozenV2BalanceForEnergy());
    assertEquals(4_200_000, account(BENEFICIARY).getFrozenV2BalanceForEnergy());
    finish(first);
    TransactionTrace second = execute(BENEFICIARY);
    assertSuccess(second);
    assertEquals(before + 2, weights()[1]);
    assertEquals(0, account(BENEFICIARY).getFrozenV2BalanceForEnergy());
    assertEquals(6_100_000, account(THIRD).getFrozenV2BalanceForEnergy());
    finish(second);
    assertEquals(before + 2, weights()[1]);
  }

  @Test
  public void twoPositiveTransfersWithinOneTransactionUseChildCommitState() throws Exception {
    install(SOURCE, destroy(BENEFICIARY));
    install(BENEFICIARY, destroy(THIRD));
    install(THIRD, "00");
    install(OUTER, twoCalls(SOURCE, BENEFICIARY, 0));
    stake(SOURCE, ENERGY, 1_600_000);
    stake(BENEFICIARY, ENERGY, 2_600_000);
    stake(THIRD, ENERGY, 1_900_000);
    long before = weights()[1];
    TransactionTrace trace = execute(OUTER);
    assertSuccess(trace);
    assertEquals(1, returnWord(trace, 1));
    assertEquals(1, returnWord(trace, 2));
    assertEquals(before + 2, weights()[1]);
    assertEquals(0, account(SOURCE).getFrozenV2BalanceForEnergy());
    assertEquals(0, account(BENEFICIARY).getFrozenV2BalanceForEnergy());
    assertEquals(6_100_000, account(THIRD).getFrozenV2BalanceForEnergy());
    finish(trace);
    assertEquals(before + 2, weights()[1]);
  }

  @Test
  public void zeroEnergySuccessRestoresFrozenEnergyWindows() throws Exception {
    prepareEnergyWindow();
    stake(CALLER, ENERGY, 1_000L * TRX_PRECISION);
    ownEnergyWeight(CALLER);
    long callerUsage = account(CALLER).getEnergyUsage();
    long callerWindow = account(CALLER).getWindowSize(ENERGY);
    // Caller is also the origin, so only the caller object may be stored.
    TransactionTrace sameAccount = execute(BENEFICIARY);
    assertZeroEnergy(sameAccount);
    assertTrue(account(CALLER).getEnergyUsage() > callerUsage);
    finish(sameAccount);
    assertEquals(callerUsage, account(CALLER).getEnergyUsage());
    assertEquals(callerWindow, account(CALLER).getWindowSize(ENERGY));

    install(SOURCE, "00");
    stake(SOURCE, ENERGY, 1_000L * TRX_PRECISION);
    install(OUTER, "00", SOURCE, 0L, 10_000_000L);
    long callerBefore = account(CALLER).getEnergyUsage();
    long originBefore = account(SOURCE).getEnergyUsage();
    long callerWindowBefore = account(CALLER).getWindowSize(ENERGY);
    long originWindowBefore = account(SOURCE).getWindowSize(ENERGY);
    TransactionTrace shared = execute(OUTER);
    assertZeroEnergy(shared);
    assertTrue(account(CALLER).getEnergyUsage() > callerBefore);
    assertTrue(account(SOURCE).getEnergyUsage() > originBefore);
    finish(shared);
    assertEquals(callerBefore, account(CALLER).getEnergyUsage());
    assertEquals(originBefore, account(SOURCE).getEnergyUsage());
    assertEquals(callerWindowBefore, account(CALLER).getWindowSize(ENERGY));
    assertEquals(originWindowBefore, account(SOURCE).getWindowSize(ENERGY));
    assertEquals(0, shared.getReceipt().getOriginEnergyUsage());
  }

  @Test
  public void zeroEnergySuccessKeepsReservationBeforeProposal() throws Exception {
    VMConfig.initAllowTvmResourceOptimization(0);
    dbManager.getDynamicPropertiesStore().saveAllowTvmResourceOptimization(0);
    prepareEnergyWindow();
    install(SOURCE, "00");
    stake(CALLER, ENERGY, 1_000L * TRX_PRECISION);
    stake(SOURCE, ENERGY, 1_000L * TRX_PRECISION);
    ownEnergyWeight(CALLER, SOURCE);
    install(OUTER, "00", SOURCE, 0L, 10_000_000L);
    TransactionTrace trace = execute(OUTER);
    assertZeroEnergy(trace);
    long callerReserved = account(CALLER).getEnergyUsage();
    long originReserved = account(SOURCE).getEnergyUsage();
    assertTrue(callerReserved > 0);
    assertTrue(originReserved > 0);
    finish(trace);
    assertEquals(callerReserved, account(CALLER).getEnergyUsage());
    assertEquals(originReserved, account(SOURCE).getEnergyUsage());
  }

  private void prepareEnergyWindow() {
    dbManager.getDynamicPropertiesStore().saveEnergyFee(100);
    dbManager.getDynamicPropertiesStore().saveTotalEnergyCurrentLimit(100_000_000L);
  }

  private void ownEnergyWeight(byte[]... addresses) {
    long weight = 0;
    for (byte[] address : addresses) {
      weight += account(address).getFrozenV2BalanceForEnergy() / TRX_PRECISION;
    }
    dbManager.getDynamicPropertiesStore().saveTotalEnergyWeight(weight);
  }

  private void assertZeroEnergy(TransactionTrace trace) {
    assertSuccess(trace);
    assertEquals(0, trace.getReceipt().getEnergyUsageTotal());
    assertEquals(0, trace.getReceipt().getEnergyUsage());
    assertEquals(0, trace.getReceipt().getEnergyFee());
  }
}
