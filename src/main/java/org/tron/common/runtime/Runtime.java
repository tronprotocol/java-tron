package org.tron.common.runtime;

import static com.google.common.primitives.Longs.max;
import static com.google.common.primitives.Longs.min;
import static org.apache.commons.lang3.ArrayUtils.isEmpty;
import static org.tron.common.runtime.utils.MUtil.transfer;
import static org.tron.common.runtime.vm.program.InternalTransaction.ExecutorType.ET_CONSTANT_TYPE;
import static org.tron.common.runtime.vm.program.InternalTransaction.ExecutorType.ET_NORMAL_TYPE;
import static org.tron.common.runtime.vm.program.InternalTransaction.ExecutorType.ET_PRE_TYPE;
import static org.tron.common.runtime.vm.program.InternalTransaction.ExecutorType.ET_UNKNOWN_TYPE;
import static org.tron.common.runtime.vm.program.InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE;
import static org.tron.common.runtime.vm.program.InternalTransaction.TrxType.TRX_CONTRACT_CREATION_TYPE;
import static org.tron.common.runtime.vm.program.InternalTransaction.TrxType.TRX_PRECOMPILED_TYPE;
import static org.tron.common.runtime.vm.program.InternalTransaction.TrxType.TRX_UNKNOWN_TYPE;

import com.google.protobuf.ByteString;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.tron.common.runtime.config.SystemProperties;
import org.tron.common.runtime.vm.PrecompiledContracts;
import org.tron.common.runtime.vm.VM;
import org.tron.common.runtime.vm.program.InternalTransaction;
import org.tron.common.runtime.vm.program.InternalTransaction.ExecutorType;
import org.tron.common.runtime.vm.program.Program;
import org.tron.common.runtime.vm.program.Program.OutOfResourceException;
import org.tron.common.runtime.vm.program.ProgramPrecompile;
import org.tron.common.runtime.vm.program.ProgramResult;
import org.tron.common.runtime.vm.program.invoke.ProgramInvoke;
import org.tron.common.runtime.vm.program.invoke.ProgramInvokeFactory;
import org.tron.common.storage.Deposit;
import org.tron.common.storage.DepositImpl;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.actuator.Actuator;
import org.tron.core.actuator.ActuatorFactory;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.capsule.ContractCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.Parameter.ChainConstant;
import org.tron.core.db.CpuProcessor;
import org.tron.core.db.StorageMarket;
import org.tron.core.db.TransactionTrace;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.OutOfSlotTimeException;
import org.tron.protos.Contract;
import org.tron.protos.Contract.CreateSmartContract;
import org.tron.protos.Contract.TriggerSmartContract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.SmartContract;
import org.tron.protos.Protocol.SmartContract.ABI;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;


/**
 * @author Guo Yonggang
 * @since 28.04.2018
 */
@Slf4j(topic = "Runtime")
public class Runtime {


  SystemProperties config;

  private Transaction trx;
  private Block block = null;
  private Deposit deposit;
  private ProgramInvokeFactory programInvokeFactory = null;
  private String runtimeError;
  private boolean readyToExecute = false;

  private CpuProcessor cpuProcessor = null;
  private StorageMarket storageMarket = null;
  PrecompiledContracts.PrecompiledContract precompiledContract = null;
  private ProgramResult result = new ProgramResult();


  private VM vm = null;
  private Program program = null;

  private InternalTransaction.TrxType trxType = TRX_UNKNOWN_TYPE;
  private ExecutorType executorType = ET_UNKNOWN_TYPE;

  //tx trace
  private TransactionTrace trace;


  /**
   * For block's trx run
   */
  public Runtime(TransactionTrace trace, Block block, Deposit deosit,
      ProgramInvokeFactory programInvokeFactory) {
    this.trace = trace;
    this.trx = trace.getTrx().getInstance();

    if (Objects.nonNull(block)) {
      this.block = block;
      this.executorType = ET_NORMAL_TYPE;
    } else {
      this.block = Block.newBuilder().build();
      this.executorType = ET_PRE_TYPE;
    }
    this.deposit = deosit;
    this.programInvokeFactory = programInvokeFactory;
    this.cpuProcessor = new CpuProcessor(deposit.getDbManager());
    this.storageMarket = new StorageMarket(deposit.getDbManager());

    Transaction.Contract.ContractType contractType = this.trx.getRawData().getContract(0).getType();
    switch (contractType.getNumber()) {
      case ContractType.TriggerSmartContract_VALUE:
        trxType = TRX_CONTRACT_CALL_TYPE;
        break;
      case ContractType.CreateSmartContract_VALUE:
        trxType = TRX_CONTRACT_CREATION_TYPE;
        break;
      default:
        trxType = TRX_PRECOMPILED_TYPE;
    }
  }

