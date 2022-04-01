package stest.tron.wallet.dailybuild.assetissue.grammar;

import static org.tron.protos.Protocol.Transaction.Result.contractResult.BAD_JUMP_DESTINATION_VALUE;
import static org.tron.protos.Protocol.Transaction.Result.contractResult.OUT_OF_MEMORY_VALUE;
import static org.tron.protos.Protocol.Transaction.Result.contractResult.OUT_OF_TIME_VALUE;
import static org.tron.protos.Protocol.Transaction.Result.contractResult.REVERT_VALUE;
import static org.tron.protos.Protocol.Transaction.Result.contractResult.STACK_TOO_LARGE_VALUE;
import static org.tron.protos.Protocol.Transaction.Result.contractResult.STACK_TOO_SMALL_VALUE;

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
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Result.contractResult;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class ContractGrammar004 {


  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethed.getFinalAddress(testNetAccountKey);
  byte[] contractAddress = null;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] grammarAddress = ecKey1.getAddress();
  String testKeyForGrammarAddress = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private ManagedChannel channelSolidity = null;
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(1);
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private String compilerVersion = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.solidityCompilerVersion");

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(testKeyForGrammarAddress);
    channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true).build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1).usePlaintext(true).build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);
  }

  @Test(enabled = true, description = "ContractResult is OUT_OF_TIME")
  public void test1Grammar001() {
    Assert.assertTrue(PublicMethed
        .sendcoin(grammarAddress, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String filePath = "./src/test/resources/soliditycode/walletTestMutiSign004.sol";
    String contractName = "timeoutTest";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    byte[] contractAddress = PublicMethed
        .deployContract(contractName, abi, code, "", maxFeeLimit, 0L, 100, null,
            testKeyForGrammarAddress, grammarAddress, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContract smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);
    org.testng.Assert.assertTrue(smartContract.getAbi().toString() != null);
    String txid = null;
    Optional<TransactionInfo> infoById = null;
    String initParmes = "\"" + "100000" + "\"";
    txid = PublicMethed
        .triggerContract(contractAddress, "testUseCpu(uint256)", initParmes, false, 0, maxFeeLimit,
            grammarAddress, testKeyForGrammarAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("Txid is " + txid);
    logger.info("Trigger energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());

    Optional<Transaction> byId = PublicMethed.getTransactionById(txid, blockingStubFull);
    logger.info("getRet:" + byId.get().getRet(0));
    logger.info("getNumber:" + byId.get().getRet(0).getContractRet().getNumber());
    logger.info("getContractRetValue:" + byId.get().getRet(0).getContractRetValue());
    logger.info("getContractRet:" + byId.get().getRet(0).getContractRet());
    logger.info("ById:" + byId);

    Assert.assertEquals(byId.get().getRet(0).getContractRetValue(), OUT_OF_TIME_VALUE);
    Assert.assertEquals(byId.get().getRet(0).getContractRet(), contractResult.OUT_OF_TIME);

    Assert
        .assertEquals(ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray()), "");
    Assert.assertEquals(contractResult.OUT_OF_TIME, infoById.get().getReceipt().getResult());

    Assert.assertEquals(byId.get().getRet(0).getRet().getNumber(), 0);
    Assert.assertEquals(byId.get().getRet(0).getRetValue(), 0);


  }


  @Test(enabled = true, description = "ContractResult is OUT_OF_MEMORY")
  public void test2Grammar002() {
    String filePath = "./src/test/resources/soliditycode/testOutOfMem.sol";
    String contractName = "Test";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    byte[] contractAddress = PublicMethed
        .deployContract(contractName, abi, code, "", maxFeeLimit, 0L, 100, null,
            testKeyForGrammarAddress, grammarAddress, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContract smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);
    org.testng.Assert.assertTrue(smartContract.getAbi().toString() != null);
    String txid = null;
    Optional<TransactionInfo> infoById = null;
    String initParmes = "\"" + "31457280" + "\"";
    txid = PublicMethed
        .triggerContract(contractAddress, "testOutOfMem(uint256)", initParmes, false, 0,
            maxFeeLimit, grammarAddress, testKeyForGrammarAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("Txid is " + txid);
    logger.info("Trigger energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());

    Optional<Transaction> byId = PublicMethed.getTransactionById(txid, blockingStubFull);
    logger.info("getRet:" + byId.get().getRet(0));
    logger.info("getNumber:" + byId.get().getRet(0).getContractRet().getNumber());
    logger.info("getContractRetValue:" + byId.get().getRet(0).getContractRetValue());
    logger.info("getContractRet:" + byId.get().getRet(0).getContractRet());
    logger.info("ById:" + byId);

    logger.info("infoById:" + infoById);

    Assert.assertEquals(byId.get().getRet(0).getContractRetValue(), OUT_OF_MEMORY_VALUE);
    Assert.assertEquals(byId.get().getRet(0).getContractRet(), contractResult.OUT_OF_MEMORY);

    Assert
        .assertEquals(ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray()), "");
    Assert.assertEquals(contractResult.OUT_OF_MEMORY, infoById.get().getReceipt().getResult());

    Assert.assertEquals(byId.get().getRet(0).getRet().getNumber(), 0);
    Assert.assertEquals(byId.get().getRet(0).getRetValue(), 0);


  }


  @Test(enabled = true, description = "ContractResult is BAD_JUMP_DESTINATION")
  public void test3Grammar003() {
    String contractName = "Test";

    String code = "608060405234801561001057600080fd5b50d3801561001d57600080fd5b50d2801561002a57600"
        + "080fd5b5061011f8061003a6000396000f30060806040526004361060485763ffffffff7c01000000000000"
        + "000000000000000000000000000000000000000000006000350416634ef5a0088114604d5780639093b95b1"
        + "4608c575b600080fd5b348015605857600080fd5b50d38015606457600080fd5b50d28015607057600080fd"
        + "5b50607a60043560b8565b60408051918252519081900360200190f35b348015609757600080fd5b50d3801"
        + "560a357600080fd5b50d2801560af57600080fd5b5060b660ee565b005b6000606082604051908082528060"
        + "20026020018201604052801560e5578160200160208202803883390190505b50905050919050565b6001805"
        + "600a165627a7a7230582092ba162087e13f41c6d6c00ba493edc5a5a6250a3840ece5f99aa38b66366a7000"
        + "29";
    String abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"x\",\"type\":\"uint256\"}],\"name\""
        + ":\"testOutOfMem\",\"outputs\":[{\"name\":\"r\",\"type\":\"bytes32\"}],\"payable\":false"
        + ",\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs"
        + "\":[],\"name\":\"testBadJumpDestination\",\"outputs\":[],\"payable\":false,\"stateMutab"
        + "ility\":\"nonpayable\",\"type\":\"function\"}]";

    byte[] contractAddress = PublicMethed
        .deployContract(contractName, abi, code, "", maxFeeLimit, 0L, 100, null,
            testKeyForGrammarAddress, grammarAddress, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContract smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);
    org.testng.Assert.assertTrue(smartContract.getAbi().toString() != null);
    String txid = null;
    Optional<TransactionInfo> infoById = null;
    txid = PublicMethed
        .triggerContract(contractAddress, "testBadJumpDestination()", "#", false, 0, maxFeeLimit,
            grammarAddress, testKeyForGrammarAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("Txid is " + txid);
    logger.info("Trigger energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());

    Optional<Transaction> byId = PublicMethed.getTransactionById(txid, blockingStubFull);
    logger.info("getRet:" + byId.get().getRet(0));
    logger.info("getNumber:" + byId.get().getRet(0).getContractRet().getNumber());
    logger.info("getContractRetValue:" + byId.get().getRet(0).getContractRetValue());
    logger.info("getContractRet:" + byId.get().getRet(0).getContractRet());
    logger.info("ById:" + byId);

    logger.info("infoById:" + infoById);

    Assert.assertEquals(byId.get().getRet(0).getContractRetValue(), BAD_JUMP_DESTINATION_VALUE);
    Assert.assertEquals(byId.get().getRet(0).getContractRet(), contractResult.BAD_JUMP_DESTINATION);

    Assert
        .assertEquals(ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray()), "");
    Assert
        .assertEquals(contractResult.BAD_JUMP_DESTINATION, infoById.get().getReceipt().getResult());

    Assert.assertEquals(byId.get().getRet(0).getRet().getNumber(), 0);
    Assert.assertEquals(byId.get().getRet(0).getRetValue(), 0);


  }


  @Test(enabled = true, description = "ContractResult is OUT_OF_ENERGY")
  public void test4Grammar004() {

    String filePath = "src/test/resources/soliditycode/contractUnknownException.sol";
    String contractName = "testC";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    String txid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit, 20L, 100,
            null, testKeyForGrammarAddress, grammarAddress, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("Txid is " + txid);
    logger.info("Trigger energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());

    Optional<Transaction> byId = PublicMethed.getTransactionById(txid, blockingStubFull);
    logger.info("getRet:" + byId.get().getRet(0));
    logger.info("getNumber:" + byId.get().getRet(0).getContractRet().getNumber());
    logger.info("getContractRetValue:" + byId.get().getRet(0).getContractRetValue());
    logger.info("getContractRet:" + byId.get().getRet(0).getContractRet());
    logger.info("ById:" + byId);

    logger.info("infoById:" + infoById);

    Assert.assertEquals(byId.get().getRet(0).getContractRetValue(), REVERT_VALUE);
    Assert.assertEquals(byId.get().getRet(0).getContractRet(), contractResult.REVERT);

    Assert.assertEquals(ByteArray.toHexString(infoById.get()
            .getContractResult(0).toByteArray()),
        "4e487b710000000000000000000000000000000000000000000000000000000000000001");
    Assert.assertEquals(contractResult.REVERT, infoById.get().getReceipt().getResult());

    Assert.assertEquals(byId.get().getRet(0).getRet().getNumber(), 0);
    Assert.assertEquals(byId.get().getRet(0).getRetValue(), 0);

  }


  @Test(enabled = true, description = "ContractResult is ILLEGAL_OPERATION")
  public void test5Grammar005() {

    String filePath = "src/test/resources/soliditycode/assertExceptiontest1DivideInt.sol";
    String contractName = "divideIHaveArgsReturnStorage";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    contractAddress = PublicMethed
        .deployContract(contractName, abi, code, "", maxFeeLimit, 0L, 100, null,
            testKeyForGrammarAddress, grammarAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String txid = "";
    String num = "4" + "," + "0";

    txid = PublicMethed
        .triggerContract(contractAddress, "divideIHaveArgsReturn(int256,int256)", num, false, 0,
            maxFeeLimit, grammarAddress, testKeyForGrammarAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("infoById:" + infoById);
    Optional<Transaction> byId = PublicMethed.getTransactionById(txid, blockingStubFull);
    logger.info("getRet:" + byId.get().getRet(0));
    logger.info("getNumber:" + byId.get().getRet(0).getContractRet().getNumber());
    logger.info("getContractRetValue:" + byId.get().getRet(0).getContractRetValue());
    logger.info("getContractRet:" + byId.get().getRet(0).getContractRet());

    Assert.assertEquals(byId.get().getRet(0).getContractRet().getNumber(), REVERT_VALUE);
    Assert.assertEquals(byId.get().getRet(0).getContractRetValue(), REVERT_VALUE);
    Assert.assertEquals(byId.get().getRet(0).getContractRet(), contractResult.REVERT);

    Assert
        .assertEquals(ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray()),
            "4e487b710000000000000000000000000000000000000000000000000000000000000012");
    Assert.assertEquals(contractResult.REVERT, infoById.get().getReceipt().getResult());

  }


  @Test(enabled = true, description = "ContractResult is REVERT")
  public void test6Grammar006() {

    String filePath =
        "src/test/resources/soliditycode/requireExceptiontest1TestRequireContract.sol";
    String contractName = "TestThrowsContract";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    contractAddress = PublicMethed
        .deployContract(contractName, abi, code, "", maxFeeLimit, 0L, 100, null,
            testKeyForGrammarAddress, grammarAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    final String txid = PublicMethed
        .triggerContract(contractAddress, "testRequire()", "#", false, 0, maxFeeLimit,
            grammarAddress, testKeyForGrammarAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("infoById:" + infoById);
    Optional<Transaction> byId = PublicMethed.getTransactionById(txid, blockingStubFull);
    logger.info("getRet:" + byId.get().getRet(0));
    logger.info("getNumber:" + byId.get().getRet(0).getContractRet().getNumber());
    logger.info("getContractRetValue:" + byId.get().getRet(0).getContractRetValue());
    logger.info("getContractRet:" + byId.get().getRet(0).getContractRet());

    Assert.assertEquals(byId.get().getRet(0).getContractRet().getNumber(), REVERT_VALUE);
    Assert.assertEquals(byId.get().getRet(0).getContractRetValue(), REVERT_VALUE);
    Assert.assertEquals(byId.get().getRet(0).getContractRet(), contractResult.REVERT);

    Assert
        .assertEquals(ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray()), "");
    Assert.assertEquals(contractResult.REVERT, infoById.get().getReceipt().getResult());

  }

  @Test(enabled = true, description = "ContractResult is SUCCESS")
  public void test7Grammar007() {

    String filePath = "src/test/resources/soliditycode/assertExceptiontest1DivideInt.sol";
    String contractName = "divideIHaveArgsReturnStorage";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    contractAddress = PublicMethed
        .deployContract(contractName, abi, code, "", maxFeeLimit, 0L, 100, null,
            testKeyForGrammarAddress, grammarAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String txid = "";
    String num = "4" + "," + "2";

    txid = PublicMethed
        .triggerContract(contractAddress, "divideIHaveArgsReturn(int256,int256)", num, false, 0,
            maxFeeLimit, grammarAddress, testKeyForGrammarAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("infoById:" + infoById);
    Optional<Transaction> byId = PublicMethed.getTransactionById(txid, blockingStubFull);
    logger.info("getRet:" + byId.get().getRet(0));
    logger.info("getNumber:" + byId.get().getRet(0).getContractRet().getNumber());
    logger.info("getContractRetValue:" + byId.get().getRet(0).getContractRetValue());
    logger.info("getContractRet:" + byId.get().getRet(0).getContractRet());

    Assert.assertEquals(byId.get().getRet(0).getContractRet().getNumber(),
        contractResult.SUCCESS_VALUE);
    Assert.assertEquals(byId.get().getRet(0).getContractRetValue(), contractResult.SUCCESS_VALUE);
    Assert.assertEquals(byId.get().getRet(0).getContractRet(), contractResult.SUCCESS);

    Assert.assertEquals(contractResult.SUCCESS, infoById.get().getReceipt().getResult());

    Assert.assertEquals(ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray()),
        "0000000000000000000000000000000000000000000000000000000000000002");

  }


  @Test(enabled = true, description = "ContractResult is TRANSFER_FAILED")
  public void test8Grammar008() {
    String filePath = "src/test/resources/soliditycode/TransferFailed001.sol";
    String contractName = "EnergyOfTransferFailedTest";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    contractAddress = PublicMethed
        .deployContract(contractName, abi, code, "", maxFeeLimit, 1000000L, 100, null,
            testKeyForGrammarAddress, grammarAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Account info;

    AccountResourceMessage resourceInfo = PublicMethed
        .getAccountResource(grammarAddress, blockingStubFull);
    info = PublicMethed.queryAccount(testKeyForGrammarAddress, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    ECKey ecKey2 = new ECKey(Utils.getRandom());
    byte[] nonexistentAddress = ecKey2.getAddress();
    String txid = "";
    String num = "1" + ",\"" + Base58.encode58Check(nonexistentAddress) + "\"";

    txid = PublicMethed
        .triggerContract(contractAddress, "testTransferTrxNonexistentTarget(uint256,address)", num,
            false, 0, maxFeeLimit, grammarAddress, testKeyForGrammarAddress, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("infobyid : --- " + infoById);

    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsage();
    Long netFee = infoById.get().getReceipt().getNetFee();
    long energyUsageTotal = infoById.get().getReceipt().getEnergyUsageTotal();
    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);
    logger.info("energyUsageTotal:" + energyUsageTotal);

    Account infoafter = PublicMethed.queryAccount(testKeyForGrammarAddress, blockingStubFull1);
    AccountResourceMessage resourceInfoafter = PublicMethed
        .getAccountResource(grammarAddress, blockingStubFull1);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    logger.info(
        "infoById.get().getReceipt().getResult():   " + infoById.get().getReceipt().getResult());
    logger.info("ByteArray.toStr(infoById.get().getResMessage().toByteArray()):   " + ByteArray
        .toStr(infoById.get().getResMessage().toByteArray()));
    /*Assert.assertEquals(
        "transfer trx failed: Validate InternalTransfer error, no ToAccount."
            + " And not allowed to create account in smart contract.",
        ByteArray.toStr(infoById.get().getResMessage().toByteArray()));*/

    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    Assert.assertNotEquals(10000000, energyUsageTotal);


  }


  @Test(enabled = true, description = "ContractResult is STACK_TOO_SMALL")
  public void test9Grammar009() {

    String contractName = "TestThrowsContract";
    String abi = "[{\"constant\":false,\"inputs\":[],\"name\":\"testStackTooSmall\",\"outputs\":[]"
        + ",\"payable\":false,\"type\":\"function\",\"stateMutability\":\"nonpayable\"}]";
    String code = "60606040523415600b57fe5b5b60608060196000396000f300606060405263ffffffff60e060020"
        + "a6000350416632f3a24cc81146020575bfe5b3415602757fe5b602d602f565b005b50505b5600a165627a7a"
        + "723058208184f2ff2627a8a490bfd1233a891f2f4605375d0fec375e237ffc188cdd7ec70029";
    contractAddress = PublicMethed
        .deployContract(contractName, abi, code, "", maxFeeLimit, 0L, 100, null,
            testKeyForGrammarAddress, grammarAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    final String txid = PublicMethed
        .triggerContract(contractAddress, "testStackTooSmall()", "#", false, 0, maxFeeLimit,
            grammarAddress, testKeyForGrammarAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("infoById:" + infoById);
    Optional<Transaction> byId = PublicMethed.getTransactionById(txid, blockingStubFull);
    logger.info("getRet:" + byId.get().getRet(0));
    logger.info("getNumber:" + byId.get().getRet(0).getContractRet().getNumber());
    logger.info("getContractRetValue:" + byId.get().getRet(0).getContractRetValue());
    logger.info("getContractRet:" + byId.get().getRet(0).getContractRet());

    Assert.assertEquals(byId.get().getRet(0).getContractRet().getNumber(), STACK_TOO_SMALL_VALUE);
    Assert.assertEquals(byId.get().getRet(0).getContractRetValue(), STACK_TOO_SMALL_VALUE);
    Assert.assertEquals(byId.get().getRet(0).getContractRet(), contractResult.STACK_TOO_SMALL);

    Assert
        .assertEquals(ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray()), "");
    Assert.assertEquals(contractResult.STACK_TOO_SMALL, infoById.get().getReceipt().getResult());

  }

  @Test(enabled = true, description = "ContractResult is STACK_TOO_LARGE")
  public void test9Grammar010() {

    String contractName = "TestThrowsContract";
    String abi = "[{\"constant\":false,\"inputs\":[],\"name\":\"testStackTooLarge\",\"outputs\":[]"
        + ",\"payable\":false,\"type\":\"function\",\"stateMutability\":\"nonpayable\"}]";
    String code = "6060604052341561000c57fe5b5b610b658061001c6000396000f300606060405263ffffffff60e"
        + "060020a600035041663f7d9c5c68114610021575bfe5b341561002957fe5b610031610033565b005b600060"
        + "0160026003600460056006600760086009600a600b600c600d600e600f60106011601260136014601560166"
        + "01760186019601a601b601c601d601e601f6020602160226023602460256026602760286029602a602b602c"
        + "602d602e602f6030603160326033603460356036603760386039603a603b603c603d603e603f60406041604"
        + "26043604460456046604760486049604a604b604c604d604e604f6050605160526053605460556056605760"
        + "586059605a605b605c605d605e605f6060606160626063606460656066606760686069606a606b606c606d6"
        + "06e606f6070607160726073607460756076607760786079607a607b607c607d607e607f6080608160826083"
        + "608460856086608760886089608a608b608c608d608e608f609060916092609360946095609660976098609"
        + "9609a609b609c609d609e609f60a060a160a260a360a460a560a660a760a860a960aa60ab60ac60ad60ae60"
        + "af60b060b160b260b360b460b560b660b760b860b960ba60bb60bc60bd60be60bf60c060c160c260c360c46"
        + "0c560c660c760c860c960ca60cb60cc60cd60ce60cf60d060d160d260d360d460d560d660d760d860d960da"
        + "60db60dc60dd60de60df60e060e160e260e360e460e560e660e760e860e960ea60eb60ec60ed60ee60ef60f"
        + "060f160f260f360f460f560f660f760f860f960fa60fb60fc60fd60fe60ff61010061010161010261010361"
        + "010461010561010661010761010861010961010a61010b61010c61010d61010e61010f61011061011161011"
        + "261011361011461011561011661011761011861011961011a61011b61011c61011d61011e61011f61012061"
        + "012161012261012361012461012561012661012761012861012961012a61012b61012c61012d61012e61012"
        + "f61013061013161013261013361013461013561013661013761013861013961013a61013b61013c61013d61"
        + "013e61013f61014061014161014261014361014461014561014661014761014861014961014a61014b61014"
        + "c61014d61014e61014f61015061015161015261015361015461015561015661015761015861015961015a61"
        + "015b61015c61015d61015e61015f61016061016161016261016361016461016561016661016761016861016"
        + "961016a61016b61016c61016d61016e61016f61017061017161017261017361017461017561017661017761"
        + "017861017961017a61017b61017c61017d61017e61017f61018061018161018261018361018461018561018"
        + "661018761018861018961018a61018b61018c61018d61018e61018f61019061019161019261019361019461"
        + "019561019661019761019861019961019a61019b61019c61019d61019e61019f6101a06101a16101a26101a"
        + "36101a46101a56101a66101a76101a86101a96101aa6101ab6101ac6101ad6101ae6101af6101b06101b161"
        + "01b26101b36101b46101b56101b66101b76101b86101b96101ba6101bb6101bc6101bd6101be6101bf6101c"
        + "06101c16101c26101c36101c46101c56101c66101c76101c86101c96101ca6101cb6101cc6101cd6101ce61"
        + "01cf6101d06101d16101d26101d36101d46101d56101d66101d76101d86101d96101da6101db6101dc6101d"
        + "d6101de6101df6101e06101e16101e26101e36101e46101e56101e66101e76101e86101e96101ea6101eb61"
        + "01ec6101ed6101ee6101ef6101f06101f16101f26101f36101f46101f56101f66101f76101f86101f96101f"
        + "a6101fb6101fc6101fd6101fe6101ff61020061020161020261020361020461020561020661020761020861"
        + "020961020a61020b61020c61020d61020e61020f61021061021161021261021361021461021561021661021"
        + "761021861021961021a61021b61021c61021d61021e61021f61022061022161022261022361022461022561"
        + "022661022761022861022961022a61022b61022c61022d61022e61022f61023061023161023261023361023"
        + "461023561023661023761023861023961023a61023b61023c61023d61023e61023f61024061024161024261"
        + "024361024461024561024661024761024861024961024a61024b61024c61024d61024e61024f61025061025"
        + "161025261025361025461025561025661025761025861025961025a61025b61025c61025d61025e61025f61"
        + "026061026161026261026361026461026561026661026761026861026961026a61026b61026c61026d61026"
        + "e61026f61027061027161027261027361027461027561027661027761027861027961027a61027b61027c61"
        + "027d61027e61027f61028061028161028261028361028461028561028661028761028861028961028a61028"
        + "b61028c61028d61028e61028f61029061029161029261029361029461029561029661029761029861029961"
        + "029a61029b61029c61029d61029e61029f6102a06102a16102a26102a36102a46102a56102a66102a76102a"
        + "86102a96102aa6102ab6102ac6102ad6102ae6102af6102b06102b16102b26102b36102b46102b56102b661"
        + "02b76102b86102b96102ba6102bb6102bc6102bd6102be6102bf6102c06102c16102c26102c36102c46102c"
        + "56102c66102c76102c86102c96102ca6102cb6102cc6102cd6102ce6102cf6102d06102d16102d26102d361"
        + "02d46102d56102d66102d76102d86102d96102da6102db6102dc6102dd6102de6102df6102e06102e16102e"
        + "26102e36102e46102e56102e66102e76102e86102e96102ea6102eb6102ec6102ed6102ee6102ef6102f061"
        + "02f16102f26102f36102f46102f56102f66102f76102f86102f96102fa6102fb6102fc6102fd6102fe6102f"
        + "f61030061030161030261030361030461030561030661030761030861030961030a61030b61030c61030d61"
        + "030e61030f61031061031161031261031361031461031561031661031761031861031961031a61031b61031"
        + "c61031d61031e61031f61032061032161032261032361032461032561032661032761032861032961032a61"
        + "032b61032c61032d61032e61032f61033061033161033261033361033461033561033661033761033861033"
        + "961033a61033b61033c61033d61033e61033f61034061034161034261034361034461034561034661034761"
        + "034861034961034a61034b61034c61034d61034e61034f61035061035161035261035361035461035561035"
        + "661035761035861035961035a61035b61035c61035d61035e61035f61036061036161036261036361036461"
        + "036561036661036761036861036961036a61036b61036c61036d61036e61036f61037061037161037261037"
        + "361037461037561037661037761037861037961037a61037b61037c61037d61037e61037f61038061038161"
        + "038261038361038461038561038661038761038861038961038a61038b61038c61038d61038e61038f61039"
        + "061039161039261039361039461039561039661039761039861039961039a61039b61039c61039d61039e61"
        + "039f6103a06103a16103a26103a36103a46103a56103a66103a76103a86103a96103aa6103ab6103ac6103a"
        + "d6103ae6103af6103b06103b16103b26103b36103b46103b56103b66103b76103b86103b96103ba6103bb61"
        + "03bc6103bd6103be6103bf6103c06103c16103c26103c36103c46103c56103c66103c76103c86103c96103c"
        + "a6103cb6103cc6103cd6103ce6103cf6103d06103d16103d26103d36103d46103d56103d66103d76103d861"
        + "03d96103da6103db6103dc6103dd6103de6103df6103e06103e16103e26103e36103e46103e56103e66103e"
        + "76103e86103e96103ea6103eb6103ec6103ed6103ee6103ef6103f06103f16103f26103f36103f46103f561"
        + "03f66103f76103f86103f96103fa6103fb6103fc6103fd6103fe6103ff6104005b5600a165627a7a7230582"
        + "0998f09cc267db91352a3d0a4ab60ea08fc306fa8bc6dd78dc324a06109dcf0420029";
    contractAddress = PublicMethed
        .deployContract(contractName, abi, code, "", maxFeeLimit, 0L, 100, null,
            testKeyForGrammarAddress, grammarAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    final String txid = PublicMethed
        .triggerContract(contractAddress, "testStackTooLarge()", "#", false, 0, maxFeeLimit,
            grammarAddress, testKeyForGrammarAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("infoById:" + infoById);
    Optional<Transaction> byId = PublicMethed.getTransactionById(txid, blockingStubFull);
    logger.info("getRet:" + byId.get().getRet(0));
    logger.info("getNumber:" + byId.get().getRet(0).getContractRet().getNumber());
    logger.info("getContractRetValue:" + byId.get().getRet(0).getContractRetValue());
    logger.info("getContractRet:" + byId.get().getRet(0).getContractRet());

    Assert.assertEquals(byId.get().getRet(0).getContractRet().getNumber(), STACK_TOO_LARGE_VALUE);
    Assert.assertEquals(byId.get().getRet(0).getContractRetValue(), STACK_TOO_LARGE_VALUE);
    Assert.assertEquals(byId.get().getRet(0).getContractRet(), contractResult.STACK_TOO_LARGE);

    Assert
        .assertEquals(ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray()), "");
    Assert.assertEquals(contractResult.STACK_TOO_LARGE, infoById.get().getReceipt().getResult());

  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethed.freedResource(grammarAddress, testKeyForGrammarAddress, testNetAccountAddress,
        blockingStubFull);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelFull1 != null) {
      channelFull1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

}
