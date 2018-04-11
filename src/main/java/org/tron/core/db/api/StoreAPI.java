package org.tron.core.db.api;

import static com.googlecode.cqengine.query.QueryFactory.all;
import static com.googlecode.cqengine.query.QueryFactory.applyThresholds;
import static com.googlecode.cqengine.query.QueryFactory.between;
import static com.googlecode.cqengine.query.QueryFactory.descending;
import static com.googlecode.cqengine.query.QueryFactory.equal;
import static com.googlecode.cqengine.query.QueryFactory.or;
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
import org.tron.core.exception.NonUniqueObjectException;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Witness;

@Component
@Slf4j
public class StoreAPI {

  @Autowired
  private IndexHelper indexHelper;

  /****************************************
   *                                      *
   *            account api               *
   *                                      *
   ****************************************/

  public Account getAccountByAddress(String address) throws NonUniqueObjectException {
    IndexedCollection<Account> index = indexHelper.getAccountIndex();
    ResultSet<Account> resultSet =
        index.retrieve(equal(AccountIndex.Account_ADDRESS, address));

    if (resultSet.isEmpty()) {
      return null;
    }

    try {
      return resultSet.uniqueResult();
    } catch (com.googlecode.cqengine.resultset.common.NonUniqueObjectException e) {
      throw new NonUniqueObjectException(e);
    }
  }

  public long getAccountCount() {
    IndexedCollection<Account> index = indexHelper.getAccountIndex();
    return (long) index.size();
  }

  /****************************************
   *                                      *
   *              block api               *
   *                                      *
   ****************************************/

  public long getBlockCount() {
    IndexedCollection<Block> index = indexHelper.getBlockIndex();
    return (long) index.size();
  }

  public Block getBlockByNumber(long number) throws NonUniqueObjectException {
    IndexedCollection<Block> index = indexHelper.getBlockIndex();
    ResultSet<Block> resultSet =
        index.retrieve(equal(BlockIndex.Block_NUMBER, number));

    if (resultSet.isEmpty()) {
      return null;
    }

    try {
      return resultSet.uniqueResult();
    } catch (com.googlecode.cqengine.resultset.common.NonUniqueObjectException e) {
      throw new NonUniqueObjectException(e);
    }
  }

  public Block getBlockByTransactionId(String transactionId) throws NonUniqueObjectException {
    IndexedCollection<Block> index = indexHelper.getBlockIndex();
    ResultSet<Block> resultSet =
        index.retrieve(equal(BlockIndex.TRANSACTIONS, transactionId));

    if (resultSet.isEmpty()) {
      return null;
    }

    try {
      return resultSet.uniqueResult();
    } catch (com.googlecode.cqengine.resultset.common.NonUniqueObjectException e) {
      throw new NonUniqueObjectException(e);
    }
  }

  public List<Block> getBlocksRelatedToAccount(String accountAddress) {
    IndexedCollection<Block> index = indexHelper.getBlockIndex();
    ResultSet<Block> resultSet =
        index.retrieve(or(equal(BlockIndex.OWNERS, accountAddress),
            equal(BlockIndex.TOS, accountAddress)),
            queryOptions(orderBy(descending(BlockIndex.Block_NUMBER)),
                applyThresholds(threshold(INDEX_ORDERING_SELECTIVITY, 1.0))));

    return Lists.newArrayList(resultSet);
  }

  public List<Block> getBlocksByWitnessAddress(String WitnessAddress) {
    IndexedCollection<Block> index = indexHelper.getBlockIndex();
    ResultSet<Block> resultSet =
        index.retrieve(equal(BlockIndex.WITNESS_ADDRESS, WitnessAddress),
            queryOptions(orderBy(descending(BlockIndex.Block_NUMBER)),
                applyThresholds(threshold(INDEX_ORDERING_SELECTIVITY, 1.0))));

    return Lists.newArrayList(resultSet);
  }

