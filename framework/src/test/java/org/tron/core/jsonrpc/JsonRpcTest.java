package org.tron.core.jsonrpc;

import static org.tron.common.utils.DecodeUtil.addressPreFixString;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.addressCompatibleToByteArray;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.addressToByteArray;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.getMethodSign;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.parseEnergyFee;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.bloom.Bloom;
import org.tron.common.crypto.Hash;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ByteUtil;
import org.tron.common.utils.Commons;
import org.tron.core.exception.JsonRpcInvalidParamsException;
import org.tron.core.services.jsonrpc.TronJsonRpc.FilterRequest;
import org.tron.core.services.jsonrpc.filters.LogBlockQuery;
import org.tron.core.services.jsonrpc.filters.LogFilter;
import org.tron.core.services.jsonrpc.filters.LogFilterWrapper;
import org.tron.core.services.jsonrpc.types.CallArguments;


public class JsonRpcTest {

  public void generateCallParameterWIthMethodAndParam() {
    String ownerAddress = "TXvRyjomvtNWSKvNouTvAedRGD4w9RXLZD";
    String usdjAddress = "TLBaRhANQoJFTqre9Nf1mjuwNWjCJeYqUL"; // nile udsj address

    byte[] addressData = Commons.decodeFromBase58Check(ownerAddress);
    byte[] addressDataWord = new byte[32];
    System.arraycopy(Commons.decodeFromBase58Check(ownerAddress), 0, addressDataWord,
        32 - addressData.length, addressData.length);
    String data = getMethodSign("balanceOf(address)") + Hex.toHexString(addressDataWord);

    CallArguments transactionCall = new CallArguments();
    transactionCall.setFrom(ByteArray.toHexString(Commons.decodeFromBase58Check(ownerAddress)));
    transactionCall.setTo(ByteArray.toHexString(Commons.decodeFromBase58Check(usdjAddress)));
    transactionCall.setData(data);

    StringBuffer sb = new StringBuffer("{\"jsonrpc\":\"2.0\",\"method\":\"eth_call\",\"params\":[");
    sb.append(transactionCall);
    sb.append(", \"latest\"],\"id\":1}");

    System.out.println(sb.toString());
  }

  public void generateCallParameterWithMethod() {
    String ownerAddress = "TRXPT6Ny7EFvTPv7mFUqaFUST39WUZ4zzz";
    String usdjAddress = "TXYZopYRdj2D9XRtbG411XZZ3kM5VkAeBf"; // nile udsj address

    byte[] addressData = Commons.decodeFromBase58Check(ownerAddress);
    byte[] addressDataWord = new byte[32];
    System.arraycopy(Commons.decodeFromBase58Check(ownerAddress), 0, addressDataWord,
        32 - addressData.length, addressData.length);
    String data = getMethodSign("name()");

    CallArguments transactionCall = new CallArguments();
    transactionCall.setFrom(ByteArray.toHexString(Commons.decodeFromBase58Check(ownerAddress)));
    transactionCall.setTo(ByteArray.toHexString(Commons.decodeFromBase58Check(usdjAddress)));
    transactionCall.setData(data);

    StringBuffer sb = new StringBuffer("{\"jsonrpc\":\"2.0\",\"method\":\"eth_call\",\"params\":[");
    sb.append(transactionCall);
    sb.append(", \"latest\"],\"id\":1}");

    System.out.println(sb.toString());
  }

  private String generateStorageParameter() {
    // nile contract：TXEphLzyv5jFwvjzwMok9UoehaSn294ZhN
    String contractAddress = "41E94EAD5F4CA072A25B2E5500934709F1AEE3C64B";

    // nile：TXvRyjomvtNWSKvNouTvAedRGD4w9RXLZD
    String sendAddress = "41F0CC5A2A84CD0F68ED1667070934542D673ACBD8";
    String index = "01";
    byte[] byte1 = new DataWord(new DataWord(sendAddress).getLast20Bytes()).getData();
    byte[] byte2 = new DataWord(new DataWord(index).getLast20Bytes()).getData();
    byte[] byte3 = ByteUtil.merge(byte1, byte2);
    String position = ByteArray.toJsonHex(Hash.sha3(byte3));

    StringBuffer sb = new StringBuffer(
        "{\"jsonrpc\":\"2.0\",\"method\":\"eth_getStorageAt\",\"params\":[\"0x");
    sb.append(contractAddress + "\",\"");
    sb.append(position + "\",");
    sb.append("\"latest\"],\"id\":1}");
    return sb.toString();
  }

  private String constructData(String functionSelector, String parameter) {
    return getMethodSign(functionSelector) + parameter;
  }

