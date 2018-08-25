package org.tron.common.runtime;

import static com.google.common.primitives.Longs.max;
import static com.google.common.primitives.Longs.min;
import static org.apache.commons.lang3.ArrayUtils.isEmpty;
import static org.tron.common.runtime.utils.MUtil.transfer;
import static org.tron.common.runtime.vm.VMUtils.saveProgramTraceFile;
import static org.tron.common.runtime.vm.VMUtils.zipAndEncode;
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
import org.spongycastle.util.encoders.Hex;
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
import org.tron.core.capsule.ContractCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.Parameter.ChainConstant;
import org.tron.core.db.EnergyProcessor;
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

@Slf4j(topic = "Runtime")
public class Runtime {


  private SystemProperties config = SystemProperties.getInstance();

  private Transaction trx;
  private Block block = null;
  private Deposit deposit;
  private ProgramInvokeFactory programInvokeFactory = null;
  private String runtimeError;
  private boolean readyToExecute = false;

  private EnergyProcessor energyProcessor = null;
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
    this.energyProcessor = new EnergyProcessor(deposit.getDbManager());
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
    this.energyProcessor = new EnergyProcessor(deposit.getDbManager());
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
    //     // if (!curENERGYLimitReachedBlockENERGYLimit()) {
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

  private long getEnergyLimit(AccountCapsule account, long feeLimit, long callValue) {

    long SUN_PER_ENERGY = deposit.getDbManager().getDynamicPropertiesStore().getEnergyFee() == 0
        ? Constant.SUN_PER_ENERGY :
        deposit.getDbManager().getDynamicPropertiesStore().getEnergyFee();
    // can change the calc way
    long leftEnergyFromFreeze = energyProcessor.getAccountLeftEnergyFromFreeze(account);
    callValue = max(callValue, 0);
    long energyFromBalance = Math
        .floorDiv(max(account.getBalance() - callValue, 0), SUN_PER_ENERGY);

    long energyFromFeeLimit;
    long totalBalanceForEnergyFreeze = account.getAccountResource().getFrozenBalanceForEnergy()
        .getFrozenBalance();
    if (0 == totalBalanceForEnergyFreeze) {
      energyFromFeeLimit =
          feeLimit / SUN_PER_ENERGY;
    } else {
      long totalEnergyFromFreeze = energyProcessor
          .calculateGlobalEnergyLimit(totalBalanceForEnergyFreeze);
      long leftBalanceForEnergyFreeze = getEnergyFee(totalBalanceForEnergyFreeze,
          leftEnergyFromFreeze,
          totalEnergyFromFreeze);

      if (leftBalanceForEnergyFreeze >= feeLimit) {
        energyFromFeeLimit = BigInteger.valueOf(totalEnergyFromFreeze)
            .multiply(BigInteger.valueOf(feeLimit))
            .divide(BigInteger.valueOf(totalBalanceForEnergyFreeze)).longValue();
      } else {
        energyFromFeeLimit = Math
            .addExact(leftEnergyFromFreeze,
                (feeLimit - leftBalanceForEnergyFreeze) / SUN_PER_ENERGY);
      }
    }

    return min(Math.addExact(leftEnergyFromFreeze, energyFromBalance), energyFromFeeLimit);
  }

  private long getEnergyLimit(AccountCapsule creator, AccountCapsule caller,
      TriggerSmartContract contract, long feeLimit, long callValue) {

    long callerEnergyLimit = getEnergyLimit(caller, feeLimit, callValue);
    if (Arrays.equals(creator.getAddress().toByteArray(), caller.getAddress().toByteArray())) {
      return callerEnergyLimit;
    }

    // creatorEnergyFromFreeze
    long creatorEnergyLimit = energyProcessor.getAccountLeftEnergyFromFreeze(creator);

    SmartContract smartContract = this.deposit
        .getContract(contract.getContractAddress().toByteArray()).getInstance();
    long consumeUserResourcePercent = smartContract.getConsumeUserResourcePercent();

    consumeUserResourcePercent = max(0, min(consumeUserResourcePercent, 100));

    if (consumeUserResourcePercent <= 0) {
      return creatorEnergyLimit;
    }

    if (creatorEnergyLimit * consumeUserResourcePercent
        >= (100 - consumeUserResourcePercent) * callerEnergyLimit) {
      return 100 * Math.floorDiv(callerEnergyLimit, consumeUserResourcePercent);
    } else {
      return Math.addExact(callerEnergyLimit, creatorEnergyLimit);
    }
  }

