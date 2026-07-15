package org.tron.core.vm.config;

import lombok.Setter;
import org.tron.common.parameter.CommonParameter;

/**
 * For developer only
 */
public class VMConfig {

  private static boolean vmTraceCompressed = false;

  @Setter
  private static boolean vmTrace = false;

  /**
   * Snapshot of all chain/store-derived VM config flags. The block-processing (HEAD) path
   * installs it as the process-wide {@link #globalSnapshot}; a constant call executing against a
   * non-HEAD (solidity/PBFT) snapshot installs its own view into {@link #localSnapshot} so it never
   * overwrites the flags the consensus path relies on.
   */
  public static class Snapshot {
    public boolean allowTvmTransferTrc10;
    public boolean allowTvmConstantinople;
    public boolean allowMultiSign;
    public boolean allowTvmSolidity059;
    public boolean allowShieldedTRC20Transaction;
    public boolean allowTvmIstanbul;
    public boolean allowTvmFreeze;
    public boolean allowTvmVote;
    public boolean allowTvmLondon;
    public boolean allowTvmCompatibleEvm;
    public boolean allowHigherLimitForMaxCpuTimeOfOneTx;
    public boolean allowTvmFreezeV2;
    public boolean allowOptimizedReturnValueOfChainId;
    public boolean allowDynamicEnergy;
    public long dynamicEnergyThreshold;
    public long dynamicEnergyIncreaseFactor;
    public long dynamicEnergyMaxFactor;
    public boolean allowTvmShanghai;
    public boolean allowEnergyAdjustment;
    public boolean allowStrictMath;
    public boolean allowTvmCancun;
    public boolean disableJavaLangMath;
    public boolean allowTvmBlob;
    public boolean allowTvmSelfdestructRestriction;
    public boolean allowTvmOsaka;
    public boolean allowHardenResourceCalculation;
  }

  // HEAD / block-processing config, written by the consensus path; read by everyone with no
  // thread-local override. volatile so a wholesale install is safely published across threads.
  private static volatile Snapshot globalSnapshot = new Snapshot();

  // Per-thread override used only by constant calls bound to a non-HEAD (solidity/PBFT) snapshot.
  private static final ThreadLocal<Snapshot> localSnapshot = new ThreadLocal<>();

  private static Snapshot current() {
    Snapshot local = localSnapshot.get();
    return local != null ? local : globalSnapshot;
  }

  /**
   * Install the process-wide (HEAD / block-processing) config and drop any thread-local view.
   */
  public static void setGlobalSnapshot(Snapshot snapshot) {
    globalSnapshot = snapshot;
    localSnapshot.remove();
  }

  /**
   * Install a thread-local config view for a constant call executing against a non-HEAD snapshot.
   */
  public static void setLocalSnapshot(Snapshot snapshot) {
    localSnapshot.set(snapshot);
  }

  /**
   * Drop the thread-local config view so this thread falls back to the global config.
   */
  public static void clearLocalSnapshot() {
    localSnapshot.remove();
  }

  private VMConfig() {
  }

  public static boolean vmTrace() {
    return vmTrace;
  }

  public static boolean vmTraceCompressed() {
    return vmTraceCompressed;
  }

  public static void initVmHardFork(boolean pass) {
    CommonParameter.ENERGY_LIMIT_HARD_FORK = pass;
  }

  // The init* setters below mutate the global (HEAD) config in place. They are kept for tests and
  // legacy callers; production config loading goes through ConfigLoader -> setGlobalSnapshot, which
  // publishes a fresh Snapshot wholesale via the volatile field.
  public static void initAllowMultiSign(long allow) {
    globalSnapshot.allowMultiSign = allow == 1;
  }

  public static void initAllowTvmTransferTrc10(long allow) {
    globalSnapshot.allowTvmTransferTrc10 = allow == 1;
  }

  public static void initAllowTvmConstantinople(long allow) {
    globalSnapshot.allowTvmConstantinople = allow == 1;
  }

  public static void initAllowTvmSolidity059(long allow) {
    globalSnapshot.allowTvmSolidity059 = allow == 1;
  }

  public static void initAllowShieldedTRC20Transaction(long allow) {
    globalSnapshot.allowShieldedTRC20Transaction = allow == 1;
  }

  public static void initAllowTvmIstanbul(long allow) {
    globalSnapshot.allowTvmIstanbul = allow == 1;
  }

  public static void initAllowTvmFreeze(long allow) {
    globalSnapshot.allowTvmFreeze = allow == 1;
  }

  public static void initAllowTvmVote(long allow) {
    globalSnapshot.allowTvmVote = allow == 1;
  }

  public static void initAllowTvmLondon(long allow) {
    globalSnapshot.allowTvmLondon = allow == 1;
  }

  public static void initAllowTvmCompatibleEvm(long allow) {
    globalSnapshot.allowTvmCompatibleEvm = allow == 1;
  }

