package stest.tron.wallet.onlinestress.tvm;

import static stest.tron.wallet.common.client.utils.PublicMethed.getFinalAddress;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.WalletGrpc;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;

public class Tusd2UsdPool {
  private final String testNetAccountKey = "1ec9c30c9c246572557d8aaf88fd0823b70fb4b5a085be80959d66be0afb2848";
  private final byte[] testNetAccountAddress = getFinalAddress(testNetAccountKey);


  byte[] addr2pContract = WalletClient.decodeFromBase58Check("TUfoYLeHrksNnk5n4wae7jJqSoQ8jfAo8j");
  byte[] TUSD2usdSWAP = WalletClient.decodeFromBase58Check("TU23ZCpt5zvVrsQg6HmEBfhLDcaFNh61rt");
  byte[] TUSDDepositer = WalletClient.decodeFromBase58Check("TTVGxpV2jsBwt348VgmHfGFbjKL6cWe16A");
  byte[] addr2pLp = WalletClient.decodeFromBase58Check("TTJ6NuhhrCTHUDgcAWzbZHjzFjsGswQVye");
  byte[] tusd2USDLp = WalletClient.decodeFromBase58Check("TV8XFRaJATdpMcRywuuAG6JeNQSJ56Q2hf");
  byte[] TUSD = WalletClient.decodeFromBase58Check("TRz7J6dD2QWxBoumfYt4b3FaiRG23pXfop");
  byte[] USDD = WalletClient.decodeFromBase58Check("TGjgvdTWWrybVLaVeFqSyVqJQWjxqRYbaK");
  byte[] USDT = WalletClient.decodeFromBase58Check("TXYZopYRdj2D9XRtbG411XZZ3kM5VkAeBf");
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);

  @BeforeSuite
  public void beforeSuite() throws IOException {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  @BeforeClass(enabled = true)
  public void beforeClass() throws IOException {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }

  @Test(enabled = true, description = "approve")
  public void approve() {
    List<byte[]> tokenlist = new ArrayList<byte[]>();
    tokenlist.add(addr2pLp);
    tokenlist.add(tusd2USDLp);
    tokenlist.add(TUSD);
    tokenlist.add(USDD);
    tokenlist.add(USDT);
    List<byte[]> contractlist = new ArrayList<byte[]>();
    contractlist.add(addr2pContract);
    contractlist.add(TUSD2usdSWAP);
    contractlist.add(TUSDDepositer);
    for (int i = 0;i < tokenlist.size();i++) {
      for (int j = 0;j < contractlist.size();j++) {
        String data = "\"" + Base58.encode58Check(contractlist.get(j)) + "\",\"-1\"";
        String txid = PublicMethed
            .triggerContract(tokenlist.get(i), "approve(address,uint256)", data, false,
                0, maxFeeLimit, testNetAccountAddress, testNetAccountKey, blockingStubFull);
        PublicMethed.waitProduceNextBlock(blockingStubFull);
        Optional<TransactionInfo> infoById = PublicMethed
            .getTransactionInfoById(txid, blockingStubFull);
        Assert.assertTrue(infoById.get().getResultValue() == 0);
      }
    }
  }

  @Test(enabled = true, description = "addQualidity")
  public void addQualidity(){
    String sres1 = PublicMethed.addZeroForNum(new BigInteger("1000000000000000000").toString(16),64);
    String sres2 = PublicMethed.addZeroForNum(new BigInteger("1000000000000000000").toString(16),64);
    String sres3 = PublicMethed.addZeroForNum(new BigInteger("1000000").toString(16),64);
    String sres4 = PublicMethed.addZeroForNum(new BigInteger("1").toString(16),64);
    String param = sres1 + sres2 + sres3 + sres4;
    System.out.println("param: " + param);

    String txid = PublicMethed.triggerContract(TUSDDepositer,
        "add_liquidity(uint256[3],uint256)", param, true,
        0, maxFeeLimit, testNetAccountAddress, testNetAccountKey, blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = null;
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);

    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(tusd2USDLp,
            "balanceOf(address)", "\"TDsLZoTfrnN4cSzsqAatYn9PRhYEwR1z6z\"", false,
            0, 0, "0", 0, testNetAccountAddress, testNetAccountKey, blockingStubFull);
    String lpBalance = ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray());
    System.out.println("lpBalance:"+lpBalance);
    Assert.assertTrue(!lpBalance.equals("0000000000000000000000000000000000000000000000000000000000000000"));
    System.out.println("添加流动性成功～");
  }

  @Test(enabled = true, description = "Trigger admin_fees method")
  public void admin_fees() {
    for (int i = 0; i < 2; i++) {
      String param = PublicMethed.addZeroForNum(i+"",64);
      GrpcAPI.TransactionExtention transactionExtention = PublicMethed
          .triggerConstantContractForExtention(TUSD2usdSWAP,
              "admin_balances(uint256)", param, true,
              0, 0, "0", 0, testNetAccountAddress, testNetAccountKey, blockingStubFull);
      int TUSD2usdSWAPvalue =
          ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
      String TUSD2usdSWAPvl = ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray());
      transactionExtention = PublicMethed
          .triggerConstantContractForExtention(addr2pContract,
              "admin_balances(uint256)", param, true,
              0, 0, "0", 0, testNetAccountAddress, testNetAccountKey, blockingStubFull);
      int addr2pContractvalue = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
      String addr2pContractvl = ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray());

      if (TUSD2usdSWAPvalue >= 0 && addr2pContractvalue >= 0) {
        System.out.println("admin_balances TUSD2usdSWAP:" + TUSD2usdSWAPvalue + " addr2pContract:" + addr2pContractvalue);
      } else {
        System.out.println("admin_balances TUSD2usdSWAP:" + TUSD2usdSWAPvl + " addr2pContract:" + addr2pContractvl);
      }
    }
  }

  @Test(enabled = true, description = "Trigger removeliquidity method")
  public void removeliquidity(){
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(tusd2USDLp,
            "balanceOf(address)", "\"TDsLZoTfrnN4cSzsqAatYn9PRhYEwR1z6z\"", false,
            0, 0, "0", 0, testNetAccountAddress, testNetAccountKey, blockingStubFull);
    String lpBalance = ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray());
    System.out.println("lpBalance:"+lpBalance);
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(tusd2USDLp,
            "totalSupply()", "#", false,
            0, 0, "0", 0, testNetAccountAddress, testNetAccountKey, blockingStubFull);
    String totalSupply = ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray());
    System.out.println("totalSupply:"+totalSupply);
    if (!lpBalance.equals(totalSupply)) {
      System.out.println("lpBalance != totalSupply !!!!!!!");
    } else {
      while (!lpBalance.equals("0000000000000000000000000000000000000000000000000000000000000000")){
        String param = lpBalance+PublicMethed.addZeroForNum("0",64)+PublicMethed.addZeroForNum("0",64)+PublicMethed.addZeroForNum("0",64);

        // remove_liquidity(uint256,uint256[2])
        String txid = PublicMethed.triggerContract(TUSDDepositer,
            "remove_liquidity(uint256,uint256[3])", param, true,
            0, maxFeeLimit, testNetAccountAddress, testNetAccountKey, blockingStubFull);
        Optional<Protocol.TransactionInfo> infoById = null;
        PublicMethed.waitProduceNextBlock(blockingStubFull);
        infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
        Assert.assertTrue(infoById.get().getResultValue() == 0);
        transactionExtention = PublicMethed
            .triggerConstantContractForExtention(tusd2USDLp,
                "balanceOf(address)", "\"TDsLZoTfrnN4cSzsqAatYn9PRhYEwR1z6z\"", false,
                0, 0, "0", 0, testNetAccountAddress, testNetAccountKey, blockingStubFull);
        lpBalance = ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray());
        System.out.println("lpBalance:"+lpBalance);
        transactionExtention = PublicMethed
            .triggerConstantContractForExtention(tusd2USDLp,
                "totalSupply()", "#", false,
                0, 0, "0", 0, testNetAccountAddress, testNetAccountKey, blockingStubFull);
        totalSupply = ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray());
        System.out.println("totalSupply:"+totalSupply);
        Assert.assertTrue(lpBalance.equals(totalSupply));
        System.out.println("移除全部流动性成功～");
      };
    }
  }


  @Test(enabled = true, description = "Trigger remove Onecoin liquidity method")
  public void removeOnecoinliquidity(String i){
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(tusd2USDLp,
            "balanceOf(address)", "\"TDsLZoTfrnN4cSzsqAatYn9PRhYEwR1z6z\"", false,
            0, 0, "0", 0, testNetAccountAddress, testNetAccountKey, blockingStubFull);
    String lpBalance = ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray());
    System.out.println("lpBalance:"+lpBalance);

    String param = lpBalance + PublicMethed.addZeroForNum(i,64) + "0000000000000000000000000000000000000000000000000000000000000001";
    System.out.println(param);
    String txid = PublicMethed.triggerContract(TUSDDepositer,
        "remove_liquidity_one_coin(uint256,int128,uint256)", param, true,
        0, maxFeeLimit, testNetAccountAddress, testNetAccountKey, blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = null;
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    System.out.println("removeOnecoinliquidit-"+i+" txid:"+txid);

    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(tusd2USDLp,
            "balanceOf(address)", "\"TDsLZoTfrnN4cSzsqAatYn9PRhYEwR1z6z\"", false,
            0, 0, "0", 0, testNetAccountAddress, testNetAccountKey, blockingStubFull);
    int lpBal = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    System.out.println("lpBal:"+lpBal);
    Assert.assertTrue(lpBal == 0);
  }

  @Test(enabled = true,threadPoolSize = 1,invocationCount = 1 , description = "Trigger remove_liquidity_imbalance method")
  public void testImbalanceRemove(String param) {
    System.out.println(param);
    String txid = PublicMethed.triggerContract(TUSDDepositer,
          "remove_liquidity_imbalance(uint256[3],uint256)", param, true,
          0, maxFeeLimit, testNetAccountAddress, testNetAccountKey, blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = null;
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
  }

  @Test(enabled = true)
  public void test(){
//    addQualidity();
//    removeOnecoinliquidity("0");
//    addQualidity();
//    removeOnecoinliquidity("1");
//    addQualidity();
//    removeOnecoinliquidity("2");
//    addQualidity();
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(tusd2USDLp,
            "balanceOf(address)", "\"TDsLZoTfrnN4cSzsqAatYn9PRhYEwR1z6z\"", false,
            0, 0, "0", 0, testNetAccountAddress, testNetAccountKey, blockingStubFull);
    String lpBalance = ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray());
    String param = PublicMethed.addZeroForNum(new BigInteger("123456789").toString(16),64)
        + PublicMethed.addZeroForNum(new BigInteger("23456789").toString(16),64)
        + PublicMethed.addZeroForNum(new BigInteger("456789").toString(16),64)
        + lpBalance;
    testImbalanceRemove(param);
  }
  /*@Test(enabled = true,threadPoolSize = 1,invocationCount = 1,description = "Trigger remove_liquidity_imbalance method")
  public void testBalanceAndTotalsupplyAndDyAndaminfee() {
    for (int i=0 ; i < 3 ; i++){
      String k = String.valueOf(i);
      GrpcAPI.TransactionExtention transactionExtention = PublicMethed
          .triggerConstantContractForExtention(contractAddress2p,
              "balances(uint256)", k, false,
              0, 0, "0", 0, testNetAccountAddress, testNetAccountKey, blockingStubFull);
      String tpvalue = ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray());
      transactionExtention = PublicMethed
          .triggerConstantContractForExtention(contractAddressvy,
              "balances(uint256)", k, false,
              0, 0, "0", 0, testNetAccountAddress, testNetAccountKey, blockingStubFull);
      String vyvalue = ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray());
      System.out.println("balances:" + k + ":" + tpvalue + "," + vyvalue);
      Assert.assertEquals( tpvalue , vyvalue);
    }

        GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(lpToekn,
            "totalSupply()", "#", false,
            0, 0, "0", 0, testNetAccountAddress, testNetAccountKey, blockingStubFull);
    String tokenvalue = ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray());
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddressCv,
            "totalSupply()", "#", false,
            0, 0, "0", 0, testNetAccountAddress, testNetAccountKey, blockingStubFull);
    String cvvalue = ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray());
    System.out.println("totalSupply:" + tokenvalue + "," + cvvalue);
    Assert.assertEquals( tokenvalue , cvvalue);
    testgetdy();
    admin_fees();
  }*/

  public static String stringto16(String s){
//    s = "100000000000000000000000000";
    String _hex = Long.toHexString(Long.parseLong(s));
    System.out.println("转16进制为：" + _hex);
    return _hex;
  }

  public static String to64String(String s){
    int l = s.length();
    String sres = "";
    for (int i =0 ; i < 64 -l;i++ ){
      sres +="0";
    }
    sres += s;
    return sres;
  }

  public String gethex(String s){
    String  url = "http://127.0.0.1:5000/test?p=" + s;
//    String res = testDemo.testhttpGet(url);
//    String[] r = res.split("\"");
////    System.out.println(r[1].substring(2));
//    return r[1].substring(2);
    return "";
  }
}
