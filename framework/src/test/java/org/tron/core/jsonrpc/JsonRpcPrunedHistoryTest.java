package org.tron.core.jsonrpc;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.base.Strings;
import java.io.IOException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.tron.api.GrpcAPI.TransactionInfoList;
import org.tron.core.Wallet;
import org.tron.core.db2.core.Chainbase;
import org.tron.core.exception.jsonrpc.JsonRpcInvalidParamsException;
import org.tron.core.exception.jsonrpc.JsonRpcPrunedHistoryException;
import org.tron.core.services.NodeInfoService;
import org.tron.core.services.jsonrpc.JsonRpcApiUtil;
import org.tron.core.services.jsonrpc.TronJsonRpc.FilterRequest;
import org.tron.core.services.jsonrpc.TronJsonRpcImpl;
import org.tron.core.services.jsonrpc.filters.LogFilterWrapper;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.BlockHeader;
import org.tron.protos.Protocol.Transaction;

public class JsonRpcPrunedHistoryTest {

  private static final long LOWEST_BLOCK_NUM = 100L;
  private static final long LOWEST_RECEIPT_BLOCK_NUM = 150L;
  private static final long HEAD_BLOCK_NUM = 200L;
  private static final String PRUNED_MESSAGE = "Pruned history unavailable";
  private static final String BELOW_CUTOFF_HEX = "0x10";
  private static final String AT_CUTOFF_HEX = "0x64";
  private static final long IN_RECEIPT_GAP_NUM = 112L;
  private static final String IN_RECEIPT_GAP_HEX = "0x70";
  private static final String LOWEST_RECEIPT_BLOCK_HEX = "0x96";
  private static final String INVALID_INDEX = "1a";
  private static final String UNKNOWN_HASH = "0x" + Strings.repeat("ab", 32);

  private TronJsonRpcImpl rpc;

  @After
  public void tearDown() throws IOException {
    if (rpc != null) {
      rpc.close();
      rpc = null;
    }
  }

  private static Block newBlock(long number, int transactionCount) {
    Block.Builder builder = Block.newBuilder().setBlockHeader(BlockHeader.newBuilder()
        .setRawData(BlockHeader.raw.newBuilder().setNumber(number)));
    for (int i = 0; i < transactionCount; i++) {
      builder.addTransactions(Transaction.newBuilder());
    }
    return builder.build();
  }

  private static Wallet newMockWallet(boolean liteNode) {
    Wallet wallet = mock(Wallet.class);
    when(wallet.isLiteNode()).thenReturn(liteNode);
    when(wallet.getLowestBlockNum()).thenReturn(liteNode ? LOWEST_BLOCK_NUM : 0L);
    when(wallet.getLowestBlockNumOfReceiptStore())
        .thenReturn(liteNode ? LOWEST_RECEIPT_BLOCK_NUM : 0L);
    when(wallet.getCursor()).thenReturn(Chainbase.Cursor.HEAD);
    when(wallet.getNowBlock()).thenReturn(newBlock(HEAD_BLOCK_NUM, 0));
    // a LiteNode snapshot copies genesis explicitly, so block 0 stays retrievable below the cutoff
    when(wallet.getBlockByNum(0L)).thenReturn(newBlock(0L, 0));
    return wallet;
  }

  private TronJsonRpcImpl newRpc(boolean liteNode) {
    rpc = new TronJsonRpcImpl(mock(NodeInfoService.class), newMockWallet(liteNode));
    return rpc;
  }

  private static Wallet newHistoryOffMockWallet() {
    return newHistoryOffMockWallet(true);
  }

  private static Wallet newHistoryOffMockWallet(boolean liteNode) {
    Wallet wallet = newMockWallet(liteNode);
    when(wallet.getLowestBlockNumOfReceiptStore()).thenReturn(Long.MAX_VALUE);
    return wallet;
  }

  @Test
  public void testParseBlockTagEarliestOnLiteNode() throws Exception {
    Assert.assertEquals(LOWEST_RECEIPT_BLOCK_NUM,
        JsonRpcApiUtil.parseBlockTag("earliest", newMockWallet(true)));
  }

  @Test
  public void testParseBlockTagEarliestOnFullNode() throws Exception {
    Assert.assertEquals(0L, JsonRpcApiUtil.parseBlockTag("earliest", newMockWallet(false)));
  }

