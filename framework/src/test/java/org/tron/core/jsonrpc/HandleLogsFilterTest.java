package org.tron.core.jsonrpc;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.logsfilter.capsule.LogsFilterCapsule;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.runtime.vm.LogInfo;
import org.tron.core.exception.jsonrpc.JsonRpcInvalidParamsException;
import org.tron.core.services.jsonrpc.TronJsonRpc.FilterRequest;
import org.tron.core.services.jsonrpc.TronJsonRpcImpl;
import org.tron.core.services.jsonrpc.filters.FilterResult;
import org.tron.core.services.jsonrpc.filters.LogFilterAndResult;
import org.tron.protos.Protocol.TransactionInfo;

public class HandleLogsFilterTest {

  private static final String FILTER_ID_1 = "handle-logs-test-001";
  private static final String FILTER_ID_2 = "handle-logs-test-002";

  private TronJsonRpcImpl jsonRpc;

  @Before
  public void setUp() {
    jsonRpc = new TronJsonRpcImpl(null, null);
  }

  @After
  public void tearDown() throws Exception {
    jsonRpc.close();
  }

  private TransactionInfo buildTxInfoWithLog(byte[] address) {
    LogInfo logInfo = new LogInfo(address,
        Collections.singletonList(new DataWord(new byte[32])), new byte[0]);
    return TransactionInfo.newBuilder().addLog(LogInfo.buildLog(logInfo)).build();
  }

  /**
   * Events dispatched to a matching filter in the serial (<=10000 entries) path.
   */
  @Test
  public void testMatchingFilter_receivesLogElements() throws JsonRpcInvalidParamsException {
    FilterRequest fr = new FilterRequest();
    LogFilterAndResult filterAndResult = new LogFilterAndResult(fr, 100L, null);
    jsonRpc.getEventFilter2ResultFull().put(FILTER_ID_1, filterAndResult);

    List<TransactionInfo> txInfoList =
        Collections.singletonList(buildTxInfoWithLog(new byte[20]));
    LogsFilterCapsule capsule =
        new LogsFilterCapsule(150L, "0xabcdef", null, txInfoList, false, false);

    jsonRpc.handleLogsFilter(capsule);

    Assert.assertEquals(1, filterAndResult.getResult().size());
  }

  /**
   * Filter with fromBlock=100 does not receive a capsule whose blockNumber is 50.
   */
  @Test
  public void testBlockNumberBelowRange_noResult() throws JsonRpcInvalidParamsException {
    FilterRequest fr = new FilterRequest();
    // currentMaxBlockNum=100 → fromBlock=100, toBlock=MAX_VALUE
    LogFilterAndResult filterAndResult = new LogFilterAndResult(fr, 100L, null);
    jsonRpc.getEventFilter2ResultFull().put(FILTER_ID_1, filterAndResult);

    List<TransactionInfo> txInfoList =
        Collections.singletonList(buildTxInfoWithLog(new byte[20]));
    LogsFilterCapsule capsule =
        new LogsFilterCapsule(50L, "0xabcdef", null, txInfoList, false, false);

    jsonRpc.handleLogsFilter(capsule);

    Assert.assertTrue(filterAndResult.getResult().isEmpty());
  }

  /**
   * An expired filter is removed from the map during handleLogsFilter.
   */
  @Test
  public void testExpiredFilter_removedFromMap() throws Exception {
    FilterRequest fr = new FilterRequest();
    LogFilterAndResult filterAndResult = new LogFilterAndResult(fr, 100L, null);

    Field expireField = FilterResult.class.getDeclaredField("expireTimeStamp");
    expireField.setAccessible(true);
    expireField.setLong(filterAndResult, 0L);

    Map<String, LogFilterAndResult> map = jsonRpc.getEventFilter2ResultFull();
    map.put(FILTER_ID_1, filterAndResult);
    Assert.assertTrue(map.containsKey(FILTER_ID_1));

    List<TransactionInfo> txInfoList =
        Collections.singletonList(buildTxInfoWithLog(new byte[20]));
    LogsFilterCapsule capsule =
        new LogsFilterCapsule(150L, "0xabcdef", null, txInfoList, false, false);

    jsonRpc.handleLogsFilter(capsule);

    Assert.assertFalse("expired filter should be removed", map.containsKey(FILTER_ID_1));
  }

