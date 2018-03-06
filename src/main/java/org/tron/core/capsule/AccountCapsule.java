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
import com.google.protobuf.InvalidProtocolBufferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.protos.Protocal.Account;
import org.tron.protos.Protocal.AccountType;

public class AccountCapsule {

  protected static final Logger logger = LoggerFactory.getLogger("AccountCapsule");

  private byte[] data;

  private Account account;

  private boolean unpacked;

  private synchronized void unPack() {
    if (unpacked) {
      return;
    }

    try {
      this.account = Account.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage());
    }

    unpacked = true;
  }

  /**
   * initial account capsule.
   */
  public AccountCapsule(AccountType accountType, ByteString address, long balance) {
    this.account = Account.newBuilder()
        .setType(accountType)
        .setAddress(address)
        .setBalance(balance)
        .build();
  }

  public AccountCapsule() {
    unpacked = true;
  }

  private void pack() {
    if (data == null) {
      this.data = this.account.toByteArray();
    }
  }

  public byte[] getData() {
    pack();
    return data;
  }

  public ByteString getAddress() {
    unPack();
    return this.account.getAddress();
  }

  @Override
  public String toString() {
    unPack();
    return this.account.toString();
  }
}
