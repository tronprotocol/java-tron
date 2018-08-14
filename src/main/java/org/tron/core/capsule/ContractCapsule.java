/*
 * java-tron is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-tron is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.core.capsule;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.Sha256Hash;
import org.tron.protos.Contract.CreateSmartContract;
import org.tron.protos.Contract.TriggerSmartContract;
import org.tron.protos.Protocol.SmartContract;
import org.tron.protos.Protocol.Transaction;

@Slf4j
public class ContractCapsule implements ProtoCapsule<SmartContract> {

  private SmartContract smartContract;

  /**
   * constructor TransactionCapsule.
   */
  public ContractCapsule(SmartContract smartContract) {
    this.smartContract = smartContract;
  }

  public ContractCapsule(byte[] data) {
    try {
      this.smartContract = SmartContract.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      // logger.debug(e.getMessage());
    }
  }

  public static CreateSmartContract getSmartContractFromTransaction(Transaction trx) {
    try {
      Any any = trx.getRawData().getContract(0).getParameter();
      CreateSmartContract createSmartContract = any.unpack(CreateSmartContract.class);
      return createSmartContract;
    } catch (InvalidProtocolBufferException e) {
      return null;
    }
  }

  public static TriggerSmartContract getTriggerContractFromTransaction(Transaction trx) {
    try {
      Any any = trx.getRawData().getContract(0).getParameter();
      TriggerSmartContract contractTriggerContract = any.unpack(TriggerSmartContract.class);
      return contractTriggerContract;
    } catch (InvalidProtocolBufferException e) {
      return null;
    }
  }

  public Sha256Hash getHash() {
    byte[] transBytes = this.smartContract.toByteArray();
    return Sha256Hash.of(transBytes);
  }

  public Sha256Hash getCodeHash() {
    byte[] bytecode = smartContract.getBytecode().toByteArray();
    return Sha256Hash.of(bytecode);
  }

  @Override
  public byte[] getData() {
    return this.smartContract.toByteArray();
  }

  @Override
  public SmartContract getInstance() {
    return this.smartContract;
  }

  @Override
  public String toString() {
    return this.smartContract.toString();
  }

  public byte[] getOriginAddress() {
    return this.smartContract.getOriginAddress().toByteArray();
  }

  public long getConsumeUserResourcePercent() {
    return this.smartContract.getConsumeUserResourcePercent();
  }
}
