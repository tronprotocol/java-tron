package org.tron.core.db.api;

import com.googlecode.cqengine.IndexedCollection;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Witness;

@Component
@Slf4j
public class APIHelper {

  @Resource
  private IndexedCollection<Transaction> transactionIndex;
  @Resource
  private IndexedCollection<Block> blockIndex;
  @Resource
  private IndexedCollection<Witness> witnessIndex;
  @Resource
  private IndexedCollection<Account> accountIndex;

  private <T> void add(IndexedCollection<T> index, T t) {
    index.add(t);
  }

  public void add(Transaction t) {
    add(transactionIndex, t);
  }

  public void add(Block b) {
    add(blockIndex, b);
  }

  public void add(Witness w) {
    add(witnessIndex, w);
  }

  public void add(Account a) {
    add(accountIndex, a);
  }

}
