package org.tron.common.runtime2.tvm;

import static org.apache.commons.lang3.ArrayUtils.EMPTY_BYTE_ARRAY;
import static org.apache.commons.lang3.ArrayUtils.getLength;
import static org.apache.commons.lang3.ArrayUtils.isEmpty;
import static org.apache.commons.lang3.ArrayUtils.isNotEmpty;
import static org.apache.commons.lang3.ArrayUtils.nullToEmpty;
import static org.tron.common.utils.BIUtil.isPositive;
import static org.tron.common.utils.BIUtil.toBI;
import static org.tron.common.utils.ByteUtil.stripLeadingZeroes;

import com.google.protobuf.ByteString;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.crypto.Hash;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.runtime.vm.EnergyCost;
import org.tron.common.runtime.vm.MessageCall;
import org.tron.common.runtime.vm.PrecompiledContracts;
import org.tron.common.runtime.vm.program.InternalTransaction;
import org.tron.common.runtime.vm.program.Memory;
import org.tron.common.runtime.vm.program.ProgramResult;
import org.tron.common.runtime.vm.program.Stack;
import org.tron.common.runtime2.config.VMConfig;
import org.tron.common.runtime2.tvm.VMConstant.RefundReasonConstant;
import org.tron.common.storage.Deposit;
import org.tron.common.utils.ByteUtil;
import org.tron.common.utils.FastByteComparisons;
import org.tron.core.Wallet;
import org.tron.core.actuator.TransferActuator;
import org.tron.core.actuator.TransferAssetActuator;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.ContractCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.StoreException;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;

@Slf4j(topic = "VM2")
@Data
public class ContractExecutor {

  private VMConfig vmConfig;
  private byte[] ops;
  private int pc;
  private byte lastOp;
  private byte previouslyExecutedOp;
  private boolean stopped;
  private Stack stack;
  private Memory memory;
  private Deposit storage;
  private ContractContext program;
  private int callDeep = 0;
  private long nonce;
  private byte[] returnDataBuffer;


  public DataWord getContractAddress() {
    return new DataWord(program.getContractAddress());
  }

  public DataWord getOriginAddress() {
    return new DataWord(program.getOrigin());
  }

  public DataWord getCallerAddress() {
    return new DataWord(program.getCallerAddress());
  }

  public DataWord getCallValue() {
    return new DataWord(program.getCallValue());
  }

  public DataWord getTokenValue() {
    return new DataWord(program.getTokenValue());
  }

  public DataWord getTokenId() {
    return new DataWord(program.getTokenId());
  }

  public DataWord getLastHash() {
    return new DataWord(program.getBlockInfo().getLastHash());
  }

  public DataWord getCoinbase() {
    return new DataWord(program.getBlockInfo().getCoinbase());
  }

  public DataWord getTimestamp() {
    return new DataWord(program.getBlockInfo().getTimestamp());
  }

  public DataWord getNumber() {
    return new DataWord(program.getBlockInfo().getNumber());
  }

  public DataWord getDifficulty() {
    return new DataWord(program.getBlockInfo().getDifficulty());
  }


  public ContractExecutor(Deposit deposit, ContractContext program) {
    stopped = false;
    stack = new Stack();
    memory = new Memory();
    storage = deposit;
    ops = program.getOps();
    this.program = program;
    this.nonce = program.getInternalTransaction().getNonce();
  }

  public static ContractExecutor createEnvironment(Deposit deposit, ContractContext program,
      VMConfig vmConfig) {
    ContractExecutor env = new ContractExecutor(deposit, program);
    env.setVmConfig(vmConfig);
    return env;
  }


  public static ContractExecutor createEnvironment(Deposit deposit, ContractContext program,
      ContractExecutor pre) {
    ContractExecutor env = new ContractExecutor(deposit, program);
    if (pre != null) {
      env.callDeep = pre.getCallDeep() + 1;
      env.setVmConfig(pre.getVmConfig());
    }
    return env;
  }


  /**
   *
   * @return
   * @throws ContractValidateException
   */
  public ContractExecutor execute() throws ContractValidateException {
    try {
      //check static call
      preStaticCheck();
      //transfer assets
      transferAssets();
      //processCode
      Interpreter.getInstance().play(this);
      //save code for create
      postProcess();
    } catch (StackOverflowError soe) {
      // if JVM StackOverflow then convert to runtimeExcepton
      setException(ExceptionFactory.jvmStackOverFlow());
    } catch (RuntimeException e) {
      setException(e);
    } catch (ContractValidateException e) {
      // throw ContractValidateException to upper
      throw e;
    } catch (Throwable throwable) {
      setException(ExceptionFactory.unknownThrowable(throwable.getMessage()));
    }
    return this;
  }


  private void preStaticCheck() {
    long tokenValue = program.getTokenValue();
    long callValue = program.getCallValue();
    //checkStaticCall
    if (program.isStatic() && valueGtZero(tokenValue, callValue)) {
      //throw exception and exit.
        throw ExceptionFactory.staticCallTransferException();
    }
  }

  private boolean valueGtZero(long tokenValue, long callValue) {
    return callValue > 0 || tokenValue > 0;
  }

