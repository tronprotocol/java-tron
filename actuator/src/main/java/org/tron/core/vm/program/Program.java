package org.tron.core.vm.program;

import static java.lang.StrictMath.min;
import static java.lang.String.format;
import static org.apache.commons.lang3.ArrayUtils.EMPTY_BYTE_ARRAY;
import static org.apache.commons.lang3.ArrayUtils.getLength;
import static org.apache.commons.lang3.ArrayUtils.isEmpty;
import static org.apache.commons.lang3.ArrayUtils.isNotEmpty;
import static org.apache.commons.lang3.ArrayUtils.nullToEmpty;
import static org.tron.common.utils.ByteUtil.stripLeadingZeroes;
import static org.tron.core.config.Parameter.ChainConstant.TRX_PRECISION;

import com.google.protobuf.ByteString;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.map.LRUMap;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.bouncycastle.util.encoders.Hex;
import org.tron.common.crypto.Hash;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.runtime.InternalTransaction;
import org.tron.common.runtime.ProgramResult;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.utils.BIUtil;
import org.tron.common.utils.ByteUtil;
import org.tron.common.utils.FastByteComparisons;
import org.tron.common.utils.Utils;
import org.tron.common.utils.WalletUtil;
import org.tron.core.ChainBaseManager;
import org.tron.core.Constant;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.ContractCapsule;
import org.tron.core.capsule.ContractStateCapsule;
import org.tron.core.capsule.DelegatedResourceCapsule;
import org.tron.core.capsule.VotesCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.db.BandwidthProcessor;
import org.tron.core.db.EnergyProcessor;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.TronException;
import org.tron.core.utils.TransactionUtil;
import org.tron.core.vm.EnergyCost;
import org.tron.core.vm.MessageCall;
import org.tron.core.vm.Op;
import org.tron.core.vm.OperationRegistry;
import org.tron.core.vm.PrecompiledContracts;
import org.tron.core.vm.VM;
import org.tron.core.vm.VMConstant;
import org.tron.core.vm.VMUtils;
import org.tron.core.vm.config.VMConfig;
import org.tron.core.vm.nativecontract.CancelAllUnfreezeV2Processor;
import org.tron.core.vm.nativecontract.DelegateResourceProcessor;
import org.tron.core.vm.nativecontract.FreezeBalanceProcessor;
import org.tron.core.vm.nativecontract.FreezeBalanceV2Processor;
import org.tron.core.vm.nativecontract.UnDelegateResourceProcessor;
import org.tron.core.vm.nativecontract.UnfreezeBalanceProcessor;
import org.tron.core.vm.nativecontract.UnfreezeBalanceV2Processor;
import org.tron.core.vm.nativecontract.VoteWitnessProcessor;
import org.tron.core.vm.nativecontract.WithdrawExpireUnfreezeProcessor;
import org.tron.core.vm.nativecontract.WithdrawRewardProcessor;
import org.tron.core.vm.nativecontract.param.CancelAllUnfreezeV2Param;
import org.tron.core.vm.nativecontract.param.DelegateResourceParam;
import org.tron.core.vm.nativecontract.param.FreezeBalanceParam;
import org.tron.core.vm.nativecontract.param.FreezeBalanceV2Param;
import org.tron.core.vm.nativecontract.param.UnDelegateResourceParam;
import org.tron.core.vm.nativecontract.param.UnfreezeBalanceParam;
import org.tron.core.vm.nativecontract.param.UnfreezeBalanceV2Param;
import org.tron.core.vm.nativecontract.param.VoteWitnessParam;
import org.tron.core.vm.nativecontract.param.WithdrawExpireUnfreezeParam;
import org.tron.core.vm.nativecontract.param.WithdrawRewardParam;
import org.tron.core.vm.program.invoke.ProgramInvoke;
import org.tron.core.vm.program.invoke.ProgramInvokeFactory;
import org.tron.core.vm.program.listener.CompositeProgramListener;
import org.tron.core.vm.program.listener.ProgramListenerAware;
import org.tron.core.vm.program.listener.ProgramStorageChangeListener;
import org.tron.core.vm.repository.Key;
import org.tron.core.vm.repository.Repository;
import org.tron.core.vm.trace.ProgramTrace;
import org.tron.core.vm.trace.ProgramTraceListener;
import org.tron.core.vm.utils.MUtil;
import org.tron.core.vm.utils.VoteRewardUtil;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.contract.Common;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract.Builder;

@Slf4j(topic = "VM")
public class Program {

  private static final int MAX_DEPTH = 64;
  //Max size for stack checks
  private static final int MAX_STACK_SIZE = 1024;
  private static final String VALIDATE_FOR_SMART_CONTRACT_FAILURE =
      "validateForSmartContract failure:%s";
  private static final String INVALID_TOKEN_ID_MSG = "not valid token id";
  private static final String REFUND_ENERGY_FROM_MESSAGE_CALL = "refund energy from message call";
  private static final String CALL_PRE_COMPILED = "call pre-compiled";
  private static final int lruCacheSize = CommonParameter.getInstance().getSafeLruCacheSize();
  private static final LRUMap<Key, ProgramPrecompile> programPrecompileLRUMap
      = new LRUMap<>(lruCacheSize);
  private long nonce;
  private byte[] rootTransactionId;
  private InternalTransaction internalTransaction;
  private ProgramInvoke invoke;
  private ProgramOutListener listener;
  private ProgramTraceListener traceListener;
  private ProgramStorageChangeListener storageDiffListener = new ProgramStorageChangeListener();
  private CompositeProgramListener programListener = new CompositeProgramListener();
  private Stack stack;
  private Memory memory;
  private ContractState contractState;
  private byte[] returnDataBuffer;
  private ProgramResult result = new ProgramResult();
  private ProgramTrace trace = new ProgramTrace();
  private byte[] ops;
  private byte[] codeAddress;
  private int pc;
  private byte lastOp;
  private byte previouslyExecutedOp;
  private boolean stopped;
  private ProgramPrecompile programPrecompile;
  private int contractVersion;
  private DataWord adjustedCallEnergy;
  @Getter
  @Setter
  private long contextContractFactor;
  @Getter
  @Setter
  private long callPenaltyEnergy;

  public Program(byte[] ops, byte[] codeAddress, ProgramInvoke programInvoke,
                 InternalTransaction internalTransaction) {
    this.invoke = programInvoke;
    this.internalTransaction = internalTransaction;
    this.ops = nullToEmpty(ops);
    this.codeAddress = codeAddress;

    traceListener = new ProgramTraceListener(VMConfig.vmTrace());
    this.memory = setupProgramListener(new Memory());
    this.stack = setupProgramListener(new Stack());
    this.contractState = setupProgramListener(new ContractState(programInvoke));
    this.trace = new ProgramTrace(programInvoke);
    this.nonce = internalTransaction.getNonce();
  }

  static String formatBinData(byte[] binData, int startPC) {
    StringBuilder ret = new StringBuilder();
    for (int i = 0; i < binData.length; i += 16) {
      ret.append(Utils.align("" + Integer.toHexString(startPC + (i)) + ":", ' ', 8, false));
      ret.append(Hex.toHexString(binData, i, min(16, binData.length - i))).append('\n');
    }
    return ret.toString();
  }

  public byte[] getRootTransactionId() {
    return rootTransactionId.clone();
  }

  public void setRootTransactionId(byte[] rootTransactionId) {
    this.rootTransactionId = rootTransactionId.clone();
  }

  public void setContractVersion(int version) {
    this.contractVersion = version;
  }

  public int getContractVersion() {
    return this.contractVersion;
  }

  public void setAdjustedCallEnergy(DataWord adjustedCallEnergy) {
    this.adjustedCallEnergy = adjustedCallEnergy;
  }

  public DataWord getAdjustedCallEnergy() {
    return this.adjustedCallEnergy;
  }

  public long getNonce() {
    return nonce;
  }

  public void setNonce(long nonceValue) {
    nonce = nonceValue;
  }

  public ProgramPrecompile getProgramPrecompile() {
    if (isConstantCall()) {
      if (programPrecompile == null) {
        programPrecompile = ProgramPrecompile.compile(ops);
      }
      return programPrecompile;
    }
    if (programPrecompile == null) {
      Key key = getJumpDestAnalysisCacheKey();
      if (programPrecompileLRUMap.containsKey(key)) {
        programPrecompile = programPrecompileLRUMap.get(key);
      } else {
        programPrecompile = ProgramPrecompile.compile(ops);
        programPrecompileLRUMap.put(key, programPrecompile);
      }
    }
    return programPrecompile;
  }

  public int getCallDeep() {
    return invoke.getCallDeep();
  }

  /**
   * @param transferAddress the address send TRX to.
   * @param value the TRX value transferred in the internal transaction
   */
  private InternalTransaction addInternalTx(DataWord energyLimit, byte[] senderAddress,
      byte[] transferAddress,
      long value, byte[] data, String note, long nonce, Map<String, Long> tokenInfo) {

    InternalTransaction addedInternalTx = null;
    if (internalTransaction != null) {
      addedInternalTx = getResult()
          .addInternalTransaction(internalTransaction.getHash(), getCallDeep(),
              senderAddress, transferAddress, value, data, note, nonce, tokenInfo);
    }

    return addedInternalTx;
  }

  private <T extends ProgramListenerAware> T setupProgramListener(T programListenerAware) {
    if (programListener.isEmpty()) {
      programListener.addListener(traceListener);
      programListener.addListener(storageDiffListener);
    }

    programListenerAware.setProgramListener(programListener);

    return programListenerAware;
  }

  public Map<DataWord, DataWord> getStorageDiff() {
    return storageDiffListener.getDiff();
  }

  public byte getOp(int pc) {
    return (getLength(ops) <= pc) ? 0 : ops[pc];
  }

  public byte getCurrentOp() {
    return isEmpty(ops) ? 0 : ops[pc];
  }

  public int getCurrentOpIntValue() {
    return getCurrentOp() & 0xff;
  }

  /**
   * Last Op can only be set publicly (no getLastOp method), is used for logging.
   */
  public void setLastOp(byte op) {
    this.lastOp = op;
  }

  /**
   * Returns the last fully executed OP.
   */
  public byte getPreviouslyExecutedOp() {
    return this.previouslyExecutedOp;
  }

  /**
   * Should be set only after the OP is fully executed.
   */
  public void setPreviouslyExecutedOp(byte op) {
    this.previouslyExecutedOp = op;
  }

  public void stackPush(byte[] data) {
    stackPush(new DataWord(data));
  }

  public void stackPush(DataWord stackWord) {
    verifyStackOverflow(0, 1); //Sanity Check
    stack.push(stackWord);
  }

  public void stackPushZero() {
    stackPush(DataWord.ZERO());
  }

  public void stackPushOne() {
    stackPush(DataWord.ONE());
  }

  public Stack getStack() {
    return this.stack;
  }

  public int getPC() {
    return pc;
  }

  public void setPC(DataWord pc) {
    this.setPC(pc.intValue());
  }

  public void setPC(int pc) {
    this.pc = pc;

    if (this.pc >= ops.length) {
      stop();
    }
  }

