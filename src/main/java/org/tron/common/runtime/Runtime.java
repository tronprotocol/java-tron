package org.tron.common.runtime;

import static com.google.common.primitives.Longs.max;
import static com.google.common.primitives.Longs.min;
import static org.apache.commons.lang3.ArrayUtils.isEmpty;
import static org.tron.common.runtime.vm.program.InternalTransaction.ExecuterType.ET_CONSTANT_TYPE;
import static org.tron.common.runtime.vm.program.InternalTransaction.ExecuterType.ET_NORMAL_TYPE;
import static org.tron.common.runtime.vm.program.InternalTransaction.ExecuterType.ET_PRE_TYPE;
import static org.tron.common.runtime.vm.program.InternalTransaction.ExecuterType.ET_UNKNOWN_TYPE;
import static org.tron.common.runtime.vm.program.InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE;
import static org.tron.common.runtime.vm.program.InternalTransaction.TrxType.TRX_CONTRACT_CREATION_TYPE;
import static org.tron.common.runtime.vm.program.InternalTransaction.TrxType.TRX_PRECOMPILED_TYPE;
import static org.tron.common.runtime.vm.program.InternalTransaction.TrxType.TRX_UNKNOWN_TYPE;

import com.google.protobuf.ByteString;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.runtime.config.SystemProperties;
import org.tron.common.runtime.vm.PrecompiledContracts;
import org.tron.common.runtime.vm.VM;
import org.tron.common.runtime.vm.program.InternalTransaction;
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
public class Runtime {

