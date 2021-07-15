package stest.tron.wallet.onlinestress.sun20;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class stake001 {

  private static final long now = System.currentTimeMillis();
  private static final long totalSupply = 10000000000L;
  private static String name = "testAssetIssue_" + Long.toString(now);
  private static String abbr = "testAsset_" + Long.toString(now);
  private static String description = "desc_" + Long.toString(now);
  private static String url = "url_" + Long.toString(now);
  private static String assetIssueId = null;
  private final String testKey001 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey001);
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private byte[] contractAddress = null;

  private ECKey ecKey1 = new ECKey(Utils.getRandom());
  private byte[] dev001Address = ecKey1.getAddress();
  private String dev001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private ECKey ecKey2 = new ECKey(Utils.getRandom());
  private byte[] dev002Address = ecKey2.getAddress();
  private String dev002Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  private String USDJAddr = "TLBaRhANQoJFTqre9Nf1mjuwNWjCJeYqUL";
  private String TUSDAddr = "TRz7J6dD2QWxBoumfYt4b3FaiRG23pXfop";
  private String USDTAddr = "TXYZopYRdj2D9XRtbG411XZZ3kM5VkAeBf";
  private String sspSUNAddr = "TDqjTkZ63yHB19w2n7vPm2qAkLHwn9fKKk";
  private String sun_trx_lp = "TRGdvFN6N7eXFH1dZHodi5intH25A9KNFH";
  private String threePool_lp = "TVigzABMbbmXbNAziBGj7GHp5EEvH6zDmP";
  private String win_trx_lp = "TXmTRMpZMh8wUHkigkDkxWHmTatUeDxyE4";
  private String threePoolAddress = "TDsvxs3Cu6vtzTWUhQL5V2mwiTAz2ALWoM";
  // need change
  private String FeeConverter = "TSiu6pm591yNB5fQnkyPx4X2rdkyYj6oNG";
  private String veSunStaker = "TMbTkyg9AhWJjHLxDMtD3hmc6sxNU573HB";
  private String LpTokenStakerAuto = "TMLNTTGa2VG35b9TTSEwR2SmurxFMHi8f5";
  private String veSUN = "TEktYjxfgzhZabA2oeyxcbKwQiGneaK6TH";
  private String Vote = "TRAytDZUVRoff1S7rT96BKU2sAmbVCEUqe";

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
    channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true).build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }

  @Test(enabled = true, description = "get3PoolLPAndApproveLPStaker")
  public void get3PoolLPAndApproveLPStaker() {
    String txid;
    String param;
    Optional<TransactionInfo> infoById;
    TransactionExtention transactionExtention;
    // USDJ approve to 3pool
    param = "\"" +threePoolAddress + "\",-1";
    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(USDJAddr), "approve(address,uint256)", param, false,
        0, maxFeeLimit, fromAddress, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());

    // TUSD approve to 3pool
    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(TUSDAddr), "approve(address,uint256)", param, false,
        0, maxFeeLimit, fromAddress, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());

    // USDT approve to 3pool
    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(USDTAddr), "approve(address,uint256)", param, false,
        0, maxFeeLimit, fromAddress, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());

    // add_liquidity
    param = "[\"1234567890123456789\",\"12345678\",\"1234567\"]";
    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(threePoolAddress), "add_liquidity(uint256[3])", param, false,
        0, maxFeeLimit, fromAddress, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());
    // get 3pool lp count
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(threePool_lp),
            "balanceOf(address)", "\"" + WalletClient.encode58Check(fromAddress) + "\"", false,
            0, 0, "0", 0, fromAddress, testKey001, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    String balancesHex =  ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()).replaceAll("^(0+)", "");
    balancesHex = balancesHex.length()==0?"0":balancesHex;
    Long balances =  Long.parseLong(balancesHex,16);
    System.out.println("3pool lp balances : "+balances);

    param = "\"" + LpTokenStakerAuto + "\",-1";
    // 3ppol lp approve to LpTokenStakerAuto
    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(threePool_lp), "approve(address,uint256)", param, false,
        0, maxFeeLimit, fromAddress, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());

    // sun-trx lp approve to LpTokenStakerAuto
    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(sun_trx_lp), "approve(address,uint256)", param, false,
        0, maxFeeLimit, fromAddress, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());

    // sun-trx lp approve to LpTokenStakerAuto
    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(win_trx_lp), "approve(address,uint256)", param, false,
        0, maxFeeLimit, fromAddress, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());
  }

  @Test(enabled = true, description = "veSUNStaker_stakeWithLock")
  public void veSUNStaker_stakeWithLock() {
    TransactionExtention transactionExtention;
    // lockedBalances
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(veSunStaker),
            "lockedBalances(address)", "\"" + WalletClient.encode58Check(fromAddress) + "\"", false,
            0, 0, "0", 0, fromAddress, testKey001, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    String lockedBalancesBeforeHex =  ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()).substring(128,192).replaceAll("^(0+)", "");
    lockedBalancesBeforeHex = lockedBalancesBeforeHex.length()==0?"0":lockedBalancesBeforeHex;
    Long lockedBalancesBefore =  Long.parseLong(lockedBalancesBeforeHex,16);
    System.out.println("lockedBalancesBefore: "+lockedBalancesBefore);
    String lockEndTimestampHex =  ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()).substring(256).replaceAll("^(0+)", "");
    lockEndTimestampHex = lockEndTimestampHex.length()==0?"0":lockEndTimestampHex;
    int lockEndTimestamp =  Integer.parseInt(lockEndTimestampHex,16);
    System.out.println("lockEndTimestamp: "+lockEndTimestamp);
    // lockedSupply
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(veSunStaker),
            "lockedSupply()", "", false,
            0, 0, "0", 0, fromAddress, testKey001, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    Long lockedSupplyBefore =  ByteArray.toLong(transactionExtention.getConstantResult(0).toByteArray());
    System.out.println("lockedSupplyBefore: "+lockedSupplyBefore);
    // totalBalance
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(veSunStaker),
            "totalBalance(address)", "\"" + WalletClient.encode58Check(fromAddress) + "\"", false,
            0, 0, "0", 0, fromAddress, testKey001, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    Long totalBalanceBefore =  ByteArray.toLong(transactionExtention.getConstantResult(0).toByteArray());
    System.out.println("totalBalanceBefore: "+totalBalanceBefore);
    // totalSupply
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(veSunStaker),
            "totalSupply()", "", false,
            0, 0, "0", 0, fromAddress, testKey001, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    Long totalSupplyBefore =  ByteArray.toLong(transactionExtention.getConstantResult(0).toByteArray());
    System.out.println("totalSupplyBefore: "+totalSupplyBefore);
    // unlockedBalance
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(veSunStaker),
            "unlockedBalance(address)", "\"" + WalletClient.encode58Check(fromAddress) + "\"", false,
            0, 0, "0", 0, fromAddress, testKey001, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    Long unlockedBalanceBefore =  ByteArray.toLong(transactionExtention.getConstantResult(0).toByteArray());
    System.out.println("unlockedBalanceBefore: "+unlockedBalanceBefore);

    int currentTimestamp = getSecondTimestampTwo(0);
    System.out.println("currentTimestamp: "+currentTimestamp);
    System.out.println("is end: "+(currentTimestamp>lockEndTimestamp));

    String txid;
    Optional<TransactionInfo> infoById;
    boolean needStake = true;
    if (needStake) {
      /*// get allowance
      transactionExtention = PublicMethed
          .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(sspSUNAddr),
              "allowance(address,address)", "\"" + WalletClient.encode58Check(fromAddress) + "\",\"" + veSunStaker + "\"", false,
              0, 0, "0", 0, fromAddress, testKey001, blockingStubFull);
      Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
      String allowanceHex =  ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()).replaceAll("^(0+)", "");
      allowanceHex = allowanceHex.length()==0?"0":allowanceHex;
      Long allowance =  Long.parseLong(allowanceHex,16);
      if (allowance == 0) {*/
        // approve
        txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(sspSUNAddr), "approve(address,uint256)", "\"" +veSunStaker + "\",-1", false,
            0, maxFeeLimit, fromAddress, testKey001, blockingStubFull);
        PublicMethed.waitProduceNextBlock(blockingStubFull);
        infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
        Assert.assertEquals(0, infoById.get().getResultValue());
