package org.tron.common.runtime;

import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.runtime.config.SystemProperties;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.runtime.vm.PrecompiledContracts;
import org.tron.common.runtime.vm.VM;
import org.tron.common.runtime.vm.program.InternalTransaction;
import org.tron.common.runtime.vm.program.Program;
import org.tron.common.runtime.vm.program.ProgramPrecompile;
import org.tron.common.runtime.vm.program.ProgramResult;
import org.tron.common.runtime.vm.program.invoke.ProgramInvoke;
import org.tron.common.runtime.vm.program.invoke.ProgramInvokeFactory;
import org.tron.core.actuator.Actuator;
import org.tron.core.actuator.ActuatorFactory;
import org.tron.core.capsule.*;
import org.tron.core.db.Manager;
import org.tron.protos.Contract;
import org.tron.protos.Contract.ContractDeployContract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;

import javax.xml.crypto.Data;
import java.util.List;

import static org.apache.commons.lang3.ArrayUtils.isEmpty;
import static org.tron.common.runtime.vm.program.InternalTransaction.ExecuterType.*;
import static org.tron.common.runtime.vm.program.InternalTransaction.TrxType.*;

public class Runtime {

    private static final Logger logger = LoggerFactory.getLogger("execute");

    SystemProperties config;

    private Transaction trx;
    private Block block = null;
    private Manager dbManager;
    private ProgramInvokeFactory programInvokeFactory = null;
    private String runtimeError;

    PrecompiledContracts.PrecompiledContract precompiledContract = null;
    private ProgramResult result = new ProgramResult();
    private VM vm = null;
    private Program program = null;

    private InternalTransaction.TrxType trxType = TRX_UNKNOWN_TYPE;
    private InternalTransaction.ExecuterType executerType = ET_UNKNOWN_TYPE;


    /**
     * For block's trx run
     *
     * @param tx
     * @param block
     * @param dbManager
     * @param programInvokeFactory
     */
    public Runtime(Transaction tx, Block block, Manager dbManager,
                   ProgramInvokeFactory programInvokeFactory) {
        this.trx = tx;
        this.block = block;
        this.dbManager = dbManager;
        this.programInvokeFactory = programInvokeFactory;
        this.executerType = ET_NORMAL_TYPE;

        Transaction.Contract.ContractType contractType = tx.getRawData().getContract(0).getType();
        switch (contractType.getNumber()) {
            case Transaction.Contract.ContractType.TriggerContract_VALUE:
                trxType = TRX_CONTRACT_CALL_TYPE;
                break;
            case Transaction.Contract.ContractType.DeployContract_VALUE:
                trxType = TRX_CONTRACT_CREATION_TYPE;
                break;
            default:
                trxType = TRX_PRECOMPILED_TYPE;

        }
    }

    /**
     * For pre trx run
     *
     * @param tx
     * @param dbManager
     * @param programInvokeFactory
     */
    public Runtime(Transaction tx, Manager dbManager, ProgramInvokeFactory programInvokeFactory) {
        this.trx = tx;
        this.dbManager = dbManager;
        this.programInvokeFactory = programInvokeFactory;
        this.executerType = ET_PRE_TYPE;
        Transaction.Contract.ContractType contractType = tx.getRawData().getContract(0).getType();
        switch (contractType.getNumber()) {
            case Transaction.Contract.ContractType.TriggerContract_VALUE:
                trxType = TRX_CONTRACT_CALL_TYPE;
                break;
            case Transaction.Contract.ContractType.DeployContract_VALUE:
                trxType = TRX_CONTRACT_CREATION_TYPE;
                break;
            default:
                trxType = TRX_PRECOMPILED_TYPE;

        }
    }

    /**
     * For constant trx
     *
     * @param tx
     * @param programInvokeFactory
     */
    public Runtime(Transaction tx, ProgramInvokeFactory programInvokeFactory, Manager manager) {
        trx = tx;
        this.dbManager = manager;
        this.programInvokeFactory = programInvokeFactory;
        executerType = ET_CONSTANT_TYPE;
        trxType = TRX_CONTRACT_CALL_TYPE;

    }


