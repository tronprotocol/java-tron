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
import org.tron.protos.contract.AssetIssueContractOuterClass.AssetIssueContract;
import org.tron.protos.contract.StableMarketContractOuterClass.StableCoinInfo;

@Slf4j(topic = "capsule")
public class StableCoinInfoCapsule implements ProtoCapsule<StableCoinInfo> {

  private StableCoinInfo stableCoinInfo;

  /**
   * get asset issue contract from bytes data.
   */
  public StableCoinInfoCapsule(byte[] data) {
    try {
      this.stableCoinInfo = StableCoinInfo.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage());
    }
  }

  public StableCoinInfoCapsule(StableCoinInfo stableCoinInfo) {
    this.stableCoinInfo = stableCoinInfo;
  }

  public StableCoinInfoCapsule(AssetIssueContract assetIssueContract, long tobinFee) {
    this.stableCoinInfo = StableCoinInfo.newBuilder()
            .setAssetIssue(assetIssueContract).setTobinFee(String.valueOf(tobinFee)).build();
  }

  public byte[] getData() {
    return this.stableCoinInfo.toByteArray();
  }

  @Override
  public StableCoinInfo getInstance() {
    return this.stableCoinInfo;
  }

  @Override
  public String toString() {
    return this.stableCoinInfo.toString();
  }

  public String getId() {
    return this.stableCoinInfo.getAssetIssue().getId();
  }

}