  private static final Logger logger = LoggerFactory.getLogger("execute");

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
  private InternalTransaction.ExecuterType executerType = ET_UNKNOWN_TYPE;

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
      this.executerType = ET_NORMAL_TYPE;
    } else {
      this.block = Block.newBuilder().build();
      this.executerType = ET_PRE_TYPE;
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
  public Runtime(Transaction tx, DepositImpl deposit, ProgramInvokeFactory programInvokeFactory) {
    this.trx = tx;
    this.deposit = deposit;
    this.programInvokeFactory = programInvokeFactory;
    this.executerType = ET_PRE_TYPE;
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
  public Runtime(Transaction tx, ProgramInvokeFactory programInvokeFactory, Deposit deposit) {
    trx = tx;
    this.deposit = deposit;
    this.programInvokeFactory = programInvokeFactory;
    executerType = ET_CONSTANT_TYPE;
    trxType = TRX_CONTRACT_CALL_TYPE;

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

    switch (trxType) {
      case TRX_PRECOMPILED_TYPE:
        readyToExecute = true;
        break;
      case TRX_CONTRACT_CREATION_TYPE:
      case TRX_CONTRACT_CALL_TYPE:
        if (!curCPULimitReachedBlockCPULimit()) {
          readyToExecute = true;
        }
        break;
      default:
        break;
    }
  }


  public BigInteger getBlockCPULeftInUs() {

    // insure block is not null
    BigInteger curBlockHaveElapsedCPUInUs =
        BigInteger.valueOf(
            1000 * (DateTime.now().getMillis() - block.getBlockHeader().getRawDataOrBuilder()
                .getTimestamp())); // us
    BigInteger curBlockCPULimitInUs = BigInteger.valueOf((long)
        (1000 * ChainConstant.BLOCK_PRODUCED_INTERVAL * 0.5
            * ChainConstant.BLOCK_PRODUCED_TIME_OUT
            / 100)); // us

    return curBlockCPULimitInUs.subtract(curBlockHaveElapsedCPUInUs);

  }

  public boolean curCPULimitReachedBlockCPULimit() {

    if (executerType == ET_NORMAL_TYPE) {
      BigInteger blockCPULeftInUs = getBlockCPULeftInUs();
      BigInteger oneTxCPULimitInUs = BigInteger
          .valueOf(Constant.CPU_LIMIT_IN_ONE_TX_OF_SMART_CONTRACT);

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

    long cpuInUsFromDrop = (long) (limitInDrop * 1.0 / Constant.DROP_PER_CPU_US);

    return min(maxCpuInUsByAccount, max(cpuInUsFromFreeze, cpuInUsFromDrop)); // us

  }

  private long getAccountCPULimitInUs(AccountCapsule creator, AccountCapsule sender,
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
      return senderCpuLimit + creatorCpuFromFrozen;
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

  /*
   **/
  private void create()
      throws ContractExeException {
    CreateSmartContract contract = ContractCapsule.getSmartContractFromTransaction(trx);
    SmartContract newSmartContract = contract.getNewContract();

    byte[] code = newSmartContract.getBytecode().toByteArray();
    byte[] contractAddress = Wallet.generateContractAddress(trx);
    byte[] ownerAddress = contract.getOwnerAddress().toByteArray();

    // insure one owner just have one contract
    if (this.deposit.getContractByNormalAccount(ownerAddress) != null) {
      logger.error("Trying to create second contract with one account: address: " + Wallet
          .encode58Check(ownerAddress));
      return;
    }

    // insure the new contract address haven't exist
    if (deposit.getAccount(contractAddress) != null) {
      logger.error("Trying to create a contract with existing contract address: " + Wallet
          .encode58Check(contractAddress));
      return;
    }

    newSmartContract = newSmartContract.toBuilder()
        .setContractAddress(ByteString.copyFrom(contractAddress)).build();

    // create vm to constructor smart contract
    try {

      // todo use default value for cpu max and storage max
      AccountCapsule creator = this.deposit
          .getAccount(newSmartContract.getOriginAddress().toByteArray());
      long thisTxCPULimitInUs;
      long maxCpuInUsByCreator = trx.getRawData().getMaxCpuUsage();
      long limitInDrop = trx.getRawData().getFeeLimit();
      long accountCPULimitInUs = getAccountCPULimitInUs(creator, limitInDrop,
          maxCpuInUsByCreator);
      if (executerType == ET_NORMAL_TYPE) {
        long blockCPULeftInUs = getBlockCPULeftInUs().longValue();
        thisTxCPULimitInUs = min(accountCPULimitInUs, blockCPULeftInUs,
            Constant.CPU_LIMIT_IN_ONE_TX_OF_SMART_CONTRACT);
      } else {
        thisTxCPULimitInUs = min(accountCPULimitInUs,
            Constant.CPU_LIMIT_IN_ONE_TX_OF_SMART_CONTRACT);
      }

      long vmStartInUs = System.nanoTime() / 1000;
      long vmShouldEndInUs = vmStartInUs + thisTxCPULimitInUs;

      byte[] ops = newSmartContract.getBytecode().toByteArray();
      InternalTransaction internalTransaction = new InternalTransaction(trx);
      ProgramInvoke programInvoke = programInvokeFactory
          .createProgramInvoke(TRX_CONTRACT_CREATION_TYPE, executerType, trx,
              block, deposit, vmStartInUs, vmShouldEndInUs);
      this.vm = new VM(config);
      this.program = new Program(ops, programInvoke, internalTransaction, config);
    } catch (Exception e) {
      logger.error(e.getMessage());
      return;
    }

    program.getResult().setContractAddress(contractAddress);

    deposit.createAccount(contractAddress, newSmartContract.getName(),
        Protocol.AccountType.Contract);

    deposit.createContract(contractAddress, new ContractCapsule(newSmartContract));
    deposit.saveCode(contractAddress, ProgramPrecompile.getCode(code));
    deposit.createContractByNormalAccountIndex(ownerAddress, new BytesCapsule(contractAddress));

    // transfer from callerAddress to contractAddress according to callValue
    byte[] callerAddress = contract.getOwnerAddress().toByteArray();
    byte[] callValue = newSmartContract.getCallValue().toByteArray();
    if (null != callValue && callValue.length != 0) {
      long callValueLong = new BigInteger(Hex.toHexString(callValue), 16).longValue();
      this.deposit.addBalance(callerAddress, -callValueLong);
      this.deposit.addBalance(contractAddress, callValueLong);
    }

  }

  /**
   * **
   */
  private void call()
      throws ContractExeException {
    Contract.TriggerSmartContract contract = ContractCapsule.getTriggerContractFromTransaction(trx);
    if (contract == null) {
      return;
    }

    byte[] contractAddress = contract.getContractAddress().toByteArray();
    byte[] code = this.deposit.getCode(contractAddress);
    if (isEmpty(code)) {

    } else {

      AccountCapsule sender = this.deposit.getAccount(contract.getOwnerAddress().toByteArray());
      AccountCapsule creator = this.deposit.getAccount(
          this.deposit.getContract(contractAddress).getInstance()
              .getOriginAddress().toByteArray());

      // todo use default value for cpu max and storage max
      long thisTxCPULimitInUs;
      long maxCpuInUsBySender = trx.getRawData().getMaxCpuUsage();
      long limitInDrop = trx.getRawData().getFeeLimit();
      long accountCPULimitInUs = getAccountCPULimitInUs(creator, sender, contract,
          maxCpuInUsBySender, limitInDrop);
      if (executerType == ET_NORMAL_TYPE) {
        long blockCPULeftInUs = getBlockCPULeftInUs().longValue();
        thisTxCPULimitInUs = min(accountCPULimitInUs, blockCPULeftInUs,
            Constant.CPU_LIMIT_IN_ONE_TX_OF_SMART_CONTRACT);
      } else {
        thisTxCPULimitInUs = min(accountCPULimitInUs,
            Constant.CPU_LIMIT_IN_ONE_TX_OF_SMART_CONTRACT);
      }

      long vmStartInUs = System.nanoTime() / 1000;
      long vmShouldEndInUs = vmStartInUs + thisTxCPULimitInUs;

      ProgramInvoke programInvoke = programInvokeFactory
          .createProgramInvoke(TRX_CONTRACT_CALL_TYPE, executerType, trx,
              block, deposit, vmStartInUs, vmShouldEndInUs);
      this.vm = new VM(config);
      InternalTransaction internalTransaction = new InternalTransaction(trx);
      this.program = new Program(null, code, programInvoke, internalTransaction, config);
    }

    program.getResult().setContractAddress(contractAddress);
    //transfer from callerAddress to targetAddress according to callValue
    byte[] callerAddress = contract.getOwnerAddress().toByteArray();
    byte[] callValue = contract.getCallValue().toByteArray();
    if (null != callValue && callValue.length != 0) {
      long callValueLong = new BigInteger(Hex.toHexString(callValue), 16).longValue();
      this.deposit.addBalance(callerAddress, -callValueLong);
      this.deposit.addBalance(contractAddress, callValueLong);
    }

  }

  public void go() {

    if (!readyToExecute) {
      return;
    }

    try {
      if (vm != null) {
        vm.play(program);

        result = program.getResult();
        if (isCallConstant()) {
          return;
        }

        if (result.getException() != null || result.isRevert()) {
          result.getDeleteAccounts().clear();
          result.getLogInfoList().clear();
          result.resetFutureRefund();

          if (result.getException() != null) {
            throw result.getException();
          } else {
            runtimeError = "REVERT opcode executed";
          }
        } else {

          // touchedAccounts.addAll(result.getTouchedAccounts());
          // check storage useage
          long useedStorageSize =
              deposit.getBeforeRunStorageSize() - deposit.computeAfterRunStorageSize();
          if (useedStorageSize > trx.getRawData().getMaxStorageUsage()) {
            result.setException(Program.Exception.notEnoughStorage());
            throw result.getException();
          }
          spendUsage(useedStorageSize);
          if (executerType == ET_NORMAL_TYPE) {
            deposit.commit();
          }
        }
      } else {
        if (executerType == ET_NORMAL_TYPE) {
          deposit.commit();
        }
      }
    } catch (OutOfResourceException e) {
      spendUsage(0);
      logger.error(e.getMessage());
      runtimeError = e.getMessage();
    } catch (Exception e) {
      logger.error(e.getMessage());
      runtimeError = e.getMessage();
    }
  }

  private void spendUsage(long useedStorageSize) {

    cpuProcessor = new CpuProcessor(deposit.getDbManager());

    long now = System.nanoTime() / 1000;
    long cpuUsage = now - program.getVmStartInUs();

//    ContractCapsule contract = deposit.getContract(result.getContractAddress());
//    ByteString originAddress = contract.getInstance().getOriginAddress();
//    AccountCapsule origin = deposit.getAccount(originAddress.toByteArray());
    if (useedStorageSize <= 0) {
      trace.setBill(cpuUsage, useedStorageSize);
      return;
    }
    byte[] callerAddressBytes = TransactionCapsule.getOwner(trx.getRawData().getContract(0));
    AccountCapsule caller = deposit.getAccount(callerAddressBytes);
    long storageFee = trx.getRawData().getFeeLimit();
    long cpuFee = (cpuUsage - cpuProcessor.getAccountLeftCpuInUsFromFreeze(caller))
        * Constant.DROP_PER_CPU_US;
    if (cpuFee > 0) {
      storageFee -= cpuFee;
    }
    long tryBuyStorage = storageMarket.tryBuyStorage(caller, storageFee);
    if (tryBuyStorage + caller.getStorageLeft() < useedStorageSize) {
      throw Program.Exception.notEnoughStorage();
    }
    trace.setBill(cpuUsage, useedStorageSize);
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

  public RuntimeSummary finalization() {
    return null;
  }

  public ProgramResult getResult() {
    return result;
  }

}