    public void precompiled() {

        try {
            TransactionCapsule trxCap = new TransactionCapsule(trx);
            final List<Actuator> actuatorList = ActuatorFactory.createActuator(trxCap, dbManager);
            TransactionResultCapsule ret = new TransactionResultCapsule();

            for (Actuator act : actuatorList) {
                act.validate();
                act.execute(ret);
                trxCap.setResult(ret);
            }
        } catch (RuntimeException e) {
            program.setRuntimeFailure(e);
        } catch (Exception e) {
            program.setRuntimeFailure(new RuntimeException(e.getMessage()));
        } finally {

        }
    }

    public void execute() {
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

    private void call() {
        Contract.ContractTriggerContract contract = ContractCapsule.getTriggerContractFromTransaction(trx);
        if (contract == null) return;

        byte[] contractAddress = contract.getContractAddress().toByteArray();
        byte[] code = dbManager.getCodeStore().get(contractAddress).getData();
        if (isEmpty(code)) {

        } else {
            ProgramInvoke programInvoke = programInvokeFactory.createProgramInvoke(TRX_CONTRACT_CALL_TYPE, executerType, trx,
                    block, dbManager);
            this.vm = new VM(config);
            this.program = new Program(dbManager.getAccountStore().get(contractAddress).getInstance().getCodeHash().toByteArray(),
                    code, programInvoke, new InternalTransaction(trx), config);
        }

        // transfer
        /*
        DataWord sender = this.program.getCallerAddress();
        DataWord toAddress = this.program.
        DataWord amountData = this.program.getCallValue();
        long amount = amountData.longValue();
        this.program.getCallerAddress()
        */


    }

    /*
     **/
    private void create() {
        ContractDeployContract contract = ContractCapsule.getDeployContractFromTransaction(trx);

        // Create a Contract Account by ownerAddress or If the address exist, random generate one
        byte[] code = contract.getBytecode().toByteArray();
        ContractDeployContract.ABI abi = contract.getAbi();
        byte[] ownerAddress = contract.getOwnerAddress().toByteArray();
        ByteString newContractAddress = contract.getContractAddress();

        // crate vm to constructor smart contract
        try {
            byte[] ops = contract.getBytecode().toByteArray();
            InternalTransaction internalTransaction = new InternalTransaction(trx);
            ProgramInvoke programInvoke = programInvokeFactory.createProgramInvoke(TRX_CONTRACT_CREATION_TYPE, executerType, trx,
                    block, dbManager);
            this.vm = new VM(config);
            this.program = new Program(ops, programInvoke, internalTransaction, config);
        } catch(Exception e) {
            logger.error(e.getMessage());
            return;
        }

        AccountCapsule accountCapsule = new AccountCapsule(newContractAddress, Protocol.AccountType.Contract);
        dbManager.getAccountStore().put(newContractAddress.toByteArray(), accountCapsule);
        dbManager.getContractStore().put(newContractAddress.toByteArray(), new ContractCapsule(trx));
        dbManager.getCodeStore().put(newContractAddress.toByteArray(), new CodeCapsule(ProgramPrecompile.getCode(code)));
    }

    public void go() {

        try {
            if (vm != null) {
                if (config.vmOn()) {
                    vm.play(program);
                }

                result = program.getResult();
                if (result.getException() != null || result.isRevert()) {
                    result.getDeleteAccounts().clear();
                    result.getLogInfoList().clear();
                    result.resetFutureRefund();

                    if (result.getException() != null) {
                        throw result.getException();
                    } else {
                        runtimeError = "REVERT opcode executed";
                    }
                }
            }
        } catch(Exception e) {
            logger.error(e.getMessage());
            runtimeError = e.getMessage();
        }
    }

    public ProgramResult getResult() {
        return result;
    }
}
