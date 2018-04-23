package org.tron.core.db.api;

import static com.googlecode.cqengine.query.QueryFactory.all;
import static com.googlecode.cqengine.query.QueryFactory.and;
import static com.googlecode.cqengine.query.QueryFactory.applyThresholds;
import static com.googlecode.cqengine.query.QueryFactory.between;
import static com.googlecode.cqengine.query.QueryFactory.descending;
import static com.googlecode.cqengine.query.QueryFactory.equal;
import static com.googlecode.cqengine.query.QueryFactory.greaterThanOrEqualTo;
import static com.googlecode.cqengine.query.QueryFactory.lessThan;
import static com.googlecode.cqengine.query.QueryFactory.or;
import static com.googlecode.cqengine.query.QueryFactory.orderBy;
import static com.googlecode.cqengine.query.QueryFactory.queryOptions;
import static com.googlecode.cqengine.query.QueryFactory.threshold;
import static com.googlecode.cqengine.query.option.EngineThresholds.INDEX_ORDERING_SELECTIVITY;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import com.googlecode.cqengine.IndexedCollection;
import com.googlecode.cqengine.resultset.ResultSet;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.tron.core.db.api.index.AccountIndex;
import org.tron.core.db.api.index.AssetIssueIndex;
import org.tron.core.db.api.index.BlockIndex;
import org.tron.core.db.api.index.TransactionIndex;
import org.tron.core.db.api.index.WitnessIndex;
import org.tron.core.exception.NonUniqueObjectException;
import org.tron.protos.Contract.AssetIssueContract;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Witness;

@Component
@Slf4j
public class StoreAPI {

  @Autowired(required = false)
  private IndexHelper indexHelper;

  /* *******************************************************************************
   * *                            account api                                      *
   * *******************************************************************************
   */
  public List<Account> getAccountAll() {
    IndexedCollection<Account> index = indexHelper.getAccountIndex();
    return ImmutableList.copyOf(index);
  }