  @Test
  public void testGetBlockByNumberBelowCutoffReturns4444() {
    TronJsonRpcImpl liteRpc = newRpc(true);

    JsonRpcPrunedHistoryException e = assertThrows(JsonRpcPrunedHistoryException.class,
        () -> liteRpc.ethGetBlockByNumber(BELOW_CUTOFF_HEX, false));
    Assert.assertEquals(PRUNED_MESSAGE, e.getMessage());
    Assert.assertNull(e.getData());
  }

  @Test
  public void testGetBlockByNumberAtCutoffPasses() throws Exception {
    Assert.assertNull(newRpc(true).ethGetBlockByNumber(AT_CUTOFF_HEX, false));
  }

  @Test
  public void testGetBlockByNumberEarliestOnLiteNodePasses() throws Exception {
    Assert.assertNull(newRpc(true).ethGetBlockByNumber("earliest", false));
  }

  @Test
  public void testGetBlockByNumberOnFullNodePasses() throws Exception {
    Assert.assertNull(newRpc(false).ethGetBlockByNumber(BELOW_CUTOFF_HEX, false));
  }

  @Test
  public void testGetBlockTransactionCountGenesisOnLiteNodePasses() throws Exception {
    // genesis is retained below the cutoff, so a single-block lookup must not return 4444
    Assert.assertEquals("0x0", newRpc(true).ethGetBlockTransactionCountByNumber("0x0"));
  }

  @Test
  public void testGetBlockTransactionCountGenesisWithTransactionsOnLiteNode() throws Exception {
    // Genesis carries initial-allocation transactions in its body but never has transactionInfo
    // (initGenesis writes only blockStore/blockIndexStore, no processBlock). The count endpoint
    // reads the body, so it must return the real count, not 4444.
    Wallet wallet = newMockWallet(true);
    when(wallet.getBlockByNum(0L)).thenReturn(newBlock(0L, 2));
    rpc = new TronJsonRpcImpl(mock(NodeInfoService.class), wallet);

    Assert.assertEquals("0x2", rpc.ethGetBlockTransactionCountByNumber("0x0"));
  }

  @Test
  public void testGetTransactionByBlockNumberAndIndexGenesisOnLiteNodePasses() throws Exception {
    // index beyond the (empty) genesis body yields null, not 4444
    Assert.assertNull(newRpc(true).getTransactionByBlockNumberAndIndex("0x0", "0x0"));
  }

  @Test
  public void testFutureBlockOnLiteNodeReturnsNullNotPruned() throws Exception {
    // above the cutoff and simply not produced yet: null, never 4444
    Assert.assertNull(newRpc(true).ethGetBlockByNumber("0x7fffffff", false));
  }

  @Test
  public void testGetBlockTransactionCountBelowCutoffReturns4444() {
    TronJsonRpcImpl liteRpc = newRpc(true);

    JsonRpcPrunedHistoryException e = assertThrows(JsonRpcPrunedHistoryException.class,
        () -> liteRpc.ethGetBlockTransactionCountByNumber(BELOW_CUTOFF_HEX));
    Assert.assertEquals(PRUNED_MESSAGE, e.getMessage());
    Assert.assertNull(e.getData());
  }

  @Test
  public void testGetTransactionByBlockNumberAndIndexBelowCutoffReturns4444() {
    TronJsonRpcImpl liteRpc = newRpc(true);

    JsonRpcPrunedHistoryException e = assertThrows(JsonRpcPrunedHistoryException.class,
        () -> liteRpc.getTransactionByBlockNumberAndIndex(BELOW_CUTOFF_HEX, "0x0"));
    Assert.assertEquals(PRUNED_MESSAGE, e.getMessage());
    Assert.assertNull(e.getData());
  }

  @Test
  public void testGetTransactionByBlockNumberAndIndexInvalidIndexBelowCutoff() {
    TronJsonRpcImpl liteRpc = newRpc(true);

    assertThrows(JsonRpcInvalidParamsException.class,
        () -> liteRpc.getTransactionByBlockNumberAndIndex(BELOW_CUTOFF_HEX, INVALID_INDEX));
  }

