package org.tron.core.jsonrpc;

import static org.tron.core.services.jsonrpc.filters.LogMatch.matchBlock;

import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.runtime.vm.LogInfo;
import org.tron.common.utils.ByteArray;
import org.tron.core.exception.JsonRpcInvalidParamsException;
import org.tron.core.services.jsonrpc.TronJsonRpc.FilterRequest;
import org.tron.core.services.jsonrpc.TronJsonRpc.LogFilterElement;
import org.tron.core.services.jsonrpc.filters.LogFilter;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.Protocol.TransactionInfo.Log;

public class LogMatchExactlyTest {

  String addressTest = "0xd4048be096f969f51fd5642a9c744ec2a7eb89fe";
  String topicTest1 = "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef";
  String topicTest2 = "0x0000000000000000000000000000000000000000000000000000000000000000";
  String topicTest3 = "0x00000000000000000000000098ff8c0e1effbc70b23de702f415ec1e5ed76d42";
  String topicTest4 = "0x0000000000000000000000000000000000000000000000000000000000000783";

  private TransactionInfo createTransactionInfo(byte[] address, byte[][] topicArray, byte[] data) {
    List<Log> logList = new ArrayList<>();
    List<DataWord> topics = new ArrayList<>();
    for (byte[] topic : topicArray) {
      topics.add(new DataWord(topic));
    }

    TransactionInfo.Builder builder = TransactionInfo.newBuilder();

    LogInfo logInfo = new LogInfo(address, topics, data);
    logList.add(LogInfo.buildLog(logInfo));
    builder.addAllLog(logList);

    return builder.build();
  }

  private TransactionInfo createTransactionInfo() {

    byte[] address = ByteArray.fromHexString(addressTest);
    byte[] topic1 = ByteArray.fromHexString(topicTest1);
    byte[] topic2 = ByteArray.fromHexString(topicTest2);
    byte[] topic3 = ByteArray.fromHexString(topicTest3);
    byte[] topic4 = ByteArray.fromHexString(topicTest4);

    return createTransactionInfo(address, new byte[][] {topic1, topic2, topic3, topic4}, null);
  }

  @Test
  public void testMatchOneAddress1() {
    TransactionInfo transactionInfo = createTransactionInfo();
    try {
      LogFilter logFilter = new LogFilter(
          new FilterRequest(null, null, addressTest, null,
              null));
      Assert.assertTrue(logFilter.matchesExactly(transactionInfo.getLog(0)));
    } catch (JsonRpcInvalidParamsException e) {
      Assert.fail();
    }
  }

  @Test
  public void testMatchOneAddress2() {
    TransactionInfo transactionInfo = createTransactionInfo();
    try {
      LogFilter logFilter = new LogFilter(
          new FilterRequest(null, null, addressTest.substring(2), null,
              null));
      Assert.assertTrue(logFilter.matchesExactly(transactionInfo.getLog(0)));
    } catch (JsonRpcInvalidParamsException e) {
      Assert.fail();
    }
  }

  @Test
  public void testMatchOneAddress3() {
    TransactionInfo transactionInfo = createTransactionInfo();
    try {
      LogFilter logFilter = new LogFilter(
          new FilterRequest(null, null, "0x1111111111111111111111111111111111111111", null,
              null));
      Assert.assertFalse(logFilter.matchesExactly(transactionInfo.getLog(0)));
    } catch (JsonRpcInvalidParamsException e) {
      Assert.fail();
    }
  }

  @Test
  public void testMatchMultiAddress() {
    TransactionInfo transactionInfo = createTransactionInfo();
    List<String> addressList = new ArrayList<>();
    addressList.add(addressTest);
    addressList.add("0x0000000000000000000000000000000000000000");
    try {
      LogFilter logFilter = new LogFilter(
          new FilterRequest(null, null, addressList, null,
              null));
      Assert.assertTrue(logFilter.matchesExactly(transactionInfo.getLog(0)));
    } catch (JsonRpcInvalidParamsException e) {
      Assert.fail();
    }
  }

  @Test
  public void testMatchOneTopic1() {
    TransactionInfo transactionInfo = createTransactionInfo();
    try {
      LogFilter logFilter = new LogFilter(new FilterRequest(null, null, null,
          new String[] {topicTest1}, null));
      Assert.assertTrue(logFilter.matchesExactly(transactionInfo.getLog(0)));
    } catch (JsonRpcInvalidParamsException e) {
      Assert.fail();
    }
  }

