package org.tron.core.db.api;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import com.googlecode.cqengine.resultset.ResultSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.tron.core.db.api.index.AccountIndex;
import org.tron.core.db.api.index.AssetIssueIndex;
import org.tron.core.db.api.index.BlockIndex;
import org.tron.core.db.api.index.Index;
import org.tron.core.db.api.index.TransactionIndex;
import org.tron.core.db.api.index.WitnessIndex;
import org.tron.core.db.common.WrappedByteArray;
import org.tron.core.exception.NonUniqueObjectException;
import org.tron.protos.Contract.AssetIssueContract;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Witness;

import java.util.Collections;
import java.util.List;

import static com.googlecode.cqengine.query.QueryFactory.all;
import static com.googlecode.cqengine.query.QueryFactory.and;
import static com.googlecode.cqengine.query.QueryFactory.applyThresholds;
import static com.googlecode.cqengine.query.QueryFactory.ascending;
import static com.googlecode.cqengine.query.QueryFactory.between;
import static com.googlecode.cqengine.query.QueryFactory.descending;
import static com.googlecode.cqengine.query.QueryFactory.equal;
import static com.googlecode.cqengine.query.QueryFactory.greaterThan;
import static com.googlecode.cqengine.query.QueryFactory.lessThan;
import static com.googlecode.cqengine.query.QueryFactory.or;
import static com.googlecode.cqengine.query.QueryFactory.orderBy;
import static com.googlecode.cqengine.query.QueryFactory.queryOptions;
import static com.googlecode.cqengine.query.QueryFactory.threshold;
import static com.googlecode.cqengine.query.option.EngineThresholds.INDEX_ORDERING_SELECTIVITY;
import static org.tron.core.config.Parameter.DatabaseConstants.TRANSACTIONS_COUNT_LIMIT_MAX;

@Component
@Slf4j
public class StoreAPI {

  @Autowired(required = false)
  private IndexHelper indexHelper;

  /********************************************************************************
   *                            account api                                       *
   ********************************************************************************
   */
  public List<Account> getAccountAll() {
    Index.Iface<Account> index = indexHelper.getAccountIndex();
    return ImmutableList.copyOf(index);
  }

  public Account getAccountByAddress(String address) throws NonUniqueObjectException {
    if (StringUtils.isEmpty(address)) {
      logger.info("address is empty");
      return null;
    }
    Index.Iface<Account> index = indexHelper.getAccountIndex();
    try (ResultSet<Account> resultSet = index
        .retrieve(equal(AccountIndex.Account_ADDRESS, address))) {
      if (resultSet.isEmpty()) {
        return null;
      }

      return resultSet.uniqueResult();
    } catch (com.googlecode.cqengine.resultset.common.NonUniqueObjectException e) {
      throw new NonUniqueObjectException(e);
    }
  }

  public long getAccountCount() {
    Index.Iface<Account> index = indexHelper.getAccountIndex();
    return index.size();
  }

  /********************************************************************************
   *                          block api                                           *
   ********************************************************************************
   */
  public long getBlockCount() {
    Index.Iface<Block> index = indexHelper.getBlockIndex();
    return index.size();
  }

