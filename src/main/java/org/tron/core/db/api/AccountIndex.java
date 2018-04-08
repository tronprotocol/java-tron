package org.tron.core.db.api;

import static com.googlecode.cqengine.query.QueryFactory.attribute;

import com.googlecode.cqengine.attribute.Attribute;
import com.googlecode.cqengine.persistence.Persistence;
import org.tron.protos.Protocol.Account;

public class AccountIndex extends AbstractIndex<Account> {

  private static final Attribute<Account, String> ADDRESS =
      attribute("account address", account -> account.getAddress().toStringUtf8());
  private static final Attribute<Account, String> NAME =
      attribute("account name", account -> account.getAccountName().toStringUtf8());
  private static final Attribute<Account, Long> BALANCE =
      attribute("account balance", Account::getBalance);

  public AccountIndex(Persistence<Account, ? extends Comparable> persistence) {
    super(persistence);
  }

}
