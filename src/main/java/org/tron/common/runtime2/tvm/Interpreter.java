package org.tron.common.runtime2.tvm;

import com.google.protobuf.ByteString;
import org.tron.common.runtime.vm.program.InternalTransaction;
import org.tron.core.Wallet;
import org.tron.core.capsule.ContractCapsule;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;

public class Interpreter {

  public void play(Program program, ProgramEnv env) throws ContractValidateException {
    Protocol.Transaction trx = program.getInternalTransaction().getTransaction();
    if (program.getTrxType() == InternalTransaction.TrxType.TRX_CONTRACT_CREATION_TYPE) {
      byte[] contractAddress = Wallet.generateContractAddress(trx);
      // insure the new contract address haven't exist
      if (env.getStorage().getAccount(contractAddress) != null) {
        throw new ContractValidateException(
                "Trying to create a contract with existing contract address: " + Wallet
                        .encode58Check(contractAddress));
      }
      //if not created by smartcontract then
      Contract.CreateSmartContract contract = ContractCapsule.getSmartContractFromTransaction(trx);
      Protocol.SmartContract newSmartContract = contract.getNewContract();
      newSmartContract = newSmartContract.toBuilder()
              .setContractAddress(ByteString.copyFrom(contractAddress)).build();

      env.getStorage().createAccount(contractAddress, newSmartContract.getName(),
              Protocol.AccountType.Contract);

      env.getStorage().createContract(contractAddress, new ContractCapsule(newSmartContract));
      program.getProgramResult().setContractAddress(contractAddress);

      //transefer Trx and trc10
      // transfer from callerAddress to contractAddress according to callValue
      if (program.getCallValue() > 0) {
        ProgramEnv.transfer(env.getStorage(), program.getCallerAddress(), contractAddress, program.getCallValue());
      }
      if (program.getTokenValue() > 0) {
        ProgramEnv.transferToken(env.getStorage(), program.getCallerAddress(), contractAddress, String.valueOf(program.getTokenId()),
                program.getTokenValue());
      }

    } else {

    }


  }

}