  @Test
  public void testMatchOneTopic2() {
    TransactionInfo transactionInfo = createTransactionInfo();
    try {
      LogFilter logFilter = new LogFilter(new FilterRequest(null, null, null,
          new String[] {topicTest2}, null));
      Assert.assertFalse(logFilter.matchesExactly(transactionInfo.getLog(0)));
    } catch (JsonRpcInvalidParamsException e) {
      Assert.fail();
    }
  }

  @Test
  public void testMatchMultiTopic1() {
    TransactionInfo transactionInfo = createTransactionInfo();
    List<String> topicList = new ArrayList<>();
    topicList.add(topicTest1);
    topicList.add(topicTest2);
    try {
      LogFilter logFilter = new LogFilter(new FilterRequest(null, null, null,
          new Object[] {topicList}, null));
      Assert.assertTrue(logFilter.matchesExactly(transactionInfo.getLog(0)));
    } catch (JsonRpcInvalidParamsException e) {
      Assert.fail();
    }
  }

  @Test
  public void testMatchMultiTopic2() {
    TransactionInfo transactionInfo = createTransactionInfo();
    List<String> topicList = new ArrayList<>();
    topicList.add(topicTest1);
    topicList.add(topicTest3);
    topicList.add(topicTest4);

    try {
      LogFilter logFilter = new LogFilter(new FilterRequest(null, null, null,
          new Object[] {null, topicList}, null));
      Assert.assertFalse(logFilter.matchesExactly(transactionInfo.getLog(0)));
    } catch (JsonRpcInvalidParamsException e) {
      Assert.fail();
    }
  }

  @Test
  public void testMatchMultiTopic3() {
    TransactionInfo transactionInfo = createTransactionInfo();
    List<String> topicList1 = new ArrayList<>();
    topicList1.add(topicTest1);
    topicList1.add(topicTest2);

    List<String> topicList2 = new ArrayList<>();
    topicList2.add(topicTest3);
    topicList2.add(topicTest4);
    try {
      LogFilter logFilter = new LogFilter(new FilterRequest(null, null, null,
          new Object[] {topicList1, null, topicList2}, null));
      Assert.assertTrue(logFilter.matchesExactly(transactionInfo.getLog(0)));
    } catch (JsonRpcInvalidParamsException e) {
      Assert.fail();
    }
  }

  @Test
  public void testMatchAddressMultiTopic() {
    TransactionInfo transactionInfo = createTransactionInfo();
    List<String> addressList = new ArrayList<>();
    addressList.add(addressTest);
    addressList.add("0x0000000000000000000000000000000000000000");

    List<String> topicList2 = new ArrayList<>();
    topicList2.add(topicTest3);
    topicList2.add(topicTest4);
    try {
      LogFilter logFilter = new LogFilter(new FilterRequest(null, null, addressList,
          new Object[] {topicTest1, null, topicList2}, null));
      Assert.assertTrue(logFilter.matchesExactly(transactionInfo.getLog(0)));

      logFilter = new LogFilter(new FilterRequest(null, null, addressList,
          new Object[] {topicTest1, null, topicTest4}, null));
      Assert.assertFalse(logFilter.matchesExactly(transactionInfo.getLog(0)));

      logFilter = new LogFilter(new FilterRequest(null, null, addressList,
          new Object[] {topicTest2, null, topicList2}, null));
      Assert.assertFalse(logFilter.matchesExactly(transactionInfo.getLog(0)));
    } catch (JsonRpcInvalidParamsException e) {
      Assert.fail();
    }
  }

  @Test
  public void testMatchBlock() {
    TransactionInfo transactionInfo = createTransactionInfo();
    List<TransactionInfo> transactionInfoList = new ArrayList<>();
    transactionInfoList.add(transactionInfo);

    try {
      LogFilter logFilter = new LogFilter(new FilterRequest(null, null, addressTest,
          null, null));
      List<LogFilterElement> elementList =
          matchBlock(logFilter, 100, null, transactionInfoList, false);
      Assert.assertEquals(1, elementList.size());
    } catch (JsonRpcInvalidParamsException e) {
      Assert.fail();
    }
  }
}