  public boolean isStopped() {
    return stopped;
  }

  public void stop() {
    stopped = true;
  }

  public void setHReturn(byte[] buff) {
    getResult().setHReturn(buff);
  }

  public void step() {
    setPC(pc + 1);
  }

  public byte[] sweep(int n) {

    if (pc + n > ops.length) {
      stop();
    }

    byte[] data = Arrays.copyOfRange(ops, pc, pc + n);
    pc += n;
    if (pc >= ops.length) {
      stop();
    }

    return data;
  }

  public DataWord stackPop() {
    return stack.pop();
  }

  /**
   * . Verifies that the stack is at least <code>stackSize</code>
   *
   * @param stackSize int
   * @throws StackTooSmallException If the stack is smaller than <code>stackSize</code>
   */
  public void verifyStackSize(int stackSize) {
    if (stack.size() < stackSize) {
      throw Exception.tooSmallStack(stackSize, stack.size());
    }
  }

  public void verifyStackOverflow(int argsReqs, int returnReqs) {
    if ((stack.size() - argsReqs + returnReqs) > MAX_STACK_SIZE) {
      throw new StackTooLargeException(
          "Expected: overflow " + MAX_STACK_SIZE + " elements stack limit");
    }
  }

  public int getMemSize() {
    return memory.size();
  }

  public void memorySave(DataWord addrB, DataWord value) {
    memory.write(addrB.intValue(), value.getData(), value.getData().length, false);
  }

  public void memorySave(int addr, byte[] value) {
    memory.write(addr, value, value.length, false);
  }

  /**
   * . Allocates a piece of memory and stores value at given offset address
   *
   * @param addr is the offset address
   * @param allocSize size of memory needed to write
   * @param value the data to write to memory
   */
  public void memorySave(int addr, int allocSize, byte[] value) {
    memory.extendAndWrite(addr, allocSize, value);
  }

  public void memorySaveLimited(int addr, byte[] data, int dataSize) {
    memory.write(addr, data, dataSize, true);
  }

  public void memoryExpand(DataWord outDataOffs, DataWord outDataSize) {
    if (!outDataSize.isZero()) {
      memory.extend(outDataOffs.intValue(), outDataSize.intValue());
    }
  }

  public DataWord memoryLoad(DataWord addr) {
    return memory.readWord(addr.intValue());
  }

  public DataWord memoryLoad(int address) {
    return memory.readWord(address);
  }

  public byte[] memoryChunk(int offset, int size) {
    return memory.read(offset, size);
  }

  /**
   * . Allocates extra memory in the program for a specified size, calculated from a given offset
   *
   * @param offset the memory address offset
   * @param size the number of bytes to allocate
   */
  public void allocateMemory(int offset, int size) {
    memory.extend(offset, size);
  }

  public void suicide(DataWord obtainerAddress) {

    byte[] owner = getContextAddress();
    byte[] obtainer = obtainerAddress.toTronAddress();

    if (VMConfig.allowTvmVote()) {
      withdrawRewardAndCancelVote(owner, getContractState());
    }

    long balance = getContractState().getBalance(owner);

    if (logger.isDebugEnabled()) {
      logger.debug("Transfer to: [{}] heritage: [{}]",
          Hex.toHexString(obtainer),
          balance);
    }

    increaseNonce();

    InternalTransaction internalTx = addInternalTx(null, owner, obtainer, balance, null,
        "suicide", nonce, getContractState().getAccount(owner).getAssetMapV2());

    if (FastByteComparisons.compareTo(owner, 0, 20, obtainer, 0, 20) == 0) {
      // if owner == obtainer just zeroing account according to Yellow Paper
      getContractState().addBalance(owner, -balance);
      byte[] blackHoleAddress = getContractState().getBlackHoleAddress();
      if (VMConfig.allowTvmTransferTrc10()) {
        getContractState().addBalance(blackHoleAddress, balance);
        MUtil.transferAllToken(getContractState(), owner, blackHoleAddress);
      }
    } else {
      createAccountIfNotExist(getContractState(), obtainer);
      try {
        MUtil.transfer(getContractState(), owner, obtainer, balance);
        if (VMConfig.allowTvmTransferTrc10()) {
          MUtil.transferAllToken(getContractState(), owner, obtainer);
        }
      } catch (ContractValidateException e) {
        if (VMConfig.allowTvmConstantinople()) {
          throw new TransferException(
              "transfer all token or transfer all trx failed in suicide: %s", e.getMessage());
        }
        throw new BytecodeExecutionException("transfer failure");
      }
    }
    if (VMConfig.allowTvmFreeze()) {
      byte[] blackHoleAddress = getContractState().getBlackHoleAddress();
      if (FastByteComparisons.isEqual(owner, obtainer)) {
        transferDelegatedResourceToInheritor(owner, blackHoleAddress, getContractState());
      } else {
        transferDelegatedResourceToInheritor(owner, obtainer, getContractState());
      }
    }
    if (VMConfig.allowTvmFreezeV2()) {
      byte[] Inheritor =
          FastByteComparisons.isEqual(owner, obtainer)
              ? getContractState().getBlackHoleAddress()
              : obtainer;
      long expireUnfrozenBalance = transferFrozenV2BalanceToInheritor(owner, Inheritor, getContractState());
      if (expireUnfrozenBalance > 0 && internalTx != null) {
        internalTx.setValue(internalTx.getValue() + expireUnfrozenBalance);
      }
    }
    getResult().addDeleteAccount(this.getContractAddress());
  }

  public Repository getContractState() {
    return this.contractState;
  }

  private void transferDelegatedResourceToInheritor(byte[] ownerAddr, byte[] inheritorAddr, Repository repo) {

    // delegated resource from sender to owner, just abandon
    // in order to making that sender can unfreeze their balance in future
    // nothing will be deleted

    // delegated resource from owner to receiver
    // there cannot be any resource when suicide

    AccountCapsule ownerCapsule = repo.getAccount(ownerAddr);

    // transfer owner`s frozen balance for bandwidth to inheritor
    long frozenBalanceForBandwidthOfOwner = 0;
    // check if frozen for bandwidth exists
    if (ownerCapsule.getFrozenCount() != 0) {
      frozenBalanceForBandwidthOfOwner = ownerCapsule.getFrozenList().get(0).getFrozenBalance();
    }
    repo.addTotalNetWeight(-frozenBalanceForBandwidthOfOwner / TRX_PRECISION);

    long frozenBalanceForEnergyOfOwner =
        ownerCapsule.getAccountResource().getFrozenBalanceForEnergy().getFrozenBalance();
    repo.addTotalEnergyWeight(-frozenBalanceForEnergyOfOwner / TRX_PRECISION);

    // transfer all kinds of frozen balance to BlackHole
    repo.addBalance(inheritorAddr, frozenBalanceForBandwidthOfOwner + frozenBalanceForEnergyOfOwner);
  }

  private long transferFrozenV2BalanceToInheritor(byte[] ownerAddr, byte[] inheritorAddr, Repository repo) {
    AccountCapsule ownerCapsule = repo.getAccount(ownerAddr);
    AccountCapsule inheritorCapsule = repo.getAccount(inheritorAddr);
    long now = repo.getHeadSlot();

    // transfer frozen resource
    ownerCapsule.getFrozenV2List().stream()
        .filter(freezeV2 -> freezeV2.getAmount() > 0)
        .forEach(
            freezeV2 -> {
              switch (freezeV2.getType()) {
                case BANDWIDTH:
                  inheritorCapsule.addFrozenBalanceForBandwidthV2(freezeV2.getAmount());
                  break;
                case ENERGY:
                  inheritorCapsule.addFrozenBalanceForEnergyV2(freezeV2.getAmount());
                  break;
                case TRON_POWER:
                  inheritorCapsule.addFrozenForTronPowerV2(freezeV2.getAmount());
                  break;
              }
            });

    // merge usage
    BandwidthProcessor bandwidthProcessor = new BandwidthProcessor(ChainBaseManager.getInstance());
    bandwidthProcessor.updateUsageForDelegated(ownerCapsule);
    ownerCapsule.setLatestConsumeTime(now);
    if (ownerCapsule.getNetUsage() > 0) {
      long newNetUsage =
          bandwidthProcessor.unDelegateIncrease(
              inheritorCapsule,
              ownerCapsule,
              ownerCapsule.getNetUsage(),
              Common.ResourceCode.BANDWIDTH,
              now);
      inheritorCapsule.setNetUsage(newNetUsage);
      inheritorCapsule.setLatestConsumeTime(now);
    }

    EnergyProcessor energyProcessor =
        new EnergyProcessor(
            repo.getDynamicPropertiesStore(), ChainBaseManager.getInstance().getAccountStore());
    energyProcessor.updateUsage(ownerCapsule);
    ownerCapsule.setLatestConsumeTimeForEnergy(now);
    if (ownerCapsule.getEnergyUsage() > 0) {
      long newEnergyUsage =
          energyProcessor.unDelegateIncrease(
              inheritorCapsule,
              ownerCapsule,
              ownerCapsule.getEnergyUsage(),
              Common.ResourceCode.ENERGY,
              now);
      inheritorCapsule.setEnergyUsage(newEnergyUsage);
      inheritorCapsule.setLatestConsumeTimeForEnergy(now);
    }

    // withdraw expire unfrozen balance
    long nowTimestamp = repo.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp();
    long expireUnfrozenBalance =
        ownerCapsule.getUnfrozenV2List().stream()
            .filter(
                unFreezeV2 ->
                    unFreezeV2.getUnfreezeAmount() > 0 && unFreezeV2.getUnfreezeExpireTime() <= nowTimestamp)
            .mapToLong(Protocol.Account.UnFreezeV2::getUnfreezeAmount)
            .sum();
    if (expireUnfrozenBalance > 0) {
      inheritorCapsule.setBalance(inheritorCapsule.getBalance() + expireUnfrozenBalance);
      increaseNonce();
      addInternalTx(null, ownerAddr, inheritorAddr, expireUnfrozenBalance, null,
          "withdrawExpireUnfreezeWhileSuiciding", nonce, null);
    }
    clearOwnerFreezeV2(ownerCapsule);
    repo.updateAccount(ownerCapsule.createDbKey(), ownerCapsule);
    repo.updateAccount(inheritorCapsule.createDbKey(), inheritorCapsule);
    return expireUnfrozenBalance;
  }

  private void clearOwnerFreezeV2(AccountCapsule ownerCapsule) {
    ownerCapsule.clearFrozenV2();
    ownerCapsule.setNetUsage(0);
    ownerCapsule.setNewWindowSize(Common.ResourceCode.BANDWIDTH, 0);
    ownerCapsule.setEnergyUsage(0);
    ownerCapsule.setNewWindowSize(Common.ResourceCode.ENERGY, 0);
    ownerCapsule.clearUnfrozenV2();
  }

