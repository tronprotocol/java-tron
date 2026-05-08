package org.tron.common.logsfilter;

import static org.tron.core.Constant.ADD_PRE_FIX_BYTE_MAINNET;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.bouncycastle.crypto.OutputLengthException;
import org.bouncycastle.util.Arrays;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.crypto.Hash;
import org.tron.common.runtime.TvmTestUtils;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.WalletUtil;
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

    Assert.assertFalse(WalletUtil.isConstant(abi, new byte[3]));

    byte[] data = ByteArray.fromHexString(dataStr);
    List<byte[]> topicList = new LinkedList<>();
    topicList.add(Hash.sha3(eventSign.getBytes()));
    topicList.add(ByteArray
        .fromHexString("0xb7685f178b1c93df3422f7bfcb61ae2c6f66d0947bb9eb293259c231b986b81b"));

    ABI.Entry entry = null;
    for (ABI.Entry e : abi.getEntrysList()) {
      if (e.getName().equalsIgnoreCase("eventBytesL")) {
        entry = e;
        break;
      }
    }

    Assert.assertEquals(LogInfoTriggerParser.getEntrySignature(entry), eventSign);
    Assert.assertArrayEquals(Hash.sha3(LogInfoTriggerParser.getEntrySignature(entry).getBytes()),
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

  @Test
  public void testParseDataBytesIntegerTypes() {
    // uint256 = 255
    byte[] uintData = ByteArray.fromHexString(
        "00000000000000000000000000000000000000000000000000000000000000ff");
    Assert.assertEquals("255", ContractEventParser.parseDataBytes(uintData, "uint256", 0));

    // int256 = -1 (two's complement 0xFF..FF is signed negative one)
    byte[] negIntData = ByteArray.fromHexString(
        "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff");
    Assert.assertEquals("-1", ContractEventParser.parseDataBytes(negIntData, "int256", 0));

    // trcToken is classified as INT_NUMBER
    byte[] tokenData = ByteArray.fromHexString(
        "0000000000000000000000000000000000000000000000000000000000000064");
    Assert.assertEquals("100", ContractEventParser.parseDataBytes(tokenData, "trcToken", 0));
  }

  @Test
  public void testParseDataBytesBool() {
    byte[] trueData = ByteArray.fromHexString(
        "0000000000000000000000000000000000000000000000000000000000000001");
    Assert.assertEquals("true", ContractEventParser.parseDataBytes(trueData, "bool", 0));

    byte[] falseData = ByteArray.fromHexString(
        "0000000000000000000000000000000000000000000000000000000000000000");
    Assert.assertEquals("false", ContractEventParser.parseDataBytes(falseData, "bool", 0));
  }

  @Test
  public void testParseDataBytesFixedBytes() {
    String hex = "1234567890abcdef0000000000000000000000000000000000000000000000ff";
    byte[] data = ByteArray.fromHexString(hex);
    Assert.assertEquals(hex, ContractEventParser.parseDataBytes(data, "bytes32", 0));
  }

  @Test
  public void testParseDataBytesAddress() {
    Wallet.setAddressPreFixByte(ADD_PRE_FIX_BYTE_MAINNET);
    // last 20 bytes = ca35...733c => Base58Check = TUQPrDEJkV4ttkrL7cVv1p3mikWYfM7LWt
    byte[] data = ByteArray.fromHexString(
        "000000000000000000000000ca35b7d915458ef540ade6068dfe2f44e8fa733c");
    Assert.assertEquals("TUQPrDEJkV4ttkrL7cVv1p3mikWYfM7LWt",
        ContractEventParser.parseDataBytes(data, "address", 0));
  }

  @Test
  public void testParseDataBytesDynamicBytes() {
    // offset 0x20 | length 3 | 0x010203 padded to 32 bytes
    byte[] data = ByteArray.fromHexString(
        "0000000000000000000000000000000000000000000000000000000000000020"
            + "0000000000000000000000000000000000000000000000000000000000000003"
            + "0102030000000000000000000000000000000000000000000000000000000000");
    Assert.assertEquals("010203", ContractEventParser.parseDataBytes(data, "bytes", 0));
  }

  @Test
  public void testParseDataBytesEmptyString() {
    // offset 0x20 | length 0
    byte[] data = ByteArray.fromHexString(
        "0000000000000000000000000000000000000000000000000000000000000020"
            + "0000000000000000000000000000000000000000000000000000000000000000");
    Assert.assertEquals("", ContractEventParser.parseDataBytes(data, "string", 0));
  }

  @Test
  public void testParseDataBytesNonEmptyString() {
    // "hello world" is 11 ASCII bytes (68656c6c6f20776f726c64), padded to 32 bytes.
    byte[] data = ByteArray.fromHexString(
        "0000000000000000000000000000000000000000000000000000000000000020"
            + "000000000000000000000000000000000000000000000000000000000000000b"
            + "68656c6c6f20776f726c64000000000000000000000000000000000000000000");
    Assert.assertEquals("hello world", ContractEventParser.parseDataBytes(data, "string", 0));
  }

  @Test
  public void testParseDataBytesMultiByteUtf8String() {
    // "中文" UTF-8 = e4b8ad e69687 (6 bytes), padded to 32 bytes.
    byte[] data = ByteArray.fromHexString(
        "0000000000000000000000000000000000000000000000000000000000000020"
            + "0000000000000000000000000000000000000000000000000000000000000006"
            + "e4b8ade696870000000000000000000000000000000000000000000000000000");
    Assert.assertEquals("中文", ContractEventParser.parseDataBytes(data, "string", 0));
  }

  @Test
  public void testParseRevert() {
    String dataHex = "08c379a0"
        + "0000000000000000000000000000000000000000000000000000000000000020"
        + "0000000000000000000000000000000000000000000000000000000000000016"
        + "6e6f7420656e6f75676820696e7075742076616c756500000000000000000000";

    byte[] data = ByteArray.fromHexString(dataHex);
    String msg = ContractEventParser.parseDataBytes(Arrays.copyOfRange(data, 4, data.length),
        "string", 0);
    Assert.assertEquals(msg, "not enough input value");

  }

  @Test
  public void testSubBytesRejectsOversizedLength() {
    // Length must fit in the available source bytes. Reject instead of
    // truncating so oversized ABI lengths are not silently coerced.
    byte[] src = new byte[]{1, 2, 3};
    try {
      ContractEventParser.subBytes(src, 0, Integer.MAX_VALUE);
      Assert.fail("Expected OutputLengthException");
    } catch (OutputLengthException e) {
      Assert.assertTrue(e.getMessage().contains("data start:0"));
      Assert.assertTrue(e.getMessage().contains("length:2147483647"));
      Assert.assertTrue(e.getMessage().contains("src.length:3"));
    }
  }

  @Test
  public void testSubBytesAcceptsExactLength() {
    byte[] src = new byte[]{1, 2, 3, 4};
    byte[] result = ContractEventParser.subBytes(src, 1, 3);
    Assert.assertArrayEquals(new byte[]{2, 3, 4}, result);
  }

  @Test
  public void testSubBytesRejectsNegativeOffset() {
    // ABI offsets are unsigned, but BigInteger(byte[]) interprets 0xFF..FF as
    // -1. The guard should reject that value before System.arraycopy runs.
    byte[] src = new byte[]{1, 2, 3, 4};
    try {
      ContractEventParser.subBytes(src, -1, 3);
      Assert.fail("Expected OutputLengthException");
    } catch (OutputLengthException e) {
      Assert.assertTrue(e.getMessage().contains("data start:-1"));
      Assert.assertTrue(e.getMessage().contains("length:3"));
      Assert.assertTrue(e.getMessage().contains("src.length:4"));
    }
  }

  @Test
  public void testSubBytesRejectsEmptySource() {
    try {
      ContractEventParser.subBytes(new byte[0], 0, 0);
      Assert.fail("Expected OutputLengthException");
    } catch (OutputLengthException e) {
      Assert.assertTrue(e.getMessage().contains("source data is empty"));
    }
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testParseDataBytesRejectsNegativeOffset() {
    // End-to-end check: an offset field of 0xFF..FF decodes to -1 and should
    // be rejected through the existing UnsupportedOperationException path.
    String dataHex = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"
        + "0000000000000000000000000000000000000000000000000000000000000003"
        + "414243";
    byte[] data = ByteArray.fromHexString(dataHex);

    ContractEventParser.parseDataBytes(data, "string", 0);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testParseDataBytesRejectsMalformedLength() {
    // ABI-encoded "string" whose declared length exceeds the available payload
    // should be rejected via the existing UnsupportedOperationException path.
    String dataHex = "0000000000000000000000000000000000000000000000000000000000000020"
        + "000000000000000000000000000000000000000000000000000000007fffffff"
        + "414243";
    byte[] data = ByteArray.fromHexString(dataHex);

    ContractEventParser.parseDataBytes(data, "string", 0);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testParseDataBytesRejectsNegativeLength() {
    // ABI length is an unsigned word. If 0xFF..FF is decoded as -1, reject it
    // instead of treating it as an empty string/bytes payload.
    String dataHex = "0000000000000000000000000000000000000000000000000000000000000020"
        + "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"
        + "414243";
    byte[] data = ByteArray.fromHexString(dataHex);

    ContractEventParser.parseDataBytes(data, "string", 0);
  }
}
