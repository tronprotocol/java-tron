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
package org.tron.common.runtime.vm.program;

import static java.lang.StrictMath.min;
import static java.lang.String.format;
import static org.apache.commons.lang3.ArrayUtils.EMPTY_BYTE_ARRAY;
import static org.apache.commons.lang3.ArrayUtils.getLength;
import static org.apache.commons.lang3.ArrayUtils.isEmpty;
import static org.apache.commons.lang3.ArrayUtils.isNotEmpty;
import static org.apache.commons.lang3.ArrayUtils.nullToEmpty;
import static org.tron.common.runtime.utils.MUtil.convertToTronAddress;
import static org.tron.common.runtime.utils.MUtil.transfer;
import static org.tron.common.utils.BIUtil.isPositive;
import static org.tron.common.utils.BIUtil.toBI;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.crypto.ECKey;
import org.tron.common.runtime.config.DefaultConfig;
import org.tron.common.runtime.config.SystemProperties;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.runtime.vm.DropCost;
import org.tron.common.runtime.vm.MessageCall;
import org.tron.common.runtime.vm.OpCode;
import org.tron.common.runtime.vm.PrecompiledContracts;
import org.tron.common.runtime.vm.VM;
import org.tron.common.runtime.vm.program.invoke.ProgramInvoke;
import org.tron.common.runtime.vm.program.invoke.ProgramInvokeFactory;
import org.tron.common.runtime.vm.program.invoke.ProgramInvokeFactoryImpl;
import org.tron.common.runtime.vm.program.listener.CompositeProgramListener;
import org.tron.common.runtime.vm.program.listener.ProgramListenerAware;
import org.tron.common.runtime.vm.program.listener.ProgramStorageChangeListener;
import org.tron.common.runtime.vm.trace.ProgramTrace;
import org.tron.common.runtime.vm.trace.ProgramTraceListener;
import org.tron.common.storage.Deposit;
import org.tron.common.utils.ByteArraySet;
import org.tron.common.utils.ByteUtil;
import org.tron.common.utils.FastByteComparisons;
import org.tron.common.utils.Sha256Hash;
import org.tron.common.utils.Utils;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.exception.ContractExeException;
import org.tron.protos.Protocol;

/**
 * @author Roman Mandeleil
 * @since 01.06.2014
 */
public class Program {

  private static final Logger logger = LoggerFactory.getLogger("VM");

  private static final int MAX_DEPTH = 1024;
  //Max size for stack checks
  private static final int MAX_STACKSIZE = 1024;

  private InternalTransaction transaction;

  private ProgramInvoke invoke;
  private ProgramInvokeFactory programInvokeFactory = new ProgramInvokeFactoryImpl();

  private ProgramOutListener listener;
  private ProgramTraceListener traceListener;
  private ProgramStorageChangeListener storageDiffListener = new ProgramStorageChangeListener();
  private CompositeProgramListener programListener = new CompositeProgramListener();

  private Stack stack;
  private Memory memory;
  private Storage storage;
  private byte[] returnDataBuffer;

  private ProgramResult result = new ProgramResult();
  private ProgramTrace trace = new ProgramTrace();

  //private byte[] codeHash;
  private byte[] ops;
  private int pc;
  private byte lastOp;
  private byte previouslyExecutedOp;
  private boolean stopped;
  private ByteArraySet touchedAccounts = new ByteArraySet();

  private ProgramPrecompile programPrecompile;

  private final SystemProperties config;

  public Program(byte[] ops, ProgramInvoke programInvoke) {
    this(ops, programInvoke, null);
  }

  public Program(byte[] ops, ProgramInvoke programInvoke, InternalTransaction transaction) {
    this(ops, programInvoke, transaction, SystemProperties.getDefault());
  }

  public Program(byte[] ops, ProgramInvoke programInvoke, InternalTransaction transaction,
      SystemProperties config) {
    this(null, ops, programInvoke, transaction, config);
  }

