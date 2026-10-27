package org.tron.core.vm.nativecontract;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.tron.core.config.Parameter.ChainConstant.TRX_PRECISION;
import static org.tron.protos.contract.Common.ResourceCode.BANDWIDTH;

import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.db.TransactionTrace;
import org.tron.core.store.StoreFactory;
import org.tron.core.vm.config.VMConfig;
import org.tron.core.vm.repository.Repository;
import org.tron.core.vm.repository.RepositoryImpl;

public class Tip944VmTest extends TvmOptimizationVmTestBase {
  @Override
  protected void enableOptimization() {
    VMConfig.initAllowTvmVotePowerOptimization(1);
    dbManager.getDynamicPropertiesStore().saveAllowTvmVotePowerOptimization(1);
  }

  @Test
  public void failedVoteRollsBackRewardAndInitialization() throws Exception {
    install(SOURCE, vote(1, 2));
    stake(SOURCE, BANDWIDTH, TRX_PRECISION);
    pendingReward(SOURCE);
    byte[] before = account(SOURCE).getData();
    TransactionTrace failedVote = execute(SOURCE);
    assertSuccess(failedVote);
    assertEquals(0, returnWord(failedVote, 0));
    assertRejectedInternal(failedVote, "voteWitness");
    finish(failedVote);
    assertArrayEquals(before, account(SOURCE).getData());
    assertEquals(1, dbManager.getDelegationStore().getBeginCycle(SOURCE));
    assertEquals(1, dbManager.getDelegationStore().getEndCycle(SOURCE));
    assertNull(dbManager.getDelegationStore().getAccountVote(2, SOURCE));

    // Positive control: this same reward history really credits allowance on a valid vote.
    Repository root = RepositoryImpl.createRoot(StoreFactory.getInstance());
    root.saveCode(SOURCE, Hex.decode(vote(1, 1)));
    root.commit();
    TransactionTrace successfulVote = execute(SOURCE);
    assertSuccess(successfulVote);
    assertEquals(1, returnWord(successfulVote, 0));
    finish(successfulVote);
    assertEquals(142, account(SOURCE).getAllowance());
    assertEquals(TRX_PRECISION, account(SOURCE).getInstance().getOldTronPower());
    assertEquals(2, dbManager.getDelegationStore().getBeginCycle(SOURCE));
    assertEquals(3, dbManager.getDelegationStore().getEndCycle(SOURCE));
    assertNotNull(dbManager.getDelegationStore().getAccountVote(2, SOURCE));
  }

  @Test
  public void outerRevertRestoresSuccessfulChildVoteRewardAndInitialization() throws Exception {
    install(SOURCE, vote(1, 1));
    install(OUTER, outer(SOURCE, true));
    stake(SOURCE, BANDWIDTH, TRX_PRECISION);
    pendingReward(SOURCE);
    byte[] before = account(SOURCE).getData();
    long[] beforeWeights = weights();
    TransactionTrace trace = execute(OUTER);
    assertTrue(trace.getRuntime().getResult().isRevert());
    assertEquals(1, returnWord(trace, 0)); // Inner VOTEWITNESS succeeded before outer REVERT.
    assertEquals(1, returnWord(trace, 1)); // The CALL also returned success.
    assertRejectedInternal(trace, "voteWitness");
    finish(trace);
    assertArrayEquals(before, account(SOURCE).getData());
    assertArrayEquals(beforeWeights, weights());
    assertEquals(1, dbManager.getDelegationStore().getBeginCycle(SOURCE));
    assertEquals(1, dbManager.getDelegationStore().getEndCycle(SOURCE));
    assertNull(dbManager.getDelegationStore().getAccountVote(2, SOURCE));
    assertNull(dbManager.getVotesStore().get(SOURCE));
  }