  /*
   **/
  private void create()
      throws ContractExeException, ContractValidateException {
    if (!deposit.getDbManager().getDynamicPropertiesStore().supportVM()) {
      throw new ContractExeException("VM work is off, need to be opened by the committee");
    }

    CreateSmartContract contract = ContractCapsule.getSmartContractFromTransaction(trx);
    SmartContract newSmartContract = contract.getNewContract();

    byte[] code = newSmartContract.getBytecode().toByteArray();
    byte[] contractAddress = Wallet.generateContractAddress(trx);
    byte[] ownerAddress = contract.getOwnerAddress().toByteArray();

    long percent = contract.getNewContract().getConsumeUserResourcePercent();
    if (percent < 0 || percent > 100) {
      throw new ContractExeException("percent must be >= 0 and <= 100");
    }

    // insure the new contract address haven't exist
    if (deposit.getAccount(contractAddress) != null) {
      throw new ContractExeException(
          "Trying to create a contract with existing contract address: " + Wallet
              .encode58Check(contractAddress));
    }

    newSmartContract = newSmartContract.toBuilder()
        .setContractAddress(ByteString.copyFrom(contractAddress)).build();
    long callValue = newSmartContract.getCallValue();
    // create vm to constructor smart contract
    try {

      AccountCapsule creator = this.deposit
          .getAccount(newSmartContract.getOriginAddress().toByteArray());
      // if (executorType == ET_NORMAL_TYPE) {
      //   long blockENERGYLeftInUs = getBlockENERGYLeftInUs().longValue();
      //   thisTxENERGYLimitInUs = min(blockENERGYLeftInUs,
      //       Constant.ENERGY_LIMIT_IN_ONE_TX_OF_SMART_CONTRACT);
      // } else {
      //   thisTxENERGYLimitInUs = Constant.ENERGY_LIMIT_IN_ONE_TX_OF_SMART_CONTRACT;
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
      long energyLimit = getEnergyLimit(creator, feeLimit, callValue);
      byte[] ops = newSmartContract.getBytecode().toByteArray();
      InternalTransaction internalTransaction = new InternalTransaction(trx);

      ProgramInvoke programInvoke = programInvokeFactory
          .createProgramInvoke(TRX_CONTRACT_CREATION_TYPE, executorType, trx,
              block, deposit, vmStartInUs, vmShouldEndInUs, energyLimit);
      this.vm = new VM(config);
      this.program = new Program(ops, programInvoke, internalTransaction, config);
      Program.setRootTransactionId(new TransactionCapsule(trx).getTransactionId().getBytes());
      Program.resetNonce();
      Program.setRootCallConstant(isCallConstant());
    } catch (Exception e) {
      logger.error(e.getMessage());
      throw new ContractExeException(e.getMessage());
    }

    program.getResult().setContractAddress(contractAddress);

    deposit.createAccount(contractAddress, newSmartContract.getName(),
        Protocol.AccountType.Contract);

    deposit.createContract(contractAddress, new ContractCapsule(newSmartContract));
    deposit.saveCode(contractAddress, ProgramPrecompile.getCode(code));
    // deposit.createContractByNormalAccountIndex(ownerAddress, new BytesCapsule(contractAddress));

    // transfer from callerAddress to contractAddress according to callValue
    byte[] callerAddress = contract.getOwnerAddress().toByteArray();
    if (callValue > 0) {
      transfer(this.deposit, callerAddress, contractAddress, callValue);
    }

  }

  /**
   * **
   */

