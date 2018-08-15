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
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.crypto.ECKey.ECDSASignature;
import org.tron.common.utils.Sha256Hash;
import org.tron.protos.Contract.TriggerSmartContract;
import org.tron.protos.Protocol.SmartContract;
import org.tron.protos.Protocol.Transaction;

@Slf4j
public class CodeCapsule implements ProtoCapsule<byte[]> {

  private byte[] code;

  public CodeCapsule(byte[] code) {
    this.code = code;
  }

  public CodeCapsule(ByteString bs) {
    this.code = bs.toByteArray();
  }

  public static SmartContract getCreationContractFromTransaction(Transaction trx) {
    try {
      Any any = trx.getRawData().getContract(0).getParameter();
      SmartContract contractCreationContract = any.unpack(SmartContract.class);
      return contractCreationContract;
    } catch (InvalidProtocolBufferException e) {
      return null;
    }
  }

  public static TriggerSmartContract getCallContractFromTransaction(Transaction trx) {
    try {
      Any any = trx.getRawData().getContract(0).getParameter();
      TriggerSmartContract contractCallContract = any.unpack(TriggerSmartContract.class);
      return contractCallContract;
    } catch (InvalidProtocolBufferException e) {
      return null;
    }
  }

  public Sha256Hash getHash() {
    return Sha256Hash.of(this.code);
  }

  public Sha256Hash getCodeHash() {
    return Sha256Hash.of(this.code);
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

  @Override
  public byte[] getData() {
    return this.code;
  }

  @Override
  public byte[] getInstance() {
    return this.code;
  }

  @Override
  public String toString() {
    return Arrays.toString(this.code);
  }
}
