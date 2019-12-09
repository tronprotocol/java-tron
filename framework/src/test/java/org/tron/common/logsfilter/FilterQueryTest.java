package org.tron.common.logsfilter;

import static org.tron.common.logsfilter.EventPluginLoader.matchFilter;
import static org.tron.common.logsfilter.FilterQuery.parseFromBlockNumber;
import static org.tron.common.logsfilter.FilterQuery.parseToBlockNumber;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.logsfilter.capsule.ContractEventTriggerCapsule;
import org.tron.common.runtime.LogEventWrapper;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract.ABI.Entry;

public class FilterQueryTest {

  @Test
  public synchronized void testParseFilterQueryBlockNumber() {
    {
      String blockNum = "";
      Assert.assertEquals(FilterQuery.LATEST_BLOCK_NUM, parseToBlockNumber(blockNum));
    }

    {
      String blockNum = "earliest";
      Assert.assertEquals(FilterQuery.EARLIEST_BLOCK_NUM, parseFromBlockNumber(blockNum));
    }

    {
      String blockNum = "13245";
      Assert.assertEquals(13245, parseToBlockNumber(blockNum));
    }
  }

  @Test
  public synchronized void testMatchFilter() {
    String[] addrList = {"address1", "address2"};
    String[] topList = {"top1", "top2"};
    Map topMap = new HashMap<String, String>();
    List<byte[]> addressList = new ArrayList<>();
    addressList.add(addrList[0].getBytes());
    addressList.add(addrList[1].getBytes());
    topMap.put("1", topList[0]);
    topMap.put("2", topList[1]);
    LogEventWrapper event = new LogEventWrapper();
    ((LogEventWrapper) event).setTopicList(addressList);
    ((LogEventWrapper) event).setData(new byte[]{});
    ((LogEventWrapper) event).setEventSignature("");
    ((LogEventWrapper) event).setAbiEntry(Entry.newBuilder().setName("testABI").build());
    event.setBlockNumber(new Long(123));
    ContractEventTriggerCapsule capsule = new ContractEventTriggerCapsule(event);
    capsule.getContractEventTrigger().setContractAddress("address1");
    capsule.getContractEventTrigger().setTopicMap(topMap);

    {
      Assert.assertEquals(true, matchFilter(capsule.getContractEventTrigger()));
    }

    {
      FilterQuery filterQuery = new FilterQuery();
      filterQuery.setFromBlock(1);
      filterQuery.setToBlock(100);
      EventPluginLoader.getInstance().setFilterQuery(filterQuery);
      Assert.assertEquals(false, matchFilter(capsule.getContractEventTrigger()));
    }

    {
      FilterQuery filterQuery = new FilterQuery();
      filterQuery.setFromBlock(133);
      filterQuery.setToBlock(190);
      EventPluginLoader.getInstance().setFilterQuery(filterQuery);
      Assert.assertEquals(false, matchFilter(capsule.getContractEventTrigger()));
    }

    {
      FilterQuery filterQuery = new FilterQuery();
      filterQuery.setFromBlock(100);
      filterQuery.setToBlock(190);
      filterQuery.setContractAddressList(Arrays.asList(addrList));
      filterQuery.setContractTopicList(Arrays.asList(topList));
      EventPluginLoader.getInstance().setFilterQuery(filterQuery);
      Assert.assertEquals(true, matchFilter(capsule.getContractEventTrigger()));
    }
  }
}
