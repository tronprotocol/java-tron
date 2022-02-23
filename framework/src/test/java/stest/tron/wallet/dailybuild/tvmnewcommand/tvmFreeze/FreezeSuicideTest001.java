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
public class FreezeSuicideTest001 {

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
  private long freezeEnergyUseage;
  private long callValue;
  private byte[] create2Address;
  private final Long freezeCount = 1000_000000L;


  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    PublicMethed.printAddress(testKey001);
    PublicMethed.printAddress(testKey002);
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
    Assert.assertTrue(PublicMethed.sendcoin(testAddress002,200_0000_000000L,
        testFoundationAddress,testFoundationKey,blockingStubFull));

    String filePath = "src/test/resources/soliditycode/freezeContract001.sol";
    String contractName = "TestFreeze";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    callValue = 50000_000000L;
    contractAddress = PublicMethed
        .deployContract(contractName, abi, code, "", maxFeeLimit, callValue,
            100, null, testFoundationKey,
            testFoundationAddress, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed.freezeBalanceGetEnergy(testAddress002,
        100_0000_000000L,0,0,testKey002,blockingStubFull));
  }

  @Test(enabled = true, description = "when delegate freeze, cannot suicide")
  public void FreezeSuicideTest001() {

    Account contractAccount_before = PublicMethed.queryAccount(contractAddress,blockingStubFull);

    // freeze(address payable receiver, uint amount, uint res)
    String methedStr = "freeze(address,uint256,uint256)";
    String argsStr = "\"" + Base58.encode58Check(testAddress002) + "\"," + freezeCount + "," + "1";
    String txid = PublicMethed.triggerContract(contractAddress,methedStr,argsStr,
        false,0,maxFeeLimit,testAddress001,testKey001,blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Account contractAccount_after = PublicMethed.queryAccount(contractAddress, blockingStubFull);

    Assert.assertEquals(contractAccount_before.getAccountResource()
            .getDelegatedFrozenBalanceForEnergy() + freezeCount,
        contractAccount_after.getAccountResource().getDelegatedFrozenBalanceForEnergy());
    Assert.assertEquals(contractAccount_before.getBalance() - freezeCount,
        contractAccount_after.getBalance());

    methedStr = "destroy(address)";
    argsStr = "\"" + Base58.encode58Check(testAddress002) + "\"";
    txid = PublicMethed.triggerContract(contractAddress,methedStr,argsStr,
        false,0,maxFeeLimit,testAddress001,testKey001,blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    TransactionInfo info = PublicMethed.getTransactionInfoById(txid,blockingStubFull).get();
    Assert.assertEquals(code.FAILED,info.getResult());
    Assert.assertEquals(contractResult.REVERT,info.getReceipt().getResult());


    methedStr = "unfreeze(address,uint256)";
    argsStr = "\"" + Base58.encode58Check(testAddress002) + "\"," + "1";
    PublicMethed.triggerContract(contractAddress,methedStr,argsStr,
        false,0,maxFeeLimit,testAddress001,testKey001,blockingStubFull);
  }

  @Test(enabled = true, description = "when delegate freeze to self, then suicide")
  public void FreezeSuicideTest002() {

    Account contractAccount_before = PublicMethed.queryAccount(contractAddress,blockingStubFull);
    AccountResourceMessage freezeAccount_before = PublicMethed
        .getAccountResource(testAddress002,blockingStubFull);

    // freeze(address payable receiver, uint amount, uint res)
    String methedStr = "freeze(address,uint256,uint256)";
    String argsStr = "\"" + Base58.encode58Check(contractAddress) + "\"," + freezeCount + "," + "0";
    String txid = PublicMethed.triggerContract(contractAddress,methedStr,argsStr,
        false,0,maxFeeLimit,testAddress001,testKey001,blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    AccountResourceMessage freezeAccount_after = PublicMethed
        .getAccountResource(testAddress002,blockingStubFull);
    Account contractAccount_after = PublicMethed.queryAccount(contractAddress, blockingStubFull);

    Assert.assertEquals(freezeCount.longValue(),
        contractAccount_after.getFrozen(0).getFrozenBalance());
    Assert.assertEquals(contractAccount_before.getBalance() - freezeCount,
        contractAccount_after.getBalance());

    logger.info("freezeAccount_before.getNetLimit : " + freezeAccount_before.getNetLimit());
    logger.info("freezeAccount_after.getNetLimit : " + freezeAccount_after.getNetLimit());
    Assert.assertTrue(freezeAccount_after.getNetLimit() < freezeAccount_before.getNetLimit());


    Long beforeBalance = PublicMethed.queryAccount(testAddress002,blockingStubFull).getBalance();
    methedStr = "destroy(address)";
    argsStr = "\"" + Base58.encode58Check(testAddress002) + "\"";
    txid = PublicMethed.triggerContract(contractAddress,methedStr,argsStr,
        false,0,maxFeeLimit,testAddress001,testKey001,blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    TransactionInfo info = PublicMethed.getTransactionInfoById(txid,blockingStubFull).get();
    Assert.assertEquals(code.SUCESS,info.getResult());
    Assert.assertEquals(contractResult.SUCCESS,info.getReceipt().getResult());

    freezeAccount_after = PublicMethed
        .getAccountResource(testAddress002,blockingStubFull);
    Assert.assertEquals(freezeAccount_before.getNetLimit(),
        freezeAccount_after.getNetLimit());

    Long AfterBalance = PublicMethed.queryAccount(testAddress002,blockingStubFull).getBalance();
    Assert.assertEquals(beforeBalance + callValue, AfterBalance.longValue());


  }

  @Test(enabled = true, description = "suicide、freeze、unfreeze、getExpireTime "
      + "with suicided create2 address")
  public void FreezeSuicideTest003() {

    String filePath = "src/test/resources/soliditycode/freezeContract001.sol";
    String contractName = "TestFreeze";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String bytecode = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    callValue = 10000_000000L;
    contractAddress = PublicMethed
        .deployContract(contractName, abi, bytecode, "", maxFeeLimit, callValue,
            100, null, testFoundationKey,
            testFoundationAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    // triggerconstant create2 function, and get create2 Address
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

    // freeze to create2 Address, active create2 address
    String methedStr = "freeze(address,uint256,uint256)";
    String argsStr = "\"" + Base58.encode58Check(create2Address) + "\"," + freezeCount + "," + "1";
    logger.info("argsStr: " + argsStr);
    String txid = PublicMethed.triggerContract(contractAddress,methedStr,argsStr,
        false,0,maxFeeLimit,testAddress001,testKey001,blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    TransactionInfo info = PublicMethed.getTransactionInfoById(txid,blockingStubFull).get();
    Assert.assertEquals(code.SUCESS,info.getResult());
    Assert.assertEquals(contractResult.SUCCESS,info.getReceipt().getResult());

    // create2 contract Address, turn create2 address to contract type
    txid = PublicMethed.triggerContract(contractAddress,create2MethedStr,
        create2ArgsStr,false,0,maxFeeLimit,testAddress001,testKey001,blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    info = PublicMethed.getTransactionInfoById(txid,blockingStubFull).get();
    Assert.assertEquals(code.SUCESS,info.getResult());
    Assert.assertEquals(contractResult.SUCCESS,info.getReceipt().getResult());


    // create2 contract suicide
    methedStr = "destroy(address)";
    argsStr = "\"" + Base58.encode58Check(testAddress002) + "\"";
    txid = PublicMethed.triggerContract(create2Address,methedStr,argsStr,false,0,
        maxFeeLimit,testAddress001,testKey001,blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    info = PublicMethed.getTransactionInfoById(txid,blockingStubFull).get();
    Assert.assertEquals(code.SUCESS,info.getResult());
    Assert.assertEquals(contractResult.SUCCESS,info.getReceipt().getResult());

    // get create2 account ExpireTime
    methedStr = "getExpireTime(address,uint256)";
    argsStr = "\"" + Base58.encode58Check(create2Address) + "\"" + ",1";
    TransactionExtention extention = PublicMethed
        .triggerConstantContractForExtention(contractAddress,methedStr,argsStr,
            false,0,maxFeeLimit,"#",0, testAddress001,testKey001,blockingStubFull);
    Long ExpireTime = ByteArray.toLong(extention.getConstantResult(0).toByteArray());
    logger.info("ExpireTime: " + ExpireTime);
    Assert.assertTrue(ExpireTime > 0);

    // suicide FreezeTest contract, and should be failed
    methedStr = "destroy(address)";
    argsStr = "\"" + Base58.encode58Check(testAddress002) + "\"";
    txid = PublicMethed.triggerContract(contractAddress,methedStr,argsStr,false,0,
        maxFeeLimit,testAddress001,testKey001,blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    info = PublicMethed.getTransactionInfoById(txid,blockingStubFull).get();
    Assert.assertEquals(code.FAILED,info.getResult());
    Assert.assertEquals(contractResult.REVERT,info.getReceipt().getResult());

    Account contract_before = PublicMethed.queryAccount(contractAddress,blockingStubFull);

    // unfreeze suicide create2 account
    methedStr = "unfreeze(address,uint256)";
    argsStr = "\"" + Base58.encode58Check(create2Address) + "\"," + "1";
    txid = PublicMethed.triggerContract(contractAddress,methedStr,argsStr,
        false,0,maxFeeLimit,testAddress001,testKey001,blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    info = PublicMethed.getTransactionInfoById(txid,blockingStubFull).get();
    Assert.assertEquals(code.SUCESS,info.getResult());
    Assert.assertEquals(contractResult.SUCCESS,info.getReceipt().getResult());

    Account contract_after = PublicMethed.queryAccount(contractAddress,blockingStubFull);
    Assert.assertEquals(contract_before.getBalance() + freezeCount, contract_after.getBalance());
    Assert.assertEquals(contract_after.getAccountResource().getDelegatedFrozenBalanceForEnergy(),
        contract_before.getAccountResource().getDelegatedFrozenBalanceForEnergy() - freezeCount);


  }


}