  public Account getAccountByAddress(String address) throws NonUniqueObjectException {
    if (StringUtils.isEmpty(address)) {
      logger.info("address is empty");
      return null;
    }
    IndexedCollection<Account> index = indexHelper.getAccountIndex();
    ResultSet<Account> resultSet = index.retrieve(equal(AccountIndex.Account_ADDRESS, address));

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

  /* *******************************************************************************
   * *                          block api                                          *
   * *******************************************************************************
   */
  public long getBlockCount() {
    IndexedCollection<Block> index = indexHelper.getBlockIndex();
    return (long) index.size();
  }

  public Block getBlockByNumber(long number) throws NonUniqueObjectException {
    IndexedCollection<Block> index = indexHelper.getBlockIndex();
    ResultSet<Block> resultSet = index.retrieve(equal(BlockIndex.Block_NUMBER, number));

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
    if (StringUtils.isEmpty(transactionId)) {
      logger.info("transactionId is empty");
      return null;
    }
    IndexedCollection<Block> index = indexHelper.getBlockIndex();
    ResultSet<Block> resultSet = index.retrieve(equal(BlockIndex.TRANSACTIONS, transactionId));

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
    if (StringUtils.isEmpty(accountAddress)) {
      logger.info("accountAddress is empty");
      return Lists.newArrayList();
    }
    IndexedCollection<Block> index = indexHelper.getBlockIndex();
    ResultSet<Block> resultSet =
        index.retrieve(
            or(equal(BlockIndex.OWNERS, accountAddress), equal(BlockIndex.TOS, accountAddress)),
            queryOptions(
                orderBy(descending(BlockIndex.Block_NUMBER)),
                applyThresholds(threshold(INDEX_ORDERING_SELECTIVITY, 1.0))));

    return ImmutableList.copyOf(resultSet);
  }

  public List<Block> getBlocksByWitnessAddress(String WitnessAddress) {
    if (StringUtils.isEmpty(WitnessAddress)) {
      logger.info("WitnessAddress is empty");
      return Lists.newArrayList();
    }
    IndexedCollection<Block> index = indexHelper.getBlockIndex();
    ResultSet<Block> resultSet =
        index.retrieve(
            equal(BlockIndex.WITNESS_ADDRESS, WitnessAddress),
            queryOptions(
                orderBy(descending(BlockIndex.Block_NUMBER)),
                applyThresholds(threshold(INDEX_ORDERING_SELECTIVITY, 1.0))));

    return ImmutableList.copyOf(resultSet);
  }

  public List<Block> getBlocksByWitnessId(Long witnessId) {
    IndexedCollection<Block> index = indexHelper.getBlockIndex();
    ResultSet<Block> resultSet =
        index.retrieve(
            equal(BlockIndex.WITNESS_ID, witnessId),
            queryOptions(
                orderBy(descending(BlockIndex.Block_NUMBER)),
                applyThresholds(threshold(INDEX_ORDERING_SELECTIVITY, 1.0))));

    return ImmutableList.copyOf(resultSet);
  }

  public List<Block> getLatestBlocks(int topN) {
    if (topN <= 0) {
      return Collections.emptyList();
    }

    IndexedCollection<Block> index = indexHelper.getBlockIndex();
    ResultSet<Block> resultSet =
        index.retrieve(
            all(Block.class),
            queryOptions(
                orderBy(descending(BlockIndex.Block_NUMBER)),
                applyThresholds(threshold(INDEX_ORDERING_SELECTIVITY, 1.0))));

    return ImmutableList.copyOf(Streams.stream(resultSet).limit(topN).iterator());
  }

  /* *******************************************************************************
   * *                       transaction api                                       *
   * *******************************************************************************
   */
  public long getTransactionCount() {
    IndexedCollection<Transaction> index = indexHelper.getTransactionIndex();
    return (long) index.size();
  }

  public Transaction getTransactionById(String id) throws NonUniqueObjectException {
    if (StringUtils.isEmpty(id)) {
      logger.info("id is empty");
      return null;
    }
    IndexedCollection<Transaction> index = indexHelper.getTransactionIndex();
    ResultSet<Transaction> resultSet = index.retrieve(equal(TransactionIndex.Transaction_ID, id));

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
    if (StringUtils.isEmpty(address)) {
      logger.info("address is empty");
      return Lists.newArrayList();
    }
    IndexedCollection<Transaction> index = indexHelper.getTransactionIndex();
    ResultSet<Transaction> resultSet =
        index.retrieve(
            equal(TransactionIndex.OWNERS, address),
            queryOptions(
                orderBy(descending(TransactionIndex.TIMESTAMP)),
                applyThresholds(threshold(INDEX_ORDERING_SELECTIVITY, 1.0))));

    return ImmutableList.copyOf(resultSet);
  }

  public List<Transaction> getTransactionsToThis(String address) {
    if (StringUtils.isEmpty(address)) {
      logger.info("address is empty");
      return Lists.newArrayList();
    }
    IndexedCollection<Transaction> index = indexHelper.getTransactionIndex();
    ResultSet<Transaction> resultSet =
        index.retrieve(
            equal(TransactionIndex.TOS, address),
            queryOptions(
                orderBy(descending(TransactionIndex.TIMESTAMP)),
                applyThresholds(threshold(INDEX_ORDERING_SELECTIVITY, 1.0))));

    return ImmutableList.copyOf(resultSet);
  }

  public List<Transaction> getTransactionsRelatedToAccount(String accountAddress) {
    if (StringUtils.isEmpty(accountAddress)) {
      logger.info("accountAddress is empty");
      return Lists.newArrayList();
    }
    IndexedCollection<Transaction> index = indexHelper.getTransactionIndex();
    ResultSet<Transaction> resultSet =
        index.retrieve(
            or(
                equal(TransactionIndex.OWNERS, accountAddress),
                equal(TransactionIndex.TOS, accountAddress)),
            queryOptions(
                orderBy(descending(TransactionIndex.TIMESTAMP)),
                applyThresholds(threshold(INDEX_ORDERING_SELECTIVITY, 1.0))));

    return ImmutableList.copyOf(resultSet);
  }

  public List<Transaction> getTransactionsByTimestamp(
      long beginInMilliseconds, long endInMilliseconds) {
    if (endInMilliseconds < beginInMilliseconds) {
      return Collections.emptyList();
    }

    IndexedCollection<Transaction> index = indexHelper.getTransactionIndex();
    ResultSet<Transaction> resultSet =
        index.retrieve(
            between(TransactionIndex.TIMESTAMP, beginInMilliseconds, endInMilliseconds),
            queryOptions(
                orderBy(descending(TransactionIndex.TIMESTAMP)),
                applyThresholds(threshold(INDEX_ORDERING_SELECTIVITY, 1.0))));

    return ImmutableList.copyOf(resultSet);
  }

  public List<Transaction> getLatestTransactions(int topN) {
    if (topN <= 0) {
      return Collections.emptyList();
    }

    IndexedCollection<Transaction> index = indexHelper.getTransactionIndex();
    ResultSet<Transaction> resultSet =
        index.retrieve(
            all(Transaction.class),
            queryOptions(
                orderBy(descending(TransactionIndex.TIMESTAMP)),
                applyThresholds(threshold(INDEX_ORDERING_SELECTIVITY, 1.0))));

    return ImmutableList.copyOf(Streams.stream(resultSet).limit(topN).iterator());
  }

  /* *******************************************************************************
   * *                            witness api                                      *
   * *******************************************************************************
   */
  public Witness getWitnessByAddress(String address) throws NonUniqueObjectException {
    if (StringUtils.isEmpty(address)) {
      logger.info("address is empty");
      return null;
    }
    IndexedCollection<Witness> index = indexHelper.getWitnessIndex();
    ResultSet<Witness> resultSet = index.retrieve(equal(WitnessIndex.Witness_ADDRESS, address));

    if (resultSet.isEmpty()) {
      return null;
    }

    try {
      return resultSet.uniqueResult();
    } catch (com.googlecode.cqengine.resultset.common.NonUniqueObjectException e) {
      throw new NonUniqueObjectException(e);
    }
  }

  public Witness getWitnessByUrl(String url) throws NonUniqueObjectException {
    if (StringUtils.isEmpty(url)) {
      logger.info("url is empty");
      return null;
    }
    IndexedCollection<Witness> index = indexHelper.getWitnessIndex();
    ResultSet<Witness> resultSet = index.retrieve(equal(WitnessIndex.Witness_URL, url));

    if (resultSet.isEmpty()) {
      return null;
    }

    try {
      return resultSet.uniqueResult();
    } catch (com.googlecode.cqengine.resultset.common.NonUniqueObjectException e) {
      throw new NonUniqueObjectException(e);
    }
  }

  public Witness getWitnessByPublicKey(String publicKey) throws NonUniqueObjectException {
    if (StringUtils.isEmpty(publicKey)) {
      logger.info("publicKey is empty");
      return null;
    }
    IndexedCollection<Witness> index = indexHelper.getWitnessIndex();
    ResultSet<Witness> resultSet = index.retrieve(equal(WitnessIndex.PUBLIC_KEY, publicKey));

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

  /* *******************************************************************************
   * *                        AssetIssue api                                       *
   * *******************************************************************************
   */
  public List<AssetIssueContract> getAssetIssueAll() {
    IndexedCollection<AssetIssueContract> index = indexHelper.getAssetIssueIndex();
    return ImmutableList.copyOf(index);
  }

  public List<AssetIssueContract> getAssetIssueByTime(long currentInMilliseconds) {
    IndexedCollection<AssetIssueContract> index = indexHelper.getAssetIssueIndex();
    ResultSet<AssetIssueContract> resultSet =
        index.retrieve(
            and(
                lessThan(AssetIssueIndex.AssetIssue_START, currentInMilliseconds),
                greaterThanOrEqualTo(AssetIssueIndex.AssetIssue_END, currentInMilliseconds)),
            queryOptions(
                orderBy(descending(AssetIssueIndex.AssetIssue_END)),
                applyThresholds(threshold(INDEX_ORDERING_SELECTIVITY, 1.0))));

    return ImmutableList.copyOf(resultSet);
  }

  public AssetIssueContract getAssetIssueByName(String name) throws NonUniqueObjectException {
    if (StringUtils.isEmpty(name)) {
      logger.info("name is empty");
      return null;
    }
    IndexedCollection<AssetIssueContract> index = indexHelper.getAssetIssueIndex();
    ResultSet<AssetIssueContract> resultSet =
        index.retrieve(equal(AssetIssueIndex.AssetIssue_NAME, name));

    if (resultSet.isEmpty()) {
      return null;
    }

    try {
      return resultSet.uniqueResult();
    } catch (com.googlecode.cqengine.resultset.common.NonUniqueObjectException e) {
      throw new NonUniqueObjectException(e);
    }
  }

  public List<AssetIssueContract> getAssetIssueByOwnerAddress(String ownerAddress) {
    if (StringUtils.isEmpty(ownerAddress)) {
      logger.info("ownerAddress is empty");
      return Lists.newArrayList();
    }
    IndexedCollection<AssetIssueContract> index = indexHelper.getAssetIssueIndex();
    ResultSet<AssetIssueContract> resultSet =
        index.retrieve(equal(AssetIssueIndex.AssetIssue_OWNER_RADDRESS, ownerAddress));

    return ImmutableList.copyOf(resultSet);
  }
}
