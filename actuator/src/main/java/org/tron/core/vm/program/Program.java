/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.core.vm.program;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.crypto.Hash;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.runtime.InternalTransaction;
import org.tron.common.runtime.ProgramResult;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.utils.*;
import org.tron.core.capsule.*;
import org.tron.core.config.Parameter;
import org.tron.core.db.TransactionTrace;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.TronException;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.utils.TransactionUtil;
import org.tron.core.vm.*;
import org.tron.core.vm.config.VMConfig;
import org.tron.core.vm.nativecontract.*;
import org.tron.core.vm.nativecontract.param.*;
import org.tron.core.vm.program.invoke.ProgramInvoke;
import org.tron.core.vm.program.invoke.ProgramInvokeFactory;
import org.tron.core.vm.program.invoke.ProgramInvokeFactoryImpl;
import org.tron.core.vm.program.listener.CompositeProgramListener;
import org.tron.core.vm.program.listener.ProgramListenerAware;
import org.tron.core.vm.program.listener.ProgramStorageChangeListener;
import org.tron.core.vm.repository.Repository;
import org.tron.core.vm.trace.ProgramTrace;
import org.tron.core.vm.trace.ProgramTraceListener;
import org.tron.core.vm.utils.MUtil;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract.Builder;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.StrictMath.min;
import static java.lang.String.format;
import static org.apache.commons.lang3.ArrayUtils.*;
import static org.tron.common.utils.ByteUtil.stripLeadingZeroes;
import static org.tron.core.config.Parameter.ChainConstant.FROZEN_PERIOD;

