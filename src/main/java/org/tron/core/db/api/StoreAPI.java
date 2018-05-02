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
import org.tron.core.db.common.WrappedByteArray;
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
    AccountIndex index = indexHelper.getAccountIndex();
    return ImmutableList.copyOf(index);
  }

  public Account getAccountByAddress(String address) throws NonUniqueObjectException {
    if (StringUtils.isEmpty(address)) {
      logger.info("address is empty");
      return null;
    }
    AccountIndex index = indexHelper.getAccountIndex();
    ResultSet<Account> resultSet = index.retrieve(equal(index.Account_ADDRESS, address));

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
    AccountIndex index = indexHelper.getAccountIndex();
    return index.size();
  }

  /* *******************************************************************************
   * *                          block api                                          *
   * *******************************************************************************
   */
  public long getBlockCount() {
    BlockIndex index = indexHelper.getBlockIndex();
    return index.size();
  }

  public Block getBlockByNumber(long number) throws NonUniqueObjectException {
    BlockIndex index = indexHelper.getBlockIndex();
    ResultSet<Block> resultSet = index.retrieve(equal(index.Block_NUMBER, number));

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
    BlockIndex index = indexHelper.getBlockIndex();
    ResultSet<Block> resultSet = index.retrieve(equal(index.TRANSACTIONS, transactionId));

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

    BlockIndex index = indexHelper.getBlockIndex();
    ResultSet<Block> resultSet =
        index.retrieve(
            or(equal(index.OWNERS, accountAddress), equal(index.TOS, accountAddress)),
            queryOptions(
                orderBy(descending(index.Block_NUMBER)),
                applyThresholds(threshold(INDEX_ORDERING_SELECTIVITY, 1.0))));

    return ImmutableList.copyOf(resultSet);
  }

  public List<Block> getBlocksByWitnessAddress(String WitnessAddress) {
    if (StringUtils.isEmpty(WitnessAddress)) {
      logger.info("WitnessAddress is empty");
      return Lists.newArrayList();
    }
    BlockIndex index = indexHelper.getBlockIndex();
    ResultSet<Block> resultSet =
        index.retrieve(
            equal(index.WITNESS_ADDRESS, WitnessAddress),
            queryOptions(
                orderBy(descending(index.Block_NUMBER)),
                applyThresholds(threshold(INDEX_ORDERING_SELECTIVITY, 1.0))));

    return ImmutableList.copyOf(resultSet);
  }

  public List<Block> getBlocksByWitnessId(Long witnessId) {
    BlockIndex index = indexHelper.getBlockIndex();
    ResultSet<Block> resultSet =
        index.retrieve(
            equal(index.WITNESS_ID, witnessId),
            queryOptions(
                orderBy(descending(index.Block_NUMBER)),
                applyThresholds(threshold(INDEX_ORDERING_SELECTIVITY, 1.0))));

    return ImmutableList.copyOf(resultSet);
  }

  public List<Block> getLatestBlocks(int topN) {
    if (topN <= 0) {
      return Collections.emptyList();
    }

    BlockIndex index = indexHelper.getBlockIndex();
    ResultSet<Block> resultSet =
        index.retrieve(
            all(WrappedByteArray.class),
            queryOptions(
                orderBy(descending(index.Block_NUMBER)),
                applyThresholds(threshold(INDEX_ORDERING_SELECTIVITY, 1.0))));

    return ImmutableList.copyOf(Streams.stream(resultSet).limit(topN).iterator());
  }

  /* *******************************************************************************
   * *                       transaction api                                       *
   * *******************************************************************************
   */
  public long getTransactionCount() {
    TransactionIndex index = indexHelper.getTransactionIndex();
    return index.size();
  }

  public Transaction getTransactionById(String id) throws NonUniqueObjectException {
    if (StringUtils.isEmpty(id)) {
      logger.info("id is empty");
      return null;
    }
    TransactionIndex index = indexHelper.getTransactionIndex();
    ResultSet<Transaction> resultSet = index.retrieve(equal(index.Transaction_ID, id));

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
    TransactionIndex index = indexHelper.getTransactionIndex();
    ResultSet<Transaction> resultSet =
        index.retrieve(
            equal(index.OWNERS, address),
            queryOptions(
                orderBy(descending(index.TIMESTAMP)),
                applyThresholds(threshold(INDEX_ORDERING_SELECTIVITY, 1.0))));

    return ImmutableList.copyOf(resultSet);
  }

  public List<Transaction> getTransactionsToThis(String address) {
    if (StringUtils.isEmpty(address)) {
      logger.info("address is empty");
      return Lists.newArrayList();
    }
    TransactionIndex index = indexHelper.getTransactionIndex();
    ResultSet<Transaction> resultSet =
        index.retrieve(
            equal(index.TOS, address),
            queryOptions(
                orderBy(descending(index.TIMESTAMP)),
                applyThresholds(threshold(INDEX_ORDERING_SELECTIVITY, 1.0))));

    return ImmutableList.copyOf(resultSet);
  }

  public List<Transaction> getTransactionsRelatedToAccount(String accountAddress) {
    if (StringUtils.isEmpty(accountAddress)) {
      logger.info("accountAddress is empty");
      return Lists.newArrayList();
    }
    TransactionIndex index = indexHelper.getTransactionIndex();
    ResultSet<Transaction> resultSet =
        index.retrieve(
            or(
                equal(index.OWNERS, accountAddress),
                equal(index.TOS, accountAddress)),
            queryOptions(
                orderBy(descending(index.TIMESTAMP)),
                applyThresholds(threshold(INDEX_ORDERING_SELECTIVITY, 1.0))));

    return ImmutableList.copyOf(resultSet);
  }

  public List<Transaction> getTransactionsByTimestamp(
      long beginInMilliseconds, long endInMilliseconds) {
    if (endInMilliseconds < beginInMilliseconds) {
      return Collections.emptyList();
    }

    TransactionIndex index = indexHelper.getTransactionIndex();
    ResultSet<Transaction> resultSet =
        index.retrieve(
            between(index.TIMESTAMP, beginInMilliseconds, endInMilliseconds),
            queryOptions(
                orderBy(descending(index.TIMESTAMP)),
                applyThresholds(threshold(INDEX_ORDERING_SELECTIVITY, 1.0))));

    return ImmutableList.copyOf(resultSet);
  }

  public List<Transaction> getLatestTransactions(int topN) {
    if (topN <= 0) {
      return Collections.emptyList();
    }

    TransactionIndex index = indexHelper.getTransactionIndex();
    ResultSet<Transaction> resultSet =
        index.retrieve(
            all(WrappedByteArray.class),
            queryOptions(
                orderBy(descending(index.TIMESTAMP)),
                applyThresholds(threshold(INDEX_ORDERING_SELECTIVITY, 1.0))));

    return ImmutableList.copyOf(Streams.stream(resultSet).limit(topN).iterator());
  }

  /* *******************************************************************************
   * *                            witness api                                      *
   * *******************************************************************************
   */
  public List<Witness> getWitnessAll() {
    WitnessIndex index = indexHelper.getWitnessIndex();
    return ImmutableList.copyOf(index);
  }

  public Witness getWitnessByAddress(String address) throws NonUniqueObjectException {
    if (StringUtils.isEmpty(address)) {
      logger.info("address is empty");
      return null;
    }
    WitnessIndex index = indexHelper.getWitnessIndex();
    ResultSet<Witness> resultSet = index.retrieve(equal(index.Witness_ADDRESS, address));

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
    WitnessIndex index = indexHelper.getWitnessIndex();
    ResultSet<Witness> resultSet = index.retrieve(equal(index.Witness_URL, url));

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
    WitnessIndex index = indexHelper.getWitnessIndex();
    ResultSet<Witness> resultSet = index.retrieve(equal(index.PUBLIC_KEY, publicKey));

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
    WitnessIndex index = indexHelper.getWitnessIndex();
    return index.size();
  }

  /* *******************************************************************************
   * *                        AssetIssue api                                       *
   * *******************************************************************************
   */
  public List<AssetIssueContract> getAssetIssueAll() {
    AssetIssueIndex index = indexHelper.getAssetIssueIndex();
    return ImmutableList.copyOf(index);
  }

  public List<AssetIssueContract> getAssetIssueByTime(long currentInMilliseconds) {
    AssetIssueIndex index = indexHelper.getAssetIssueIndex();
    ResultSet<AssetIssueContract> resultSet =
        index.retrieve(
            and(
                lessThan(index.AssetIssue_START, currentInMilliseconds),
                greaterThanOrEqualTo(index.AssetIssue_END, currentInMilliseconds)),
            queryOptions(
                orderBy(descending(index.AssetIssue_END)),
                applyThresholds(threshold(INDEX_ORDERING_SELECTIVITY, 1.0))));

    return ImmutableList.copyOf(resultSet);
  }

  public AssetIssueContract getAssetIssueByName(String name) throws NonUniqueObjectException {
    if (StringUtils.isEmpty(name)) {
      logger.info("name is empty");
      return null;
    }
    AssetIssueIndex index = indexHelper.getAssetIssueIndex();
    ResultSet<AssetIssueContract> resultSet =
        index.retrieve(equal(index.AssetIssue_NAME, name));

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
    AssetIssueIndex index = indexHelper.getAssetIssueIndex();
    ResultSet<AssetIssueContract> resultSet =
        index.retrieve(equal(index.AssetIssue_OWNER_RADDRESS, ownerAddress));

    return ImmutableList.copyOf(resultSet);
  }
}