//      }

      if (lockedBalancesBefore == 0) {
        if (lockEndTimestamp !=0 && currentTimestamp >= lockEndTimestamp) {
          // withdrawExpiredLocks
          txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(veSunStaker), "withdrawExpiredLocks()", "", false,
              0, maxFeeLimit, fromAddress, testKey001, blockingStubFull);
          PublicMethed.waitProduceNextBlock(blockingStubFull);
          infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
          Assert.assertEquals(0, infoById.get().getResultValue());
        }
        // stakeWithLock
        txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(veSunStaker), "stake(uint256,bool,uint256)", "123456781234567890,true,"+getSecondTimestampTwo(28239), false,
            0, maxFeeLimit, fromAddress, testKey001, blockingStubFull);
        PublicMethed.waitProduceNextBlock(blockingStubFull);
        infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
        Assert.assertEquals(0, infoById.get().getResultValue());

      } else if (lockedBalancesBefore > 0) {
        // increaseLock
        int lockTime = getSecondTimestampTwo(28239);
        txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(veSunStaker), "increaseLock(uint256,uint256)", "123456781234567890,"+lockTime, false,
            0, maxFeeLimit, fromAddress, testKey001, blockingStubFull);
        PublicMethed.waitProduceNextBlock(blockingStubFull);
        infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
        Assert.assertEquals(0, infoById.get().getResultValue());
      }

      if (lockedBalancesBefore == 0 && lockEndTimestamp == 0) {
        transactionExtention = PublicMethed
            .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(veSunStaker),
                "lockedBalances(address)", "\"" + WalletClient.encode58Check(fromAddress) + "\"", false,
                0, 0, "0", 0, fromAddress, testKey001, blockingStubFull);
        Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
        String lockedBalancesAfterHex =  ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()).substring(128,192).replaceAll("^(0+)", "");
        lockedBalancesAfterHex = lockedBalancesAfterHex.length()==0?"0":lockedBalancesAfterHex;
        Long lockedBalancesAfter =  Long.parseLong(lockedBalancesAfterHex,16);
        System.out.println("lockedBalancesBefore: "+lockedBalancesBefore+",lockedBalancesAfter: "+lockedBalancesAfter);
        String lockEndTimestampAfterHex =  ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()).substring(256).replaceAll("^(0+)", "");
        lockEndTimestampAfterHex = lockEndTimestampAfterHex.length()==0?"0":lockEndTimestampAfterHex;
        int lockEndTimestampAfter =  Integer.parseInt(lockEndTimestampAfterHex,16);
        System.out.println("lockEndTimestampAfter: "+lockEndTimestampAfter);
        Assert
            .assertEquals(lockedBalancesBefore+123456781234567890l, lockedBalancesAfter.longValue());
        transactionExtention = PublicMethed
            .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(veSunStaker),
                "lockedSupply()", "", false,
                0, 0, "0", 0, fromAddress, testKey001, blockingStubFull);
        Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
        Long lockedSupplyAfter =  ByteArray.toLong(transactionExtention.getConstantResult(0).toByteArray());
        System.out.println("lockedSupplyBefore: "+lockedSupplyBefore+",lockedSupplyAfter: "+lockedSupplyAfter);
        Assert
            .assertEquals(lockedSupplyBefore+123456781234567890l, lockedSupplyAfter.longValue());
        // totalBalance
        transactionExtention = PublicMethed
            .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(veSunStaker),
                "totalBalance(address)", "\"" + WalletClient.encode58Check(fromAddress) + "\"", false,
                0, 0, "0", 0, fromAddress, testKey001, blockingStubFull);
        Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
        Long totalBalanceAfter =  ByteArray.toLong(transactionExtention.getConstantResult(0).toByteArray());
        System.out.println("totalBalanceBefore: "+totalBalanceBefore+",totalBalanceAfter: "+totalBalanceAfter);
        Assert
            .assertEquals(totalBalanceBefore+123456781234567890l, totalBalanceAfter.longValue());
        // totalSupply
        transactionExtention = PublicMethed
            .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(veSunStaker),
                "totalSupply()", "", false,
                0, 0, "0", 0, fromAddress, testKey001, blockingStubFull);
        Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
        Long totalSupplyAfter =  ByteArray.toLong(transactionExtention.getConstantResult(0).toByteArray());
        System.out.println("totalSupplyBefore: "+totalSupplyBefore+",totalSupplyAfter: "+totalSupplyAfter);
        Assert
            .assertEquals(totalSupplyBefore+123456781234567890l, totalSupplyAfter.longValue());
        // unlockedBalance
        transactionExtention = PublicMethed
            .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(veSunStaker),
                "unlockedBalance(address)", "\"" + WalletClient.encode58Check(fromAddress) + "\"", false,
                0, 0, "0", 0, fromAddress, testKey001, blockingStubFull);
        Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
        Long unlockedBalanceAfter =  ByteArray.toLong(transactionExtention.getConstantResult(0).toByteArray());
        System.out.println("unlockedBalanceBefore: "+unlockedBalanceBefore+",unlockedBalanceAfter: "+unlockedBalanceAfter);
        Assert
            .assertEquals(unlockedBalanceBefore.longValue(), unlockedBalanceAfter.longValue());
      } else {
        transactionExtention = PublicMethed
            .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(veSunStaker),
                "lockedBalances(address)", "\"" + WalletClient.encode58Check(fromAddress) + "\"", false,
                0, 0, "0", 0, fromAddress, testKey001, blockingStubFull);
        Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
        String lockedBalancesAfterHex =  ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()).substring(128,192).replaceAll("^(0+)", "");
        lockedBalancesAfterHex = lockedBalancesAfterHex.length()==0?"0":lockedBalancesAfterHex;
        Long lockedBalancesAfter =  Long.parseLong(lockedBalancesAfterHex,16);
        System.out.println("lockedBalancesBefore: "+lockedBalancesBefore+",lockedBalancesAfter: "+lockedBalancesAfter);
        String lockEndTimestampAfterHex =  ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()).substring(256).replaceAll("^(0+)", "");
        lockEndTimestampAfterHex = lockEndTimestampAfterHex.length()==0?"0":lockEndTimestampAfterHex;
        int lockEndTimestampAfter =  Integer.parseInt(lockEndTimestampAfterHex,16);
        System.out.println("lockEndTimestampAfter: "+lockEndTimestampAfter);
        Assert
            .assertEquals(lockedBalancesBefore.longValue(), lockedBalancesAfter.longValue());
        transactionExtention = PublicMethed
            .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(veSunStaker),
                "lockedSupply()", "", false,
                0, 0, "0", 0, fromAddress, testKey001, blockingStubFull);
        Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
        Long lockedSupplyAfter =  ByteArray.toLong(transactionExtention.getConstantResult(0).toByteArray());
        System.out.println("lockedSupplyBefore: "+lockedSupplyBefore+",lockedSupplyAfter: "+lockedSupplyAfter);
        Assert
            .assertEquals(lockedSupplyBefore.longValue(), lockedSupplyAfter.longValue());
        // totalBalance
        transactionExtention = PublicMethed
            .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(veSunStaker),
                "totalBalance(address)", "\"" + WalletClient.encode58Check(fromAddress) + "\"", false,
                0, 0, "0", 0, fromAddress, testKey001, blockingStubFull);
        Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
        Long totalBalanceAfter =  ByteArray.toLong(transactionExtention.getConstantResult(0).toByteArray());
        System.out.println("totalBalanceBefore: "+totalBalanceBefore+",totalBalanceAfter: "+totalBalanceAfter);
        Assert
            .assertEquals(totalBalanceBefore.longValue(), totalBalanceAfter.longValue());
        // totalSupply
        transactionExtention = PublicMethed
            .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(veSunStaker),
                "totalSupply()", "", false,
                0, 0, "0", 0, fromAddress, testKey001, blockingStubFull);
        Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
        Long totalSupplyAfter =  ByteArray.toLong(transactionExtention.getConstantResult(0).toByteArray());
        System.out.println("totalSupplyBefore: "+totalSupplyBefore+",totalSupplyAfter: "+totalSupplyAfter);
        Assert
            .assertEquals(totalSupplyBefore.longValue(), totalSupplyAfter.longValue());
        // unlockedBalance
        transactionExtention = PublicMethed
            .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(veSunStaker),
                "unlockedBalance(address)", "\"" + WalletClient.encode58Check(fromAddress) + "\"", false,
                0, 0, "0", 0, fromAddress, testKey001, blockingStubFull);
        Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
        Long unlockedBalanceAfter =  ByteArray.toLong(transactionExtention.getConstantResult(0).toByteArray());
        System.out.println("unlockedBalanceBefore: "+unlockedBalanceBefore+",unlockedBalanceAfter: "+unlockedBalanceAfter);
        Assert
            .assertEquals(unlockedBalanceBefore.longValue(), unlockedBalanceAfter.longValue());
      }
    }
    // veSUN.balanceOf
    String param = PublicMethed.addZeroForNum(ByteArray.toHexString(fromAddress).substring(2), 64);
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(veSUN),
            "balanceOf(bytes32)", param, true,
            0, 0, "0", 0, fromAddress, testKey001, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    String balanceOfHex =  ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()).replaceAll("^(0+)", "");
    balanceOfHex = balanceOfHex.length()==0?"0":balanceOfHex;
    Long balanceOf =  Long.parseLong(balanceOfHex,16);
    System.out.println("veSUN.balanceOf: "+balanceOf.longValue());
  }

  @Test(enabled = true, description = "vote_vote_for_gauge_weights")
  public void vote_vote_for_gauge_weights() {
    TransactionExtention transactionExtention;
    String txid;
    Optional<TransactionInfo> infoById;
    // vote_for_gauge_weights
    String param = PublicMethed.addZeroForNum(ByteArray.toHexString(WalletClient.decodeFromBase58Check(threePool_lp)).substring(2), 64)+PublicMethed.addZeroForNum(Integer.toHexString(3600),64);
    System.out.println("param: "+param);
    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(Vote), "vote_for_gauge_weights(bytes32,uint256)", param, true,
        0, maxFeeLimit, fromAddress, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());

    // gauge_relative_weight
    param = PublicMethed.addZeroForNum(ByteArray.toHexString(WalletClient.decodeFromBase58Check(threePool_lp)).substring(2), 64)+PublicMethed.addZeroForNum(Integer.toHexString(getNextHalfAHourTimestamp().intValue()),64);
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(Vote),
            "gauge_relative_weight(bytes32,uint256)", param, true,
            0, 0, "0", 0, fromAddress, testKey001, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    System.out.println("gauge_relative_weight: "+ByteArray.toLong(transactionExtention.getConstantResult(0).toByteArray()));

    // vote_for_gauge_weights
    param = PublicMethed.addZeroForNum(ByteArray.toHexString(WalletClient.decodeFromBase58Check(sun_trx_lp)).substring(2), 64)+PublicMethed.addZeroForNum(Integer.toHexString(1600),64);
    System.out.println("param: "+param);
    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(Vote), "vote_for_gauge_weights(bytes32,uint256)", param, true,
        0, maxFeeLimit, fromAddress, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());

    // gauge_relative_weight
    param = PublicMethed.addZeroForNum(ByteArray.toHexString(WalletClient.decodeFromBase58Check(sun_trx_lp)).substring(2), 64)+PublicMethed.addZeroForNum(Integer.toHexString(getNextHalfAHourTimestamp().intValue()),64);
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(Vote),
            "gauge_relative_weight(bytes32)", param, true,
            0, 0, "0", 0, fromAddress, testKey001, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    System.out.println("gauge_relative_weight: "+ByteArray.toLong(transactionExtention.getConstantResult(0).toByteArray()));

    // vote_for_gauge_weights
    param = PublicMethed.addZeroForNum(ByteArray.toHexString(WalletClient.decodeFromBase58Check(win_trx_lp)).substring(2), 64)+PublicMethed.addZeroForNum(Integer.toHexString(0),64);
    System.out.println("param: "+param);
    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(Vote), "vote_for_gauge_weights(bytes32,uint256)", param, true,
        0, maxFeeLimit, fromAddress, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());

    // gauge_relative_weight
    param = PublicMethed.addZeroForNum(ByteArray.toHexString(WalletClient.decodeFromBase58Check(win_trx_lp)).substring(2), 64)+PublicMethed.addZeroForNum(Integer.toHexString(getNextHalfAHourTimestamp().intValue()),64);
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(Vote),
            "gauge_relative_weight(bytes32)", param, true,
            0, 0, "0", 0, fromAddress, testKey001, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    System.out.println("gauge_relative_weight: "+ByteArray.toLong(transactionExtention.getConstantResult(0).toByteArray()));
  }

  @Test(enabled = true, description = "getEndLockTime")
  public void getEndLockTime() {
    TransactionExtention transactionExtention;
    String txid;
    Optional<TransactionInfo> infoById;
    // vote_for_gauge_weights
    String param = PublicMethed.addZeroForNum(
        ByteArray.toHexString(WalletClient.decodeFromBase58Check(threePool_lp)).substring(2), 64)
        + PublicMethed.addZeroForNum(Integer.toHexString(3000), 64);
    System.out.println("param: " + param);
    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(Vote),
        "vote_for_gauge_weights(bytes32,uint256)", param, true,
        0, maxFeeLimit, fromAddress, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());

    // gauge_relative_weight
    param = PublicMethed.addZeroForNum(
        ByteArray.toHexString(WalletClient.decodeFromBase58Check(threePool_lp)).substring(2), 64)
        + PublicMethed
        .addZeroForNum(Integer.toHexString(getNextHalfAHourTimestamp().intValue()), 64);
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(Vote),
            "gauge_relative_weight(bytes32,uint256)", param, true,
            0, 0, "0", 0, fromAddress, testKey001, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    System.out.println("gauge_relative_weight: " + ByteArray
        .toLong(transactionExtention.getConstantResult(0).toByteArray()));
  }

  @Test(enabled = true, description = "vote_gauge_relative_weight_write")
  public void vote_gauge_relative_weight_write() {
    String txid;
    Optional<TransactionInfo> infoById;
    // gauge_relative_weight_write
    String param = PublicMethed.addZeroForNum(ByteArray.toHexString(WalletClient.decodeFromBase58Check(threePool_lp)).substring(2), 64);
    System.out.println("param: "+param);
    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(Vote), "gauge_relative_weight_write(bytes32)", param, true,
        0, maxFeeLimit, fromAddress, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());

    param = PublicMethed.addZeroForNum(ByteArray.toHexString(WalletClient.decodeFromBase58Check(sun_trx_lp)).substring(2), 64);
    System.out.println("param: "+param);
    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(Vote), "gauge_relative_weight_write(bytes32)", param, true,
        0, maxFeeLimit, fromAddress, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());
  }

  @Test(enabled = true, description = " veSUNStaker notify")
  public void veSUNStakerNotify() {
    String txid;
    Optional<TransactionInfo> infoById;
    TransactionExtention transactionExtention;
    // notify TUSD 10
    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(TUSDAddr), "transfer(address,uint256)", "\"" + FeeConverter + "\",1000000000000000000", false,
        0, maxFeeLimit, fromAddress, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());

    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(FeeConverter), "notify(address)", "\"" + TUSDAddr + "\"", false,
        0, maxFeeLimit, fromAddress, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());
    // TUSDRewardData
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(veSunStaker),
            "rewardData(address)", "\"" + TUSDAddr + "\"", false,
            0, 0, "0", 0, fromAddress, testKey001, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    String periodFinishTUSDHex = ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()).substring(0,64).replaceAll("^(0+)", "");
    periodFinishTUSDHex = periodFinishTUSDHex.length()==0?"0":periodFinishTUSDHex;
    Long periodFinishTUSD = Long.parseLong(periodFinishTUSDHex,16);
    String rewardRateTUSDHex = ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()).substring(64,128).replaceAll("^(0+)", "");
    rewardRateTUSDHex = rewardRateTUSDHex.length()==0?"0":rewardRateTUSDHex;
    Long rewardRateTUSD = Long.parseLong(rewardRateTUSDHex,16);
    String lastUpdateTimeTUSDHex = ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()).substring(128,192).replaceAll("^(0+)", "");
    lastUpdateTimeTUSDHex = lastUpdateTimeTUSDHex.length()==0?"0":lastUpdateTimeTUSDHex;
    Long lastUpdateTimeTUSD = Long.parseLong(lastUpdateTimeTUSDHex,16);
    String rewardPerTokenStoredTUSDHex = ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()).substring(192).replaceAll("^(0+)", "");
    rewardPerTokenStoredTUSDHex = rewardPerTokenStoredTUSDHex.length()==0?"0":rewardPerTokenStoredTUSDHex;
    Long rewardPerTokenStoredTUSD = Long.parseLong(rewardPerTokenStoredTUSDHex,16);
    System.out.println("\nperiodFinishTUSD: "+periodFinishTUSD+",rewardRateTUSD: "+rewardRateTUSD+",lastUpdateTimeTUSD: "+lastUpdateTimeTUSD+",rewardPerTokenStoredTUSD: "+rewardPerTokenStoredTUSD);

    /*// addReward SUN
    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(LpTokenStakerAuto), "deposit(uint256,uint256)", "1,123456781234567890", false,
        0, maxFeeLimit, fromAddress, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());
    // notify SUN 10
    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(veSunStaker), "notifyRewardAmount(address,uint256)", "\"" + sspSUNAddr + "\",\"1000000000000000000\"", false,
        0, maxFeeLimit, fromAddress, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());*/
    // SUNRewardData
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(veSunStaker),
            "rewardData(address)", "\"" + sspSUNAddr + "\"", false,
            0, 0, "0", 0, fromAddress, testKey001, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    String periodFinishSUNHex = ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()).substring(0,64).replaceAll("^(0+)", "");
    periodFinishSUNHex = periodFinishSUNHex.length()==0?"0":periodFinishSUNHex;
    Long periodFinishSUN = Long.parseLong(periodFinishSUNHex,16);
    String rewardRateSUNHex = ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()).substring(64,128).replaceAll("^(0+)", "");
    rewardRateSUNHex = rewardRateSUNHex.length()==0?"0":rewardRateSUNHex;
    Long rewardRateSUN = Long.parseLong(rewardRateSUNHex,16);
    String lastUpdateTimeSUNHex = ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()).substring(128,192).replaceAll("^(0+)", "");
    lastUpdateTimeSUNHex = lastUpdateTimeSUNHex.length()==0?"0":lastUpdateTimeSUNHex;
    Long lastUpdateTimeSUN = Long.parseLong(lastUpdateTimeSUNHex,16);
    String rewardPerTokenStoredSUNHex = ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()).substring(192).replaceAll("^(0+)", "");
    rewardPerTokenStoredSUNHex = rewardPerTokenStoredSUNHex.length()==0?"0":rewardPerTokenStoredSUNHex;
    Long rewardPerTokenStoredSUN = Long.parseLong(rewardPerTokenStoredSUNHex,16);
    System.out.println("\nperiodFinishSUN: "+periodFinishSUN+",rewardRateSUN: "+rewardRateSUN+",lastUpdateTimeSUN: "+lastUpdateTimeSUN+",rewardPerTokenStoredSUN: "+rewardPerTokenStoredSUN);
  }

  public static int getSecondTimestampTwo(int addTime){
    String timestamp = String.valueOf(new Date().getTime()/1000);
    return Integer.valueOf(timestamp)+addTime;
  }

  public static Long getNextHalfAHourTimestamp(){
    Long l= System.currentTimeMillis() - System.currentTimeMillis()%1800000 + 1800000;
    System.out.println("nextHalfAHourTimestamp is:" + l);
    System.out.println("date is:" + new Date(l));
    return l;
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