  @Test
  public void testGetTransactionByBlockNumberAndIndexInvalidIndexMissingBlock() {
    TronJsonRpcImpl liteRpc = newRpc(true);

    assertThrows(JsonRpcInvalidParamsException.class,
        () -> liteRpc.getTransactionByBlockNumberAndIndex(AT_CUTOFF_HEX, INVALID_INDEX));
  }

  @Test
  public void testGetTransactionByBlockHashAndIndexInvalidIndexMissingBlock() {
    TronJsonRpcImpl liteRpc = newRpc(true);

    assertThrows(JsonRpcInvalidParamsException.class,
        () -> liteRpc.getTransactionByBlockHashAndIndex(UNKNOWN_HASH, INVALID_INDEX));
  }

  @Test
  public void testGetBlockReceiptsBelowCutoffReturns4444() {
    TronJsonRpcImpl liteRpc = newRpc(true);

    JsonRpcPrunedHistoryException e = assertThrows(JsonRpcPrunedHistoryException.class,
        () -> liteRpc.getBlockReceipts(BELOW_CUTOFF_HEX));
    Assert.assertEquals(PRUNED_MESSAGE, e.getMessage());
    Assert.assertNull(e.getData());
  }

  @Test
  public void testGetBlockReceiptsFutureBlockWithHistoryOffReturnsNull() throws Exception {
    // a block that does not exist yet is not pruned history
    rpc = new TronJsonRpcImpl(mock(NodeInfoService.class), newHistoryOffMockWallet());

    Assert.assertNull(rpc.getBlockReceipts("0x7fffffff"));
  }

  @Test
  public void testGetLogsFromBlockBelowCutoffReturns4444() {
    TronJsonRpcImpl liteRpc = newRpc(true);
    FilterRequest fr = new FilterRequest("0x0", "latest", null, null, null);

    JsonRpcPrunedHistoryException e = assertThrows(JsonRpcPrunedHistoryException.class,
        () -> liteRpc.getLogs(fr));
    Assert.assertEquals(PRUNED_MESSAGE, e.getMessage());
    Assert.assertNull(e.getData());
  }

  @Test
  public void testNewFilterFromBlockBelowCutoffReturns4444() {
    TronJsonRpcImpl liteRpc = newRpc(true);
    FilterRequest fr = new FilterRequest("0x0", "latest", null, null, null);

    JsonRpcPrunedHistoryException e = assertThrows(JsonRpcPrunedHistoryException.class,
        () -> liteRpc.newFilter(fr));
    Assert.assertEquals(PRUNED_MESSAGE, e.getMessage());
    Assert.assertNull(e.getData());
  }

  @Test
  public void testNewFilterEarliestOnLiteNodePasses() throws Exception {
    FilterRequest fr = new FilterRequest("earliest", "latest", null, null, null);

    String filterId = newRpc(true).newFilter(fr);

    Assert.assertNotNull(filterId);
    Assert.assertTrue(filterId.startsWith("0x"));
  }

  @Test
  public void testLogFilterGenesisOnlyRangeOnLiteNodePasses() throws Exception {
    FilterRequest fr = new FilterRequest("0x0", "0x0", null, null, null);

    LogFilterWrapper wrapper =
        new LogFilterWrapper(fr, HEAD_BLOCK_NUM, newMockWallet(true), false);

    Assert.assertEquals(0L, wrapper.getFromBlock());
    Assert.assertEquals(0L, wrapper.getToBlock());
  }

  @Test
  public void testLogFilterGenesisBlockHashOnLiteNodePasses() throws Exception {
    Wallet wallet = newMockWallet(true);
    when(wallet.getBlockById(org.mockito.ArgumentMatchers.any()))
        .thenReturn(newBlock(0L, 0));
    FilterRequest fr = new FilterRequest(null, null, null, null,
        "0x" + Strings.repeat("00", 32));

    LogFilterWrapper wrapper = new LogFilterWrapper(fr, HEAD_BLOCK_NUM, wallet, false);

    Assert.assertEquals(0L, wrapper.getFromBlock());
  }

  @Test
  public void testLogFilterEarliestWithLowToBlockIsInvalidRange() {
    FilterRequest fr = new FilterRequest("earliest", "0x5", null, null, null);

    assertThrows(JsonRpcInvalidParamsException.class,
        () -> new LogFilterWrapper(fr, HEAD_BLOCK_NUM, newMockWallet(true), false));
  }