  @Test
  public void testConstructData() {
    String expectedData =
        "07211ef7000000000000000000000000000000000000000000000000000000000000000"
            + "3000000000000000000000000000000000000000000000000000000000000000100"
            + "000000000000000000000000000000000000000000000000000000000f4240";

    String functionSelector = "get_dy_underlying(int128,int128,uint256)";
    String parameter = "000000000000000000000000000000000000000000000000000000000"
        + "00000030000000000000000000000000000000000000000000000000000000000000001"
        + "00000000000000000000000000000000000000000000000000000000000f4240";
    Assert.assertEquals(expectedData, constructData(functionSelector, parameter));
  }

  @Test
  public void testGetEnergyPrice() {
    String energyPriceHistory =
        "0:100,1542607200000:20,1544724000000:10,1606240800000:40,1613044800000:140";
    Assert.assertEquals(100L, parseEnergyFee(1542607100000L, energyPriceHistory));
    Assert.assertEquals(20L, parseEnergyFee(1542607210000L, energyPriceHistory));
    Assert.assertEquals(10L, parseEnergyFee(1544724100000L, energyPriceHistory));
    Assert.assertEquals(40L, parseEnergyFee(1606240810000L, energyPriceHistory));
    Assert.assertEquals(140L, parseEnergyFee(1613044810000L, energyPriceHistory));
  }

  @Test
  public void testAddressCompatibleToByteArray() {
    String rawAddress = "548794500882809695a8a687866e76d4271a1abc";
    byte[] expectedBytes = ByteArray.fromHexString(addressPreFixString + rawAddress);

    String addressNoPre = "0x" + rawAddress;
    String addressWithPre = "0x" + addressPreFixString + rawAddress;

    try {
      Assert.assertArrayEquals(expectedBytes, addressCompatibleToByteArray(rawAddress));
      Assert.assertArrayEquals(expectedBytes, addressCompatibleToByteArray(addressNoPre));
      Assert.assertArrayEquals(expectedBytes, addressCompatibleToByteArray(addressWithPre));
    } catch (JsonRpcInvalidParamsException e) {
      Assert.fail();
    }

    try {
      addressCompatibleToByteArray(rawAddress.substring(1));
    } catch (JsonRpcInvalidParamsException e) {
      Assert.assertEquals("invalid address hash value", e.getMessage());
    }

    try {
      addressCompatibleToByteArray(rawAddress + "00");
    } catch (JsonRpcInvalidParamsException e) {
      Assert.assertEquals("invalid address hash value", e.getMessage());
    }

  }

  @Test
  public void testAddressToByteArray() {
    String rawAddress = "548794500882809695a8a687866e76d4271a1abc";
    byte[] expectedBytes = ByteArray.fromHexString(rawAddress);
    String addressNoPre = "0x" + rawAddress;
    try {
      Assert.assertArrayEquals(expectedBytes, addressToByteArray(rawAddress));
      Assert.assertArrayEquals(expectedBytes, addressToByteArray(addressNoPre));
    } catch (JsonRpcInvalidParamsException e) {
      Assert.fail();
    }

    //test padding 0 ahead if length(address) = 39
    String address1 = "048794500882809695a8a687866e76d4271a1abc";
    byte[] expectedBytes2 = ByteArray.fromHexString(address1);
    Assert.assertEquals(address1.length(), 40);
    String address2 = address1.substring(1);
    try {
      Assert.assertArrayEquals(addressToByteArray(address1), expectedBytes2);
      Assert.assertArrayEquals(addressToByteArray(address1), addressToByteArray(address2));
    } catch (JsonRpcInvalidParamsException e) {
      Assert.fail();
    }
  }

