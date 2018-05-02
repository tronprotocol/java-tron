package org.tron.core.db.api.index;

import static com.googlecode.cqengine.query.QueryFactory.attribute;

import com.googlecode.cqengine.attribute.Attribute;
import com.googlecode.cqengine.index.navigable.NavigableIndex;
import com.googlecode.cqengine.index.suffix.SuffixTreeIndex;
import com.googlecode.cqengine.persistence.Persistence;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.db.TronDatabase;
import org.tron.core.db.common.WrappedByteArray;
import org.tron.protos.Protocol.Account;

@Component
@Slf4j
public class AccountIndex extends AbstractIndex<AccountCapsule, Account> {

  public static Attribute<WrappedByteArray, String> Account_ADDRESS;
  public static Attribute<WrappedByteArray, Long> Account_BALANCE;

  @Autowired
  public AccountIndex(@Qualifier("accountStore") final TronDatabase<AccountCapsule> database) {
    super();
    this.database = database;
  }

  public AccountIndex(final TronDatabase<AccountCapsule> database,
      Persistence<WrappedByteArray, ? extends Comparable> persistence) {
    super(persistence);
    this.database = database;
  }

  @PostConstruct
  public void init() {
    index.addIndex(SuffixTreeIndex.onAttribute(Account_ADDRESS));
    index.addIndex(NavigableIndex.onAttribute(Account_BALANCE));
    fill();
  }

  @Override
  protected void setAttribute() {
    Account_ADDRESS = attribute("account address",
        bytes -> ByteArray.toHexString(bytes.getBytes()));
    Account_BALANCE = attribute("account balance",
        bytes -> {
          try {
            Account account = getObject(bytes);
            return account.getBalance();
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });

  }
}
