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
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.protos.Protocal.Account;
import org.tron.protos.Protocal.Account.Vote;
import org.tron.protos.Protocal.AccountType;

public class AccountCapsule implements ProtoCapsule<Account> {

  protected static final Logger logger = LoggerFactory.getLogger("AccountCapsule");

  private Account account;

  public AccountCapsule(byte[] data) {
    try {
      this.account = Account.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage());
    }
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

  public AccountCapsule(ByteString address, ByteString accountName,
      AccountType accountType, int typeValue) {
    this.account = Account.newBuilder()
        .setType(accountType)
        .setAddress(address)
        .setTypeValue(typeValue)
        .build();
  }

  public AccountCapsule(Account account) {
    this.account = account;
  }

  public byte[] getData() {
    return this.account.toByteArray();
  }

  @Override
  public Account getInstance() {
    return this.account;
  }

  public ByteString getAddress() {
    return this.account.getAddress();
  }

  public AccountType getType() {
    return this.account.getType();
  }


  public long getBalance() {
    return this.account.getBalance();
  }

  public void setBalance(long balance) {
    this.account = this.account.toBuilder().setBalance(balance).build();
  }

  @Override
  public String toString() {
    return this.account.toString();
  }


  public void addVotes(ByteString voteAddress, long voteAdd) {
    this.account = this.account.toBuilder()
        .addVotes(Vote.newBuilder().setVoteAddress(voteAddress).setVoteCount(voteAdd).build())
        .build();
  }

  public List<Vote> getVotesList() {
    return this.account.getVotesList();
  }
}