/**
 * @author Roman Mandeleil
 * @since 01.06.2014
 */

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
  private final VMConfig config;
  private long nonce;
  private byte[] rootTransactionId;
  private InternalTransaction internalTransaction;
  private ProgramInvoke invoke;
  private ProgramInvokeFactory programInvokeFactory = new ProgramInvokeFactoryImpl();
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
  private int pc;
  private byte lastOp;
  private byte previouslyExecutedOp;
  private boolean stopped;
  private ProgramPrecompile programPrecompile;

  public Program(byte[] ops, ProgramInvoke programInvoke) {
    this(ops, programInvoke, null);
  }

  public Program(byte[] ops, ProgramInvoke programInvoke, InternalTransaction internalTransaction) {
    this(ops, programInvoke, internalTransaction, VMConfig.getInstance());
  }

  public Program(byte[] ops, ProgramInvoke programInvoke, InternalTransaction internalTransaction,
      VMConfig config) {
    this.config = config;
    this.invoke = programInvoke;
    this.internalTransaction = internalTransaction;
    this.ops = nullToEmpty(ops);

    traceListener = new ProgramTraceListener(config.vmTrace());
    this.memory = setupProgramListener(new Memory());
    this.stack = setupProgramListener(new Stack());
    this.contractState = setupProgramListener(new ContractState(programInvoke));
    this.trace = new ProgramTrace(config, programInvoke);
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

  public static String stringifyMultiline(byte[] code) {
    int index = 0;
    StringBuilder sb = new StringBuilder();
    BitSet mask = buildReachableBytecodesMask(code);
    ByteArrayOutputStream binData = new ByteArrayOutputStream();
    int binDataStartPC = -1;

    while (index < code.length) {
      final byte opCode = code[index];
      OpCode op = OpCode.code(opCode);

      if (!mask.get(index)) {
        if (binDataStartPC == -1) {
          binDataStartPC = index;
        }
        binData.write(code[index]);
        index++;
        if (index < code.length) {
          continue;
        }
      }

      if (binDataStartPC != -1) {
        sb.append(formatBinData(binData.toByteArray(), binDataStartPC));
        binDataStartPC = -1;
        binData = new ByteArrayOutputStream();
        if (index == code.length) {
          continue;
        }
      }

      sb.append(Utils.align("" + Integer.toHexString(index) + ":", ' ', 8, false));

      if (op == null) {
        sb.append("<UNKNOWN>: ").append(0xFF & opCode).append("\n");
        index++;
        continue;
      }

      if (op.name().startsWith("PUSH")) {
        sb.append(' ').append(op.name()).append(' ');

        int nPush = op.val() - OpCode.PUSH1.val() + 1;
        byte[] data = Arrays.copyOfRange(code, index + 1, index + nPush + 1);
        BigInteger bi = new BigInteger(1, data);
        sb.append("0x").append(bi.toString(16));
        if (bi.bitLength() <= 32) {
          sb.append(" (").append(new BigInteger(1, data).toString()).append(") ");
        }

        index += nPush + 1;
      } else {
        sb.append(' ').append(op.name());
        index++;
      }
      sb.append('\n');
    }

    return sb.toString();
  }

  static BitSet buildReachableBytecodesMask(byte[] code) {
    NavigableSet<Integer> gotos = new TreeSet<>();
    ByteCodeIterator it = new ByteCodeIterator(code);
    BitSet ret = new BitSet(code.length);
    int lastPush = 0;
    int lastPushPC = 0;
    do {
      ret.set(it.getPC()); // reachable bytecode
      if (it.isPush()) {
        lastPush = new BigInteger(1, it.getCurOpcodeArg()).intValue();
        lastPushPC = it.getPC();
      }
      if (it.getCurOpcode() == OpCode.JUMP || it.getCurOpcode() == OpCode.JUMPI) {
        if (it.getPC() != lastPushPC + 1) {
          // some PC arithmetic we totally can't deal with
          // assuming all bytecodes are reachable as a fallback
          ret.set(0, code.length);
          return ret;
        }
        int jumpPC = lastPush;
        if (!ret.get(jumpPC)) {
          // code was not explored yet
          gotos.add(jumpPC);
        }
      }
      if (it.getCurOpcode() == OpCode.JUMP || it.getCurOpcode() == OpCode.RETURN
          || it.getCurOpcode() == OpCode.STOP) {
        if (gotos.isEmpty()) {
          break;
        }
        it.setPC(gotos.pollFirst());
      }
    } while (it.next());
    return ret;
  }

  public static String stringify(byte[] code) {
    int index = 0;
    StringBuilder sb = new StringBuilder();

    while (index < code.length) {
      final byte opCode = code[index];
      OpCode op = OpCode.code(opCode);

      if (op == null) {
        sb.append(" <UNKNOWN>: ").append(0xFF & opCode).append(" ");
        index++;
        continue;
      }

      if (op.name().startsWith("PUSH")) {
        sb.append(' ').append(op.name()).append(' ');

        int nPush = op.val() - OpCode.PUSH1.val() + 1;
        byte[] data = Arrays.copyOfRange(code, index + 1, index + nPush + 1);
        BigInteger bi = new BigInteger(1, data);
        sb.append("0x").append(bi.toString(16)).append(" ");

        index += nPush + 1;
      } else {
        sb.append(' ').append(op.name());
        index++;
      }
    }

    return sb.toString();
  }

  public byte[] getRootTransactionId() {
    return rootTransactionId.clone();
  }

  public void setRootTransactionId(byte[] rootTransactionId) {
    this.rootTransactionId = rootTransactionId.clone();
  }

  public long getNonce() {
    return nonce;
  }

  public void setNonce(long nonceValue) {
    nonce = nonceValue;
  }

  public ProgramPrecompile getProgramPrecompile() {
    if (programPrecompile == null) {
      programPrecompile = ProgramPrecompile.compile(ops);
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
    stackPush(new DataWord(0));
  }

  public void stackPushOne() {
    DataWord stackWord = new DataWord(1);
    stackPush(stackWord);
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

    byte[] owner = TransactionTrace.convertToTronAddress(getContractAddress().getLast20Bytes());
    byte[] obtainer = TransactionTrace.convertToTronAddress(obtainerAddress.getLast20Bytes());

    if (VMConfig.allowTvmStake()) {
      withdrawRewardToBalance(owner, getContractState());
    }

    long balance = getContractState().getBalance(owner);

    if (logger.isDebugEnabled()) {
      logger.debug("Transfer to: [{}] heritage: [{}]",
          Hex.toHexString(obtainer),
          balance);
    }

    increaseNonce();

    addInternalTx(null, owner, obtainer, balance, null, "suicide", nonce,
        getContractState().getAccount(owner).getAssetMapV2());

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
    if (VMConfig.allowTvmStake()) {
      suicideFreezeBalanceAndVote(owner, obtainer, getContractState());
      //delete delegationStore
      getResult().addDeleteDelegation(this.getContractAddress());
    }
    getResult().addDeleteAccount(this.getContractAddress());
  }

  public Repository getContractState() {
    return this.contractState;
  }

  private void withdrawRewardToBalance(byte[] owner, Repository repository) {
    ContractService contractService = ContractService.getInstance();
    contractService.withdrawReward(owner, getContractState());
    AccountCapsule accountCapsule = repository.getAccount(owner);
    long oldBalance = accountCapsule.getBalance();
    long allowance = accountCapsule.getAllowance();
    accountCapsule.setInstance(accountCapsule.getInstance().toBuilder()
            .setBalance(oldBalance + allowance)
            .setAllowance(0L)
            .setLatestWithdrawTime(getTimestamp().longValue() * 1000)
            .build());
    // todo internal tx
    repository.putAccountValue(accountCapsule.createDbKey(), accountCapsule);
    if (logger.isDebugEnabled()) {
      logger.debug("Transfer withdraw allowance to balance {}", allowance);
    }
  }

  private void suicideFreezeBalanceAndVote(byte[] owner, byte[] obtainer, Repository repository) {
    AccountCapsule ownerCapsule = repository.getAccount(owner);
    if (ownerCapsule.getFrozenCount() == 0) {
      return;
    }

    //process owner frozen for self
    if (FastByteComparisons.compareTo(obtainer, 0, 20,
            TransactionTrace.convertToTronAddress(new byte[20]), 0, 20) == 0
            || FastByteComparisons.compareTo(owner, 0, 20, obtainer, 0, 20) == 0
            || FastByteComparisons.compareTo(obtainer, 0, 20,
            repository.getBlackHoleAddress(), 0, 20) == 0) {
      // if obtainer equal zero or black hole or owner
      byte[] realObtain = obtainer;
      if(FastByteComparisons.compareTo(owner, 0, 20, obtainer, 0, 20) == 0) {
        realObtain = repository.getBlackHoleAddress();
      }
      long unfreezeBalance = ownerCapsule.getFrozenList().get(0).getFrozenBalance();
      AccountCapsule realObtainCapsule = repository.getAccount(realObtain);
      realObtainCapsule.setBalance(realObtainCapsule.getBalance() + unfreezeBalance);
      ownerCapsule.setInstance(ownerCapsule.getInstance().toBuilder()
              .removeFrozen(0).build());
      repository.updateAccount(realObtain, realObtainCapsule);
      repository
              .addTotalNetWeight(-unfreezeBalance / Parameter.ChainConstant.TRX_PRECISION);
    } else {
      AccountCapsule obtainCapsule = repository.getAccount(obtainer);
      long now = getTimestamp().longValue() * 1000;
      long ownerBandwidthBalance = ownerCapsule.getFrozenList().get(0).getFrozenBalance();
      long ownerBandwidthExpire = ownerCapsule.getFrozenList().get(0).getExpireTime();
      long newBandwidthExpire = ownerBandwidthExpire;
      long newFrozenBalanceForBandwidth = ownerBandwidthBalance;
      if (obtainCapsule.getFrozenCount() > 0) {
        long obtainBandwidthBalance = obtainCapsule.getFrozenList().get(0).getFrozenBalance();
        long obtainBandwidthExpire = obtainCapsule.getFrozenList().get(0).getExpireTime();
        long maxExpire = repository.getDynamicPropertiesStore().getMinFrozenTime() * Parameter.ChainConstant.FROZEN_PERIOD;
        newBandwidthExpire = now
                + BigInteger.valueOf(Long.max(0, Long.min(ownerBandwidthExpire - now, maxExpire)))
                .multiply(BigInteger.valueOf(ownerBandwidthBalance))
                .add(BigInteger.valueOf(Long.max(0, Long.min(obtainBandwidthExpire - now, maxExpire)))
                        .multiply(BigInteger.valueOf(obtainBandwidthBalance)))
                .divide(BigInteger.valueOf(
                        Math.addExact(ownerBandwidthBalance, obtainBandwidthBalance)))
                .longValue();
        newFrozenBalanceForBandwidth = Math.addExact(ownerBandwidthBalance, obtainBandwidthBalance);
      }
      obtainCapsule.setFrozenForBandwidth(newFrozenBalanceForBandwidth, newBandwidthExpire);
      repository.updateAccount(obtainer, obtainCapsule);
      ownerCapsule.setInstance(ownerCapsule.getInstance().toBuilder()
              .removeFrozen(0).build());
    }
    //vote
    {
      VotesCapsule ownerVotesCapsule = repository.getVotesCapsule(owner);

      //get owner oldVotes
      List<Protocol.Vote> oldVotes;
      if (ownerVotesCapsule == null) {
        oldVotes = ownerCapsule.getVotesList();
      } else {
        oldVotes = ownerVotesCapsule.getOldVotes();
        //delete ownerVotesCapsule
        getResult().addDeleteVotes(this.getContractAddress());
      }
      // merge oldVotes to address(zero)
      if (!oldVotes.isEmpty()) {
        ownerCapsule.clearVotes();
        //merge oldVotes to zero
        byte[] zeroAddress = TransactionTrace.convertToTronAddress(new byte[20]);
        VotesCapsule zeroVotesCapsule = repository.getVotesCapsule(zeroAddress);
        if (zeroVotesCapsule == null) {
          zeroVotesCapsule = new VotesCapsule(ByteString.copyFrom(zeroAddress), oldVotes);
        } else {
          int zeroOldVoteSize = zeroVotesCapsule.getOldVotes().size();
          Map<ByteString, Integer> zeroOldVotesIndex = new HashMap<>(zeroOldVoteSize);
          for(int i = 0; i < zeroOldVoteSize; i++){
            zeroOldVotesIndex.put(zeroVotesCapsule.getOldVotes().get(i).getVoteAddress(), i);
          }
          for (Protocol.Vote vote : oldVotes) {
            if (zeroOldVotesIndex.containsKey(vote.getVoteAddress())) {
              int index = zeroOldVotesIndex.get(vote.getVoteAddress());
              long newOldVoteCount = vote.getVoteCount() + zeroVotesCapsule.getOldVotes().get(index).getVoteCount();
              zeroVotesCapsule.setOldVote(index, Protocol.Vote.newBuilder()
                      .setVoteAddress(vote.getVoteAddress())
                      .setVoteCount(newOldVoteCount).build());
            } else {
              zeroVotesCapsule.addOldVotes(vote.getVoteAddress(), vote.getVoteCount());
            }
          }
        }
        repository.updateVotesCapsule(zeroAddress, zeroVotesCapsule);
      }
    }

    repository.updateAccount(owner, ownerCapsule);
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
    byte[] senderAddress = TransactionTrace
        .convertToTronAddress(this.getContractAddress().getLast20Bytes());

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
      SmartContract newSmartContract = SmartContract.newBuilder()
          .setContractAddress(ByteString.copyFrom(newAddress)).setConsumeUserResourcePercent(100)
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
    ProgramInvoke programInvoke = programInvokeFactory.createProgramInvoke(
        this, new DataWord(newAddress), getContractAddress(), value, new DataWord(0),
        new DataWord(0),
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
      VM vm = new VM(config);
      Program program = new Program(programCode, programInvoke, internalTx, config);
      program.setRootTransactionId(this.rootTransactionId);
      vm.play(program);
      createResult = program.getResult();
      getTrace().merge(program.getTrace());
      // always commit nonce
      this.nonce = program.nonce;

    }

    // 4. CREATE THE CONTRACT OUT OF RETURN
    byte[] code = createResult.getHReturn();

    long saveCodeEnergy = (long) getLength(code) * EnergyCost.getInstance().getCREATE_DATA();

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

      internalTx.reject();
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
            Hex.toHexString(
                TransactionTrace.convertToTronAddress(getContractAddress().getLast20Bytes())),
            refundEnergy);
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
    byte[] codeAddress = TransactionTrace
        .convertToTronAddress(msg.getCodeAddress().getLast20Bytes());
    byte[] senderAddress = TransactionTrace
        .convertToTronAddress(getContractAddress().getLast20Bytes());
    byte[] contextAddress = msg.getType().callIsStateless() ? senderAddress : codeAddress;

    if (logger.isDebugEnabled()) {
      logger.debug(msg.getType().name()
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
      DataWord callValue = msg.getType().callIsDelegate() ? getCallValue() : msg.getEndowment();
      ProgramInvoke programInvoke = programInvokeFactory.createProgramInvoke(
          this, new DataWord(contextAddress),
          msg.getType().callIsDelegate() ? getCallerAddress() : getContractAddress(),
          !isTokenTransfer ? callValue : new DataWord(0),
          !isTokenTransfer ? new DataWord(0) : callValue,
          !isTokenTransfer ? new DataWord(0) : msg.getTokenId(),
          contextBalance, data, deposit, msg.getType().callIsStatic() || isStaticCall(),
          byTestingSuite(), vmStartInUs, getVmShouldEndInUs(), msg.getEnergy().longValueSafe());
      if (isConstantCall()) {
        programInvoke.setConstantCall();
      }
      VM vm = new VM(config);
      Program program = new Program(programCode, programInvoke, internalTx, config);
      program.setRootTransactionId(this.rootTransactionId);
      vm.play(program);
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

  public void futureRefundEnergy(long energyValue) {
    logger.debug("Future refund added: [{}]", energyValue);
    getResult().addFutureRefund(energyValue);
  }

  public void resetFutureRefund() {
    getResult().resetFutureRefund();
  }

  public void storageSave(DataWord word1, DataWord word2) {
    DataWord keyWord = word1.clone();
    DataWord valWord = word2.clone();
    getContractState()
        .putStorageValue(
            TransactionTrace.convertToTronAddress(getContractAddress().getLast20Bytes()), keyWord,
            valWord);
  }

  public byte[] getCode() {
    return ops.clone();
  }

  public byte[] getCodeAt(DataWord address) {
    byte[] code = invoke.getDeposit()
        .getCode(TransactionTrace.convertToTronAddress(address.getLast20Bytes()));
    return nullToEmpty(code);
  }

  public byte[] getCodeHashAt(DataWord address) {
    byte[] tronAddr = TransactionTrace.convertToTronAddress(address.getLast20Bytes());
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
    long balance = getContractState()
        .getBalance(TransactionTrace.convertToTronAddress(address.getLast20Bytes()));
    return new DataWord(balance);
  }

  public DataWord getRewardBalance(DataWord address) {
    ContractService contractService = ContractService.getInstance();
    long rewardBalance = contractService
            .queryReward(TransactionTrace.convertToTronAddress(address.getLast20Bytes()), getContractState());
    return new DataWord(rewardBalance);
  }

  public DataWord isContract(DataWord address) {
    ContractCapsule contract = getContractState()
        .getContract(TransactionTrace.convertToTronAddress(address.getLast20Bytes()));
    return contract != null ? new DataWord(1) : new DataWord(0);
  }

  public DataWord isSRCandidate(DataWord address) {
    WitnessCapsule witnessCapsule = getContractState()
            .getWitnessCapsule(TransactionTrace.convertToTronAddress(address.getLast20Bytes()));
    return witnessCapsule != null ? new DataWord(1) : new DataWord(0);
  }

  public DataWord getOriginAddress() {
    return invoke.getOriginAddress().clone();
  }

  public DataWord getCallerAddress() {
    return invoke.getCallerAddress().clone();
  }

  public DataWord getChainId() {
    return new DataWord(Hex.toHexString(getContractState()
        .getBlockByNum(0).getBlockId().getBytes()));
  }
  public DataWord getDropPrice() {
    return new DataWord(1);
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
    DataWord ret = getContractState()
        .getStorageValue(
            TransactionTrace.convertToTronAddress(getContractAddress().getLast20Bytes()),
            key.clone());
    return ret == null ? null : ret.clone();
  }

  public DataWord getTokenBalance(DataWord address, DataWord tokenId) {
    checkTokenIdInTokenBalance(tokenId);
    long ret = getContractState()
        .getTokenBalance(TransactionTrace.convertToTronAddress(address.getLast20Bytes()),
            String.valueOf(tokenId.longValue()).getBytes());
    return ret == 0 ? new DataWord(0) : new DataWord(ret);
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
        globalOutput.append("[Op: ").append(OpCode.code(lastOp).name()).append("]\n");
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
    if(VMConfig.allowTvmIstanbul()) {
      senderAddress = TransactionTrace
          .convertToTronAddress(this.getContractAddress().getLast20Bytes());
    } else {
      senderAddress = TransactionTrace
          .convertToTronAddress(this.getCallerAddress().getLast20Bytes());
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

    byte[] senderAddress = TransactionTrace
        .convertToTronAddress(this.getContractAddress().getLast20Bytes());
    byte[] codeAddress = TransactionTrace
        .convertToTronAddress(msg.getCodeAddress().getLast20Bytes());
    byte[] contextAddress = msg.getType().callIsStateless() ? senderAddress : codeAddress;

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
      contract.setCallerAddress(TransactionTrace.convertToTronAddress(msg.getType().callIsDelegate()
          ? getCallerAddress().getLast20Bytes() : getContractAddress().getLast20Bytes()));
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

  public DataWord getCallEnergy(OpCode op, DataWord requestedEnergy, DataWord availableEnergy) {
    return requestedEnergy.compareTo(availableEnergy) > 0 ? availableEnergy : requestedEnergy;
  }

  public DataWord getCreateEnergy(DataWord availableEnergy) {
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

    public OpCode getCurOpcode() {
      return pc < code.length ? OpCode.code(code[pc]) : null;
    }

    public boolean isPush() {
      return getCurOpcode() != null && getCurOpcode().name().startsWith("PUSH");
    }

    public byte[] getCurOpcodeArg() {
      if (isPush()) {
        int nPush = getCurOpcode().val() - OpCode.PUSH1.val() + 1;
        byte[] data = Arrays.copyOfRange(code, pc + 1, pc + nPush + 1);
        return data;
      } else {
        return new byte[0];
      }
    }

    public boolean next() {
      pc += 1 + getCurOpcodeArg().length;
      return pc < code.length;
    }
  }

  public boolean stake(DataWord srAddress, DataWord stakeAmount) {
    Repository repository = getContractState().newRepositoryChild();
    StakeProcessor stakeProcessor = new StakeProcessor();
    StakeParam stakeParam = new StakeParam();
    byte[] owner = TransactionTrace.convertToTronAddress(getContractAddress().getLast20Bytes());
    stakeParam.setOwnerAddress(owner);
    stakeParam.setSrAddress(TransactionTrace.convertToTronAddress(srAddress.getLast20Bytes()));
    stakeParam.setNow(getTimestamp().longValue() * 1000);
    try {
      stakeParam.setStakeAmount(stakeAmount.sValue().longValueExact());
      stakeProcessor.process(stakeParam, repository);
      repository.commit();
      return true;
    } catch (ContractValidateException e) {
      logger.error("validateForStake failure:{}", e.getMessage());
    } catch (ContractExeException e) {
      logger.error("executeForStake failure:{}", e.getMessage());
    } catch (ArithmeticException e) {
      logger.error("stakeAmount out of long range");
    }
    return false;
  }

  public boolean unstake() {
    Repository repository = getContractState().newRepositoryChild();
    UnstakeProcessor unstakeProcessor = new UnstakeProcessor();
    UnstakeParam unstakeParam = new UnstakeParam();
    byte[] owner = TransactionTrace.convertToTronAddress(getContractAddress().getLast20Bytes());
    unstakeParam.setOwnerAddress(owner);
    unstakeParam.setNow(getTimestamp().longValue() * 1000);
    try {
      unstakeProcessor.validate(unstakeParam, repository);
      unstakeProcessor.execute(unstakeParam, repository);
      repository.commit();
      return true;
    } catch (ContractValidateException e) {
      logger.error("validateForUnstake failure:{}", e.getMessage());
    } catch (ContractExeException e) {
      logger.error("executeForUnstake failure:{}", e.getMessage());
    }
    return false;
  }

  public void withdrawReward() {
    Repository repository = getContractState().newRepositoryChild();
    WithdrawRewardProcessor withdrawRewardContractProcessor = new WithdrawRewardProcessor();
    WithdrawRewardParam withdrawRewardParam = new WithdrawRewardParam();
    byte[] ownerAddress = TransactionTrace.convertToTronAddress(getContractAddress().getLast20Bytes());
    withdrawRewardParam.setTargetAddress(ownerAddress);
    try {
      withdrawRewardContractProcessor.validate(withdrawRewardParam, repository,
          getTimestamp().longValue() * 1000);
      long allowance = withdrawRewardContractProcessor.execute(withdrawRewardParam, repository,
          getTimestamp().longValue() * 1000);
      stackPush(new DataWord(allowance));
      repository.commit();
    } catch (ContractValidateException e) {
      logger.error("validateForWithdrawReward failure:{}", e.getMessage());
      stackPushZero();
    }
  }

  public void tokenIssue(DataWord name, DataWord abbr, DataWord totalSupply, DataWord precision) {
    Repository repository = getContractState().newRepositoryChild();
    byte[] ownerAddress = TransactionTrace.convertToTronAddress(getContractAddress().getLast20Bytes());
    TokenIssueProcessor tokenIssueProcessor = new TokenIssueProcessor();
    TokenIssueParam tokenIssueParam = new TokenIssueParam();
    tokenIssueParam.setName(name.getNoEndZeroesData());
    tokenIssueParam.setAbbr(abbr.getNoEndZeroesData());
    tokenIssueParam.setTotalSupply(totalSupply.sValue().longValueExact());
    tokenIssueParam.setPrecision(precision.sValue().intValueExact());
    tokenIssueParam.setOwnerAddress(ownerAddress);
    try {
      tokenIssueProcessor.validate(tokenIssueParam, repository);
      tokenIssueProcessor.execute(tokenIssueParam, repository);
      stackPush(new DataWord(repository.getTokenIdNum()));
      repository.commit();
    } catch (ContractValidateException e) {
      logger.error("validateForAssetIssue failure:{}", e.getMessage());
      stackPushZero();
    }
  }

  public void updateAsset(DataWord urlDataOffs, DataWord descriptionDataOffs) {
    Repository repository = getContractState().newRepositoryChild();
    byte[] ownerAddress = TransactionTrace.convertToTronAddress(getContractAddress().getLast20Bytes());
    DataWord urlSize = memoryLoad(urlDataOffs);
    DataWord descriptionSize = memoryLoad(descriptionDataOffs);
    byte[] urlData = memoryChunk(urlDataOffs.intValueSafe() + DataWord.WORD_SIZE,
            urlSize.intValueSafe());
    byte[] descriptionData = memoryChunk(descriptionDataOffs.intValueSafe() + DataWord.WORD_SIZE,
            descriptionSize.intValueSafe());
    UpdateAssetParam updateAssetParam = new UpdateAssetParam();
    updateAssetParam.setOwnerAddress(ownerAddress);
    updateAssetParam.setNewUrl(urlData);
    updateAssetParam.setNewDesc(descriptionData);
    UpdateAssetProcessor updateAssetProcessor = new UpdateAssetProcessor();
    try {
      updateAssetProcessor.validate(updateAssetParam, repository);
      updateAssetProcessor.execute(updateAssetParam, repository);
      stackPushOne();
      repository.commit();
    } catch (ContractValidateException e) {
      logger.error("validateForUpdateAsset failure:{}", e.getMessage());
      stackPushZero();
    }
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

    public static OutOfEnergyException notEnoughOpEnergy(OpCode op, long opEnergy,
        long programEnergy) {
      return new OutOfEnergyException(
          "Not enough energy for '%s' operation executing: opEnergy[%d], programEnergy[%d];", op,
          opEnergy,
          programEnergy);
    }

    public static OutOfEnergyException notEnoughOpEnergy(OpCode op, DataWord opEnergy,
        DataWord programEnergy) {
      return notEnoughOpEnergy(op, opEnergy.longValue(), programEnergy.longValue());
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


    public static OutOfMemoryException memoryOverflow(OpCode op) {
      return new OutOfMemoryException("Out of Memory when '%s' operation executing", op.name());
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
