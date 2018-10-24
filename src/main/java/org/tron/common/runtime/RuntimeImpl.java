package org.tron.common.runtime;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.apache.commons.lang3.ArrayUtils.getLength;
import static org.apache.commons.lang3.ArrayUtils.isNotEmpty;
import static org.tron.common.runtime.utils.MUtil.convertToTronAddress;
import static org.tron.common.runtime.utils.MUtil.transfer;
import static org.tron.common.runtime.vm.VMUtils.saveProgramTraceFile;
import static org.tron.common.runtime.vm.VMUtils.zipAndEncode;
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
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.runtime.config.VMConfig;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.runtime.vm.EnergyCost;
import org.tron.common.runtime.vm.VM;
import org.tron.common.runtime.vm.program.InternalTransaction;
import org.tron.common.runtime.vm.program.InternalTransaction.ExecutorType;
import org.tron.common.runtime.vm.program.Program;
import org.tron.common.runtime.vm.program.Program.JVMStackOverFlowException;
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
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.ContractCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.Parameter.ChainConstant;
import org.tron.core.config.args.Args;
import org.tron.core.db.EnergyProcessor;
import org.tron.core.db.TransactionTrace;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.VMIllegalException;
import org.tron.protos.Contract;
import org.tron.protos.Contract.CreateSmartContract;
import org.tron.protos.Contract.TriggerSmartContract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.SmartContract;
import org.tron.protos.Protocol.SmartContract.ABI;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Transaction.Result.contractResult;

@Slf4j(topic = "Runtime")
public class RuntimeImpl implements Runtime {

  private VMConfig config = VMConfig.getInstance();

  private Transaction trx;
  private BlockCapsule blockCap = null;
  private Deposit deposit;
  private ProgramInvokeFactory programInvokeFactory = null;
  private String runtimeError;

  private EnergyProcessor energyProcessor = null;
  private ProgramResult result = new ProgramResult();


  private VM vm = null;
  private Program program = null;
  private InternalTransaction rootInternalTransaction;

  @Getter
  @Setter
  private InternalTransaction.TrxType trxType = TRX_UNKNOWN_TYPE;
  private ExecutorType executorType = ET_UNKNOWN_TYPE;

  //tx trace
  private TransactionTrace trace;