  /**
   * For pre trx run
   */
  @Deprecated
  public Runtime(Transaction tx, DepositImpl deposit, ProgramInvokeFactory programInvokeFactory) {
    this.trx = tx;
    this.deposit = deposit;
    this.programInvokeFactory = programInvokeFactory;
    this.executorType = ET_PRE_TYPE;
    Transaction.Contract.ContractType contractType = tx.getRawData().getContract(0).getType();
    switch (contractType.getNumber()) {
      case Transaction.Contract.ContractType.TriggerSmartContract_VALUE:
        trxType = TRX_CONTRACT_CALL_TYPE;
        break;
      case Transaction.Contract.ContractType.CreateSmartContract_VALUE:
        trxType = TRX_CONTRACT_CREATION_TYPE;
        break;
      default:
        trxType = TRX_PRECOMPILED_TYPE;

    }
  }

  /**
   * For constant trx
   */
  @Deprecated
  public Runtime(Transaction tx, ProgramInvokeFactory programInvokeFactory, Deposit deposit) {
    trx = tx;
    this.deposit = deposit;
    this.programInvokeFactory = programInvokeFactory;
    executorType = ET_CONSTANT_TYPE;
    trxType = TRX_CONTRACT_CALL_TYPE;

  }


  /**
   * For constant trx with latest block.
   */
  public Runtime(Transaction tx, Block block, DepositImpl deposit,
      ProgramInvokeFactory programInvokeFactory) {
    this.trx = tx;
    this.deposit = deposit;
    this.programInvokeFactory = programInvokeFactory;
    this.executorType = ET_PRE_TYPE;
    this.block = block;
    this.cpuProcessor = new CpuProcessor(deposit.getDbManager());
    this.storageMarket = new StorageMarket(deposit.getDbManager());
    Transaction.Contract.ContractType contractType = tx.getRawData().getContract(0).getType();
    switch (contractType.getNumber()) {
      case Transaction.Contract.ContractType.TriggerSmartContract_VALUE:
        trxType = TRX_CONTRACT_CALL_TYPE;
        break;
      case Transaction.Contract.ContractType.CreateSmartContract_VALUE:
        trxType = TRX_CONTRACT_CREATION_TYPE;
        break;
      default:
        trxType = TRX_PRECOMPILED_TYPE;
    }
  }


  public void precompiled() throws ContractValidateException, ContractExeException {
    TransactionCapsule trxCap = new TransactionCapsule(trx);
    final List<Actuator> actuatorList = ActuatorFactory
        .createActuator(trxCap, deposit.getDbManager());

    for (Actuator act : actuatorList) {
      act.validate();
      act.execute(result.getRet());
    }
  }

  /**
   */
  public void init() {
    readyToExecute = true;
    // switch (trxType) {
    //   case TRX_PRECOMPILED_TYPE:
    //     readyToExecute = true;
    //     break;
    //   case TRX_CONTRACT_CREATION_TYPE:
    //   case TRX_CONTRACT_CALL_TYPE:
    //     // if (!curCPULimitReachedBlockCPULimit()) {
    //     //   readyToExecute = true;
    //     // }
    //     readyToExecute = true;
    //     break;
    //   default:
    //     readyToExecute = true;
    //     break;
    // }
  }


  public BigInteger getBlockCPULeftInUs() {

    // insure block is not null
    BigInteger curBlockHaveElapsedCPUInUs =
        BigInteger.valueOf(
            1000 * (DateTime.now().getMillis() - block.getBlockHeader().getRawData()
                .getTimestamp())); // us
    BigInteger curBlockCPULimitInUs = BigInteger.valueOf((long)
        (1000 * ChainConstant.BLOCK_PRODUCED_INTERVAL * 0.5
            * ChainConstant.BLOCK_PRODUCED_TIME_OUT
            / 100)); // us

    return curBlockCPULimitInUs.subtract(curBlockHaveElapsedCPUInUs);

  }

