package org.tron.core.db.accountstate;

import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.StringUtil;
import org.tron.protos.Protocol.Account;

@Slf4j(topic = "AccountState")
public class AccountStateEntity {

  private Account account;

  public AccountStateEntity() {
  }

  public AccountStateEntity(Account account) {
    Account.Builder builder = Account.newBuilder();
    builder.setAddress(account.getAddress());
    builder.setBalance(account.getBalance());
    builder.setAllowance(account.getAllowance());
    this.account = builder.build();
  }

  public static AccountStateEntity parse(byte[] data) {
    try {
      return new AccountStateEntity().setAccount(Account.parseFrom(data));
    } catch (Exception e) {
      logger.error("parse to AccountStateEntity error! reason: {}", e.getMessage());
    }
    return null;
  }

  public Account getAccount() {
    return account;
  }

  public AccountStateEntity setAccount(Account account) {
    this.account = account;
    return this;
  }

  public byte[] toByteArrays() {
    return account.toByteArray();
  }

  @Override
  public String toString() {
    return "address:" + StringUtil.encode58Check(account
        .getAddress().toByteArray()) + "; " + account
        .toString();
  }
}
