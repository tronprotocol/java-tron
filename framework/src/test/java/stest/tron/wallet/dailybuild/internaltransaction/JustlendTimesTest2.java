package stest.tron.wallet.dailybuild.internaltransaction;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
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
public class JustlendTimesTest2 {

  private final String testAccountKey = "6815B367FDDE637E53E9ADC8E69424E07724333C9A2B973CFA469975E20753FC";
  private final byte[] testAccountAddress = PublicMethed.getFinalAddress(testAccountKey);
  private final String testAccountKey2 = "6815B367FDDE637E53E9ADC8E69424E07724333C9A2B973CFA469975E20753FC";
  private final byte[] testAccountAddress2 = PublicMethed.getFinalAddress(testAccountKey2);
  private final String testAccountKey3 = "FC8BF0238748587B9617EB6D15D47A66C0E07C1A1959033CF249C6532DC29FE6";
  private final byte[] testAccountAddress3 = PublicMethed.getFinalAddress(testAccountKey3);
  private final String testAccountKey4 = "324a2052e491e99026442d81df4d2777292840c1b3949e20696c49096c6bacb8";
  private final byte[] testAccountAddress4 = PublicMethed.getFinalAddress(testAccountKey4);
  private final String testAccountKey5 = "2925e186bb1e88988855f11ebf20ea3a6e19ed92328b0ffb576122e769d45b68";
  private final byte[] testAccountAddress5 = PublicMethed.getFinalAddress(testAccountKey5);
  private final String accountKey = "7d5a7396d6430edb7f66aa5736ef388f2bea862c9259de8ad8c2cfe080f6f5a0";
  private final byte[] accountAddress = PublicMethed.getFinalAddress(accountKey);

  private final String ownerKey = "1ec9c30c9c246572557d8aaf88fd0823b70fb4b5a085be80959d66be0afb2848";
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
      WalletClient.decodeFromBase58Check("TEgfD7cGY7xHuoGuyDBdMicysbbZhpjwx9");
  private static byte[] priceAddress =
      WalletClient.decodeFromBase58Check("TSjNzNRKdQMZtazPD7dkMi1Tt8Zj4ZC3wA");
  private static byte[] trxAddress =
      WalletClient.decodeFromBase58Check("TVEvT4vNh3oAabHjxPhgU11Se9riqatQ9h");
  private static byte[] sunAddress =
      WalletClient.decodeFromBase58Check("TZAe6nHgHRU9BwHax1Do8MAeLQg8ikXzLZ");
  private static byte[] usdtAddress =
      WalletClient.decodeFromBase58Check("TVrvkKyDqrrveLkZ67qDYJi29QYdG8iAJi");
  private static byte[] usdjAddress =
      WalletClient.decodeFromBase58Check("TNP36KeZaAzCCnjgNHm7w3dCFkKDsQ9Wh3");
  private static byte[] winAddress =
      WalletClient.decodeFromBase58Check("TUU67mPrNxJS53AtpySji5LQqB5eZgnxu4");
  private static byte[] btcAddress =
      WalletClient.decodeFromBase58Check("TDAJq9iNagWNScz18xLx6HfyJf8ktp1aru");
  private static byte[] jstAddress =
      WalletClient.decodeFromBase58Check("TMxwHannVrcp1HteiJTW1ZDDAS6iEsPJ3s");
  private static byte[] wbttAddress =
      WalletClient.decodeFromBase58Check("TMDx1DFX62v7wfpByMKRbWX8xZhiU6w5s7");
  private static byte[] ethAddress =
      WalletClient.decodeFromBase58Check("TEsaQVSi84WSASUvUxqN81b6kUGghV5dzH");
  private static byte[] eth2Address =
      WalletClient.decodeFromBase58Check("TL1yMm8YUbW8TkwC3J1Qfy3L6uGQvaDzrY");
  private static byte[] eth3Address =
      WalletClient.decodeFromBase58Check("TQeWYubxpW1h6Kc5L289r26FGUK4hd6xS1");
  private static byte[] eth4Address =
      WalletClient.decodeFromBase58Check("TVT81JPzTXsQCWCAydVPDecn2xFnXGkAV9");
  private static byte[] eth5Address =
      WalletClient.decodeFromBase58Check("TCr81A6pA9qZATBz36GQUBySc74j77HQg8");
  private static byte[] eth6Address =
      WalletClient.decodeFromBase58Check("TFHc1Q3KxYvS8AcWRyeitQxJ393P8qBkfr");
  private static byte[] eth7Address =
      WalletClient.decodeFromBase58Check("TKogFfhggBV9PkrUT9vd5Vjc2x9dQs9B1E");
  private static byte[] eth8Address =
      WalletClient.decodeFromBase58Check("TW33icujtYYLZPBvvtsKhfjkE2qmUMWkGZ");
  private static byte[] eth9Address =
      WalletClient.decodeFromBase58Check("TSdxbcvDiwassY8pyiz8Jphd4tpid4xYZF");
  private static byte[] eth10Address =
      WalletClient.decodeFromBase58Check("TVu1viG4cTBHxhdeekK1fgNnxPbn3Ktctx");
  String usdjTokenAddress= "TJ7KV8qKAyAFkdYu1Xr4mEAmhieNJ24ZmF";
  String jstTokenAddress= "TYrR7VCv1UcWb18ndBkewvhSApf97vnXEf";
  String usdtTokenAddress= "TSyUnhJdZ8NFNBawRzS8JDatwbFKiuFJVU";// 6
  String sunTokenAddress= "TD6z6KmS2EWh1Bg2gwP5cqSaAvchZizHPq";
  String winTokenAddress= "TVQSiwAf3EkGdr5sKAJ1kywaGnRqhuSBeo";// 6
  String btcTokenAddress= "TE8P2NpxGjn4vF4G3ZvAXkeaog3Zu2mnrz";// 8
  String wbttTokenAddress= "TYM21rrHMgD7RrjQzUn6nYn3kbdjEwtBpx";// 6
  String ethTokenAddress= "TMjF58nf7X4h721tFJCjebMiHWpmJhRuUB";
  String eth2TokenAddress= "TEvRu1eq3wTZromoUuViMEnD9nhieVkLrv";
  String eth3TokenAddress= "TCr27WGkiDfn6RJjVLL6upx5gPYC4viH6o";
  String eth4TokenAddress= "TURMZAEYoq5MoBPsSAdKnddEesVwrixdfv";
  String eth5TokenAddress= "TPwz2xMddMSgjJVaetztx38gBevK9von7y";

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