  @Test
  public void testGetLogsInReceiptGapReturns4444() {
    TronJsonRpcImpl liteRpc = newRpc(true);
    FilterRequest fr = new FilterRequest(IN_RECEIPT_GAP_HEX, "latest", null, null, null);

    JsonRpcPrunedHistoryException e = assertThrows(JsonRpcPrunedHistoryException.class,
        () -> liteRpc.getLogs(fr));
    Assert.assertEquals(PRUNED_MESSAGE, e.getMessage());
    Assert.assertNull(e.getData());
  }

  @Test
  public void testGetBlockReceiptsInReceiptGapReturns4444() {
    Wallet wallet = newMockWallet(true);
    when(wallet.getBlockByNum(IN_RECEIPT_GAP_NUM))
        .thenReturn(newBlock(IN_RECEIPT_GAP_NUM, 1));
    rpc = new TronJsonRpcImpl(mock(NodeInfoService.class), wallet);

    JsonRpcPrunedHistoryException e = assertThrows(JsonRpcPrunedHistoryException.class,
        () -> rpc.getBlockReceipts(IN_RECEIPT_GAP_HEX));
    Assert.assertEquals(PRUNED_MESSAGE, e.getMessage());
    Assert.assertNull(e.getData());
  }

  @Test
  public void testGetBlockReceiptsEmptyBlockInReceiptGapReturns4444() {
    // the check ignores the transaction count, so an empty block is not special-cased
    Wallet wallet = newMockWallet(true);
    when(wallet.getBlockByNum(IN_RECEIPT_GAP_NUM))
        .thenReturn(newBlock(IN_RECEIPT_GAP_NUM, 0));
    rpc = new TronJsonRpcImpl(mock(NodeInfoService.class), wallet);

    JsonRpcPrunedHistoryException e = assertThrows(JsonRpcPrunedHistoryException.class,
        () -> rpc.getBlockReceipts(IN_RECEIPT_GAP_HEX));
    Assert.assertEquals(PRUNED_MESSAGE, e.getMessage());
    Assert.assertNull(e.getData());
  }

  @Test
  public void testGetBlockReceiptsAtLowestReceiptBlockPasses() throws Exception {
    Wallet wallet = newMockWallet(true);
    when(wallet.getBlockByNum(LOWEST_RECEIPT_BLOCK_NUM))
        .thenReturn(newBlock(LOWEST_RECEIPT_BLOCK_NUM, 0));
    when(wallet.getTransactionInfoByBlockNum(LOWEST_RECEIPT_BLOCK_NUM))
        .thenReturn(TransactionInfoList.getDefaultInstance());
    rpc = new TronJsonRpcImpl(mock(NodeInfoService.class), wallet);

    Assert.assertNotNull(rpc.getBlockReceipts(LOWEST_RECEIPT_BLOCK_HEX));
  }

  @Test
  public void testGetBlockTransactionCountInReceiptGapPasses() throws Exception {
    Wallet wallet = newMockWallet(true);
    when(wallet.getBlockByNum(IN_RECEIPT_GAP_NUM))
        .thenReturn(newBlock(IN_RECEIPT_GAP_NUM, 1));
    rpc = new TronJsonRpcImpl(mock(NodeInfoService.class), wallet);

    Assert.assertEquals("0x1", rpc.ethGetBlockTransactionCountByNumber(IN_RECEIPT_GAP_HEX));
  }

  @Test
  public void testParseBlockTagEarliestWithHistoryOffFallsBackToLowestBlock() throws Exception {
    Assert.assertEquals(LOWEST_BLOCK_NUM,
        JsonRpcApiUtil.parseBlockTag("earliest", newHistoryOffMockWallet()));
  }

  @Test
  public void testGetBlockByNumberEarliestWithHistoryOffPasses() throws Exception {
    rpc = new TronJsonRpcImpl(mock(NodeInfoService.class), newHistoryOffMockWallet());

    Assert.assertNull(rpc.ethGetBlockByNumber("earliest", false));
  }

