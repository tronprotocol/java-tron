package org.tron.core.db.api;

import static com.googlecode.cqengine.query.QueryFactory.all;
import static com.googlecode.cqengine.query.QueryFactory.applyThresholds;
import static com.googlecode.cqengine.query.QueryFactory.between;
import static com.googlecode.cqengine.query.QueryFactory.contains;
import static com.googlecode.cqengine.query.QueryFactory.descending;
import static com.googlecode.cqengine.query.QueryFactory.equal;
import static com.googlecode.cqengine.query.QueryFactory.orderBy;
import static com.googlecode.cqengine.query.QueryFactory.queryOptions;
import static com.googlecode.cqengine.query.QueryFactory.threshold;
import static com.googlecode.cqengine.query.option.EngineThresholds.INDEX_ORDERING_SELECTIVITY;

import com.google.common.collect.Lists;
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
import org.tron.core.db.api.index.WitnessIndex;
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
    IndexedCollection<Transaction> index = indexHelper.getTransactionIndex();
    ResultSet<Transaction> resultSet =
        index.retrieve(equal(TransactionIndex.Transaction_ID, id));
    if (resultSet.isEmpty()) {
      return null;
    }

    if (resultSet.size() != 1 && resultSet.iterator().hasNext()) {
      return resultSet.iterator().next();
    }

    return resultSet.uniqueResult();
  }

  // TODO
  public List<Transaction> getTransactionsFromThis(String address) {
    IndexedCollection<Transaction> index = indexHelper.getTransactionIndex();
    ResultSet<Transaction> resultSet =
        index.retrieve(equal(TransactionIndex.OWNERS, address));

    return Lists.newArrayList(resultSet);
  }

  // TODO
  public List<Transaction> getTransactionsToThis(String address) {
    IndexedCollection<Transaction> index = indexHelper.getTransactionIndex();
    ResultSet<Transaction> resultSet =
        index.retrieve(equal(TransactionIndex.TOS, address));

    return Lists.newArrayList(resultSet);
  }

  // TODO
  public List<Transaction> getTransactionsByTimestamp(
      long beginInMilliseconds, long endInMilliseconds) {
    if (endInMilliseconds < beginInMilliseconds) {
      return Collections.emptyList();
    }

    IndexedCollection<Transaction> index = indexHelper.getTransactionIndex();
    ResultSet<Transaction> resultSet =
        index.retrieve(between(TransactionIndex.TIMESTAMP, beginInMilliseconds, endInMilliseconds));

    return Lists.newArrayList(resultSet);
  }

  // TODO
  public Block getBlockByNumber(long number) {
    IndexedCollection<Block> index = indexHelper.getBlockIndex();
    ResultSet<Block> resultSet =
        index.retrieve(equal(BlockIndex.Block_NUMBER, number));
    if (resultSet.isEmpty()) {
      return null;
    }

    if (resultSet.size() != 1 && resultSet.iterator().hasNext()) {
      return resultSet.iterator().next();
    }

    return resultSet.uniqueResult();
  }

  // TODO
  public Block getBlockByTransactionId(String transactionId) {
    IndexedCollection<Block> index = indexHelper.getBlockIndex();
    //TODO TRANSACTIONS is all transactions not ids
    ResultSet<Block> resultSet =
        index.retrieve(equal(BlockIndex.TRANSACTIONS, transactionId));
    if (resultSet.isEmpty()) {
      return null;
    }

    if (resultSet.size() != 1 && resultSet.iterator().hasNext()) {
      return resultSet.iterator().next();
    }

    return resultSet.uniqueResult();
  }

  public List<Block> getBlocksRelatedToAccount(String accountAddress) {
    IndexedCollection<Block> index = indexHelper.getBlockIndex();
    //TODO from or to address of transaction in blocks
    ResultSet<Block> resultSet =
        index.retrieve(all(Block.class), queryOptions(orderBy(descending(BlockIndex.Block_NUMBER)),
            applyThresholds(threshold(INDEX_ORDERING_SELECTIVITY, 1.0))));

    return Streams.stream(resultSet)
        .collect(Collectors.toList());
  }

  public List<Block> getBlocksByWitnessAddress(String WitnessAddress) {
    IndexedCollection<Block> index = indexHelper.getBlockIndex();
    ResultSet<Block> resultSet =
        index.retrieve(all(Block.class),
            queryOptions(equal(BlockIndex.WITNESS_ADDRESS, WitnessAddress),
                applyThresholds(threshold(INDEX_ORDERING_SELECTIVITY, 1.0))));

    return Streams.stream(resultSet)
        .collect(Collectors.toList());
  }

  public List<Block> getBlocksByWitnessId(Long witnessId) {
    IndexedCollection<Block> index = indexHelper.getBlockIndex();
    ResultSet<Block> resultSet =
        index.retrieve(all(Block.class), queryOptions(equal(BlockIndex.WITNESS_ID, witnessId),
            applyThresholds(threshold(INDEX_ORDERING_SELECTIVITY, 1.0))));

    return Streams.stream(resultSet)
        .collect(Collectors.toList());
  }

  // TODO
  public Witness getWitnessByAddress(String address) {
    IndexedCollection<Witness> index = indexHelper.getWitnessIndex();
    ResultSet<Witness> resultSet =
        index.retrieve(equal(WitnessIndex.Witness_ADDRESS, address));
    if (resultSet.isEmpty()) {
      return null;
    }

    if (resultSet.size() != 1 && resultSet.iterator().hasNext()) {
      return resultSet.iterator().next();
    }

    return resultSet.uniqueResult();
  }

  // TODO
  public Witness getWitnessByUrl(String url) {
    IndexedCollection<Witness> index = indexHelper.getWitnessIndex();
    ResultSet<Witness> resultSet =
        index.retrieve(equal(WitnessIndex.Witness_URL, url));
    if (resultSet.isEmpty()) {
      return null;
    }

    if (resultSet.size() != 1 && resultSet.iterator().hasNext()) {
      return resultSet.iterator().next();
    }

    return resultSet.uniqueResult();
  }

  // TODO
  public Witness getWitnessByPublicKey(String publicKey) {
    IndexedCollection<Witness> index = indexHelper.getWitnessIndex();
    ResultSet<Witness> resultSet =
        index.retrieve(equal(WitnessIndex.PUBLIC_KEY, publicKey));
    if (resultSet.isEmpty()) {
      return null;
    }

    if (resultSet.size() != 1 && resultSet.iterator().hasNext()) {
      return resultSet.iterator().next();
    }

    return resultSet.uniqueResult();
  }

}