  @Test(enabled = true, description = "newAccount")
  public void newAccount() {
    ArrayList<String> addressList = new ArrayList<>();
    addressList.add(Base58.encode58Check(testAccountAddress));
    addressList.add(Base58.encode58Check(testAccountAddress2));
    addressList.add(Base58.encode58Check(testAccountAddress3));
    addressList.add(Base58.encode58Check(testAccountAddress4));
    addressList.add(Base58.encode58Check(testAccountAddress5));

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
    for (String address : addressList) {
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
    }

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
    /*argStr = "00000000000000000000000000000000000000000004f68ca6d8cd91c6000000";
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
    }*/
  }

  @Test(enabled = true, description = "addNewMarket")
  public void addNewMarket() {
    String address = Base58.encode58Check(eth4Address);
    String name = "eth4";
//    supportMaket(address);

    Map<String, String> map = new HashMap<String, String>();
    map.put(eth4TokenAddress, "379000000");
//    for (String key : map.keySet()) {
//      System.out.println(key + ":" + map.get(key));
//      setprice(key,map.get(key));
//    }
    entermarket(address,name);
//    setCollateralFactor(address);

//    approve();

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
    map.put(usdj, "36140000");
    map.put(jst,"8450000");
    map.put(usdt,"36570000000000000000");
    map.put(sun,"379000000");
    map.put(win,"3587000000000000");
    map.put(btc,"6810278183330000000000");
    map.put(wbtt,"11204000000000000");
    map.put(eth,"379000000");
    map.put(eth2,"379000000");


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



  @Test(enabled = true)
  public void entermarket() {
    entermarketAll();
  }

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
//    addressMap.put("eth3", eth3Address);
//    addressMap.put("eth4", eth4Address);
//    addressMap.put("eth5", eth5Address);
//    addressMap.put("eth6", eth6Address);
//    addressMap.put("eth7", eth7Address);
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
    // trx
    String txid;
    Optional<Protocol.TransactionInfo>  infoById;
    /*String txid = PublicMethed.triggerContract(trxAddress,
        "mint(uint256)", "100000000000000", false,
        100000000000000l, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo>  infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(0,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));
    txid = PublicMethed.triggerContract(trxAddress,
        "borrow(uint256)", "10000000000", false,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(0,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));*/

    // eth6 18
    /*String txid = PublicMethed.triggerContract(eth6Address,
        "mint(uint256)", "00000000000000000000000000000000000000000000152d02c7e14af6800000", true,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(0,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));*/

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
    /*
    String txid = PublicMethed.triggerContract(eth6Address,
        "mint(uint256)", "00000000000000000000000000000000000000000000152d02c7e14af6800000", true,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(0,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));
    txid = PublicMethed.triggerContract(eth6Address,
        "borrow(uint256)", "00000000000000000000000000000000000000000000021e19e0c9bab2400000", true,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(0,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));*/

    // eth5 18
    /*txid = PublicMethed.triggerContract(eth5Address,
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
    Assert.assertEquals(0,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));*/

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
    /*txid = PublicMethed.triggerContract(eth3Address,
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
    Assert.assertEquals(0,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));*/

    // eth2 18
    /*txid = PublicMethed.triggerContract(eth2Address,
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
    Assert.assertEquals(0,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));*/
  }