  public boolean curCPULimitReachedBlockCPULimit() {

    if (executorType == ET_NORMAL_TYPE) {
      BigInteger blockCPULeftInUs = getBlockCPULeftInUs();
      BigInteger oneTxCPULimitInUs = BigInteger
          .valueOf(Constant.MAX_CPU_TIME_OF_ONE_TX);

      // TODO get from account
      BigInteger increasedStorageLimit = BigInteger.valueOf(10000000);

      boolean cumulativeCPUReached =
          oneTxCPULimitInUs.compareTo(blockCPULeftInUs) > 0;

      if (cumulativeCPUReached) {
        logger.error("cumulative CPU Reached");
        return true;
      }
    }

    return false;
  }

  private long getAccountCPULimitInUs(AccountCapsule account,
      long limitInDrop, long maxCpuInUsByAccount) {

    CpuProcessor cpuProcessor = new CpuProcessor(this.deposit.getDbManager());
    long cpuInUsFromFreeze = cpuProcessor.getAccountLeftCpuInUsFromFreeze(account);

    long cpuInUsFromDrop = Math.floorDiv(limitInDrop, Constant.SUN_PER_GAS);

    return min(maxCpuInUsByAccount, max(cpuInUsFromFreeze, cpuInUsFromDrop)); // us

  }

  private long getAccountCPULimitInUsByPercent(AccountCapsule creator, AccountCapsule sender,
      TriggerSmartContract contract, long maxCpuInUsBySender, long limitInDrop) {

    long senderCpuLimit = getAccountCPULimitInUs(sender, limitInDrop,
        maxCpuInUsBySender);
    if (Arrays.equals(creator.getAddress().toByteArray(), sender.getAddress().toByteArray())) {
      return senderCpuLimit;
    }

    CpuProcessor cpuProcessor = new CpuProcessor(this.deposit.getDbManager());
    long creatorCpuFromFrozen = cpuProcessor.getAccountLeftCpuInUsFromFreeze(creator);

    SmartContract smartContract = this.deposit
        .getContract(contract.getContractAddress().toByteArray()).getInstance();
    double consumeUserResourcePercent = smartContract.getConsumeUserResourcePercent() * 1.0 / 100;

    if (consumeUserResourcePercent >= 1.0) {
      consumeUserResourcePercent = 1.0;
    }
    if (consumeUserResourcePercent <= 0.0) {
      consumeUserResourcePercent = 0.0;
    }

    if (consumeUserResourcePercent <= 0.0) {
      return creatorCpuFromFrozen;
    }

    if (creatorCpuFromFrozen * consumeUserResourcePercent
        >= (1 - consumeUserResourcePercent) * senderCpuLimit) {
      return (long) (senderCpuLimit / consumeUserResourcePercent);
    } else {
      return Math.addExact(senderCpuLimit, creatorCpuFromFrozen);
    }
  }

  public void execute() throws ContractValidateException, ContractExeException {

    if (!readyToExecute) {
      return;
    }
    switch (trxType) {
      case TRX_PRECOMPILED_TYPE:
        precompiled();
        break;
      case TRX_CONTRACT_CREATION_TYPE:
        create();
        break;
      case TRX_CONTRACT_CALL_TYPE:
        call();
        break;
      default:
        break;
    }
  }

  private long getGasLimit(AccountCapsule account, long feeLimit) {

    // will change the name from us to gas
    // can change the calc way
    long cpuGasFromFreeze = cpuProcessor.getAccountLeftCpuInUsFromFreeze(account);
    long cpuGasFromBalance = Math.floorDiv(account.getBalance(), Constant.SUN_PER_GAS);

    long cpuGasFromFeeLimit;
    long balanceForCpuFreeze = account.getAccountResource().getFrozenBalanceForCpu()
        .getFrozenBalance();
    if (0 == balanceForCpuFreeze) {
      cpuGasFromFeeLimit = feeLimit / Constant.SUN_PER_GAS;
    } else {
      long totalCpuGasFromFreeze = cpuProcessor.calculateGlobalCpuLimit(balanceForCpuFreeze);
      long leftBalanceForCpuFreeze = getCpuFee(balanceForCpuFreeze, cpuGasFromFreeze,
          totalCpuGasFromFreeze);

      if (leftBalanceForCpuFreeze >= feeLimit) {
        cpuGasFromFeeLimit = BigInteger.valueOf(totalCpuGasFromFreeze)
            .multiply(BigInteger.valueOf(feeLimit))
            .divide(BigInteger.valueOf(balanceForCpuFreeze)).longValue();
      } else {
        cpuGasFromFeeLimit = Math
            .addExact(cpuGasFromFreeze,
                (feeLimit - leftBalanceForCpuFreeze) / Constant.SUN_PER_GAS);
      }
    }

    return min(Math.addExact(cpuGasFromFreeze, cpuGasFromBalance), cpuGasFromFeeLimit);
  }

