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
import org.tron.common.utils.StringUtil;
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

  /**
   * Account address is a 40-bits hex string.
   */
  public void setAddress(String address) {

    if (StringUtil.isHexString(address, 40)) {
      this.address = address;
    } else {
      throw new IllegalArgumentException(
          "The address(" + address + ") must be a 40-bit hexadecimal string.");
    }
  }

  public long getBalance() {
    return Long.parseLong(balance);
  }

  /**
   * Account balance is a long type.
   */
  public void setBalance(String balance) {
    try {
      Long.parseLong(balance);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Balance(" + balance + ") must be Long type.");
    }

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

  /**
   * Account name is a no-empty string.
   */
  public void setAccountName(String accountName) {
    if (StringUtils.isBlank(accountName)) {
      throw new IllegalArgumentException("Account name must be non-empty.");
    }

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
        throw new IllegalArgumentException("Account type error: Not Normal/AssetIssue/Contract");
    }

    return accountType;
  }

  /**
   * Account type: Normal/AssetIssue/Contract.
   */
  public void setAccountType(String accountType) {
    switch (accountType) {
      case "Normal":
      case "AssetIssue":
      case "Contract":
        this.accountType = accountType;
        break;
      default:
        throw new IllegalArgumentException("Account type error: Not Normal/AssetIssue/Contract");
    }
  }
}