  public Block getBlockByNumber(long number) throws NonUniqueObjectException {
    Index.Iface<Block> index = indexHelper.getBlockIndex();
    try (ResultSet<Block> resultSet = index.retrieve(equal(BlockIndex.Block_NUMBER, number))) {
      if (resultSet.isEmpty()) {
        return null;
      }

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
    Index.Iface<Block> index = indexHelper.getBlockIndex();
    try (ResultSet<Block> resultSet = index
        .retrieve(equal(BlockIndex.TRANSACTIONS, transactionId))) {
      if (resultSet.isEmpty()) {
        return null;
      }

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

    Index.Iface<Block> index = indexHelper.getBlockIndex();
    try (ResultSet<Block> resultSet =
        index.retrieve(
            or(equal(BlockIndex.OWNERS, accountAddress), equal(BlockIndex.TOS, accountAddress)),
            queryOptions(
                orderBy(descending(BlockIndex.Block_NUMBER)),
                applyThresholds(threshold(INDEX_ORDERING_SELECTIVITY, 1.0))))) {
      return ImmutableList.copyOf(resultSet);
    }
  }

  public List<Block> getBlocksByWitnessAddress(String WitnessAddress) {
    if (StringUtils.isEmpty(WitnessAddress)) {
      logger.info("WitnessAddress is empty");
      return Lists.newArrayList();
    }
    Index.Iface<Block> index = indexHelper.getBlockIndex();
    try (ResultSet<Block> resultSet =
        index.retrieve(
            equal(BlockIndex.WITNESS_ADDRESS, WitnessAddress),
            queryOptions(
                orderBy(descending(BlockIndex.Block_NUMBER)),
                applyThresholds(threshold(INDEX_ORDERING_SELECTIVITY, 1.0))))) {
      return ImmutableList.copyOf(resultSet);
    }
  }

  public List<Block> getBlocksByWitnessId(Long witnessId) {
    Index.Iface<Block> index = indexHelper.getBlockIndex();
    try (ResultSet<Block> resultSet =
        index.retrieve(
            equal(BlockIndex.WITNESS_ID, witnessId),
            queryOptions(
                orderBy(descending(BlockIndex.Block_NUMBER)),
                applyThresholds(threshold(INDEX_ORDERING_SELECTIVITY, 1.0))))) {
      return ImmutableList.copyOf(resultSet);
    }
  }

  public List<Block> getLatestBlocks(int topN) {
    if (topN <= 0) {
      return Collections.emptyList();
    }

    Index.Iface<Block> index = indexHelper.getBlockIndex();
    try (ResultSet<Block> resultSet =
        index.retrieve(
            all(WrappedByteArray.class),
            queryOptions(
                orderBy(descending(BlockIndex.Block_NUMBER)),
                applyThresholds(threshold(INDEX_ORDERING_SELECTIVITY, 1.0))))) {
      return ImmutableList.copyOf(Streams.stream(resultSet).limit(topN).iterator());
    }
  }

  /*******************************************************************************
   *                       transaction api                                       *
   *******************************************************************************
   */
  public long getTransactionCount() {
    Index.Iface<Transaction> index = indexHelper.getTransactionIndex();
    return index.size();
  }

  public Transaction getTransactionById(String id) throws NonUniqueObjectException {
    if (StringUtils.isEmpty(id)) {
      logger.info("id is empty");
      return null;
    }
    Index.Iface<Transaction> index = indexHelper.getTransactionIndex();
    try (ResultSet<Transaction> resultSet = index
        .retrieve(equal(TransactionIndex.Transaction_ID, id))) {
      if (resultSet.isEmpty()) {
        return null;
      }

      return resultSet.uniqueResult();
    } catch (com.googlecode.cqengine.resultset.common.NonUniqueObjectException e) {
      throw new NonUniqueObjectException(e);
    }
  }

  public List<Transaction> getTransactionsFromThis(String address,long offset,long limit) {
    if (StringUtils.isEmpty(address)) {
      logger.info("address is empty");
      return Lists.newArrayList();
    }
    Index.Iface<Transaction> index = indexHelper.getTransactionIndex();
    try (ResultSet<Transaction> resultSet =
                 index.retrieve(
                         equal(TransactionIndex.OWNERS, address),
                         queryOptions(
                                 orderBy(ascending(TransactionIndex.TIMESTAMP)),
                                 applyThresholds(threshold(INDEX_ORDERING_SELECTIVITY, 1.0))))) {
      if (limit > TRANSACTIONS_COUNT_LIMIT_MAX) {
        limit = TRANSACTIONS_COUNT_LIMIT_MAX;
      }
      return ImmutableList.copyOf(Streams.stream(resultSet).skip(offset).limit(limit).iterator());
    }
  }

  public long getTransactionsFromThisCount(String address) {
    Index.Iface<Transaction> index = indexHelper.getTransactionIndex();
    try (ResultSet<Transaction> resultSet =
                 index.retrieve(equal(TransactionIndex.OWNERS, address))) {
      return resultSet.size();
    }
  }

  public List<Transaction> getTransactionsToThis(String address, long offset, long limit) {
    if (StringUtils.isEmpty(address)) {
      logger.info("address is empty");
      return Lists.newArrayList();
    }
    Index.Iface<Transaction> index = indexHelper.getTransactionIndex();
    try (ResultSet<Transaction> resultSet =
        index.retrieve(
                equal(TransactionIndex.TOS, address),
                queryOptions(
                        orderBy(ascending(TransactionIndex.TIMESTAMP)),
                        applyThresholds(threshold(INDEX_ORDERING_SELECTIVITY, 1.0))))) {
      if (limit > TRANSACTIONS_COUNT_LIMIT_MAX) {
        limit = TRANSACTIONS_COUNT_LIMIT_MAX;
      }
      return ImmutableList.copyOf(Streams.stream(resultSet).skip(offset).limit(limit).iterator());
    }
  }

  public long getTransactionsToThisCount(String address) {
    Index.Iface<Transaction> index = indexHelper.getTransactionIndex();
    try (ResultSet<Transaction> resultSet =
                 index.retrieve(equal(TransactionIndex.TOS, address))) {
      return resultSet.size();
    }
  }

  public List<Transaction> getTransactionsRelatedToAccount(String accountAddress) {
    if (StringUtils.isEmpty(accountAddress)) {
      logger.info("accountAddress is empty");
      return Lists.newArrayList();
    }
    Index.Iface<Transaction> index = indexHelper.getTransactionIndex();
    try (ResultSet<Transaction> resultSet =
        index.retrieve(
            or(
                equal(TransactionIndex.OWNERS, accountAddress),
                equal(TransactionIndex.TOS, accountAddress)))) {
      return ImmutableList.copyOf(resultSet);
    }
  }

  public List<Transaction> getTransactionsByTimestamp(
      long beginInMilliseconds, long endInMilliseconds, long offset, long limit) {
    if (endInMilliseconds < beginInMilliseconds) {
      return Collections.emptyList();
    }

    Index.Iface<Transaction> index = indexHelper.getTransactionIndex();
    try (ResultSet<Transaction> resultSet =
        index.retrieve(
            between(TransactionIndex.TIMESTAMP, beginInMilliseconds, endInMilliseconds))) {
      if (limit > TRANSACTIONS_COUNT_LIMIT_MAX) {
        limit = TRANSACTIONS_COUNT_LIMIT_MAX;
      }
      return ImmutableList.copyOf(Streams.stream(resultSet).skip(offset).limit(limit).iterator());
    }
  }

  public long getTransactionsByTimestampCount(long beginInMilliseconds, long endInMilliseconds) {
    if (endInMilliseconds < beginInMilliseconds) {
      return 0;
    }
    Index.Iface<Transaction> index = indexHelper.getTransactionIndex();
    try (ResultSet<Transaction> resultSet =
                 index.retrieve(
                         between(TransactionIndex.TIMESTAMP, beginInMilliseconds, endInMilliseconds))) {
      return resultSet.size();
    }
  }

  public List<Transaction> getLatestTransactions(int topN) {
    if (topN <= 0) {
      return Collections.emptyList();
    }

    Index.Iface<Transaction> index = indexHelper.getTransactionIndex();
    try (ResultSet<Transaction> resultSet =
        index.retrieve(
            all(WrappedByteArray.class),
            queryOptions(
                orderBy(descending(TransactionIndex.TIMESTAMP)),
                applyThresholds(threshold(INDEX_ORDERING_SELECTIVITY, 1.0))))) {
      return ImmutableList.copyOf(Streams.stream(resultSet).limit(topN).iterator());
    }
  }

  /*******************************************************************************
   *                            witness api                                      *
   *******************************************************************************
   */
  public List<Witness> getWitnessAll() {
    Index.Iface<Witness> index = indexHelper.getWitnessIndex();
    return ImmutableList.copyOf(index);
  }

  public Witness getWitnessByAddress(String address) throws NonUniqueObjectException {
    if (StringUtils.isEmpty(address)) {
      logger.info("address is empty");
      return null;
    }
    Index.Iface<Witness> index = indexHelper.getWitnessIndex();
    try (ResultSet<Witness> resultSet = index
        .retrieve(equal(WitnessIndex.Witness_ADDRESS, address))) {
      if (resultSet.isEmpty()) {
        return null;
      }

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
    Index.Iface<Witness> index = indexHelper.getWitnessIndex();
    try (ResultSet<Witness> resultSet = index.retrieve(equal(WitnessIndex.Witness_URL, url))) {
      if (resultSet.isEmpty()) {
        return null;
      }

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
    Index.Iface<Witness> index = indexHelper.getWitnessIndex();
    try (ResultSet<Witness> resultSet = index.retrieve(equal(WitnessIndex.PUBLIC_KEY, publicKey))) {
      if (resultSet.isEmpty()) {
        return null;
      }

      return resultSet.uniqueResult();
    } catch (com.googlecode.cqengine.resultset.common.NonUniqueObjectException e) {
      throw new NonUniqueObjectException(e);
    }
  }

  public long getWitnessCount() {
    Index.Iface<Witness> index = indexHelper.getWitnessIndex();
    return index.size();
  }

  /********************************************************************************
   *                         AssetIssue api                                       *
   ********************************************************************************
   */
  public List<AssetIssueContract> getAssetIssueAll() {
    Index.Iface<AssetIssueContract> index = indexHelper.getAssetIssueIndex();
    return ImmutableList.copyOf(index);
  }

  public List<AssetIssueContract> getAssetIssueByTime(long currentInMilliseconds) {
    Index.Iface<AssetIssueContract> index = indexHelper.getAssetIssueIndex();
    try (ResultSet<AssetIssueContract> resultSet =
        index.retrieve(
            and(
                lessThan(AssetIssueIndex.AssetIssue_START, currentInMilliseconds),
                greaterThan(AssetIssueIndex.AssetIssue_END, currentInMilliseconds)))) {
      resultSet.size();
      return ImmutableList.copyOf(resultSet);
    }
  }

  public AssetIssueContract getAssetIssueByName(String name) throws NonUniqueObjectException {
    if (StringUtils.isEmpty(name)) {
      logger.info("name is empty");
      return null;
    }
    Index.Iface<AssetIssueContract> index = indexHelper.getAssetIssueIndex();
    try (ResultSet<AssetIssueContract> resultSet =
        index.retrieve(equal(AssetIssueIndex.AssetIssue_NAME, name))) {
      if (resultSet.isEmpty()) {
        return null;
      }

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
    Index.Iface<AssetIssueContract> index = indexHelper.getAssetIssueIndex();
    try (ResultSet<AssetIssueContract> resultSet =
        index.retrieve(equal(AssetIssueIndex.AssetIssue_OWNER_ADDRESS, ownerAddress))) {
      return ImmutableList.copyOf(resultSet);
    }
  }
}
