package org.tron.core.jsonrpc;

import static org.tron.common.utils.DecodeUtil.addressPreFixString;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.addressCompatibleToByteArray;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.addressToByteArray;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.getMethodSign;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.parseEnergyFee;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.bloom.Bloom;
import org.tron.common.crypto.Hash;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ByteUtil;
import org.tron.common.utils.Commons;
import org.tron.core.exception.jsonrpc.JsonRpcInvalidParamsException;
import org.tron.core.services.jsonrpc.JsonRpcApiUtil;
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
      Assert.assertEquals("invalid address", e.getMessage());
    }

    try {
      addressCompatibleToByteArray(rawAddress + "00");
    } catch (JsonRpcInvalidParamsException e) {
      Assert.assertEquals("invalid address", e.getMessage());
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

    // oversized input rejected before fromHexString
    try {
      addressToByteArray("0x" + new String(new char[64]).replace('\0', 'a'));
      Assert.fail();
    } catch (JsonRpcInvalidParamsException e) {
      Assert.assertTrue(e.getMessage().contains("invalid address"));
    }

    // invalid hex char -> invalid params, not a leaked DecoderException
    try {
      addressToByteArray("0x548794500882809695a8a687866e76d4271a1abz");
      Assert.fail();
    } catch (JsonRpcInvalidParamsException e) {
      Assert.assertTrue(e.getMessage().contains("invalid address"));
    }
  }

  /**
   * test address and topic parameters
   */
  @Test
  public void testLogFilter() {

    //topic must be 63 or 64 hex string, full 64-char form here
    try {
      new LogFilter(new FilterRequest(null, null, null,
          new String[] {"0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef"},
          null));
    } catch (JsonRpcInvalidParamsException e) {
      Assert.fail();
    }

    //63-char form: leading zero stripped by some clients, padded back to the same topic
    String paddedAddressTopic =
        "000000000000000000000000f0cc5a2a84cd0f68ed1667070934542d673acbd8";
    try {
      LogFilter full = new LogFilter(new FilterRequest(null, null, null,
          new String[] {null, "0x" + paddedAddressTopic}, null));
      LogFilter stripped = new LogFilter(new FilterRequest(null, null, null,
          new String[] {null, "0x" + paddedAddressTopic.substring(1)}, null));
      Assert.assertArrayEquals(full.getTopics().get(1)[0], stripped.getTopics().get(1)[0]);
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

    // non-string element in address array -> -32602, not a leaked ClassCastException
    JsonRpcInvalidParamsException badAddrElement = Assert.assertThrows(
        JsonRpcInvalidParamsException.class,
        () -> new LogFilter(new FilterRequest(null, null,
            new ArrayList<>(Collections.singletonList(1)), null, null)));
    Assert.assertEquals("invalid address at index 0: 1", badAddrElement.getMessage());

    // non-string element in nested topic array -> -32602, not a leaked ClassCastException
    JsonRpcInvalidParamsException badTopicElement = Assert.assertThrows(
        JsonRpcInvalidParamsException.class,
        () -> new LogFilter(new FilterRequest(null, null, null,
            new Object[] {new ArrayList<>(Collections.singletonList(1))}, null)));
    Assert.assertEquals("invalid topic(s): 1", badTopicElement.getMessage());

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

  @Test
  public void testLogFilterAddressSizeLimit() {
    // Two valid 20-byte addresses (40 hex chars with 0x prefix)
    String addr1 = "0xaa6612f03443517ced2bdcf27958c22353ceeab9";
    String addr2 = "0xbb7723a04554628ced3cdf38069b433464ffbc0a";
    String addr3 = "0xcc8834b15665739def4de049f17a544575aabcd1";

    int savedLimit = CommonParameter.getInstance().jsonRpcMaxAddressSize;
    try {
      CommonParameter.getInstance().jsonRpcMaxAddressSize = 2;

      // Exactly at limit — must not throw
      ArrayList<String> atLimit = new ArrayList<>();
      atLimit.add(addr1);
      atLimit.add(addr2);
      FilterRequest frAtLimit = new FilterRequest();
      frAtLimit.setAddress(atLimit);
      try {
        new LogFilter(frAtLimit);
      } catch (JsonRpcInvalidParamsException e) {
        Assert.fail("address list at limit should not throw: " + e.getMessage());
      }

      // One over limit — must throw with expected message
      ArrayList<String> overLimit = new ArrayList<>();
      overLimit.add(addr1);
      overLimit.add(addr2);
      overLimit.add(addr3);
      FilterRequest frOverLimit = new FilterRequest();
      frOverLimit.setAddress(overLimit);
      try {
        new LogFilter(frOverLimit);
        Assert.fail("address list over limit should have thrown JsonRpcInvalidParamsException");
      } catch (JsonRpcInvalidParamsException e) {
        Assert.assertTrue(e.getMessage().contains("exceed max addresses:"));
      }

      // Limit = 0 means disabled — large list must pass
      CommonParameter.getInstance().jsonRpcMaxAddressSize = 0;
      ArrayList<String> largeList = new ArrayList<>(Collections.nCopies(500, addr1));
      FilterRequest frDisabled = new FilterRequest();
      frDisabled.setAddress(largeList);
      try {
        new LogFilter(frDisabled);
      } catch (JsonRpcInvalidParamsException e) {
        Assert.fail("limit=0 should disable the check: " + e.getMessage());
      }
    } finally {
      CommonParameter.getInstance().jsonRpcMaxAddressSize = savedLimit;
    }
  }

  private int[] getBloomIndex(String s) {
    Bloom bloom = Bloom.create(Hash.sha3(ByteArray.fromHexString(s)));
    BitSet bs = BitSet.valueOf(bloom.getData());

    List<Integer> bitIndexList = new ArrayList<>();
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      // operate on index i here
      if (i == Integer.MAX_VALUE) {
        break; // or (i+1) would overflow
      }
      bitIndexList.add(i);
    }

    return bitIndexList.stream().mapToInt(Integer::intValue).toArray();
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
              null,
              false);

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

  @Test
  public void testGetConditionWithHashCollision() {
    try {
      List<String> addressList = new ArrayList<>();
      addressList.add("0x57f1887a8bf19b14fc0df6fd9b2acc9af147ea85");
      addressList.add("0x3038114c1a1e72c5bfa8b003bc3650ad2ba254a0");

      Object[] topics = new Object[0];

      LogFilterWrapper logFilterWrapper =
          new LogFilterWrapper(new FilterRequest(null,
              null,
              addressList,
              topics,
              null),
              100,
              null,
              false);

      LogBlockQuery logBlockQuery = new LogBlockQuery(logFilterWrapper, null, 100, null);
      int[][][] conditions = logBlockQuery.getConditions();
      //level = depth(address) + depth(topics), skip null
      Assert.assertEquals(1, conditions.length);
      //elements number
      Assert.assertEquals(2, conditions[0].length);

      Assert.assertEquals(3, conditions[0][0].length);
      //Hash collision, only two nonZero position
      Assert.assertEquals(2, conditions[0][1].length);

      Assert.assertArrayEquals(conditions[0][0],
          getBloomIndex("0x57f1887a8bf19b14fc0df6fd9b2acc9af147ea85"));
      Assert.assertArrayEquals(conditions[0][1],
          getBloomIndex("0x3038114c1a1e72c5bfa8b003bc3650ad2ba254a0"));

    } catch (JsonRpcInvalidParamsException e) {
      Assert.fail();
    }
  }

  @Test
  public void testGenerateFilterId() {
    Assert.assertEquals(32, JsonRpcApiUtil.generateFilterId().length());
  }
}
