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

package org.tron.core.actuator;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.contract.AssetIssueContractOuterClass.AssetIssueContract;

@Slf4j(topic = "actuator")
public class MarketCancelOrderActuator extends AbstractActuator {

  public MarketCancelOrderActuator() {
    super(ContractType.MakerCancelOrderContract, AssetIssueContract.class);
  }

  @Override
  public boolean execute(Object result) throws ContractExeException {

    return true;
  }

  @Override
  public boolean validate() throws ContractValidateException {
    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return any.unpack(AssetIssueContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 0L;
  }

}