  private void withdrawRewardAndCancelVote(byte[] owner, Repository repo) {
    VoteRewardUtil.withdrawReward(owner, repo);

    AccountCapsule ownerCapsule = repo.getAccount(owner);
    if (!ownerCapsule.getVotesList().isEmpty()) {
      VotesCapsule votesCapsule = repo.getVotes(owner);
      if (votesCapsule == null) {
        votesCapsule = new VotesCapsule(ByteString.copyFrom(owner),
            ownerCapsule.getVotesList());
      } else {
        votesCapsule.clearNewVotes();
      }
      ownerCapsule.clearVotes();
      ownerCapsule.setOldTronPower(0);
      repo.updateVotes(owner, votesCapsule);
    }
    try {
      long balance = ownerCapsule.getBalance();
      long allowance = ownerCapsule.getAllowance();
      ownerCapsule.setInstance(ownerCapsule.getInstance().toBuilder()
          .setBalance(Math.addExact(balance, allowance))
          .setAllowance(0)
          .setLatestWithdrawTime(getTimestamp().longValue() * 1000)
          .build());
      repo.updateAccount(ownerCapsule.createDbKey(), ownerCapsule);
    } catch (ArithmeticException e) {
      throw new BytecodeExecutionException("Suicide: balance and allowance out of long range.");
    }
  }

  public boolean canSuicide() {
    byte[] owner = getContextAddress();
    AccountCapsule accountCapsule = getContractState().getAccount(owner);

    boolean freezeCheck = !VMConfig.allowTvmFreeze()
        || (accountCapsule.getDelegatedFrozenBalanceForBandwidth() == 0
        && accountCapsule.getDelegatedFrozenBalanceForEnergy() == 0);

    boolean freezeV2Check = freezeV2Check(accountCapsule);
    return freezeCheck && freezeV2Check;
//    boolean voteCheck = !VMConfig.allowTvmVote()
//        || (accountCapsule.getVotesList().size() == 0
//        && VoteRewardUtil.queryReward(owner, getContractState()) == 0
//        && getContractState().getAccountVote(
//            getContractState().getBeginCycle(owner), owner) == null);
//    return freezeCheck && voteCheck;
  }

  private boolean freezeV2Check(AccountCapsule accountCapsule) {
    if (!VMConfig.allowTvmFreezeV2()) {
      return true;
    }
    long now = getContractState().getDynamicPropertiesStore().getLatestBlockHeaderTimestamp();

    boolean isDelegatedResourceEmpty =
        accountCapsule.getDelegatedFrozenV2BalanceForBandwidth() == 0
            && accountCapsule.getDelegatedFrozenV2BalanceForEnergy() == 0;
    boolean isUnFrozenV2ListEmpty =
        CollectionUtils.isEmpty(
            accountCapsule.getUnfrozenV2List().stream()
                .filter(unFreezeV2 -> unFreezeV2.getUnfreezeExpireTime() > now)
                .collect(Collectors.toList()));

    return isDelegatedResourceEmpty && isUnFrozenV2ListEmpty;
  }

  @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
  public void createContract(DataWord value, DataWord memStart, DataWord memSize) {
    returnDataBuffer = null; // reset return buffer right before the call

    if (getCallDeep() == MAX_DEPTH) {
      stackPushZero();
      return;
    }
    // [1] FETCH THE CODE FROM THE MEMORY
    byte[] programCode = memoryChunk(memStart.intValue(), memSize.intValue());

    byte[] newAddress = TransactionUtil
        .generateContractAddress(rootTransactionId, nonce);

    createContractImpl(value, programCode, newAddress, false);
  }

  private void createContractImpl(DataWord value, byte[] programCode, byte[] newAddress,
      boolean isCreate2) {
    byte[] senderAddress = getContextAddress();

    if (logger.isDebugEnabled()) {
      logger.debug("creating a new contract inside contract run: [{}]",
          Hex.toHexString(senderAddress));
    }

    long endowment = value.value().longValueExact();
    if (getContractState().getBalance(senderAddress) < endowment) {
      stackPushZero();
      return;
    }

    AccountCapsule existingAccount = getContractState().getAccount(newAddress);
    boolean contractAlreadyExists = existingAccount != null;

    if (VMConfig.allowTvmConstantinople()) {
      contractAlreadyExists =
          contractAlreadyExists && isContractExist(existingAccount, getContractState());
    }
    Repository deposit = getContractState().newRepositoryChild();
    if (VMConfig.allowTvmConstantinople()) {
      if (existingAccount == null) {
        deposit.createAccount(newAddress, "CreatedByContract",
            AccountType.Contract);
      } else if (!contractAlreadyExists) {
        existingAccount.updateAccountType(AccountType.Contract);
        existingAccount.clearDelegatedResource();
        deposit.updateAccount(newAddress, existingAccount);
      }

      if (!contractAlreadyExists) {
        Builder builder = SmartContract.newBuilder();
        if (VMConfig.allowTvmCompatibleEvm()) {
          builder.setVersion(getContractVersion());
        }
        builder.setContractAddress(ByteString.copyFrom(newAddress))
            .setConsumeUserResourcePercent(100)
            .setOriginAddress(ByteString.copyFrom(senderAddress));
        if (isCreate2) {
          builder.setTrxHash(ByteString.copyFrom(rootTransactionId));
        }
        SmartContract newSmartContract = builder.build();
        deposit.createContract(newAddress, new ContractCapsule(newSmartContract));
      }
    } else {
      deposit.createAccount(newAddress, "CreatedByContract",
          Protocol.AccountType.Contract);
      Builder builder = SmartContract.newBuilder();
      if (VMConfig.allowTvmCompatibleEvm()) {
        builder.setVersion(getContractVersion());
      }
      SmartContract newSmartContract = builder.setContractAddress(ByteString.copyFrom(newAddress))
          .setConsumeUserResourcePercent(100)
          .setOriginAddress(ByteString.copyFrom(senderAddress)).build();
      deposit.createContract(newAddress, new ContractCapsule(newSmartContract));
      // In case of hashing collisions, check for any balance before createAccount()
      long oldBalance = deposit.getBalance(newAddress);
      deposit.addBalance(newAddress, oldBalance);
    }

    // [4] TRANSFER THE BALANCE
    long newBalance = 0L;
    if (!byTestingSuite() && endowment > 0) {
      try {
        VMUtils.validateForSmartContract(deposit, senderAddress, newAddress, endowment);
      } catch (ContractValidateException e) {
        // TODO: unreachable exception
        throw new BytecodeExecutionException(VALIDATE_FOR_SMART_CONTRACT_FAILURE, e.getMessage());
      }
      deposit.addBalance(senderAddress, -endowment);
      newBalance = deposit.addBalance(newAddress, endowment);
    }

    // actual energy subtract
    DataWord energyLimit = this.getCreateEnergy(getEnergyLimitLeft());
    spendEnergy(energyLimit.longValue(), "internal call");

    increaseNonce();
    // [5] COOK THE INVOKE AND EXECUTE
    InternalTransaction internalTx = addInternalTx(null, senderAddress, newAddress, endowment,
        programCode, "create", nonce, null);
    long vmStartInUs = System.nanoTime() / 1000;
    ProgramInvoke programInvoke = ProgramInvokeFactory.createProgramInvoke(
        this, new DataWord(newAddress), getContractAddress(), value, DataWord.ZERO(),
        DataWord.ZERO(),
        newBalance, null, deposit, false, byTestingSuite(), vmStartInUs,
        getVmShouldEndInUs(), energyLimit.longValueSafe());
    if (isConstantCall()) {
      programInvoke.setConstantCall();
    }
    ProgramResult createResult = ProgramResult.createEmpty();

    if (contractAlreadyExists) {
      createResult.setException(new BytecodeExecutionException(
          "Trying to create a contract with existing contract address: 0x" + Hex
              .toHexString(newAddress)));
    } else if (isNotEmpty(programCode)) {
      Program program = new Program(programCode, newAddress, programInvoke, internalTx);
      program.setRootTransactionId(this.rootTransactionId);
      if (VMConfig.allowTvmCompatibleEvm()) {
        program.setContractVersion(getContractVersion());
      }
      VM.play(program, OperationRegistry.getTable());
      createResult = program.getResult();
      getTrace().merge(program.getTrace());
      // always commit nonce
      this.nonce = program.nonce;
    }

    // 4. CREATE THE CONTRACT OUT OF RETURN
    byte[] code = createResult.getHReturn();

    if (code.length != 0 && VMConfig.allowTvmLondon() && code[0] == (byte) 0xEF) {
      createResult.setException(Program.Exception
          .invalidCodeException());
    }

    long saveCodeEnergy = (long) getLength(code) * EnergyCost.getCreateData();

    long afterSpend =
        programInvoke.getEnergyLimit() - createResult.getEnergyUsed() - saveCodeEnergy;
    if (!createResult.isRevert()) {
      if (afterSpend < 0) {
        createResult.setException(
            Exception.notEnoughSpendEnergy("No energy to save just created contract code",
                saveCodeEnergy, programInvoke.getEnergyLimit() - createResult.getEnergyUsed()));
      } else {
        createResult.spendEnergy(saveCodeEnergy);
        deposit.saveCode(newAddress, code);
      }
    }

    getResult().merge(createResult);

    if (createResult.getException() != null || createResult.isRevert()) {
      logger.debug("contract run halted by Exception: contract: [{}], exception: [{}]",
          Hex.toHexString(newAddress),
          createResult.getException());

      if (internalTx != null) {
        internalTx.reject();
      }

      createResult.rejectInternalTransactions();

      stackPushZero();

      if (createResult.getException() != null) {
        return;
      } else {
        returnDataBuffer = createResult.getHReturn();
      }
    } else {
      if (!byTestingSuite()) {
        deposit.commit();
      }

      // IN SUCCESS PUSH THE ADDRESS INTO THE STACK
      stackPush(new DataWord(newAddress));
    }

    // 5. REFUND THE REMAIN Energy
    refundEnergyAfterVM(energyLimit, createResult);
  }

  public void refundEnergyAfterVM(DataWord energyLimit, ProgramResult result) {

    long refundEnergy = energyLimit.longValueSafe() - result.getEnergyUsed();
    if (refundEnergy > 0) {
      refundEnergy(refundEnergy, "remain energy from the internal call");
      if (logger.isDebugEnabled()) {
        logger.debug("The remaining energy is refunded, account: [{}], energy: [{}] ",
            Hex.toHexString(getContextAddress()), refundEnergy);
      }
    }
  }