  private void transferAssets()
      throws ContractValidateException {
    byte[] contractAddress = fetchAddress();
    long tokenId = program.getTokenId();
    long tokenValue = program.getTokenValue();
    long callValue = program.getCallValue();

    // tokenid can only be 0
    // or (MIN_TOKEN_ID, Long.Max]
    if (tokenId <= VMConstant.MIN_TOKEN_ID && tokenId != 0) {
      throw new ContractValidateException("tokenId must > " + VMConstant.MIN_TOKEN_ID);
    }
    // tokenid can only be 0 when tokenvalue = 0,
    // or (MIN_TOKEN_ID, Long.Max]
    if (tokenValue > 0 && tokenId == 0) {
      throw new ContractValidateException("invalid arguments with tokenValue = "
          + tokenValue
          + ", tokenId = " + tokenId);
    }

    //transefer Trx and trc10
    // transfer from callerAddress to contractAddress according to callValue
    if (program.getCallValue() > 0) {
      ContractExecutor
          .transfer(this.getStorage(), program.getCallerAddress(), contractAddress, callValue);
    }
    if (program.getTokenValue() > 0) {
      ContractExecutor
          .transferToken(this.getStorage(), program.getCallerAddress(),
              contractAddress, String.valueOf(tokenId), tokenValue);
    }
  }

  private byte[] fetchAddress()
      throws ContractValidateException {
    byte[] contractAddress;
    Protocol.Transaction trx = program.getInternalTransaction().getTransaction();
    if (program.getTrxType() == InternalTransaction.TrxType.TRX_CONTRACT_CREATION_TYPE) {
      //create or create2 OpCode
      if (program.getCallInfo().fromVM) {
        byte[] newAddress = program.getCallInfo().getNewAddress();
        AccountCapsule existingAccount = getStorage().getAccount(newAddress);
        ContractCapsule contractCapsule = null;
        //create Account for create2
        if (existingAccount != null) {
          contractCapsule = getStorage()
              .getContract(existingAccount.getAddress().toByteArray());
        } else {
          this.getStorage().createAccount(newAddress, "CreatedByContract",
              Protocol.AccountType.Contract);
        }
        //judge if is already exist addr
        if (existingAccount != null && contractCapsule != null) {
          throw new ContractValidateException(
              "Trying to create a contract with existing contract address: " + Hex
                  .toHexString(newAddress));
        } else if (existingAccount != null) {
          //update accountInfos
          existingAccount.updateAccountType(Protocol.AccountType.Contract);
          existingAccount.clearDelegatedResource();
          this.getStorage().updateAccount(newAddress, existingAccount);
        }
        Protocol.SmartContract.Builder builder = Protocol.SmartContract.newBuilder();
        builder.setContractAddress(ByteString.copyFrom(newAddress))
            .setConsumeUserResourcePercent(100)
            .setOriginAddress(ByteString.copyFrom(program.getCallerAddress()));
        if (program.getCallInfo().isCreate2) {
          builder.setTrxHash(ByteString.copyFrom(program.getRootTransactionId()));
        }
        Protocol.SmartContract newSmartContract = builder.build();
        this.getStorage().createContract(newAddress, new ContractCapsule(newSmartContract));
        contractAddress = newAddress;


      } else {
        //normal create
        contractAddress = Wallet.generateContractAddress(trx);
        // insure the new contract address haven't exist
        if (this.getStorage().getAccount(contractAddress) != null) {
          throw new ContractValidateException(
              "Trying to create a contract with existing contract address: " + Wallet
                  .encode58Check(contractAddress));
        }
        //if not created by smartcontract then
        Contract.CreateSmartContract contract =
            ContractCapsule.getSmartContractFromTransaction(trx);
        Protocol.SmartContract newSmartContract = contract.getNewContract();
        newSmartContract = newSmartContract.toBuilder()
            .setContractAddress(ByteString.copyFrom(contractAddress)).build();

        this.getStorage().createAccount(contractAddress, newSmartContract.getName(),
            Protocol.AccountType.Contract);

        this.getStorage().createContract(contractAddress, new ContractCapsule(newSmartContract));

      }
      program.setContractAddress(contractAddress);
    } else {
      contractAddress = program.getContractAddress();
    }
    program.getProgramResult().setContractAddress(contractAddress);

    return contractAddress;
  }


  private void setException(RuntimeException soe) {
    program.getProgramResult().setException(soe);
    program.getProgramResult().setRuntimeError(soe.getMessage());
  }

  private void postProcess() {
    ProgramResult result = program.getProgramResult();
    if (program.getTrxType() == InternalTransaction.TrxType.TRX_CONTRACT_CREATION_TYPE
        && !result.isRevert()) {
      byte[] code = program.getProgramResult().getHReturn();
      long saveCodeEnergy = (long) getLength(code) * EnergyCost.getInstance().getCREATE_DATA();
      long afterSpend = program.getEnergyLimitLeft().longValue() - saveCodeEnergy;
      if (afterSpend < 0) {
        throw ExceptionFactory.notEnoughSpendEnergy("saveContractCode",
            saveCodeEnergy, program.getEnergyLimit() - result.getEnergyUsed());
      } else {
        result.spendEnergy(saveCodeEnergy);
        this.getStorage().saveCode(program.getContractAddress(), code);
      }
    }
  }


