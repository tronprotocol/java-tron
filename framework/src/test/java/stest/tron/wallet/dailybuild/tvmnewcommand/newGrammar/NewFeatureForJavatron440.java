package stest.tron.wallet.dailybuild.tvmnewcommand.newGrammar;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol;
import org.tron.protos.contract.SmartContractOuterClass;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;



@Slf4j
public class NewFeatureForJavatron440 {

  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethed.getFinalAddress(testNetAccountKey);
  byte[] contractC = null;
  byte[] contractD = null;
  byte[] create2Address;
  String create2Str;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contractExcAddress = ecKey1.getAddress();
  String contractExcKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(contractExcKey);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    Assert.assertTrue(PublicMethed
        .sendcoin(contractExcAddress, 300100_000_000L,
            testNetAccountAddress, testNetAccountKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String filePath = "src/test/resources/soliditycode/NewFeatureJavatron440.sol";
    String contractName = "C";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    contractC = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        500000000L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContractOuterClass.SmartContract smartContract = PublicMethed.getContract(contractC,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());

    contractName = "D";
    retMap = PublicMethed.getBycodeAbi(filePath, contractName);

    code = retMap.get("byteCode").toString();
    abi = retMap.get("abI").toString();
    contractD = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        500000000L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    smartContract = PublicMethed.getContract(contractD,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());
  }


  @Test(enabled = true, description = "test get base fee value = commit.No 11 energy fee")
  public void test01baseFee() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractC,
            "baseFee()", "#", false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    long basefee = ByteArray.toLong(transactionExtention.getConstantResult(0).toByteArray());
    logger.info("basefee: " + basefee);
    long energyfee;
    Protocol.ChainParameters chainParameters = blockingStubFull
        .getChainParameters(GrpcAPI.EmptyMessage.newBuilder().build());
    Optional<Protocol.ChainParameters> getChainParameters = Optional.ofNullable(chainParameters);
    logger.info(Long.toString(getChainParameters.get().getChainParameterCount()));
    String key = "";
    boolean flag = false;
    for (Integer i = 0; i < getChainParameters.get().getChainParameterCount(); i++) {
      key = getChainParameters.get().getChainParameter(i).getKey();
      if ("getEnergyFee".equals(key)) {
        energyfee = getChainParameters.get().getChainParameter(i).getValue();
        logger.info("energyfee: " + energyfee);
        Assert.assertEquals(basefee, energyfee);
        flag = true;
      }
    }
    Assert.assertTrue(flag);
  }

