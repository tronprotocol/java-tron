package org.tron.common.logsfilter;
import static org.tron.common.logsfilter.FilterQuery.matchFilter;
import static org.tron.common.logsfilter.FilterQuery.parseFilterQueryBlockNumber;

import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.logsfilter.capsule.ContractEventTriggerCapsule;
import org.tron.common.runtime.vm.LogEventWrapper;

public class FilterQueryTest {
  @Test
  public synchronized void testParseFilterQueryBlockNumber() {
    {
      String blockNum = "";
      Assert.assertEquals(FilterQuery.LATEST_BLOCK_NUM, parseFilterQueryBlockNumber(blockNum));
    }

    {
      String blockNum = "earliest";
      Assert.assertEquals(FilterQuery.EARLIEST_BLOCK_NUM, parseFilterQueryBlockNumber(blockNum));
    }

    {
      String blockNum = "13245";
      Assert.assertEquals(13245, parseFilterQueryBlockNumber(blockNum));
    }
  }

  @Test
  public synchronized void testMatchFilter() {
    String[] topAddressList = {"address1", "address2"};
    List<byte[]> addressList = new ArrayList<>();
    addressList.add(topAddressList[0].getBytes());
    addressList.add(topAddressList[1].getBytes());

    LogEventWrapper event = new LogEventWrapper();
    ((LogEventWrapper) event).setTopicList(addressList);
    ((LogEventWrapper) event).setData(new byte[]{});
    ((LogEventWrapper) event).setEventSignature("");
    ((LogEventWrapper) event).setAbiEntry(null);
    event.setBlockNum(new Long(123));
    ContractEventTriggerCapsule capsule =  new ContractEventTriggerCapsule(event);

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
      EventPluginLoader.getInstance().setFilterQuery(filterQuery);
      Assert.assertEquals(false, matchFilter(capsule.getContractEventTrigger()));
    }
  }
}
