package org.tron.core.jsonrpc;

import static org.tron.core.services.jsonrpc.TronJsonRpcImpl.handleLogsFilter;

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

  @Before
  public void setUp() {
    cleanMaps();
  }

  @After
  public void tearDown() {
    cleanMaps();
  }

  private void cleanMaps() {
    TronJsonRpcImpl.getEventFilter2ResultFull().remove(FILTER_ID_1);
    TronJsonRpcImpl.getEventFilter2ResultFull().remove(FILTER_ID_2);
    TronJsonRpcImpl.getEventFilter2ResultSolidity().remove(FILTER_ID_1);
    TronJsonRpcImpl.getEventFilter2ResultSolidity().remove(FILTER_ID_2);
  }

  private TransactionInfo buildTxInfoWithLog(byte[] address) {
    LogInfo logInfo = new LogInfo(address,
        Collections.singletonList(new DataWord(new byte[32])), new byte[0]);
    return TransactionInfo.newBuilder().addLog(LogInfo.buildLog(logInfo)).build();
  }

  /** Events dispatched to a matching filter in the serial (<=10000 entries) path. */
  @Test
  public void testMatchingFilter_receivesLogElements() throws JsonRpcInvalidParamsException {
    FilterRequest fr = new FilterRequest();
    LogFilterAndResult filterAndResult = new LogFilterAndResult(fr, 100L, null);
    TronJsonRpcImpl.getEventFilter2ResultFull().put(FILTER_ID_1, filterAndResult);

    List<TransactionInfo> txInfoList =
        Collections.singletonList(buildTxInfoWithLog(new byte[20]));
    LogsFilterCapsule capsule =
        new LogsFilterCapsule(150L, "0xabcdef", null, txInfoList, false, false);

    handleLogsFilter(capsule);

    Assert.assertEquals(1, filterAndResult.getResult().size());
  }

  /** Filter with fromBlock=100 does not receive a capsule whose blockNumber is 50. */
  @Test
  public void testBlockNumberBelowRange_noResult() throws JsonRpcInvalidParamsException {
    FilterRequest fr = new FilterRequest();
    // currentMaxBlockNum=100 → fromBlock=100, toBlock=MAX_VALUE
    LogFilterAndResult filterAndResult = new LogFilterAndResult(fr, 100L, null);
    TronJsonRpcImpl.getEventFilter2ResultFull().put(FILTER_ID_1, filterAndResult);

    List<TransactionInfo> txInfoList =
        Collections.singletonList(buildTxInfoWithLog(new byte[20]));
    LogsFilterCapsule capsule =
        new LogsFilterCapsule(50L, "0xabcdef", null, txInfoList, false, false);

    handleLogsFilter(capsule);

    Assert.assertTrue(filterAndResult.getResult().isEmpty());
  }

  /** An expired filter is removed from the map during handleLogsFilter. */
  @Test
  public void testExpiredFilter_removedFromMap() throws Exception {
    FilterRequest fr = new FilterRequest();
    LogFilterAndResult filterAndResult = new LogFilterAndResult(fr, 100L, null);

    Field expireField = FilterResult.class.getDeclaredField("expireTimeStamp");
    expireField.setAccessible(true);
    expireField.setLong(filterAndResult, 0L);

    Map<String, LogFilterAndResult> map = TronJsonRpcImpl.getEventFilter2ResultFull();
    map.put(FILTER_ID_1, filterAndResult);
    Assert.assertTrue(map.containsKey(FILTER_ID_1));

    List<TransactionInfo> txInfoList =
        Collections.singletonList(buildTxInfoWithLog(new byte[20]));
    LogsFilterCapsule capsule =
        new LogsFilterCapsule(150L, "0xabcdef", null, txInfoList, false, false);

    handleLogsFilter(capsule);

    Assert.assertFalse("expired filter should be removed", map.containsKey(FILTER_ID_1));
  }

  /** A solidified capsule is routed only to the solidity map; the full-node map is untouched. */
  @Test
  public void testSolidifiedCapsule_routedToSolidityMap() throws JsonRpcInvalidParamsException {
    FilterRequest fr = new FilterRequest();
    LogFilterAndResult solidityFilter = new LogFilterAndResult(fr, 100L, null);
    TronJsonRpcImpl.getEventFilter2ResultSolidity().put(FILTER_ID_1, solidityFilter);

    LogFilterAndResult fullFilter = new LogFilterAndResult(fr, 100L, null);
    TronJsonRpcImpl.getEventFilter2ResultFull().put(FILTER_ID_2, fullFilter);

    List<TransactionInfo> txInfoList =
        Collections.singletonList(buildTxInfoWithLog(new byte[20]));
    LogsFilterCapsule capsule =
        new LogsFilterCapsule(150L, "0xabcdef", null, txInfoList, true, false);

    handleLogsFilter(capsule);

    Assert.assertEquals(1, solidityFilter.getResult().size());
    Assert.assertTrue("full-node filter must not be touched", fullFilter.getResult().isEmpty());
  }

  /** A non-solidified capsule is routed only to the full-node map. */
  @Test
  public void testNonSolidifiedCapsule_routedToFullMap() throws JsonRpcInvalidParamsException {
    FilterRequest fr = new FilterRequest();
    LogFilterAndResult solidityFilter = new LogFilterAndResult(fr, 100L, null);
    TronJsonRpcImpl.getEventFilter2ResultSolidity().put(FILTER_ID_1, solidityFilter);

    LogFilterAndResult fullFilter = new LogFilterAndResult(fr, 100L, null);
    TronJsonRpcImpl.getEventFilter2ResultFull().put(FILTER_ID_2, fullFilter);

    List<TransactionInfo> txInfoList =
        Collections.singletonList(buildTxInfoWithLog(new byte[20]));
    LogsFilterCapsule capsule =
        new LogsFilterCapsule(150L, "0xabcdef", null, txInfoList, false, false);

    handleLogsFilter(capsule);

    Assert.assertEquals(1, fullFilter.getResult().size());
    Assert.assertTrue("solidity filter must not be touched", solidityFilter.getResult().isEmpty());
  }

  /** Both filters in the map receive events when both match. */
  @Test
  public void testMultipleMatchingFilters_bothReceiveEvents() throws JsonRpcInvalidParamsException {
    FilterRequest fr = new FilterRequest();
    LogFilterAndResult filter1 = new LogFilterAndResult(fr, 100L, null);
    LogFilterAndResult filter2 = new LogFilterAndResult(fr, 100L, null);
    TronJsonRpcImpl.getEventFilter2ResultFull().put(FILTER_ID_1, filter1);
    TronJsonRpcImpl.getEventFilter2ResultFull().put(FILTER_ID_2, filter2);

    List<TransactionInfo> txInfoList =
        Collections.singletonList(buildTxInfoWithLog(new byte[20]));
    LogsFilterCapsule capsule =
        new LogsFilterCapsule(150L, "0xabcdef", null, txInfoList, false, false);

    handleLogsFilter(capsule);

    Assert.assertEquals(1, filter1.getResult().size());
    Assert.assertEquals(1, filter2.getResult().size());
  }

  /** An empty txInfoList produces no results. */
  @Test
  public void testEmptyTxInfoList_noResult() throws JsonRpcInvalidParamsException {
    FilterRequest fr = new FilterRequest();
    LogFilterAndResult filterAndResult = new LogFilterAndResult(fr, 100L, null);
    TronJsonRpcImpl.getEventFilter2ResultFull().put(FILTER_ID_1, filterAndResult);

    LogsFilterCapsule capsule =
        new LogsFilterCapsule(150L, "0xabcdef", null, Collections.emptyList(), false, false);

    handleLogsFilter(capsule);

    Assert.assertTrue(filterAndResult.getResult().isEmpty());
  }
}
