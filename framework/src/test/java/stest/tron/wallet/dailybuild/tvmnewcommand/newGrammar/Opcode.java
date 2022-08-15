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
public class Opcode {

  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethed.getFinalAddress(testNetAccountKey);
  byte[] mapKeyContract = null;
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

    String filePath = "src/test/resources/soliditycode/opCode.sol";
    String contractName = "A";
    HashMap retMap = PublicMethed.getBycodeAbiNoOptimize(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    mapKeyContract = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContractOuterClass.SmartContract smartContract = PublicMethed.getContract(mapKeyContract,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());

  }


  @Test(enabled = true, description = "test opcode smod, used for int")
  public void test01Smod() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(mapKeyContract,
            "sssmod()", "#", true,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Protocol.Transaction transaction = transactionExtention.getTransaction();
    int trueRes = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    logger.info("truerRes: " + trueRes + "   message:" + transaction.getRet(0).getRet());
    Assert.assertEquals(1, trueRes);
  }

  @Test(enabled = true, description = "test opcode extcodecopy return contract bytecode")
  public void test02Extcodecopy() {
    String base58 = Base58.encode58Check(mapKeyContract);
    String txid = PublicMethed.triggerContract(mapKeyContract,
        "eextcodecopy(address)", "\"" + base58 + "\"", false,
        0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info("txid: " + txid + "\n" + infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());
    Assert.assertTrue(ByteArray.toHexString(
        infoById.get().getContractResult(0).toByteArray()).length() > 0);

  }

  @Test(enabled = true, description = "test opcode coinbase," 
      + "block.coinbase is sr address which produced the block")
  public void test03Coinbase() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(mapKeyContract,
            "cccoinbase()", "#", true,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Protocol.Transaction transaction = transactionExtention.getTransaction();
    String trueRes = ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray());
    logger.info("truerRes: " + trueRes + "   message:" + transaction.getRet(0).getRet());
    Assert.assertEquals("SUCESS", transaction.getRet(0).getRet().toString());
    Assert.assertTrue(trueRes.startsWith("00000000000000000000000" 
        + "0bafb56091591790e00aa05eaddcc7dc1474b5d4b")
        || trueRes.startsWith("0000000000000000000000000be88a918d74d0dfd71dc84bd4abf036d0562991"));

  }

  @Test(enabled = true, description = "test opcode difficulty,block.difficulty is always 0")
  public void test04Difficulty() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(mapKeyContract,
            "ddifficulty()", "#", true,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Protocol.Transaction transaction = transactionExtention.getTransaction();
    int trueRes = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    logger.info("truerRes: " + trueRes + "   message:" + transaction.getRet(0).getRet());
    Assert.assertEquals("SUCESS", transaction.getRet(0).getRet().toString());
    Assert.assertEquals(0, trueRes);

  }

  @Test(enabled = true, description = "test opcode gaslimit,block.gaslimit is always 0")
  public void test05Gaslimit() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(mapKeyContract,
            "gggaslimit()", "#", true,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Protocol.Transaction transaction = transactionExtention.getTransaction();
    int trueRes = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    logger.info("truerRes: " + trueRes + "   message:" + transaction.getRet(0).getRet());
    Assert.assertEquals("SUCESS", transaction.getRet(0).getRet().toString());
    Assert.assertEquals(0, trueRes);

  }