  /**
   * test address and topic parameters
   */
  @Test
  public void testLogFilter() {

    //topic must be 64 hex string
    try {
      new LogFilter(new FilterRequest(null, null, null,
          new String[] {"0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef"},
          null));
    } catch (JsonRpcInvalidParamsException e) {
      Assert.fail();
    }
    try {
      new LogFilter(new FilterRequest(null, null, null, new String[] {"0x0"}, null));
    } catch (JsonRpcInvalidParamsException e) {
      Assert.assertTrue(e.getMessage().contains("invalid topic"));
    }

    // not empty topic and null cannot be in same level
    try {
      new LogFilter(new FilterRequest(null, null, null, new String[][] {
          {"0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef", null},
      }, null));
    } catch (JsonRpcInvalidParamsException e) {
      Assert.assertTrue(e.getMessage().contains("invalid topic"));
    }

    // topic size should be <= 4
    try {
      new LogFilter(new FilterRequest(null, null, null,
          new String[] {"0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef",
              "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef",
              "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef",
              "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef",
              "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef"}, null));
    } catch (JsonRpcInvalidParamsException e) {
      Assert.assertEquals("topics size should be <= 4", e.getMessage());
    }

    //address must be 40 hex string, not 41 ahead
    try {
      new LogFilter(new FilterRequest(null, null, "0x0", null, null));
    } catch (JsonRpcInvalidParamsException e) {
      Assert.assertTrue(e.getMessage().contains("invalid address"));
    }
    try {
      new LogFilter(
          new FilterRequest(null, null, "0xaa6612f03443517ced2bdcf27958c22353ceeab9", null, null));
    } catch (JsonRpcInvalidParamsException e) {
      Assert.fail();
    }

    //address length of 42 hex string with 41 ahead will be invalid
    try {
      new LogFilter(
          new FilterRequest(null, null, "0x41aa6612f03443517ced2bdcf27958c22353ceeab9", null,
              null));
    } catch (JsonRpcInvalidParamsException e) {
      Assert.assertTrue(e.getMessage().contains("invalid address"));
    }
  }

  /**
   * test fromBlock and toBlock parameters
   */
  @Test
  public void testLogFilterWrapper() {

    // fromBlock and toBlock are both empty
    try {
      LogFilterWrapper logFilterWrapper =
          new LogFilterWrapper(new FilterRequest(null, null, null, null, null), 100, null);
      Assert.assertEquals(logFilterWrapper.getFromBlock(), 100);
      Assert.assertEquals(logFilterWrapper.getToBlock(), Long.MAX_VALUE);
    } catch (JsonRpcInvalidParamsException e) {
      Assert.fail();
    }

    // fromBlock is not empty and smaller than currentMaxBlockNum, toBlock is empty
    try {
      LogFilterWrapper logFilterWrapper =
          new LogFilterWrapper(new FilterRequest("0x14", null, null, null, null), 100, null);
      Assert.assertEquals(logFilterWrapper.getFromBlock(), 20);
      Assert.assertEquals(logFilterWrapper.getToBlock(), Long.MAX_VALUE);
    } catch (JsonRpcInvalidParamsException e) {
      Assert.fail();
    }

    // fromBlock is not empty and bigger than currentMaxBlockNum, toBlock is empty
    try {
      LogFilterWrapper logFilterWrapper =
          new LogFilterWrapper(new FilterRequest("0x78", null, null, null, null), 100, null);
      Assert.assertEquals(logFilterWrapper.getFromBlock(), 120);
      Assert.assertEquals(logFilterWrapper.getToBlock(), Long.MAX_VALUE);
    } catch (JsonRpcInvalidParamsException e) {
      Assert.fail();
    }

    // fromBlock is empty, toBlock is not empty and smaller than currentMaxBlockNum
    try {
      LogFilterWrapper logFilterWrapper =
          new LogFilterWrapper(new FilterRequest(null, "0x14", null, null, null), 100, null);
      Assert.assertEquals(logFilterWrapper.getFromBlock(), 20);
      Assert.assertEquals(logFilterWrapper.getToBlock(), 20);
    } catch (JsonRpcInvalidParamsException e) {
      Assert.fail();
    }

    // fromBlock is empty, toBlock is not empty and bigger than currentMaxBlockNum
    try {
      LogFilterWrapper logFilterWrapper =
          new LogFilterWrapper(new FilterRequest(null, "0x78", null, null, null), 100, null);
      Assert.assertEquals(logFilterWrapper.getFromBlock(), 100);
      Assert.assertEquals(logFilterWrapper.getToBlock(), 120);
    } catch (JsonRpcInvalidParamsException e) {
      Assert.fail();
    }

    // fromBlock is not empty, toBlock is not empty
    try {
      LogFilterWrapper logFilterWrapper =
          new LogFilterWrapper(new FilterRequest("0x14", "0x78", null, null, null), 100, null);
      Assert.assertEquals(logFilterWrapper.getFromBlock(), 20);
      Assert.assertEquals(logFilterWrapper.getToBlock(), 120);
    } catch (JsonRpcInvalidParamsException e) {
      Assert.fail();
    }
    try {
      LogFilterWrapper logFilterWrapper =
          new LogFilterWrapper(new FilterRequest("0x78", "0x14", null, null, null), 100, null);
    } catch (JsonRpcInvalidParamsException e) {
      Assert.assertEquals("please verify: fromBlock <= toBlock", e.getMessage());
    }

    //fromBlock or toBlock is not hex num
    try {
      LogFilterWrapper logFilterWrapper =
          new LogFilterWrapper(new FilterRequest("earliest", null, null, null, null), 100, null);
      Assert.assertEquals(logFilterWrapper.getFromBlock(), 0);
      Assert.assertEquals(logFilterWrapper.getToBlock(), Long.MAX_VALUE);
    } catch (JsonRpcInvalidParamsException e) {
      Assert.fail();
    }
    try {
      LogFilterWrapper logFilterWrapper =
          new LogFilterWrapper(new FilterRequest("latest", null, null, null, null), 100, null);
      Assert.assertEquals(logFilterWrapper.getFromBlock(), 100);
      Assert.assertEquals(logFilterWrapper.getToBlock(), Long.MAX_VALUE);
    } catch (JsonRpcInvalidParamsException e) {
      Assert.fail();
    }
    try {
      LogFilterWrapper logFilterWrapper =
          new LogFilterWrapper(new FilterRequest("pending", null, null, null, null), 100, null);
    } catch (JsonRpcInvalidParamsException e) {
      Assert.assertEquals("TAG pending not supported", e.getMessage());
    }
    try {
      LogFilterWrapper logFilterWrapper =
          new LogFilterWrapper(new FilterRequest("test", null, null, null, null), 100, null);
    } catch (JsonRpcInvalidParamsException e) {
      Assert.assertEquals("Incorrect hex syntax", e.getMessage());
    }
  }

