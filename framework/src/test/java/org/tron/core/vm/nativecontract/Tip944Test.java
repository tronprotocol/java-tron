package org.tron.core.vm.nativecontract;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.tron.core.config.Parameter.ChainConstant.TRX_PRECISION;
import static org.tron.protos.contract.Common.ResourceCode.BANDWIDTH;
import static org.tron.protos.contract.Common.ResourceCode.ENERGY;
import static org.tron.protos.contract.Common.ResourceCode.TRON_POWER;

import com.google.protobuf.ByteString;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.TestConstants;
import org.tron.common.runtime.InternalTransaction;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.utils.ForkController;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.ProposalCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.config.Parameter.ForkBlockVersionEnum;
import org.tron.core.config.args.Args;
import org.tron.core.consensus.ProposalService;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.store.StoreFactory;
import org.tron.core.utils.ProposalUtil;
import org.tron.core.vm.config.ConfigLoader;
import org.tron.core.vm.config.VMConfig;
import org.tron.core.vm.program.Program;
import org.tron.core.vm.program.invoke.ProgramInvoke;
import org.tron.core.vm.repository.Repository;
import org.tron.core.vm.repository.RepositoryImpl;
import org.tron.protos.Protocol;
import org.tron.protos.contract.Common.ResourceCode;

public class Tip944Test extends BaseTest {
  private static final byte[] OWNER = address(11);
  private static final byte[] BENEFICIARY = address(12);
  private static final byte[] WITNESS = address(13);
  private Repository repo;

  @BeforeClass
  public static void init() {
    Args.setParam(new String[]{"--output-directory", dbPath()}, TestConstants.TEST_CONF);
  }

  @Before
  public void setup() {
    VMConfig.setGlobalSnapshot(new VMConfig.Snapshot());
    ConfigLoader.disable = true;
    VMConfig.initAllowTvmVote(1);
    VMConfig.initAllowTvmFreezeV2(1);
    VMConfig.initAllowTvmSelfdestructRestriction(1);
    VMConfig.initAllowTvmVotePowerOptimization(1);
    dbManager.getDynamicPropertiesStore().saveAllowNewResourceModel(1);
    dbManager.getDynamicPropertiesStore().saveUnfreezeDelayDays(14);
    dbManager.getDynamicPropertiesStore().saveAllowTvmVotePowerOptimization(0);
    repo = RepositoryImpl.createRoot(StoreFactory.getInstance());
    repo.createAccount(OWNER, Protocol.AccountType.Contract);
    repo.createAccount(BENEFICIARY, Protocol.AccountType.Contract);
    dbManager.getWitnessStore().put(WITNESS, new WitnessCapsule(
        Protocol.Witness.newBuilder().setAddress(ByteString.copyFrom(WITNESS)).build()));
  }

  private static byte[] address(int suffix) {
    byte[] bytes = new byte[21];
    bytes[0] = 0x41;
    bytes[20] = (byte) suffix;
    return bytes;
  }

  private Program program(Repository repository, byte[] owner) {
    ProgramInvoke invoke = mock(ProgramInvoke.class);
    when(invoke.getDeposit()).thenReturn(repository);
    when(invoke.getContractAddress()).thenReturn(new DataWord(owner));
    when(invoke.getTimestamp()).thenReturn(new DataWord(1));
    InternalTransaction transaction = mock(InternalTransaction.class);
    when(transaction.getHash()).thenReturn(new byte[32]);
    return new Program(new byte[0], owner, invoke, transaction);
  }

  private void stake(byte[] owner, ResourceCode type, long amount) {
    AccountCapsule account = repo.getAccount(owner);
    switch (type) {
      case BANDWIDTH:
        account.addFrozenBalanceForBandwidthV2(amount);
        break;
      case ENERGY:
        account.addFrozenBalanceForEnergyV2(amount);
        break;
      case TRON_POWER:
        account.addFrozenForTronPowerV2(amount);
        break;
      default:
        throw new IllegalArgumentException("Unsupported stake type");
    }
    repo.updateAccount(owner, account);
  }

  private void legacy(byte[] owner, long value) {
    AccountCapsule account = repo.getAccount(owner);
    account.setOldTronPower(value);
    repo.updateAccount(owner, account);
  }

  private long legacy(byte[] owner) {
    return repo.getAccount(owner).getInstance().getOldTronPower();
  }

