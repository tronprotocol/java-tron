package org.tron.core.db.api;

import static com.googlecode.cqengine.query.QueryFactory.all;
import static com.googlecode.cqengine.query.QueryFactory.applyThresholds;
import static com.googlecode.cqengine.query.QueryFactory.descending;
import static com.googlecode.cqengine.query.QueryFactory.equal;
import static com.googlecode.cqengine.query.QueryFactory.orderBy;
import static com.googlecode.cqengine.query.QueryFactory.queryOptions;
import static com.googlecode.cqengine.query.QueryFactory.threshold;
import static com.googlecode.cqengine.query.option.EngineThresholds.INDEX_ORDERING_SELECTIVITY;

import com.google.common.collect.Streams;
import com.googlecode.cqengine.IndexedCollection;
import com.googlecode.cqengine.resultset.ResultSet;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.db.api.index.AccountIndex;
import org.tron.core.db.api.index.BlockIndex;
import org.tron.core.db.api.index.TransactionIndex;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Witness;

@Component
@Slf4j
public class StoreAPI {

  @Autowired
  private IndexHelper indexHelper;

  public Account getAccountByAddress(String address) {
    IndexedCollection<Account> index = indexHelper.getAccountIndex();
    ResultSet<Account> resultSet =
        index.retrieve(equal(AccountIndex.Account_ADDRESS, address));
    if (resultSet.isEmpty()) {
      return null;
    }

    return resultSet.uniqueResult();
  }

  public Account getAccountByName(String name) {
    IndexedCollection<Account> index = indexHelper.getAccountIndex();
    ResultSet<Account> resultSet =
        index.retrieve(equal(AccountIndex.Account_NAME, name));
    if (resultSet.isEmpty()) {
      return null;
    }

    if (resultSet.size() != 1 && resultSet.iterator().hasNext()) {
      return resultSet.iterator().next();
    }

    return resultSet.uniqueResult();
  }

  public int getAccountCount() {
    IndexedCollection<Account> index = indexHelper.getAccountIndex();
    return index.size();
  }

  public List<Transaction> getLatestTransactions(int topN) {
    if (topN <= 0) {
      return Collections.emptyList();
    }

    IndexedCollection<Transaction> index = indexHelper.getTransactionIndex();
    ResultSet<Transaction> resultSet =
        index.retrieve(all(Transaction.class),
            queryOptions(orderBy(descending(TransactionIndex.TIMESTAMP)),
                applyThresholds(threshold(INDEX_ORDERING_SELECTIVITY, 1.0))));

    return Streams.stream(resultSet)
        .limit(topN)
        .collect(Collectors.toList());
  }

  public List<Block> getLatestBlocks(int topN) {
    if (topN <= 0) {
      return Collections.emptyList();
    }

    IndexedCollection<Block> index = indexHelper.getBlockIndex();
    ResultSet<Block> resultSet =
        index.retrieve(all(Block.class), queryOptions(orderBy(descending(BlockIndex.Block_NUMBER)),
            applyThresholds(threshold(INDEX_ORDERING_SELECTIVITY, 1.0))));

    return Streams.stream(resultSet)
        .limit(topN)
        .collect(Collectors.toList());
  }

  // TODO
  public Transaction getTransactionById(String id) {
    return null;
  }

  // TODO
  public Transaction getTransactionsFromThis(String address) {
    return null;
  }

  // TODO
  public Transaction getTransactionsToThis(String address) {
    return null;
  }

  // TODO
  public List<Transaction> getTransactionsByTimestamp(
      long beginInMilliseconds, long endInMilliseconds) {
    return null;
  }

  // TODO
  public Block getBlockByNumber(long number) {
    return null;
  }

  // TODO
  public Block getBlockByTransactionId(String transactionId) {
    return null;
  }

  // TODO
  public Block getBlocksRelatedToAccount(String accountAddress) {
    return null;
  }

  // TODO
  public Block getBlocksByWitnessAddress(String WitnessAddress) {
    return null;
  }

  // TODO
  public Block getBlocksByWitnessId(String witnessId) {
    return null;
  }

  // TODO
  public Witness getWitnessByAddress(String address) {
    return null;
  }

  // TODO
  public Witness getWitnessByUrl(String url) {
    return null;
  }

  // TODO
  public Witness getWitnessByPublicKey(String publicKey) {
    return null;
  }

}
