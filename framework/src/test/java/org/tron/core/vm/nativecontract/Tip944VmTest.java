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
}
