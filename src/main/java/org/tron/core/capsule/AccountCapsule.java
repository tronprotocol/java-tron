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
import org.tron.protos.Contract.AccountCreateContract;
import org.tron.protos.Protocal.Account;
import org.tron.protos.Protocal.Account.Vote;
import org.tron.protos.Protocal.AccountType;

public class AccountCapsule implements ProtoCapsule<Account> {

  protected static final Logger logger = LoggerFactory.getLogger("AccountCapsule");

  private byte[] data;

  private Account account;

  private boolean unpacked;

  public AccountCapsule(final byte[] data) {
    this.data = data;
    this.unpacked = false;
  }


  private synchronized void unPack() {
    if (this.unpacked) {
      return;
    }

    try {
      this.account = Account.parseFrom(this.data);
    } catch (final InvalidProtocolBufferException e) {
      logger.debug(e.getMessage());
    }

    this.unpacked = true;
  }

  /**
   * initial account capsule.
   */
  public AccountCapsule(final AccountType accountType, final ByteString address,
      final long balance) {
    this.account = Account.newBuilder()
        .setType(accountType)
        .setAddress(address)
        .setBalance(balance)
        .build();
    this.unpacked = true;
  }

  public AccountCapsule(final AccountCreateContract contract) {
    this.account = Account.newBuilder()
        .setType(contract.getType())
        .setAddress(contract.getOwnerAddress())
        .setTypeValue(contract.getTypeValue())
        .build();
    this.unpacked = true;
  }

  public AccountCapsule(final ByteString address, final ByteString accountName,
      final AccountType accountType) {
    this.account = Account.newBuilder()
        .setType(accountType)
        .setAddress(address)
        .build();
    this.unpacked = true;
  }

  public AccountCapsule(final Account account) {
    this.account = account;
    this.unpacked = true;
  }

  public AccountCapsule() {
    this.unpacked = true;
  }

  private void pack() {
    if (this.data == null) {
      this.data = this.account.toByteArray();
    }
  }

  private void clearData() {
    this.data = null;
    this.unpacked = true;
  }

  public byte[] getData() {
    this.pack();
    return this.data;
  }

  @Override
  public Account getInstance() {
    return this.account;
  }

  public ByteString getAddress() {
    this.unPack();
    return this.account.getAddress();
  }

  public AccountType getType() {
    unPack();
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
    this.unPack();
    return this.account.toString();
  }

  public void addVotes(final ByteString voteAddress, final long voteAdd) {
    this.unPack();
    this.account = this.account.toBuilder()
        .addVotes(Vote.newBuilder().setVoteAddress(voteAddress).setVoteCount(voteAdd).build())
        .build();
    this.clearData();
  }

  public List<Vote> getVotesList() {
    this.unPack();
    return this.account.getVotesList();
  }

  public long getShare() {
    return this.account.getBalance();
  }

}