  @Test
  public void governanceRequiresForkAndOneWayActivation() throws Exception {
    ForkController fork = mock(ForkController.class);
    assertEquals(0, dbManager.getDynamicPropertiesStore().getAllowTvmVotePowerOptimization());
    assertThrows(ContractValidateException.class, () -> ProposalUtil.validator(
        dbManager.getDynamicPropertiesStore(), fork, 103, 1));
    when(fork.pass(ForkBlockVersionEnum.VERSION_4_8_3)).thenReturn(true);
    for (long value : new long[]{-1, 0, 2}) {
      assertThrows(ContractValidateException.class, () -> ProposalUtil.validator(
          dbManager.getDynamicPropertiesStore(), fork, 103, value));
    }
    ProposalUtil.validator(dbManager.getDynamicPropertiesStore(), fork, 103, 1);
    assertTrue(ProposalService.process(dbManager, new ProposalCapsule(
        Protocol.Proposal.newBuilder().putParameters(103, 1).build())));
    assertEquals(1, dbManager.getDynamicPropertiesStore().getAllowTvmVotePowerOptimization());
    assertThrows(ContractValidateException.class, () -> ProposalUtil.validator(
        dbManager.getDynamicPropertiesStore(), fork, 103, 1));
    ConfigLoader.disable = false;
    ConfigLoader.load(StoreFactory.getInstance(), false);
    assertTrue(VMConfig.allowTvmVotePowerOptimization());
    dbManager.getDynamicPropertiesStore().saveAllowTvmVotePowerOptimization(0);
    ConfigLoader.load(StoreFactory.getInstance(), true);
    assertFalse(VMConfig.allowTvmVotePowerOptimization());
    VMConfig.clearLocalSnapshot();
    assertTrue(VMConfig.allowTvmVotePowerOptimization());
  }

  private boolean vote(Repository repository, byte[] owner, long amount) {
    Program p = program(repository, owner);
    p.memorySave(0, new DataWord(1).getData());
    p.memorySave(32, new DataWord(WITNESS).getData());
    p.memorySave(64, new DataWord(1).getData());
    p.memorySave(96, new DataWord(amount).getData());
    return p.voteWitness(0, 1, 64, 1);
  }

  @Test
  public void initializesVotingAndPreservesSnapshot() {
    stake(OWNER, BANDWIDTH, 3 * TRX_PRECISION);
    assertTrue(vote(repo, OWNER, 3));
    assertEquals(3 * TRX_PRECISION, legacy(OWNER));
    stake(OWNER, ENERGY, 2 * TRX_PRECISION);
    assertFalse(vote(repo, OWNER, 4));
    assertEquals(3 * TRX_PRECISION, legacy(OWNER));
  }

  @Test
  public void zeroResourceInitializationUsesInvalidSentinelAndExplicitPower() {
    stake(OWNER, TRON_POWER, 2 * TRX_PRECISION);
    assertTrue(vote(repo, OWNER, 2));
    assertEquals(-1, legacy(OWNER));
    stake(OWNER, ENERGY, 4 * TRX_PRECISION);
    assertFalse(vote(repo, OWNER, 3));
    assertEquals(-1, legacy(OWNER));
  }

  @Test
  public void votingIsGatedAndFailedOrNestedExecutionDoesNotPersistInitialization() {
    stake(OWNER, BANDWIDTH, 2 * TRX_PRECISION);
    VMConfig.initAllowTvmVotePowerOptimization(0);
    assertTrue(vote(repo, OWNER, 2));
    assertEquals(0, legacy(OWNER));
    VMConfig.initAllowTvmVotePowerOptimization(1);
    assertFalse(vote(repo, OWNER, 3));
    assertEquals(0, legacy(OWNER));
    Repository child = repo.newRepositoryChild();
    assertTrue(vote(child, OWNER, 2));
    assertEquals(2 * TRX_PRECISION, child.getAccount(OWNER).getInstance().getOldTronPower());
    assertEquals(0, legacy(OWNER));
    dbManager.getDynamicPropertiesStore().saveAllowNewResourceModel(0);
    assertTrue(vote(repo, OWNER, 2));
    assertEquals(0, legacy(OWNER));
  }

  @Test
  public void initializesBeneficiaryBeforeReceiptAndRetiresSource() {
    for (boolean deleted : new boolean[]{false, true}) {
      for (long prior : new long[]{0, 7 * TRX_PRECISION, -1}) {
        setup();
        legacy(BENEFICIARY, prior);
        legacy(OWNER, 4 * TRX_PRECISION);
        stake(OWNER, BANDWIDTH, 2 * TRX_PRECISION);
        stake(OWNER, TRON_POWER, TRX_PRECISION);
        stake(BENEFICIARY, ENERGY, TRX_PRECISION);
        Program p = program(repo, OWNER);
        if (deleted) {
          p.suicide(new DataWord(BENEFICIARY));
          assertEquals(1, p.getResult().getDeleteAccounts().size());
        } else {
          p.suicide2(new DataWord(BENEFICIARY));
          assertTrue(p.getResult().getDeleteAccounts().isEmpty());
        }
        assertEquals(prior == 0 ? TRX_PRECISION : prior, legacy(BENEFICIARY));
        assertEquals(-1, legacy(OWNER));
        assertFalse(vote(repo, OWNER, 1));
        assertTrue(vote(repo, BENEFICIARY, prior == -1 ? 1 : 2));
      }
    }
  }

