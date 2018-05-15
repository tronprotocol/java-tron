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
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.ECKey.ECDSASignature;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.Wallet;
import org.tron.protos.Contract.ContractCallContract;
import org.tron.protos.Contract.ContractCreationContract;
import org.tron.protos.Protocol.Transaction;

/**
 *
 * Created by Guo Yonggang on 04.14.2018
 */
@Slf4j
public class ContractCapsule implements ProtoCapsule<Transaction> {

  private Transaction transaction;

  /**
   * constructor TransactionCapsule.
   */
  public ContractCapsule(Transaction trx) {
    this.transaction = trx;
  }

  public ContractCapsule(byte[] data) {
    try {
      this.transaction = Transaction.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      // logger.debug(e.getMessage());
    }
  }

  public static ContractCreationContract getCreationContractFromTransaction(Transaction trx) {
    try {
      Any any = trx.getRawData().getContract(0).getParameter();
      ContractCreationContract contractCreationContract = any.unpack(ContractCreationContract.class);
      return contractCreationContract;
    } catch (InvalidProtocolBufferException e) {
      return null;
    }
  }

  public static ContractCallContract getCallContractFromTransaction(Transaction trx) {
    try {
      Any any = trx.getRawData().getContract(0).getParameter();
      ContractCallContract contractCallContract = any.unpack(ContractCallContract.class);
      return contractCallContract;
    } catch (InvalidProtocolBufferException e) {
      return null;
    }
  }

  public Sha256Hash getHash() {
    byte[] transBytes = this.transaction.toByteArray();
    return Sha256Hash.of(transBytes);
  }

  public Sha256Hash getRawHash() {
    return Sha256Hash.of(this.transaction.getRawData().toByteArray());
  }

  public Sha256Hash getCodeHash() {
    try {
      Any any = transaction.getRawData().getContract(0).getParameter();
      ContractCreationContract contractCreationContract = any.unpack(ContractCreationContract.class);
      byte[] bytecode = contractCreationContract.getBytecode().toByteArray();
      return Sha256Hash.of(bytecode);
    } catch (InvalidProtocolBufferException e) {
      return null;
    }
  }
  
  public void sign(byte[] privateKey) {
    ECKey ecKey = ECKey.fromPrivate(privateKey);
    ECDSASignature signature = ecKey.sign(getRawHash().getBytes());
    ByteString sig = ByteString.copyFrom(signature.toBase64().getBytes());
    this.transaction = this.transaction.toBuilder().addSignature(sig).build();
  }

  public static String getBase64FromByteString(ByteString sign) {
    byte[] r = sign.substring(0, 32).toByteArray();
    byte[] s = sign.substring(32, 64).toByteArray();
    byte v = sign.byteAt(64);
    if (v < 27) {
      v += 27; //revId -> v
    }
    ECDSASignature signature = ECDSASignature.fromComponents(r, s, v);
    return signature.toBase64();
  }


  public Sha256Hash getContractId() {
    return Sha256Hash.of(this.transaction.toByteArray());
  }

  @Override
  public byte[] getData() {
    return this.transaction.toByteArray();
  }

  @Override
  public Transaction getInstance() {
    return this.transaction;
  }

  @Override
  public String toString() {
    return this.transaction.toString();
  }
}