  @Test
  public void testGetLogsWithHistoryOffReturns4444() {
    rpc = new TronJsonRpcImpl(mock(NodeInfoService.class), newHistoryOffMockWallet());
    FilterRequest fr = new FilterRequest(LOWEST_RECEIPT_BLOCK_HEX, "latest", null, null, null);

    JsonRpcPrunedHistoryException e = assertThrows(JsonRpcPrunedHistoryException.class,
        () -> rpc.getLogs(fr));
    Assert.assertEquals(PRUNED_MESSAGE, e.getMessage());
    Assert.assertNull(e.getData());
  }

  @Test
  public void testGetBlockReceiptsWithHistoryOffReturns4444() {
    Wallet wallet = newHistoryOffMockWallet();
    when(wallet.getBlockByNum(LOWEST_RECEIPT_BLOCK_NUM))
        .thenReturn(newBlock(LOWEST_RECEIPT_BLOCK_NUM, 1));
    rpc = new TronJsonRpcImpl(mock(NodeInfoService.class), wallet);

    JsonRpcPrunedHistoryException e = assertThrows(JsonRpcPrunedHistoryException.class,
        () -> rpc.getBlockReceipts(LOWEST_RECEIPT_BLOCK_HEX));
    Assert.assertEquals(PRUNED_MESSAGE, e.getMessage());
    Assert.assertNull(e.getData());
  }

  @Test
  public void testCheckPrunedReceiptHistoryAtMaxBlockWithHistoryOffThrows() {
    JsonRpcPrunedHistoryException e = assertThrows(JsonRpcPrunedHistoryException.class,
        () -> JsonRpcApiUtil.checkPrunedReceiptHistory(Long.MAX_VALUE, newHistoryOffMockWallet()));
    Assert.assertEquals(PRUNED_MESSAGE, e.getMessage());
    Assert.assertNull(e.getData());
  }

  @Test
  public void testGetBlockReceiptsEmptyBlockWithHistoryOffReturns4444() {
    Wallet wallet = newHistoryOffMockWallet();
    when(wallet.getBlockByNum(LOWEST_RECEIPT_BLOCK_NUM))
        .thenReturn(newBlock(LOWEST_RECEIPT_BLOCK_NUM, 0));
    rpc = new TronJsonRpcImpl(mock(NodeInfoService.class), wallet);

    JsonRpcPrunedHistoryException e = assertThrows(JsonRpcPrunedHistoryException.class,
        () -> rpc.getBlockReceipts(LOWEST_RECEIPT_BLOCK_HEX));
    Assert.assertEquals(PRUNED_MESSAGE, e.getMessage());
    Assert.assertNull(e.getData());
  }

  @Test
  public void testGetLogsOnFullNodeWithHistoryOffReturns4444() {
    // receipt persistence is a per-node switch, so the LiteNode gate must not shield a FullNode
    rpc = new TronJsonRpcImpl(mock(NodeInfoService.class), newHistoryOffMockWallet(false));
    FilterRequest fr = new FilterRequest("0x0", "latest", null, null, null);

    JsonRpcPrunedHistoryException e = assertThrows(JsonRpcPrunedHistoryException.class,
        () -> rpc.getLogs(fr));
    Assert.assertEquals(PRUNED_MESSAGE, e.getMessage());
    Assert.assertNull(e.getData());
  }

  @Test
  public void testGetBlockReceiptsOnFullNodeWithHistoryOffReturns4444() {
    Wallet wallet = newHistoryOffMockWallet(false);
    when(wallet.getBlockByNum(LOWEST_RECEIPT_BLOCK_NUM))
        .thenReturn(newBlock(LOWEST_RECEIPT_BLOCK_NUM, 1));
    rpc = new TronJsonRpcImpl(mock(NodeInfoService.class), wallet);

    JsonRpcPrunedHistoryException e = assertThrows(JsonRpcPrunedHistoryException.class,
        () -> rpc.getBlockReceipts(LOWEST_RECEIPT_BLOCK_HEX));
    Assert.assertEquals(PRUNED_MESSAGE, e.getMessage());
    Assert.assertNull(e.getData());
  }

  @Test
  public void testGetBlockByNumberOnFullNodeWithHistoryOffPasses() throws Exception {
    // bodies are unaffected by the receipt switch
    rpc = new TronJsonRpcImpl(mock(NodeInfoService.class), newHistoryOffMockWallet(false));

    Assert.assertNull(rpc.ethGetBlockByNumber(BELOW_CUTOFF_HEX, false));
  }
}