  @Test(enabled = true, description = "test get gas price value = commit.No 11 energy fee")
  public void test02GasPrice() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractC,
            "gasPrice()", "#", false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
  }

  @Test(enabled = true, description = "get create2 address, test get base fee ")
  public void test03BaseFeeFromCreate2() {
    String methedStr = "deploy(uint256)";
    String argsStr = "1";
    String txid = PublicMethed.triggerContract(contractD, methedStr, argsStr,
        false, 0, maxFeeLimit, contractExcAddress,
        contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS,
        info.get().getReceipt().getResult());

    String create2Str =
        "41" + ByteArray.toHexString(info.get().getContractResult(0).toByteArray())
            .substring(24);
    logger.info("hex create2 address: " + create2Str);
    create2Address = ByteArray.fromHexString(create2Str);
    logger.info("create2Address: " + Base58.encode58Check(create2Address));

    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(create2Address,
            "baseFeeOnly()", "#", false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    long basefee = ByteArray.toLong(transactionExtention.getConstantResult(0).toByteArray());
    logger.info("basefee: " + basefee);
    long energyfee;
    Protocol.ChainParameters chainParameters = blockingStubFull
        .getChainParameters(GrpcAPI.EmptyMessage.newBuilder().build());
    Optional<Protocol.ChainParameters> getChainParameters = Optional.ofNullable(chainParameters);
    logger.info(Long.toString(getChainParameters.get().getChainParameterCount()));
    String key = "";
    boolean flag = false;
    for (Integer i = 0; i < getChainParameters.get().getChainParameterCount(); i++) {
      key = getChainParameters.get().getChainParameter(i).getKey();
      if ("getEnergyFee".equals(key)) {
        energyfee = getChainParameters.get().getChainParameter(i).getValue();
        logger.info("energyfee: " + energyfee);
        Assert.assertEquals(basefee, energyfee);
        flag = true;
      }
    }
    Assert.assertTrue(flag);

    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(create2Address,
            "gasPriceOnly()", "#", false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    long gasprice = ByteArray.toLong(transactionExtention.getConstantResult(0).toByteArray());
    logger.info("gasprice: " + gasprice);
    Assert.assertEquals(basefee, gasprice);
  }

  @Test(enabled = true, description = "call can use 63/64 energy in new contract")
  public void test04CallEnergy() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] transferToAddress = ecKey1.getAddress();
    String transferToKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    PublicMethed.printAddress(transferToKey);

    Long temMaxLimitFee = 200000000L;
    String methedStr = "testCall(address,address)";
    String argsStr = "\"" + Base58.encode58Check(contractD) + "\"," + "\""
        + Base58.encode58Check(transferToAddress) + "\"";
    String txid = PublicMethed.triggerContract(contractC, methedStr, argsStr,
        false, 0, temMaxLimitFee, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    System.out.println("0000000" + info.toString());
    Protocol.Account testAccount =
        PublicMethed.queryAccountByAddress(transferToAddress, blockingStubFull);
    System.out.println("testAccount: " + testAccount.toString());
    Assert.assertEquals(1, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.REVERT,
        info.get().getReceipt().getResult());
    Assert.assertTrue(info.get().getInternalTransactions(0).getRejected());
    Assert.assertTrue(info.get().getReceipt().getEnergyFee() < temMaxLimitFee);
  }

  @Test(enabled = true, description = "create2 address call can use 63/64 energy in new contract")
  public void test05Create2AddressCallEnergy() {
    String methedStr = "deploy(uint256)";
    String argsStr = "2";
    String txid = PublicMethed.triggerContract(contractD, methedStr, argsStr,
        false, 0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS,
        info.get().getReceipt().getResult());

    String create2Str =
        "41" + ByteArray.toHexString(info.get().getContractResult(0).toByteArray())
            .substring(24);
    logger.info("hex create2 address: " + create2Str);
    create2Address = ByteArray.fromHexString(create2Str);
    logger.info("create2Address: " + Base58.encode58Check(create2Address));

    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] transferToAddress = ecKey1.getAddress();
    String transferToKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    PublicMethed.printAddress(transferToKey);

    Long temMaxLimitFee = 200000000L;
    methedStr = "testCall(address,address)";
    argsStr = "\"" + Base58.encode58Check(contractD) + "\"," + "\""
        + Base58.encode58Check(transferToAddress) + "\"";
    txid = PublicMethed.triggerContract(create2Address, methedStr, argsStr,
        false, 0, temMaxLimitFee, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    info = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    System.out.println("0000000" + info.get().toString());

    Protocol.Account testAccount =
        PublicMethed.queryAccountByAddress(transferToAddress, blockingStubFull);
    System.out.println("testAccount: " + testAccount.toString());
    Assert.assertEquals("", testAccount.toString());
    Assert.assertEquals(1, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.REVERT,
        info.get().getReceipt().getResult());
    Assert.assertTrue(info.get().getInternalTransactions(0).getRejected());
    Assert.assertTrue(info.get().getReceipt().getEnergyFee() < temMaxLimitFee);
  }

  @Test(enabled = true, description = "create2 address delegatecall "
      + "can use 63/64 energy in new contract")
  public void test06Create2AddressDelegateCallEnergy() {
    String methedStr = "deploy(uint256)";
    String argsStr = "5";
    String txid = PublicMethed.triggerContract(contractD, methedStr, argsStr,
        false, 0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS,
        info.get().getReceipt().getResult());

    String create2Str =
        "41" + ByteArray.toHexString(info.get().getContractResult(0).toByteArray())
            .substring(24);
    logger.info("hex create2 address: " + create2Str);
    create2Address = ByteArray.fromHexString(create2Str);
    logger.info("create2Address: " + Base58.encode58Check(create2Address));

    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] transferToAddress = ecKey1.getAddress();
    String transferToKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    PublicMethed.printAddress(transferToKey);

    Long temMaxLimitFee = 200000000L;
    methedStr = "testDelegateCall(address,address)";
    argsStr = "\"" + Base58.encode58Check(contractD) + "\"," + "\""
        + Base58.encode58Check(transferToAddress) + "\"";
    txid = PublicMethed.triggerContract(create2Address, methedStr, argsStr,
        false, 0, temMaxLimitFee, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    info = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    System.out.println("0000000" + info.toString());

    Protocol.Account testAccount =
        PublicMethed.queryAccountByAddress(transferToAddress, blockingStubFull);
    System.out.println("testAccount: " + testAccount.toString());
    Assert.assertEquals("", testAccount.toString());
    Assert.assertEquals(1, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.REVERT,
        info.get().getReceipt().getResult());
    Assert.assertTrue(info.get().getInternalTransactions(0).getRejected());
    Assert.assertTrue(info.get().getReceipt().getEnergyFee() < temMaxLimitFee);
  }

  @Test(enabled = true, description = "create2 address this.function "
      + "can use 63/64 energy in new contract")
  public void test07Create2AddressCallFunctionEnergy() {
    String methedStr = "deploy(uint256)";
    String argsStr = "6";
    String txid = PublicMethed.triggerContract(contractD, methedStr, argsStr,
        false, 0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS,
        info.get().getReceipt().getResult());

    String create2Str =
        "41" + ByteArray.toHexString(info.get().getContractResult(0).toByteArray())
            .substring(24);
    logger.info("hex create2 address: " + create2Str);
    create2Address = ByteArray.fromHexString(create2Str);
    logger.info("create2Address: " + Base58.encode58Check(create2Address));

    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] transferToAddress = ecKey1.getAddress();
    String transferToKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    PublicMethed.printAddress(transferToKey);

    Long temMaxLimitFee = 200000000L;
    methedStr = "testCallFunctionInContract(address)";
    argsStr = "\"" + Base58.encode58Check(transferToAddress) + "\"";
    txid = PublicMethed.triggerContract(create2Address, methedStr, argsStr,
        false, 0, temMaxLimitFee, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    info = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    System.out.println("0000000" + info.toString());

    Protocol.Account testAccount =
        PublicMethed.queryAccountByAddress(transferToAddress, blockingStubFull);
    System.out.println("testAccount: " + testAccount.toString());
    Assert.assertEquals("", testAccount.toString());
    Assert.assertEquals(1, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.REVERT,
        info.get().getReceipt().getResult());
    Assert.assertTrue(info.get().getInternalTransactions(0).getRejected());
    Assert.assertTrue(info.get().getReceipt().getEnergyFee() < temMaxLimitFee);
  }

  //
  @Test(enabled = false, description = "test get Ripemd160")
  public void test08getRipemd160() {
    String args = "0000000000000000000000000000000000000000000000000000000000000064";
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractC,
            "getRipemd160(bytes)", args, true,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    String result = ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray());
    System.out.println("result: " + result);
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethed.freedResource(contractExcAddress, contractExcKey,
        testNetAccountAddress, blockingStubFull);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }


}