  @Test(enabled = true, description = "test opcode pc,return current position, " 
      + "ppppc() can refer to opCode.sol")
  public void test06Pc() {
    String code = "608060405260838060116000396000f3fe608060405234" 
        + "8015600f57600080fd5b506004361060285760003560e01c806" 
        + "36d3a027714602d575b600080fd5b60336045565b6040805191825251" 
        + "9081900360200190f35b60005890509056fea264697" 
        + "0667358221220fe03cbd3d2aae7454565f203b9abd76ce74cf0ac" 
        + "a079b151cf6b8e2bfda2d5c464736f6c634300060c0033";
    String abi = "[\n" 
        + "\t{\n" 
        + "\t\t\"inputs\": [],\n" 
        + "\t\t\"stateMutability\": \"payable\",\n" 
        + "\t\t\"type\": \"constructor\"\n" 
        + "\t},\n" 
        + "\t{\n" 
        + "\t\t\"inputs\": [],\n" 
        + "\t\t\"name\": \"ppppc\",\n" 
        + "\t\t\"outputs\": [\n" 
        + "\t\t\t{\n" 
        + "\t\t\t\t\"internalType\": \"uint256\",\n" 
        + "\t\t\t\t\"name\": \"a\",\n" 
        + "\t\t\t\t\"type\": \"uint256\"\n" 
        + "\t\t\t}\n" 
        + "\t\t],\n" 
        + "\t\t\"stateMutability\": \"nonpayable\",\n" 
        + "\t\t\"type\": \"function\"\n" 
        + "\t}\n" 
        + "]";
    byte[] temContract = PublicMethed.deployContract("A", abi, code, "", maxFeeLimit,
        0L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContractOuterClass.SmartContract smartContract = PublicMethed.getContract(temContract,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());

    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(temContract,
            "ppppc()", "#", true,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Protocol.Transaction transaction = transactionExtention.getTransaction();
    int trueRes = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    logger.info("truerRes: " + trueRes + "   message:" + transaction.getRet(0).getRet());
    Assert.assertEquals("SUCESS", transaction.getRet(0).getRet().toString());
    Assert.assertEquals(72, trueRes);

  }

  @Test(enabled = true, description = "test opcode msize,return size of memory, " 
      + "msize cannot be used if optimize is open, mmmsize() can refer to opCode.sol")
  public void test07Msize() {
    String code = "608060405260b5806100126000396000f3fe6080604052348015600f5760" 
        + "0080fd5b506004361060285760003560e01c8063bf1a725d14602d575b600080fd5b60"
        + "336047565b604051603e9190605c565b60405180910390f35b600059905090565b6056"
        + "816075565b82525050565b6000602082019050606f6000830184604f565b9291505056" 
        + "5b600081905091905056fea26469706673582212202252652aad4bca9a4aa9db179e03" 
        + "f7b3bf439f47152e31f45d8587b710bce79664736f6c63430008060033";
    String abi = "[\n" 
        + "\t{\n" 
        + "\t\t\"inputs\": [],\n" 
        + "\t\t\"stateMutability\": \"payable\",\n" 
        + "\t\t\"type\": \"constructor\"\n" 
        + "\t},\n"
        + "\t{\n" 
        + "\t\t\"inputs\": [],\n" 
        + "\t\t\"name\": \"mmmsize\",\n" 
        + "\t\t\"outputs\": [\n" 
        + "\t\t\t{\n" 
        + "\t\t\t\t\"internalType\": \"uint256\",\n" 
        + "\t\t\t\t\"name\": \"a\",\n" 
        + "\t\t\t\t\"type\": \"uint256\"\n"
        + "\t\t\t}\n"
        + "\t\t],\n" 
        + "\t\t\"stateMutability\": \"nonpayable\",\n" 
        + "\t\t\"type\": \"function\"\n" 
        + "\t}\n"
        + "]";
    byte[] temContract = PublicMethed.deployContract("A", abi, code, "", maxFeeLimit,
        0L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContractOuterClass.SmartContract smartContract = PublicMethed.getContract(temContract,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());

    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(temContract,
            "mmmsize()", "#", true,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Protocol.Transaction transaction = transactionExtention.getTransaction();
    int trueRes = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    logger.info("truerRes: " + trueRes + "   message:" + transaction.getRet(0).getRet());
    Assert.assertEquals("SUCESS", transaction.getRet(0).getRet().toString());
    Assert.assertEquals(96, trueRes);

  }


  @Test(enabled = true, description = "test opcode swap14-16,solidity cannot use optimize")
  public void test08Swap() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(mapKeyContract,
            "ssswap()", "#", true,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Protocol.Transaction transaction = transactionExtention.getTransaction();
    int trueRes = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    logger.info("truerRes: " + trueRes + "   message:" + transaction.getRet(0).getRet());
    Assert.assertEquals("SUCESS", transaction.getRet(0).getRet().toString());
    Assert.assertEquals(1, trueRes);

  }

  @Test(enabled = true, description = "test opcode push13-30 but exclude push20 and push29," 
      + "solidity cannot use optimize")
  public void test08Pushx() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(mapKeyContract,
            "pppushx()", "#", true,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Protocol.Transaction transaction = transactionExtention.getTransaction();
    String trueRes = ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray());
    logger.info("truerRes: " + trueRes + "   message:" + transaction.getRet(0).getRet());
    Assert.assertEquals("SUCESS", transaction.getRet(0).getRet().toString());
    Assert.assertTrue(trueRes.contains("000000000000000000000000000000000000001" 
        + "1223344556677889900112233"));
  }

  @Test(enabled = true, description = "test opcode callcode,difference with delegatecall " 
      + "is caller and callvalue,the bytecode is compiled with solidity 0.4.22, " 
      + "can refer to opCode.sol for code")
  public void test09Callcode() {
    String code = "60806040526103b4806100136000396000f3006080604052600436106100565763" 
        + "ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416634" 
        + "cb335db811461005b578063ae02d91d14610081578063ea856db2146100a5575b600080fd5b61007f" 
        + "73ffffffffffffffffffffffffffffffffffffffff600435166024356100c9565b005b61007f73ffff" 
        + "ffffffffffffffffffffffffffffffffffff600435166024356101b6565b61007f73ffffffffffffff"
        + "ffffffffffffffffffffffffff600435166024356102a3565b604080516024808201849052825180830" 
        + "39091018152604490910182526020810180517bffffffffffffffffffffffffffffffffffffffffffff"
        + "ffffffffffff167f66c99139000000000000000000000000000000000000000000000000000000001781529" 
        + "151815173ffffffffffffffffffffffffffffffffffffffff861693600a93929182919080838360005b8" 
        + "3811015610170578181015183820152602001610158565b50505050905090810190601f16801561019d5" 
        + "780820380516001836020036101000a031916815260200191505b5091505060006040518083038185875" 
        + "af1505050505050565b60408051602480820184905282518083039091018152604490910182526020810" 
        + "180517bffffffffffffffffffffffffffffffffffffffffffffffffffffffff167f66c99139000000000" 
        + "000000000000000000000000000000000000000000000001781529151815173fffffffffffffffffffff" 
        + "fffffffffffffffffff861693600a93929182919080838360005b8381101561025d57818101518382015" 
        + "2602001610245565b50505050905090810190601f16801561028a5780820380516001836020036101000" 
        + "a031916815260200191505b5091505060006040518083038185875af2505050505050565b60408051602" 
        + "480820184905282518083039091018152604490910182526020810180517bfffffffffffffffffffffff"
        + "fffffffffffffffffffffffffffffffff167f66c991390000000000000000000000000000000000000000" 
        + "00000000000000001781529151815173ffffffffffffffffffffffffffffffffffffffff8616938291808" 
        + "38360005b8381101561034457818101518382015260200161032c565b50505050905090810190601f1680" 
        + "156103715780820380516001836020036101000a031916815260200191505b50915050600060405180830" 
        + "381855af450505050505600a165627a7a72305820210d132d0c4006264ef113f342556c616d9e69acc20"
        + "bfa80cf440a4eac170de80029";
    String abi = "[\n" 
        + "\t{\n" 
        + "\t\t\"constant\": false,\n" 
        + "\t\t\"inputs\": [\n" 
        + "\t\t\t{\n" 
        + "\t\t\t\t\"name\": \"callCAddress\",\n" 
        + "\t\t\t\t\"type\": \"address\"\n" 
        + "\t\t\t},\n" 
        + "\t\t\t{\n" 
        + "\t\t\t\t\"name\": \"amount\",\n"
        + "\t\t\t\t\"type\": \"uint256\"\n" 
        + "\t\t\t}\n" 
        + "\t\t],\n" 
        + "\t\t\"name\": \"testInCall\",\n" 
        + "\t\t\"outputs\": [],\n" 
        + "\t\t\"payable\": true,\n" 
        + "\t\t\"stateMutability\": \"payable\",\n"
        + "\t\t\"type\": \"function\"\n" 
        + "\t},\n" 
        + "\t{\n"
        + "\t\t\"constant\": false,\n" 
        + "\t\t\"inputs\": [\n" 
        + "\t\t\t{\n" 
        + "\t\t\t\t\"name\": \"callCAddress\",\n" 
        + "\t\t\t\t\"type\": \"address\"\n" 
        + "\t\t\t},\n" 
        + "\t\t\t{\n" 
        + "\t\t\t\t\"name\": \"amount\",\n" 
        + "\t\t\t\t\"type\": \"uint256\"\n" 
        + "\t\t\t}\n" 
        + "\t\t],\n" 
        + "\t\t\"name\": \"testInCallcode\",\n" 
        + "\t\t\"outputs\": [],\n" 
        + "\t\t\"payable\": true,\n" 
        + "\t\t\"stateMutability\": \"payable\",\n" 
        + "\t\t\"type\": \"function\"\n" 
        + "\t},\n" 
        + "\t{\n" 
        + "\t\t\"constant\": false,\n" 
        + "\t\t\"inputs\": [\n" 
        + "\t\t\t{\n" 
        + "\t\t\t\t\"name\": \"callCAddress\",\n" 
        + "\t\t\t\t\"type\": \"address\"\n" 
        + "\t\t\t},\n" 
        + "\t\t\t{\n" 
        + "\t\t\t\t\"name\": \"amount\",\n" 
        + "\t\t\t\t\"type\": \"uint256\"\n" 
        + "\t\t\t}\n" 
        + "\t\t],\n" 
        + "\t\t\"name\": \"testIndelegateCall\",\n" 
        + "\t\t\"outputs\": [],\n" 
        + "\t\t\"payable\": true,\n" 
        + "\t\t\"stateMutability\": \"payable\",\n" 
        + "\t\t\"type\": \"function\"\n" 
        + "\t},\n" 
        + "\t{\n" 
        + "\t\t\"inputs\": [],\n" 
        + "\t\t\"payable\": true,\n" 
        + "\t\t\"stateMutability\": \"payable\",\n"
        + "\t\t\"type\": \"constructor\"\n" 
        + "\t}\n" 
        + "]";
    byte[] contractA = PublicMethed.deployContract("A", abi, code, "", maxFeeLimit,
        1000000L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContractOuterClass.SmartContract smartContract = PublicMethed.getContract(contractA,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());

    code = "608060405260d2806100126000396000f300608060405260043610603e5763ffffffff7c0" 
        + "10000000000000000000000000000000000000000000000000000000060003504166366c99" 
        + "13981146043575b600080fd5b604c600435604e565b005b6040805173fffffffffffffffff" 
        + "fffffffffffffffffffffff3316815234602082015280820183905290517fac74fdf75f0e5" 
        + "a43f870f7135801b44f404be82b1dcad73423c542b840d1d64b9181900360600190a150560" 
        + "0a165627a7a72305820c460a35f70e363777be22b3a4ace5f95533de626073ab4e06d9bf3bbb2cffceb0029";
    abi = "[\n"
        + "\t{\n"
        + "\t\t\"constant\": false,\n"
        + "\t\t\"inputs\": [\n"
        + "\t\t\t{\n"
        + "\t\t\t\t\"name\": \"amount\",\n"
        + "\t\t\t\t\"type\": \"uint256\"\n"
        + "\t\t\t}\n"
        + "\t\t],\n"
        + "\t\t\"name\": \"trans\",\n"
        + "\t\t\"outputs\": [],\n"
        + "\t\t\"payable\": true,\n"
        + "\t\t\"stateMutability\": \"payable\",\n"
        + "\t\t\"type\": \"function\"\n"
        + "\t},\n"
        + "\t{\n"
        + "\t\t\"inputs\": [],\n"
        + "\t\t\"payable\": true,\n"
        + "\t\t\"stateMutability\": \"payable\",\n"
        + "\t\t\"type\": \"constructor\"\n"
        + "\t},\n"
        + "\t{\n"
        + "\t\t\"anonymous\": false,\n"
        + "\t\t\"inputs\": [\n"
        + "\t\t\t{\n"
        + "\t\t\t\t\"indexed\": false,\n"
        + "\t\t\t\t\"name\": \"\",\n"
        + "\t\t\t\t\"type\": \"address\"\n"
        + "\t\t\t},\n"
        + "\t\t\t{\n"
        + "\t\t\t\t\"indexed\": false,\n"
        + "\t\t\t\t\"name\": \"\",\n"
        + "\t\t\t\t\"type\": \"uint256\"\n"
        + "\t\t\t},\n"
        + "\t\t\t{\n"
        + "\t\t\t\t\"indexed\": false,\n"
        + "\t\t\t\t\"name\": \"\",\n"
        + "\t\t\t\t\"type\": \"uint256\"\n"
        + "\t\t\t}\n"
        + "\t\t],\n"
        + "\t\t\"name\": \"clog\",\n"
        + "\t\t\"type\": \"event\"\n"
        + "\t}\n"
        + "]";
    byte[] contractC = PublicMethed.deployContract("C", abi, code, "", maxFeeLimit,
        0L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    smartContract = PublicMethed.getContract(contractC,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());

    String base58C = Base58.encode58Check(contractC);

    String txid = PublicMethed.triggerContract(contractA,
        "testInCall(address,uint256)", "\"" + base58C + "\",1", false,
        1, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());
    String logRes = ByteArray.toHexString(infoById.get().getLog(0).getData().toByteArray());
    System.out.println("000000:  " + logRes);
    String b = "41" + logRes.substring(24, 64);
    String c = logRes.substring(64, 128);
    String x = ByteArray.toHexString(contractA);
    Assert.assertEquals(b, x);
    Assert.assertEquals("000000000000000000000000000000000000000000000000000000000000000a", c);

    txid = PublicMethed.triggerContract(contractA,
        "testIndelegateCall(address,uint256)", "\"" + base58C + "\",1", false,
        2, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info("txid: " + txid + "\n" + infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());
    logRes = ByteArray.toHexString(infoById.get().getLog(0).getData().toByteArray());
    System.out.println("000000:  " + logRes);
    b = "41" + logRes.substring(24, 64);
    c = logRes.substring(64, 128);
    x = ByteArray.toHexString(contractExcAddress);
    Assert.assertEquals(b, x);
    Assert.assertEquals("0000000000000000000000000000000000000000000000000000000000000002", c);


    txid = PublicMethed.triggerContract(contractA,
        "testInCallcode(address,uint256)", "\"" + base58C + "\",1", false,
        3, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info("txid: " + txid + "\n" + infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());
    logRes = ByteArray.toHexString(infoById.get().getLog(0).getData().toByteArray());
    System.out.println("000000:  " + logRes);
    b = "41" + logRes.substring(24, 64);
    c = logRes.substring(64, 128);
    x = ByteArray.toHexString(contractA);
    Assert.assertEquals(b, x);
    Assert.assertEquals("000000000000000000000000000000000000000000000000000000000000000a", c);
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