  @Test(enabled = true)
  public void approve() {
    String usdj="TJ7KV8qKAyAFkdYu1Xr4mEAmhieNJ24ZmF";
    String jst="TYrR7VCv1UcWb18ndBkewvhSApf97vnXEf";
    String usdt="TSyUnhJdZ8NFNBawRzS8JDatwbFKiuFJVU";
    String sun="TD6z6KmS2EWh1Bg2gwP5cqSaAvchZizHPq";
    String win="TVQSiwAf3EkGdr5sKAJ1kywaGnRqhuSBeo";
    String btc="TE8P2NpxGjn4vF4G3ZvAXkeaog3Zu2mnrz";
    String wbtt="TYM21rrHMgD7RrjQzUn6nYn3kbdjEwtBpx";
    String eth="TMjF58nf7X4h721tFJCjebMiHWpmJhRuUB";
    String eth2="TEvRu1eq3wTZromoUuViMEnD9nhieVkLrv";
    String eth3="TCr27WGkiDfn6RJjVLL6upx5gPYC4viH6o";
    String eth4="TURMZAEYoq5MoBPsSAdKnddEesVwrixdfv";
    String eth5="TPwz2xMddMSgjJVaetztx38gBevK9von7y";
    String eth6="TSoMFARJvevysGUR7ZBUPX5pM6v9CNmuU5";
    String eth7="TQprmUBzGJUVeebqK4UA6iTfbP6rpF9Tob";
    String eth8="TCXL6jT2yUNw85RzEicUBp8yTmd4Jc92ZW";
    String eth9="TJFEw99wkMGVEK6SUjdBFb78nDPbqcJgEL";
    String eth10="THB3BidL5CE6Eu5bs4JwzEneVW1c7vaPx2";

    String txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(usdj),
        "approve(address,uint256)", "\""+Base58.encode58Check(usdjAddress)+"\",-1", false,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(1,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));

    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(jst),
        "approve(address,uint256)", "\""+Base58.encode58Check(jstAddress)+"\",-1", false,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(1,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));

    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(usdt),
        "approve(address,uint256)", "\""+Base58.encode58Check(usdtAddress)+"\",-1", false,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(1,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));

    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(sun),
        "approve(address,uint256)", "\""+Base58.encode58Check(sunAddress)+"\",-1", false,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(1,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));

    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(win),
        "approve(address,uint256)", "\""+Base58.encode58Check(winAddress)+"\",-1", false,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(1,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));

    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(btc),
        "approve(address,uint256)", "\""+Base58.encode58Check(btcAddress)+"\",-1", false,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(1,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));

    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(wbtt),
        "approve(address,uint256)", "\""+Base58.encode58Check(wbttAddress)+"\",-1", false,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(1,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));

    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(eth),
        "approve(address,uint256)", "\""+Base58.encode58Check(ethAddress)+"\",-1", false,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(1,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));

    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(eth2),
        "approve(address,uint256)", "\""+Base58.encode58Check(eth2Address)+"\",-1", false,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(1,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));

    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(eth3),
        "approve(address,uint256)", "\""+Base58.encode58Check(eth3Address)+"\",-1", false,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(1,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));

    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(eth4),
        "approve(address,uint256)", "\""+Base58.encode58Check(eth4Address)+"\",-1", false,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(1,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));

    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(eth5),
        "approve(address,uint256)", "\""+Base58.encode58Check(eth5Address)+"\",-1", false,
        0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(1,ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));
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
      Optional<Protocol.TransactionInfo> infoById = null;
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
    for (String ctoken:addressMap.keySet()) {
      System.out.println(ctoken + ":" + addressMap.get(ctoken));
      String param = "\"" + Base58.encode58Check(addressMap.get(ctoken)) + "\"";
      String txid = PublicMethed.triggerContract(unitrAddress,
          "enterMarket(address)", param, false,
          0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
      Optional<Protocol.TransactionInfo> infoById = null;
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
      Assert.assertTrue(infoById.get().getResultValue() == 0);
    }
  }

  public void setprice(String add, String price) {
    String param = "\"" + add + "\"";
    TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(priceAddress, "assetPrices(address)", param, false, 0,
            0,
            "0", 0, testAccountAddress, testAccountKey, blockingStubFull);
    byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    BigInteger p = new BigInteger(Hex.toHexString(result),16);
    System.out.println("第一次查询价格：" + p);
    BigInteger bigprice=new BigInteger(price);
    if (bigprice.equals(p)) {
      System.out.println("价格不需要修改：" + p);
    } else
    { String txid = "";
      param = "\"" + add + "\"," + price + "";
      System.out.println("开始修改价格=======");
      System.out.println(param + "-----");
      txid = PublicMethed.triggerContract(priceAddress,
          "setPrice(address,uint256)", param, false,
          0, maxFeeLimit, ownerAddress, ownerKey, blockingStubFull);
      Optional<Protocol.TransactionInfo> infoById = null;
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
    }
  }

  public void supportMaket(String address) {
    String txid = "";
    String param = "\"" + address + "\"";
    txid = PublicMethed.triggerContract(unitrAddress,
        "_supportMarket(address)", param, false,
        0, maxFeeLimit, ownerAddress, ownerKey, blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = null;
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
    Optional<Protocol.TransactionInfo> infoById = null;
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