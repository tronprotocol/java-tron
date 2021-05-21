package stest.tron.wallet.dailybuild.tvmnewcommand.tvmFreeze;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
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
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Result.contractResult;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.Protocol.TransactionInfo.code;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class FreezeContractTest001 {

  private String testFoundationKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private byte[] testFoundationAddress = PublicMethed.getFinalAddress(testFoundationKey);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] testAddress001 = ecKey1.getAddress();
  String testKey001 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private byte[] contractAddress;


  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] testAddress002 = ecKey2.getAddress();
  String testKey002 = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

  ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] testAddress003 = ecKey3.getAddress();
  String testKey003 = ByteArray.toHexString(ecKey3.getPrivKeyBytes());

  private long freezeEnergyUseage;
  private byte[] create2Address;
  private final long freezeCount = 1000_123456L;


  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    PublicMethed.printAddress(testKey001);
    PublicMethed.printAddress(testKey002);

    PublicMethed.printAddress(testFoundationKey);
  }

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true).build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    Assert.assertTrue(PublicMethed.sendcoin(testAddress001,2000_000000L,
        testFoundationAddress,testFoundationKey,blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(testAddress002,10_000000L,
        testFoundationAddress,testFoundationKey,blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(testAddress003,12000_000000L,
        testFoundationAddress,testFoundationKey,blockingStubFull));

    String filePath = "src/test/resources/soliditycode/freezeContract001.sol";
    String contractName = "TestFreeze";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    contractAddress = PublicMethed
        .deployContract(contractName, abi, code, "", maxFeeLimit, 10000_000000L,
            100, null, testFoundationKey,
            testFoundationAddress, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
  }


  @Test(description = "contract freeze to account")
  void FreezeContractTest001() {

    AccountResourceMessage account002_before = PublicMethed
        .getAccountResource(testAddress002,blockingStubFull);
    Account contractAccount_before = PublicMethed.queryAccount(contractAddress,blockingStubFull);

    // freeze(address payable receiver, uint amount, uint res)
    String methedStr = "freeze(address,uint256,uint256)";
    String argsStr = "\"" + Base58.encode58Check(testAddress002) + "\"," + freezeCount + "," + "1";
    String txid = PublicMethed.triggerContract(contractAddress,methedStr,argsStr,
        false,0,maxFeeLimit,testAddress001,testKey001,blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    AccountResourceMessage account002_after = PublicMethed
        .getAccountResource(testAddress002,blockingStubFull);
    Account contractAccount_after = PublicMethed.queryAccount(contractAddress, blockingStubFull);

    logger.info("account002_before.getEnergyLimit : " + account002_before.getEnergyLimit());
    logger.info("account002_after.getEnergyLimit : " + account002_after.getEnergyLimit());
    Assert.assertTrue(account002_before.getEnergyLimit() < account002_after.getEnergyLimit());
    Assert.assertEquals(contractAccount_before.getAccountResource()
            .getDelegatedFrozenBalanceForEnergy() + freezeCount,
        contractAccount_after.getAccountResource().getDelegatedFrozenBalanceForEnergy());
    Assert.assertEquals(contractAccount_before.getBalance() - freezeCount,
        contractAccount_after.getBalance());

    TransactionInfo info = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    freezeEnergyUseage = info.getReceipt().getEnergyUsageTotal();


  }

  @Test(description = "contract freeze to self")
  void FreezeContractTest002() {
    AccountResourceMessage contractResource_before = PublicMethed
        .getAccountResource(contractAddress,blockingStubFull);
    Account contractAccount_before = PublicMethed.queryAccount(contractAddress,blockingStubFull);

    // freeze(address payable receiver, uint amount, uint res)
    String methedStr = "freeze(address,uint256,uint256)";
    String argsStr = "\"" + Base58.encode58Check(contractAddress) + "\"," + freezeCount + "," + "1";
    String txid = PublicMethed.triggerContract(contractAddress,methedStr,argsStr,
        false,0,maxFeeLimit,testAddress001,testKey001,blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    AccountResourceMessage contractResource_after = PublicMethed
        .getAccountResource(contractAddress,blockingStubFull);
    Account contractAccount_after = PublicMethed.queryAccount(contractAddress, blockingStubFull);

    logger.info("account002_before.getEnergyLimit : " + contractResource_before.getEnergyLimit());
    logger.info("account002_after.getEnergyLimit : " + contractResource_after.getEnergyLimit());
    Assert.assertTrue(
        contractResource_before.getEnergyLimit() < contractResource_after.getEnergyLimit());
    Assert.assertEquals(contractAccount_before.getAccountResource()
            .getFrozenBalanceForEnergy().getFrozenBalance() + freezeCount,
        contractAccount_after.getAccountResource().getFrozenBalanceForEnergy().getFrozenBalance());

  }

  @Test(description = "contract freeze to other contract")
  void FreezeContractTest003() {
    String filePath = "src/test/resources/soliditycode/freezeContract001.sol";
    String contractName = "TestFreeze";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    byte[] newContract = PublicMethed
        .deployContract(contractName, abi, code, "", maxFeeLimit, 100_000000L,
            100, null, testFoundationKey,
            testFoundationAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);


    Account contractAccount_before = PublicMethed.queryAccount(contractAddress,blockingStubFull);

    // freeze(address payable receiver, uint amount, uint res)
    String methedStr = "freeze(address,uint256,uint256)";
    String argsStr = "\"" + Base58.encode58Check(newContract) + "\"," + freezeCount + "," + "1";
    String txid = PublicMethed.triggerContract(contractAddress,methedStr,argsStr,
        false,0,maxFeeLimit,testAddress001,testKey001,blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    TransactionInfo info = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    Assert.assertEquals(TransactionInfo.code.FAILED,info.getResult());

    AccountResourceMessage contractResource_after = PublicMethed
        .getAccountResource(newContract,blockingStubFull);
    Account contractAccount_after = PublicMethed.queryAccount(contractAddress, blockingStubFull);

    logger.info("account002_after.getEnergyLimit : " + contractResource_after.getEnergyLimit());
    Assert.assertEquals(contractResource_after.getEnergyLimit(),0);
    Assert.assertEquals(contractAccount_before.getAccountResource()
            .getDelegatedFrozenBalanceForEnergy(),
        contractAccount_after.getAccountResource().getDelegatedFrozenBalanceForEnergy());
    Assert.assertEquals(contractAccount_before.getBalance(),contractAccount_after.getBalance());

  }

  @Test(description = "contract freeze to unactive account",
      dependsOnMethods = "FreezeContractTest001")
  void FreezeContractTest004() {

    ECKey ecKey = new ECKey(Utils.getRandom());
    byte[] testAddress = ecKey.getAddress();
    String testKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());

    Account contractAccount_before = PublicMethed.queryAccount(contractAddress,blockingStubFull);

    // freeze(address payable receiver, uint amount, uint res)
    String methedStr = "freeze(address,uint256,uint256)";
    String argsStr = "\"" + Base58.encode58Check(testAddress) + "\"," + freezeCount + "," + "1";
    logger.info("argsStr: " + argsStr);

    String txid = PublicMethed.triggerContract(contractAddress,methedStr,argsStr,
        false,0,maxFeeLimit,testAddress001,testKey001,blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    AccountResourceMessage account002_after = PublicMethed
        .getAccountResource(testAddress,blockingStubFull);
    Account contractAccount_after = PublicMethed.queryAccount(contractAddress, blockingStubFull);

    logger.info("account002_after.getEnergyLimit : " + account002_after.getEnergyLimit());
    Assert.assertTrue(account002_after.getEnergyLimit() > 0);
    Assert.assertEquals(contractAccount_before.getAccountResource()
            .getDelegatedFrozenBalanceForEnergy() + freezeCount,
        contractAccount_after.getAccountResource().getDelegatedFrozenBalanceForEnergy());
    Assert.assertEquals(contractAccount_before.getBalance() - freezeCount,
        contractAccount_after.getBalance());

    // check active account status
    Account testAccount = PublicMethed.queryAccount(testAddress,blockingStubFull);
    Assert.assertTrue(testAccount.getCreateTime() > 0);
    Assert.assertNotNull(testAccount.getOwnerPermission());
    Assert.assertNotNull(testAccount.getActivePermissionList());


    TransactionInfo info = PublicMethed.getTransactionInfoById(txid,blockingStubFull).get();
    Assert.assertEquals(freezeEnergyUseage + 25000L, info.getReceipt().getEnergyUsageTotal());


  }

  @Test(description = "contract freeze to pre create2 address, and UnFreeze",
      dependsOnMethods = "FreezeContractTest001")
  void FreezeContractTest005() {
    String create2ArgsStr = "1";
    String create2MethedStr = "deploy(uint256)";
    TransactionExtention exten = PublicMethed.triggerConstantContractForExtention(
        contractAddress, create2MethedStr, create2ArgsStr, false, 0, maxFeeLimit,
        "#", 0, testAddress001, testKey001, blockingStubFull);

    String addressHex =
        "41" + ByteArray.toHexString(exten.getConstantResult(0).toByteArray())
            .substring(24);
    logger.info("address_hex: " + addressHex);
    create2Address = ByteArray.fromHexString(addressHex);
    logger.info("create2Address: " + Base58.encode58Check(create2Address));


    Account contractAccount_before = PublicMethed.queryAccount(contractAddress,blockingStubFull);

    // freeze(address payable receiver, uint amount, uint res)
    String methedStr = "freeze(address,uint256,uint256)";
    String argsStr = "\"" + Base58.encode58Check(create2Address) + "\"," + freezeCount + "," + "1";
    logger.info("argsStr: " + argsStr);
    String txid = PublicMethed.triggerContract(contractAddress,methedStr,argsStr,
        false,0,maxFeeLimit,testAddress001,testKey001,blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    AccountResourceMessage account002_after = PublicMethed
        .getAccountResource(create2Address,blockingStubFull);
    Account contractAccount_after = PublicMethed.queryAccount(contractAddress, blockingStubFull);

    logger.info("account002_after.getEnergyLimit : " + account002_after.getEnergyLimit());
    Assert.assertTrue(account002_after.getEnergyLimit() > 0);
    Assert.assertEquals(contractAccount_before.getAccountResource()
            .getDelegatedFrozenBalanceForEnergy() + freezeCount,
        contractAccount_after.getAccountResource().getDelegatedFrozenBalanceForEnergy());
    Assert.assertEquals(contractAccount_before.getBalance() - freezeCount,
        contractAccount_after.getBalance());

    TransactionInfo info = PublicMethed.getTransactionInfoById(txid,blockingStubFull).get();
    Assert.assertEquals(freezeEnergyUseage + 25000L, info.getReceipt().getEnergyUsageTotal());

    txid = PublicMethed.triggerContract(contractAddress,create2MethedStr,
        create2ArgsStr,false,0,maxFeeLimit,testAddress001,testKey001,blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);


    contractAccount_before = PublicMethed.queryAccount(contractAddress,blockingStubFull);

    // freeze(address payable receiver, uint amount, uint res)
    methedStr = "getExpireTime(address,uint256)";
    argsStr = "\"" + Base58.encode58Check(create2Address) + "\"" + ",1";
    TransactionExtention extention = PublicMethed
        .triggerConstantContractForExtention(contractAddress,methedStr,argsStr,
            false,0,maxFeeLimit,"#",0, testAddress001,testKey001,blockingStubFull);
    Long ExpireTime = ByteArray.toLong(extention.getConstantResult(0).toByteArray());
    logger.info("ExpireTime: " + ExpireTime);
    Assert.assertTrue(ExpireTime > 0);

    methedStr = "unfreeze(address,uint256)";
    txid = PublicMethed.triggerContract(contractAddress,methedStr,argsStr,
        false,0,maxFeeLimit,testAddress001,testKey001,blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    contractAccount_after = PublicMethed.queryAccount(contractAddress, blockingStubFull);

    Assert.assertEquals(contractAccount_before.getAccountResource()
            .getDelegatedFrozenBalanceForEnergy() - freezeCount,
        contractAccount_after.getAccountResource().getDelegatedFrozenBalanceForEnergy());
    Assert.assertEquals(contractAccount_before.getBalance() + freezeCount,
        contractAccount_after.getBalance());

  }

  @Test(description = "Unfreeze when freeze to account",
      dependsOnMethods = "FreezeContractTest001")
  void UnFreezeContractTest001() {

    AccountResourceMessage account002_before = PublicMethed
        .getAccountResource(testAddress002,blockingStubFull);
    Account contractAccount_before = PublicMethed.queryAccount(contractAddress,blockingStubFull);

    // freeze(address payable receiver, uint amount, uint res)
    String methedStr = "getExpireTime(address,uint256)";
    String argsStr = "\"" + Base58.encode58Check(testAddress002) + "\"" + ",1";
    TransactionExtention extention = PublicMethed
        .triggerConstantContractForExtention(contractAddress,methedStr,argsStr,
        false,0,maxFeeLimit,"#",0, testAddress001,testKey001,blockingStubFull);
    Long ExpireTime = ByteArray.toLong(extention.getConstantResult(0).toByteArray());
    logger.info("ExpireTime: " + ExpireTime);
    Assert.assertTrue(ExpireTime > 0);

    methedStr = "unfreeze(address,uint256)";
    String txid = PublicMethed.triggerContract(contractAddress,methedStr,argsStr,
        false,0,maxFeeLimit,testAddress001,testKey001,blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    AccountResourceMessage account002_after = PublicMethed
        .getAccountResource(testAddress002,blockingStubFull);
    Account contractAccount_after = PublicMethed.queryAccount(contractAddress, blockingStubFull);

    logger.info("account002_before.getEnergyLimit : " + account002_before.getEnergyLimit());
    logger.info("account002_after.getEnergyLimit : " + account002_after.getEnergyLimit());

    Assert.assertEquals(contractAccount_before.getAccountResource()
            .getDelegatedFrozenBalanceForEnergy() - freezeCount,
        contractAccount_after.getAccountResource().getDelegatedFrozenBalanceForEnergy());

    Assert.assertTrue(account002_before.getEnergyLimit() > account002_after.getEnergyLimit());

  }

  @Test(description = "Unfreeze when freeze to contract self",
      dependsOnMethods = "FreezeContractTest002")
  void UnFreezeContractTest002() {

    Account contractAccount_before = PublicMethed.queryAccount(contractAddress,blockingStubFull);

    // freeze(address payable receiver, uint amount, uint res)
    String methedStr = "getExpireTime(address,uint256)";
    String argsStr = "\"" + Base58.encode58Check(contractAddress) + "\"" + ",1";
    TransactionExtention extention = PublicMethed
        .triggerConstantContractForExtention(contractAddress,methedStr,argsStr,
            false,0,maxFeeLimit,"#",0, testAddress001,testKey001,blockingStubFull);
    Long ExpireTime = ByteArray.toLong(extention.getConstantResult(0).toByteArray());
    logger.info("ExpireTime: " + ExpireTime);
    Assert.assertTrue(ExpireTime > 0);

    methedStr = "unfreeze(address,uint256)";
    String txid = PublicMethed.triggerContract(contractAddress,methedStr,argsStr,
        false,0,maxFeeLimit,testAddress001,testKey001,blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    AccountResourceMessage account002_after = PublicMethed
        .getAccountResource(testAddress002,blockingStubFull);
    Account contractAccount_after = PublicMethed.queryAccount(contractAddress, blockingStubFull);

    logger.info("account002_after.getEnergyLimit : " + account002_after.getEnergyLimit());

    Assert.assertEquals(contractAccount_before.getAccountResource()
            .getFrozenBalanceForEnergy().getFrozenBalance() - freezeCount,
        contractAccount_after.getAccountResource().getFrozenBalanceForEnergy().getFrozenBalance());


  }

  @Test(description = "energy caulate after transaction end")
  public void freezeEnergyCaulate() {

    String methedStr = "freeze(address,uint256,uint256)";
    String argsStr = "\"" + Base58.encode58Check(testAddress001) + "\"," + freezeCount + "," + "1";
    String txid = PublicMethed.triggerContract(contractAddress,methedStr,argsStr,
        false,0,maxFeeLimit,testAddress001,testKey001,blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    TransactionInfo info = PublicMethed.getTransactionInfoById(txid,blockingStubFull).get();
    AccountResourceMessage testAccount001 = PublicMethed
        .getAccountResource(testAddress001,blockingStubFull);


    Assert.assertTrue(testAccount001.getEnergyLimit() > 0);
    Assert.assertTrue(info.getReceipt().getEnergyFee() > 0);
    Assert.assertTrue(testAccount001.getEnergyLimit() > info.getReceipt().getEnergyUsageTotal());

    methedStr = "unfreeze(address,uint256)";
    argsStr = "\"" + Base58.encode58Check(testAddress001) + "\",1";
    txid = PublicMethed.triggerContract(contractAddress,methedStr,argsStr,
        false,0,maxFeeLimit,testAddress001,testKey001,blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    info = PublicMethed.getTransactionInfoById(txid,blockingStubFull).get();
    testAccount001 = PublicMethed.getAccountResource(testAddress001,blockingStubFull);

    Assert.assertEquals(code.SUCESS,info.getResult());
    Assert.assertEquals(contractResult.SUCCESS,info.getReceipt().getResult());


    Assert.assertEquals(0, info.getReceipt().getEnergyFee());
    Assert.assertEquals(0, testAccount001.getEnergyLimit());
    Assert.assertTrue(testAccount001.getEnergyUsed() > 0);
  }

  @Test(description = "get Zero Address ExpirTime,used to be that freeze to contract self",
      dependsOnMethods = "FreezeContractTest002")
  public void getZeroExpireTimeTest() {
    String ExpireTimeMethedStr = "getExpireTime(address,uint256)";
    String ExpireTimeArgsStr = "\"T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb\"" + ",0";
    TransactionExtention extention = PublicMethed
        .triggerConstantContractForExtention(contractAddress,ExpireTimeMethedStr,ExpireTimeArgsStr,
            false,0,maxFeeLimit,"#",0, testAddress001,testKey001,blockingStubFull);
    Long ExpireTime1 = ByteArray.toLong(extention.getConstantResult(0).toByteArray());
    logger.info("ExpireTime1: " + ExpireTime1);
    Assert.assertEquals(0,ExpireTime1.longValue());

    ExpireTimeArgsStr = "\"T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb\"" + ",1";
    extention = PublicMethed
        .triggerConstantContractForExtention(contractAddress,ExpireTimeMethedStr,ExpireTimeArgsStr,
            false,0,maxFeeLimit,"#",0, testAddress001,testKey001,blockingStubFull);
    Long ExpireTime2 = ByteArray.toLong(extention.getConstantResult(0).toByteArray());
    logger.info("ExpireTime2: " + ExpireTime2);
    Assert.assertEquals(0,ExpireTime2.longValue());

    // freeze(address payable receiver, uint amount, uint res)
    String methedStr = "freeze(address,uint256,uint256)";
    String argsStr = "\"" + "T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb" + "\"," + freezeCount + "," + "1";
    String txid = PublicMethed.triggerContract(contractAddress,methedStr,argsStr,
        false,0,maxFeeLimit,testAddress001,testKey001,blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    TransactionInfo info = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    Assert.assertEquals(code.SUCESS,info.getResult());
    Assert.assertEquals(contractResult.SUCCESS,info.getReceipt().getResult());

    extention = PublicMethed
        .triggerConstantContractForExtention(contractAddress,ExpireTimeMethedStr,ExpireTimeArgsStr,
            false,0,maxFeeLimit,"#",0, testAddress001,testKey001,blockingStubFull);
    Long ExpireTime = ByteArray.toLong(extention.getConstantResult(0).toByteArray());
    logger.info("ExpireTime: " + ExpireTime);
    Assert.assertEquals((ExpireTime + 3) * 1000, info.getBlockTimeStamp());


  }

  @Test(description = "freeze in constructor")
  public void FreezeContractTest006() {

    AccountResourceMessage account003_before = PublicMethed
        .getAccountResource(testAddress003,blockingStubFull);

    String filePath = "src/test/resources/soliditycode/freezeContract001.sol";
    String contractName = "D";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    long callValue = 10000_000000L;
    byte[] contractAddress = PublicMethed
        .deployContract(contractName, abi, code, "", maxFeeLimit, callValue,
            100, null, testKey003,
            testAddress003, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    AccountResourceMessage account003_after = PublicMethed
        .getAccountResource(testAddress003,blockingStubFull);
    Account contractAccount_after = PublicMethed.queryAccount(contractAddress, blockingStubFull);

    logger.info("account002_before.getEnergyLimit : " + account003_before.getEnergyLimit());
    logger.info("account002_after.getEnergyLimit : " + account003_after.getEnergyLimit());
    Assert.assertTrue(account003_before.getEnergyLimit() < account003_after.getEnergyLimit());
    Assert.assertEquals(callValue,
        contractAccount_after.getAccountResource().getDelegatedFrozenBalanceForEnergy());
    Assert.assertEquals(0, contractAccount_after.getBalance());
  }

}