  @Test
  public void recipientWithoutLegacyStakeIsInvalidatedAndTransferIsForkGated() {
    stake(OWNER, ENERGY, 2 * TRX_PRECISION);
    program(repo, OWNER).suicide2(new DataWord(BENEFICIARY));
    assertEquals(-1, legacy(BENEFICIARY));
    assertFalse(vote(repo, BENEFICIARY, 1));
    setup();
    VMConfig.initAllowTvmVotePowerOptimization(0);
    stake(OWNER, ENERGY, 2 * TRX_PRECISION);
    program(repo, OWNER).suicide2(new DataWord(BENEFICIARY));
    assertEquals(0, legacy(BENEFICIARY));
    assertTrue(vote(repo, BENEFICIARY, 2));
  }

  @Test
  public void stakeOneRedemptionInvalidatesWithOrWithoutVotes() {
    for (boolean votes : new boolean[]{false, true}) {
      setup();
      VMConfig.initAllowTvmFreeze(1);
      VMConfig.initAllowTvmVote(1);
      AccountCapsule owner = repo.getAccount(OWNER);
      owner.setFrozenForBandwidth(2 * TRX_PRECISION, 0);
      owner.setOldTronPower(2 * TRX_PRECISION);
      if (votes) {
        owner.addVotes(ByteString.copyFrom(WITNESS), 2);
      }
      repo.updateAccount(OWNER, owner);
      program(repo, OWNER).suicide2(new DataWord(BENEFICIARY));
      assertEquals(-1, legacy(OWNER));
      assertEquals(0, repo.getAccount(OWNER).getTronPower());
      assertEquals(2 * TRX_PRECISION, repo.getAccount(BENEFICIARY).getBalance());
    }
  }

  @Test
  public void selfBeneficiaryAndZeroBalancesDoNotRetireLegacyState() {
    stake(OWNER, BANDWIDTH, TRX_PRECISION);
    legacy(OWNER, TRX_PRECISION);
    program(repo, OWNER).suicide2(new DataWord(OWNER));
    assertEquals(TRX_PRECISION, legacy(OWNER));
    setup();
    legacy(OWNER, TRX_PRECISION);
    program(repo, OWNER).suicide2(new DataWord(BENEFICIARY));
    assertEquals(TRX_PRECISION, legacy(OWNER));
  }

  @Test
  public void blackHoleReceiptAndTransferRollbackUseActualExecutionState() {
    stake(OWNER, ENERGY, TRX_PRECISION);
    byte[] blackHole = repo.getBlackHoleAddress();
    program(repo, OWNER).suicide(new DataWord(OWNER));
    assertEquals(-1, legacy(OWNER));
    assertEquals(TRX_PRECISION, repo.getAccount(blackHole).getFrozenV2BalanceForEnergy());
    setup();
    stake(OWNER, ENERGY, TRX_PRECISION);
    Repository child = repo.newRepositoryChild();
    program(child, OWNER).suicide2(new DataWord(BENEFICIARY));
    assertEquals(-1, child.getAccount(OWNER).getInstance().getOldTronPower());
    assertEquals(0, legacy(OWNER));
    assertEquals(0, repo.getAccount(BENEFICIARY).getFrozenV2BalanceForEnergy());
  }

  @Test
  public void voteCleanupPreservesInitializedAndInvalidStatesWithoutStakeTransfer() {
    for (long prior : new long[]{3 * TRX_PRECISION, -1}) {
      setup();
      AccountCapsule owner = repo.getAccount(OWNER);
      owner.setOldTronPower(prior);
      owner.addVotes(ByteString.copyFrom(WITNESS), 1);
      owner.setAllowance(123);
      repo.updateAccount(OWNER, owner);
      program(repo, OWNER).suicide2(new DataWord(BENEFICIARY));
      assertEquals(prior, legacy(OWNER));
      assertTrue(repo.getAccount(OWNER).getVotesList().isEmpty());
      assertTrue(repo.getVotes(OWNER).getNewVotes().isEmpty());
      assertEquals(1, repo.getVotes(OWNER).getOldVotes().size());
      assertEquals(0, repo.getAccount(OWNER).getAllowance());
      assertEquals(123, repo.getAccount(BENEFICIARY).getBalance());
    }
  }
}
