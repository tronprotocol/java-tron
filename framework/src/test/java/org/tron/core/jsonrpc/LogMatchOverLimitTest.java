package org.tron.core.jsonrpc;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.protobuf.ByteString;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.tron.api.GrpcAPI.TransactionInfoList;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.db.Manager;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.exception.jsonrpc.JsonRpcInvalidParamsException;
import org.tron.core.exception.jsonrpc.JsonRpcTooManyResultException;
import org.tron.core.services.jsonrpc.TronJsonRpc.FilterRequest;
import org.tron.core.services.jsonrpc.TronJsonRpc.LogFilterElement;
import org.tron.core.services.jsonrpc.filters.LogBlockQuery;
import org.tron.core.services.jsonrpc.filters.LogFilterWrapper;
import org.tron.core.services.jsonrpc.filters.LogMatch;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.Protocol.TransactionInfo.Log;

/**
 * Verifies the over-limit check in {@link LogMatch#matchBlockOneByOne()}
 * The fix ensures the exception is thrown BEFORE {@code addAll}, so the result list never
 * silently exceeds {@link LogBlockQuery#MAX_RESULT}.
 */
public class LogMatchOverLimitTest {

  private static final int MAX_RESULT = LogBlockQuery.MAX_RESULT; // 10000

  /** Builds a TransactionInfoList containing one TransactionInfo with {@code logCount} logs. */
  private TransactionInfoList buildTxList(int logCount) {
    TransactionInfo.Builder txBuilder = TransactionInfo.newBuilder();
    for (int i = 0; i < logCount; i++) {
      txBuilder.addLog(Log.newBuilder()
          .setAddress(ByteString.copyFrom(new byte[20]))
          .build());
    }
    return TransactionInfoList.newBuilder()
        .addTransactionInfo(txBuilder.build())
        .build();
  }

  private Manager buildMockManager(long blockNum, TransactionInfoList txList)
      throws ItemNotFoundException {
    Manager manager = mock(Manager.class);
    ChainBaseManager chainBaseManager = mock(ChainBaseManager.class);
    BlockCapsule.BlockId blockId = new BlockCapsule.BlockId(Sha256Hash.ZERO_HASH, blockNum);

    when(manager.getChainBaseManager()).thenReturn(chainBaseManager);
    when(chainBaseManager.getBlockIdByNum(anyLong())).thenReturn(blockId);
    when(manager.getTransactionInfoByBlockNum(blockNum)).thenReturn(txList);
    return manager;
  }

  private Manager buildMockManager(long block1, TransactionInfoList txList1,
      long block2, TransactionInfoList txList2) throws ItemNotFoundException {
    Manager manager = mock(Manager.class);
    ChainBaseManager chainBaseManager = mock(ChainBaseManager.class);
    BlockCapsule.BlockId blockId = new BlockCapsule.BlockId(Sha256Hash.ZERO_HASH, 0);

    when(manager.getChainBaseManager()).thenReturn(chainBaseManager);
    when(chainBaseManager.getBlockIdByNum(anyLong())).thenReturn(blockId);
    when(manager.getTransactionInfoByBlockNum(block1)).thenReturn(txList1);
    when(manager.getTransactionInfoByBlockNum(block2)).thenReturn(txList2);
    return manager;
  }

  private LogMatch buildLogMatch(List<Long> blockNums, Manager manager)
      throws JsonRpcInvalidParamsException {
    FilterRequest fr = new FilterRequest(); // match-all filter
    LogFilterWrapper wrapper = new LogFilterWrapper(fr, 0L, null, false);
    return new LogMatch(wrapper, blockNums, manager);
  }

  /** Under the limit: all logs returned without exception. */
  @Test
  public void testUnderLimit_returnsAllResults()
      throws BadItemException, ItemNotFoundException, JsonRpcTooManyResultException,
             JsonRpcInvalidParamsException {
    int logCount = MAX_RESULT / 2; // 5000, well under limit
    Manager manager = buildMockManager(100L, buildTxList(logCount));
    LogMatch logMatch = buildLogMatch(Collections.singletonList(100L), manager);

    LogFilterElement[] results = logMatch.matchBlockOneByOne();
    Assert.assertEquals(logCount, results.length);
  }

  /**
   * The cumulative log count from two blocks equals exactly MAX_RESULT.
   * This should succeed (boundary: equal is still OK).
   */
  @Test
  public void testAtExactLimit_succeeds()
      throws BadItemException, ItemNotFoundException, JsonRpcTooManyResultException,
             JsonRpcInvalidParamsException {
    // block 1: MAX_RESULT - 1 logs, block 2: 1 log → total == MAX_RESULT
    Manager manager = buildMockManager(
        1L, buildTxList(MAX_RESULT - 1),
        2L, buildTxList(1));
    LogMatch logMatch = buildLogMatch(Arrays.asList(1L, 2L), manager);

    LogFilterElement[] results = logMatch.matchBlockOneByOne();
    Assert.assertEquals(MAX_RESULT, results.length);
  }

  /**
   * Verifies the fix: when the second block would push the total over MAX_RESULT,
   * {@link JsonRpcTooManyResultException} is thrown BEFORE {@code addAll}.
   */
  @Test
  public void testExceedsLimit_throws()
      throws ItemNotFoundException, JsonRpcInvalidParamsException {
    // block 1: MAX_RESULT - 1 logs, block 2: 2 logs → 9999 + 2 = 10001 > MAX_RESULT
    Manager manager = buildMockManager(
        1L, buildTxList(MAX_RESULT - 1),
        2L, buildTxList(2));
    LogMatch logMatch = buildLogMatch(Arrays.asList(1L, 2L), manager);

    assertThrows(JsonRpcTooManyResultException.class, logMatch::matchBlockOneByOne);
  }

  /** A block with no matching logs is skipped without incrementing the result count. */
  @Test
  public void testEmptyBlockSkipped()
      throws BadItemException, ItemNotFoundException, JsonRpcTooManyResultException,
             JsonRpcInvalidParamsException {
    // block 1: no logs (empty txInfoList → skipped), block 2: 3 logs
    Manager manager = mock(Manager.class);
    ChainBaseManager chainBaseManager = mock(ChainBaseManager.class);
    BlockCapsule.BlockId blockId = new BlockCapsule.BlockId(Sha256Hash.ZERO_HASH, 0);
    when(manager.getChainBaseManager()).thenReturn(chainBaseManager);
    when(chainBaseManager.getBlockIdByNum(anyLong())).thenReturn(blockId);
    when(manager.getTransactionInfoByBlockNum(1L))
        .thenReturn(TransactionInfoList.newBuilder().build()); // empty
    when(manager.getTransactionInfoByBlockNum(2L)).thenReturn(buildTxList(3));

    LogMatch logMatch = buildLogMatch(Arrays.asList(1L, 2L), manager);
    LogFilterElement[] results = logMatch.matchBlockOneByOne();
    Assert.assertEquals(3, results.length);
  }
}