  /**
   * . That method is for internal code invocations
   * <p/>
   * - Normal calls invoke a specified contract which updates itself - Stateless calls invoke code
   * from another contract, within the context of the caller
   *
   * @param msg is the message call object
   */
  public void callToAddress(MessageCall msg) {
    returnDataBuffer = null; // reset return buffer right before the call

    if (getCallDeep() == MAX_DEPTH) {
      stackPushZero();
      refundEnergy(msg.getEnergy().longValue(), " call deep limit reach");
      return;
    }

    byte[] data = memoryChunk(msg.getInDataOffs().intValue(), msg.getInDataSize().intValue());

    // FETCH THE SAVED STORAGE
    byte[] codeAddress = msg.getCodeAddress().toTronAddress();
    byte[] senderAddress = getContextAddress();

    byte[] contextAddress;
    if (msg.getOpCode() == Op.CALLCODE || msg.getOpCode() == Op.DELEGATECALL) {
      contextAddress = senderAddress;
    } else {
      contextAddress = codeAddress;
    }

    if (logger.isDebugEnabled()) {
      logger.debug(Op.getNameOf(msg.getOpCode())
              + " for existing contract: address: [{}], outDataOffs: [{}], outDataSize: [{}]  ",
          Hex.toHexString(contextAddress), msg.getOutDataOffs().longValue(),
          msg.getOutDataSize().longValue());
    }

    Repository deposit = getContractState().newRepositoryChild();

    // 2.1 PERFORM THE VALUE (endowment) PART
    long endowment;
    try {
      endowment = msg.getEndowment().value().longValueExact();
    } catch (ArithmeticException e) {
      if (VMConfig.allowTvmConstantinople()) {
        refundEnergy(msg.getEnergy().longValue(), "endowment out of long range");
        throw new TransferException("endowment out of long range");
      } else {
        throw e;
      }
    }
    // transfer TRX validation
    byte[] tokenId = null;

    checkTokenId(msg);

    boolean isTokenTransfer = isTokenTransfer(msg);

    if (!isTokenTransfer) {
      long senderBalance = deposit.getBalance(senderAddress);
      if (senderBalance < endowment) {
        stackPushZero();
        refundEnergy(msg.getEnergy().longValue(), REFUND_ENERGY_FROM_MESSAGE_CALL);
        return;
      }
    } else {
      // transfer trc10 token validation
      tokenId = String.valueOf(msg.getTokenId().longValue()).getBytes();
      long senderBalance = deposit.getTokenBalance(senderAddress, tokenId);
      if (senderBalance < endowment) {
        stackPushZero();
        refundEnergy(msg.getEnergy().longValue(), REFUND_ENERGY_FROM_MESSAGE_CALL);
        return;
      }
    }

    // FETCH THE CODE
    AccountCapsule accountCapsule = getContractState().getAccount(codeAddress);

    byte[] programCode =
        accountCapsule != null ? getContractState().getCode(codeAddress) : EMPTY_BYTE_ARRAY;

    // only for TRX, not for token
    long contextBalance = 0L;
    if (byTestingSuite()) {
      // This keeps track of the calls created for a test
      getResult().addCallCreate(data, contextAddress,
          msg.getEnergy().getNoLeadZeroesData(),
          msg.getEndowment().getNoLeadZeroesData());
    } else if (!ArrayUtils.isEmpty(senderAddress) && !ArrayUtils.isEmpty(contextAddress)
        && senderAddress != contextAddress && endowment > 0) {
      createAccountIfNotExist(deposit, contextAddress);
      if (!isTokenTransfer) {
        try {
          VMUtils
              .validateForSmartContract(deposit, senderAddress, contextAddress, endowment);
        } catch (ContractValidateException e) {
          if (VMConfig.allowTvmConstantinople()) {
            refundEnergy(msg.getEnergy().longValue(), REFUND_ENERGY_FROM_MESSAGE_CALL);
            throw new TransferException("transfer trx failed: %s", e.getMessage());
          }
          throw new BytecodeExecutionException(VALIDATE_FOR_SMART_CONTRACT_FAILURE, e.getMessage());
        }
        deposit.addBalance(senderAddress, -endowment);
        contextBalance = deposit.addBalance(contextAddress, endowment);
      } else {
        try {
          VMUtils.validateForSmartContract(deposit, senderAddress, contextAddress,
              tokenId, endowment);
        } catch (ContractValidateException e) {
          if (VMConfig.allowTvmConstantinople()) {
            refundEnergy(msg.getEnergy().longValue(), REFUND_ENERGY_FROM_MESSAGE_CALL);
            throw new TransferException("transfer trc10 failed: %s", e.getMessage());
          }
          throw new BytecodeExecutionException(VALIDATE_FOR_SMART_CONTRACT_FAILURE, e.getMessage());
        }
        deposit.addTokenBalance(senderAddress, tokenId, -endowment);
        deposit.addTokenBalance(contextAddress, tokenId, endowment);
      }
    }

    // CREATE CALL INTERNAL TRANSACTION
    increaseNonce();
    HashMap<String, Long> tokenInfo = new HashMap<>();
    if (isTokenTransfer) {
      tokenInfo.put(new String(stripLeadingZeroes(tokenId)), endowment);
    }
    InternalTransaction internalTx = addInternalTx(null, senderAddress, contextAddress,
        !isTokenTransfer ? endowment : 0, data, "call", nonce,
        !isTokenTransfer ? null : tokenInfo);
    ProgramResult callResult = null;
    if (isNotEmpty(programCode)) {
      long vmStartInUs = System.nanoTime() / 1000;
      DataWord callValue;
      if (msg.getOpCode() == Op.DELEGATECALL) {
        callValue = getCallValue();
      } else {
        callValue = msg.getEndowment();
      }
      ProgramInvoke programInvoke = ProgramInvokeFactory.createProgramInvoke(
          this, new DataWord(contextAddress),
          msg.getOpCode() == Op.DELEGATECALL ? getCallerAddress() : getContractAddress(),
          !isTokenTransfer ? callValue : DataWord.ZERO(),
          !isTokenTransfer ? DataWord.ZERO() : callValue,
          !isTokenTransfer ? DataWord.ZERO() : msg.getTokenId(),
          contextBalance, data, deposit,
          msg.getOpCode() == Op.STATICCALL || isStaticCall(),
          byTestingSuite(), vmStartInUs, getVmShouldEndInUs(), msg.getEnergy().longValueSafe());
      if (isConstantCall()) {
        programInvoke.setConstantCall();
      }
      Program program = new Program(programCode, codeAddress, programInvoke, internalTx);
      program.setRootTransactionId(this.rootTransactionId);
      if (VMConfig.allowTvmCompatibleEvm()) {
        program.setContractVersion(invoke.getDeposit()
            .getContract(codeAddress).getContractVersion());
      }
      VM.play(program, OperationRegistry.getTable());
      callResult = program.getResult();

      getTrace().merge(program.getTrace());
      getResult().merge(callResult);
      // always commit nonce
      this.nonce = program.nonce;

      if (callResult.getException() != null || callResult.isRevert()) {
        logger.debug("contract run halted by Exception: contract: [{}], exception: [{}]",
            Hex.toHexString(contextAddress),
            callResult.getException());
        internalTx.reject();

        callResult.rejectInternalTransactions();

        stackPushZero();

        if (callResult.getException() != null) {
          return;
        }
      } else {
        // 4. THE FLAG OF SUCCESS IS ONE PUSHED INTO THE STACK
        deposit.commit();
        stackPushOne();
      }

      if (byTestingSuite()) {
        logger.debug("Testing run, skipping storage diff listener");
      }
    } else {
      // 4. THE FLAG OF SUCCESS IS ONE PUSHED INTO THE STACK
      deposit.commit();
      stackPushOne();
    }

    // 3. APPLY RESULTS: result.getHReturn() into out_memory allocated
    if (callResult != null) {
      byte[] buffer = callResult.getHReturn();
      int offset = msg.getOutDataOffs().intValue();
      int size = msg.getOutDataSize().intValue();

      memorySaveLimited(offset, buffer, size);

      returnDataBuffer = buffer;
    }

    // 5. REFUND THE REMAIN ENERGY
    if (callResult != null) {
      BigInteger refundEnergy = msg.getEnergy().value()
          .subtract(BIUtil.toBI(callResult.getEnergyUsed()));
      if (BIUtil.isPositive(refundEnergy)) {
        refundEnergy(refundEnergy.longValueExact(), "remaining energy from the internal call");
        if (logger.isDebugEnabled()) {
          logger.debug("The remaining energy refunded, account: [{}], energy: [{}] ",
              Hex.toHexString(senderAddress),
              refundEnergy.toString());
        }
      }
    } else {
      refundEnergy(msg.getEnergy().longValue(), "remaining energy from the internal call");
    }
  }

  public void increaseNonce() {
    nonce++;
  }

  public void resetNonce() {
    nonce = 0;
  }

  public void spendEnergy(long energyValue, String opName) {
    if (getEnergylimitLeftLong() < energyValue) {
      throw new OutOfEnergyException(
          "Not enough energy for '%s' operation executing: curInvokeEnergyLimit[%d],"
              + " curOpEnergy[%d], usedEnergy[%d]",
          opName, invoke.getEnergyLimit(), energyValue, getResult().getEnergyUsed());
    }
    getResult().spendEnergy(energyValue);
  }

  public void spendEnergyWithPenalty(long total, long penalty, String opName) {
    if (getEnergylimitLeftLong() < total) {
      throw new OutOfEnergyException(
          "Not enough energy for '%s' operation executing: curInvokeEnergyLimit[%d],"
              + " curOpEnergy[%d], penaltyEnergy[%d], usedEnergy[%d]",
          opName, invoke.getEnergyLimit(), total - penalty, penalty, getResult().getEnergyUsed());
    }
    getResult().spendEnergyWithPenalty(total, penalty);
  }

  public void checkCPUTimeLimit(String opName) {

    if (CommonParameter.getInstance().isDebug()) {
      return;
    }
    if (CommonParameter.getInstance().isSolidityNode()) {
      return;
    }
    long vmNowInUs = System.nanoTime() / 1000;
    if (vmNowInUs > getVmShouldEndInUs()) {
      logger.info(
          "minTimeRatio: {}, maxTimeRatio: {}, vm should end time in us: {}, "
              + "vm now time in us: {}, vm start time in us: {}",
          CommonParameter.getInstance().getMinTimeRatio(),
          CommonParameter.getInstance().getMaxTimeRatio(),
          getVmShouldEndInUs(), vmNowInUs, getVmStartInUs());
      throw Exception.notEnoughTime(opName);
    }

  }

  public void spendAllEnergy() {
    spendEnergy(getEnergyLimitLeft().longValue(), "Spending all remaining");
  }

  public void refundEnergy(long energyValue, String cause) {
    logger
        .debug("[{}] Refund for cause: [{}], energy: [{}]", invoke.hashCode(), cause, energyValue);
    getResult().refundEnergy(energyValue);
  }

//  public void futureRefundEnergy(long energyValue) {
//    logger.debug("Future refund added: [{}]", energyValue);
//    getResult().addFutureRefund(energyValue);
//  }
//
//  public void resetFutureRefund() {
//    getResult().resetFutureRefund();
//  }

  public void storageSave(DataWord word1, DataWord word2) {
    DataWord keyWord = word1.clone();
    DataWord valWord = word2.clone();
    getContractState().putStorageValue(getContextAddress(), keyWord, valWord);
  }

  public byte[] getCode() {
    return ops.clone();
  }

  public byte[] getCodeAt(DataWord address) {
    byte[] code = invoke.getDeposit().getCode(address.toTronAddress());
    return nullToEmpty(code);
  }