  /**
   * For blockCap's trx run
   */
  public RuntimeImpl(TransactionTrace trace, BlockCapsule block, Deposit deposit,
      ProgramInvokeFactory programInvokeFactory) {
    this.trace = trace;
    this.trx = trace.getTrx().getInstance();

    if (Objects.nonNull(block)) {
      this.blockCap = block;
      this.executorType = ET_NORMAL_TYPE;
    } else {
      this.blockCap = new BlockCapsule(Block.newBuilder().build());
      this.executorType = ET_PRE_TYPE;
    }
    this.deposit = deposit;
    this.programInvokeFactory = programInvokeFactory;
    this.energyProcessor = new EnergyProcessor(deposit.getDbManager());

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
   * For constant trx with latest blockCap.
   */
  private boolean isStaticCall = false;

  public RuntimeImpl(Transaction tx, BlockCapsule block, DepositImpl deposit,
      ProgramInvokeFactory programInvokeFactory, boolean isStaticCall) {
    this(tx, block, deposit, programInvokeFactory);
    this.isStaticCall = isStaticCall;
  }

  public RuntimeImpl(Transaction tx, BlockCapsule block, DepositImpl deposit,
      ProgramInvokeFactory programInvokeFactory) {
    this.trx = tx;
    this.deposit = deposit;
    this.programInvokeFactory = programInvokeFactory;
    this.executorType = ET_PRE_TYPE;
    this.blockCap = block;
    this.energyProcessor = new EnergyProcessor(deposit.getDbManager());
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


  public BigInteger getBlockCPULeftInUs() {

    // insure blockCap is not null
    BigInteger curBlockHaveElapsedCPUInUs =
        BigInteger.valueOf(
            1000 * (DateTime.now().getMillis() - blockCap.getInstance().getBlockHeader()
                .getRawData()
                .getTimestamp())); // us
    BigInteger curBlockCPULimitInUs = BigInteger.valueOf((long)
        (1000 * ChainConstant.BLOCK_PRODUCED_INTERVAL * 0.5
            * Args.getInstance().getBlockProducedTimeOut()
            / 100)); // us

    return curBlockCPULimitInUs.subtract(curBlockHaveElapsedCPUInUs);

  }

  public void execute()
      throws ContractValidateException, ContractExeException, VMIllegalException {
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
        throw new ContractValidateException("Unknown contract type");
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
            .divide(BigInteger.valueOf(totalBalanceForEnergyFreeze)).longValueExact();
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

    if (creatorEnergyLimit * consumeUserResourcePercent
        > (100 - consumeUserResourcePercent) * callerEnergyLimit) {
      return Math.floorDiv(callerEnergyLimit * 100, consumeUserResourcePercent);
    } else {
      return Math.addExact(callerEnergyLimit, creatorEnergyLimit);
    }
  }

  private double getThisTxCPULimitInUsRatio() {

    double thisTxCPULimitInUsRatio;

    if (ET_NORMAL_TYPE == executorType) {
      // self witness 2
      if (this.blockCap != null && blockCap.generatedByMyself &&
          this.blockCap.getInstance().getBlockHeader().getWitnessSignature().isEmpty()) {
        thisTxCPULimitInUsRatio = 1.0;
      } else {
        // self witness 3, other witness 3, fullnode 2
        if (trx.getRet(0).getContractRet() == contractResult.OUT_OF_TIME) {
          thisTxCPULimitInUsRatio = Args.getInstance().getMinTimeRatio();
        } else {
          thisTxCPULimitInUsRatio = Args.getInstance().getMaxTimeRatio();
        }
      }
    } else {
      // self witness 1, other witness 1, fullnode 1
      thisTxCPULimitInUsRatio = 1.0;
    }

    return thisTxCPULimitInUsRatio;
  }

  /*
   **/
  private void create()
      throws ContractValidateException, VMIllegalException {
    if (!deposit.getDbManager().getDynamicPropertiesStore().supportVM()) {
      throw new ContractValidateException("vm work is off, need to be opened by the committee");
    }

    CreateSmartContract contract = ContractCapsule.getSmartContractFromTransaction(trx);
    if (contract == null) {
      throw new ContractValidateException("Cannot get CreateSmartContract from transaction");
    }
    SmartContract newSmartContract = contract.getNewContract();
    if (!contract.getOwnerAddress().equals(newSmartContract.getOriginAddress())) {
      logger.info("OwnerAddress not equals OriginAddress");
      throw new VMIllegalException("OwnerAddress is not equals OriginAddress");
    }
    byte[] code = newSmartContract.getBytecode().toByteArray();
    byte[] contractAddress = Wallet.generateContractAddress(trx);
    byte[] contractName = newSmartContract.getName().getBytes();

    if (contractName.length > 32) {
      throw new ContractValidateException("contractName's length cannot be greater than 32");
    }

    long percent = contract.getNewContract().getConsumeUserResourcePercent();
    if (percent < 0 || percent > 100) {
      throw new ContractValidateException("percent must be >= 0 and <= 100");
    }

    // insure the new contract address haven't exist
    if (deposit.getAccount(contractAddress) != null) {
      throw new ContractValidateException(
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

      long MAX_CPU_TIME_OF_ONE_TX = deposit.getDbManager().getDynamicPropertiesStore()
          .getMaxCpuTimeOfOneTx() * 1000;

      long thisTxCPULimitInUs = (long) (MAX_CPU_TIME_OF_ONE_TX * getThisTxCPULimitInUsRatio());

      long vmStartInUs = System.nanoTime() / 1000;
      long vmShouldEndInUs = vmStartInUs + thisTxCPULimitInUs;

      long feeLimit = trx.getRawData().getFeeLimit();
      if (feeLimit < 0 || feeLimit > VMConfig.MAX_FEE_LIMIT) {
        logger.info("invalid feeLimit {}", feeLimit);
        throw new ContractValidateException(
            "feeLimit must be >= 0 and <= " + VMConfig.MAX_FEE_LIMIT);
      }

      long energyLimit = getEnergyLimit(creator, feeLimit, callValue);
      byte[] ops = newSmartContract.getBytecode().toByteArray();
      rootInternalTransaction = new InternalTransaction(trx, trxType);

      ProgramInvoke programInvoke = programInvokeFactory
          .createProgramInvoke(TRX_CONTRACT_CREATION_TYPE, executorType, trx,
              blockCap.getInstance(), deposit, vmStartInUs, vmShouldEndInUs, energyLimit);
      this.vm = new VM(config);
      this.program = new Program(ops, programInvoke, rootInternalTransaction, config,
          this.blockCap);
      this.program.setRootTransactionId(new TransactionCapsule(trx).getTransactionId().getBytes());
      this.program.setRootCallConstant(isCallConstant());
    } catch (Exception e) {
      logger.info(e.getMessage());
      throw new ContractValidateException(e.getMessage());
    }

    program.getResult().setContractAddress(contractAddress);

    deposit.createAccount(contractAddress, newSmartContract.getName(),
        Protocol.AccountType.Contract);

    deposit.createContract(contractAddress, new ContractCapsule(newSmartContract));
    deposit.saveCode(contractAddress, ProgramPrecompile.getCode(code));

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
      throws ContractValidateException {

    if (!deposit.getDbManager().getDynamicPropertiesStore().supportVM()) {
      logger.info("vm work is off, need to be opened by the committee");
      throw new ContractValidateException("VM work is off, need to be opened by the committee");
    }

    Contract.TriggerSmartContract contract = ContractCapsule.getTriggerContractFromTransaction(trx);
    if (contract == null) {
      return;
    }

    if (contract.getContractAddress() == null) {
      throw new ContractValidateException("Cannot get contract address from TriggerContract");
    }

    byte[] contractAddress = contract.getContractAddress().toByteArray();

    ContractCapsule deployedContract = this.deposit.getContract(contractAddress);
    if (null == deployedContract) {
      logger.info("No contract or not a smart contract");
      throw new ContractValidateException("No contract or not a smart contract");
    }
    byte[] code = this.deposit.getCode(contractAddress);
    long callValue = contract.getCallValue();
    if (isNotEmpty(code)) {
      AccountCapsule caller = this.deposit.getAccount(contract.getOwnerAddress().toByteArray());
      AccountCapsule creator = this.deposit.getAccount(
          deployedContract.getInstance()
              .getOriginAddress().toByteArray());

      long MAX_CPU_TIME_OF_ONE_TX = deposit.getDbManager().getDynamicPropertiesStore()
          .getMaxCpuTimeOfOneTx() * 1000;
      long thisTxCPULimitInUs =
          (long) (MAX_CPU_TIME_OF_ONE_TX * getThisTxCPULimitInUsRatio());

      long vmStartInUs = System.nanoTime() / 1000;
      long vmShouldEndInUs = vmStartInUs + thisTxCPULimitInUs;

      long feeLimit = trx.getRawData().getFeeLimit();
      if (feeLimit < 0 || feeLimit > VMConfig.MAX_FEE_LIMIT) {
        logger.info("invalid feeLimit {}", feeLimit);
        throw new ContractValidateException(
            "feeLimit must be >= 0 and <= " + VMConfig.MAX_FEE_LIMIT);
      }
      long energyLimit;
      if (isCallConstant(contractAddress)) {
        isStaticCall = true;
        energyLimit = Constant.MAX_ENERGY_IN_TX;
      } else {
        energyLimit = getEnergyLimit(creator, caller, contract, feeLimit, callValue);
      }

      ProgramInvoke programInvoke = programInvokeFactory
          .createProgramInvoke(TRX_CONTRACT_CALL_TYPE, executorType, trx,
              blockCap.getInstance(), deposit, vmStartInUs, vmShouldEndInUs, energyLimit);
      if (isStaticCall) {
        programInvoke.setStaticCall();
      }
      this.vm = new VM(config);
      rootInternalTransaction = new InternalTransaction(trx, trxType);
      this.program = new Program(null, code, programInvoke, rootInternalTransaction, config,
          this.blockCap);
      this.program.setRootTransactionId(new TransactionCapsule(trx).getTransactionId().getBytes());
      this.program.setRootCallConstant(isCallConstant());
    }

    program.getResult().setContractAddress(contractAddress);
    //transfer from callerAddress to targetAddress according to callValue
    byte[] callerAddress = contract.getOwnerAddress().toByteArray();
    if (callValue > 0) {
      transfer(this.deposit, callerAddress, contractAddress, callValue);
    }

  }

  public void go() {
    try {
      if (vm != null) {

        TransactionCapsule trxCap = new TransactionCapsule(trx);
        if (null != blockCap && blockCap.generatedByMyself && null != trxCap.getContractRet()
            && contractResult.OUT_OF_TIME
            .equals(trxCap.getContractRet())) {
          result = program.getResult();
          program.spendAllEnergy();
          runtimeError = "Haven Time Out";
          result.setException(Program.Exception.notEnoughTime("Haven Time Out"));
          throw Program.Exception.notEnoughTime("Haven Time Out");
        }

        vm.play(program);
        result = program.getResult();

        if (isCallConstant()) {
          long callValue = TransactionCapsule.getCallValue(trx.getRawData().getContract(0));
          if (callValue > 0) {
            runtimeError = "constant cannot set call value.";
          }
          return;
        }

        if (TRX_CONTRACT_CREATION_TYPE == trxType && !result.isRevert()) {
          byte[] code = program.getResult().getHReturn();
          long saveCodeEnergy = getLength(code) * EnergyCost.getInstance().getCREATE_DATA();
          long afterSpend = program.getEnergyLimitLeft().longValue() - saveCodeEnergy;
          if (afterSpend < 0) {
            if (null == result.getException()) {
              result.setException(Program.Exception
                  .notEnoughSpendEnergy("save just created contract code",
                      saveCodeEnergy, program.getEnergyLimitLeft().longValue()));
            }
          } else {
            result.spendEnergy(saveCodeEnergy);
          }
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
          getResult().getInternalTransactions().add(0, rootInternalTransaction);
          deposit.commit();
        }
      } else {
        if (!trxType.equals(TRX_PRECOMPILED_TYPE)) {
          getResult().getInternalTransactions().add(0, rootInternalTransaction);
        }
        deposit.commit();
      }
    } catch (JVMStackOverFlowException e) {
      program.spendAllEnergy();
      result = program.getResult();
      result.setException(e);
      runtimeError = result.getException().getMessage();
      logger.info("JVMStackOverFlowException: {}", result.getException().getMessage());
    } catch (OutOfResourceException e) {
      program.spendAllEnergy();
      result = program.getResult();
      result.setException(e);
      runtimeError = result.getException().getMessage();
      logger.info("timeout: {}", result.getException().getMessage());
    } catch (ContractValidateException e) {
      logger.info("when check constant, {}", e.getMessage());
    } catch (Throwable e) {
      program.spendAllEnergy();
      result = program.getResult();
      if (Objects.isNull(result.getException())) {
        logger.info(e.getMessage(), e);
        result.setException(new RuntimeException("Unknown Throwable"));
      }
      if (StringUtils.isEmpty(runtimeError)) {
        runtimeError = result.getException().getMessage();
      }
      logger.info("runtime error is :{}", result.getException().getMessage());
    }
    trace.setBill(result.getEnergyUsed());
  }

  private long getEnergyFee(long callerEnergyUsage, long callerEnergyFrozen,
      long callerEnergyTotal) {
    if (callerEnergyTotal <= 0) {
      return 0;
    }
    return BigInteger.valueOf(callerEnergyFrozen).multiply(BigInteger.valueOf(callerEnergyUsage))
        .divide(BigInteger.valueOf(callerEnergyTotal)).longValueExact();
  }

  public boolean isCallConstant() throws ContractValidateException {

    TriggerSmartContract triggerContractFromTransaction = ContractCapsule
        .getTriggerContractFromTransaction(trx);
    if (TRX_CONTRACT_CALL_TYPE.equals(trxType)) {

      ContractCapsule contract = deposit
          .getContract(triggerContractFromTransaction.getContractAddress().toByteArray());
      if (contract == null) {
        logger.info("contract: {} is not in contract store", Wallet
            .encode58Check(triggerContractFromTransaction.getContractAddress().toByteArray()));
        throw new ContractValidateException("contract: " + Wallet
            .encode58Check(triggerContractFromTransaction.getContractAddress().toByteArray())
            + " is not in contract store");
      }
      ABI abi = contract.getInstance().getAbi();
      if (Wallet.isConstant(abi, triggerContractFromTransaction)) {
        return true;
      }
    }
    return false;
  }

  private boolean isCallConstant(byte[] address) throws ContractValidateException {

    if (TRX_CONTRACT_CALL_TYPE.equals(trxType)) {
      ABI abi = deposit.getContract(address).getInstance().getAbi();
      if (Wallet.isConstant(abi, ContractCapsule.getTriggerContractFromTransaction(trx))) {
        return true;
      }
    }
    return false;
  }

  public void finalization() {
    if (StringUtils.isEmpty(runtimeError)) {
      for (DataWord contract : result.getDeleteAccounts()) {
        deposit.deleteContract(convertToTronAddress((contract.getLast20Bytes())));
      }
    }

    if (config.vmTrace() && program != null) {
      String traceContent = program.getTrace()
          .result(result.getHReturn())
          .error(result.getException())
          .toString();

      if (config.vmTraceCompressed()) {
        traceContent = zipAndEncode(traceContent);
      }

      String txHash = Hex.toHexString(new InternalTransaction(trx, trxType).getHash());
      saveProgramTraceFile(config, txHash, traceContent);
    }

  }

  public ProgramResult getResult() {
    return result;
  }

  public String getRuntimeError() {
    return runtimeError;
  }
}
