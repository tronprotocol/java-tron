package stest.tron.wallet.onlinestress.tvm;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.spongycastle.util.encoders.Hex;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class JustlendTimesTest {

  private final String testAccountKey = "549c7797b351e48ab1c6bb5857138b418012d97526fc2acba022357d49c93ac0";
  private final byte[] testAccountAddress = PublicMethed.getFinalAddress(testAccountKey);
  private final String testAccountKey2 = "553c7b0dee17d3f5b334925f5a90fe99fb0b93d47073d69ec33eead8459d171e";
  private final byte[] testAccountAddress2 = PublicMethed.getFinalAddress(testAccountKey2);
  private final String testAccountKey3 = "324a2052e491e99026442d81df4d2777292840c1b3949e20696c49096c6bacb8";
  private final byte[] testAccountAddress3 = PublicMethed.getFinalAddress(testAccountKey3);
  private final String testAccountKey4 = "ff5d867c4434ac17d264afc6696e15365832d5e8000f75733ebb336d66df148d";
  private final byte[] testAccountAddress4 = PublicMethed.getFinalAddress(testAccountKey4);
  private final String testAccountKey5 = "2925e186bb1e88988855f11ebf20ea3a6e19ed92328b0ffb576122e769d45b68";
  private final byte[] testAccountAddress5 = PublicMethed.getFinalAddress(testAccountKey5);
  private final String accountKey = "549c7797b351e48ab1c6bb5857138b418012d97526fc2acba022357d49c93ac0";
  private final byte[] accountAddress = PublicMethed.getFinalAddress(accountKey);

  private final String ownerKey = "44FE180410D7BF05E41388A881C3C5566C6667840116EC25C6FC924CE678FC4A";
  private final byte[] ownerAddress = PublicMethed.getFinalAddress(ownerKey);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private ManagedChannel channelSolidity = null;
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private static byte[] unitrAddress =
      WalletClient.decodeFromBase58Check("TTA4zDp1YWhEaP33x6WDVULXYUHeoyMrU3");
  // oracle real PriceOracle,not PriceOracleProxy
  private static byte[] priceAddress =
      WalletClient.decodeFromBase58Check("TZ4LG8K77aApJGeDM3XSPdpNDtuRAkNjzA");
  private static byte[] trxAddress =
      WalletClient.decodeFromBase58Check("TSHDQyhQ5MtNC1UTNhzG7CBUP4vHm4c78D");
  private static byte[] sunAddress =
      WalletClient.decodeFromBase58Check("TMVGAVCQKBsYGRPwr6UemjsTTdDzWc6Ve6");
  private static byte[] usdtAddress =
      WalletClient.decodeFromBase58Check("THPe3RGQeXEvuTcKMeSSWVjoUz5Y4ZoP5g");
  private static byte[] usdjAddress =
      WalletClient.decodeFromBase58Check("TTXBBgL5SyssAW9FVXtLLeWhkUcRc4GwFD");
  private static byte[] winAddress =
      WalletClient.decodeFromBase58Check("THQmoKzJxEvaTitWkRQbQLhTxEzWS7CUNZ");
  private static byte[] btcAddress =
      WalletClient.decodeFromBase58Check("TW4bNknCSZAPzWY5XUYXLcJq22Gbrv8Vug");
  private static byte[] jstAddress =
      WalletClient.decodeFromBase58Check("TSovuV9eHF3EuDncAy9cTmGQyx1S3fj7Rr");
  private static byte[] wbttAddress =
      WalletClient.decodeFromBase58Check("TQX7q8Wxh8Hri9yYbJ7K2YShxfPwDiBc9X");
  private static byte[] ethAddress =
      WalletClient.decodeFromBase58Check("TBwUUJmVb3tkMZ8vzTPM2NoNZRiKD5eRgM");
  private static byte[] eth2Address =
      WalletClient.decodeFromBase58Check("TGDpoCzdJPb4P1w8BxX9MnYEjYrfH3BKV9");
  private static byte[] eth3Address =
      WalletClient.decodeFromBase58Check("TCdMxEmvt53Lt57vkUC1MJkRycSjkv7XUk");
  private static byte[] eth4Address =
      WalletClient.decodeFromBase58Check("TWzwVjhJDuq8HwCw7Hn73pjXH5rSErSrS3");
  private static byte[] eth5Address =
      WalletClient.decodeFromBase58Check("TNtAfshWu1722j4L1NZqYi7LRijqr8LeFc");
  private static byte[] eth6Address =
      WalletClient.decodeFromBase58Check("TXnfCfMEsuR87LoFHNsniLyS24kNv7Crrr");
  String usdjTokenAddress= "TFYur8jvdRWqfAjpEbGrN9R4jiqJehPHbr";
  String jstTokenAddress= "TJ37WKR5FmymxbyXTPhvpqJva5VPmbtCgM";
  String usdtTokenAddress= "TXh43XybYbki6oHg5jZaAMXYDSS8Z8qDr8";// 6
  String sunTokenAddress= "TS9L5A2VUU6Ew1z3aSiTQfcGwkCc3dYem7";
  String winTokenAddress= "TXmCiGhZZwHQo5XQH5u5oCrsWUoToGexKP";// 6
  String btcTokenAddress= "THtV7TxKxgi8k3ki8VDEe52RgqAqkeFKD1";// 8
  String wbttTokenAddress= "TPxynQow3siB1xLPKEB1SLk8teexGukDBV";// 6
  String ethTokenAddress= "TJnQg55WKbT5fkS8YS8srpBmHZutYidrCe";
  String eth2TokenAddress= "TNgtM3opjaELAXRhbMcY8tmo41KRRbTmpc";
  String eth3TokenAddress= "TEogNoHkjwMbyZhEZ2H9NntmHUwPSDUChY";
  String eth4TokenAddress= "TW3vQdqKtwKwsPndyo52VU343GXjJecAoi";
  String eth5TokenAddress= "TEofszusKR8Sp6wNffjwTikikDCzdg4rog";
  String eth6TokenAddress= "TZCdTKCyL1ubF53WRc4aiFhBmHFU8r9Zyn";

  private static byte[] eth7Address =
      WalletClient.decodeFromBase58Check("TKogFfhggBV9PkrUT9vd5Vjc2x9dQs9B1E");
  private static byte[] eth8Address =
      WalletClient.decodeFromBase58Check("TW33icujtYYLZPBvvtsKhfjkE2qmUMWkGZ");
  private static byte[] eth9Address =
      WalletClient.decodeFromBase58Check("TSdxbcvDiwassY8pyiz8Jphd4tpid4xYZF");
  private static byte[] eth10Address =
      WalletClient.decodeFromBase58Check("TVu1viG4cTBHxhdeekK1fgNnxPbn3Ktctx");

  private static int borrowtimeoutTimes = 0;
  private static int redeemTimes = 0;
  private static int mintTimes = 0 ;
  Integer totalNum = 0;
  Integer successNum = 0;
  Integer failedNum = 0;

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(testAccountKey);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext(true)
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);
    Protocol.Block currentBlock = blockingStubFull
        .getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    final Long beforeBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    System.out.println("beforeBlockNum: "+ beforeBlockNum);
  }

  @Test(enabled = true, description = "price")
  public void price() {
    String priceOracleAddressReal = "TGZPFmfAzW2g2pyfwWzzt4P8cWgXjwsUWh";
    String cEtherAddress = "TSdrSvYpeuNuxFFR8fhHfTcvFKCY6hvL5D";
    String usdjAddress = "TLBaRhANQoJFTqre9Nf1mjuwNWjCJeYqUL";
    String jstAddress = "TF17BgPaZYbz8oxbjhriubPDsA7ArKoLX3";
    String usdtAddress = "TXYZopYRdj2D9XRtbG411XZZ3kM5VkAeBf";
    String sunAddress = "TWrZRHY9aKQZcyjpovdH6qeCEyYZrRQDZt";
    String winAddress = "TNDSHKGBmgRx9mDYA9CnxPx55nu672yQw2";
    String btcAddress = "TG9XJ75ZWcUw69W8xViEJZQ365fRupGkFP";
    String wbttAddress = "TLELxLrgD3dq6kqS4x6dEGJ7xNFMbzK95U";
    String ethAddress = "TQz9i4JygMCzizdVu8NE4BdqesrsHv1L93";


    LinkedHashMap<String, Integer> decimals = new LinkedHashMap<>();
    decimals.put("usdt", 6);
    decimals.put("usdj", 18);
    decimals.put("jst", 18);
    decimals.put("sun", 18);
    decimals.put("win", 6);
    decimals.put("btc", 8);
    decimals.put("wbtt", 6);
    decimals.put("trx", 6);
    decimals.put("eth", 18);

    LinkedHashMap<String, BigDecimal> prices = new LinkedHashMap<>();

    prices.put("usdt", new BigDecimal("36.57"));
    prices.put("usdj", new BigDecimal("36.14"));
    prices.put("jst", new BigDecimal("0.845000"));
    prices.put("sun", new BigDecimal("379"));
    prices.put("win", new BigDecimal("0.003587"));
    prices.put("btc", new BigDecimal("681027.818333"));
    prices.put("wbtt", new BigDecimal("0.011204"));
    prices.put("eth", new BigDecimal("379"));

    System.out.println("[" + getDaiFabParam(cEtherAddress, usdjAddress, jstAddress, usdtAddress, sunAddress,
        winAddress, btcAddress, wbttAddress, ethAddress)
            + "],[1000000000000000000"
        + "," + getOraclePrice(prices.get("usdj"), decimals.get("usdj"))
        + "," + getOraclePrice(prices.get("jst"), decimals.get("jst"))
        + "," + getOraclePrice(prices.get("usdt"), decimals.get("usdt"))
        + "," + getOraclePrice(prices.get("sun"), decimals.get("sun"))
        + "," + getOraclePrice(prices.get("win"), decimals.get("win"))
        + "," + getOraclePrice(prices.get("btc"), decimals.get("btc"))
        + "," + getOraclePrice(prices.get("wbtt"), decimals.get("wbtt"))
        + "," + getOraclePrice(prices.get("eth"), decimals.get("eth"))
        + "]");

  }

  private String getDaiFabParam(String... addressList) {
    String sl = "\"";
    if (addressList.length <= 0) {
      return "";
    }
    StringBuilder result = new StringBuilder(sl).append(addressList[0]).append(sl);
    for (int i = 1; i < addressList.length; i++) {
      result.append(",").append(sl).append(addressList[i]).append(sl);
    }
    return result.toString();
  }
  private String getOraclePrice(BigDecimal realPrice, Integer decmial) {
    return realPrice.multiply(new BigDecimal("10").pow(24 - decmial)).toBigInteger().toString();
  }
  /**
   * wbtt contract:testInternalTransaction001(The tokenId in the contract must be modified first!!!)
   * 1.trigger newAccount function:sendcoin trx/token to newAccount(Need to check whether the wbtt contract needs to be redeployed)
   * 2.trigger entermarket function(every account must excute once)
   * 3.trigger mintAndBorrow function
   * 4.trigger checkMarketConditions function
   * 5.trigger addNewMarket function
   */
  @Test(enabled = true, description = "newAccount")
  public void newAccount() {
    ArrayList<String> addressList = new ArrayList<>();
    addressList.add(Base58.encode58Check(ownerAddress));
//    addressList.add(Base58.encode58Check(testAccountAddress2));
//    addressList.add(Base58.encode58Check(testAccountAddress3));
//    addressList.add(Base58.encode58Check(testAccountAddress4));
//    addressList.add(Base58.encode58Check(testAccountAddress5));

    // sendcoin 1000000000
//    for (String address : addressList) {
//      System.out.println("sendcoin to --- " + address);
//      Assert.assertTrue(PublicMethed.sendcoin(PublicMethed.decode58Check(address), 1000000000_000_000L, accountAddress,
//          accountKey, blockingStubFull));
//      PublicMethed.waitProduceNextBlock(blockingStubFull);
//    }


    // mint USDJ 1000000
    /*for (String address : addressList) {
      System.out.println("mint USDJ to --- " + address);
      String addressHex = "0000000000000000000000"+ByteArray.toHexString(PublicMethed.decode58Check(address));
      logger.info("address_hex: " + addressHex);

      String numStr = "00000000000000000000000000000000000000000000d3c21bcecceda1000000";
      String argStr = addressHex+numStr;
      String txid = PublicMethed.triggerContract(PublicMethed.decode58Check(usdjTokenAddress), "mint(address,uint256)", argStr, true,
          0, maxFeeLimit, accountAddress, accountKey, blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      Optional<TransactionInfo> infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
      Assert.assertEquals(0,infoById.get().getResultValue());

      argStr = "\"" + address + "\"";
      TransactionExtention transactionExtention = PublicMethed
          .triggerConstantContractForExtention(PublicMethed.decode58Check(usdjTokenAddress), "balanceOf(address)", argStr,
              false, 0, 0, "0", 0, accountAddress, accountKey, blockingStubFull);
      Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
      Assert
          .assertEquals(numStr, ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()));
      System.out.println(address+"--- USDJ balance --- " + numStr);
    }*/

    // mint JST 1000000
    /*for (String address : addressList) {
      System.out.println("mint JST to --- " + address);
      String addressHex = "0000000000000000000000"+ByteArray.toHexString(PublicMethed.decode58Check(address));
      logger.info("address_hex: " + addressHex);

      String numStr = "00000000000000000000000000000000000000000000d3c21bcecceda1000000";
      String argStr = addressHex+numStr;
      String txid = PublicMethed.triggerContract(PublicMethed.decode58Check(jstTokenAddress), "mint(address,uint256)", argStr, true,
          0, maxFeeLimit, accountAddress, accountKey, blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      Optional<TransactionInfo> infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
      Assert.assertEquals(0,infoById.get().getResultValue());

      argStr = "\"" + address + "\"";
      TransactionExtention transactionExtention = PublicMethed
          .triggerConstantContractForExtention(PublicMethed.decode58Check(jstTokenAddress), "balanceOf(address)", argStr,
              false, 0, 0, "0", 0, accountAddress, accountKey, blockingStubFull);
      Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
      Assert
          .assertEquals(numStr, ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()));
      System.out.println(address+"--- JST balance --- " + numStr);
    }*/

    // transfer usdt 1000000
    String argStr = "6000000000000";
    String txid = "";
    Optional<TransactionInfo> infoById;
    /*String txid = PublicMethed.triggerContract(PublicMethed.decode58Check(usdtTokenAddress), "issue(uint256)", argStr, false,
        0, maxFeeLimit, accountAddress, accountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0,infoById.get().getResultValue());*/
    /*for (String address : addressList) {
      System.out.println("transfer USDT to --- " + address);
      argStr = "\"" + address + "\",1000000000000";
      txid = PublicMethed.triggerContract(PublicMethed.decode58Check(usdtTokenAddress), "transfer(address,uint256)", argStr, false,
          0, maxFeeLimit, accountAddress, accountKey, blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
      Assert.assertEquals(0,infoById.get().getResultValue());

      argStr = "\"" + address + "\"";
      TransactionExtention transactionExtention = PublicMethed
          .triggerConstantContractForExtention(PublicMethed.decode58Check(usdtTokenAddress), "balanceOf(address)", argStr,
              false, 0, 0, "0", 0, accountAddress, accountKey, blockingStubFull);
      Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
      Assert
          .assertEquals("000000000000000000000000000000000000000000000000000000e8d4a51000", ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()));
      System.out.println(address+"--- USDT balance --- " + ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()));
    }*/

    // transfer SUN 1000000
    /*for (String address : addressList) {
      System.out.println("transfer SUN to --- " + address);
      String addressHex = "0000000000000000000000"+ByteArray.toHexString(PublicMethed.decode58Check(address));
      logger.info("address_hex: " + addressHex);

      String numStr = "00000000000000000000000000000000000000000000d3c21bcecceda1000000";
      argStr = addressHex+numStr;
      txid = PublicMethed.triggerContract(PublicMethed.decode58Check(sunTokenAddress), "transfer(address,uint256)", argStr, true,
          0, maxFeeLimit, accountAddress, accountKey, blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
      Assert.assertEquals(0,infoById.get().getResultValue());

      argStr = "\"" + address + "\"";
      TransactionExtention transactionExtention = PublicMethed
          .triggerConstantContractForExtention(PublicMethed.decode58Check(sunTokenAddress), "balanceOf(address)", argStr,
              false, 0, 0, "0", 0, accountAddress, accountKey, blockingStubFull);
      Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
      Assert
          .assertEquals(numStr, ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()));
      System.out.println(address+"--- SUN balance --- " + numStr);
    }

    // mint win 1000000
    for (String address : addressList) {
      System.out.println("mint WIN to --- " + address);
      argStr = "\"" + address + "\",1000000000000";
      txid = PublicMethed.triggerContract(PublicMethed.decode58Check(winTokenAddress), "mint(address,uint256)", argStr, false,
          0, maxFeeLimit, accountAddress, accountKey, blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
      Assert.assertEquals(0,infoById.get().getResultValue());

      argStr = "\"" + address + "\"";
      TransactionExtention transactionExtention = PublicMethed
          .triggerConstantContractForExtention(PublicMethed.decode58Check(winTokenAddress), "balanceOf(address)", argStr,
              false, 0, 0, "0", 0, accountAddress, accountKey, blockingStubFull);
      Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
      Assert
          .assertEquals("000000000000000000000000000000000000000000000000000000e8d4a51000", ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()));
      System.out.println(address+"--- WIN balance --- " + ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()));
    }*/

    // transfer btc 1000000
    /*argStr = "600000000000000";
    txid = PublicMethed.triggerContract(PublicMethed.decode58Check(btcTokenAddress), "issue(uint256)", argStr, false,
        0, maxFeeLimit, accountAddress, accountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0,infoById.get().getResultValue());
    for (String address : addressList) {
      System.out.println("transfer BTC to --- " + address);
      argStr = "\"" + address + "\",100000000000000";
      txid = PublicMethed.triggerContract(PublicMethed.decode58Check(btcTokenAddress), "transfer(address,uint256)", argStr, false,
          0, maxFeeLimit, accountAddress, accountKey, blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
      Assert.assertEquals(0,infoById.get().getResultValue());

      argStr = "\"" + address + "\"";
      TransactionExtention transactionExtention = PublicMethed
          .triggerConstantContractForExtention(PublicMethed.decode58Check(btcTokenAddress), "balanceOf(address)", argStr,
              false, 0, 0, "0", 0, accountAddress, accountKey, blockingStubFull);
      Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
      Assert
          .assertEquals("00000000000000000000000000000000000000000000000000005af3107a4000", ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()));
      System.out.println(address+"--- BTC balance --- " + ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()));
    }

    // transfer wbtt 1000000
    txid = PublicMethed.triggerContract(PublicMethed.decode58Check(wbttTokenAddress), "deposit()", "#", false,
        0, maxFeeLimit, "1003614", 6000000000000L, accountAddress, accountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0,infoById.get().getResultValue());
    for (String address : addressList) {
      System.out.println("transfer WBTT to --- " + address);
      argStr = "\"" + address + "\",1000000000000";
      txid = PublicMethed.triggerContract(PublicMethed.decode58Check(wbttTokenAddress), "transfer(address,uint256)", argStr, false,
          0, maxFeeLimit, accountAddress, accountKey, blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
      Assert.assertEquals(0,infoById.get().getResultValue());

      argStr = "\"" + address + "\"";
      TransactionExtention transactionExtention = PublicMethed
          .triggerConstantContractForExtention(PublicMethed.decode58Check(wbttTokenAddress), "balanceOf(address)", argStr,
              false, 0, 0, "0", 0, accountAddress, accountKey, blockingStubFull);
      Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
      Assert
          .assertEquals("000000000000000000000000000000000000000000000000000000e8d4a51000", ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()));
      System.out.println(address+"--- WBTT balance --- " + ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()));
    }*/

    // transfer eth 1000000
    argStr = "00000000000000000000000000000000000000000004f68ca6d8cd91c6000000";
    txid = PublicMethed.triggerContract(PublicMethed.decode58Check(ethTokenAddress), "issue(uint256)", argStr, true,
        0, maxFeeLimit, accountAddress, accountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0,infoById.get().getResultValue());
    for (String address : addressList) {
      System.out.println("transfer ETH to --- " + address);
      String addressHex = "0000000000000000000000"+ByteArray.toHexString(PublicMethed.decode58Check(address));
      logger.info("address_hex: " + addressHex);

      String numStr = "00000000000000000000000000000000000000000000d3c21bcecceda1000000";
      argStr = addressHex+numStr;
      txid = PublicMethed.triggerContract(PublicMethed.decode58Check(ethTokenAddress), "transfer(address,uint256)", argStr, true,
          0, maxFeeLimit, accountAddress, accountKey, blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
      Assert.assertEquals(0,infoById.get().getResultValue());

      argStr = "\"" + address + "\"";
      TransactionExtention transactionExtention = PublicMethed
          .triggerConstantContractForExtention(PublicMethed.decode58Check(ethTokenAddress), "balanceOf(address)", argStr,
              false, 0, 0, "0", 0, accountAddress, accountKey, blockingStubFull);
      Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
      Assert
          .assertEquals(numStr, ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()));
      System.out.println(address+"--- ETH balance --- " + numStr);
    }

    // transfer eth2 1000000
    argStr = "00000000000000000000000000000000000000000004f68ca6d8cd91c6000000";
    txid = PublicMethed.triggerContract(PublicMethed.decode58Check(eth2TokenAddress), "issue(uint256)", argStr, true,
        0, maxFeeLimit, accountAddress, accountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0,infoById.get().getResultValue());
    for (String address : addressList) {
      System.out.println("transfer ETH2 to --- " + address);
      String addressHex = "0000000000000000000000"+ByteArray.toHexString(PublicMethed.decode58Check(address));
      logger.info("address_hex: " + addressHex);

      String numStr = "00000000000000000000000000000000000000000000d3c21bcecceda1000000";
      argStr = addressHex+numStr;
      txid = PublicMethed.triggerContract(PublicMethed.decode58Check(eth2TokenAddress), "transfer(address,uint256)", argStr, true,
          0, maxFeeLimit, accountAddress, accountKey, blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
      Assert.assertEquals(0,infoById.get().getResultValue());

      argStr = "\"" + address + "\"";
      TransactionExtention transactionExtention = PublicMethed
          .triggerConstantContractForExtention(PublicMethed.decode58Check(eth2TokenAddress), "balanceOf(address)", argStr,
              false, 0, 0, "0", 0, accountAddress, accountKey, blockingStubFull);
      Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
      Assert
          .assertEquals(numStr, ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()));
      System.out.println(address+"--- ETH2 balance --- " + numStr);
    }

    // transfer eth3 1000000
    argStr = "00000000000000000000000000000000000000000004f68ca6d8cd91c6000000";
    txid = PublicMethed.triggerContract(PublicMethed.decode58Check(eth3TokenAddress), "issue(uint256)", argStr, true,
        0, maxFeeLimit, accountAddress, accountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0,infoById.get().getResultValue());
    for (String address : addressList) {
      System.out.println("transfer ETH3 to --- " + address);
      String addressHex = "0000000000000000000000"+ByteArray.toHexString(PublicMethed.decode58Check(address));
      logger.info("address_hex: " + addressHex);

      String numStr = "00000000000000000000000000000000000000000000d3c21bcecceda1000000";
      argStr = addressHex+numStr;
      txid = PublicMethed.triggerContract(PublicMethed.decode58Check(eth3TokenAddress), "transfer(address,uint256)", argStr, true,
          0, maxFeeLimit, accountAddress, accountKey, blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
      Assert.assertEquals(0,infoById.get().getResultValue());

      argStr = "\"" + address + "\"";
      TransactionExtention transactionExtention = PublicMethed
          .triggerConstantContractForExtention(PublicMethed.decode58Check(eth3TokenAddress), "balanceOf(address)", argStr,
              false, 0, 0, "0", 0, accountAddress, accountKey, blockingStubFull);
      Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
      Assert
          .assertEquals(numStr, ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()));
      System.out.println(address+"--- ETH3 balance --- " + numStr);
    }

    // transfer eth4 1000000
    argStr = "00000000000000000000000000000000000000000004f68ca6d8cd91c6000000";
    txid = PublicMethed.triggerContract(PublicMethed.decode58Check(eth4TokenAddress), "issue(uint256)", argStr, true,
        0, maxFeeLimit, accountAddress, accountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0,infoById.get().getResultValue());
    for (String address : addressList) {
      System.out.println("transfer ETH4 to --- " + address);
      String addressHex = "0000000000000000000000"+ByteArray.toHexString(PublicMethed.decode58Check(address));
      logger.info("address_hex: " + addressHex);

      String numStr = "00000000000000000000000000000000000000000000d3c21bcecceda1000000";
      argStr = addressHex+numStr;
      txid = PublicMethed.triggerContract(PublicMethed.decode58Check(eth4TokenAddress), "transfer(address,uint256)", argStr, true,
          0, maxFeeLimit, accountAddress, accountKey, blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
      Assert.assertEquals(0,infoById.get().getResultValue());

      argStr = "\"" + address + "\"";
      TransactionExtention transactionExtention = PublicMethed
          .triggerConstantContractForExtention(PublicMethed.decode58Check(eth4TokenAddress), "balanceOf(address)", argStr,
              false, 0, 0, "0", 0, accountAddress, accountKey, blockingStubFull);
      Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
      Assert
          .assertEquals(numStr, ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()));
      System.out.println(address+"--- ETH4 balance --- " + numStr);
    }

    // transfer eth5 1000000
    argStr = "00000000000000000000000000000000000000000004f68ca6d8cd91c6000000";
    txid = PublicMethed.triggerContract(PublicMethed.decode58Check(eth5TokenAddress), "issue(uint256)", argStr, true,
        0, maxFeeLimit, accountAddress, accountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0,infoById.get().getResultValue());
    for (String address : addressList) {
      System.out.println("transfer ETH5 to --- " + address);
      String addressHex = "0000000000000000000000"+ByteArray.toHexString(PublicMethed.decode58Check(address));
      logger.info("address_hex: " + addressHex);

      String numStr = "00000000000000000000000000000000000000000000d3c21bcecceda1000000";
      argStr = addressHex+numStr;
      txid = PublicMethed.triggerContract(PublicMethed.decode58Check(eth5TokenAddress), "transfer(address,uint256)", argStr, true,
          0, maxFeeLimit, accountAddress, accountKey, blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
      Assert.assertEquals(0,infoById.get().getResultValue());

      argStr = "\"" + address + "\"";
      TransactionExtention transactionExtention = PublicMethed
          .triggerConstantContractForExtention(PublicMethed.decode58Check(eth5TokenAddress), "balanceOf(address)", argStr,
              false, 0, 0, "0", 0, accountAddress, accountKey, blockingStubFull);
      Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
      Assert
          .assertEquals(numStr, ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()));
      System.out.println(address+"--- ETH5 balance --- " + numStr);
    }
  }

  @Test(enabled = true)
  public void entermarket() {
    entermarketAll();
  }

  @Test(enabled = true, description = "addNewMarket")
  public void addNewMarket() {
    String address = Base58.encode58Check(eth4Address);
    String name = "eth4";
    supportMaket(address);

    Map<String, String> map = new HashMap<String, String>();
    map.put(eth3TokenAddress, "379000000");
    for (String key : map.keySet()) {
      System.out.println(key + ":" + map.get(key));
      setprice(key,map.get(key));
    }
    entermarket(address,name);
    setCollateralFactor(address);

    approve();

    mintAndBorrow();

  }

  @Test(enabled = true, description = "trx")
  public void test01price() {
    String usdj="TJ7KV8qKAyAFkdYu1Xr4mEAmhieNJ24ZmF";
    String jst="TYrR7VCv1UcWb18ndBkewvhSApf97vnXEf";
    String usdt="TSyUnhJdZ8NFNBawRzS8JDatwbFKiuFJVU";
    String sun="TD6z6KmS2EWh1Bg2gwP5cqSaAvchZizHPq";
    String win="TVQSiwAf3EkGdr5sKAJ1kywaGnRqhuSBeo";
    String btc="TE8P2NpxGjn4vF4G3ZvAXkeaog3Zu2mnrz";
    String wbtt="TM2G2JeGVEzbHnougpn9U3PQhoFNyYdqd9";
    String eth="TMjF58nf7X4h721tFJCjebMiHWpmJhRuUB";
    String eth2="TEvRu1eq3wTZromoUuViMEnD9nhieVkLrv";
    // usdj-price:36140000,jst-price:845000,usdt-price:36570000000000000000,sun-price:379000000,win-price:3587000000000000,btc-price:6810278183330000000000,wbtt-price:11204000000000000

    Map<String, String> map = new HashMap<String, String>();
//    map.put(usdj, "36140000");
//    map.put(jst,"8450000");
//    map.put(usdt,"36570000000000000000");
//    map.put(sun,"379000000");
//    map.put(win,"3587000000000000");
//    map.put(btc,"6810278183330000000000");
//    map.put(wbtt,"11204000000000000");
//    map.put(eth,"379000000");
//    map.put(eth2,"379000000");

    map.put(jstTokenAddress,"jstTokenAddress");
    map.put(usdtTokenAddress,"usdtTokenAddress");
    map.put(usdjTokenAddress,"usdjTokenAddress");
    map.put(sunTokenAddress,"sunTokenAddress");
    map.put(winTokenAddress,"winTokenAddress");
    map.put(btcTokenAddress,"btcTokenAddress");
    map.put(wbttTokenAddress,"wbttTokenAddress");
    map.put(ethTokenAddress,"ethTokenAddress");
    map.put(eth2TokenAddress,"eth2TokenAddress");
    map.put(eth3TokenAddress,"eth3TokenAddress");
    map.put(eth4TokenAddress,"eth4TokenAddress");
    map.put(eth5TokenAddress,"eth5TokenAddress");
    map.put(eth6TokenAddress,"eth6TokenAddress");
    map.put(usdjTokenAddress,"usdjTokenAddress");

    for (String key : map.keySet()) {
      System.out.println(key + ":" + map.get(key));
      setprice(key,map.get(key));
    }
  }

  @Test(enabled = true,threadPoolSize = 5, invocationCount = 5, description = "")
  public void test01mint() {
    for (int i = 0; i < 40; i++) {
      mint(trxAddress, 100000L, "trx");
      mint(sunAddress, 10000000000000000L, "sun");
      mint(jstAddress, 10000000000000000L, "jst");
      mint(winAddress, 100000L, "win");
      mint(wbttAddress, 100000L, "wbtt");
      mint(btcAddress, 10000000L, "btc");
      mint(usdjAddress, 10000000000000000L, "usdj");
      mint(usdtAddress, 100000L, "usdt");
      mint(ethAddress, 10000000000000000L, "eth");
      mint(eth2Address, 10000000000000000L, "eth2");
      mint(eth3Address, 10000000000000000L, "eth3");
      mint(eth4Address, 10000000000000000L, "eth4");
//      mint(eth5Address, 10000000000000000L, "eth5");
//      mint(eth6Address, 10000000000000000L, "eth6");
    }
  }

  @Test(enabled = true,threadPoolSize = 5, invocationCount = 5, description = "")
  public void test01redeemUnderlying() {
    /*decimals.put("usdt", 6);
    decimals.put("usdj", 18);
    decimals.put("jst", 18);
    decimals.put("sun", 18);
    decimals.put("eth", 18);
    decimals.put("win", 6);
    decimals.put("btc", 8);
    decimals.put("wbtt", 6);
    decimals.put("trx", 6);*/
    for (int i = 0; i < 40; i++) {
      redeemUnderlying(trxAddress, 100000L, "trx");
      redeemUnderlying(sunAddress, 10000000000000000L, "sun");
      redeemUnderlying(jstAddress, 10000000000000000L, "jst");
      redeemUnderlying(winAddress, 100000L, "win");
      redeemUnderlying(wbttAddress, 100000L, "wbtt");
      redeemUnderlying(btcAddress, 10000000L, "btc");
      redeemUnderlying(usdjAddress, 10000000000000000L, "usdj");
      redeemUnderlying(usdtAddress, 100000L, "usdt");
      redeemUnderlying(ethAddress, 10000000000000000L, "eth");
      redeemUnderlying(eth2Address, 10000000000000000L, "eth2");
      redeemUnderlying(eth3Address, 10000000000000000L, "eth3");
      redeemUnderlying(eth4Address, 10000000000000000L, "eth4");
//      redeemUnderlying(eth5Address, 10000000000000000L, "eth5");
//      redeemUnderlying(eth6Address, 10000000000000000L, "eth6");
//      redeemUnderlying(eth7Address, 10000000000000000L, "eth7");
//      redeemUnderlying(eth8Address, 10000000000000000L, "eth8");
//      redeemUnderlying(eth9Address, 10000000000000000L, "eth9");
//      redeemUnderlying(eth10Address, 10000000000000000L, "eth10");
    }
  }


  @Test(enabled = true,threadPoolSize = 5, invocationCount = 5, description = "")
  public void test01borrow() {
    for (int i = 0; i < 40; i++) {
      borrow(trxAddress, 100000L, "trx");
      borrow(sunAddress, 10000000000000000L, "sun");
      borrow(jstAddress, 10000000000000000L, "jst");
      borrow(winAddress, 100000L, "win");
      borrow(wbttAddress, 100000L, "wbtt");
      borrow(btcAddress, 10000000L, "btc");
      borrow(usdjAddress, 10000000000000000L, "usdj");
      borrow(usdtAddress, 100000L, "usdt");
      borrow(ethAddress, 10000000000000000L, "eth");
      borrow(eth2Address, 10000000000000000L, "eth2");
      borrow(eth3Address, 10000000000000000L, "eth3");
      borrow(eth4Address, 10000000000000000L, "eth4");
//      borrow(eth5Address, 10000000000000000L, "eth5");
      /*borrow(eth6Address, 10000000000000000L, "eth6");
      borrow(eth7Address, 10000000000000000L, "eth7");
      borrow(eth8Address, 10000000000000000L, "eth8");
      borrow(eth9Address, 10000000000000000L, "eth9");
      borrow(eth10Address, 10000000000000000L, "eth10");*/
    }
  }

  @Test(enabled = true,threadPoolSize = 5, invocationCount = 5, description = "")
  public void test01repayBorrow() {
    for (int i = 0; i < 40; i++) {
      repayBorrow(trxAddress, 100000L, "trx");
      repayBorrow(sunAddress, 10000000000000000L, "sun");
      repayBorrow(jstAddress, 10000000000000000L, "jst");
      repayBorrow(winAddress, 100000L, "win");
      repayBorrow(wbttAddress, 100000L, "wbtt");
      repayBorrow(btcAddress, 10000000L, "btc");
      repayBorrow(usdjAddress, 10000000000000000L, "usdj");
      repayBorrow(usdtAddress, 100000L, "usdt");
      repayBorrow(ethAddress, 10000000000000000L, "eth");
      repayBorrow(eth2Address, 10000000000000000L, "eth2");
      repayBorrow(eth3Address, 10000000000000000L, "eth3");
      repayBorrow(eth4Address, 10000000000000000L, "eth4");
//      repayBorrow(eth5Address, 10000000000000000L, "eth5");
    }
  }


  /**
   * justlend stress:
   * 1.approve
   * 2.entermarketAll
   * 3.mintAndBorrow
   * 4.checkMarketConditions
   */
  @Test(enabled = true)
  public void checkMarketConditions() {
    HashMap<String, byte[]> addressMap = new HashMap<>();
    addressMap.put("trx", trxAddress);
    addressMap.put("usdj", usdjAddress);
    addressMap.put("jst", jstAddress);
    addressMap.put("usdt", usdtAddress);
    addressMap.put("sun", sunAddress);
    addressMap.put("win", winAddress);
    addressMap.put("btc", btcAddress);
    addressMap.put("wbtt", wbttAddress);
    addressMap.put("eth", ethAddress);
    addressMap.put("eth2", eth2Address);
    addressMap.put("eth3", eth3Address);
    addressMap.put("eth4", eth4Address);
    addressMap.put("eth5", eth5Address);
    addressMap.put("eth6", eth6Address);
    for (String ctoken:addressMap.keySet()) {
      TransactionExtention transactionExtention = PublicMethed
          .triggerConstantContractForExtention(addressMap.get(ctoken), "getCash()", "#", false, 0,
              0, "0", 0, testAccountAddress, testAccountKey, blockingStubFull);
      byte[] result = transactionExtention.getConstantResult(0).toByteArray();
      BigInteger cash = new BigInteger(Hex.toHexString(result).substring(0,64),16);

      transactionExtention = PublicMethed
          .triggerConstantContractForExtention(addressMap.get(ctoken), "totalBorrows()", "#", false, 0,
              0, "0", 0, testAccountAddress, testAccountKey, blockingStubFull);
      result = transactionExtention.getConstantResult(0).toByteArray();
      BigInteger totalBorrows = new BigInteger(Hex.toHexString(result),16);

      transactionExtention = PublicMethed
          .triggerConstantContractForExtention(addressMap.get(ctoken), "totalReserves()", "#", false, 0,
              0, "0", 0, testAccountAddress, testAccountKey, blockingStubFull);
      result = transactionExtention.getConstantResult(0).toByteArray();
      BigInteger totalReserves = new BigInteger(Hex.toHexString(result),16);

      transactionExtention = PublicMethed
          .triggerConstantContractForExtention(addressMap.get(ctoken), "totalSupply()", "#", false, 0,
              0, "0", 0, testAccountAddress, testAccountKey, blockingStubFull);
      result = transactionExtention.getConstantResult(0).toByteArray();
      BigInteger totalSupply = new BigInteger(Hex.toHexString(result),16);
      System.out.println(ctoken+": "+Base58.encode58Check(addressMap.get(ctoken))+": cash: "+cash+",totalBorrows: "+totalBorrows+",totalReserves: "+totalReserves+",totalSupply: "+totalSupply);
    }
  }

  @Test(enabled = true)
  public void mintAndBorrow() {
    String txid;
    Optional<TransactionInfo>  infoById;
    // trx
    txid = PublicMethed.triggerContract(trxAddress,
        "mint(uint256)", "100000000000000", false,
        100000000000000l, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(0,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));
    txid = PublicMethed.triggerContract(trxAddress,
        "borrow(uint256)", "10000000000", false,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(0,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));

    // eth10
    /*
    String txid = PublicMethed.triggerContract(eth10Address,
        "mint(uint256)", "00000000000000000000000000000000000000000000152d02c7e14af6800000", true,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(0,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));
    txid = PublicMethed.triggerContract(eth10Address,
        "borrow(uint256)", "00000000000000000000000000000000000000000000021e19e0c9bab2400000", true,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(0,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));*/

    // eth9
    /*
    String txid = PublicMethed.triggerContract(eth9Address,
        "mint(uint256)", "00000000000000000000000000000000000000000000152d02c7e14af6800000", true,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(0,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));
    txid = PublicMethed.triggerContract(eth9Address,
        "borrow(uint256)", "00000000000000000000000000000000000000000000021e19e0c9bab2400000", true,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(0,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));*/

    // eth8
    /*
    String txid = PublicMethed.triggerContract(eth8Address,
        "mint(uint256)", "00000000000000000000000000000000000000000000152d02c7e14af6800000", true,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(0,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));
    txid = PublicMethed.triggerContract(eth8Address,
        "borrow(uint256)", "00000000000000000000000000000000000000000000021e19e0c9bab2400000", true,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(0,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));*/

    // eth7
    /*
    String txid = PublicMethed.triggerContract(eth7Address,
        "mint(uint256)", "00000000000000000000000000000000000000000000152d02c7e14af6800000", true,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(0,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));
    txid = PublicMethed.triggerContract(eth7Address,
        "borrow(uint256)", "00000000000000000000000000000000000000000000021e19e0c9bab2400000", true,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(0,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));*/

    // eth6
    txid = PublicMethed.triggerContract(eth6Address,
        "mint(uint256)", "00000000000000000000000000000000000000000000152d02c7e14af6800000", true,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(0,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));
    txid = PublicMethed.triggerContract(eth6Address,
        "borrow(uint256)", "00000000000000000000000000000000000000000000021e19e0c9bab2400000", true,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(0,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));

    // eth5 18
    txid = PublicMethed.triggerContract(eth5Address,
        "mint(uint256)", "00000000000000000000000000000000000000000000152d02c7e14af6800000", true,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(0,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));
    txid = PublicMethed.triggerContract(eth5Address,
        "borrow(uint256)", "00000000000000000000000000000000000000000000021e19e0c9bab2400000", true,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(0,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));

    // eth4 18
    txid = PublicMethed.triggerContract(eth4Address,
        "mint(uint256)", "00000000000000000000000000000000000000000000152d02c7e14af6800000", true,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(0,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));
    txid = PublicMethed.triggerContract(eth4Address,
        "borrow(uint256)", "00000000000000000000000000000000000000000000021e19e0c9bab2400000", true,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(0,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));

    // eth3 18
    txid = PublicMethed.triggerContract(eth3Address,
        "mint(uint256)", "00000000000000000000000000000000000000000000152d02c7e14af6800000", true,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(0,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));
    txid = PublicMethed.triggerContract(eth3Address,
        "borrow(uint256)", "00000000000000000000000000000000000000000000021e19e0c9bab2400000", true,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(0,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));

    // eth2 18
    txid = PublicMethed.triggerContract(eth2Address,
        "mint(uint256)", "00000000000000000000000000000000000000000000152d02c7e14af6800000", true,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(0,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));
    txid = PublicMethed.triggerContract(eth2Address,
        "borrow(uint256)", "00000000000000000000000000000000000000000000021e19e0c9bab2400000", true,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(0,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));

    // jst 18
    txid = PublicMethed.triggerContract(jstAddress,
        "mint(uint256)", "00000000000000000000000000000000000000000000152d02c7e14af6800000", true,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(0,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));
    txid = PublicMethed.triggerContract(jstAddress,
        "borrow(uint256)", "00000000000000000000000000000000000000000000021e19e0c9bab2400000", true,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(0,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));

    // usdj 18
    txid = PublicMethed.triggerContract(usdjAddress,
        "mint(uint256)", "00000000000000000000000000000000000000000000152d02c7e14af6800000", true,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(0,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));
    txid = PublicMethed.triggerContract(usdjAddress,
        "borrow(uint256)", "00000000000000000000000000000000000000000000021e19e0c9bab2400000", true,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(0,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));

    // usdt 6
    txid = PublicMethed.triggerContract(usdtAddress,
        "mint(uint256)", "100000000000", false,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(0,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));
    txid = PublicMethed.triggerContract(usdtAddress,
        "borrow(uint256)", "10000000000", false,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(0,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));

    // sun 18
    txid = PublicMethed.triggerContract(sunAddress,
        "mint(uint256)", "00000000000000000000000000000000000000000000152d02c7e14af6800000", true,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(0,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));
    txid = PublicMethed.triggerContract(sunAddress,
        "borrow(uint256)", "00000000000000000000000000000000000000000000021e19e0c9bab2400000", true,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(0,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));

    // win 6
    txid = PublicMethed.triggerContract(winAddress,
        "mint(uint256)", "100000000000", false,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(0,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));
    txid = PublicMethed.triggerContract(winAddress,
        "borrow(uint256)", "10000000000", false,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(0,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));

    // btc 8
    txid = PublicMethed.triggerContract(btcAddress,
        "mint(uint256)", "10000000000000", false,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(0,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));
    txid = PublicMethed.triggerContract(btcAddress,
        "borrow(uint256)", "1000000000000", false,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(0,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));

    // wbtt 6
    txid = PublicMethed.triggerContract(wbttAddress,
        "mint(uint256)", "100000000000", false,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(0,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));
    txid = PublicMethed.triggerContract(wbttAddress,
        "borrow(uint256)", "10000000000", false,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(0,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));

    // eth 18
    txid = PublicMethed.triggerContract(ethAddress,
        "mint(uint256)", "00000000000000000000000000000000000000000000152d02c7e14af6800000", true,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(0,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));
    txid = PublicMethed.triggerContract(ethAddress,
        "borrow(uint256)", "00000000000000000000000000000000000000000000021e19e0c9bab2400000", true,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(0,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));
  }

  @Test(enabled = true)
  public void approve() {

    String txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(usdjTokenAddress),
        "approve(address,uint256)", "\""+Base58.encode58Check(usdjAddress)+"\",-1", false,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(1,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));
    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(jstTokenAddress),
        "approve(address,uint256)", "\""+Base58.encode58Check(jstAddress)+"\",-1", false,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(1,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));

    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(usdtTokenAddress),
        "approve(address,uint256)", "\""+Base58.encode58Check(usdtAddress)+"\",-1", false,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(1,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));

    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(sunTokenAddress),
        "approve(address,uint256)", "\""+Base58.encode58Check(sunAddress)+"\",-1", false,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(1,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));

    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(winTokenAddress),
        "approve(address,uint256)", "\""+Base58.encode58Check(winAddress)+"\",-1", false,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(1,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));

    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(btcTokenAddress),
        "approve(address,uint256)", "\""+Base58.encode58Check(btcAddress)+"\",-1", false,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(1,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));

    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(wbttTokenAddress),
        "approve(address,uint256)", "\""+Base58.encode58Check(wbttAddress)+"\",-1", false,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(1,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));

    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(ethTokenAddress),
        "approve(address,uint256)", "\""+Base58.encode58Check(ethAddress)+"\",-1", false,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(1,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));

    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(eth2TokenAddress),
        "approve(address,uint256)", "\""+Base58.encode58Check(eth2Address)+"\",-1", false,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(1,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));

    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(eth3TokenAddress),
        "approve(address,uint256)", "\""+Base58.encode58Check(eth3Address)+"\",-1", false,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(1,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));

    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(eth4TokenAddress),
        "approve(address,uint256)", "\""+Base58.encode58Check(eth4Address)+"\",-1", false,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(1,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));

    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(eth5TokenAddress),
        "approve(address,uint256)", "\""+Base58.encode58Check(eth5Address)+"\",-1", false,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(1,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));

    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(eth6TokenAddress),
        "approve(address,uint256)", "\""+Base58.encode58Check(eth6Address)+"\",-1", false,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(1,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));

    String argStr = "\"" + Base58.encode58Check(testAccountAddress) + "\",\"" + Base58.encode58Check(eth6Address) + "\"";
    TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(eth6TokenAddress), "allowance(address,address)", argStr,
            false, 0, 0, "0", 0, testAccountAddress, testAccountKey, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    System.out.println(testAccountAddress+"--- allowance --- " + ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()));
  }

  public void mint(byte[] Address, Long value, String flag) {
    System.out.println(flag + " mint starting......");
    String txid = PublicMethed.triggerContract(Address,
        "mint(uint256)", "\""+value.intValue()+"\"", false,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
//    PublicMethed.waitProduceNextBlock(blockingStubFull);
//    Optional<Protocol.TransactionInfo> infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
//    if (infoById.get().getReceipt().getResult().equals(contractResult.SUCCESS)) {
//      successNum++;
//    } else {
//      failedNum++;
//      logger.info("errorInfo:"+infoById.get().getReceipt().getResult().name());
//    }
//    Assert.assertEquals(0,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));
    System.out.println(flag + " mint ending......");
  }

  public void borrow(byte[] Address, Long value, String flag) {
    System.out.println(flag + " borrow starting......");
    String txid = PublicMethed.triggerContract(Address,
        "borrow(uint256)", "\""+value.intValue()+"\"", false,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
//    PublicMethed.waitProduceNextBlock(blockingStubFull);
//    Optional<Protocol.TransactionInfo> infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
//    if (infoById.get().getReceipt().getResult().equals(contractResult.SUCCESS)) {
//      successNum++;
//    } else {
//      failedNum++;
//      logger.info("errorInfo:"+infoById.get().getReceipt().getResult().name());
//    }
//    Assert.assertEquals(0, ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));
    System.out.println(flag + " borrow ending......");
  }

  public void redeemUnderlying(byte[] Address, Long value, String flag) {
    System.out.println(flag + " redeem starting......");
    String txid = PublicMethed.triggerContract(Address,
        "redeemUnderlying(uint256)", "\""+value.intValue()+"\"", false,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
//    PublicMethed.waitProduceNextBlock(blockingStubFull);
//    Optional<Protocol.TransactionInfo> infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
//    if (infoById.get().getReceipt().getResult().equals(contractResult.SUCCESS)) {
//      successNum++;
//    } else {
//      failedNum++;
//      logger.info("errorInfo:"+infoById.get().getReceipt().getResult().name());
//    }
//    Assert.assertEquals(0,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));
    System.out.println(flag + " redeem ending......");
  }

  public void repayBorrow(byte[] Address, Long value, String flag) {
    System.out.println(flag + " repayBorrow starting......");
    String txid = PublicMethed.triggerContract(Address,
        "repayBorrow(uint256)", "\""+value.intValue()+"\"", false,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
//    PublicMethed.waitProduceNextBlock(blockingStubFull);
//    Optional<Protocol.TransactionInfo> infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
//    if (infoById.get().getReceipt().getResult().equals(contractResult.SUCCESS)) {
//      successNum++;
//    } else {
//      failedNum++;
//      logger.info("errorInfo:"+infoById.get().getReceipt().getResult().name());
//    }
//    Assert.assertEquals(0,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));
    System.out.println(flag + " repayBorrow ending......");
  }

  public void entermarket(String address,String name) {
    HashMap<String, String> addressMap = new HashMap<>();
    addressMap.put(name, address);
    for (String ctoken:addressMap.keySet()) {
      System.out.println(ctoken + ":" + addressMap.get(ctoken));
      String param = "\"" + addressMap.get(ctoken) + "\"";
      String txid = PublicMethed.triggerContract(unitrAddress,
          "enterMarket(address)", param, false,
          0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
      Optional<TransactionInfo> infoById = null;
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
      Assert.assertTrue(infoById.get().getResultValue() == 0);
      Assert.assertEquals(0,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));
    }
  }

  public void entermarketAll() {
    HashMap<String, byte[]> addressMap = new HashMap<>();
    addressMap.put("trx", trxAddress);
    addressMap.put("usdj", usdjAddress);
    addressMap.put("jst", jstAddress);
    addressMap.put("usdt", usdtAddress);
    addressMap.put("sun", sunAddress);
    addressMap.put("win", winAddress);
    addressMap.put("btc", btcAddress);
    addressMap.put("wbtt", wbttAddress);
    addressMap.put("eth", ethAddress);
    addressMap.put("eth1", eth2Address);
    addressMap.put("eth2", eth3Address);
    addressMap.put("eth3", eth4Address);
    addressMap.put("eth4", eth5Address);
    addressMap.put("et5", eth6Address);
    String params = "[";
    for (String ctoken:addressMap.keySet()) {
      System.out.println(ctoken + ":" + addressMap.get(ctoken));
      String param = "\"" + Base58.encode58Check(addressMap.get(ctoken)) + "\"";
      params += param+",";
    }
    params = params.substring(0,params.length()-1)+"]";
    System.out.println("params:"+params);
    String txid = PublicMethed.triggerContract(unitrAddress,
        "enterMarkets(address[])", params, false,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
  }

  public void setprice(String add, String price) {
    String param = "\"" + add + "\"";
    TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(priceAddress, "assetPrices(address)", param, false, 0,
            0,
            "0", 0, testAccountAddress, testAccountKey, blockingStubFull);
    byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    BigInteger p = new BigInteger(Hex.toHexString(result),16);
    System.out.println("第一次查询价格：" +price+":"+ p);
//    BigInteger bigprice=new BigInteger(price);
    /*if (bigprice.equals(p)) {
      System.out.println("价格不需要修改：" + p);
    } else
    { String txid = "";
      param = "\"" + add + "\"," + price + "";
      System.out.println("开始修改价格=======");
      System.out.println(param + "-----");
      txid = PublicMethed.triggerContract(priceAddress,
          "setPrice(address,uint256)", param, false,
          0, maxFeeLimit, ownerAddress, ownerKey, blockingStubFull);
      Optional<TransactionInfo> infoById = null;
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
      Assert.assertTrue(infoById.get().getResultValue() == 0);
      Assert.assertEquals(0,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));
      System.out.println("结束修改价格=======");
      param = "\"" + add + "\"";
      System.out.println(param + ".........");
      transactionExtention = PublicMethed
          .triggerConstantContractForExtention(priceAddress, "assetPrices(address)", param, false, 0,
              0,
              "0", 0, testAccountAddress, testAccountKey, blockingStubFull);
      byte[] result1 = transactionExtention.getConstantResult(0).toByteArray();
      BigInteger p1 = new BigInteger(Hex.toHexString(result1),16);
      System.out.println("修改后价格：" + p1);
    }*/
  }

  public void supportMaket(String address) {
    String txid = "";
    String param = "\"" + address + "\"";
    txid = PublicMethed.triggerContract(unitrAddress,
        "_supportMarket(address)", param, false,
        0, maxFeeLimit, ownerAddress, ownerKey, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(0,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));
  }

  public void setCollateralFactor(String address) {
    String txid = "";
    String param = "\"" + address + "\",750000000000000000";
    txid = PublicMethed.triggerContract(unitrAddress,
        "_setCollateralFactor(address,uint256)", param, false,
        0, maxFeeLimit, ownerAddress, ownerKey, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(0,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));
  }

  @AfterClass
  public void shutdown() throws InterruptedException {
    Protocol.Block currentBlock = blockingStubFull
        .getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    final Long afterBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    System.out.println("afterBlockNum: "+ afterBlockNum);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelFull1 != null) {
      channelFull1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelSolidity != null) {
      channelSolidity.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}