  /**
   * A solidified capsule is routed only to the solidity map; the full-node map is untouched.
   */
  @Test
  public void testSolidifiedCapsule_routedToSolidityMap() throws JsonRpcInvalidParamsException {
    FilterRequest fr = new FilterRequest();
    LogFilterAndResult solidityFilter = new LogFilterAndResult(fr, 100L, null);
    jsonRpc.getEventFilter2ResultSolidity().put(FILTER_ID_1, solidityFilter);

    LogFilterAndResult fullFilter = new LogFilterAndResult(fr, 100L, null);
    jsonRpc.getEventFilter2ResultFull().put(FILTER_ID_2, fullFilter);

    List<TransactionInfo> txInfoList =
        Collections.singletonList(buildTxInfoWithLog(new byte[20]));
    LogsFilterCapsule capsule =
        new LogsFilterCapsule(150L, "0xabcdef", null, txInfoList, true, false);

    jsonRpc.handleLogsFilter(capsule);

    Assert.assertEquals(1, solidityFilter.getResult().size());
    Assert.assertTrue("full-node filter must not be touched", fullFilter.getResult().isEmpty());
  }

  /**
   * A non-solidified capsule is routed only to the full-node map.
   */
  @Test
  public void testNonSolidifiedCapsule_routedToFullMap() throws JsonRpcInvalidParamsException {
    FilterRequest fr = new FilterRequest();
    LogFilterAndResult solidityFilter = new LogFilterAndResult(fr, 100L, null);
    jsonRpc.getEventFilter2ResultSolidity().put(FILTER_ID_1, solidityFilter);

    LogFilterAndResult fullFilter = new LogFilterAndResult(fr, 100L, null);
    jsonRpc.getEventFilter2ResultFull().put(FILTER_ID_2, fullFilter);

    List<TransactionInfo> txInfoList =
        Collections.singletonList(buildTxInfoWithLog(new byte[20]));
    LogsFilterCapsule capsule =
        new LogsFilterCapsule(150L, "0xabcdef", null, txInfoList, false, false);

    jsonRpc.handleLogsFilter(capsule);

    Assert.assertEquals(1, fullFilter.getResult().size());
    Assert.assertTrue("solidity filter must not be touched", solidityFilter.getResult().isEmpty());
  }

  /**
   * Both filters in the map receive events when both match.
   */
  @Test
  public void testMultipleMatchingFilters_bothReceiveEvents() throws JsonRpcInvalidParamsException {
    FilterRequest fr = new FilterRequest();
    LogFilterAndResult filter1 = new LogFilterAndResult(fr, 100L, null);
    LogFilterAndResult filter2 = new LogFilterAndResult(fr, 100L, null);
    jsonRpc.getEventFilter2ResultFull().put(FILTER_ID_1, filter1);
    jsonRpc.getEventFilter2ResultFull().put(FILTER_ID_2, filter2);

    List<TransactionInfo> txInfoList =
        Collections.singletonList(buildTxInfoWithLog(new byte[20]));
    LogsFilterCapsule capsule =
        new LogsFilterCapsule(150L, "0xabcdef", null, txInfoList, false, false);

    jsonRpc.handleLogsFilter(capsule);

    Assert.assertEquals(1, filter1.getResult().size());
    Assert.assertEquals(1, filter2.getResult().size());
  }

  /**
   * An empty txInfoList produces no results.
   */
  @Test
  public void testEmptyTxInfoList_noResult() throws JsonRpcInvalidParamsException {
    FilterRequest fr = new FilterRequest();
    LogFilterAndResult filterAndResult = new LogFilterAndResult(fr, 100L, null);
    jsonRpc.getEventFilter2ResultFull().put(FILTER_ID_1, filterAndResult);

    LogsFilterCapsule capsule = new LogsFilterCapsule(150L, "0xabcdef", null,
        Collections.emptyList(), false, false);

    jsonRpc.handleLogsFilter(capsule);

    Assert.assertTrue(filterAndResult.getResult().isEmpty());
  }

  private void setParallelThreshold(int value) {
    jsonRpc.setFilterParallelThreshold(value);
  }