  public Program(byte[] codeHash, byte[] ops, ProgramInvoke programInvoke,
      InternalTransaction transaction, SystemProperties config) {
    this.config = config;
    this.invoke = programInvoke;
    this.transaction = transaction;

    //this.codeHash = codeHash;
    this.ops = nullToEmpty(ops);

    //traceListener = new ProgramTraceListener(config.vmTrace());
    this.memory = setupProgramListener(new Memory());
    this.stack = setupProgramListener(new Stack());
    this.storage = setupProgramListener(new Storage(programInvoke));
    //this.trace = new ProgramTrace(config, programInvoke);
  }

  public ProgramPrecompile getProgramPrecompile() {
    if (programPrecompile == null) {
            /*
            if (codeHash != null && commonConfig.precompileSource() != null) {
                programPrecompile = commonConfig.precompileSource().get(codeHash);
            }
            */
      if (programPrecompile == null) {
        programPrecompile = ProgramPrecompile.compile(ops);

                /*
                if (codeHash != null && commonConfig.precompileSource() != null) {
                    commonConfig.precompileSource().put(codeHash, programPrecompile);
                }
                */
      }

    }
    return programPrecompile;
  }

  public int getCallDeep() {
    return invoke.getCallDeep();
  }

  private InternalTransaction addInternalTx(DataWord dropLimit, byte[] senderAddress,
      byte[] receiveAddress,
      long value, byte[] data, String note) {

    InternalTransaction result = null;
    if (transaction != null) {
      //data = config.recordInternalTransactionsData() ? data : null;
      //result = getResult().addInternalTransaction(transaction.getHash(), getCallDeep(),
      //        getGasPrice(), gasLimit, senderAddress, receiveAddress, value.toByteArray(), data, note);
      result = getResult().addInternalTransaction(transaction.getHash(), getCallDeep(),
          senderAddress, receiveAddress, value, data, note);
    }

    return result;
  }