  public byte[] getCodeHashAt(DataWord address) {
    byte[] tronAddr = address.toTronAddress();
    AccountCapsule account = getContractState().getAccount(tronAddr);
    if (account != null) {
      ContractCapsule contract = getContractState().getContract(tronAddr);
      byte[] codeHash;
      if (contract != null) {
        codeHash = contract.getCodeHash();
        if (ByteUtil.isNullOrZeroArray(codeHash)) {
          byte[] code = getCodeAt(address);
          codeHash = Hash.sha3(code);
          contract.setCodeHash(codeHash);
          getContractState().updateContract(tronAddr, contract);
        }
      } else {
        codeHash = Hash.sha3(new byte[0]);
      }
      return codeHash;
    } else {
      return EMPTY_BYTE_ARRAY;
    }
  }

  public byte[] getCodeHash() {
    ContractCapsule contract = getContractState().getContract(codeAddress);
    byte[] codeHash;
    if (contract == null) {
      codeHash = Hash.sha3(ops);
    } else {
      codeHash = contract.getCodeHash();
      if (ByteUtil.isNullOrZeroArray(codeHash)) {
        codeHash = Hash.sha3(ops);
      }
    }
    return codeHash;
  }

  private Key getJumpDestAnalysisCacheKey() {
    return Key.create(ByteUtil.merge(codeAddress, getCodeHash()));
  }

  public byte[] getContextAddress() {
    return invoke.getContractAddress().toTronAddress();
  }

  public DataWord getContractAddress() {
    return invoke.getContractAddress().clone();
  }

  public DataWord getBlockHash(int index) {
    if (index < this.getNumber().longValue()
        && index >= Math.max(256, this.getNumber().longValue()) - 256) {

      BlockCapsule blockCapsule = contractState.getBlockByNum(index);

      if (Objects.nonNull(blockCapsule)) {
        return new DataWord(blockCapsule.getBlockId().getBytes()).clone();
      } else {
        return DataWord.ZERO.clone();
      }
    } else {
      return DataWord.ZERO.clone();
    }

  }

  public DataWord getBalance(DataWord address) {
    long balance = getContractState().getBalance(address.toTronAddress());
    return new DataWord(balance);
  }

  public DataWord getRewardBalance(DataWord address) {
    long rewardBalance = VoteRewardUtil.queryReward(address.toTronAddress(), getContractState());
    return new DataWord(rewardBalance);
  }

  public DataWord isContract(DataWord address) {
    ContractCapsule contract = getContractState().getContract(address.toTronAddress());
    return contract != null ? DataWord.ONE() : DataWord.ZERO();
  }

  public DataWord isSRCandidate(DataWord address) {
    WitnessCapsule witnessCapsule = getContractState().getWitness(address.toTronAddress());
    return witnessCapsule != null ? DataWord.ONE() : DataWord.ZERO();
  }

  public DataWord getOriginAddress() {
    return invoke.getOriginAddress().clone();
  }

  public DataWord getCallerAddress() {
    return invoke.getCallerAddress().clone();
  }

  public DataWord getChainId() {
    byte[] chainId = getContractState().getBlockByNum(0).getBlockId().getBytes();
    if (VMConfig.allowTvmCompatibleEvm() || VMConfig.allowOptimizedReturnValueOfChainId()) {
      chainId = Arrays.copyOfRange(chainId, chainId.length - 4, chainId.length);
    }
    return new DataWord(chainId).clone();
  }

  public long getEnergylimitLeftLong() {
    return invoke.getEnergyLimit() - getResult().getEnergyUsed();
  }

  public DataWord getEnergyLimitLeft() {
    return new DataWord(invoke.getEnergyLimit() - getResult().getEnergyUsed());
  }

  public long getVmShouldEndInUs() {
    return invoke.getVmShouldEndInUs();
  }

  public DataWord getCallValue() {
    return invoke.getCallValue().clone();
  }

  public DataWord getDataSize() {
    return invoke.getDataSize().clone();
  }

  public DataWord getDataValue(DataWord index) {
    return invoke.getDataValue(index);
  }

  public byte[] getDataCopy(DataWord offset, DataWord length) {
    return invoke.getDataCopy(offset, length);
  }

  public DataWord getReturnDataBufferSize() {
    return new DataWord(getReturnDataBufferSizeI());
  }

  private int getReturnDataBufferSizeI() {
    return returnDataBuffer == null ? 0 : returnDataBuffer.length;
  }

  public byte[] getReturnDataBufferData(DataWord off, DataWord size) {
    if ((long) off.intValueSafe() + size.intValueSafe() > getReturnDataBufferSizeI()) {
      return null;
    }
    return returnDataBuffer == null ? new byte[0] :
        Arrays.copyOfRange(returnDataBuffer, off.intValueSafe(),
            off.intValueSafe() + size.intValueSafe());
  }

  public DataWord storageLoad(DataWord key) {
    DataWord ret = getContractState().getStorageValue(getContextAddress(), key.clone());
    return ret == null ? null : ret.clone();
  }

  public DataWord getTokenBalance(DataWord address, DataWord tokenId) {
    checkTokenIdInTokenBalance(tokenId);
    long ret = getContractState().getTokenBalance(address.toTronAddress(),
        String.valueOf(tokenId.longValue()).getBytes());
    return new DataWord(ret);
  }

  public DataWord getTokenValue() {
    return invoke.getTokenValue().clone();
  }

  public DataWord getTokenId() {
    return invoke.getTokenId().clone();
  }

  public DataWord getPrevHash() {
    return invoke.getPrevHash().clone();
  }

  public DataWord getCoinbase() {
    return invoke.getCoinbase().clone();
  }

  public DataWord getTimestamp() {
    return invoke.getTimestamp().clone();
  }

  public DataWord getNumber() {
    return invoke.getNumber().clone();
  }

  public DataWord getDifficulty() {
    return invoke.getDifficulty().clone();
  }

  public boolean isStaticCall() {
    return invoke.isStaticCall();
  }

  public boolean isConstantCall() {
    return invoke.isConstantCall();
  }

  public ProgramResult getResult() {
    return result;
  }

  public void setRuntimeFailure(RuntimeException e) {
    getResult().setException(e);
  }

  public String memoryToString() {
    return memory.toString();
  }

  public void fullTrace() {
    if (logger.isTraceEnabled() || listener != null) {

      StringBuilder stackData = new StringBuilder();
      for (int i = 0; i < stack.size(); ++i) {
        stackData.append(" ").append(stack.get(i));
        if (i < stack.size() - 1) {
          stackData.append("\n");
        }
      }

      if (stackData.length() > 0) {
        stackData.insert(0, "\n");
      }

      StringBuilder memoryData = new StringBuilder();
      StringBuilder oneLine = new StringBuilder();
      if (memory.size() > 320) {
        memoryData.append("... Memory Folded.... ")
            .append("(")
            .append(memory.size())
            .append(") bytes");
      } else {
        for (int i = 0; i < memory.size(); ++i) {

          byte value = memory.readByte(i);
          oneLine.append(ByteUtil.oneByteToHexString(value)).append(" ");

          if ((i + 1) % 16 == 0) {
            String tmp = format("[%4s]-[%4s]", Integer.toString(i - 15, 16),
                Integer.toString(i, 16)).replace(" ", "0");
            memoryData.append("").append(tmp).append(" ");
            memoryData.append(oneLine);
            if (i < memory.size()) {
              memoryData.append("\n");
            }
            oneLine.setLength(0);
          }
        }
      }
      if (memoryData.length() > 0) {
        memoryData.insert(0, "\n");
      }

      StringBuilder opsString = new StringBuilder();
      for (int i = 0; i < ops.length; ++i) {

        String tmpString = Integer.toString(ops[i] & 0xFF, 16);
        tmpString = tmpString.length() == 1 ? "0" + tmpString : tmpString;

        if (i != pc) {
          opsString.append(tmpString);
        } else {
          opsString.append(" >>").append(tmpString).append("");
        }

      }
      if (pc >= ops.length) {
        opsString.append(" >>");
      }
      if (opsString.length() > 0) {
        opsString.insert(0, "\n ");
      }

      logger.trace(" -- OPS --     {}", opsString);
      logger.trace(" -- STACK --   {}", stackData);
      logger.trace(" -- MEMORY --  {}", memoryData);
      logger.trace("\n  Spent Drop: [{}]/[{}]\n  Left Energy:  [{}]\n",
          getResult().getEnergyUsed(),
          invoke.getEnergyLimit(),
          getEnergyLimitLeft().longValue());

      StringBuilder globalOutput = new StringBuilder("\n");
      if (stackData.length() > 0) {
        stackData.append("\n");
      }

      if (pc != 0) {
        globalOutput.append("[Op: ").append(Op.getNameOf(lastOp)).append("]\n");
      }

      globalOutput.append(" -- OPS --     ").append(opsString).append("\n");
      globalOutput.append(" -- STACK --   ").append(stackData).append("\n");
      globalOutput.append(" -- MEMORY --  ").append(memoryData).append("\n");

      if (getResult().getHReturn() != null) {
        globalOutput.append("\n  HReturn: ").append(
            Hex.toHexString(getResult().getHReturn()));
      }

      // sophisticated assumption that msg.data != codedata
      // means we are calling the contract not creating it
      byte[] txData = invoke.getDataCopy(DataWord.ZERO, getDataSize());
      if (!Arrays.equals(txData, ops)) {
        globalOutput.append("\n  msg.data: ").append(Hex.toHexString(txData));
      }
      globalOutput.append("\n\n  Spent Energy: ").append(getResult().getEnergyUsed());

      if (listener != null) {
        listener.output(globalOutput.toString());
      }
    }
  }

  public void saveOpTrace() {
    if (this.pc < ops.length) {
      trace.addOp(ops[pc], pc, getCallDeep(), getEnergyLimitLeft(), traceListener.resetActions());
    }
  }

  public ProgramTrace getTrace() {
    return trace;
  }

  public void createContract2(DataWord value, DataWord memStart, DataWord memSize, DataWord salt) {
    byte[] senderAddress;
    if (VMConfig.allowTvmCompatibleEvm() && getCallDeep() == MAX_DEPTH) {
      stackPushZero();
      return;
    }
    if (VMConfig.allowTvmIstanbul()) {
      senderAddress = getContextAddress();
    } else {
      senderAddress = getCallerAddress().toTronAddress();
    }
    byte[] programCode = memoryChunk(memStart.intValue(), memSize.intValue());

    byte[] contractAddress = WalletUtil
        .generateContractAddress2(senderAddress, salt.getData(), programCode);
    createContractImpl(value, programCode, contractAddress, true);
  }

  public void addListener(ProgramOutListener listener) {
    this.listener = listener;
  }

  public int verifyJumpDest(DataWord nextPC) {
    if (nextPC.bytesOccupied() > 4) {
      throw Exception.badJumpDestination(-1);
    }
    int ret = nextPC.intValue();
    if (!getProgramPrecompile().hasJumpDest(ret)) {
      throw Exception.badJumpDestination(ret);
    }
    return ret;
  }

