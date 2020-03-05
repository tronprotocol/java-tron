package org.tron.common.logsfilter;

import static org.tron.core.Constant.ADD_PRE_FIX_BYTE_MAINNET;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.testng.Assert;
import org.tron.common.crypto.Hash;
import org.tron.common.runtime.TvmTestUtils;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.core.vm.LogInfoTriggerParser;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract.ABI;

public class EventParserTest {

  @Test
  public synchronized void testEventParser() {

    Wallet.setAddressPreFixByte(ADD_PRE_FIX_BYTE_MAINNET);

    String eventSign = "eventBytesL(address,bytes,bytes32,uint256,string)";

    String abiStr = "[{\"constant\":false,\"inputs\":[{\"name\":\"_address\",\"type\":"
        + "\"address\"},{\"name\":\"_random\",\"type\":\"bytes\"}],\"name\":\"randomNum\","
        + "\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\""
        + "function\"},{\"inputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\""
        + ",\"type\":\"constructor\"},{\"anonymous\":true,\"inputs\":[{\"indexed\":true,\"name\""
        + ":\"addr\",\"type\":\"address\"},{\"indexed\":false,\"name\":\"random\",\"type\":"
        + "\"bytes\"},{\"indexed\":false,\"name\":\"last1\",\"type\":\"bytes32\"},{\"indexed\""
        + ":false,\"name\":\"t2\",\"type\":\"uint256\"}],\"name\":\"eventAnonymous\",\"type\":"
        + "\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"name\":\"addr\","
        + "\"type\":\"address\"},{\"indexed\":false,\"name\":\"random\",\"type\":\"bytes\"},"
        + "{\"indexed\":true,\"name\":\"last1\",\"type\":\"bytes32\"},{\"indexed\":false,\"name\":"
        + "\"t2\",\"type\":\"uint256\"},{\"indexed\":false,\"name\":\"str\",\"type\":\"string\"}]"
        + ",\"name\":\"eventBytesL\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\""
        + ":[{\"indexed\":true,\"name\":\"addr\",\"type\":\"address\"},{\"indexed\":false,"
        + "\"name\":\"random\",\"type\":\"bytes\"},{\"indexed\":false,\"name\":\"last1\","
        + "\"type\":\"bytes32\"},{\"indexed\":false,\"name\":\"t2\",\"type\":\"uint256\"},"
        + "{\"indexed\":false,\"name\":\"str\",\"type\":\"string\"}],\"name\":\"eventBytes\","
        + "\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"name\":\""
        + "addr\",\"type\":\"address\"},{\"indexed\":false,\"name\":\"random\",\"type\":\"bytes\"},"
        + "{\"indexed\":false,\"name\":\"\",\"type\":\"bytes32\"},{\"indexed\":false,\"name\""
        + ":\"last1\",\"type\":\"bytes32[]\"},{\"indexed\":false,\"name\":\"t2\",\"type\":"
        + "\"uint256\"},{\"indexed\":false,\"name\":\"str\",\"type\":\"string\"}],\"name\":"
        + "\"eventByteArr\",\"type\":\"event\"}]";

    String dataStr = "0x000000000000000000000000ca35b7d915458ef540ade6068dfe2f44e8fa733c000000000"
        + "00000000000000000000000000000000000000000000000000000800000000000000000000000000000000"
        + "00000000000000000000000000000000100000000000000000000000000000000000000000000000000000"
        + "000000000c0000000000000000000000000000000000000000000000000000000000000000201090000000"
        + "00000000000000000000000000000000000000000000000000000000000000000000000000000000000000"
        + "000000000000000000000000000000a6162636465666731323300000000000000000000000000000000000"
        + "000000000";
    ABI abi = TvmTestUtils.jsonStr2Abi(abiStr);

    byte[] data = ByteArray.fromHexString(dataStr);
    List<byte[]> topicList = new LinkedList<>();
    topicList.add(Hash.sha3(eventSign.getBytes()));
    topicList.add(ByteArray
        .fromHexString("0xb7685f178b1c93df3422f7bfcb61ae2c6f66d0947bb9eb293259c231b986b81b"));

    ABI.Entry entry = null;
    for (ABI.Entry e : abi.getEntrysList()) {
      System.out.println(e.getName());
      if (e.getName().equalsIgnoreCase("eventBytesL")) {
        entry = e;
        break;
      }
    }

    Assert.assertEquals(LogInfoTriggerParser.getEntrySignature(entry), eventSign);
    Assert.assertEquals(Hash.sha3(LogInfoTriggerParser.getEntrySignature(entry).getBytes()),
        topicList.get(0));
    Assert.assertNotNull(entry);
    Map<String, String> dataMap = ContractEventParserAbi.parseEventData(data, topicList, entry);
    Map<String, String> topicMap = ContractEventParserAbi.parseTopics(topicList, entry);

    Assert.assertEquals(dataMap.get("0"), "TUQPrDEJkV4ttkrL7cVv1p3mikWYfM7LWt");
    Assert.assertEquals(dataMap.get("addr"), "TUQPrDEJkV4ttkrL7cVv1p3mikWYfM7LWt");

    Assert.assertEquals(dataMap.get("1"), "0109");
    Assert.assertEquals(dataMap.get("random"), "0109");

    Assert.assertEquals(topicMap.get("2"),
        "b7685f178b1c93df3422f7bfcb61ae2c6f66d0947bb9eb293259c231b986b81b");
    Assert.assertEquals(topicMap.get("last1"),
        "b7685f178b1c93df3422f7bfcb61ae2c6f66d0947bb9eb293259c231b986b81b");

    Assert.assertEquals(dataMap.get("3"), "1");
    Assert.assertEquals(dataMap.get("t2"), "1");

    Assert.assertEquals(dataMap.get("4"), "abcdefg123");
    Assert.assertEquals(dataMap.get("str"), "abcdefg123");

  }
}
