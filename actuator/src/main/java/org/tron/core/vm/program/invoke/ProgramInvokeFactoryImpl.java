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
package org.tron.core.vm.program.invoke;

import static org.tron.common.runtime.InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE;
import static org.tron.common.runtime.InternalTransaction.TrxType.TRX_CONTRACT_CREATION_TYPE;
import static org.tron.common.utils.WalletUtil.generateContractAddress;

import lombok.extern.slf4j.Slf4j;
import org.spongycastle.util.Arrays;
import org.springframework.stereotype.Component;
import org.tron.common.runtime.InternalTransaction;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.utils.ByteUtil;
import org.tron.core.capsule.ContractCapsule;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.vm.program.Program;
import org.tron.core.vm.repository.Repository;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.contract.SmartContractOuterClass.CreateSmartContract;
import org.tron.protos.contract.SmartContractOuterClass.TriggerSmartContract;


/**
 * @author Roman Mandeleil
 * @since 08.06.2014
 */
@Component("ProgramInvokeFactory")
@Slf4j(topic = "vm")
public class ProgramInvokeFactoryImpl implements ProgramInvokeFactory {

  // Invocation by the wire tx
  @Override
  public ProgramInvoke createProgramInvoke(InternalTransaction.TrxType trxType,
      InternalTransaction.ExecutorType executorType, Transaction tx, long tokenValue, long tokenId,
      Block block,
      Repository deposit, long vmStartInUs,
      long vmShouldEndInUs, long energyLimit) throws ContractValidateException {
    byte[] contractAddress;
    byte[] ownerAddress;
    long balance;
    byte[] data;
    byte[] lastHash = null;
    byte[] coinbase = null;
    long timestamp = 0L;
    long number = -1L;

    if (trxType == TRX_CONTRACT_CREATION_TYPE) {
      CreateSmartContract contract = ContractCapsule.getSmartContractFromTransaction(tx);
      contractAddress = generateContractAddress(tx);
      ownerAddress = contract.getOwnerAddress().toByteArray();
      balance = deposit.getBalance(ownerAddress);
      data = ByteUtil.EMPTY_BYTE_ARRAY;
      long callValue = contract.getNewContract().getCallValue();

      switch (executorType) {
        case ET_NORMAL_TYPE:
        case ET_PRE_TYPE:
          if (null != block) {
            lastHash = block.getBlockHeader().getRawDataOrBuilder().getParentHash().toByteArray();
            coinbase = block.getBlockHeader().getRawDataOrBuilder().getWitnessAddress()
                .toByteArray();
            timestamp = block.getBlockHeader().getRawDataOrBuilder().getTimestamp() / 1000;
            number = block.getBlockHeader().getRawDataOrBuilder().getNumber();
          }
          break;
        default:
          break;
      }

      return new ProgramInvokeImpl(contractAddress, ownerAddress, ownerAddress, balance, callValue,
          tokenValue, tokenId, data, lastHash, coinbase, timestamp, number, deposit, vmStartInUs,
          vmShouldEndInUs, energyLimit);

    } else if (trxType == TRX_CONTRACT_CALL_TYPE) {
      TriggerSmartContract contract = ContractCapsule
          .getTriggerContractFromTransaction(tx);
      /***         ADDRESS op       ***/
      // YP: Get address of currently executing account.
      byte[] address = contract.getContractAddress().toByteArray();

      /***         ORIGIN op       ***/
      // YP: This is the sender of original transaction; it is never a contract.
      byte[] origin = contract.getOwnerAddress().toByteArray();

      /***         CALLER op       ***/
      // YP: This is the address of the account that is directly responsible for this execution.
      byte[] caller = contract.getOwnerAddress().toByteArray();

      /***         BALANCE op       ***/
      balance = deposit.getBalance(caller);

      /***        CALLVALUE op      ***/
      long callValue = contract.getCallValue();

      /***     CALLDATALOAD  op   ***/
      /***     CALLDATACOPY  op   ***/
      /***     CALLDATASIZE  op   ***/
      data = contract.getData().toByteArray();

      switch (executorType) {
        case ET_CONSTANT_TYPE:
          break;
        case ET_PRE_TYPE:
        case ET_NORMAL_TYPE:
          if (null != block) {
            /***    PREVHASH  op  ***/
            lastHash = block.getBlockHeader().getRawDataOrBuilder().getParentHash().toByteArray();
            /***   COINBASE  op ***/
            coinbase = block.getBlockHeader().getRawDataOrBuilder().getWitnessAddress()
                .toByteArray();
            /*** TIMESTAMP  op  ***/
            timestamp = block.getBlockHeader().getRawDataOrBuilder().getTimestamp() / 1000;
            /*** NUMBER  op  ***/
            number = block.getBlockHeader().getRawDataOrBuilder().getNumber();
          }
          break;
        default:
          break;
      }

      return new ProgramInvokeImpl(address, origin, caller, balance, callValue, tokenValue, tokenId,
          data,
          lastHash, coinbase, timestamp, number, deposit, vmStartInUs, vmShouldEndInUs,
          energyLimit);
    }
    throw new ContractValidateException("Unknown contract type");
  }

  /**
   * This invocation created for contract call contract
   */
  @Override
  public ProgramInvoke createProgramInvoke(Program program, DataWord toAddress,
      DataWord callerAddress,
      DataWord inValue, DataWord tokenValue, DataWord tokenId, long balanceInt, byte[] dataIn,
      Repository deposit, boolean isStaticCall, boolean byTestingSuite, long vmStartInUs,
      long vmShouldEndInUs, long energyLimit) {

    DataWord address = toAddress;
    DataWord origin = program.getOriginAddress();
    DataWord caller = callerAddress;
    DataWord balance = new DataWord(balanceInt);
    DataWord callValue = inValue;

    byte[] data = Arrays.clone(dataIn);
    DataWord lastHash = program.getPrevHash();
    DataWord coinbase = program.getCoinbase();
    DataWord timestamp = program.getTimestamp();
    DataWord number = program.getNumber();
    DataWord difficulty = program.getDifficulty();

    return new ProgramInvokeImpl(address, origin, caller, balance, callValue, tokenValue, tokenId,
        data, lastHash, coinbase, timestamp, number, difficulty,
        deposit, program.getCallDeep() + 1, isStaticCall, byTestingSuite, vmStartInUs,
        vmShouldEndInUs, energyLimit);
  }


}
