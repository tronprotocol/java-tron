package org.tron.common.runtime;

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
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.TronException;
import org.tron.protos.Contract;
import org.tron.protos.Contract.CreateSmartContract;
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

  PrecompiledContracts.PrecompiledContract precompiledContract = null;
  private ProgramResult result = new ProgramResult();


  private VM vm = null;
  private Program program = null;

  private InternalTransaction.TrxType trxType = TRX_UNKNOWN_TYPE;
  private InternalTransaction.ExecuterType executerType = ET_UNKNOWN_TYPE;


  /**
   * For block's trx run
   */
  public Runtime(Transaction tx, Block block, Deposit deosit,
      ProgramInvokeFactory programInvokeFactory) {
    this.trx = tx;
    if (Objects.nonNull(block)) {
      this.block = block;
      this.executerType = ET_NORMAL_TYPE;
    } else {
      this.block = Block.newBuilder().build();
      this.executerType = ET_PRE_TYPE;
    }
    this.deposit = deosit;
    this.programInvokeFactory = programInvokeFactory;

    Transaction.Contract.ContractType contractType = tx.getRawData().getContract(0).getType();
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

      boolean cumulativeCPUReached =
          oneTxCPULimitInUs.compareTo(blockCPULeftInUs) > 0;

      if (cumulativeCPUReached) {
        logger.error("cumulative CPU Reached");
        return true;
      }
    }

    return false;
  }

//  private long getAccountCPULimitInUs(List<AccountCapsule> accountCapsules) {
//    long ret = 0;
//    accountCapsules.forEach(accountCapsule ->
//        ret += getAccountCPULimit(accountCapsule));
//
//    return ret;
//
//  }

  private long getAccountCPULimitInUs(AccountCapsule... accountCapsules) {

    return 100000;

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
      long thisTxCPULimitInUs;
      long accountCPULimitInUs = getAccountCPULimitInUs(sender, creator);
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

      AccountCapsule creator = this.deposit
          .getAccount(newSmartContract.getOriginAddress().toByteArray());
      long thisTxCPULimitInUs;
      long accountCPULimitInUs = getAccountCPULimitInUs(creator);
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

    //
//    final Account account = new Account();
//    account.setAccountName(asset.get("accountName").unwrapped().toString());
//    account.setAccountType(asset.get("accountType").unwrapped().toString());
//    account.setAddress(Wallet.decodeFromBase58Check(asset.get("address").unwrapped().toString()));
//    account.setBalance(asset.get("balance").unwrapped().toString());
//    return account;
    //

    deposit.createAccount(contractAddress, ByteString.copyFromUtf8("jack"),
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
        spendUsage(true);

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
      logger.error(e.getMessage());
      runtimeError = e.getMessage();
    } catch (TronException e) {
      spendUsage(false);
      logger.error(e.getMessage());
      runtimeError = e.getMessage();
    }
    //todo catch over resource exception
//    catch (Exception e) {
//      logger.error(e.getMessage());
//      runtimeError = e.getMessage()
//  }

  }

  private void spendUsage(boolean spandStorage) {
    long cpuUsage, storageUsage;
    storageUsage = 0;
    long now = System.nanoTime() / 1000;
    cpuUsage = now - program.getVmStartInUs();
    if (executerType == ET_NORMAL_TYPE) {
      /*
       * trx.getCpuRecipt
       *
       * */
    }
    ContractCapsule contract = deposit.getContract(result.getContractAddress());
    ByteString originAddress = contract.getInstance().getOriginAddress();
    AccountCapsule origin = deposit.getAccount(originAddress.toByteArray());

    byte[] callerAddressBytes = TransactionCapsule.getOwner(trx.getRawData().getContract(0));
    AccountCapsule caller = deposit.getAccount(callerAddressBytes);

    spendCpuUsage(cpuUsage, origin, caller);
    if (spandStorage) {
      spendStorageUsage(storageUsage, origin, caller);
    }
  }

  private void spendCpuUsage(long cpuUsage, AccountCapsule origin, AccountCapsule caller) {
    //TODO get origin cpu
    long originCpu;
    originCpu = getCpuByAccount(origin);

    //TODO get caller cpu
    long callerCpu;
    callerCpu = getCpuByAccount(caller);
    //TODO get caller cpulimit（trx）
    long callerCpuLimit;
    callerCpuLimit = 0;
    cpuUsage = getCpuUsageLess(cpuUsage, origin, originCpu);
    if (cpuUsage <= 0) {
      return;
    }
    cpuUsage = getCpuUsageLess(cpuUsage, caller, callerCpu);
    if (cpuUsage <= 0) {
      return;
    }
    long overCpu = getCpuByLimit(callerCpuLimit);
    cpuUsage = getCpuUsageLess(cpuUsage, caller, overCpu);
    if (cpuUsage <= 0) {
      return;
    }
  }

  private long getCpuUsageLess(long cpuUsage, AccountCapsule origin, long cpu) {
    if (cpuUsage <= cpu) {
      //todo origin add cpu -cpuUsage
    } else {
      //todo origin add cpu -cpu
    }
    deposit.getDbManager().getAccountStore().put(origin.getAddress().toByteArray(), origin);
    return cpuUsage - cpu;
  }

  private long getCpuByLimit(long callerCpuLimit) {

    // TODO conversion limit(trx) to cpu
    return 0;
  }

  private void spendStorageUsage(long storageUsage, AccountCapsule origin, AccountCapsule caller) {
    //TODO get origin  storage
    long orginStorage;
    orginStorage = getStorageByAccount(origin);

    //TODO get caller  storage
    long callerStorage;
    callerStorage = getStorageByAccount(caller);
    //TODO get caller storagelimit（trx）
    long callerStorageLimit;
    callerStorageLimit = 0;
    //storage 大于 1+2+3  不执行commit result 设置Error 正常退出 processesTransaction
    storageUsage = getStorageUsageLess(storageUsage, origin, orginStorage);
    if (storageUsage <= 0) {
      return;
    }
    storageUsage = getStorageUsageLess(storageUsage, caller, callerStorage);
    if (storageUsage <= 0) {
      return;
    }
    long overStorage = getStorageByLimit(callerStorageLimit);
    storageUsage = getStorageUsageLess(storageUsage, caller, overStorage);
    if (storageUsage <= 0) {
      return;
    }
  }

  private long getStorageByLimit(long callerCpuLimit) {

    // TODO conversion limit(trx) to cpu
    return 0;
  }

  private long getStorageUsageLess(long storageUsage, AccountCapsule origin, long storage) {
    if (storageUsage <= storage) {
      //todo origin add storage -storageUsage
    } else {
      //todo origin add storage -storage
    }
    deposit.getDbManager().getAccountStore().put(origin.getAddress().toByteArray(), origin);
    return storageUsage - storage;
  }

  private long getStorageByAccount(AccountCapsule origin) {
    // TODO get Storage by account
    return 0;
  }

  private long getCpuByAccount(AccountCapsule origin) {
    // TODO get cpu by account
    return 0;
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
