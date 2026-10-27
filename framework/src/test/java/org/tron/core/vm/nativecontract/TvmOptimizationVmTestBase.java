package org.tron.core.vm.nativecontract;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.tron.core.config.Parameter.ChainConstant.TRX_PRECISION;

import com.google.protobuf.ByteString;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Locale;
import org.bouncycastle.util.encoders.Hex;
import org.tron.common.BaseMethodTest;
import org.tron.common.runtime.RuntimeImpl;
import org.tron.common.runtime.TvmTestUtils;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.ContractCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.db.TransactionTrace;
import org.tron.core.store.DelegationStore;
import org.tron.core.store.StoreFactory;
import org.tron.core.vm.config.ConfigLoader;
import org.tron.core.vm.config.VMConfig;
import org.tron.core.vm.repository.Repository;
import org.tron.core.vm.repository.RepositoryImpl;
import org.tron.protos.Protocol;
import org.tron.protos.contract.Common.ResourceCode;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;

/** Real transaction/CALL fixtures for the independent TVM optimization proposals. */
public abstract class TvmOptimizationVmTestBase extends BaseMethodTest {
  protected static final byte[] CALLER = address(21);
  protected static final byte[] SOURCE = address(22);
  protected static final byte[] BENEFICIARY = address(23);
  protected static final byte[] OUTER = address(24);
  protected static final byte[] WITNESS = address(25);
  protected static final byte[] THIRD = address(26);

  @Override
  protected String[] extraArgs() {
    return new String[]{"--debug"};
  }

  @Override
  protected void afterInit() {
    ConfigLoader.disable = true;
    VMConfig.setGlobalSnapshot(new VMConfig.Snapshot());
    VMConfig.initVmHardFork(true);
    VMConfig.initAllowTvmTransferTrc10(1);
    VMConfig.initAllowTvmConstantinople(1);
    VMConfig.initAllowTvmSolidity059(1);
    VMConfig.initAllowTvmIstanbul(1);
    VMConfig.initAllowTvmFreeze(1);
    VMConfig.initAllowTvmFreezeV2(1);
    VMConfig.initAllowTvmVote(1);
    VMConfig.initAllowEnergyAdjustment(1);
    VMConfig.initAllowTvmSelfdestructRestriction(1);
    dbManager.getDynamicPropertiesStore().saveAllowNewResourceModel(1);
    dbManager.getDynamicPropertiesStore().saveUnfreezeDelayDays(14);
    dbManager.getDynamicPropertiesStore().saveAllowTvmVote(1);
    dbManager.getDynamicPropertiesStore().saveCurrentCycleNumber(0);
    enableOptimization();
    Repository root = RepositoryImpl.createRoot(StoreFactory.getInstance());
    root.createAccount(CALLER, Protocol.AccountType.Normal);
    root.addBalance(CALLER, 1_000_000_000_000_000L);
    root.commit();
    dbManager.getWitnessStore().put(WITNESS, new WitnessCapsule(
        Protocol.Witness.newBuilder().setAddress(ByteString.copyFrom(WITNESS)).build()));
    install(BENEFICIARY, "00");
  }

  protected abstract void enableOptimization();

  protected void install(byte[] address, String runtime) {
    Repository root = RepositoryImpl.createRoot(StoreFactory.getInstance());
    root.createAccount(address, Protocol.AccountType.Contract);
    root.createContract(address, new ContractCapsule(SmartContract.newBuilder()
        .setContractAddress(ByteString.copyFrom(address))
        .setOriginAddress(ByteString.copyFrom(CALLER))
        .setConsumeUserResourcePercent(100).setOriginEnergyLimit(1_000_000L).build()));
    root.saveCode(address, Hex.decode(runtime));
    root.commit();
  }

  protected AccountCapsule account(byte[] address) {
    return dbManager.getAccountStore().get(address);
  }

  protected void save(AccountCapsule account) {
    dbManager.getAccountStore().put(account.createDbKey(), account);
  }

  protected void stake(byte[] address, ResourceCode type, long amount) {
    AccountCapsule account = account(address);
    switch (type) {
      case BANDWIDTH:
        long oldNet = account.getFrozenV2BalanceWithDelegated(type) / TRX_PRECISION;
        account.addFrozenBalanceForBandwidthV2(amount);
        dbManager.getDynamicPropertiesStore().saveTotalNetWeight(
            dbManager.getDynamicPropertiesStore().getTotalNetWeight()
                + account.getFrozenV2BalanceWithDelegated(type) / TRX_PRECISION - oldNet);
        break;
      case ENERGY:
        long oldEnergy = account.getFrozenV2BalanceWithDelegated(type) / TRX_PRECISION;
        account.addFrozenBalanceForEnergyV2(amount);
        dbManager.getDynamicPropertiesStore().saveTotalEnergyWeight(
            dbManager.getDynamicPropertiesStore().getTotalEnergyWeight()
                + account.getFrozenV2BalanceWithDelegated(type) / TRX_PRECISION - oldEnergy);
        break;
      default:
        throw new IllegalArgumentException("Unsupported fixture stake type");
    }
    save(account);
  }

