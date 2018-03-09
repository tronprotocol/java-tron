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

package org.tron.core.config.args;

import com.google.protobuf.ByteString;
import org.apache.commons.lang3.StringUtils;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Protocol.AccountType;

public class Account {

  private String accountName;

  private String accountType;

  private String address;

  private String balance;

  public String getAddress() {
    return address;
  }

  public byte[] getAddressBytes() {
    return ByteArray.fromHexString(this.address);
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public long getBalance() {
    return Long.parseLong(balance);
  }


  public void setBalance(String balance) {
    this.balance = balance;
  }

  /**
   * get account from configuration.
   */
  public ByteString getAccountName() {
    if (StringUtils.isBlank(this.accountName)) {
      return ByteString.EMPTY;
    }

    return ByteString.copyFrom(ByteArray.fromString(this.accountName));
  }

  public void setAccountName(String accountName) {
    this.accountName = accountName;
  }

  /**
   * switch account type.
   */
  public AccountType getAccountType() {
    AccountType accountType;
    switch (this.accountType) {
      case "Normal":
        accountType = AccountType.Normal;
        break;
      case "AssetIssue":
        accountType = AccountType.AssetIssue;
        break;
      case "Contract":
        accountType = AccountType.Contract;
        break;
      default:
        throw new IllegalArgumentException("account type error.");
    }

    return accountType;
  }

  public void setAccountType(String accountType) {
    this.accountType = accountType;
  }
}