  private <T extends ProgramListenerAware> T setupProgramListener(T programListenerAware) {
    if (programListener.isEmpty()) {
      //programListener.addListener(traceListener);
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
   * Should be set only after the OP is fully executed.
   */
  public void setPreviouslyExecutedOp(byte op) {
    this.previouslyExecutedOp = op;
  }

  /**
   * Returns the last fully executed OP.
   */
  public byte getPreviouslyExecutedOp() {
    return this.previouslyExecutedOp;
  }

  public void stackPush(byte[] data) {
    stackPush(new DataWord(data));
  }

  public void stackPushZero() {
    stackPush(new DataWord(0));
  }

  public void stackPushOne() {
    DataWord stackWord = new DataWord(1);
    stackPush(stackWord);
  }

  public void stackPush(DataWord stackWord) {
    verifyStackOverflow(0, 1); //Sanity Check
    stack.push(stackWord);
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
   * Verifies that the stack is at least <code>stackSize</code>
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
    if ((stack.size() - argsReqs + returnReqs) > MAX_STACKSIZE) {
      throw new StackTooLargeException(
          "Expected: overflow " + MAX_STACKSIZE + " elements stack limit");
    }
  }

  public int getMemSize() {
    return memory.size();
  }

  public void memorySave(DataWord addrB, DataWord value) {
    memory.write(addrB.intValue(), value.getData(), value.getData().length, false);
  }

  public void memorySaveLimited(int addr, byte[] data, int dataSize) {
    memory.write(addr, data, dataSize, true);
  }

  public void memorySave(int addr, byte[] value) {
    memory.write(addr, value, value.length, false);
  }

  public void memoryExpand(DataWord outDataOffs, DataWord outDataSize) {
    if (!outDataSize.isZero()) {
      memory.extend(outDataOffs.intValue(), outDataSize.intValue());
    }
  }

  /**
   * Allocates a piece of memory and stores value at given offset address
   *
   * @param addr is the offset address
   * @param allocSize size of memory needed to write
   * @param value the data to write to memory
   */
  public void memorySave(int addr, int allocSize, byte[] value) {
    memory.extendAndWrite(addr, allocSize, value);
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
   * Allocates extra memory in the program for a specified size, calculated from a given offset
   *
   * @param offset the memory address offset
   * @param size the number of bytes to allocate
   */
  public void allocateMemory(int offset, int size) {
    memory.extend(offset, size);
  }


  public void suicide(DataWord obtainerAddress)
      throws ContractExeException {

    byte[] owner = convertToTronAddress(getOwnerAddress().getLast20Bytes());
    byte[] obtainer = convertToTronAddress(obtainerAddress.getLast20Bytes());
    long balance = getStorage().getBalance(owner);

    if (logger.isInfoEnabled()) {
      logger.info("Transfer to: [{}] heritage: [{}]",
          Hex.toHexString(obtainer),
          balance);
    }

    addInternalTx(null, owner, obtainer, balance, null, "suicide");

    if (FastByteComparisons.compareTo(owner, 0, 20, obtainer, 0, 20) == 0) {
      // if owner == obtainer just zeroing account according to Yellow Paper
      getStorage().addBalance(owner, -balance);
    } else {
      transfer(getStorage(), owner, obtainer, balance);
    }

    getResult().addDeleteAccount(this.getOwnerAddress());
  }

  public Deposit getStorage() {
    return this.storage;
  }

  @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
  public void createContract(DataWord value, DataWord memStart, DataWord memSize)
      throws ContractExeException {
    returnDataBuffer = null; // reset return buffer right before the call

    if (getCallDeep() == MAX_DEPTH) {
      stackPushZero();
      return;
    }

    byte[] senderAddress = convertToTronAddress(this.getOwnerAddress().getLast20Bytes());
    long endowment = value.value().longValue();
    if (getStorage().getBalance(senderAddress) < endowment) {
      stackPushZero();
      return;
    }

    // [1] FETCH THE CODE FROM THE MEMORY
    byte[] programCode = memoryChunk(memStart.intValue(), memSize.intValue());

    if (logger.isInfoEnabled()) {
      logger.info("creating a new contract inside contract run: [{}]",
          Hex.toHexString(senderAddress));
    }

    //BlockchainConfig blockchainConfig = config.getBlockchainConfig().getConfigForBlock(getNumber().longValue());
    //  actual gas subtract
    //DataWord gasLimit = blockchainConfig.getCreateGas(getGas());
    //spendGas(gasLimit.longValue(), "internal call");
    DataWord gasLimit = new DataWord(DropCost.getInstance().getCREATE());

    // [2] CREATE THE CONTRACT ADDRESS
    // byte[] newAddress = HashUtil.calcNewAddr(getOwnerAddress().getLast20Bytes() nonce);
    byte[] privKey = Sha256Hash.hash(getOwnerAddress().getData());
    ECKey ecKey = ECKey.fromPrivate(privKey);
    byte[] newAddress = ecKey.getAddress();

    AccountCapsule existingAddr = getStorage().getAccount(newAddress);
    //boolean contractAlreadyExists = existingAddr != null && existingAddr.isContractExist(blockchainConfig);
    boolean contractAlreadyExists = existingAddr != null;

        /*
        if (byTestingSuite()) {
            // This keeps track of the contracts created for a test
            getResult().addCallCreate(programCode, EMPTY_BYTE_ARRAY,
                    gasLimit.getNoLeadZeroesData(),
                    value.getNoLeadZeroesData());
        }
        */

    Deposit deposit = getStorage();

    //In case of hashing collisions, check for any balance before createAccount()
    long oldBalance = deposit.getBalance(newAddress);
    deposit.createAccount(newAddress, Protocol.AccountType.Contract);

    deposit.addBalance(newAddress, oldBalance);

    // [4] TRANSFER THE BALANCE
    long newBalance = 0L;
    if (!byTestingSuite()) {
      deposit.addBalance(senderAddress, -endowment);
      newBalance = deposit.addBalance(newAddress, endowment);
    }

    checkCPULimit("BEFORE CREATE");


    // [5] COOK THE INVOKE AND EXECUTE
    InternalTransaction internalTx = addInternalTx(getDroplimit(), senderAddress, null, endowment,
        programCode, "create");
    long vmStartInUs = System.nanoTime() / 1000;
    ProgramInvoke programInvoke = programInvokeFactory.createProgramInvoke(
        this, new DataWord(newAddress), getOwnerAddress(), value,
        newBalance, null, deposit, false, byTestingSuite(), vmStartInUs,
        getVmShouldEndInUs());

    ProgramResult result = ProgramResult.createEmpty();

    if (contractAlreadyExists) {
      result.setException(new BytecodeExecutionException(
          "Trying to create a contract with existing contract address: 0x" + Hex
              .toHexString(newAddress)));
    } else if (isNotEmpty(programCode)) {
      VM vm = new VM(config);
      Program program = new Program(programCode, programInvoke, internalTx, config);
      vm.play(program);
      result = program.getResult();

      getResult().merge(result);
    }

    checkCPULimit("AFTER CREATE");

    // 4. CREATE THE CONTRACT OUT OF RETURN
    byte[] code = result.getHReturn();

    //long storageCost = getLength(code) * getBlockchainConfig().getGasCost().getCREATE_DATA();
    long storageCost = getLength(code) * DropCost.getInstance().getCREATE_DATA();
    // todo storage cost?
    // long afterSpend = programInvoke.getDroplimit().longValue() - storageCost - result.getDropUsed();
    if (getLength(code) > DefaultConfig.getMaxCodeLength()) {
      result.setException(Exception
          .notEnoughSpendingGas("Contract size too large: " + getLength(result.getHReturn()),
              storageCost, this));
    } else if (!result.isRevert()) {
      result.spendDrop(storageCost);
      deposit.saveCode(newAddress, code);
    }

    if (result.getException() != null || result.isRevert()) {
      logger.debug("contract run halted by Exception: contract: [{}], exception: [{}]",
          Hex.toHexString(newAddress),
          result.getException());

      internalTx.reject();
      result.rejectInternalTransactions();

      // deposit.rollback();
      stackPushZero();

      if (result.getException() != null) {
        return;
      } else {
        returnDataBuffer = result.getHReturn();
      }
    } else {
      if (!byTestingSuite()) {
        deposit.commit();
      }

      // IN SUCCESS PUSH THE ADDRESS INTO THE STACK
      stackPush(new DataWord(newAddress));
    }

    // 5. REFUND THE REMAIN GAS
    long refundGas = gasLimit.longValue() - result.getDropUsed();
    if (refundGas > 0) {
      refundGas(refundGas, "remain gas from the internal call");
      if (logger.isInfoEnabled()) {
        logger.info("The remaining gas is refunded, account: [{}], gas: [{}] ",
            Hex.toHexString(convertToTronAddress(getOwnerAddress().getLast20Bytes())),
            refundGas);
      }
    }
  }

  /**
   * That method is for internal code invocations
   * <p/>
   * - Normal calls invoke a specified contract which updates itself - Stateless calls invoke code
   * from another contract, within the context of the caller
   *
   * @param msg is the message call object
   */
  public void callToAddress(MessageCall msg)
      throws ContractExeException, OutOfResourceException {
    returnDataBuffer = null; // reset return buffer right before the call

    if (getCallDeep() == MAX_DEPTH) {
      stackPushZero();
      refundGas(msg.getGas().longValue(), " call deep limit reach");
      return;
    }

    byte[] data = memoryChunk(msg.getInDataOffs().intValue(), msg.getInDataSize().intValue());

    // FETCH THE SAVED STORAGE
    byte[] codeAddress = convertToTronAddress(msg.getCodeAddress().getLast20Bytes());
    byte[] senderAddress = convertToTronAddress(getOwnerAddress().getLast20Bytes());
    byte[] contextAddress = msg.getType().callIsStateless() ? senderAddress : codeAddress;

    if (logger.isInfoEnabled()) {
      logger.info(msg.getType().name()
              + " for existing contract: address: [{}], outDataOffs: [{}], outDataSize: [{}]  ",
          Hex.toHexString(contextAddress), msg.getOutDataOffs().longValue(),
          msg.getOutDataSize().longValue());
    }

    //Repository track = getStorage().startTracking();
    Deposit deposit = getStorage().newDepositChild();

    // 2.1 PERFORM THE VALUE (endowment) PART
    long endowment = msg.getEndowment().value().longValue();
    long senderBalance = deposit.getBalance(senderAddress);
    if (senderBalance < endowment) {
      stackPushZero();
      refundGas(msg.getGas().longValue(), "refund gas from message call");
      return;
    }

    // FETCH THE CODE
    AccountCapsule accountCapsule = getStorage().getAccount(codeAddress);

    byte[] programCode =
        accountCapsule != null ? getStorage().getCode(codeAddress) : EMPTY_BYTE_ARRAY;

    long contextBalance = 0L;
    if (byTestingSuite()) {
      // This keeps track of the calls created for a test
      getResult().addCallCreate(data, contextAddress,
          msg.getGas().getNoLeadZeroesData(),
          msg.getEndowment().getNoLeadZeroesData());
    } else {
      deposit.addBalance(senderAddress, -endowment);
      contextBalance = deposit.addBalance(contextAddress, endowment);
    }

    // CREATE CALL INTERNAL TRANSACTION
    InternalTransaction internalTx = addInternalTx(getDroplimit(), senderAddress, contextAddress,
        endowment, data, "call");

    checkCPULimit("BEFORE CALL");

    ProgramResult result = null;
    if (isNotEmpty(programCode)) {
      long vmStartInUs = System.nanoTime() / 1000;
      ProgramInvoke programInvoke = programInvokeFactory.createProgramInvoke(
          this, new DataWord(contextAddress),
          msg.getType().callIsDelegate() ? getCallerAddress() : getOwnerAddress(),
          msg.getType().callIsDelegate() ? getCallValue() : msg.getEndowment(),
          contextBalance, data, deposit, msg.getType().callIsStatic() || isStaticCall(),
          byTestingSuite(), vmStartInUs, getVmShouldEndInUs());

      VM vm = new VM(config);
      Program program = new Program(null, programCode, programInvoke, internalTx, config);
      vm.play(program);
      result = program.getResult();

      getTrace().merge(program.getTrace());
      getResult().merge(result);

      if (result.getException() != null || result.isRevert()) {
        logger.debug("contract run halted by Exception: contract: [{}], exception: [{}]",
            Hex.toHexString(contextAddress),
            result.getException());

        internalTx.reject();
        result.rejectInternalTransactions();

        // deposit.rollback();
        stackPushZero();

        if (result.getException() != null) {
          return;
        }
      } else {
        // 4. THE FLAG OF SUCCESS IS ONE PUSHED INTO THE STACK
        deposit.commit();
        stackPushOne();
      }

      if (byTestingSuite()) {
        logger.info("Testing run, skipping storage diff listener");
      }
//      else if (Arrays.equals(transaction.getReceiveAddress(), internalTx.getReceiveAddress())) {
//        storageDiffListener.merge(program.getStorageDiff());
//      }
    } else {
      // 4. THE FLAG OF SUCCESS IS ONE PUSHED INTO THE STACK
      deposit.commit();
      stackPushOne();
    }

    checkCPULimit("BEFORE CALL");

    // 3. APPLY RESULTS: result.getHReturn() into out_memory allocated
    if (result != null) {
      byte[] buffer = result.getHReturn();
      int offset = msg.getOutDataOffs().intValue();
      int size = msg.getOutDataSize().intValue();

      memorySaveLimited(offset, buffer, size);

      returnDataBuffer = buffer;
    }

    // 5. REFUND THE REMAIN GAS
    if (result != null) {
      BigInteger refundGas = msg.getGas().value().subtract(toBI(result.getDropUsed()));
      if (isPositive(refundGas)) {
        refundGas(refundGas.longValue(), "remaining gas from the internal call");
        if (logger.isInfoEnabled()) {
          logger.info("The remaining gas refunded, account: [{}], gas: [{}] ",
              Hex.toHexString(senderAddress),
              refundGas.toString());
        }
      }
    } else {
      refundGas(msg.getGas().longValue(), "remaining gas from the internal call");
    }

  }

  public void spendDrop(long dropValue, String cause) {
//        if (getDroplimitLong() < dropValue) {
//            throw Exception.notEnoughSpendingGas(cause, dropValue, this);
//        }
    getResult().spendDrop(dropValue);
  }

  public void checkCPULimit(String opName) throws OutOfResourceException {

    if (!Args.getInstance().isDebug()) {
      long vmNowInUs = System.nanoTime() / 1000;
      if (vmNowInUs > getVmShouldEndInUs()) {
        throw Exception.notEnoughCPU(opName);
      }
    }
  }

  public void spendAllGas() {
    spendDrop(getDroplimit().longValue(), "Spending all remaining");
  }

  public void refundGas(long gasValue, String cause) {
    logger.info("[{}] Refund for cause: [{}], gas: [{}]", invoke.hashCode(), cause, gasValue);
    getResult().refundGas(gasValue);
  }

  public void futureRefundGas(long gasValue) {
    logger.info("Future refund added: [{}]", gasValue);
    getResult().addFutureRefund(gasValue);
  }

  public void resetFutureRefund() {
    getResult().resetFutureRefund();
  }

  public void storageSave(DataWord word1, DataWord word2) {
    //storageSave(word1.getData(), word2.getData());
    DataWord keyWord = word1.clone();
    DataWord valWord = word2.clone();
    getStorage().addStorageValue(convertToTronAddress(getOwnerAddress().getLast20Bytes()), keyWord,
        valWord);
  }

    /*
    public void storageSave(byte[] key, byte[] val) {
        DataWord keyWord = new DataWord(key);
        DataWord valWord = new DataWord(val);
        getStorage().addStorageRow(getOwnerAddress().getLast20Bytes(), keyWord, valWord);
    }
    */

  public byte[] getCode() {
    return ops;
  }

  public byte[] getCodeAt(DataWord address) {
    byte[] code = invoke.getDeposit().getCode(convertToTronAddress(address.getLast20Bytes()));
    return nullToEmpty(code);
  }

  public DataWord getOwnerAddress() {
    return invoke.getOwnerAddress().clone();
  }

  public DataWord getBlockHash(int index) {
        /*
        return index < this.getNumber().longValue() && index >= Math.max(256, this.getNumber().intValue()) - 256 ?
                new DataWord(this.invoke.getBlockStore().getBlockHashByNumber(index, getPrevHash().getData())).clone() :
                DataWord.ZERO.clone();
        */
    if (index < this.getNumber().longValue()
        && index >= Math.max(256, this.getNumber().longValue()) - 256) {

      List<BlockCapsule> blocks = this.invoke.getBlockStore().getBlockByLatestNum(1);
      if (CollectionUtils.isNotEmpty(blocks)) {
        BlockCapsule blockCapsule = blocks.get(0);
        return new DataWord(blockCapsule.getBlockId().getBytes());
      } else {
        return DataWord.ZERO.clone();
      }
    } else {
      return DataWord.ZERO.clone();
    }

  }

  public DataWord getBalance(DataWord address) {
    long balance = getStorage().getBalance(convertToTronAddress(address.getLast20Bytes()));
    return new DataWord(balance);
  }

  public DataWord getOriginAddress() {
    return invoke.getOriginAddress().clone();
  }

  public DataWord getCallerAddress() {
    return invoke.getCallerAddress().clone();
  }

  public DataWord getDropPrice() {
    return new DataWord(1);
  }

  public long getDroplimitLong() {
    return invoke.getDroplimitLong() - getResult().getDropUsed();
  }

  public DataWord getDroplimit() {
    return new DataWord(invoke.getDroplimitLong() - getResult().getDropUsed());
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
    DataWord ret = getStorage()
        .getStorageValue(convertToTronAddress(getOwnerAddress().getLast20Bytes()), key.clone());
    return ret == null ? null : ret.clone();
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
    return new DataWord(0); //invoke.getDifficulty().clone();
  }

  public boolean isStaticCall() {
    return invoke.isStaticCall();
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
      logger.trace("\n  Spent Drop: [{}]/[{}]\n  Left Gas:  [{}]\n",
          getResult().getDropUsed(),
          invoke.getDroplimit().longValue(),
          getDroplimit().longValue());

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
      globalOutput.append("\n\n  Spent Gas: ").append(getResult().getDropUsed());

      if (listener != null) {
        listener.output(globalOutput.toString());
      }
    }
  }

  public void saveOpTrace() {
    if (this.pc < ops.length) {
      trace.addOp(ops[pc], pc, getCallDeep(), getDroplimit(), traceListener.resetActions());
    }
  }

  public ProgramTrace getTrace() {
    return trace;
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

  static class ByteCodeIterator {

    byte[] code;
    int pc;

    public ByteCodeIterator(byte[] code) {
      this.code = code;
    }

    public void setPC(int pc) {
      this.pc = pc;
    }

    public int getPC() {
      return pc;
    }

    public OpCode getCurOpcode() {
      return pc < code.length ? OpCode.code(code[pc]) : null;
    }

    public boolean isPush() {
      return getCurOpcode() != null ? getCurOpcode().name().startsWith("PUSH") : false;
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
      if (it.getCurOpcode() == OpCode.JUMP || it.getCurOpcode() == OpCode.RETURN ||
          it.getCurOpcode() == OpCode.STOP) {
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
    BitSet mask = buildReachableBytecodesMask(code);
    String binData = "";

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
      PrecompiledContracts.PrecompiledContract contract)
      throws ContractExeException {
    returnDataBuffer = null; // reset return buffer right before the call

    if (getCallDeep() == MAX_DEPTH) {
      stackPushZero();
      this.refundGas(msg.getGas().longValue(), " call deep limit reach");
      return;
    }

    // Repository track = getStorage().startTracking();
    Deposit deposit = getStorage();

    byte[] senderAddress = convertToTronAddress(this.getOwnerAddress().getLast20Bytes());
    byte[] codeAddress = convertToTronAddress(msg.getCodeAddress().getLast20Bytes());
    byte[] contextAddress = msg.getType().callIsStateless() ? senderAddress : codeAddress;

    long endowment = msg.getEndowment().value().longValue();
    long senderBalance = deposit.getBalance(senderAddress);
    if (senderBalance < endowment) {
      stackPushZero();
      this.refundGas(msg.getGas().longValue(), "refund gas from message call");
      return;
    }

    byte[] data = this.memoryChunk(msg.getInDataOffs().intValue(),
        msg.getInDataSize().intValue());

    // Charge for endowment - is not reversible by rollback
    transfer(deposit, senderAddress, contextAddress, msg.getEndowment().value().longValue());

    long requiredGas = contract.getGasForData(data);
    if (requiredGas > msg.getGas().longValue()) {

      this.refundGas(0, "call pre-compiled"); //matches cpp logic
      this.stackPushZero();
      // deposit.rollback();
    } else {
      // Delegate or not. if is delegated, we will use msg sender, otherwise use contract address
      contract.setCallerAddress(convertToTronAddress(msg.getType().callIsDelegate() ?
          getCallerAddress().getLast20Bytes() : getOwnerAddress().getLast20Bytes()));
      // this is the depositImpl, not storage as above
      contract.setDeposit(this.invoke.getDeposit());
      contract.setResult(this.result);
      Pair<Boolean, byte[]> out = contract.execute(data);

      if (out.getLeft()) { // success
        this.refundGas(msg.getGas().longValue() - requiredGas, "call pre-compiled");
        this.stackPushOne();
        returnDataBuffer = out.getRight();
        deposit.commit();
      } else {
        // spend all gas on failure, push zero and revert state changes
        this.refundGas(0, "call pre-compiled");
        this.stackPushZero();
        // deposit.rollback();
      }

      this.memorySave(msg.getOutDataOffs().intValue(), out.getRight());
    }
  }

  public boolean byTestingSuite() {
    return invoke.byTestingSuite();
  }

  public interface ProgramOutListener {

    void output(String out);
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
  }

  @SuppressWarnings("serial")
  public static class OutOfGasException extends BytecodeExecutionException {

    public OutOfGasException(String message, Object... args) {
      super(format(message, args));
    }
  }

  @SuppressWarnings("serial")
  public static class OutOfResourceException extends BytecodeExecutionException {

    public OutOfResourceException(String message, Object... args) {
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
  public static class StaticCallModificationException extends BytecodeExecutionException {

    public StaticCallModificationException() {
      super("Attempt to call a state modifying opcode inside STATICCALL");
    }
  }

  public static class Exception {

    public static OutOfGasException notEnoughOpGas(OpCode op, long opGas, long programGas) {
      return new OutOfGasException(
          "Not enough gas for '%s' operation executing: opGas[%d], programGas[%d];", op, opGas,
          programGas);
    }

    public static OutOfGasException notEnoughOpGas(OpCode op, DataWord opGas,
        DataWord programGas) {
      return notEnoughOpGas(op, opGas.longValue(), programGas.longValue());
    }


    public static OutOfResourceException notEnoughCPU(String op) {
      return new OutOfResourceException(
          "Not enough CPU resource when '%s' operation executing", op);
    }


    public static OutOfMemoryException memoryOverflow(OpCode op) {
      return new OutOfMemoryException("Out of Memory when '%s' operation executing", op);
    }

    public static OutOfStorageException notEnoughStorage() {
      return new OutOfStorageException("Not enough Storage resource");
    }


    public static OutOfGasException notEnoughOpGas(OpCode op, BigInteger opGas,
        BigInteger programGas) {
      return notEnoughOpGas(op, opGas.longValue(), programGas.longValue());
    }

    public static OutOfGasException notEnoughSpendingGas(String cause, long gasValue,
        Program program) {
      return new OutOfGasException(
          "Not enough gas for '%s' cause spending: invokeGas[%d], gas[%d], usedGas[%d];",
          cause, program.invoke.getDroplimit().longValue(), gasValue,
          program.getResult().getDropUsed());
    }

    public static OutOfGasException gasOverflow(BigInteger actualGas, BigInteger gasLimit) {
      return new OutOfGasException("Gas value overflow: actualGas[%d], gasLimit[%d];",
          actualGas.longValue(), gasLimit.longValue());
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

  /**
   * used mostly for testing reasons
   */
  public byte[] getMemory() {
    return memory.read(0, memory.size());
  }

  /**
   * used mostly for testing reasons
   */
  public void initMem(byte[] data) {
    this.memory.write(0, data, data.length, false);
  }

  public long getVmStartInUs() {
    return this.invoke.getVmStartInUs();
  }

}