  /**
   * Parallel path: every matching filter receives exactly one event — no events dropped or
   * double-counted under concurrent dispatch.
   */
  @Test(timeout = 10000)
  public void testParallelPath_allMatchingFilters_receiveEvents() throws Exception {
    setParallelThreshold(2);
    int count = 5;
    FilterRequest fr = new FilterRequest();
    List<TransactionInfo> txInfoList =
        Collections.singletonList(buildTxInfoWithLog(new byte[20]));
    Map<String, LogFilterAndResult> map = jsonRpc.getEventFilter2ResultFull();
    String prefix = "parallel-match-";
    for (int i = 0; i < count; i++) {
      map.put(prefix + i, new LogFilterAndResult(fr, 0L, null));
    }

    LogsFilterCapsule capsule =
        new LogsFilterCapsule(150L, "0xabcdef", null, txInfoList, false, false);
    jsonRpc.handleLogsFilter(capsule);

    for (int i = 0; i < count; i++) {
      Assert.assertEquals("filter " + i + " must receive exactly one event",
          1, map.get(prefix + i).getResult().size());
    }
  }

  /**
   * Parallel path: expired filters are evicted and all valid filters still receive their events.
   */
  @Test(timeout = 10000)
  public void testParallelPath_expiredFiltersRemoved() throws Exception {
    setParallelThreshold(2);
    int expiredCount = 2;
    int validCount = 3;
    FilterRequest fr = new FilterRequest();
    Field expireField = FilterResult.class.getDeclaredField("expireTimeStamp");
    expireField.setAccessible(true);
    Map<String, LogFilterAndResult> map = jsonRpc.getEventFilter2ResultFull();
    String prefix = "parallel-expire-";
    for (int i = 0; i < expiredCount + validCount; i++) {
      LogFilterAndResult filter = new LogFilterAndResult(fr, 0L, null);
      if (i < expiredCount) {
        expireField.setLong(filter, 0L);
      }
      map.put(prefix + i, filter);
    }

    List<TransactionInfo> txInfoList =
        Collections.singletonList(buildTxInfoWithLog(new byte[20]));
    LogsFilterCapsule capsule =
        new LogsFilterCapsule(150L, "0xabcdef", null, txInfoList, false, false);
    jsonRpc.handleLogsFilter(capsule);

    for (int i = 0; i < expiredCount; i++) {
      Assert.assertFalse("expired filter " + i + " should be removed",
          map.containsKey(prefix + i));
    }
    for (int i = expiredCount; i < expiredCount + validCount; i++) {
      Assert.assertEquals("valid filter " + i + " must receive one event",
          1, map.get(prefix + i).getResult().size());
    }
  }

  /**
   * Parallel path: a solidified capsule dispatches only to the solidity map; the full-node map
   * is untouched even though it holds entries.
   */
  @Test(timeout = 10000)
  public void testParallelPath_solidifiedCapsule_routedToSolidityMap() throws Exception {
    setParallelThreshold(2);
    int count = 5;
    FilterRequest fr = new FilterRequest();
    List<TransactionInfo> txInfoList =
        Collections.singletonList(buildTxInfoWithLog(new byte[20]));
    Map<String, LogFilterAndResult> solidityMap = jsonRpc.getEventFilter2ResultSolidity();
    Map<String, LogFilterAndResult> fullMap = jsonRpc.getEventFilter2ResultFull();
    String solidityPrefix = "parallel-solid-";
    for (int i = 0; i < count; i++) {
      solidityMap.put(solidityPrefix + i, new LogFilterAndResult(fr, 0L, null));
    }
    LogFilterAndResult fullFilter = new LogFilterAndResult(fr, 0L, null);
    fullMap.put("parallel-solid-full-0", fullFilter);

    LogsFilterCapsule capsule =
        new LogsFilterCapsule(150L, "0xabcdef", null, txInfoList, true, false);
    jsonRpc.handleLogsFilter(capsule);

    for (int i = 0; i < count; i++) {
      Assert.assertEquals("solidity filter " + i + " must receive one event",
          1, solidityMap.get(solidityPrefix + i).getResult().size());
    }
    Assert.assertTrue("full-map filter must not receive events",
        fullFilter.getResult().isEmpty());
  }
}