  @Test
  public void outerRevertRestoresSuccessfulChildSelfDestructState() throws Exception {
    VMConfig.initAllowTvmSelfdestructRestriction(0);
    install(SOURCE, destroy(BENEFICIARY));
    install(OUTER, outer(SOURCE, true));
    stake(SOURCE, BANDWIDTH, 1_600_000);
    stake(BENEFICIARY, BANDWIDTH, 2_600_000);
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
  public void scheduledSourceCannotReuseLegacyPowerBeforePhysicalDeletion() throws Exception {
    VMConfig.initAllowTvmSelfdestructRestriction(0);
    install(SOURCE, destroyOrVote(BENEFICIARY));
    install(OUTER, twoCalls(SOURCE, SOURCE, 32));
    stake(SOURCE, BANDWIDTH, 2 * TRX_PRECISION);
    legacyVotes(SOURCE, 3 * TRX_PRECISION);
    TransactionTrace trace = execute(OUTER);
    assertSuccess(trace);
    assertEquals(1, returnWord(trace, 1));
    assertEquals(1, returnWord(trace, 2));
    assertEquals(0, returnWord(trace, 0));
    assertEquals(1, trace.getRuntime().getResult().getInternalTransactions().stream()
        .filter(tx -> "voteWitness".equals(tx.getNote()) && tx.isRejected()).count());
    assertNotNull(account(SOURCE));
    assertTrue(dbManager.getCodeStore().has(SOURCE));
    assertEquals(-1, account(SOURCE).getInstance().getOldTronPower());
    assertTrue(account(SOURCE).getVotesList().isEmpty());
    assertTrue(dbManager.getVotesStore().get(SOURCE).getNewVotes().isEmpty());
    assertFalse(trace.getRuntime().getResult().getDeleteAccounts().isEmpty());
    finish(trace);
    assertNull(account(SOURCE));
    assertFalse(dbManager.getCodeStore().has(SOURCE));
    assertTrue(dbManager.getVotesStore().get(SOURCE).getNewVotes().isEmpty());
  }

  private void replaceCode(byte[] address, String code) {
    Repository root = RepositoryImpl.createRoot(StoreFactory.getInstance());
    root.saveCode(address, Hex.decode(code));
    root.commit();
  }

  private static String stakeOneCode(boolean freeze) {
    return pushAddress(SOURCE) + (freeze ? "63004c4b40" : "") + push(0)
        + (freeze ? "d5" : "d6") + push(0) + "52" + push(32) + push(0) + "f3";
  }

  @Test
  public void stakeOneOpcodesUseSnapshotRulesWithIndependentVmFlags() throws Exception {
    // Override the VM flags independently to exercise the getAllTronPower path as well.
    VMConfig.initAllowTvmFreezeV2(0);
    install(SOURCE, stakeOneCode(true));
    AccountCapsule source = account(SOURCE);
    source.setBalance(5 * TRX_PRECISION);
    save(source);
    TransactionTrace frozen = execute(SOURCE);
    assertSuccess(frozen);
    assertEquals(1, returnWord(frozen, 0));
    finish(frozen);
    assertEquals(5 * TRX_PRECISION, account(SOURCE).getFrozenBalance());
    assertEquals(-1, account(SOURCE).getInstance().getOldTronPower());
    replaceCode(SOURCE, vote(1, 5));
    TransactionTrace rejectedVote = execute(SOURCE);
    assertSuccess(rejectedVote);
    assertEquals(0, returnWord(rejectedVote, 0));
    assertRejectedInternal(rejectedVote, "voteWitness");
    finish(rejectedVote);

    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(
        account(SOURCE).getFrozenList().get(0).getExpireTime() + 1);
    replaceCode(SOURCE, stakeOneCode(false));
    TransactionTrace unfrozen = execute(SOURCE);
    assertSuccess(unfrozen);
    assertEquals(1, returnWord(unfrozen, 0));
    finish(unfrozen);
    assertEquals(0, account(SOURCE).getFrozenBalance());
    assertEquals(-1, account(SOURCE).getInstance().getOldTronPower());
    replaceCode(SOURCE, vote(1, 5));
    TransactionTrace afterUnfreeze = execute(SOURCE);
    assertSuccess(afterUnfreeze);
    assertEquals(0, returnWord(afterUnfreeze, 0));
    finish(afterUnfreeze);
  }

  @Test
  public void stakeOneOnlyConfigurationPreservesVotingPowerSelectionWithoutUnfreezeDelay()
      throws Exception {
    VMConfig.initAllowTvmFreezeV2(0);
    dbManager.getDynamicPropertiesStore().saveUnfreezeDelayDays(0);
    install(SOURCE, stakeOneCode(true));
    AccountCapsule source = account(SOURCE);
    source.setBalance(5 * TRX_PRECISION);
    save(source);
    TransactionTrace frozen = execute(SOURCE);
    assertSuccess(frozen);
    assertEquals(1, returnWord(frozen, 0));
    finish(frozen);
    assertEquals(-1, account(SOURCE).getInstance().getOldTronPower());

    replaceCode(SOURCE, vote(1, 5));
    TransactionTrace voted = execute(SOURCE);
    assertSuccess(voted);
    assertEquals(1, returnWord(voted, 0)); // Existing selection still uses getTronPower().
    finish(voted);
    assertEquals(-1, account(SOURCE).getInstance().getOldTronPower());

    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(
        account(SOURCE).getFrozenList().get(0).getExpireTime() + 1);
    replaceCode(SOURCE, stakeOneCode(false));
    TransactionTrace unfrozen = execute(SOURCE);
    assertSuccess(unfrozen);
    assertEquals(1, returnWord(unfrozen, 0));
    finish(unfrozen);
    assertEquals(-1, account(SOURCE).getInstance().getOldTronPower());
    replaceCode(SOURCE, vote(1, 5));
    TransactionTrace rejectedVote = execute(SOURCE);
    assertSuccess(rejectedVote);
    assertEquals(0, returnWord(rejectedVote, 0));
    finish(rejectedVote);
  }

  @Test
  public void outerRevertRestoresStakeOneFreezeAndUninitializedSnapshot() throws Exception {
    VMConfig.initAllowTvmFreezeV2(0);
    install(SOURCE, stakeOneCode(true));
    install(OUTER, outer(SOURCE, true));
    AccountCapsule source = account(SOURCE);
    source.setBalance(5 * TRX_PRECISION);
    save(source);
    byte[] before = account(SOURCE).getData();
    long[] beforeWeights = weights();
    TransactionTrace trace = execute(OUTER);
    assertTrue(trace.getRuntime().getResult().isRevert());
    assertEquals(1, returnWord(trace, 0));
    assertEquals(1, returnWord(trace, 1));
    assertRejectedInternal(trace, "freezeForBandwidth");
    finish(trace);
    assertArrayEquals(before, account(SOURCE).getData());
    assertArrayEquals(beforeWeights, weights());
  }

  @Test
  public void outerRevertRestoresStakeOneUnfreezeSnapshotVotesAndReward() throws Exception {
    VMConfig.initAllowTvmFreezeV2(0);
    install(SOURCE, stakeOneCode(false));
    install(OUTER, outer(SOURCE, true));
    AccountCapsule source = account(SOURCE);
    source.setFrozenForBandwidth(5 * TRX_PRECISION, 0);
    save(source);
    pendingReward(SOURCE);
    byte[] before = account(SOURCE).getData();
    long[] beforeWeights = weights();
    TransactionTrace trace = execute(OUTER);
    assertTrue(trace.getRuntime().getResult().isRevert());
    assertEquals(1, returnWord(trace, 0));
    assertEquals(1, returnWord(trace, 1));
    assertRejectedInternal(trace, "unfreezeForBandwidth");
    finish(trace);
    assertArrayEquals(before, account(SOURCE).getData());
    assertArrayEquals(beforeWeights, weights());
    assertNull(dbManager.getVotesStore().get(SOURCE));
    assertEquals(1, dbManager.getDelegationStore().getBeginCycle(SOURCE));
    assertEquals(1, dbManager.getDelegationStore().getEndCycle(SOURCE));
    assertNull(dbManager.getDelegationStore().getAccountVote(2, SOURCE));
  }

  @Test
  public void stakeTwoActivationKeepsStakeOneFreezeOpcodeDisabled() throws Exception {
    install(SOURCE, stakeOneCode(true));
    AccountCapsule source = account(SOURCE);
    source.setBalance(5 * TRX_PRECISION);
    save(source);
    byte[] before = account(SOURCE).getData();
    TransactionTrace trace = execute(SOURCE);
    assertSuccess(trace);
    assertEquals(0, returnWord(trace, 0));
    finish(trace);
    assertArrayEquals(before, account(SOURCE).getData());
    assertTrue(trace.getRuntime().getResult().getInternalTransactions().isEmpty());
  }
}
