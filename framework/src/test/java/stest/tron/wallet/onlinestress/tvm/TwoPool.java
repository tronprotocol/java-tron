package stest.tron.wallet.onlinestress.tvm;

import static stest.tron.wallet.common.client.utils.PublicMethed.getFinalAddress;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Random;
import org.junit.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.WalletGrpc;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.protos.Protocol;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.PublicMethed;

public class TwoPool {
  private final String testNetAccountKey = "1ec9c30c9c246572557d8aaf88fd0823b70fb4b5a085be80959d66be0afb2848";
  private final byte[] testNetAccountAddress = getFinalAddress(testNetAccountKey);


  byte[] contractAddress2p = null;
  byte[] contractAddressvy = null;
  byte[] lpToekn = null;
  byte[] contractAddressCv = null ;
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

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() throws IOException {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    // 100/200
//    contractAddress2p = WalletClient.decodeFromBase58Check("TJZ8xMTdzAERHrQaTSdKZXZVcnFykoEi7p");
//    lpToekn = WalletClient.decodeFromBase58Check("TSafTDHbogzfW1CU58Ysgb9MvaETSoNhSf");
//    // 1000-->100  1⃣
    contractAddress2p = WalletClient.decodeFromBase58Check("TS69gRTyaCvH8PDaXN5z6Q9MXGa2WwbAC8");
    lpToekn = WalletClient.decodeFromBase58Check("TCuVk5ziKUvqQgZfDa3YGNUAojUhLtqL9M");
//    // 1000-->100  2⃣
//    contractAddress2p = WalletClient.decodeFromBase58Check("TMVpdft286N1DnLPX3wD7UvSHaZ9Qq8P9Z");
//    lpToekn = WalletClient.decodeFromBase58Check("TEs3cUjTrtrxHsSNu4UgKWU7SfokzFWfKX");
//    // 1000-->100  3⃣
//    contractAddress2p = WalletClient.decodeFromBase58Check("TG7BJwoSacT5A3LnvyGuLWaBXjP4fmwMDK");
//    lpToekn = WalletClient.decodeFromBase58Check("TFaztAk1eLM12m18si7RpnneUYxBCkVGee");
//    // 1000-->100  4⃣
//    contractAddress2p = WalletClient.decodeFromBase58Check("TFirn8etdPhHDcRb8yqTCLEJHVGgaBZDRz");
//    lpToekn = WalletClient.decodeFromBase58Check("TCxBVcpQ7SedqzZFGXFRgidvdPk3C9gyez");
  }

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

