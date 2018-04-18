package org.tron.core.db.api.index;

import static com.googlecode.cqengine.query.QueryFactory.attribute;

import com.googlecode.cqengine.attribute.Attribute;
import com.googlecode.cqengine.index.navigable.NavigableIndex;
import com.googlecode.cqengine.index.suffix.SuffixTreeIndex;
import com.googlecode.cqengine.persistence.Persistence;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Protocol.Account;

@Component
@Slf4j
public class AccountIndex extends AbstractIndex<Account> {

  public static final Attribute<Account, String> Account_ADDRESS =
      attribute("account address",
          account -> ByteArray.toHexString(account.getAddress().toByteArray()));
  public static final Attribute<Account, Long> Account_BALANCE =
      attribute("account balance", Account::getBalance);

  public AccountIndex() {
    super();
  }

  public AccountIndex(Persistence<Account, ? extends Comparable> persistence) {
    super(persistence);
  }

  @PostConstruct
  public void init() {
    addIndex(SuffixTreeIndex.onAttribute(Account_ADDRESS));
    addIndex(NavigableIndex.onAttribute(Account_BALANCE));
  }

}
