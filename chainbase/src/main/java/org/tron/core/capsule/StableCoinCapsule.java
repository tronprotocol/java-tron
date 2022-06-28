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

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.ByteArray;
import org.tron.protos.contract.StableMarketContract.StableCoinContract;

@Slf4j(topic = "capsule")
public class StableCoinCapsule implements ProtoCapsule<StableCoinContract> {

  private StableCoinContract stableCoinContract;

  /**
   * get asset issue contract from bytes data.
   */
  public StableCoinCapsule(byte[] data) {
    try {
      this.stableCoinContract = StableCoinContract.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage());
    }
  }

  public StableCoinCapsule(StableCoinContract stableCoinContract) {
    this.stableCoinContract = stableCoinContract;
  }

  public StableCoinCapsule(byte[] tokenId, long tobinFee) {
    this.stableCoinContract = StableCoinContract.newBuilder()
            .setAssetId(ByteArray.toStr(tokenId)).setTobinFee(String.valueOf(tobinFee)).build();
  }

  public byte[] getData() {
    return this.stableCoinContract.toByteArray();
  }

  @Override
  public StableCoinContract getInstance() {
    return this.stableCoinContract;
  }

  @Override
  public String toString() {
    return this.stableCoinContract.toString();
  }

  public String getId() {
    return this.stableCoinContract.getAssetId();
  }

}