  @Test(enabled = true, description = "Trigger getdy method")
  public void testgetdy() {
//  get_dy(uint128,uint128,uint256)
    ArrayList<String> parmlist = new ArrayList<String>();
    parmlist.add("0000000000000000000000000000000000000000000000000000000000000000" +
        "0000000000000000000000000000000000000000000000000000000000000001" +
        "000000000000000000000000000000000000000000000000016345785d8a0000");
    parmlist.add("0000000000000000000000000000000000000000000000000000000000000001" +
        "0000000000000000000000000000000000000000000000000000000000000002" +
        "000000000000000000000000000000000000000000000000016345785d8a0000");
    parmlist.add("0000000000000000000000000000000000000000000000000000000000000002" +
        "0000000000000000000000000000000000000000000000000000000000000001" +
        "000000000000000000000000000000000000000000000000016345785d8a0000");
    for (String s :parmlist) {
      GrpcAPI.TransactionExtention transactionExtention = PublicMethed
          .triggerConstantContractForExtention(contractAddress2p,
              "get_dy(uint128,uint128,uint256)", s, true,
              0, 0, "0", 0, testNetAccountAddress, testNetAccountKey, blockingStubFull);
      String tpvalue =
          ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray());
      transactionExtention = PublicMethed
          .triggerConstantContractForExtention(contractAddressvy,
              "get_dy(int128,int128,uint256)", s, true,
              0, 0, "0", 0, testNetAccountAddress, testNetAccountKey, blockingStubFull);
      String vyvalue =
          ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray());
      System.out.println("get_dy 3p:" + tpvalue + " vy:" + vyvalue);
      Assert.assertEquals(tpvalue, vyvalue);
    }
  }

  @Test(enabled = true, description = "Trigger admin_fees method")
  public void admin_fees() {
//    admin_balances(uint256)
    ArrayList<String> parmlist = new ArrayList<String>();
    parmlist.add("0000000000000000000000000000000000000000000000000000000000000000");
    parmlist.add("0000000000000000000000000000000000000000000000000000000000000001");
    parmlist.add("0000000000000000000000000000000000000000000000000000000000000002");
    for (String s : parmlist) {
      GrpcAPI.TransactionExtention transactionExtention = PublicMethed
          .triggerConstantContractForExtention(contractAddress2p,
              "admin_balances(uint256)", s, true,
              0, 0, "0", 0, testNetAccountAddress, testNetAccountKey, blockingStubFull);
      String tpvalue =
          ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray());
      transactionExtention = PublicMethed
          .triggerConstantContractForExtention(contractAddressvy,
              "admin_balances(uint256)", s, true,
              0, 0, "0", 0, testNetAccountAddress, testNetAccountKey, blockingStubFull);
      String vyvalue =
          ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray());
      System.out.println("admin_balances 3p:" + tpvalue + " vy:" + vyvalue);
      Assert.assertEquals(tpvalue, vyvalue);
    }
  }

  @Test(enabled = true, description = "Trigger exchange method")
  public void exchange(String i,String j, String dx, String dy, String Remark){
    System.out.println("---------------"+Remark+" begin---------------");
    removeliquidity();
    addQualidity100_1();
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress2p,
            "A()", "#", false,
            0, 0, "0", 0, testNetAccountAddress, testNetAccountKey, blockingStubFull);
    int A = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    System.out.println("A 值:"+A);

    String param = to64String(i)+to64String(j)+to64String(dx)+to64String(dy);

    String txid = PublicMethed.triggerContract(contractAddress2p,
        "exchange(uint128,uint128,uint256,uint256)", param, true,
        0, maxFeeLimit, testNetAccountAddress, testNetAccountKey, blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = null;
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    System.out.println("txid:"+txid);
    System.out.println("---------------"+Remark+" end---------------");
  }

  @Test(enabled = true, description = "Trigger removeliquidity method")
  public void removeliquidity(){
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(lpToekn,
            "balanceOf(address)", "\"TDsLZoTfrnN4cSzsqAatYn9PRhYEwR1z6z\"", false,
            0, 0, "0", 0, testNetAccountAddress, testNetAccountKey, blockingStubFull);
    String lpBalance = ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray());
    System.out.println("lpBalance:"+lpBalance);
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(lpToekn,
            "totalSupply()", "#", false,
            0, 0, "0", 0, testNetAccountAddress, testNetAccountKey, blockingStubFull);
    String totalSupply = ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray());
    System.out.println("totalSupply:"+totalSupply);
    if (!lpBalance.equals(totalSupply)) {
      System.out.println("lpBalance != totalSupply !!!!!!!");
    } else {
      while (!lpBalance.equals("0000000000000000000000000000000000000000000000000000000000000000")){
        String param = lpBalance+"00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000";

        // remove_liquidity(uint256,uint256[2])
        String txid = PublicMethed.triggerContract(contractAddress2p,
            "remove_liquidity(uint256,uint256[2])", param, true,
            0, maxFeeLimit, testNetAccountAddress, testNetAccountKey, blockingStubFull);
        Optional<Protocol.TransactionInfo> infoById = null;
        PublicMethed.waitProduceNextBlock(blockingStubFull);
        infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
        Assert.assertTrue(infoById.get().getResultValue() == 0);
        transactionExtention = PublicMethed
            .triggerConstantContractForExtention(lpToekn,
                "balanceOf(address)", "\"TDsLZoTfrnN4cSzsqAatYn9PRhYEwR1z6z\"", false,
                0, 0, "0", 0, testNetAccountAddress, testNetAccountKey, blockingStubFull);
        lpBalance = ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray());
        System.out.println("lpBalance:"+lpBalance);
        transactionExtention = PublicMethed
            .triggerConstantContractForExtention(lpToekn,
                "totalSupply()", "#", false,
                0, 0, "0", 0, testNetAccountAddress, testNetAccountKey, blockingStubFull);
        totalSupply = ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray());
        System.out.println("totalSupply:"+totalSupply);
        Assert.assertTrue(lpBalance.equals(totalSupply));
        System.out.println("移除全部流动性成功～");
      };
    }
  }

  @Test(enabled = true, description = "Trigger addQualidity method")
  public void addQualidity8_1(){
    // 8:1
    String sres1 = "000000000000000000000000000000000000000000930de8a8075bd7ba880000";
    String sres2 = "0000000000000000000000000000000000000000000000000000143603b541c0";
    String param = sres1 + sres2 + "0000000000000000000000000000000000000000000000000000000000000001";

    // add_liquidity(uint256[2],uint256)
    String txid = PublicMethed.triggerContract(contractAddress2p,
        "add_liquidity(uint256[2],uint256)", param, true,
        0, maxFeeLimit, testNetAccountAddress, testNetAccountKey, blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = null;
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);

    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(lpToekn,
            "balanceOf(address)", "\"TDsLZoTfrnN4cSzsqAatYn9PRhYEwR1z6z\"", false,
            0, 0, "0", 0, testNetAccountAddress, testNetAccountKey, blockingStubFull);
    String lpBalance = ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray());
    System.out.println("lpBalance:"+lpBalance);
    Assert.assertTrue(!lpBalance.equals("0000000000000000000000000000000000000000000000000000000000000000"));
    System.out.println("添加8：1流动性成功～");
  }

  @Test(enabled = true, description = "Trigger addQualidity method")
  public void addQualidity3_1(){
    // 3:1
    String sres1 = "0000000000000000000000000000000000000000007c13bc4b2c133c56000000";
    String sres2 = "00000000000000000000000000000000000000000000000000002d79883d2000";
    String param = sres1 + sres2 + "0000000000000000000000000000000000000000000000000000000000000001";

    // add_liquidity(uint256[2],uint256)
    String txid = PublicMethed.triggerContract(contractAddress2p,
        "add_liquidity(uint256[2],uint256)", param, true,
        0, maxFeeLimit, testNetAccountAddress, testNetAccountKey, blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = null;
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);

    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(lpToekn,
            "balanceOf(address)", "\"TDsLZoTfrnN4cSzsqAatYn9PRhYEwR1z6z\"", false,
            0, 0, "0", 0, testNetAccountAddress, testNetAccountKey, blockingStubFull);
    String lpBalance = ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray());
    System.out.println("lpBalance:"+lpBalance);
    Assert.assertTrue(!lpBalance.equals("0000000000000000000000000000000000000000000000000000000000000000"));
    System.out.println("添加3：1流动性成功～");
  }

  @Test(enabled = true, description = "Trigger addQualidity method")
  public void addQualidity1_1(){
    // 1:1
    String sres1 = "00000000000000000000000000000000000000000052b7d2dcc80cd2e4000000";
    String sres2 = "00000000000000000000000000000000000000000000000000005af3107a4000";
    String param = sres1 + sres2 + "0000000000000000000000000000000000000000000000000000000000000001";

    // add_liquidity(uint256[2],uint256)
    String txid = PublicMethed.triggerContract(contractAddress2p,
        "add_liquidity(uint256[2],uint256)", param, true,
        0, maxFeeLimit, testNetAccountAddress, testNetAccountKey, blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = null;
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);

    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(lpToekn,
            "balanceOf(address)", "\"TDsLZoTfrnN4cSzsqAatYn9PRhYEwR1z6z\"", false,
            0, 0, "0", 0, testNetAccountAddress, testNetAccountKey, blockingStubFull);
    String lpBalance = ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray());
    System.out.println("lpBalance:"+lpBalance);
    Assert.assertTrue(!lpBalance.equals("0000000000000000000000000000000000000000000000000000000000000000"));
    System.out.println("添加1：1流动性成功～");
  }

  @Test(enabled = true, description = "Trigger addQualidity method")
  public void addQualidity100_1(){
    // 100:1
    String sres1 = "000000000000000000000000000000000000000000a3cc52ec060d98cbc40000";
    String sres2 = "000000000000000000000000000000000000000000000000000001cd0cff9580";
    String param = sres1 + sres2 + "0000000000000000000000000000000000000000000000000000000000000001";

    // add_liquidity(uint256[2],uint256)
    String txid = PublicMethed.triggerContract(contractAddress2p,
        "add_liquidity(uint256[2],uint256)", param, true,
        0, maxFeeLimit, testNetAccountAddress, testNetAccountKey, blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = null;
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);

    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(lpToekn,
            "balanceOf(address)", "\"TDsLZoTfrnN4cSzsqAatYn9PRhYEwR1z6z\"", false,
            0, 0, "0", 0, testNetAccountAddress, testNetAccountKey, blockingStubFull);
    String lpBalance = ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray());
    System.out.println("lpBalance:"+lpBalance);
    Assert.assertTrue(!lpBalance.equals("0000000000000000000000000000000000000000000000000000000000000000"));
    System.out.println("添加100：1流动性成功～");
  }

  @Test(enabled = true, description = "Trigger remove Onecoin liquidity method")
  public void removeOnecoinliquidity(){
//    remove_liquidity_one_coin(uint256,uint128,uint256)
    String txid = "";
    Random r = new Random();
    int k = r.nextInt(2); // 生成[0,2]区间的整数
    int i = r.nextInt(9)+1;
    String s = String.valueOf(BigInteger.valueOf(i).
        multiply(BigInteger.valueOf(1000000000)).
        multiply(BigInteger.valueOf(1000000000)).
        multiply(BigInteger.valueOf(10000000)));
    System.out.println(s);
    String sres1 = to64String(gethex(s));
    String sres2 = to64String(gethex(String.valueOf(k)));
    String param = sres1 + sres2 + "0000000000000000000000000000000000000000000000000000000000000001";
    System.out.println(param);
    txid = PublicMethed.triggerContract(contractAddress2p,
        "remove_liquidity_one_coin(uint256,uint128,uint256)", param, true,
        0, maxFeeLimit, testNetAccountAddress, testNetAccountKey, blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = null;
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);

    System.out.println(param);
    txid = PublicMethed.triggerContract(contractAddressvy,
        "remove_liquidity_one_coin(uint256,int128,uint256)", param, true,
        0, maxFeeLimit, testNetAccountAddress, testNetAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
  }

  @Test(enabled = true,threadPoolSize = 1,invocationCount = 1 , description = "Trigger remove_liquidity_imbalance method")
  public void testImbalanceRemove() {
    String txid = "";
    Random r = new Random();
    int i = r.nextInt(9)+1;
    int j = r.nextInt(9)+1;
    int m = r.nextInt(9)+1;
    String s1 = String.valueOf(BigInteger.valueOf(i).
        multiply(BigInteger.valueOf(1000000000)).
        multiply(BigInteger.valueOf(1000000000)).
        multiply(BigInteger.valueOf(10000000)));
    String s2 = String.valueOf(BigInteger.valueOf(j).
        multiply(BigInteger.valueOf(1000000000)).
        multiply(BigInteger.valueOf(1000000000)).
        multiply(BigInteger.valueOf(10000000)));
    String s3 = String.valueOf(BigInteger.valueOf(m).
        multiply(BigInteger.valueOf(1000000000)).
        multiply(BigInteger.valueOf(10000)));
    System.out.println(s1 + " " + s2  + " " + s3);
    String sres1 = to64String(gethex(s1));
    String sres2 = to64String(gethex(s2));
    String sres3 = to64String(gethex(s3));
    String param = sres1 + sres2 + sres3 + "000000000000000000000000000000000000314dc6448d9338c15b0a00000000";
    System.out.println(param);
    txid = PublicMethed.triggerContract(contractAddress2p,
          "remove_liquidity_imbalance(uint256[3],uint256)", param, true,
          0, maxFeeLimit, testNetAccountAddress, testNetAccountKey, blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = null;
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    txid = PublicMethed.triggerContract(contractAddressvy,
        "remove_liquidity_imbalance(uint256[3],uint256)", param, true,
        0, maxFeeLimit, testNetAccountAddress, testNetAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
  }

  @Test(enabled = true,threadPoolSize = 1,invocationCount = 1,description = "Trigger remove_liquidity_imbalance method")
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
  }

  @Test(enabled = true)
  public void test(){
    // A to B 1万
    exchange("0","1", "21e19e0c9bab2400000", "1", "A to B 1万");
    // B to A 1万
    exchange("1","0", "2540be400", "1" ,"B to A 1万");
    // A to B 10万
    exchange("0","1", "152d02c7e14af6800000", "1", "A to B 10万");
    // B to A 10万
    exchange("1","0", "174876e800", "1", "B to A 10万");
    // A to B 100万
    exchange("0","1", "d3c21bcecceda1000000", "1" ,"A to B 100万");
    // B to A 100万
    exchange("1","0", "e8d4a51000", "1", "B to A 100万");
    removeliquidity();
  }
}
