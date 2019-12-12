package stest.tron.wallet.contract.scenario;

import static org.tron.protos.Protocol.Transaction.Result.contractResult.BAD_JUMP_DESTINATION_VALUE;
import static org.tron.protos.Protocol.Transaction.Result.contractResult.OUT_OF_ENERGY_VALUE;

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
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Result.contractResult;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class ContractScenario016 {

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
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private String compilerVersion = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.solidityCompilerVersion");

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
    PublicMethed.printAddress(testKeyForGrammarAddress);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext(true)
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);
  }

  @Test(enabled = true, description = "ContractResult is BAD_JUMP_DESTINATION")
  public void test1Grammar001() {
    Assert.assertTrue(PublicMethed
        .sendcoin(grammarAddress, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
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

    byte[] contractAddress = PublicMethed.deployContract(contractName, abi, code,
        "", maxFeeLimit,
        0L, 100, null, testKeyForGrammarAddress, grammarAddress, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContract smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);
    org.testng.Assert.assertTrue(smartContract.getAbi().toString() != null);
    String txid = null;
    Optional<TransactionInfo> infoById = null;
    txid = PublicMethed.triggerContract(contractAddress,
        "testBadJumpDestination()", "#", false,
        0, maxFeeLimit, grammarAddress, testKeyForGrammarAddress, blockingStubFull);
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
  public void test2Grammar002() {

    String filePath = "src/test/resources/soliditycode/contractUnknownException.sol";
    String contractName = "testC";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    String txid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            20L, 100, null, testKeyForGrammarAddress,
            grammarAddress, blockingStubFull);
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

    Assert.assertEquals(byId.get().getRet(0).getContractRetValue(), OUT_OF_ENERGY_VALUE);
    Assert.assertEquals(byId.get().getRet(0).getContractRet(), contractResult.OUT_OF_ENERGY);

    Assert
        .assertEquals(ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray()), "");
    Assert
        .assertEquals(contractResult.OUT_OF_ENERGY, infoById.get().getReceipt().getResult());

    Assert.assertEquals(byId.get().getRet(0).getRet().getNumber(), 0);
    Assert.assertEquals(byId.get().getRet(0).getRetValue(), 0);

  }


  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelFull1 != null) {
      channelFull1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

}
