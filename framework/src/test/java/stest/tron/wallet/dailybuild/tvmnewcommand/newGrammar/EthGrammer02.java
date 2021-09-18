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
import stest.tron.wallet.common.client.utils.PublicMethed;



@Slf4j
public class EthGrammer02 {

  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethed.getFinalAddress(testNetAccountKey);
  byte[] contractD = null;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contractExcAddress = ecKey1.getAddress();
  String contractExcKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);

  int salt = 11;

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

    String filePath = "src/test/resources/soliditycode/EthGrammer02.sol";
    String contractName = "D";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    contractD = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        500000000L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContractOuterClass.SmartContract smartContract = PublicMethed.getContract(contractD,
        blockingStubFull);
    Assert.assertEquals(1, smartContract.getVersion());
    Assert.assertNotNull(smartContract.getAbi());
  }

  @Test(enabled = true, description = "can not deploy contract with bytecode ef")
  public void test16forbiddenBytecodeStartWithEf() {
    String code = "60ef60005360016000f3";
    String abi = "[{\"inputs\":[],\"stateMutability\":\"payable\",\"type\":\"constructor\"}]";
    String txid = PublicMethed.deployContractAndGetTransactionInfoById("test",
        abi, code, "", maxFeeLimit,
        500000000L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("info: " + info.get().toString());
    Assert.assertEquals(1, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.INVALID_CODE,
        info.get().getReceipt().getResult());
    Assert.assertEquals("invalid code: must not begin with 0xef".toLowerCase(),
        ByteArray.toStr(info.get().getResMessage().toByteArray()).toLowerCase());
  }

  @Test(enabled = true, description = "can not deploy contract with bytecode ef00")
  public void test17forbiddenBytecodeStartWithEf() {
    String code = "60ef60005360026000f3";
    String abi = "[{\"inputs\":[],\"stateMutability\":\"payable\",\"type\":\"constructor\"}]";
    String txid = PublicMethed.deployContractAndGetTransactionInfoById("test",
        abi, code, "", maxFeeLimit, 500000000L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("info: " + info.get().toString());
    Assert.assertEquals(1, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.INVALID_CODE,
        info.get().getReceipt().getResult());
    Assert.assertEquals("invalid code: must not begin with 0xef".toLowerCase(),
        ByteArray.toStr(info.get().getResMessage().toByteArray()).toLowerCase());
  }

  @Test(enabled = true, description = "can not deploy contract with bytecode ef0000")
  public void test18forbiddenBytecodeStartWithEf() {
    String code = "60ef60005360036000f3";
    String abi = "[{\"inputs\":[],\"stateMutability\":\"payable\",\"type\":\"constructor\"}]";
    String txid = PublicMethed.deployContractAndGetTransactionInfoById("test", abi,
        code, "", maxFeeLimit, 500000000L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("info: " + info.get().toString());
    Assert.assertEquals(1, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.INVALID_CODE,
        info.get().getReceipt().getResult());
    Assert.assertEquals("invalid code: must not begin with 0xef".toLowerCase(),
        ByteArray.toStr(info.get().getResMessage().toByteArray()).toLowerCase());
  }

  @Test(enabled = true, description = "can not deploy contract with bytecode"
      + " ef00000000000000000000000000000000000000000000000000000000000000")
  public void test19forbiddenBytecodeStartWithEf() {
    String code = "60ef60005360206000f3";
    String abi = "[{\"inputs\":[],\"stateMutability\":\"payable\",\"type\":\"constructor\"}]";
    String txid = PublicMethed.deployContractAndGetTransactionInfoById("test", abi,
        code, "", maxFeeLimit, 500000000L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("info: " + info.get().toString());
    Assert.assertEquals(1, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.INVALID_CODE,
        info.get().getReceipt().getResult());
    Assert.assertEquals("invalid code: must not begin with 0xef".toLowerCase(),
        ByteArray.toStr(info.get().getResMessage().toByteArray()).toLowerCase());
  }

  @Test(enabled = true, description = "can deploy contract with bytecode fe")
  public void test20forbiddenBytecodeStartWithEf() {
    String code = "60fe60005360016000f3";
    String abi = "[{\"inputs\":[],\"stateMutability\":\"payable\",\"type\":\"constructor\"}]";
    String txid = PublicMethed.deployContractAndGetTransactionInfoById("test", abi,
        code, "", maxFeeLimit, 500000000L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("info: " + info.get().toString());
    Assert.assertEquals(0, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS,
        info.get().getReceipt().getResult());
  }

  @Test(enabled = true, description = "can not deploy contract by create with bytecode ef")
  public void test21forbiddenBytecodeStartWithEf() {
    String methedStr = "createDeployEf(bytes)";
    String argsStr = "\"0x60ef60005360016000f3\"";
    String txid = PublicMethed.triggerContract(contractD, methedStr, argsStr,
        false, 0, maxFeeLimit, contractExcAddress,
        contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(1, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.REVERT,
        info.get().getReceipt().getResult());
  }

  @Test(enabled = true, description = "can not deploy contract by create with bytecode ef00")
  public void test22forbiddenBytecodeStartWithEf() {
    String methedStr = "createDeployEf(bytes)";
    String argsStr = "\"0x60ef60005360026000f3\"";
    String txid = PublicMethed.triggerContract(contractD, methedStr, argsStr,
        false, 0, maxFeeLimit, contractExcAddress,
        contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(1, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.REVERT,
        info.get().getReceipt().getResult());
  }

  @Test(enabled = true, description = "can not deploy contract by create with bytecode ef0000")
  public void test23forbiddenBytecodeStartWithEf() {
    String methedStr = "createDeployEf(bytes)";
    String argsStr = "\"0x60ef60005360036000f3\"";
    String txid = PublicMethed.triggerContract(contractD, methedStr, argsStr,
        false, 0, maxFeeLimit, contractExcAddress,
        contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(1, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.REVERT,
        info.get().getReceipt().getResult());
  }

  @Test(enabled = true, description = "can not deploy contract by create with bytecode "
      + "ef00000000000000000000000000000000000000000000000000000000000000")
  public void test24forbiddenBytecodeStartWithEf() {
    String methedStr = "createDeployEf(bytes)";
    String argsStr = "\"0x60ef60005360206000f3\"";
    String txid = PublicMethed.triggerContract(contractD, methedStr, argsStr,
        false, 0, maxFeeLimit, contractExcAddress,
        contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(1, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.REVERT,
        info.get().getReceipt().getResult());
  }

  @Test(enabled = true, description = "can deploy contract by create with bytecode fe")
  public void test25forbiddenBytecodeStartWithEf() {
    String methedStr = "createDeployEf(bytes)";
    String argsStr = "\"0x60fe60005360016000f3\"";
    String txid = PublicMethed.triggerContract(contractD, methedStr, argsStr,
        false, 0, maxFeeLimit, contractExcAddress,
        contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS,
        info.get().getReceipt().getResult());
  }

  @Test(enabled = true, description = "can not deploy contract by create2 with bytecode ef")
  public void test26forbiddenBytecodeStartWithEf() {
    String methedStr = "create2DeployEf(bytes,uint256)";
    String argsStr = "\"0x60ef60005360016000f3\"," + salt;
    String txid = PublicMethed.triggerContract(contractD, methedStr, argsStr,
        false, 0, maxFeeLimit, contractExcAddress,
        contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(1, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.REVERT,
        info.get().getReceipt().getResult());
  }

  @Test(enabled = true, description = "can not deploy contract by create2 with bytecode ef00")
  public void test27forbiddenBytecodeStartWithEf() {
    salt++;
    String methedStr = "create2DeployEf(bytes,uint256)";
    String argsStr = "\"0x60ef60005360026000f3\"," + salt;
    String txid = PublicMethed.triggerContract(contractD, methedStr, argsStr,
        false, 0, maxFeeLimit, contractExcAddress,
        contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(1, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.REVERT,
        info.get().getReceipt().getResult());
  }

  @Test(enabled = true, description = "can not deploy contract by create2 with bytecode ef0000")
  public void test28forbiddenBytecodeStartWithEf() {
    salt++;
    String methedStr = "create2DeployEf(bytes,uint256)";
    String argsStr = "\"0x60ef60005360036000f3\"," + salt;
    String txid = PublicMethed.triggerContract(contractD, methedStr, argsStr,
        false, 0, maxFeeLimit, contractExcAddress,
        contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(1, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.REVERT,
        info.get().getReceipt().getResult());
  }

  @Test(enabled = true, description = "can not deploy contract by create2 with bytecode "
      + "ef00000000000000000000000000000000000000000000000000000000000000")
  public void test29forbiddenBytecodeStartWithEf() {
    salt++;
    String methedStr = "create2DeployEf(bytes,uint256)";
    String argsStr = "\"0x60ef60005360206000f3\"," + salt;
    String txid = PublicMethed.triggerContract(contractD, methedStr, argsStr,
        false, 0, maxFeeLimit, contractExcAddress,
        contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(1, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.REVERT,
        info.get().getReceipt().getResult());
  }

  @Test(enabled = true, description = "can deploy contract by create2 with bytecode fe")
  public void test30forbiddenBytecodeStartWithEf() {
    salt++;
    String methedStr = "create2DeployEf(bytes,uint256)";
    String argsStr = "\"0x60fe60005360016000f3\"," + salt;
    String txid = PublicMethed.triggerContract(contractD, methedStr, argsStr,
        false, 0, maxFeeLimit, contractExcAddress,
        contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS,
        info.get().getReceipt().getResult());
  }

  @Test(enabled = true, description = "can not sendcoin to contract")
  public void test31forbiddenSendTrxToContract() {
    Assert.assertFalse(PublicMethed
        .sendcoin(contractD, 100_000_000L,
            testNetAccountAddress, testNetAccountKey, blockingStubFull));
  }

  @Test(enabled = true, description = "db key can use high 16 bytes,"
      + "0x6162630000000000000000000000000000000000000000000000000000000000")
  public void test32DbKeyUseHigh16Bytes() {
    String slot = "0x6162630000000000000000000000000000000000000000000000000000000000";
    long value = 121;
    String methedStr = "setSlot(bytes,uint256)";
    String argsStr = "\"" + slot + "\"," + value;
    String txid = PublicMethed.triggerContract(contractD, methedStr, argsStr,
        false, 0, maxFeeLimit, contractExcAddress,
        contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS,
        info.get().getReceipt().getResult());

    methedStr = "getSlot(bytes)";
    argsStr = "\"" + slot + "\"";
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractD,
            methedStr, argsStr, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    long result = ByteArray.toLong(transactionExtention.getConstantResult(0).toByteArray());
    logger.info("result: " + result);
    Assert.assertEquals(value, result);
  }

  @Test(enabled = true, description = "slot high 16bytes all f,"
      + "0xffffffffffffffffffffffffffffffff00000000000000000000000000000000")
  public void test33DbKeyUseHigh16Bytes() {
    String slot = "0xffffffffffffffffffffffffffffffff00000000000000000000000000000000";
    long value = 122;
    String methedStr = "setSlot(bytes,uint256)";
    String argsStr = "\"" + slot + "\"," + value;
    String txid = PublicMethed.triggerContract(contractD, methedStr, argsStr,
        false, 0, maxFeeLimit, contractExcAddress,
        contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS,
        info.get().getReceipt().getResult());

    methedStr = "getSlot(bytes)";
    argsStr = "\"" + slot + "\"";
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractD,
            methedStr, argsStr, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    long result = ByteArray.toLong(transactionExtention.getConstantResult(0).toByteArray());
    logger.info("result: " + result);
    Assert.assertEquals(value, result);
  }

  @Test(enabled = true, description = "slot high 16bytes 1,"
      + " 0x0000000000000000000000000000000100000000000000000000000000000000")
  public void test34DbKeyUseHigh16Bytes() {
    String slot = "0x0000000000000000000000000000000100000000000000000000000000000000";
    long value = 123;
    String methedStr = "setSlot(bytes,uint256)";
    String argsStr = "\"" + slot + "\"," + value;
    String txid = PublicMethed.triggerContract(contractD, methedStr, argsStr,
        false, 0, maxFeeLimit, contractExcAddress,
        contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS,
        info.get().getReceipt().getResult());

    methedStr = "getSlot(bytes)";
    argsStr = "\"" + slot + "\"";
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractD,
            methedStr, argsStr, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    long result = ByteArray.toLong(transactionExtention.getConstantResult(0).toByteArray());
    logger.info("result: " + result);
    Assert.assertEquals(value, result);
  }

  @Test(enabled = true, description = "slot high 16bytes all 0,low 16bytes 1."
      + " 0x0000000000000000000000000000000000000000000000000000000000000001")
  public void test35DbKeyUseHigh16Bytes() {
    String slot = "0x0000000000000000000000000000000000000000000000000000000000000001";
    long value = 124;
    String methedStr = "setSlot(bytes,uint256)";
    String argsStr = "\"" + slot + "\"," + value;
    String txid = PublicMethed.triggerContract(contractD, methedStr, argsStr,
        false, 0, maxFeeLimit, contractExcAddress,
        contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS,
        info.get().getReceipt().getResult());

    methedStr = "getSlot(bytes)";
    argsStr = "\"" + slot + "\"";
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractD,
            methedStr, argsStr, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    long result = ByteArray.toLong(transactionExtention.getConstantResult(0).toByteArray());
    logger.info("result: " + result);
    Assert.assertEquals(value, result);
  }

  @Test(enabled = true, description = "slot all 0,"
      + " 0x0000000000000000000000000000000000000000000000000000000000000000")
  public void test36DbKeyUseHigh16BytesAllBytes0() {
    String slot = "0x0000000000000000000000000000000000000000000000000000000000000000";
    long value = 125;
    String methedStr = "setSlot(bytes,uint256)";
    String argsStr = "\"" + slot + "\"," + value;
    String txid = PublicMethed.triggerContract(contractD, methedStr, argsStr,
        false, 0, maxFeeLimit, contractExcAddress,
        contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS,
        info.get().getReceipt().getResult());

    methedStr = "getSlot(bytes)";
    argsStr = "\"" + slot + "\"";
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractD,
            methedStr, argsStr, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    long result = ByteArray.toLong(transactionExtention.getConstantResult(0).toByteArray());
    logger.info("result: " + result);
    Assert.assertEquals(value, result);
  }

  @Test(enabled = true, description = "slot all f,"
      + " 0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff")
  public void test37DbKeyUseHigh16BytesAllBytesF() {
    String slot = "0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff";
    long value = 126;
    String methedStr = "setSlot(bytes,uint256)";
    String argsStr = "\"" + slot + "\"," + value;
    String txid = PublicMethed.triggerContract(contractD, methedStr, argsStr,
        false, 0, maxFeeLimit, contractExcAddress,
        contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info =
        PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, info.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS,
        info.get().getReceipt().getResult());

    methedStr = "getSlot(bytes)";
    argsStr = "\"" + slot + "\"";
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractD,
            methedStr, argsStr, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    long result = ByteArray.toLong(transactionExtention.getConstantResult(0).toByteArray());
    logger.info("result: " + result);
    Assert.assertEquals(value, result);
  }

  @Test(enabled = true, description = "TransactionExtention has logs and internal_transactions")
  public void test38ConstantLogEven() {
    salt++;
    String methedStr = "create2DeployEf(bytes,uint256)";
    String argsStr = "\"0x60fe60005360016000f3\"," + salt;
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractD,
            methedStr, argsStr, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals(1, transactionExtention.getLogsCount());
    Assert.assertEquals(1, transactionExtention.getInternalTransactionsCount());
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

