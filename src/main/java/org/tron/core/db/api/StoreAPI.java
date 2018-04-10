package org.tron.core.db.api;

import static com.googlecode.cqengine.query.QueryFactory.equal;

import com.googlecode.cqengine.IndexedCollection;
import com.googlecode.cqengine.resultset.ResultSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.db.api.index.AccountIndex;
import org.tron.protos.Protocol.Account;

@Component
@Slf4j
public class StoreAPI {

  @Autowired
  private IndexHelper indexHelper;

  public Account getAccountByAddress(String address) {
    IndexedCollection<Account> accountIndex = indexHelper.getAccountIndex();
    ResultSet<Account> resultSet = accountIndex
        .retrieve(equal(AccountIndex.Account_ADDRESS, address));
    if (resultSet.isEmpty()) {
      return null;
    }

    return resultSet.uniqueResult();
  }
}