  private void call()
      throws ContractExeException, ContractValidateException {

    if (!deposit.getDbManager().getDynamicPropertiesStore().supportVM()) {
      throw new ContractExeException("VM work is off, need to be opened by the committee");
    }

    Contract.TriggerSmartContract contract = ContractCapsule.getTriggerContractFromTransaction(trx);
    if (contract == null) {
      return;
    }

    byte[] contractAddress = contract.getContractAddress().toByteArray();
    byte[] code = this.deposit.getCode(contractAddress);
    long callValue = contract.getCallValue();
    if (isEmpty(code)) {

    } else {

      AccountCapsule caller = this.deposit.getAccount(contract.getOwnerAddress().toByteArray());
      AccountCapsule creator = this.deposit.getAccount(
          this.deposit.getContract(contractAddress).getInstance()
              .getOriginAddress().toByteArray());

      long thisTxENERGYLimitInUs;
      if (ET_NORMAL_TYPE == executorType) {
        thisTxENERGYLimitInUs = Constant.MAX_CPU_TIME_OF_ONE_TX_WHEN_VERIFY_BLOCK;
      } else {
        thisTxENERGYLimitInUs = Constant.MAX_CPU_TIME_OF_ONE_TX;
      }

      long vmStartInUs = System.nanoTime() / 1000;
      long vmShouldEndInUs = vmStartInUs + thisTxENERGYLimitInUs;

      long feeLimit = trx.getRawData().getFeeLimit();
      long energyLimit;
      try {
        energyLimit = getEnergyLimit(creator, caller, contract, feeLimit, callValue);
      } catch (Exception e) {
        logger.error(e.getMessage());
        throw new ContractExeException(e.getMessage());
      }

      if (isCallConstant(contractAddress)) {
        energyLimit = Constant.MAX_ENERGY_IN_TX;
      }

      ProgramInvoke programInvoke = programInvokeFactory
          .createProgramInvoke(TRX_CONTRACT_CALL_TYPE, executorType, trx,
              block, deposit, vmStartInUs, vmShouldEndInUs, energyLimit);
      this.vm = new VM(config);
      InternalTransaction internalTransaction = new InternalTransaction(trx);
      this.program = new Program(null, code, programInvoke, internalTransaction, config);
      Program.setRootTransactionId(new TransactionCapsule(trx).getTransactionId().getBytes());
      Program.resetNonce();
      Program.setRootCallConstant(isCallConstant());
    }

    program.getResult().setContractAddress(contractAddress);
    //transfer from callerAddress to targetAddress according to callValue
    byte[] callerAddress = contract.getOwnerAddress().toByteArray();
    if (callValue > 0) {
      transfer(this.deposit, callerAddress, contractAddress, callValue);
    }

  }

  public void go() throws OutOfSlotTimeException {
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

        if (result.getException() != null || result.isRevert()) {
          result.getDeleteAccounts().clear();
          result.getLogInfoList().clear();
          result.resetFutureRefund();

          if (result.getException() != null) {
            program.spendAllEnergy();
            runtimeError = result.getException().getMessage();
            throw result.getException();
          } else {
            runtimeError = "REVERT opcode executed";
          }
        } else {
          deposit.commit();
        }
      } else {
        deposit.commit();
      }
    } catch (OutOfResourceException e) {
      logger.error("runtime error is :{}", e.getMessage());
      throw new OutOfSlotTimeException(e.getMessage());
    } catch (Throwable e) {
      if (Objects.isNull(result.getException())) {
        result.setException(new RuntimeException("Unknown Throwable"));
      }
      if (StringUtils.isEmpty(runtimeError)) {
        runtimeError = result.getException().getMessage();
      }
      logger.error("runtime error is :{}", result.getException().getMessage());
    }
    trace.setBill(result.getEnergyUsed());
  }

  private long getEnergyFee(long callerEnergyUsage, long callerEnergyFrozen,
      long callerEnergyTotal) {
    if (callerEnergyTotal <= 0) {
      return 0;
    }
    return BigInteger.valueOf(callerEnergyFrozen).multiply(BigInteger.valueOf(callerEnergyUsage))
        .divide(BigInteger.valueOf(callerEnergyTotal)).longValue();
  }

  public boolean isCallConstant() {
    TriggerSmartContract triggerContractFromTransaction = ContractCapsule
        .getTriggerContractFromTransaction(trx);
    if (TRX_CONTRACT_CALL_TYPE.equals(trxType)) {
      ABI abi = deposit
          .getContract(triggerContractFromTransaction.getContractAddress().toByteArray())
          .getInstance().getAbi();
      if (Wallet.isConstant(abi, triggerContractFromTransaction)) {
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

  public void finalization() {
    if (config.vmTrace() && program != null && result != null) {
      String trace = program.getTrace()
          .result(result.getHReturn())
          .error(result.getException())
          .toString();

      if (config.vmTraceCompressed()) {
        trace = zipAndEncode(trace);
      }

      String txHash = Hex.toHexString(new InternalTransaction(trx).getHash());
      saveProgramTraceFile(config, txHash, trace);
    }

  }

  public ProgramResult getResult() {
    return result;
  }

  public String getRuntimeError() {
    return runtimeError;
  }
}