  public void callToPrecompiledAddress(MessageCall msg,
      PrecompiledContracts.PrecompiledContract contract) {
    returnDataBuffer = null; // reset return buffer right before the call

    if (getCallDeep() == MAX_DEPTH) {
      stackPushZero();
      this.refundEnergy(msg.getEnergy().longValue(), " call deep limit reach");
      return;
    }

    Repository deposit = getContractState().newRepositoryChild();

    byte[] senderAddress = getContextAddress();
    byte[] contextAddress;
    if (msg.getOpCode() == Op.CALLCODE || msg.getOpCode() == Op.DELEGATECALL) {
      contextAddress = senderAddress;
    } else {
      contextAddress = msg.getCodeAddress().toTronAddress();
    }

    long endowment = msg.getEndowment().value().longValueExact();
    long senderBalance = 0;
    byte[] tokenId = null;

    checkTokenId(msg);
    boolean isTokenTransfer = isTokenTransfer(msg);
    // transfer TRX validation
    if (!isTokenTransfer) {
      senderBalance = deposit.getBalance(senderAddress);
    } else {
      // transfer trc10 token validation
      tokenId = String.valueOf(msg.getTokenId().longValue()).getBytes();
      senderBalance = deposit.getTokenBalance(senderAddress, tokenId);
    }
    if (senderBalance < endowment) {
      stackPushZero();
      refundEnergy(msg.getEnergy().longValue(), REFUND_ENERGY_FROM_MESSAGE_CALL);
      return;
    }
    byte[] data = this.memoryChunk(msg.getInDataOffs().intValue(),
        msg.getInDataSize().intValue());

    // Charge for endowment - is not reversible by rollback
    if (!ArrayUtils.isEmpty(senderAddress) && !ArrayUtils.isEmpty(contextAddress)
        && senderAddress != contextAddress && msg.getEndowment().value().longValueExact() > 0) {
      if (!isTokenTransfer) {
        try {
          MUtil.transfer(deposit, senderAddress, contextAddress,
              msg.getEndowment().value().longValueExact());
        } catch (ContractValidateException e) {
          throw new BytecodeExecutionException("transfer failure");
        }
      } else {
        try {
          VMUtils
              .validateForSmartContract(deposit, senderAddress, contextAddress, tokenId, endowment);
        } catch (ContractValidateException e) {
          throw new BytecodeExecutionException(VALIDATE_FOR_SMART_CONTRACT_FAILURE, e.getMessage());
        }
        deposit.addTokenBalance(senderAddress, tokenId, -endowment);
        deposit.addTokenBalance(contextAddress, tokenId, endowment);
      }
    }

    long requiredEnergy = contract.getEnergyForData(data);
    if (requiredEnergy > msg.getEnergy().longValue()) {
      // Not need to throw an exception, method caller needn't know that
      // regard as consumed the energy
      this.refundEnergy(0, CALL_PRE_COMPILED); //matches cpp logic
      this.stackPushZero();
    } else {
      // Delegate or not. if is delegated, we will use msg sender, otherwise use contract address
      if (msg.getOpCode() == Op.DELEGATECALL) {
        contract.setCallerAddress(getCallerAddress().toTronAddress());
      } else {
        contract.setCallerAddress(getContextAddress());
      }
      // this is the depositImpl, not contractState as above
      contract.setRepository(deposit);
      contract.setResult(this.result);
      contract.setConstantCall(isConstantCall());
      contract.setVmShouldEndInUs(getVmShouldEndInUs());
      Pair<Boolean, byte[]> out = contract.execute(data);

      if (out.getLeft()) { // success
        this.refundEnergy(msg.getEnergy().longValue() - requiredEnergy, CALL_PRE_COMPILED);
        this.stackPushOne();
        returnDataBuffer = out.getRight();
        deposit.commit();
      } else {
        // spend all energy on failure, push zero and revert state changes
        this.refundEnergy(0, CALL_PRE_COMPILED);
        this.stackPushZero();
        if (Objects.nonNull(this.result.getException())) {
          throw result.getException();
        }
      }

      this.memorySave(msg.getOutDataOffs().intValue(), out.getRight());
    }
  }

  public boolean byTestingSuite() {
    return invoke.byTestingSuite();
  }

  /**
   * check TokenId TokenId  \ isTransferToken -------------------------------------------------------------------
   * false                                     true -----------------------------------------------
   * (-∞,Long.Min)        Not possible            error: msg.getTokenId().value().longValueExact()
   * ---------------------------------------------------------------------------------------------
   * [Long.Min, 0)        Not possible                               error
   * --------------------------------------------------------------------------------------------- 0
   * allowed and only allowed                    error (guaranteed in CALLTOKEN) transfertoken id=0
   * should not transfer TRX） ---------------------------------------------------------------------
   * (0-100_0000]          Not possible                              error
   * ---------------------------------------------------------------------------------------------
   * (100_0000, Long.Max]  Not possible                             allowed
   * ---------------------------------------------------------------------------------------------
   * (Long.Max,+∞)         Not possible          error: msg.getTokenId().value().longValueExact()
   * ---------------------------------------------------------------------------------------------
   */
  public void checkTokenId(MessageCall msg) {
    if (VMConfig.allowMultiSign()) { //allowMultiSign proposal
      // tokenid should not get Long type overflow
      long tokenId;
      try {
        tokenId = msg.getTokenId().sValue().longValueExact();
      } catch (ArithmeticException e) {
        if (VMConfig.allowTvmConstantinople()) {
          refundEnergy(msg.getEnergy().longValue(), REFUND_ENERGY_FROM_MESSAGE_CALL);
          throw new TransferException(VALIDATE_FOR_SMART_CONTRACT_FAILURE, INVALID_TOKEN_ID_MSG);
        }
        throw e;
      }
      // tokenId can only be 0 when isTokenTransferMsg is false
      // or tokenId can be (MIN_TOKEN_ID, Long.Max] when isTokenTransferMsg == true
      if ((tokenId <= VMConstant.MIN_TOKEN_ID && tokenId != 0)
          || (tokenId == 0 && msg.isTokenTransferMsg())) {
        // tokenId == 0 is a default value for token id DataWord.
        if (VMConfig.allowTvmConstantinople()) {
          refundEnergy(msg.getEnergy().longValue(), REFUND_ENERGY_FROM_MESSAGE_CALL);
          throw new TransferException(VALIDATE_FOR_SMART_CONTRACT_FAILURE, INVALID_TOKEN_ID_MSG);
        }
        throw new BytecodeExecutionException(
            String.format(VALIDATE_FOR_SMART_CONTRACT_FAILURE, INVALID_TOKEN_ID_MSG));
      }
    }
  }

  public boolean isTokenTransfer(MessageCall msg) {
    if (VMConfig.allowMultiSign()) { //allowMultiSign proposal
      return msg.isTokenTransferMsg();
    } else {
      return msg.getTokenId().longValue() != 0;
    }
  }

  public void checkTokenIdInTokenBalance(DataWord tokenIdDataWord) {
    if (VMConfig.allowMultiSign()) { //allowMultiSigns proposal
      // tokenid should not get Long type overflow
      long tokenId;
      try {
        tokenId = tokenIdDataWord.sValue().longValueExact();
      } catch (ArithmeticException e) {
        if (VMConfig.allowTvmConstantinople()) {
          throw new TransferException(VALIDATE_FOR_SMART_CONTRACT_FAILURE, INVALID_TOKEN_ID_MSG);
        }
        throw e;
      }

      // or tokenId can only be (MIN_TOKEN_ID, Long.Max]
      if (tokenId <= VMConstant.MIN_TOKEN_ID) {
        throw new BytecodeExecutionException(
            String.format(VALIDATE_FOR_SMART_CONTRACT_FAILURE, INVALID_TOKEN_ID_MSG));
      }
    }
  }

  public DataWord getCallEnergy(DataWord requestedEnergy, DataWord availableEnergy) {
    if (VMConfig.allowTvmCompatibleEvm() && getContractVersion() == 1) {
      DataWord availableEnergyReduce = availableEnergy.clone();
      availableEnergyReduce.div(new DataWord(64));
      availableEnergy.sub(availableEnergyReduce);
    }
    return requestedEnergy.compareTo(availableEnergy) > 0 ? availableEnergy : requestedEnergy;
  }

  public DataWord getCreateEnergy(DataWord availableEnergy) {
    if (VMConfig.allowTvmCompatibleEvm() && getContractVersion() == 1) {
      DataWord availableEnergyReduce = availableEnergy.clone();
      availableEnergyReduce.div(new DataWord(64));
      availableEnergy.sub(availableEnergyReduce);
    }
    return availableEnergy;
  }

  /**
   * . used mostly for testing reasons
   */
  public byte[] getMemory() {
    return memory.read(0, memory.size());
  }

  /**
   * . used mostly for testing reasons
   */
  public void initMem(byte[] data) {
    this.memory.write(0, data, data.length, false);
  }

  public long getVmStartInUs() {
    return this.invoke.getVmStartInUs();
  }

  private boolean isContractExist(AccountCapsule existingAddr, Repository deposit) {
    return deposit.getContract(existingAddr.getAddress().toByteArray()) != null;
  }

  private void createAccountIfNotExist(Repository deposit, byte[] contextAddress) {
    if (VMConfig.allowTvmSolidity059()) {
      //after solidity059 proposal , allow contract transfer trc10 or TRX to non-exist address(would create one)
      AccountCapsule sender = deposit.getAccount(contextAddress);
      if (sender == null) {
        deposit.createNormalAccount(contextAddress);
      }
    }
  }

  public interface ProgramOutListener {

    void output(String out);
  }

  static class ByteCodeIterator {

    private byte[] code;
    private int pc;

    public ByteCodeIterator(byte[] code) {
      this.code = code;
    }

    public int getPC() {
      return pc;
    }

    public void setPC(int pc) {
      this.pc = pc;
    }

  }

  public boolean freeze(DataWord receiverAddress, DataWord frozenBalance, DataWord resourceType) {
    Repository repository = getContractState().newRepositoryChild();
    byte[] owner = getContextAddress();
    byte[] receiver = receiverAddress.toTronAddress();

    increaseNonce();
    InternalTransaction internalTx = addInternalTx(null, owner, receiver,
        frozenBalance.longValue(), null,
        "freezeFor" + convertResourceToString(resourceType), nonce, null);

    FreezeBalanceParam param = new FreezeBalanceParam();
    param.setOwnerAddress(owner);
    param.setReceiverAddress(receiver);
    boolean needCheckFrozenTime = CommonParameter.getInstance()
        .getCheckFrozenTime() == 1; // for test
    param.setFrozenDuration(needCheckFrozenTime
        ? repository.getDynamicPropertiesStore().getMinFrozenTime() : 0);
    param.setResourceType(parseResourceCode(resourceType));
    try {
      FreezeBalanceProcessor processor = new FreezeBalanceProcessor();
      param.setFrozenBalance(frozenBalance.sValue().longValueExact());
      processor.validate(param, repository);
      processor.execute(param, repository);
      repository.commit();
      return true;
    } catch (ContractValidateException e) {
      logger.error("TVM Freeze: validate failure. Reason: {}", e.getMessage());
    } catch (ArithmeticException e) {
      logger.error("TVM Freeze: frozenBalance out of long range.");
    }
    if (internalTx != null) {
      internalTx.reject();
    }
    return false;
  }

