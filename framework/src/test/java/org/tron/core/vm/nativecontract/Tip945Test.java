package org.tron.core.vm.nativecontract;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
import org.tron.core.vm.Op;
import org.tron.core.vm.OperationRegistry;
import org.tron.core.vm.VM;
import org.tron.core.vm.config.ConfigLoader;
import org.tron.core.vm.config.VMConfig;
import org.tron.core.vm.program.Program;
import org.tron.core.vm.program.invoke.ProgramInvoke;
import org.tron.core.vm.repository.Repository;
import org.tron.core.vm.repository.RepositoryImpl;
import org.tron.protos.Protocol;
import org.tron.protos.contract.Common.ResourceCode;

public class Tip945Test extends BaseTest {
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
    VMConfig.initAllowTvmFreezeV2(1);
    VMConfig.initAllowTvmSelfdestructRestriction(1);
    VMConfig.initAllowTvmResourceOptimization(1);
    dbManager.getDynamicPropertiesStore().saveAllowNewResourceModel(1);
    dbManager.getDynamicPropertiesStore().saveUnfreezeDelayDays(14);
    dbManager.getDynamicPropertiesStore().saveAllowTvmResourceOptimization(0);
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
    assertEquals(0, dbManager.getDynamicPropertiesStore().getAllowTvmResourceOptimization());
    assertThrows(ContractValidateException.class, () -> ProposalUtil.validator(
        dbManager.getDynamicPropertiesStore(), fork, 104, 1));
    when(fork.pass(ForkBlockVersionEnum.VERSION_4_8_3)).thenReturn(true);
    for (long value : new long[]{-1, 0, 2}) {
      assertThrows(ContractValidateException.class, () -> ProposalUtil.validator(
          dbManager.getDynamicPropertiesStore(), fork, 104, value));
    }
    ProposalUtil.validator(dbManager.getDynamicPropertiesStore(), fork, 104, 1);
    assertTrue(ProposalService.process(dbManager, new ProposalCapsule(
        Protocol.Proposal.newBuilder().putParameters(104, 1).build())));
    assertEquals(1, dbManager.getDynamicPropertiesStore().getAllowTvmResourceOptimization());
    assertThrows(ContractValidateException.class, () -> ProposalUtil.validator(
        dbManager.getDynamicPropertiesStore(), fork, 104, 1));
    ConfigLoader.disable = false;
    ConfigLoader.load(StoreFactory.getInstance(), false);
    assertTrue(VMConfig.allowTvmResourceOptimization());
    dbManager.getDynamicPropertiesStore().saveAllowTvmResourceOptimization(0);
    ConfigLoader.load(StoreFactory.getInstance(), true);
    assertFalse(VMConfig.allowTvmResourceOptimization());
    VMConfig.clearLocalSnapshot();
    assertTrue(VMConfig.allowTvmResourceOptimization());
  }

  private Program arrays(int witnessLength, int amountLength) {
    Program p = spy(program(repo, OWNER));
    p.memorySave(0, new DataWord(witnessLength).getData());
    p.memorySave(2048, new DataWord(amountLength).getData());
    return p;
  }

  @Test
  public void oversizeRejectsBeforeAnyPayloadWorkWithExpectedTrace() {
    Program p = arrays(31, 31);
    assertFalse(p.voteWitness(0, 31, 2048, 31));
    verify(p, never()).memoryChunk(anyInt(), anyInt());
    assertEquals(1, p.getNonce());
    assertEquals(1, p.getResult().getInternalTransactions().size());
    InternalTransaction tx = p.getResult().getInternalTransactions().get(0);
    assertTrue(tx.isRejected());
    assertEquals("", tx.getExtra());
    assertNull(repo.getVotes(OWNER));
  }

  @Test
  public void metadataAndLengthChecksRetainPriority() {
    Program p = arrays(32, 31);
    assertThrows(Program.BytecodeExecutionException.class,
        () -> p.voteWitness(0, 31, 2048, 31));
    for (long activation : new long[]{0, 1}) {
      VMConfig.initAllowTvmResourceOptimization(activation);
      Program mismatch = arrays(31, 30);
      assertFalse(mismatch.voteWitness(0, 31, 2048, 30));
      InternalTransaction tx = mismatch.getResult().getInternalTransactions().get(0);
      assertEquals(activation == 1, tx.isRejected());
      assertEquals("", tx.getExtra());
      assertNull(repo.getVotes(OWNER));
      verify(mismatch, never()).memoryChunk(anyInt(), anyInt());
    }
  }

  @Test
  public void zeroAndMaximumAreAcceptedAndLegacyLimitPathStillBuildsPayload() {
    Program empty = arrays(0, 0);
    assertTrue(empty.voteWitness(0, 0, 2048, 0));
    Program maximum = arrays(30, 30);
    for (int i = 0; i < 30; i++) {
      maximum.memorySave(32 + 32 * i, new DataWord(WITNESS).getData());
    }
    assertTrue(maximum.voteWitness(0, 30, 2048, 30));
    VMConfig.initAllowTvmResourceOptimization(0);
    Program legacy = arrays(31, 31);
    assertFalse(legacy.voteWitness(0, 31, 2048, 31));
    verify(legacy, times(2)).memoryChunk(anyInt(), anyInt());
    assertFalse(legacy.getResult().getInternalTransactions().get(0).getExtra().isEmpty());
  }

  private long[] weights() {
    return new long[]{repo.getTotalNetWeight(), repo.getTotalEnergyWeight(),
        repo.getTotalTronPowerWeight()};
  }

  @Test
  public void independentlyRoundsEachStakeTypeAndIncludesBeneficiaryDelegation() {
    for (ResourceCode type : new ResourceCode[]{BANDWIDTH, ENERGY, TRON_POWER}) {
      for (long beneficiaryAmount : new long[]{2_300_000, 2_400_000, 2_600_000}) {
        setup();
        stake(OWNER, type, 1_600_000);
        stake(BENEFICIARY, type, beneficiaryAmount);
        long[] before = weights();
        program(repo, OWNER).suicide2(new DataWord(BENEFICIARY));
        long[] after = weights();
        for (int i = 0; i < 3; i++) {
          assertEquals(i == type.getNumber() && beneficiaryAmount >= 2_400_000 ? 1 : 0,
              after[i] - before[i]);
        }
      }
    }
    setup();
    stake(OWNER, BANDWIDTH, 1_600_000);
    stake(BENEFICIARY, BANDWIDTH, 600_000);
    AccountCapsule beneficiary = repo.getAccount(BENEFICIARY);
    beneficiary.addDelegatedFrozenV2BalanceForBandwidth(600_000);
    repo.updateAccount(BENEFICIARY, beneficiary);
    long before = repo.getTotalNetWeight();
    program(repo, OWNER).suicide2(new DataWord(BENEFICIARY));
    assertEquals(before, repo.getTotalNetWeight());
  }

  @Test
  public void sequentialTransfersAndDeletedSourcesUseOperationLocalSnapshots() {
    stake(OWNER, ENERGY, 1_600_000);
    stake(BENEFICIARY, ENERGY, 2_600_000);
    long before = repo.getTotalEnergyWeight();
    Program p = program(repo, OWNER);
    p.suicide(new DataWord(BENEFICIARY));
    assertEquals(1, p.getResult().getDeleteAccounts().size());
    assertEquals(before + 1, repo.getTotalEnergyWeight());
    program(repo, OWNER).suicide(new DataWord(BENEFICIARY));
    assertEquals(before + 1, repo.getTotalEnergyWeight());
  }

  @Test
  public void selfBeneficiaryAndRevertedTransferLeaveParentWeightsUnchanged() {
    stake(OWNER, ENERGY, 1_600_000);
    stake(BENEFICIARY, ENERGY, 2_600_000);
    long[] before = weights();
    program(repo, OWNER).suicide2(new DataWord(OWNER));
    assertArrayEquals(before, weights());
    Repository child = repo.newRepositoryChild();
    program(child, OWNER).suicide2(new DataWord(BENEFICIARY));
    assertEquals(before[1] + 1, child.getTotalEnergyWeight());
    assertArrayEquals(before, weights());
    assertEquals(1_600_000, repo.getAccount(OWNER).getFrozenV2BalanceForEnergy());
  }

  @Test
  public void disabledFlagAndNonTransferredBalancesLeaveWeightsUnchanged() {
    stake(OWNER, BANDWIDTH, 1_600_000);
    stake(BENEFICIARY, BANDWIDTH, 2_600_000);
    VMConfig.initAllowTvmResourceOptimization(0);
    long[] before = weights();
    program(repo, OWNER).suicide2(new DataWord(BENEFICIARY));
    assertArrayEquals(before, weights());
    setup();
    AccountCapsule owner = repo.getAccount(OWNER);
    owner.setBalance(3 * TRX_PRECISION);
    owner.addUnfrozenV2List(ENERGY, 2 * TRX_PRECISION, 0);
    repo.updateAccount(OWNER, owner);
    before = weights();
    program(repo, OWNER).suicide2(new DataWord(BENEFICIARY));
    assertArrayEquals(before, weights());
  }

  @Test
  public void correctedTransferDeltaPreservesPreExistingGlobalWeightOffset() {
    stake(OWNER, ENERGY, 1_600_000);
    stake(BENEFICIARY, ENERGY, 2_600_000);
    // Per-account weights total 3; retain a pre-existing global shortfall of 1.
    repo.addTotalEnergyWeight(2 - repo.getTotalEnergyWeight());
    program(repo, OWNER).suicide2(new DataWord(BENEFICIARY));
    assertEquals(0, repo.getAccount(OWNER).getFrozenV2BalanceForEnergy());
    assertEquals(4_200_000, repo.getAccount(BENEFICIARY).getFrozenV2BalanceForEnergy());
    assertEquals(3, repo.getTotalEnergyWeight());
    assertEquals(1, repo.getAccount(BENEFICIARY).getFrozenV2BalanceForEnergy()
        / TRX_PRECISION - repo.getTotalEnergyWeight());
  }

  private Program opcodeProgram(boolean staticCall, long energy) {
    VMConfig.initAllowTvmVote(1);
    ProgramInvoke invoke = mock(ProgramInvoke.class);
    when(invoke.getDeposit()).thenReturn(repo);
    when(invoke.getContractAddress()).thenReturn(new DataWord(OWNER));
    when(invoke.isStaticCall()).thenReturn(staticCall);
    when(invoke.getEnergyLimit()).thenReturn(energy);
    when(invoke.getVmShouldEndInUs()).thenReturn(Long.MAX_VALUE);
    InternalTransaction transaction = mock(InternalTransaction.class);
    when(transaction.getHash()).thenReturn(new byte[32]);
    Program p = new Program(new byte[]{(byte) Op.VOTEWITNESS}, OWNER, invoke, transaction);
    p.setRootTransactionId(new byte[32]);
    p.memorySave(0, new DataWord(31).getData());
    p.memorySave(2048, new DataWord(31).getData());
    p.stackPush(new DataWord(0));
    p.stackPush(new DataWord(31));
    p.stackPush(new DataWord(2048));
    p.stackPush(new DataWord(31));
    return p;
  }

  @Test
  public void actualVoteOpcodePushesZeroForOversizedInput() {
    Program p = opcodeProgram(false, 1_000_000);
    VM.play(p, OperationRegistry.prepareAndGetTable(false));
    assertNull(p.getResult().getException());
    assertEquals(1, p.getStack().size());
    assertEquals(DataWord.ZERO(), p.getStack().peek());
    assertEquals(1, p.getNonce());
    assertTrue(p.getResult().getInternalTransactions().get(0).isRejected());
    assertEquals("", p.getResult().getInternalTransactions().get(0).getExtra());
    assertNull(repo.getVotes(OWNER));
  }

  @Test
  public void actualOpcodeEnergyAndStaticChecksPrecedeCountRejection() {
    for (long activation : new long[]{0, 1}) {
      VMConfig.initAllowTvmResourceOptimization(activation);
      Program insufficient = opcodeProgram(true, 1);
      VM.play(insufficient, OperationRegistry.prepareAndGetTable(false));
      assertTrue(insufficient.getResult().getException() instanceof Program.OutOfEnergyException);
      assertEquals(0, insufficient.getNonce());
      assertTrue(insufficient.getResult().getInternalTransactions().isEmpty());
      Program staticCall = opcodeProgram(true, 1_000_000);
      VM.play(staticCall, OperationRegistry.prepareAndGetTable(false));
      assertTrue(staticCall.getResult().getException()
          instanceof Program.StaticCallModificationException);
      assertEquals(0, staticCall.getNonce());
      assertTrue(staticCall.getResult().getInternalTransactions().isEmpty());
      assertNull(repo.getVotes(OWNER));
    }
  }

  @Test
  public void independentActivationPreservesVotingStateAndWeightBoundaries() {
    for (long power : new long[]{0, 1}) {
      for (long resource : new long[]{0, 1}) {
        setup();
        VMConfig.initAllowTvmVotePowerOptimization(power);
        VMConfig.initAllowTvmResourceOptimization(resource);
        stake(OWNER, BANDWIDTH, 1_600_000);
        stake(BENEFICIARY, BANDWIDTH, 2_600_000);
        Program p = program(repo, OWNER);
        p.memorySave(0, new DataWord(31).getData());
        p.memorySave(2048, new DataWord(31).getData());
        assertFalse(p.voteWitness(0, 31, 2048, 31));
        assertEquals(0, legacy(OWNER));
        assertEquals(resource == 1,
            p.getResult().getInternalTransactions().get(0).getExtra().isEmpty());
        long before = repo.getTotalNetWeight();
        p.suicide2(new DataWord(BENEFICIARY));
        assertEquals(before + resource, repo.getTotalNetWeight());
        assertEquals(power == 1 ? -1 : 0, legacy(OWNER));
        assertEquals(power == 1 ? 2_600_000 : 0, legacy(BENEFICIARY));
      }
    }
  }
}