  public static void initAllowHigherLimitForMaxCpuTimeOfOneTx(long allow) {
    globalSnapshot.allowHigherLimitForMaxCpuTimeOfOneTx = allow == 1;
  }

  public static void initAllowTvmFreezeV2(long allow) {
    globalSnapshot.allowTvmFreezeV2 = allow == 1;
  }

  public static void initAllowOptimizedReturnValueOfChainId(long allow) {
    globalSnapshot.allowOptimizedReturnValueOfChainId = allow == 1;
  }

  public static void initAllowDynamicEnergy(long allow) {
    globalSnapshot.allowDynamicEnergy = allow == 1;
  }

  public static void initDynamicEnergyThreshold(long threshold) {
    globalSnapshot.dynamicEnergyThreshold = threshold;
  }

  public static void initDynamicEnergyIncreaseFactor(long increaseFactor) {
    globalSnapshot.dynamicEnergyIncreaseFactor = increaseFactor;
  }

  public static void initDynamicEnergyMaxFactor(long maxFactor) {
    globalSnapshot.dynamicEnergyMaxFactor = maxFactor;
  }

  public static void initAllowTvmShangHai(long allow) {
    globalSnapshot.allowTvmShanghai = allow == 1;
  }

  public static void initAllowEnergyAdjustment(long allow) {
    globalSnapshot.allowEnergyAdjustment = allow == 1;
  }

  public static void initAllowStrictMath(long allow) {
    globalSnapshot.allowStrictMath = allow == 1;
  }

  public static void initAllowTvmCancun(long allow) {
    globalSnapshot.allowTvmCancun = allow == 1;
  }

  public static void initDisableJavaLangMath(long allow) {
    globalSnapshot.disableJavaLangMath = allow == 1;
  }

  public static void initAllowTvmBlob(long allow) {
    globalSnapshot.allowTvmBlob = allow == 1;
  }

  public static void initAllowTvmSelfdestructRestriction(long allow) {
    globalSnapshot.allowTvmSelfdestructRestriction = allow == 1;
  }

  public static void initAllowTvmOsaka(long allow) {
    globalSnapshot.allowTvmOsaka = allow == 1;
  }

  public static void initAllowHardenResourceCalculation(long allow) {
    globalSnapshot.allowHardenResourceCalculation = allow == 1;
  }

  public static boolean getEnergyLimitHardFork() {
    return CommonParameter.ENERGY_LIMIT_HARD_FORK;
  }

  public static boolean allowTvmTransferTrc10() {
    return current().allowTvmTransferTrc10;
  }

  public static boolean allowTvmConstantinople() {
    return current().allowTvmConstantinople;
  }

  public static boolean allowMultiSign() {
    return current().allowMultiSign;
  }

  public static boolean allowTvmSolidity059() {
    return current().allowTvmSolidity059;
  }

  public static boolean allowShieldedTRC20Transaction() {
    return current().allowShieldedTRC20Transaction;
  }

  public static boolean allowTvmIstanbul() {
    return current().allowTvmIstanbul;
  }

  public static boolean allowTvmFreeze() {
    return current().allowTvmFreeze;
  }

  public static boolean allowTvmVote() {
    return current().allowTvmVote;
  }

  public static boolean allowTvmLondon() {
    return current().allowTvmLondon;
  }

  public static boolean allowTvmCompatibleEvm() {
    return current().allowTvmCompatibleEvm;
  }

  public static boolean allowHigherLimitForMaxCpuTimeOfOneTx() {
    return current().allowHigherLimitForMaxCpuTimeOfOneTx;
  }

  public static boolean allowTvmFreezeV2() {
    return current().allowTvmFreezeV2;
  }

  public static boolean allowOptimizedReturnValueOfChainId() {
    return current().allowOptimizedReturnValueOfChainId;
  }

  public static boolean allowDynamicEnergy() {
    return current().allowDynamicEnergy;
  }

  public static long getDynamicEnergyThreshold() {
    return current().dynamicEnergyThreshold;
  }

  public static long getDynamicEnergyIncreaseFactor() {
    return current().dynamicEnergyIncreaseFactor;
  }

  public static long getDynamicEnergyMaxFactor() {
    return current().dynamicEnergyMaxFactor;
  }

  public static boolean allowTvmShanghai() {
    return current().allowTvmShanghai;
  }

  public static boolean allowEnergyAdjustment() {
    return current().allowEnergyAdjustment;
  }

  public static boolean allowStrictMath() {
    return current().allowStrictMath;
  }

  public static boolean allowTvmCancun() {
    return current().allowTvmCancun;
  }

  public static boolean disableJavaLangMath() {
    return current().disableJavaLangMath;
  }

  public static boolean allowTvmBlob() {
    return current().allowTvmBlob;
  }

  public static boolean allowTvmSelfdestructRestriction() {
    return current().allowTvmSelfdestructRestriction;
  }

  public static boolean allowTvmOsaka() {
    return current().allowTvmOsaka;
  }

  public static boolean allowHardenResourceCalculation() {
    return current().allowHardenResourceCalculation;
  }
}
