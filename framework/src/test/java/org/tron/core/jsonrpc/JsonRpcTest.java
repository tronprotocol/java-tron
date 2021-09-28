package org.tron.core.jsonrpc;

import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.getMethodSign;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.parseEnergyFee;

import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.crypto.Hash;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ByteUtil;
import org.tron.common.utils.Commons;
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
    transactionCall.from = ByteArray.toHexString(Commons.decodeFromBase58Check(ownerAddress));
    transactionCall.to = ByteArray.toHexString(Commons.decodeFromBase58Check(usdjAddress));
    transactionCall.data = data;

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
    transactionCall.from = ByteArray.toHexString(Commons.decodeFromBase58Check(ownerAddress));
    transactionCall.to = ByteArray.toHexString(Commons.decodeFromBase58Check(usdjAddress));
    transactionCall.data = data;

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
    String data = getMethodSign(functionSelector) + parameter;
    return data;
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
}