  public boolean unfreeze(DataWord receiverAddress, DataWord resourceType) {
    Repository repository = getContractState().newRepositoryChild();
    byte[] owner = getContextAddress();
    byte[] receiver = receiverAddress.toTronAddress();

    increaseNonce();
    InternalTransaction internalTx = addInternalTx(null, owner, receiver, 0, null,
        "unfreezeFor" + convertResourceToString(resourceType), nonce, null);

    UnfreezeBalanceParam param = new UnfreezeBalanceParam();
    param.setOwnerAddress(owner);
    param.setReceiverAddress(receiver);
    param.setResourceType(parseResourceCode(resourceType));
    try {
      UnfreezeBalanceProcessor processor = new UnfreezeBalanceProcessor();
      processor.validate(param, repository);
      long unfreezeBalance = processor.execute(param, repository);
      repository.commit();
      if (internalTx != null) {
        internalTx.setValue(unfreezeBalance);
      }
      return true;
    } catch (ContractValidateException e) {
      logger.error("TVM Unfreeze: validate failure. Reason: {}", e.getMessage());
    }
    if (internalTx != null) {
      internalTx.reject();
    }
    return false;
  }

  public long freezeExpireTime(DataWord targetAddress, DataWord resourceType) {
    byte[] owner = getContextAddress();
    byte[] target = targetAddress.toTronAddress();
    int resourceCode = resourceType.intValue();
    if (FastByteComparisons.isEqual(owner, target)) {
      AccountCapsule ownerCapsule = getContractState().getAccount(owner);
      if (resourceCode == 0) { //  for bandwidth
        if (ownerCapsule.getFrozenCount() != 0
            && ownerCapsule.getFrozenList().get(0).getFrozenBalance() != 0) {
          return ownerCapsule.getFrozenList().get(0).getExpireTime();
        }
      } else if (resourceCode == 1) { // for energy
        if (ownerCapsule.getAccountResource().getFrozenBalanceForEnergy().getFrozenBalance() != 0) {
          return ownerCapsule.getAccountResource().getFrozenBalanceForEnergy().getExpireTime();
        }
      }
    } else {
      byte[] key = DelegatedResourceCapsule.createDbKey(owner, target);
      DelegatedResourceCapsule delegatedResourceCapsule = getContractState().getDelegatedResource(key);
      if (delegatedResourceCapsule != null) {
        if (resourceCode == 0) {
          if (delegatedResourceCapsule.getFrozenBalanceForBandwidth() != 0) {
            return delegatedResourceCapsule.getExpireTimeForBandwidth();
          }
        } else if (resourceCode == 1) {
          if (delegatedResourceCapsule.getFrozenBalanceForEnergy() != 0) {
            return delegatedResourceCapsule.getExpireTimeForEnergy();
          }
        }
      }
    }
    return 0;
  }

  public boolean freezeBalanceV2(DataWord frozenBalance, DataWord resourceType) {
    Repository repository = getContractState().newRepositoryChild();
    byte[] owner = getContextAddress();

    increaseNonce();
    InternalTransaction internalTx = addInternalTx(null, owner, owner,
        frozenBalance.longValue(), null,
        "freezeBalanceV2For" + convertResourceToString(resourceType), nonce, null);

    try {
      FreezeBalanceV2Param param = new FreezeBalanceV2Param();
      param.setOwnerAddress(owner);
      param.setResourceType(parseResourceCodeV2(resourceType));
      param.setFrozenBalance(frozenBalance.sValue().longValueExact());

      FreezeBalanceV2Processor processor = new FreezeBalanceV2Processor();
      processor.validate(param, repository);
      processor.execute(param, repository);
      repository.commit();
      return true;
    } catch (ContractValidateException e) {
      logger.error("TVM FreezeBalanceV2: validate failure. Reason: {}", e.getMessage());
    } catch (ArithmeticException e) {
      logger.error("TVM FreezeBalanceV2: frozenBalance out of long range.");
    }
    if (internalTx != null) {
      internalTx.reject();
    }
    return false;
  }

  public boolean unfreezeBalanceV2(DataWord unfreezeBalance, DataWord resourceType) {
    Repository repository = getContractState().newRepositoryChild();
    byte[] owner = getContextAddress();

    increaseNonce();
    InternalTransaction internalTx = addInternalTx(null, owner, owner,
        unfreezeBalance.longValue(), null,
        "unfreezeBalanceV2For" + convertResourceToString(resourceType), nonce, null);

    try {
      UnfreezeBalanceV2Param param = new UnfreezeBalanceV2Param();
      param.setOwnerAddress(owner);
      param.setUnfreezeBalance(unfreezeBalance.sValue().longValueExact());
      param.setResourceType(parseResourceCodeV2(resourceType));

      UnfreezeBalanceV2Processor processor = new UnfreezeBalanceV2Processor();
      processor.validate(param, repository);
      long unfreezeExpireBalance = processor.execute(param, repository);
      repository.commit();
      if (unfreezeExpireBalance > 0) {
        increaseNonce();
        addInternalTx(null, owner, owner, unfreezeExpireBalance, null,
            "withdrawExpireUnfreezeWhileUnfreezing", nonce, null);
      }
      return true;
    } catch (ContractValidateException e) {
      logger.error("TVM UnfreezeBalanceV2: validate failure. Reason: {}", e.getMessage());
    } catch (ArithmeticException e) {
      logger.error("TVM UnfreezeBalanceV2: balance out of long range.");
    }
    if (internalTx != null) {
      internalTx.reject();
    }
    return false;
  }

  public long withdrawExpireUnfreeze() {
    Repository repository = getContractState().newRepositoryChild();
    byte[] owner = getContextAddress();

    increaseNonce();
    InternalTransaction internalTx = addInternalTx(null, owner, owner, 0, null,
        "withdrawExpireUnfreeze", nonce, null);

    try {
      WithdrawExpireUnfreezeParam param = new WithdrawExpireUnfreezeParam();
      param.setOwnerAddress(owner);

      WithdrawExpireUnfreezeProcessor processor = new WithdrawExpireUnfreezeProcessor();
      processor.validate(param, repository);
      long expireUnfreezeBalance = processor.execute(param, repository);
      repository.commit();
      if (internalTx != null) {
        internalTx.setValue(expireUnfreezeBalance);
      }
      return expireUnfreezeBalance;
    } catch (ContractValidateException e) {
      logger.error("TVM WithdrawExpireUnfreeze: validate failure. Reason: {}", e.getMessage());
    } catch (ContractExeException e) {
      logger.error("TVM WithdrawExpireUnfreeze: execute failure. Reason: {}", e.getMessage());
    }
    if (internalTx != null) {
      internalTx.reject();
    }
    return 0;
  }

  public boolean cancelAllUnfreezeV2Action() {
    Repository repository = getContractState().newRepositoryChild();
    byte[] owner = getContextAddress();

    increaseNonce();
    InternalTransaction internalTx = addInternalTx(null, owner, owner, 0, null,
        "cancelAllUnfreezeV2", nonce, null);

    try {
      CancelAllUnfreezeV2Param param = new CancelAllUnfreezeV2Param();
      param.setOwnerAddress(owner);

      CancelAllUnfreezeV2Processor processor = new CancelAllUnfreezeV2Processor();
      processor.validate(param, repository);
      long withdrawExpireBalance = processor.execute(param, repository);
      repository.commit();
      if (withdrawExpireBalance > 0) {
        increaseNonce();
        addInternalTx(null, owner, owner, withdrawExpireBalance, null,
            "withdrawExpireUnfreezeWhileCanceling", nonce, null);
      }
      return true;
    } catch (ContractValidateException e) {
      logger.error("TVM CancelAllUnfreezeV2: validate failure. Reason: {}", e.getMessage());
    } catch (ContractExeException e) {
      logger.error("TVM CancelAllUnfreezeV2: execute failure. Reason: {}", e.getMessage());
    }
    if (internalTx != null) {
      internalTx.reject();
    }
    return false;
  }

  public boolean delegateResource(
      DataWord receiverAddress, DataWord delegateBalance, DataWord resourceType) {
    Repository repository = getContractState().newRepositoryChild();
    byte[] owner = getContextAddress();
    byte[] receiver = receiverAddress.toTronAddress();

    increaseNonce();
    InternalTransaction internalTx = addInternalTx(null, owner, receiver,
        delegateBalance.longValue(), null,
        "delegateResourceOf" + convertResourceToString(resourceType), nonce, null);

    try {
      DelegateResourceParam param = new DelegateResourceParam();
      param.setOwnerAddress(owner);
      param.setReceiverAddress(receiver);
      param.setDelegateBalance(delegateBalance.sValue().longValueExact());
      param.setResourceType(parseResourceCodeV2(resourceType));

      DelegateResourceProcessor processor = new DelegateResourceProcessor();
      processor.validate(param, repository);
      processor.execute(param, repository);
      repository.commit();
      return true;
    } catch (ContractValidateException e) {
      logger.error("TVM DelegateResource: validate failure. Reason: {}", e.getMessage());
    } catch (ArithmeticException e) {
      logger.error("TVM DelegateResource: balance out of long range.");
    }
    if (internalTx != null) {
      internalTx.reject();
    }
    return false;
  }

  public boolean unDelegateResource(
      DataWord receiverAddress, DataWord unDelegateBalance, DataWord resourceType) {
    Repository repository = getContractState().newRepositoryChild();
    byte[] owner = getContextAddress();
    byte[] receiver = receiverAddress.toTronAddress();

    increaseNonce();
    InternalTransaction internalTx = addInternalTx(null, owner, receiver,
        unDelegateBalance.longValue(), null,
        "unDelegateResourceOf" + convertResourceToString(resourceType), nonce, null);

    try {
      UnDelegateResourceParam param = new UnDelegateResourceParam();
      param.setOwnerAddress(owner);
      param.setReceiverAddress(receiver);
      param.setUnDelegateBalance(unDelegateBalance.sValue().longValueExact());
      param.setResourceType(parseResourceCodeV2(resourceType));

      UnDelegateResourceProcessor processor = new UnDelegateResourceProcessor();
      processor.validate(param, repository);
      processor.execute(param, repository);
      repository.commit();
      return true;
    } catch (ContractValidateException e) {
      logger.error("TVM UnDelegateResource: validate failure. Reason: {}", e.getMessage());
    } catch (ArithmeticException e) {
      logger.error("TVM UnDelegateResource: balance out of long range.");
    }
    if (internalTx != null) {
      internalTx.reject();
    }
    return false;
  }

  private Common.ResourceCode parseResourceCode(DataWord resourceType) {
    switch (resourceType.intValue()) {
      case 0:
        return Common.ResourceCode.BANDWIDTH;
      case 1:
        return Common.ResourceCode.ENERGY;
      default:
        return Common.ResourceCode.UNRECOGNIZED;
    }
  }

  private Common.ResourceCode parseResourceCodeV2(DataWord resourceType) {
    try {
      byte type = resourceType.sValue().byteValueExact();
      switch (type) {
        case 0:
          return Common.ResourceCode.BANDWIDTH;
        case 1:
          return Common.ResourceCode.ENERGY;
        case 2:
          return Common.ResourceCode.TRON_POWER;
        default:
          return Common.ResourceCode.UNRECOGNIZED;
      }
    } catch (ArithmeticException e) {
      logger.error("TVM ParseResourceCodeV2: invalid resource code: {}", resourceType.sValue());
      return Common.ResourceCode.UNRECOGNIZED;
    }
  }