  private long getGasLimit(AccountCapsule creator, AccountCapsule caller,
      TriggerSmartContract contract, long feeLimit) {

    long callerGasLimit = getGasLimit(caller, feeLimit);
    if (Arrays.equals(creator.getAddress().toByteArray(), caller.getAddress().toByteArray())) {
      return callerGasLimit;
    }

    // creatorCpuGasFromFreeze
    long creatorGasLimit = cpuProcessor.getAccountLeftCpuInUsFromFreeze(creator);

    SmartContract smartContract = this.deposit
        .getContract(contract.getContractAddress().toByteArray()).getInstance();
    long consumeUserResourcePercent = smartContract.getConsumeUserResourcePercent();

    consumeUserResourcePercent = max(0, min(consumeUserResourcePercent, 100));

    if (consumeUserResourcePercent <= 0) {
      return creatorGasLimit;
    }

    if (creatorGasLimit * consumeUserResourcePercent
        >= (100 - consumeUserResourcePercent) * callerGasLimit) {
      return 100 * Math.floorDiv(callerGasLimit, consumeUserResourcePercent);
    } else {
      return Math.addExact(callerGasLimit, creatorGasLimit);
    }
  }

  /*
   **/
  private void create()
      throws ContractExeException, ContractValidateException {
    CreateSmartContract contract = ContractCapsule.getSmartContractFromTransaction(trx);
    SmartContract newSmartContract = contract.getNewContract();

    byte[] code = newSmartContract.getBytecode().toByteArray();
    byte[] contractAddress = Wallet.generateContractAddress(trx);
    byte[] ownerAddress = contract.getOwnerAddress().toByteArray();

    long percent = contract.getNewContract().getConsumeUserResourcePercent();
    if (percent < 0 || percent > 100) {
      throw new ContractExeException("percent must be >= 0 and <= 100");
    }
    // insure one owner just have one contract
    // if (this.deposit.getContractByNormalAccount(ownerAddress) != null) {
    //   logger.error("Trying to create second contract with one account: address: " + Wallet
    //       .encode58Check(ownerAddress));
    //   return;
    // }

    // insure the new contract address haven't exist
    if (deposit.getAccount(contractAddress) != null) {
      throw new ContractExeException(
          "Trying to create a contract with existing contract address: " + Wallet
              .encode58Check(contractAddress));
    }

    newSmartContract = newSmartContract.toBuilder()
        .setContractAddress(ByteString.copyFrom(contractAddress)).build();

    // create vm to constructor smart contract
    try {

      AccountCapsule creator = this.deposit
          .getAccount(newSmartContract.getOriginAddress().toByteArray());
      // if (executorType == ET_NORMAL_TYPE) {
      //   long blockCPULeftInUs = getBlockCPULeftInUs().longValue();
      //   thisTxCPULimitInUs = min(blockCPULeftInUs,
      //       Constant.CPU_LIMIT_IN_ONE_TX_OF_SMART_CONTRACT);
      // } else {
      //   thisTxCPULimitInUs = Constant.CPU_LIMIT_IN_ONE_TX_OF_SMART_CONTRACT;
      // }

      long thisTxCPULimitInUs;
      if (ET_NORMAL_TYPE == executorType) {
        thisTxCPULimitInUs = Constant.MAX_CPU_TIME_OF_ONE_TX_WHEN_VERIFY_BLOCK;
      } else {
        thisTxCPULimitInUs = Constant.MAX_CPU_TIME_OF_ONE_TX;
      }
      long vmStartInUs = System.nanoTime() / 1000;
      long vmShouldEndInUs = vmStartInUs + thisTxCPULimitInUs;

      long feeLimit = trx.getRawData().getFeeLimit();
      long gasLimit = getGasLimit(creator, feeLimit);
      byte[] ops = newSmartContract.getBytecode().toByteArray();
      InternalTransaction internalTransaction = new InternalTransaction(trx);

      // todo: callvalue should pass into this function
      ProgramInvoke programInvoke = programInvokeFactory
          .createProgramInvoke(TRX_CONTRACT_CREATION_TYPE, executorType, trx,
              block, deposit, vmStartInUs, vmShouldEndInUs, gasLimit);
      this.vm = new VM(config);
      this.program = new Program(ops, programInvoke, internalTransaction, config);
    } catch (Exception e) {
      logger.error(e.getMessage());
      throw new ContractExeException(e.getMessage());
    }

    program.getResult().setContractAddress(contractAddress);

    deposit.createAccount(contractAddress, newSmartContract.getName(),
        Protocol.AccountType.Contract);

    deposit.createContract(contractAddress, new ContractCapsule(newSmartContract));
    deposit.saveCode(contractAddress, ProgramPrecompile.getCode(code));
    deposit.createContractByNormalAccountIndex(ownerAddress, new BytesCapsule(contractAddress));

    // transfer from callerAddress to contractAddress according to callValue
    byte[] callerAddress = contract.getOwnerAddress().toByteArray();
    long callValue = newSmartContract.getCallValue();
    if (callValue > 0) {
      transfer(this.deposit, callerAddress, contractAddress, callValue);
    }

  }

