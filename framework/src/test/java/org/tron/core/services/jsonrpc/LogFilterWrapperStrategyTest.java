package org.tron.core.services.jsonrpc;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.core.Wallet;
import org.tron.core.exception.jsonrpc.JsonRpcInvalidParamsException;
import org.tron.core.services.jsonrpc.TronJsonRpc.FilterRequest;
import org.tron.core.services.jsonrpc.filters.LogFilterWrapper;

/**
 * Verify LogFilterWrapper strategies match develop branch behavior.
 *
 * Four filter strategies based on parameter emptiness (develop branch semantics):
 * - Strategy 1: Both fromBlock and toBlock are empty -> (currentMaxBlockNum, Long.MAX_VALUE)
 * - Strategy 2: fromBlock empty, toBlock non-empty -> based on toBlock value
 * - Strategy 3: fromBlock non-empty, toBlock empty -> (fromBlock, Long.MAX_VALUE)
 * - Strategy 4: Both non-empty -> parse both, handle "latest" using snapshot
 */
public class LogFilterWrapperStrategyTest {

  private Wallet mockWallet;
  private static final long CURRENT_MAX_BLOCK = 81628775L;

  @Before
  public void setUp() {
    mockWallet = mock(Wallet.class);
    when(mockWallet.getHeadBlockNum()).thenReturn(CURRENT_MAX_BLOCK);
    when(mockWallet.getSolidBlockNum()).thenReturn(CURRENT_MAX_BLOCK - 100);
  }

  private LogFilterWrapper createFilter(String fromBlock, String toBlock) throws Exception {
    FilterRequest request = new FilterRequest(fromBlock, toBlock, null, null, null);
    return new LogFilterWrapper(request, CURRENT_MAX_BLOCK, mockWallet, false);
  }

  @Test
  public void testStrategy1_BothNull() throws Exception {
    LogFilterWrapper filter = createFilter(null, null);
    assertEquals("fromBlock should be currentMaxBlockNum", CURRENT_MAX_BLOCK,
        filter.getFromBlock());
    assertEquals("toBlock should be Long.MAX_VALUE", Long.MAX_VALUE,
        filter.getToBlock());
  }

  @Test
  public void testStrategy1_BothEmptyString() throws Exception {
    LogFilterWrapper filter = createFilter("", "");
    assertEquals(CURRENT_MAX_BLOCK, filter.getFromBlock());
    assertEquals(Long.MAX_VALUE, filter.getToBlock());
  }

  @Test
  public void testStrategy2_FromEmptyToHex() throws Exception {
    // toBlock = 0x100 = 256
    // fromBlock = min(256, CURRENT_MAX_BLOCK) = 256
    LogFilterWrapper filter = createFilter(null, "0x100");
    assertEquals(256L, filter.getFromBlock());
    assertEquals(256L, filter.getToBlock());
  }

  @Test
  public void testStrategy2_FromEmptyToLatest() throws Exception {
    // toBlock = "latest" is treated as Long.MAX_VALUE in Strategy 2
    // fromBlock = min(Long.MAX_VALUE, CURRENT_MAX_BLOCK) = CURRENT_MAX_BLOCK
    LogFilterWrapper filter = createFilter(null, "latest");
    assertEquals(CURRENT_MAX_BLOCK, filter.getFromBlock());
    assertEquals(Long.MAX_VALUE, filter.getToBlock());
  }

  @Test
  public void testStrategy2_FromEmptyStringToHex() throws Exception {
    LogFilterWrapper filter = createFilter("", "0x200");
    assertEquals(512L, filter.getFromBlock());
    assertEquals(512L, filter.getToBlock());
  }

  @Test
  public void testStrategy3_FromHexToEmpty() throws Exception {
    // fromBlock = 0x1 = 1
    // toBlock = Long.MAX_VALUE (tracking future blocks)
    LogFilterWrapper filter = createFilter("0x1", null);
    assertEquals(1L, filter.getFromBlock());
    assertEquals(Long.MAX_VALUE, filter.getToBlock());
  }

  @Test
  public void testStrategy3_FromLatestToEmpty() throws Exception {
    // fromBlock = "latest" (using snapshot) = currentMaxBlockNum
    // toBlock = Long.MAX_VALUE
    LogFilterWrapper filter = createFilter("latest", null);
    assertEquals(CURRENT_MAX_BLOCK, filter.getFromBlock());
    assertEquals(Long.MAX_VALUE, filter.getToBlock());
  }

  @Test
  public void testStrategy3_FromHexToEmptyString() throws Exception {
    LogFilterWrapper filter = createFilter("0x5", "");
    assertEquals(5L, filter.getFromBlock());
    assertEquals(Long.MAX_VALUE, filter.getToBlock());
  }

  @Test
  public void testStrategy4_BothHex() throws Exception {
    // fromBlock = 1, toBlock = 256
    LogFilterWrapper filter = createFilter("0x1", "0x100");
    assertEquals(1L, filter.getFromBlock());
    assertEquals(256L, filter.getToBlock());
  }

  @Test
  public void testStrategy4_BothLatest() throws Exception {
    // Both "latest" are non-empty, so Strategy 4.
    // fromBlock "latest" -> currentMaxBlockNum (snapshot). toBlock "latest" -> Long.MAX_VALUE.
    LogFilterWrapper filter = createFilter("latest", "latest");
    assertEquals(CURRENT_MAX_BLOCK, filter.getFromBlock());
    assertEquals(Long.MAX_VALUE, filter.getToBlock());
  }

  @Test
  public void testStrategy4_FromHexToLatest() throws Exception {
    // fromBlock = 0x1 (concrete). toBlock = "latest" resolves to Long.MAX_VALUE.
    LogFilterWrapper filter = createFilter("0x1", "latest");
    assertEquals(1L, filter.getFromBlock());
    assertEquals(Long.MAX_VALUE, filter.getToBlock());
  }

  @Test
  public void testStrategy4_FromLatestToHexAboveLatest() throws Exception {
    // This test requires a toBlock value larger than currentMaxBlockNum
    // Using 0x5000000 (83886080) which is > 81628775
    LogFilterWrapper filter = createFilter("latest", "0x5000000");
    assertEquals(CURRENT_MAX_BLOCK, filter.getFromBlock());
    assertEquals(83886080L, filter.getToBlock());
  }

  @Test
  public void testStrategy4_InvertedRangeThrows() throws Exception {
    // fromBlock (0x100 = 256) > toBlock (0x1 = 1) should throw
    try {
      createFilter("0x100", "0x1");
      Assert.fail("Expected exception");
    } catch (JsonRpcInvalidParamsException e) {
      assertEquals("please verify: fromBlock <= toBlock", e.getMessage());
    }
  }

  @Test
  public void testStrategy4_LatestGreaterThanSmallBlock_Throws() throws Exception {
    // fromBlock = "latest" (currentMaxBlockNum = 81628775) > toBlock (0x100 = 256) should throw
    try {
      createFilter("latest", "0x100");
      Assert.fail("Expected exception");
    } catch (JsonRpcInvalidParamsException e) {
      assertEquals("please verify: fromBlock <= toBlock", e.getMessage());
    }
  }
}