  protected long[] weights() {
    return new long[]{dbManager.getDynamicPropertiesStore().getTotalNetWeight(),
        dbManager.getDynamicPropertiesStore().getTotalEnergyWeight(),
        dbManager.getDynamicPropertiesStore().getTotalTronPowerWeight()};
  }

  protected void legacyVotes(byte[] address, long legacy) {
    AccountCapsule account = account(address);
    account.setOldTronPower(legacy);
    account.addVotes(ByteString.copyFrom(WITNESS), 1);
    save(account);
  }

  protected void pendingReward(byte[] address) {
    legacyVotes(address, 0);
    AccountCapsule account = account(address);
    account.setAllowance(100);
    save(account);
    dbManager.getDynamicPropertiesStore().saveCurrentCycleNumber(2);
    DelegationStore delegation = dbManager.getDelegationStore();
    delegation.setBeginCycle(address, 1);
    delegation.setEndCycle(address, 1);
    delegation.setWitnessVi(0, WITNESS, BigInteger.ZERO);
    delegation.setWitnessVi(1, WITNESS,
        DelegationStore.DECIMAL_OF_VI_REWARD.multiply(BigInteger.valueOf(42)));
  }

  protected TransactionTrace execute(byte[] contract) throws Exception {
    TransactionCapsule tx = new TransactionCapsule(
        TvmTestUtils.generateTriggerSmartContractAndGetTransaction(
            CALLER, contract, new byte[0], 0, 1_000_000_000L));
    TransactionTrace trace =
        new TransactionTrace(tx, StoreFactory.getInstance(), new RuntimeImpl());
    tx.setTrxTrace(trace);
    trace.init(null);
    trace.exec();
    return trace;
  }

  protected void finish(TransactionTrace trace) throws Exception {
    trace.finalization();
    trace.setResult();
  }

  protected void assertSuccess(TransactionTrace trace) {
    assertNull(trace.getRuntime().getRuntimeError());
    assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS,
        trace.getRuntime().getResult().getResultCode());
  }

  protected void assertRejectedInternal(TransactionTrace trace, String note) {
    assertEquals(1, trace.getRuntime().getResult().getInternalTransactions().stream()
        .filter(tx -> note.equals(tx.getNote()) && tx.isRejected()).count());
  }

  protected long returnWord(TransactionTrace trace, int index) {
    byte[] data = trace.getRuntime().getResult().getHReturn();
    return new BigInteger(1, Arrays.copyOfRange(data, index * 32, (index + 1) * 32)).longValue();
  }

  protected static String push(int value) {
    return "61" + String.format(Locale.ROOT, "%04x", value);
  }

  protected static String pushAddress(byte[] address) {
    return "73" + Hex.toHexString(Arrays.copyOfRange(address, 1, 21));
  }

  protected static String destroy(byte[] beneficiary) {
    return pushAddress(beneficiary) + "ff";
  }

  protected static String vote(int count, int amount) {
    // Two ABI-style arrays at memory offsets 0 and 2048; VOTEWITNESS returns one stack word.
    String headers = push(count) + push(0) + "52" + push(count) + push(2048) + "52";
    String entry = pushAddress(WITNESS) + push(32) + "52"
        + push(amount) + push(2080) + "52";
    return headers + entry + push(0) + push(count) + push(2048) + push(count) + "d8"
        + push(0) + "52" + push(32) + push(0) + "f3";
  }

  protected static String call(byte[] target, int inputSize) {
    return push(32) + push(0) + push(inputSize) + push(0) + push(0)
        + pushAddress(target) + "5af1";
  }

  protected static String outer(byte[] child, boolean revert) {
    // Return/revert the inner return value and CALL success, proving successful child execution.
    return call(child, 0) + push(32) + "52" + push(64) + push(0) + (revert ? "fd" : "f3");
  }

  protected static String twoCalls(byte[] first, byte[] second, int secondInputSize) {
    return call(first, 0) + push(32) + "52" + call(second, secondInputSize)
        + push(64) + "52" + push(96) + push(0) + "f3";
  }

  protected static String destroyOrVote(byte[] beneficiary) {
    String destruction = destroy(beneficiary);
    int voteOffset = 5 + Hex.decode(destruction).length;
    return "3661" + String.format(Locale.ROOT, "%04x", voteOffset) + "57"
        + destruction + "5b" + vote(1, 1);
  }

  private static byte[] address(int last) {
    byte[] address = new byte[21];
    address[0] = 0x41;
    address[20] = (byte) last;
    return address;
  }
}