  /**
   * **
   */

  private void call()
      throws ContractExeException, ContractValidateException {
    Contract.TriggerSmartContract contract = ContractCapsule.getTriggerContractFromTransaction(trx);
    if (contract == null) {
      return;
    }

    byte[] contractAddress = contract.getContractAddress().toByteArray();
    byte[] code = this.deposit.getCode(contractAddress);
    if (isEmpty(code)) {

    } else {

      AccountCapsule caller = this.deposit.getAccount(contract.getOwnerAddress().toByteArray());
      AccountCapsule creator = this.deposit.getAccount(
          this.deposit.getContract(contractAddress).getInstance()
              .getOriginAddress().toByteArray());

      long thisTxCPULimitInUs;
      if (ET_NORMAL_TYPE == executorType) {
        thisTxCPULimitInUs = Constant.MAX_CPU_TIME_OF_ONE_TX_WHEN_VERIFY_BLOCK;
      } else {
        thisTxCPULimitInUs = Constant.MAX_CPU_TIME_OF_ONE_TX;
      }

      long vmStartInUs = System.nanoTime() / 1000;
      long vmShouldEndInUs = vmStartInUs + thisTxCPULimitInUs;

      long feeLimit = trx.getRawData().getFeeLimit();
      long gasLimit;
      try {
        gasLimit = getGasLimit(creator, caller, contract, feeLimit);
      } catch (Exception e) {
        logger.error(e.getMessage());
        throw new ContractExeException(e.getMessage());
      }

      if (isCallConstant(contractAddress)) {
        gasLimit = Constant.MAX_GAS_IN_TX;
      }

      ProgramInvoke programInvoke = programInvokeFactory
          .createProgramInvoke(TRX_CONTRACT_CALL_TYPE, executorType, trx,
              block, deposit, vmStartInUs, vmShouldEndInUs, gasLimit);
      this.vm = new VM(config);
      InternalTransaction internalTransaction = new InternalTransaction(trx);
      this.program = new Program(null, code, programInvoke, internalTransaction, config);
    }

    program.getResult().setContractAddress(contractAddress);
    //transfer from callerAddress to targetAddress according to callValue
    byte[] callerAddress = contract.getOwnerAddress().toByteArray();
    long callValue = contract.getCallValue();
    if (callValue > 0) {
      transfer(this.deposit, callerAddress, contractAddress, callValue);
    }

  }

  public void go() throws OutOfSlotTimeException, ContractExeException {
    if (!readyToExecute) {
      return;
    }

    try {
      if (vm != null) {
        vm.play(program);

        program.getResult().setRet(result.getRet());
        result = program.getResult();
        if (isCallConstant()) {
          long callValue = TransactionCapsule.getCallValue(trx.getRawData().getContract(0));
          if (callValue > 0) {
            runtimeError = "constant cannot set call value.";
          }
          return;
        }

        // todo: consume bandwidth for successful creating contract

        if (result.getException() != null || result.isRevert()) {
          result.getDeleteAccounts().clear();
          result.getLogInfoList().clear();
          result.resetFutureRefund();
          program.spendAllGas();
          spendUsage(0);
          if (result.getException() != null) {
            runtimeError = result.getException().getMessage();
            throw result.getException();
          } else {
            runtimeError = "REVERT opcode executed";
          }
        } else {
          long usedStorageSize =
              deposit.computeAfterRunStorageSize() - deposit.getBeforeRunStorageSize();
          if (!spendUsage(usedStorageSize)) {
            throw Program.Exception.notEnoughStorage();
          }
          deposit.commit();
        }

      } else {
        deposit.commit();
      }
    } catch (OutOfResourceException e) {
      logger.error(e.getMessage());
      throw new OutOfSlotTimeException(e.getMessage());
    } catch (ArithmeticException e) {
      logger.error(e.getMessage());
      throw new ContractExeException(e.getMessage());
    } catch (Exception e) {
      logger.error(e.getMessage());
      if (StringUtils.isNoneEmpty(runtimeError)) {
        runtimeError = e.getMessage();
      }
    }
  }