  public void step() {
    setPC(pc + 1);
  }


  public void stop() {
    stopped = true;
  }

  public void setPC(int pc) {
    this.pc = pc;
  }

  public int getPC() {
    return pc;
  }

  public byte getCurrentOp() {
    return isEmpty(ops) ? 0 : ops[pc];
  }


  public DataWord stackPop() {
    return stack.pop();
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


  public byte[] memoryChunk(int offset, int size) {
    return memory.read(offset, size);
  }

  public void memorySave(DataWord addrB, DataWord value) {
    memory.write(addrB.intValue(), value.getData(), value.getData().length, false);
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

  public void memorySave(int addr, byte[] value) {
    memory.write(addr, value, value.length, false);
  }

  public void memorySaveLimited(int addr, byte[] data, int dataSize) {
    memory.write(addr, data, dataSize, true);
  }

  public DataWord memoryLoad(DataWord addr) {
    return memory.readWord(addr.intValue());
  }

  public DataWord memoryLoad(int address) {
    return memory.readWord(address);
  }

  public void memoryExpand(DataWord outDataOffs, DataWord outDataSize) {
    if (!outDataSize.isZero()) {
      memory.extend(outDataOffs.intValue(), outDataSize.intValue());
    }
  }

  public int getMemSize() {
    return memory.size();
  }

  public void storageSave(DataWord word1, DataWord word2) {
    DataWord keyWord = word1.clone();
    DataWord valWord = word2.clone();
    storage.putStorageValue(convertToTronAddress(getContractAddress().getLast20Bytes()), keyWord,
        valWord);
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


  public static void transfer(Deposit deposit, byte[] fromAddress, byte[] toAddress, long amount)
      throws ContractValidateException {
    if (0 == amount) {
      return;
    }
    TransferActuator.validateForSmartContract(deposit, fromAddress, toAddress, amount);
    deposit.addBalance(toAddress, amount);
    deposit.addBalance(fromAddress, -amount);
  }

  public static void transferAllToken(Deposit deposit, byte[] fromAddress, byte[] toAddress) {
    AccountCapsule fromAccountCap = deposit.getAccount(fromAddress);
    Protocol.Account.Builder fromBuilder = fromAccountCap.getInstance().toBuilder();
    AccountCapsule toAccountCap = deposit.getAccount(toAddress);
    Protocol.Account.Builder toBuilder = toAccountCap.getInstance().toBuilder();
    fromAccountCap.getAssetMapV2().forEach((tokenId, amount) -> {
      toBuilder.putAssetV2(tokenId, toBuilder.getAssetV2Map().getOrDefault(tokenId, 0L) + amount);
      fromBuilder.putAssetV2(tokenId, 0L);
    });
    deposit.putAccountValue(fromAddress, new AccountCapsule(fromBuilder.build()));
    deposit.putAccountValue(toAddress, new AccountCapsule(toBuilder.build()));
  }

  public static void transferToken(Deposit deposit, byte[] fromAddress, byte[] toAddress,
      String tokenId, long amount)
      throws ContractValidateException {
    if (0 == amount) {
      return;
    }
    TransferAssetActuator
        .validateForSmartContract(deposit, fromAddress, toAddress, tokenId.getBytes(), amount);
    deposit.addTokenBalance(toAddress, tokenId.getBytes(), amount);
    deposit.addTokenBalance(fromAddress, tokenId.getBytes(), -amount);
  }

  public static byte[] convertToTronAddress(byte[] address) {
    if (address.length == 20) {
      byte[] newAddress = new byte[21];
      byte[] temp = new byte[]{Wallet.getAddressPreFixByte()};
      System.arraycopy(temp, 0, newAddress, 0, temp.length);
      System.arraycopy(address, 0, newAddress, temp.length, address.length);
      address = newAddress;
    }
    return address;
  }

  /**
   * . Verifies that the stack is at least <code>stackSize</code>
   */
  public void verifyStackSize(int stackSize) {
    if (stack.size() < stackSize) {
      throw ExceptionFactory.tooSmallStack(stackSize, stack.size());
    }
  }

  public void verifyStackOverflow(int argsReqs, int returnReqs) {
    if ((stack.size() - argsReqs + returnReqs) > VMConstant.MAX_STACK_SIZE) {
      throw ExceptionFactory
          .tooLargeStack((stack.size() - argsReqs + returnReqs), VMConstant.MAX_STACK_SIZE);
    }
  }

  public int verifyJumpDest(DataWord nextPC) {
    if (nextPC.bytesOccupied() > 4) {
      throw org.tron.common.runtime.vm.program.Program.Exception.badJumpDestination(-1);
    }
    int ret = nextPC.intValue();
    if (!program.getProgramPrecompile().hasJumpDest(ret)) {
      throw org.tron.common.runtime.vm.program.Program.Exception.badJumpDestination(ret);
    }
    return ret;
  }

  public boolean isDeadAccount(DataWord address) {
    return storage.getAccount(convertToTronAddress(address.getLast20Bytes()))
        == null;
  }

  public DataWord getBalance(DataWord address) {
    long balance = storage.getBalance(convertToTronAddress(address.getLast20Bytes()));
    return new DataWord(balance);
  }

  public DataWord getTokenBalance(DataWord address, DataWord tokenId) {
    // tokenid should not get Long type overflow
    long tokenIdL;
    try {
      tokenIdL = tokenId.sValue().longValueExact();
    } catch (ArithmeticException e) {
      throw ExceptionFactory.tokenInvalid();
    }
    // or tokenId can only be (MIN_TOKEN_ID, Long.Max]
    if (tokenIdL <= VMConstant.MIN_TOKEN_ID) {
      throw ExceptionFactory.tokenInvalid();
    }

    long ret = storage.getTokenBalance(convertToTronAddress(address.getLast20Bytes()),
        String.valueOf(tokenId.longValue()).getBytes());
    return ret == 0 ? new DataWord(0) : new DataWord(ret);
  }


  public DataWord storageLoad(DataWord key) {
    DataWord ret = storage
        .getStorageValue(
            convertToTronAddress(getContractAddress().getLast20Bytes()), key.clone());
    return ret == null ? null : ret.clone();
  }

  public void futureRefundEnergy(long energyValue) {
    logger.debug("Future refund added: [{}]", energyValue);
    program.getProgramResult().addFutureRefund(energyValue);
  }

  public void spendEnergy(long energyValue, String opName) {
    if (program.getEnergylimitLeftLong() < energyValue) {
      throw new org.tron.common.runtime.vm.program.Program.OutOfEnergyException(
          "Not enough energy for '%s' operation executing: curInvokeEnergyLimit[%d],"
              + " curOpEnergy[%d], usedEnergy[%d]",
          opName, program.getEnergyLimit(), energyValue,
          program.getProgramResult().getEnergyUsed());
    }
    program.getProgramResult().spendEnergy(energyValue);
  }

  public void checkCPUTimeLimit(String opName) {
    if (Args.getInstance().isDebug()) {
      return;
    }
    if (Args.getInstance().isSolidityNode()) {
      return;
    }
    long vmNowInUs = System.nanoTime() / 1000;
    if (vmNowInUs > program.getVmShouldEndInUs()) {
      logger.info(
          "minTimeRatio: {}, maxTimeRatio: {}, vm should end time in us: {}, "
              + "vm now time in us: {}, vm start time in us: {}",
          Args.getInstance().getMinTimeRatio(), Args.getInstance().getMaxTimeRatio(),
          program.getVmShouldEndInUs(), vmNowInUs, program.getVmStartInUs());
      throw org.tron.common.runtime.vm.program.Program.Exception.notEnoughTime(opName);
    }

  }


  public DataWord getCallEnergy(DataWord requestedEnergy, DataWord availableEnergy) {
    return requestedEnergy.compareTo(availableEnergy) > 0 ? availableEnergy : requestedEnergy;
  }

  public void spendAllEnergy() {
    spendEnergy(program.getEnergylimitLeftLong(), "Spending all remaining");
  }


  public void resetFutureRefund() {
    program.getProgramResult().resetFutureRefund();
  }


  public DataWord isContract(DataWord address) {
    ContractCapsule contract = storage
        .getContract(convertToTronAddress(address.getLast20Bytes()));
    return contract != null ? new DataWord(1) : new DataWord(0);
  }

  /*     CALLDATALOAD  op   */
  public DataWord getDataValue(DataWord indexData) {
    byte[] msgData = program.getMsgData();
    BigInteger tempIndex = indexData.value();
    int index = tempIndex.intValue(); // possible overflow is caught below
    int size = 32; // maximum datavalue size

    if (msgData == null || index >= msgData.length
        || tempIndex.compareTo(VMConfig.MAX_MSG_DATA) > 0) {
      return new DataWord();
    }
    if (index + size > msgData.length) {
      size = msgData.length - index;
    }

    byte[] data = new byte[32];
    System.arraycopy(msgData, index, data, 0, size);
    return new DataWord(data);
  }


  public DataWord getDataSize() {
    if (program.getMsgData() == null || program.getMsgData().length == 0) {
      return DataWord.ZERO.clone();
    }
    int size = program.getMsgData().length;
    return new DataWord(size);
  }

  /*  CALLDATACOPY */
  public byte[] getDataCopy(DataWord offsetData, DataWord lengthData) {

    int offset = offsetData.intValueSafe();
    int length = lengthData.intValueSafe();

    byte[] data = new byte[length];

    if (program.getMsgData() == null) {
      return data;
    }
    if (offset > program.getMsgData().length) {
      return data;
    }
    if (offset + length > program.getMsgData().length) {
      length = program.getMsgData().length - offset;
    }

    System.arraycopy(program.getMsgData(), offset, data, 0, length);

    return data;
  }


  public DataWord getReturnDataBufferSize() {
    return new DataWord(getReturnDataBufferSizeI());
  }

  private int getReturnDataBufferSizeI() {
    return returnDataBuffer == null ? 0 : returnDataBuffer.length;
  }

  public byte[] getReturnDataBufferData(DataWord off, DataWord size) {
    if ((long) off.intValueSafe() + size.intValueSafe() > getReturnDataBufferSizeI()) {
      throw new org.tron.common.runtime.vm.program.Program
          .ReturnDataCopyIllegalBoundsException(off, size,
          getReturnDataBufferSize().longValueSafe());
    }
    return returnDataBuffer == null ? new byte[0] :
        Arrays.copyOfRange(returnDataBuffer, off.intValueSafe(),
            off.intValueSafe() + size.intValueSafe());
  }


  public byte[] getCodeAt(DataWord address) {
    byte[] code = storage.getCode(convertToTronAddress(address.getLast20Bytes()));
    return nullToEmpty(code);
  }

  public byte[] getCodeHashAt(DataWord address) {
    byte[] tronAddr = convertToTronAddress(address.getLast20Bytes());
    AccountCapsule account = storage.getAccount(tronAddr);
    if (account != null) {
      ContractCapsule contract = storage.getContract(tronAddr);
      byte[] codeHash;
      if (contract != null) {
        codeHash = contract.getCodeHash();
        if (ByteUtil.isNullOrZeroArray(codeHash)) {
          byte[] code = getCodeAt(address);
          codeHash = Hash.sha3(code);
          contract.setCodeHash(codeHash);
          storage.updateContract(tronAddr, contract);
        }
      } else {
        codeHash = Hash.sha3(new byte[0]);
      }
      return codeHash;
    } else {
      return EMPTY_BYTE_ARRAY;
    }
  }

  public DataWord getBlockHash(int index) {
    if (index < program.getBlockInfo().getNumber()
        && index >= Math.max(256, program.getBlockInfo().getNumber()) - 256) {

      BlockCapsule blockCapsule = getBlockByNum(index);

      if (Objects.nonNull(blockCapsule)) {
        return new DataWord(blockCapsule.getBlockId().getBytes());
      } else {
        return DataWord.ZERO.clone();
      }
    } else {
      return DataWord.ZERO.clone();
    }

  }

  public BlockCapsule getBlockByNum(int index) {
    try {
      return storage.getDbManager().getBlockByNum(index);
    } catch (StoreException e) {
      throw new org.tron.common.runtime.vm.program.Program
          .IllegalOperationException("cannot find block num");
    }
  }

  public void increaseNonce() {
    nonce++;
  }

  /**
   * @param transferAddress the address send trx to.
   * @param value the trx value transferred in the internaltransaction
   */
  private InternalTransaction addInternalTx(byte[] senderAddress,
      byte[] transferAddress, long value, byte[] data,
      String note, long nonce, Map<String, Long> tokenInfo) {

    InternalTransaction addedInternalTx = null;
    if (program.getInternalTransaction() != null) {
      addedInternalTx = program.getProgramResult()
          .addInternalTransaction(program.getInternalTransaction().getHash(), getCallDeep(),
              senderAddress, transferAddress, value, data, note, nonce, tokenInfo);
    }

    return addedInternalTx;
  }


  public void suicide(DataWord obtainerAddress) {

    byte[] owner = convertToTronAddress(getContractAddress().getLast20Bytes());
    byte[] obtainer = convertToTronAddress(obtainerAddress.getLast20Bytes());
    long balance = storage.getBalance(owner);

    if (logger.isDebugEnabled()) {
      logger.debug("Transfer to: [{}] heritage: [{}]",
          Hex.toHexString(obtainer),
          balance);
    }

    increaseNonce();

    addInternalTx(owner, obtainer, balance, null, "suicide", nonce,
        getStorage().getAccount(owner).getAssetMapV2());

    if (FastByteComparisons.compareTo(owner, 0, 20, obtainer, 0, 20) == 0) {
      // if owner == obtainer just zeroing account according to Yellow Paper
      storage.addBalance(owner, -balance);
      byte[] blackHoleAddress = storage.getBlackHoleAddress();
      storage.addBalance(blackHoleAddress, balance);
      transferAllToken(storage, owner, blackHoleAddress);
    } else {
      try {
        transfer(storage, owner, obtainer, balance);
        transferAllToken(storage, owner, obtainer);
      } catch (ContractValidateException e) {
        throw ExceptionFactory.transferSuicideAllTokenException(e.getMessage());
      }
    }
    program.getProgramResult().addDeleteAccount(this.getContractAddress());
  }


  public void createContract(DataWord value, DataWord memStart, DataWord memSize) {
    createContract(value, memStart, memSize, null, false);
  }

  public void createContract(DataWord value, DataWord memStart,
      DataWord memSize, DataWord salt, boolean isCreate2) {
    returnDataBuffer = null; // reset return buffer right before the call
    long endowment = value.value().longValueExact();
    byte[] senderAddress = convertToTronAddress(this.getContractAddress().getLast20Bytes());

    if (getCallDeep() == VMConstant.MAX_CALLDEEP_DEPTH) {
      stackPushZero();
      return;
    }
    //simply judgement of if balance sufficient
    if (getStorage().getBalance(senderAddress) < endowment) {
      stackPushZero();
      return;
    }

    // [1] fenth memory code
    byte[] programCode = memoryChunk(memStart.intValue(), memSize.intValue());
    // [2] generate new address
    byte[] newAddress;
    if (isCreate2) {
      newAddress = Wallet
          .generateContractAddress2(
              convertToTronAddress(getCallerAddress().getLast20Bytes()), salt.getData(),
              programCode);
    } else {
      newAddress = Wallet
          .generateContractAddress(program.getRootTransactionId(), nonce);
    }

    if (logger.isDebugEnabled()) {
      logger.debug("creating a new contract inside contract run: [{}]",
          Hex.toHexString(senderAddress));
    }

    // [3] create Program
    // actual energy subtract
    DataWord energyLimit = program.getEnergyLimitLeft();
    spendEnergy(energyLimit.longValue(), "internal call");
    // add internalTx
    increaseNonce();
    InternalTransaction internalTx = addInternalTx(senderAddress, newAddress, endowment,
        programCode, "create", nonce, null);
    long vmStartInUs = System.nanoTime() / 1000;

    ContractContext create = new ContractContext();
    create.setInternalTransaction(internalTx);
    create.setTrxType(InternalTransaction.TrxType.TRX_CONTRACT_CREATION_TYPE);
    create.setCallValue(endowment);
    create.setCallerAddress(senderAddress);
    create.setOps(programCode);
    create.setOrigin(program.getOrigin());
    create.setMsgData(ByteUtil.EMPTY_BYTE_ARRAY);
    create.setEnergyLimit(energyLimit.longValue());

    create.setVmStartInUs(vmStartInUs);
    create.setVmShouldEndInUs(program.getVmShouldEndInUs());
    create.setStatic(false);

    create.getCallInfo().setFromVM(true);
    create.getCallInfo().setCreate2(isCreate2);
    create.getCallInfo().setNewAddress(newAddress);

    create.setBlockInfo(program.getBlockInfo());

    create.setRootTransactionId(program.getRootTransactionId());

    //[4]execute

    //setup program environment
    ContractExecutor cenv = ContractExecutor
        .createEnvironment(this.getStorage().newDepositChild(), create, this);
    //play program
    try {
      cenv.execute();
    } catch (ContractValidateException e) {
      //frankly, is the transfer reason
      throw ExceptionFactory.transferException("create opcode validateForSmartContract failure");
    }
    // always commit nonce
    this.nonce = cenv.nonce;

    ProgramResult createResult = create.getProgramResult();

    //deal with result

    program.getProgramResult().merge(createResult);

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
      cenv.getStorage().commit();
      //success then stackpush new address
      stackPush(new DataWord(newAddress));
    }
    // 5. REFUND THE REMAIN Energy
    refundEnergyAfterVM(energyLimit, createResult);

  }


  public void callToPrecompiledAddress(MessageCall msg,
      PrecompiledContracts.PrecompiledContract contract) {
    returnDataBuffer = null; // reset return buffer right before the call

    if (getCallDeep() == VMConstant.MAX_CALLDEEP_DEPTH) {
      stackPushZero();
      this.refundEnergy(msg.getEnergy().longValue(), " call deep limit reach");
      return;
    }

    Deposit deposit = storage.newDepositChild();

    byte[] senderAddress = convertToTronAddress(this.getContractAddress().getLast20Bytes());
    byte[] codeAddress = convertToTronAddress(msg.getCodeAddress().getLast20Bytes());
    byte[] contextAddress = msg.getType().callIsStateless() ? senderAddress : codeAddress;

    long endowment = msg.getEndowment().value().longValueExact();
    long senderBalance = 0;
    byte[] tokenId = null;

    checkTokenId(msg);
    boolean isTokenTransfer = msg.isTokenTransferMsg();
    // transfer trx validation
    if (!isTokenTransfer) {
      senderBalance = deposit.getBalance(senderAddress);
    } else {
      // transfer trc10 token validation
      tokenId = String.valueOf(msg.getTokenId().longValue()).getBytes();
      senderBalance = deposit.getTokenBalance(senderAddress, tokenId);
    }
    if (senderBalance < endowment) {
      stackPushZero();
      refundEnergy(msg.getEnergy().longValue(), RefundReasonConstant.FROM_MSG_CALL);
      return;
    }
    byte[] data = this.memoryChunk(msg.getInDataOffs().intValue(),
        msg.getInDataSize().intValue());

    // Charge for endowment - is not reversible by rollback
    if (!ArrayUtils.isEmpty(senderAddress) && !ArrayUtils.isEmpty(contextAddress)
        && senderAddress != contextAddress && msg.getEndowment().value().longValueExact() > 0) {
      if (!isTokenTransfer) {
        try {
          transfer(deposit, senderAddress, contextAddress,
              msg.getEndowment().value().longValueExact());
        } catch (ContractValidateException e) {
          throw new org.tron.common.runtime.vm.program.Program
              .BytecodeExecutionException("transfer failure");
        }
      } else {
        try {
          TransferAssetActuator
              .validateForSmartContract(
                  deposit, senderAddress, contextAddress, tokenId, endowment);
        } catch (ContractValidateException e) {
          throw new org.tron.common.runtime.vm.program.Program
              .BytecodeExecutionException(ExceptionFactory.VALIDATE_FOR_SMART_CONTRACT_FAILURE,
              e.getMessage());
        }
        deposit.addTokenBalance(senderAddress, tokenId, -endowment);
        deposit.addTokenBalance(contextAddress, tokenId, endowment);
      }
    }

    long requiredEnergy = contract.getEnergyForData(data);
    if (requiredEnergy > msg.getEnergy().longValue()) {
      // Not need to throw an exception, method caller needn't know that
      // regard as consumed the energy
      this.refundEnergy(0, RefundReasonConstant.CALL_PRECOMPILED); //matches cpp logic
      this.stackPushZero();
    } else {
      // Delegate or not. if is delegated, we will use msg sender, otherwise use contract address
      contract.setCallerAddress(convertToTronAddress(msg.getType().callIsDelegate()
          ? getCallerAddress().getLast20Bytes() : getContractAddress().getLast20Bytes()));
      // this is the depositImpl, not contractState as above
      contract.setDeposit(deposit);
      contract.setResult(program.getProgramResult());
      contract.setStaticCall(program.isStatic());
      Pair<Boolean, byte[]> out = contract.execute(data);

      if (out.getLeft()) { // success
        this.refundEnergy(msg.getEnergy().longValue() - requiredEnergy,
            RefundReasonConstant.CALL_PRECOMPILED);
        this.stackPushOne();
        returnDataBuffer = out.getRight();
        deposit.commit();
      } else {
        // spend all energy on failure, push zero and revert state changes
        this.refundEnergy(0, RefundReasonConstant.CALL_PRECOMPILED);
        this.stackPushZero();
        if (Objects.nonNull(program.getProgramResult().getException())) {
          throw program.getProgramResult().getException();
        }
      }

      this.memorySave(msg.getOutDataOffs().intValue(), out.getRight());
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
    if (getCallDeep() == VMConstant.MAX_CALLDEEP_DEPTH) {
      stackPushZero();
      refundEnergy(msg.getEnergy().longValue(), " call deep limit reach");
      return;
    }
    // FETCH THE SAVED STORAGE
    byte[] codeAddress = convertToTronAddress(msg.getCodeAddress().getLast20Bytes());
    byte[] senderAddress = convertToTronAddress(getContractAddress().getLast20Bytes());
    byte[] contextAddress = msg.getType().callIsStateless() ? senderAddress : codeAddress;
    byte[] tokenId = String.valueOf(msg.getTokenId().longValue()).getBytes();
    //PERFORM THE VALUE (endowment) PART
    long endowment;
    try {
      endowment = msg.getEndowment().value().longValueExact();
    } catch (ArithmeticException e) {
      refundEnergy(msg.getEnergy().longValue(), "endowment out of long range");
      throw ExceptionFactory.transferException("endowment out of long range");
    }
    Deposit childStroage = getStorage().newDepositChild();

    // transfer trx validation
    boolean isTokenTransfer = msg.isTokenTransferMsg();
    // if not suffcient then stack push zero (need optimized)
    if (!isTokenTransfer) {
      long senderBalance = childStroage.getBalance(senderAddress);
      if (senderBalance < endowment) {
        stackPushZero();
        refundEnergy(msg.getEnergy().longValue(), RefundReasonConstant.FROM_MSG_CALL);
        return;
      }
    } else {
      // transfer trc10 token validation
      tokenId = String.valueOf(msg.getTokenId().longValue()).getBytes();
      long senderBalance = childStroage.getTokenBalance(senderAddress, tokenId);
      if (senderBalance < endowment) {
        stackPushZero();
        refundEnergy(msg.getEnergy().longValue(), RefundReasonConstant.FROM_MSG_CALL);
        return;
      }
    }

    byte[] data = memoryChunk(msg.getInDataOffs().intValue(), msg.getInDataSize().intValue());

    // CREATE CALL INTERNAL TRANSACTION
    increaseNonce();
    HashMap<String, Long> tokenInfo = new HashMap<>();
    if (isTokenTransfer) {
      tokenInfo.put(new String(stripLeadingZeroes(tokenId)), endowment);
    }
    InternalTransaction internalTx = addInternalTx(senderAddress, contextAddress,
        !isTokenTransfer ? endowment : 0, data, "call", nonce,
        !isTokenTransfer ? null : tokenInfo);
    ProgramResult callResult = null;

    // FETCH THE CODE
    AccountCapsule accountCapsule = getStorage().getAccount(codeAddress);
    byte[] programCode =
        accountCapsule != null ? getStorage().getCode(codeAddress) : EMPTY_BYTE_ARRAY;

    long vmStartInUs = System.nanoTime() / 1000;
    DataWord callValue = msg.getType().callIsDelegate() ? getCallValue() : msg.getEndowment();

    ContractContext call = new ContractContext();
    call.setInternalTransaction(internalTx);
    call.setTrxType(InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE);
    call.setCallValue(isTokenTransfer ? 0 : callValue.longValueSafe());
    call.setTokenValue(isTokenTransfer ? callValue.longValueSafe() : 0);
    call.setTokenId(isTokenTransfer ? msg.getTokenId().longValueSafe() : 0);
    call.setCallerAddress(
        msg.getType().callIsDelegate() ? convertToTronAddress(getCallerAddress().getLast20Bytes())
            : senderAddress);
    call.setOps(programCode);
    call.setOrigin(program.getOrigin());
    call.setMsgData(data);
    call.setEnergyLimit(msg.getEnergy().longValueSafe());
    call.setVmStartInUs(vmStartInUs);
    call.setVmShouldEndInUs(program.getVmShouldEndInUs());
    call.setStatic(program.isStatic() || msg.getType().callIsStatic());
    call.setContractAddress(contextAddress);
    call.getCallInfo().setFromVM(true);
    call.setBlockInfo(program.getBlockInfo());
    call.setRootTransactionId(program.getRootTransactionId());

    ContractExecutor cenv = createEnvironment(childStroage, call, this);
    try {
      cenv.execute();
    } catch (ContractValidateException e) {
      refundEnergy(msg.getEnergy().longValue(), RefundReasonConstant.FROM_MSG_CALL);
      throw ExceptionFactory.transferException(e.getMessage());
    }
    if (isNotEmpty(programCode)) {
      callResult = call.getProgramResult();

      program.getProgramResult().merge(callResult);
      // always commit nonce
      this.nonce = cenv.nonce;

      if (callResult.getException() != null || callResult.isRevert()) {
        logger.debug("contract run halted by Exception: contract: [{}], exception: [{}]",
            Hex.toHexString(contextAddress),
            callResult.getException());
        if (internalTx != null) {
          internalTx.reject();
        }
        callResult.rejectInternalTransactions();

        stackPushZero();

        if (callResult.getException() != null) {
          return;
        }
      } else {
        // 4. THE FLAG OF SUCCESS IS ONE PUSHED INTO THE STACK
        childStroage.commit();
        stackPushOne();
      }
    } else {
      // 4. THE FLAG OF SUCCESS IS ONE PUSHED INTO THE STACK
      childStroage.commit();
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
      BigInteger refundEnergy = msg.getEnergy().value().subtract(toBI(callResult.getEnergyUsed()));
      if (isPositive(refundEnergy)) {
        refundEnergy(refundEnergy.longValueExact(), "remaining energy from the internal call");
        if (logger.isDebugEnabled()) {
          logger.debug("The remaining energy refunded, account: [{}], energy: [{}] ",
              Hex.toHexString(senderAddress),
              refundEnergy.toString());
        }
      }
    } else {
      refundEnergy(msg.getEnergy().longValue(), "remaining esnergy from the internal call");
    }
  }


  public void refundEnergyAfterVM(DataWord energyLimit, ProgramResult result) {

    long refundEnergy = energyLimit.longValueSafe() - result.getEnergyUsed();
    if (refundEnergy > 0) {
      refundEnergy(refundEnergy, "remain energy from the internal call");
      if (logger.isDebugEnabled()) {
        logger.debug("The remaining energy is refunded, account: [{}], energy: [{}] ",
            Hex.toHexString(convertToTronAddress(getContractAddress().getLast20Bytes())),
            refundEnergy);
      }
    }
  }

  public void refundEnergy(long energyValue, String cause) {
    logger.debug("[{}] Refund for cause: [{}], energy: [{}]",
        program.hashCode(), cause, energyValue);
    program.getProgramResult().refundEnergy(energyValue);
  }


  /**
   * check TokenId TokenId  \ isTransferToken -----------------------------------------------------
   * false                                     true -----------------------------------------------
   * (-∞,Long.Min)        Not possible            error: msg.getTokenId().value().longValueExact()
   * ---------------------------------------------------------------------------------------------
   * [Long.Min, 0)        Not possible                               error
   * --------------------------------------------------------------------------------------------- 0
   * allowed and only allowed                    error (guaranteed in CALLTOKEN) transfertoken id=0
   * should not transfer trx） ---------------------------------------------------------------------
   * (0-100_0000]          Not possible                              error
   * ---------------------------------------------------------------------------------------------
   * (100_0000, Long.Max]  Not possible                             allowed
   * ---------------------------------------------------------------------------------------------
   * (Long.Max,+∞)         Not possible          error: msg.getTokenId().value().longValueExact()
   * ---------------------------------------------------------------------------------------------
   */
  public void checkTokenId(MessageCall msg) {
    // tokenid should not get Long type overflow
    long tokenId;
    try {
      tokenId = msg.getTokenId().sValue().longValueExact();
    } catch (ArithmeticException e) {
      refundEnergy(msg.getEnergy().longValue(), RefundReasonConstant.FROM_MSG_CALL);
      throw ExceptionFactory.tokenInvalid();
    }
    // tokenId can only be 0 when isTokenTransferMsg == false
    // or tokenId can be (MIN_TOKEN_ID, Long.Max] when isTokenTransferMsg == true
    if ((tokenId <= VMConstant.MIN_TOKEN_ID && tokenId != 0)
        || (tokenId == 0 && msg.isTokenTransferMsg())) {
      // tokenId == 0 is a default value for token id DataWord.
      refundEnergy(msg.getEnergy().longValue(), RefundReasonConstant.FROM_MSG_CALL);
      throw ExceptionFactory.tokenInvalid();
    }

  }

}