  private String convertResourceToString(DataWord resourceType) {
    switch (resourceType.intValue()) {
      case 0:
        return "Bandwidth";
      case 1:
        return "Energy";
      case 2:
        return "TronPower";
      default:
        return "UnknownType";
    }
  }

  public boolean voteWitness(int witnessArrayOffset, int witnessArrayLength,
      int amountArrayOffset, int amountArrayLength) {
    Repository repository = getContractState().newRepositoryChild();
    byte[] owner = getContextAddress();

    increaseNonce();
    InternalTransaction internalTx = addInternalTx(null, owner, null, 0, null,
        "voteWitness", nonce, null);

    if (memoryLoad(witnessArrayOffset).intValueSafe() != witnessArrayLength ||
        memoryLoad(amountArrayOffset).intValueSafe() != amountArrayLength) {
      logger.warn("TVM VoteWitness: memory array length do not match length parameter!");
      throw new BytecodeExecutionException(
          "TVM VoteWitness: memory array length do not match length parameter!");
    }

    if (witnessArrayLength != amountArrayLength) {
      logger.warn("TVM VoteWitness: witness array length {} does not match amount array length {}",
          witnessArrayLength, amountArrayLength);
      return false;
    }

    try {
      VoteWitnessParam param = new VoteWitnessParam();
      param.setVoterAddress(owner);

      byte[] witnessArrayData = memoryChunk(Math.addExact(witnessArrayOffset, DataWord.WORD_SIZE),
          Math.multiplyExact(witnessArrayLength, DataWord.WORD_SIZE));
      byte[] amountArrayData = memoryChunk(Math.addExact(amountArrayOffset, DataWord.WORD_SIZE),
          Math.multiplyExact(amountArrayLength, DataWord.WORD_SIZE));

      for (int i = 0; i < witnessArrayLength; i++) {
        DataWord witness = new DataWord(Arrays.copyOfRange(witnessArrayData,
            i * DataWord.WORD_SIZE, (i + 1) * DataWord.WORD_SIZE));
        DataWord amount = new DataWord(Arrays.copyOfRange(amountArrayData,
            i * DataWord.WORD_SIZE, (i + 1) * DataWord.WORD_SIZE));
        param.addVote(witness.toTronAddress(), amount.sValue().longValueExact());
      }
      if (internalTx != null) {
        internalTx.setExtra(param.toJsonStr());
      }

      VoteWitnessProcessor processor = new VoteWitnessProcessor();
      processor.validate(param, repository);
      processor.execute(param, repository);
      repository.commit();
      return true;
    } catch (ContractValidateException e) {
      logger.error("TVM VoteWitness: validate failure. Reason: {}", e.getMessage());
    } catch (ContractExeException e) {
      logger.error("TVM VoteWitness: execute failure. Reason: {}", e.getMessage());
    } catch (ArithmeticException e) {
      logger.error("TVM VoteWitness: int or long out of range. caused by: {}", e.getMessage());
    }
    if (internalTx != null) {
      internalTx.reject();
    }
    return false;
  }

  public long withdrawReward() {
    Repository repository = getContractState().newRepositoryChild();
    byte[] owner = getContextAddress();

    increaseNonce();
    InternalTransaction internalTx = addInternalTx(null, owner, owner, 0, null,
        "withdrawReward", nonce, null);

    WithdrawRewardParam param = new WithdrawRewardParam();
    param.setOwnerAddress(owner);
    param.setNowInMs(getTimestamp().longValue() * 1000);
    try {
      WithdrawRewardProcessor processor = new WithdrawRewardProcessor();
      processor.validate(param, repository);
      long allowance = processor.execute(param, repository);
      repository.commit();
      if (internalTx != null) {
        internalTx.setValue(allowance);
      }
      return allowance;
    } catch (ContractValidateException e) {
      logger.error("TVM WithdrawReward: validate failure. Reason: {}", e.getMessage());
    } catch (ContractExeException e) {
      logger.error("TVM WithdrawReward: execute failure. Reason: {}", e.getMessage());
    }
    if (internalTx != null) {
      internalTx.reject();
    }
    return 0;
  }

  public long updateContextContractFactor() {
    ContractStateCapsule contractStateCapsule =
        contractState.getContractState(getContextAddress());

    if (contractStateCapsule == null) {
      contractStateCapsule = new ContractStateCapsule(
          contractState.getDynamicPropertiesStore().getCurrentCycleNumber());
      contractState.updateContractState(getContextAddress(), contractStateCapsule);
    } else {
      if (contractStateCapsule.catchUpToCycle(
          contractState.getDynamicPropertiesStore().getCurrentCycleNumber(),
          VMConfig.getDynamicEnergyThreshold(),
          VMConfig.getDynamicEnergyIncreaseFactor(),
          VMConfig.getDynamicEnergyMaxFactor())) {
        contractState.updateContractState(getContextAddress(), contractStateCapsule
        );
      }
    }
    contextContractFactor = contractStateCapsule.getEnergyFactor()
        + Constant.DYNAMIC_ENERGY_FACTOR_DECIMAL;
    return contextContractFactor;
  }

  public void addContextContractUsage(long value) {
    ContractStateCapsule contractStateCapsule =
        contractState.getContractState(getContextAddress());

    contractStateCapsule.addEnergyUsage(value);
    contractState.updateContractState(getContextAddress(), contractStateCapsule);
  }

  /**
   * Denotes problem when executing Ethereum bytecode. From blockchain and peer perspective this is
   * quite normal situation and doesn't mean exceptional situation in terms of the program
   * execution
   */
  @SuppressWarnings("serial")
  public static class BytecodeExecutionException extends RuntimeException {

    public BytecodeExecutionException(String message) {
      super(message);
    }

    public BytecodeExecutionException(String message, Object... args) {
      super(format(message, args));
    }
  }

  public static class AssetIssueException extends BytecodeExecutionException {

    public AssetIssueException(String message, Object... args) {
      super(format(message, args));
    }
  }

  public static class TransferException extends BytecodeExecutionException {

    public TransferException(String message, Object... args) {
      super(format(message, args));
    }
  }

  @SuppressWarnings("serial")
  public static class OutOfEnergyException extends BytecodeExecutionException {

    public OutOfEnergyException(String message, Object... args) {
      super(format(message, args));
    }
  }

  @SuppressWarnings("serial")
  public static class OutOfTimeException extends BytecodeExecutionException {

    public OutOfTimeException(String message, Object... args) {
      super(format(message, args));
    }
  }

  @SuppressWarnings("serial")
  public static class OutOfMemoryException extends BytecodeExecutionException {

    public OutOfMemoryException(String message, Object... args) {
      super(format(message, args));
    }
  }

  @SuppressWarnings("serial")
  public static class OutOfStorageException extends BytecodeExecutionException {

    public OutOfStorageException(String message, Object... args) {
      super(format(message, args));
    }
  }

  @SuppressWarnings("serial")
  public static class PrecompiledContractException extends BytecodeExecutionException {

    public PrecompiledContractException(String message, Object... args) {
      super(format(message, args));
    }
  }

  @SuppressWarnings("serial")
  public static class IllegalOperationException extends BytecodeExecutionException {

    public IllegalOperationException(String message, Object... args) {
      super(format(message, args));
    }
  }

  @SuppressWarnings("serial")
  public static class InvalidCodeException extends BytecodeExecutionException {

    public InvalidCodeException(String message) {
      super(message);
    }
  }

  @SuppressWarnings("serial")
  public static class BadJumpDestinationException extends BytecodeExecutionException {

    public BadJumpDestinationException(String message, Object... args) {
      super(format(message, args));
    }
  }

  @SuppressWarnings("serial")
  public static class StackTooSmallException extends BytecodeExecutionException {

    public StackTooSmallException(String message, Object... args) {
      super(format(message, args));
    }
  }

  @SuppressWarnings("serial")
  public static class ReturnDataCopyIllegalBoundsException extends BytecodeExecutionException {

    public ReturnDataCopyIllegalBoundsException(DataWord off, DataWord size,
        long returnDataSize) {
      super(String
          .format(
              "Illegal RETURNDATACOPY arguments: offset (%s) + size (%s) > RETURNDATASIZE (%d)",
              off, size, returnDataSize));
    }
  }

  @SuppressWarnings("serial")
  public static class JVMStackOverFlowException extends BytecodeExecutionException {

    public JVMStackOverFlowException() {
      super("StackOverflowError:  exceed default JVM stack size!");
    }
  }

  @SuppressWarnings("serial")
  public static class StaticCallModificationException extends BytecodeExecutionException {

    public StaticCallModificationException() {
      super("Attempt to call a state modifying opcode inside STATICCALL");
    }
  }

  public static class Exception {

    private Exception() {
    }

    public static OutOfEnergyException notEnoughSpendEnergy(String hint, long needEnergy,
        long leftEnergy) {
      return new OutOfEnergyException(
          "Not enough energy for '%s' executing: needEnergy[%d], leftEnergy[%d];", hint, needEnergy,
          leftEnergy);
    }

    public static OutOfTimeException notEnoughTime(String op) {
      return new OutOfTimeException(
          "CPU timeout for '%s' operation executing", op);
    }

    public static OutOfTimeException alreadyTimeOut() {
      return new OutOfTimeException("Already Time Out");
    }

    public static OutOfMemoryException memoryOverflow(int op) {
      return new OutOfMemoryException("Out of Memory when '%s' operation executing",
          Op.getNameOf(op));
    }

    public static OutOfStorageException notEnoughStorage() {
      return new OutOfStorageException("Not enough ContractState resource");
    }

    public static PrecompiledContractException contractValidateException(TronException e) {
      return new PrecompiledContractException(e.getMessage());
    }

    public static PrecompiledContractException contractExecuteException(TronException e) {
      return new PrecompiledContractException(e.getMessage());
    }

    public static OutOfEnergyException energyOverflow(BigInteger actualEnergy,
        BigInteger energyLimit) {
      return new OutOfEnergyException("Energy value overflow: actualEnergy[%d], energyLimit[%d];",
          actualEnergy.longValueExact(), energyLimit.longValueExact());
    }

    public static IllegalOperationException invalidOpCode(byte... opCode) {
      return new IllegalOperationException("Invalid operation code: opCode[%s];",
          Hex.toHexString(opCode, 0, 1));
    }

    public static InvalidCodeException invalidCodeException() {
      return new InvalidCodeException("invalid code: must not begin with 0xef");
    }

    public static BadJumpDestinationException badJumpDestination(int pc) {
      return new BadJumpDestinationException("Operation with pc isn't 'JUMPDEST': PC[%d];", pc);
    }

    public static StackTooSmallException tooSmallStack(int expectedSize, int actualSize) {
      return new StackTooSmallException("Expected stack size %d but actual %d;", expectedSize,
          actualSize);
    }
  }

  @SuppressWarnings("serial")
  public class StackTooLargeException extends BytecodeExecutionException {

    public StackTooLargeException(String message) {
      super(message);
    }
  }
}
