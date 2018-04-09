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
public class StoreAPI {

  @Resource
  private IndexedCollection<Transaction> transactionIndex;
  @Resource
  private IndexedCollection<Block> blockIndex;
  @Resource
  private IndexedCollection<Witness> witnessIndex;
  @Resource
  private IndexedCollection<Account> accountIndex;

  
}
