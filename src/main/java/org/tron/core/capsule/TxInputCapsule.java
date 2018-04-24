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

import com.google.protobuf.ByteString;
import org.tron.protos.Protocol.TXInput;

public class TxInputCapsule implements ProtoCapsule<TXInput> {

  private TXInput txInput;

  /**
   * constructor TxInputCapsule.
   *
   * @param txId byte[] txId
   * @param vout int vout
   * @param signature byte[] signature
   * @param pubKey byte[] pubKey
   */
  public TxInputCapsule(byte[] txId, long vout, byte[]
      signature, byte[] pubKey) {
    TXInput.raw txInputRaw = TXInput.raw.newBuilder()
        .setTxID(ByteString.copyFrom(txId))
        .setVout(vout)
        .setPubKey(ByteString.copyFrom(pubKey)).build();

    this.txInput = TXInput.newBuilder()
        .setRawData(txInputRaw)
        .setSignature(ByteString.copyFrom(signature))
        .build();

  }

  public TXInput getTxInput() {
    return txInput;
  }

  public boolean validate() {
    return true;
  }

  @Override
  public byte[] getData() {
    return new byte[0];
  }

  @Override
  public TXInput getInstance() {
    return this.txInput;
  }
}