  private boolean spendUsage(long usedStorageSize) {

    long cpuUsage = result.getGasUsed();

    ContractCapsule contract = deposit.getContract(result.getContractAddress());
    ByteString originAddress = contract.getInstance().getOriginAddress();
    AccountCapsule origin = deposit.getAccount(originAddress.toByteArray());
    long originResourcePercent = 100 - contract.getConsumeUserResourcePercent();
    originResourcePercent = min(originResourcePercent, 100);
    originResourcePercent = max(originResourcePercent, 0);
    long originCpuUsage = Math.multiplyExact(cpuUsage, originResourcePercent) / 100;
    originCpuUsage = min(originCpuUsage, cpuProcessor.getAccountLeftCpuInUsFromFreeze(origin));
    long callerCpuUsage = cpuUsage - originCpuUsage;

    if (usedStorageSize <= 0) {
      trace.setBill(cpuUsage, 0);
      return true;
    }
    long originStorageUsage = Math
        .multiplyExact(usedStorageSize, originResourcePercent) / 100;
    originStorageUsage = min(originStorageUsage, origin.getStorageLeft());
    long callerStorageUsage = usedStorageSize - originStorageUsage;

    byte[] callerAddressBytes = TransactionCapsule.getOwner(trx.getRawData().getContract(0));
    AccountCapsule caller = deposit.getAccount(callerAddressBytes);
    long storageFee = trx.getRawData().getFeeLimit();
    long callerCpuFrozen = caller.getCpuFrozenBalance();
    long callerCpuLeft = cpuProcessor.getAccountLeftCpuInUsFromFreeze(caller);
    long callerCpuTotal = cpuProcessor.calculateGlobalCpuLimit(callerCpuFrozen);

    if (callerCpuUsage <= callerCpuLeft) {
      long cpuFee = getCpuFee(callerCpuUsage, callerCpuFrozen, callerCpuTotal);
      storageFee -= cpuFee;
    } else {
      long cpuFee = getCpuFee(callerCpuLeft, callerCpuFrozen, callerCpuTotal);
      storageFee -= (cpuFee + Math
          .multiplyExact(callerCpuUsage - callerCpuLeft, Constant.SUN_PER_GAS));
    }
    long tryBuyStorage = storageMarket.tryBuyStorage(storageFee);
    if (tryBuyStorage + caller.getStorageLeft() < callerStorageUsage) {
      trace.setBill(cpuUsage, 0);
      return false;
    }
    trace.setBill(cpuUsage, usedStorageSize);
    return true;
  }

  private long getCpuFee(long callerCpuUsage, long callerCpuFrozen, long callerCpuTotal) {
    if (callerCpuTotal <= 0) {
      return 0;
    }
    return BigInteger.valueOf(callerCpuFrozen).multiply(BigInteger.valueOf(callerCpuUsage))
        .divide(BigInteger.valueOf(callerCpuTotal)).longValue();
  }

  private boolean isCallConstant() {
    if (TRX_CONTRACT_CALL_TYPE.equals(trxType)) {
      ABI abi = deposit.getContract(result.getContractAddress()).getInstance().getAbi();
      if (Wallet.isConstant(abi, ContractCapsule.getTriggerContractFromTransaction(trx))) {
        return true;
      }
    }
    return false;
  }

  private boolean isCallConstant(byte[] address) {
    if (TRX_CONTRACT_CALL_TYPE.equals(trxType)) {
      ABI abi = deposit.getContract(address).getInstance().getAbi();
      if (Wallet.isConstant(abi, ContractCapsule.getTriggerContractFromTransaction(trx))) {
        return true;
      }
    }
    return false;
  }

  public RuntimeSummary finalization() {
    return null;
  }

  public ProgramResult getResult() {
    return result;
  }

  public String getRuntimeError() {
    return runtimeError;
  }
}