  private int[] getBloomIndex(String s) {
    Bloom bloom = Bloom.create(Hash.sha3(ByteArray.fromHexString(s)));
    BitSet bs = BitSet.valueOf(bloom.getData());

    int[] bitIndex = new int[3]; //must same as the number of hash function in Bloom
    int nonZeroCount = 0;
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      // operate on index i here
      if (i == Integer.MAX_VALUE) {
        break; // or (i+1) would overflow
      }
      bitIndex[nonZeroCount++] = i;
    }

    return bitIndex;
  }

  @Test
  public void testGetConditions() {
    try {
      List<String> addressList = new ArrayList<>();
      addressList.add("0x57f1887a8bf19b14fc0df6fd9b2acc9af147ea85");
      addressList.add("0x0bc529c00c6401aef6d220be8c6ea1667f6ad93e");
      Object address = addressList;

      Object[] topics = new Object[3];
      topics[0] = "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef";
      topics[1] = null;
      List<String> topicList = new ArrayList<>();
      topicList.add("0x000000000000000000000000088ee5007c98a9677165d78dd2109ae4a3d04d0c");
      topicList.add("0x00000000000000000000000056178a0d5f301baf6cf3e1cd53d9863437345bf9");
      topicList.add("0x000000000000000000000000bb2b8038a1640196fbe3e38816f3e67cba72d940");
      topicList.add("0x00000000000000000000000056178a0d5f301baf6cf3e1cd53d9863437345bf9");
      topicList.add("0x00000000000000000000000056178a0d5f301baf6cf3e1cd53d9863437345bf9");
      topics[2] = topicList;

      LogFilterWrapper logFilterWrapper =
          new LogFilterWrapper(new FilterRequest(null,
              null,
              address,
              topics,
              null),
              100,
              null);

      LogBlockQuery logBlockQuery = new LogBlockQuery(logFilterWrapper, null, 100, null);
      int[][][] conditions = logBlockQuery.getConditions();
      //level = depth(address) + depth(topics), skip null
      Assert.assertEquals(3, conditions.length);
      //elements number
      Assert.assertEquals(2, conditions[0].length);
      Assert.assertEquals(1, conditions[1].length);
      Assert.assertEquals(5, conditions[2].length);

      for (int i = 0; i < conditions.length; i++) {
        for (int j = 0; j < conditions[i].length; j++) {
          Assert.assertEquals(3, conditions[i][j].length);
        }
      }

      Assert.assertArrayEquals(conditions[0][0],
          getBloomIndex("0x57f1887a8bf19b14fc0df6fd9b2acc9af147ea85"));
      Assert.assertArrayEquals(conditions[0][1],
          getBloomIndex("0x0bc529c00c6401aef6d220be8c6ea1667f6ad93e"));
      Assert.assertArrayEquals(conditions[1][0],
          getBloomIndex("0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef"));
      Assert.assertArrayEquals(conditions[2][4],
          getBloomIndex("0x00000000000000000000000056178a0d5f301baf6cf3e1cd53d9863437345bf9"));

    } catch (JsonRpcInvalidParamsException e) {
      Assert.fail();
    }
  }
}
