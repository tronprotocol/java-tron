package stest.tron.wallet.contract.scenario;

import com.alibaba.fastjson.JSONObject;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.junit.Assert;
import org.spongycastle.util.encoders.Hex;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class ContractScenario004 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contract004Address = ecKey1.getAddress();
  String contract004Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private JSONObject responseContent;
  private HttpResponse response;

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }

  @Test(enabled = false)
  public void deployErc20TronTokenWithoutData() {
    Assert.assertTrue(PublicMethed.sendcoin(contract004Address, 200000000L, fromAddress,
        testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethed.freezeBalanceGetEnergy(contract004Address, 100000000L,
        3, 1, contract004Key, blockingStubFull));
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(contract004Address,
        blockingStubFull);
    Long energyLimit = accountResource.getEnergyLimit();
    Long energyUsage = accountResource.getEnergyUsed();

    logger.info("before energy limit is " + Long.toString(energyLimit));
    logger.info("before energy usage is " + Long.toString(energyUsage));

    String filePath = "./src/test/resources/soliditycode//contractScenario004.sol";
    String contractName = "TronToken";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    String txid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            0L, 100, null, contract004Key, contract004Address, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> info = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    System.out.println(info);
    Assert.assertTrue(info.get().getResultValue() == 1);
  }

  @Test(enabled = true)
  public void deployErc20TronTokenWithData() {
    PublicMethed.printAddress(contract004Key);
    Assert.assertTrue(PublicMethed
        .sendcoin(contract004Address, 200000000L, fromAddress, testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethed.freezeBalanceGetEnergy(contract004Address, 100000000L,
        3, 1, contract004Key, blockingStubFull));
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(contract004Address,
        blockingStubFull);
    Long energyLimit = accountResource.getEnergyLimit();
    Long energyUsage = accountResource.getEnergyUsed();

    logger.info("before energy limit is " + Long.toString(energyLimit));
    logger.info("before energy usage is " + Long.toString(energyUsage));

    String filePath = "./src/test/resources/soliditycode//contractScenario004.sol";
    String contractName = "TronToken";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    String constructorStr = "constructor(address)";
    String data = "\"" + Base58.encode58Check(contract004Address) + "\"";
    String txid = PublicMethed
        .deployContractWithConstantParame(contractName, abi, code, constructorStr, data, "",
            maxFeeLimit, 0L, 100, null, contract004Key, contract004Address, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> info = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    System.out.println(info);
    Assert.assertTrue(info.get().getResultValue() == 0);
  }

  /**
   * 1亿：
   * USDT:99048984.471872*1e6 = 99048984471872
   * USDJ:774953.268318*1e18 = 7.74953268318e+23	774953268318000000000000
   * TUSD:185629.162328*1e18 = 1.85629162328e+23	185629162328000000000000
   * ["774953268318000000000000","185629162328000000000000","99048984471872"]
   * 99048984471872
   *
   * 1千万：
   * 10000000/537 = 18621.973929236497
   * 18621.973929236497*532 = 9906890.130353816
   * 18621.973929236497*4 = 74487.89571694599
   * USDT:9904898.4471872*1e6 = 9904898447187.2
   * USDJ:77495.32683179999*1e18 = 7.749532683179999e+22	77495326831799990000000
   * TUSD:18562.9162328*1e18 = 1.8562916232799998e+22	18562916232799998000000
   * 9904898.4471872+77495.32683179999+18562.9162328 = 10000956.690251801
   * ["77495326831799990000000","18562916232799998000000","9904898447187"]
   *
   * USDJaDDR, TUSDAddr, USDTAddr
   * 100个USDT兑换USDJ		2,0,100000000,0
   * 100个USDT兑换TUSD		2,1,100000000,0
   * 100个USDJ兑换TUSD		0,1,100000000000000000000,0
   * 1万个USDT兑换USDJ		2,0,10000000000,0
   * 1万个USDT兑换TUSD		2,1,10000000000,0
   * 1万个USDJ兑换TUSD		0,1,10000000000000000000000,0
   */
  @Test(enabled = true)
  public void testStableswapContract() {
    // 100个USDT兑换USDJ		2,0,100000000,0
//    cz1yi811();
//    cz1yi622();
//    exchange("2","0","5f5e100","0","100个USDT兑换USDJ");
//    // 100个USDJ兑换USDT		0,2,100000000000000000000,0
    System.out.println("----------cz1yi811-------------------");
    cz1yi811();
//    cz1yi622();
    exchange("0","2","56bc75e2d63100000","0","100个USDJ兑换USDT");
//    // 100个USDJ兑换TUSD		0,1,100000000000000000000,0
//    cz1yi811();
//    cz1yi622();
//    exchange("0","1","56bc75e2d63100000","0","100个USDJ兑换TUSD");
//    // 1万个USDT兑换USDJ		2,0,10000000000,0
//    cz1yi811();
//    cz1yi622();
//    exchange("2","0","2540be400","0","1万个USDT兑换USDJ");
//    // 1万个USDJ兑换USDT		0,2,10000000000000000000000,0
    cz1yi811();
//    cz1yi622();
    exchange("0","2","21e19e0c9bab2400000","0","1万个USDJ兑换USDT");
//    // 1万个USDJ兑换TUSD		0,1,10000000000000000000000,0
//    cz1yi811();
//    cz1yi622();
//    exchange("0","1","21e19e0c9bab2400000","0","1万个USDJ兑换TUSD");
//    getEvent();
    System.out.println("----------cz1yi622-------------------");
        cz1yi622();
    exchange("0","2","56bc75e2d63100000","0","100个USDJ兑换USDT");
        cz1yi622();
    exchange("0","2","21e19e0c9bab2400000","0","1万个USDJ兑换USDT");
  }


  public void exchange(String i,String j,String dx,String mindy,String msg){
    String StableSwap3Pool = "TVf65TGmwQmd11bM7kVykJx7x1jbu9oc1N";

    String param = to64String(i)+to64String(j)+to64String(dx)+to64String(mindy);
    String txid = PublicMethed.triggerContract(PublicMethed.decode58Check(StableSwap3Pool),
        "exchange(uint128,uint128,uint256,uint256)", param, true,
        0, maxFeeLimit, fromAddress, testKey002, blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = null;
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    System.out.println(msg);
    System.out.println("exchange-txid:"+txid);
  }

  public void cz1yi811(){
    String StableSwap3Pool = "TVf65TGmwQmd11bM7kVykJx7x1jbu9oc1N";
    String CRVLiquidityToken = "TYUS4V7yQesHNF8HjrG9xvD6VLqMxVxdTV";

    // totalSupply
    TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(PublicMethed.decode58Check(CRVLiquidityToken), "totalSupply()", "#", false, 0, 0,
            "0", 0, fromAddress, testKey002, blockingStubFull);
    byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    String lpAmount = Hex.toHexString(result);
    BigInteger p = new BigInteger(lpAmount,16);
    System.out.println("一亿 totalSupply：" + p);
    if (p.compareTo(BigInteger.ZERO) == 1) {
      // remove_liquidity
      String param = to64String(lpAmount)+"000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000";
      String txid = PublicMethed.triggerContract(PublicMethed.decode58Check(StableSwap3Pool),
          "remove_liquidity(uint256,uint256[3])", param, true,
          0, maxFeeLimit, fromAddress, testKey002, blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      Optional<Protocol.TransactionInfo> infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
      Assert.assertTrue(infoById.get().getResultValue() == 0);
    }
    // add_liquidity
    String param = to64String("84595161401484a000000") + to64String("84595161401484a000000") + to64String("48c273950000") + "0000000000000000000000000000000000000000000000000000000000000001";
    System.out.println("一亿 add_liquidity param:"+param);
    String txid = PublicMethed.triggerContract(PublicMethed.decode58Check(StableSwap3Pool),
        "add_liquidity(uint256[3],uint256)", param, true,
        0, maxFeeLimit, fromAddress, testKey002, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
  }

  public void cz1yi622(){
    String StableSwap3Pool = "TVf65TGmwQmd11bM7kVykJx7x1jbu9oc1N";
    String CRVLiquidityToken = "TYUS4V7yQesHNF8HjrG9xvD6VLqMxVxdTV";

    // totalSupply
    TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(PublicMethed.decode58Check(CRVLiquidityToken), "totalSupply()", "#", false, 0, 0,
            "0", 0, fromAddress, testKey002, blockingStubFull);
    byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    String lpAmount = Hex.toHexString(result);
    BigInteger p = new BigInteger(lpAmount,16);
    System.out.println("一亿 totalSupply：" + p);
    if (p.compareTo(BigInteger.ZERO) == 1) {
      // remove_liquidity
      String param = to64String(lpAmount)+"000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000";
      String txid = PublicMethed.triggerContract(PublicMethed.decode58Check(StableSwap3Pool),
          "remove_liquidity(uint256,uint256[3])", param, true,
          0, maxFeeLimit, fromAddress, testKey002, blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      Optional<Protocol.TransactionInfo> infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
      Assert.assertTrue(infoById.get().getResultValue() == 0);
    }

    // add_liquidity
    String param = to64String("108b2a2c28029094000000") + to64String("108b2a2c28029094000000") + to64String("3691d6afc000") + "0000000000000000000000000000000000000000000000000000000000000001";
    System.out.println("一亿 add_liquidity param:"+param);
    String txid = PublicMethed.triggerContract(PublicMethed.decode58Check(StableSwap3Pool),
        "add_liquidity(uint256[3],uint256)", param, true,
        0, maxFeeLimit, fromAddress, testKey002, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
  }

  public void cz1qianwan(){
    String StableSwap3Pool = "TVf65TGmwQmd11bM7kVykJx7x1jbu9oc1N";
    String CRVLiquidityToken = "TYUS4V7yQesHNF8HjrG9xvD6VLqMxVxdTV";

    // totalSupply
    TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(PublicMethed.decode58Check(CRVLiquidityToken), "totalSupply()", "#", false, 0, 0,
            "0", 0, fromAddress, testKey002, blockingStubFull);
    byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    String lpAmount = Hex.toHexString(result);
    BigInteger p = new BigInteger(lpAmount,16);
    System.out.println("一千万 totalSupply：" + p);
    // remove_liquidity
    String param = to64String(lpAmount)+"000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000";
    String txid = PublicMethed.triggerContract(PublicMethed.decode58Check(StableSwap3Pool),
        "remove_liquidity(uint256,uint256[3])", param, true,
        0, maxFeeLimit, fromAddress, testKey002, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    // add_liquidity
    param = to64String("106907b3adaefc309980") + to64String("3ee4c366b2fb0393b80") + to64String("90229f41753") + "0000000000000000000000000000000000000000000000000000000000000001";
    txid = PublicMethed.triggerContract(PublicMethed.decode58Check(StableSwap3Pool),
        "add_liquidity(uint256[3],uint256)", param, true,
        0, maxFeeLimit, fromAddress, testKey002, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
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
  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