  public List<Block> getBlocksByWitnessId(Long witnessId) {
    IndexedCollection<Block> index = indexHelper.getBlockIndex();
    ResultSet<Block> resultSet =
        index.retrieve(equal(BlockIndex.WITNESS_ID, witnessId),
            queryOptions(orderBy(descending(BlockIndex.Block_NUMBER)),
                applyThresholds(threshold(INDEX_ORDERING_SELECTIVITY, 1.0))));

    return Lists.newArrayList(resultSet);
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

  /****************************************
   *                                      *
   *            transaction api           *
   *                                      *
   ****************************************/

  public long getTransactionCount() {
    IndexedCollection<Transaction> index = indexHelper.getTransactionIndex();
    return (long) index.size();
  }

  public Transaction getTransactionById(String id) throws NonUniqueObjectException {
    IndexedCollection<Transaction> index = indexHelper.getTransactionIndex();
    ResultSet<Transaction> resultSet =
        index.retrieve(equal(TransactionIndex.Transaction_ID, id));

    if (resultSet.isEmpty()) {
      return null;
    }

    try {
      return resultSet.uniqueResult();
    } catch (com.googlecode.cqengine.resultset.common.NonUniqueObjectException e) {
      throw new NonUniqueObjectException(e);
    }
  }

  public List<Transaction> getTransactionsFromThis(String address) {
    IndexedCollection<Transaction> index = indexHelper.getTransactionIndex();
    ResultSet<Transaction> resultSet =
        index.retrieve(equal(TransactionIndex.OWNERS, address),
            queryOptions(orderBy(descending(TransactionIndex.TIMESTAMP)),
                applyThresholds(threshold(INDEX_ORDERING_SELECTIVITY, 1.0))));

    return Lists.newArrayList(resultSet);
  }

  public List<Transaction> getTransactionsToThis(String address) {
    IndexedCollection<Transaction> index = indexHelper.getTransactionIndex();
    ResultSet<Transaction> resultSet =
        index.retrieve(equal(TransactionIndex.TOS, address),
            queryOptions(orderBy(descending(TransactionIndex.TIMESTAMP)),
                applyThresholds(threshold(INDEX_ORDERING_SELECTIVITY, 1.0))));

    return Lists.newArrayList(resultSet);
  }

  public List<Transaction> getAllTransactions(String address) {
    IndexedCollection<Transaction> index = indexHelper.getTransactionIndex();
    ResultSet<Transaction> resultSet =
        index.retrieve(or(equal(TransactionIndex.OWNERS, address),
            equal(TransactionIndex.TOS, address)),
            queryOptions(orderBy(descending(TransactionIndex.TIMESTAMP)),
                applyThresholds(threshold(INDEX_ORDERING_SELECTIVITY, 1.0))));

    return Lists.newArrayList(resultSet);
  }

  public List<Transaction> getTransactionsByTimestamp(
      long beginInMilliseconds, long endInMilliseconds) {
    if (endInMilliseconds < beginInMilliseconds) {
      return Collections.emptyList();
    }

    IndexedCollection<Transaction> index = indexHelper.getTransactionIndex();
    ResultSet<Transaction> resultSet =
        index.retrieve(between(TransactionIndex.TIMESTAMP, beginInMilliseconds, endInMilliseconds),
            queryOptions(orderBy(descending(TransactionIndex.TIMESTAMP)),
                applyThresholds(threshold(INDEX_ORDERING_SELECTIVITY, 1.0))));

    return Lists.newArrayList(resultSet);
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

  /****************************************
   *                                      *
   *            witness api               *
   *                                      *
   ****************************************/

  public Witness getWitnessByAddress(String address) throws NonUniqueObjectException {
    IndexedCollection<Witness> index = indexHelper.getWitnessIndex();
    ResultSet<Witness> resultSet =
        index.retrieve(equal(WitnessIndex.Witness_ADDRESS, address));

    if (resultSet.isEmpty()) {
      return null;
    }

    try {
      return resultSet.uniqueResult();
    } catch (com.googlecode.cqengine.resultset.common.NonUniqueObjectException e) {
      throw new NonUniqueObjectException(e);
    }
  }

  public List<Witness> getWitnessListByUrl(String url) {
    IndexedCollection<Witness> index = indexHelper.getWitnessIndex();
    ResultSet<Witness> resultSet =
        index.retrieve(equal(WitnessIndex.Witness_URL, url));

    if (resultSet.isEmpty()) {
      return null;
    }

    return Lists.newArrayList(resultSet);
  }

  public Witness getWitnessByPublicKey(String publicKey) throws NonUniqueObjectException {
    IndexedCollection<Witness> index = indexHelper.getWitnessIndex();
    ResultSet<Witness> resultSet =
        index.retrieve(equal(WitnessIndex.PUBLIC_KEY, publicKey));

    if (resultSet.isEmpty()) {
      return null;
    }

    try {
      return resultSet.uniqueResult();
    } catch (com.googlecode.cqengine.resultset.common.NonUniqueObjectException e) {
      throw new NonUniqueObjectException(e);
    }
  }

  public long getWitnessCount() {
    IndexedCollection<Witness> index = indexHelper.getWitnessIndex();
    return (long) index.size();
  }


}
