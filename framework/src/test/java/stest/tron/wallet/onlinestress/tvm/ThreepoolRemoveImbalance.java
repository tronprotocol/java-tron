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

public class ThreepoolRemoveImbalance {
  private final String testNetAccountKey = "127109F1B8FEC8CFAB6A3D4DCE13BBD748195BFD0B08F12FB2776446CC09A11F";
  private final byte[] testNetAccountAddress = getFinalAddress(testNetAccountKey);


  byte[] contractAddress3p = null;
  byte[] contractAddressvy = null;
  byte[] contractAddressToekn = null;
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
    contractAddress3p = WalletClient.decodeFromBase58Check("TX9QZ5bfXrqR5pKdyae2jwWG8vkQc9zZpV");
    contractAddressvy = WalletClient.decodeFromBase58Check("TRbNhyJCKn9SQNTC8Sw3ExjRPTsp6C3HWZ");
    contractAddressToekn = WalletClient.decodeFromBase58Check("TWvGjKzKeNHVY7wJNU9U4GYxDmGfgs7gba");
    contractAddressCv = WalletClient.decodeFromBase58Check("TBpwoLUrrr4Usiby1LxDXD2mif4hfvV1Xu");

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
          .triggerConstantContractForExtention(contractAddress3p,
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
          .triggerConstantContractForExtention(contractAddress3p,
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

  @Test(enabled = true, description = "Trigger addQualidity method")
  public void addQualidity(){
//    add_liquidity(uint256[3],uint256)
    Random r = new Random();
    int i = r.nextInt(9)+10;
    String s = String.valueOf(BigInteger.valueOf(i).
        multiply(BigInteger.valueOf(1000000000)).
        multiply(BigInteger.valueOf(1000000000)).
        multiply(BigInteger.valueOf(10000000)));
    String txid = "";
    System.out.println(s);
    String sres3 = to64String(gethex("100000000000000"));
    String sres12 = to64String(gethex(s));
    String param = sres12 + sres12 + sres3 + "0000000000000000000000000000000000000000000000000000000000000001";
//    System.out.println(param);

    txid = PublicMethed.triggerContract(contractAddress3p,
        "add_liquidity(uint256[3],uint256)", param, true,
        0, maxFeeLimit, testNetAccountAddress, testNetAccountKey, blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = null;
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    txid = PublicMethed.triggerContract(contractAddressvy,
        "add_liquidity(uint256[3],uint256)", param, true,
        0, maxFeeLimit, testNetAccountAddress, testNetAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
  }

  @Test(enabled = true, description = "Trigger exchange method")
  public void exchange(){
//  exchange(uint128,uint128,uint256,uint256)
    String txid = "";
    ArrayList<String> parmlist = new ArrayList<String>();
    parmlist.add("00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000108b2a2c280290940000000000000000000000000000000000000000000000000000000000000000000001");
    parmlist.add("0000000000000000000000000000000000000000000000000000000000000002000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000005af3107a40000000000000000000000000000000000000000000000000000000000000000001");
    for (String param:parmlist){
      txid = PublicMethed.triggerContract(contractAddress3p,
          "exchange(uint128,uint128,uint256,uint256)", param, true,
          0, maxFeeLimit, testNetAccountAddress, testNetAccountKey, blockingStubFull);
      Optional<Protocol.TransactionInfo> infoById = null;
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
      Assert.assertTrue(infoById.get().getResultValue() == 0);
      txid = PublicMethed.triggerContract(contractAddressvy,
          "exchange(int128,int128,uint256,uint256)", param, true,
          0, maxFeeLimit, testNetAccountAddress, testNetAccountKey, blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
      Assert.assertTrue(infoById.get().getResultValue() == 0);
    }
  }

  @Test(enabled = true, description = "Trigger removeliquidity method")
  public void removeliquidity(){
//    remove_liquidity(uint256,uint256[3])
    String txid = "";
    Random r = new Random();
    int i = r.nextInt(9)+1;
    String s = String.valueOf(BigInteger.valueOf(i).
        multiply(BigInteger.valueOf(1000000000)).
        multiply(BigInteger.valueOf(1000000000)).
        multiply(BigInteger.valueOf(10000000)));
    String param = to64String(gethex(s))+"000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000";
    System.out.println(s + " : "+param);

    txid = PublicMethed.triggerContract(contractAddress3p,
        "remove_liquidity(uint256,uint256[3])", param, true,
        0, maxFeeLimit, testNetAccountAddress, testNetAccountKey, blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = null;
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    txid = PublicMethed.triggerContract(contractAddressvy,
        "remove_liquidity(uint256,uint256[3])", param, true,
        0, maxFeeLimit, testNetAccountAddress, testNetAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
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
    txid = PublicMethed.triggerContract(contractAddress3p,
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
    txid = PublicMethed.triggerContract(contractAddress3p,
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
          .triggerConstantContractForExtention(contractAddress3p,
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
        .triggerConstantContractForExtention(contractAddressToekn,
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

  @Test(enabled = true, invocationCount = 50)
  public void test(){
//    addQualidity();
    removeliquidity();
    testImbalanceRemove();
    testBalanceAndTotalsupplyAndDyAndaminfee();
    exchange();
    addQualidity();
    removeOnecoinliquidity();
    testBalanceAndTotalsupplyAndDyAndaminfee();
  }
}
