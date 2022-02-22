package stest.tron.wallet.dailybuild.tvmnewcommand.zenProofCommand;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class VerifyMintProof001 {

  private final String foundationKey001 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] foundationAddress001 = PublicMethed.getFinalAddress(foundationKey001);
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private byte[] contractAddress = null;

  private ECKey ecKey1 = new ECKey(Utils.getRandom());
  private byte[] testAddress001 = ecKey1.getAddress();
  private String testPriKey001 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {

    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    PublicMethed.printAddress(testPriKey001);
    Assert.assertTrue(PublicMethed.sendcoin(testAddress001, 1000_000_000L, foundationAddress001,
        foundationKey001, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true, description = "Deploy VerfyMintProof contract ")
  public void verifyMintProofTest001() {

    //before deploy, check account resource
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(testAddress001,
        blockingStubFull);
    Protocol.Account info = PublicMethed.queryAccount(testPriKey001, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = accountResource.getEnergyUsed();
    Long beforeNetUsed = accountResource.getNetUsed();
    Long beforeFreeNetUsed = accountResource.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);

    String filePath = "./src/test/resources/soliditycode/VerifyMintProof001.sol";
    String contractName = "VerifyMintProof001Test";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    String txid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "",
            maxFeeLimit, 0L, 0, 10000,
            "0", 0, null, testPriKey001,
            testAddress001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = null;
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);

    Assert.assertEquals(0, infoById.get().getResultValue());

    contractAddress = infoById.get().getContractAddress().toByteArray();
    SmartContract smartContract = PublicMethed.getContract(contractAddress,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());

  }

  @Test(enabled = true, description = "data length != 1504")
  public void verifyMintProofTest002() {

    String argsStr = "\""
        + "a634da705bbacb108a276ce26470568873d573e6f1f00d3a2b2e93b93f4b1a0c"
        + "2eb2b8ae07c858dafd7d99f4487a779878b1f87fb632c7fccff14d44c0b23e56"
        + "61ba88273d52c44cf4e1939ce6e76b97ef2611ce4cf472c5e8a61e66463f948d"
        + "8ffed5e9e6125a292dcb2f2855a753893467176b19ed366b3fc7c182e5b62cc1"
        + "d01bb22cba6ca8a514f36c5f24e6dcaf953f77db33c5e6db4f2a756b2e4793b7"
        + "be6e29b29309c37b9a1a5fe1e6ad42b1ed17c6d84d0fb4ed39772dceb5af6d23"
        + "01ed5d94ce6b69efc2bbe863f7798d871ae5bfc3db4eb36073fd9b8eb08d6c0c"
        + "52439f429ee437454cd59b8068ec9350b611f9b41cf5fa840c911227a2db3546"
        + "f0d190023a929d821aaf0529066bd81eac321ad0c9cf98c4a39060d636140a99"

        + "2ac86687e4c5284a8272390684e557d9a70bcd8dbaec6b8c8cb6114b13e01f22"
        + "c1dd79631dc9bd508f87d77bae4bebf31917c981d1ed1f8d8d9e637a7e56db0b"

        + "0000000000000000000000000000000000000000000000000000000000000064"

        + "33e4e8db7e8d3c127620de9901e7c6e65ca675b1c69455784a98aa7e4ed31a91"

        + "0000000000000000000000000000000000000000000000000000000000000000"
        + "0000000000000000000000000000000000000000000000000000000000000000"
        + "0000000000000000000000000000000000000000000000000000000000000000"
        + "0000000000000000000000000000000000000000000000000000000000000000"
        + "0000000000000000000000000000000000000000000000000000000000000000"
        + "0000000000000000000000000000000000000000000000000000000000000000"
        + "0000000000000000000000000000000000000000000000000000000000000000"
        + "0000000000000000000000000000000000000000000000000000000000000000"
        + "0000000000000000000000000000000000000000000000000000000000000000"
        + "0000000000000000000000000000000000000000000000000000000000000000"
        + "0000000000000000000000000000000000000000000000000000000000000000"
        + "0000000000000000000000000000000000000000000000000000000000000000"
        + "0000000000000000000000000000000000000000000000000000000000000000"
        + "0000000000000000000000000000000000000000000000000000000000000000"
        + "0000000000000000000000000000000000000000000000000000000000000000"
        + "0000000000000000000000000000000000000000000000000000000000000000"
        + "0000000000000000000000000000000000000000000000000000000000000000"
        + "0000000000000000000000000000000000000000000000000000000000000000"
        + "0000000000000000000000000000000000000000000000000000000000000000"
        + "0000000000000000000000000000000000000000000000000000000000000000"
        + "0000000000000000000000000000000000000000000000000000000000000000"
        + "0000000000000000000000000000000000000000000000000000000000000000"
        + "0000000000000000000000000000000000000000000000000000000000000000"
        + "0000000000000000000000000000000000000000000000000000000000000000"
        + "0000000000000000000000000000000000000000000000000000000000000000"
        + "0000000000000000000000000000000000000000000000000000000000000000"
        + "0000000000000000000000000000000000000000000000000000000000000000"
        + "0000000000000000000000000000000000000000000000000000000000000000"
        + "0000000000000000000000000000000000000000000000000000000000000000"
        + "0000000000000000000000000000000000000000000000000000000000000000"
        + "0000000000000000000000000000000000000000000000000000000000000000"
        + "0000000000000000000000000000000000000000000000000000000000000000"
        + "0000000000000000000000000000000000000000000000000000000000000000"

        + "0000000000000000000000000000000000000000000000000000000000000000"
        // add more bytes32
        + "0000000000000000000000000000000000000000000000000000000000000064"
        + "\"";

    String methedStr = "VerifyMintProofSize002(bytes)";

    String TriggerTxid = PublicMethed.triggerContract(contractAddress, methedStr, argsStr, false,
        0, maxFeeLimit, testAddress001, testPriKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info("TriggerTxid: " + TriggerTxid);

    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("infoById : " + infoById);

    Assert.assertEquals(0, infoById.get().getResultValue());
    String contractResult = ByteArray
        .toHexString(infoById.get().getContractResult(0).toByteArray());

    Assert.assertEquals(""
        + "0000000000000000000000000000000000000000000000000000000000000001"  // 1 : true
        + "0000000000000000000000000000000000000000000000000000000000000040"
        + "0000000000000000000000000000000000000000000000000000000000000020"
        + "0000000000000000000000000000000000000000000000000000000000000000", contractResult);


  }

  @Test(enabled = true, description = "data is empty")
  public void verifyMintProofTest003() {
    String methedStr = "VerifyMintProofSize003()";

    String TriggerTxid = PublicMethed.triggerContract(contractAddress, methedStr, "", false,
        0, maxFeeLimit, testAddress001, testPriKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info("TriggerTxid: " + TriggerTxid);

    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("infoById : " + infoById);

    Assert.assertEquals(0, infoById.get().getResultValue());
    String contractResult = ByteArray
        .toHexString(infoById.get().getContractResult(0).toByteArray());

    Assert.assertEquals(""
        + "0000000000000000000000000000000000000000000000000000000000000001"  // 1 : true
        + "0000000000000000000000000000000000000000000000000000000000000040"
        + "0000000000000000000000000000000000000000000000000000000000000020"
        + "0000000000000000000000000000000000000000000000000000000000000000", contractResult);

  }

  @Test(enabled = true, description = "leafCount greate than 2^32-1")
  public void verifyMintProofTest004() {

    String methedStr = "VerifyMintProofSize002(bytes)";

    String argsStr = "\""
        + "b55a21aaee0ce8f1c8ffaa0dbd23105cb55a21aaee0ce8f1c8ffaa0dbd23105a"
        + "b55a21aaee0ce8f1c8ffaa0dbd23105cb55a21aaee0ce8f1c8ffaa0dbd23105a"
        + "b55a21aaee0ce8f1c8ffaa0dbd23105cb55a21aaee0ce8f1c8ffaa0dbd23105a"
        + "b55a21aaee0ce8f1c8ffaa0dbd23105cb55a21aaee0ce8f1c8ffaa0dbd23105a"
        + "b55a21aaee0ce8f1c8ffaa0dbd23105cb55a21aaee0ce8f1c8ffaa0dbd23105a"
        + "b55a21aaee0ce8f1c8ffaa0dbd23105cb55a21aaee0ce8f1c8ffaa0dbd23105a"
        + "b55a21aaee0ce8f1c8ffaa0dbd23105cb55a21aaee0ce8f1c8ffaa0dbd23105a"
        + "b55a21aaee0ce8f1c8ffaa0dbd23105cb55a21aaee0ce8f1c8ffaa0dbd23105a"
        + "b55a21aaee0ce8f1c8ffaa0dbd23105cb55a21aaee0ce8f1c8ffaa0dbd23105a"

        + "b55a21aaee0ce8f1c8ffaa0dbd23105cb55a21aaee0ce8f1c8ffaa0dbd23105a"
        + "b55a21aaee0ce8f1c8ffaa0dbd23105cb55a21aaee0ce8f1c8ffaa0dbd23105a"

        + "000000000000000000000000000000000000000000000000000000000000000a"

        + "b55a21aaee0ce8f1c8ffaa0dbd23105cb55a21aaee0ce8f1c8ffaa0dbd23105a"

        + "b55a21aaee0ce8f1c8ffaa0dbd23105cb55a21aaee0ce8f1c8ffaa0dbd23105a"
        + "b55a21aaee0ce8f1c8ffaa0dbd23105cb55a21aaee0ce8f1c8ffaa0dbd23105a"
        + "b55a21aaee0ce8f1c8ffaa0dbd23105cb55a21aaee0ce8f1c8ffaa0dbd23105a"
        + "b55a21aaee0ce8f1c8ffaa0dbd23105cb55a21aaee0ce8f1c8ffaa0dbd23105a"
        + "b55a21aaee0ce8f1c8ffaa0dbd23105cb55a21aaee0ce8f1c8ffaa0dbd23105a"
        + "b55a21aaee0ce8f1c8ffaa0dbd23105cb55a21aaee0ce8f1c8ffaa0dbd23105a"
        + "b55a21aaee0ce8f1c8ffaa0dbd23105cb55a21aaee0ce8f1c8ffaa0dbd23105a"
        + "b55a21aaee0ce8f1c8ffaa0dbd23105cb55a21aaee0ce8f1c8ffaa0dbd23105a"
        + "b55a21aaee0ce8f1c8ffaa0dbd23105cb55a21aaee0ce8f1c8ffaa0dbd23105a"
        + "b55a21aaee0ce8f1c8ffaa0dbd23105cb55a21aaee0ce8f1c8ffaa0dbd23105a"
        + "b55a21aaee0ce8f1c8ffaa0dbd23105cb55a21aaee0ce8f1c8ffaa0dbd23105a"
        + "b55a21aaee0ce8f1c8ffaa0dbd23105cb55a21aaee0ce8f1c8ffaa0dbd23105a"
        + "b55a21aaee0ce8f1c8ffaa0dbd23105cb55a21aaee0ce8f1c8ffaa0dbd23105a"
        + "b55a21aaee0ce8f1c8ffaa0dbd23105cb55a21aaee0ce8f1c8ffaa0dbd23105a"
        + "b55a21aaee0ce8f1c8ffaa0dbd23105cb55a21aaee0ce8f1c8ffaa0dbd23105a"
        + "b55a21aaee0ce8f1c8ffaa0dbd23105cb55a21aaee0ce8f1c8ffaa0dbd23105a"
        + "b55a21aaee0ce8f1c8ffaa0dbd23105cb55a21aaee0ce8f1c8ffaa0dbd23105a"
        + "b55a21aaee0ce8f1c8ffaa0dbd23105cb55a21aaee0ce8f1c8ffaa0dbd23105a"
        + "b55a21aaee0ce8f1c8ffaa0dbd23105cb55a21aaee0ce8f1c8ffaa0dbd23105a"
        + "b55a21aaee0ce8f1c8ffaa0dbd23105cb55a21aaee0ce8f1c8ffaa0dbd23105a"
        + "b55a21aaee0ce8f1c8ffaa0dbd23105cb55a21aaee0ce8f1c8ffaa0dbd23105a"
        + "b55a21aaee0ce8f1c8ffaa0dbd23105cb55a21aaee0ce8f1c8ffaa0dbd23105a"
        + "b55a21aaee0ce8f1c8ffaa0dbd23105cb55a21aaee0ce8f1c8ffaa0dbd23105a"
        + "b55a21aaee0ce8f1c8ffaa0dbd23105cb55a21aaee0ce8f1c8ffaa0dbd23105a"
        + "b55a21aaee0ce8f1c8ffaa0dbd23105cb55a21aaee0ce8f1c8ffaa0dbd23105a"
        + "b55a21aaee0ce8f1c8ffaa0dbd23105cb55a21aaee0ce8f1c8ffaa0dbd23105a"
        + "b55a21aaee0ce8f1c8ffaa0dbd23105cb55a21aaee0ce8f1c8ffaa0dbd23105a"
        + "b55a21aaee0ce8f1c8ffaa0dbd23105cb55a21aaee0ce8f1c8ffaa0dbd23105a"
        + "b55a21aaee0ce8f1c8ffaa0dbd23105cb55a21aaee0ce8f1c8ffaa0dbd23105a"
        + "b55a21aaee0ce8f1c8ffaa0dbd23105cb55a21aaee0ce8f1c8ffaa0dbd23105a"
        + "b55a21aaee0ce8f1c8ffaa0dbd23105cb55a21aaee0ce8f1c8ffaa0dbd23105a"
        + "b55a21aaee0ce8f1c8ffaa0dbd23105cb55a21aaee0ce8f1c8ffaa0dbd23105a"
        + "b55a21aaee0ce8f1c8ffaa0dbd23105cb55a21aaee0ce8f1c8ffaa0dbd23105a"

        + "0000000000000000000000000000000000000000000000000000000100000002"
        + "\"";

    String TriggerTxid = PublicMethed.triggerContract(contractAddress, methedStr, argsStr, false,
        0, maxFeeLimit, testAddress001, testPriKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info("TriggerTxid: " + TriggerTxid);

    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("infoById : " + infoById);

    Assert.assertEquals(0, infoById.get().getResultValue());
    String contractResult = ByteArray.toHexString(infoById.get()
        .getContractResult(0).toByteArray());

    Assert.assertEquals(""
        + ""  // 1 : true
        + "0000000000000000000000000000000000000000000000000000000000000001"
        + "0000000000000000000000000000000000000000000000000000000000000040"
        + "0000000000000000000000000000000000000000000000000000000000000020"
        + "0000000000000000000000000000000000000000000000000000000000000000", contractResult);
  }

  @Test(enabled = true, description = "verify success with address call")
  public void verifyMintProofTest005() {
    String argsStr =
        "\"a634da705bbacb108a276ce26470568873d573e6f1f00d3a2b2e93b93f4b1a0c"
            + "2eb2b8ae07c858dafd7d99f4487a779878b1f87fb632c7fccff14d44c0b23e56"
            + "61ba88273d52c44cf4e1939ce6e76b97ef2611ce4cf472c5e8a61e66463f948d"
            + "8ffed5e9e6125a292dcb2f2855a753893467176b19ed366b3fc7c182e5b62cc1"
            + "d01bb22cba6ca8a514f36c5f24e6dcaf953f77db33c5e6db4f2a756b2e4793b7"
            + "be6e29b29309c37b9a1a5fe1e6ad42b1ed17c6d84d0fb4ed39772dceb5af6d23"
            + "01ed5d94ce6b69efc2bbe863f7798d871ae5bfc3db4eb36073fd9b8eb08d6c0c"
            + "52439f429ee437454cd59b8068ec9350b611f9b41cf5fa840c911227a2db3546"
            + "f0d190023a929d821aaf0529066bd81eac321ad0c9cf98c4a39060d636140a99"

            + "2ac86687e4c5284a8272390684e557d9a70bcd8dbaec6b8c8cb6114b13e01f22"
            + "c1dd79631dc9bd508f87d77bae4bebf31917c981d1ed1f8d8d9e637a7e56db0b"

            + "0000000000000000000000000000000000000000000000000000000000000064"

            + "33e4e8db7e8d3c127620de9901e7c6e65ca675b1c69455784a98aa7e4ed31a91"

            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"

            + "0000000000000000000000000000000000000000000000000000000000000000\"";

    String methedStr = "VerifyMintProofSize002(bytes)";

    String TriggerTxid = PublicMethed.triggerContract(contractAddress, methedStr, argsStr, false,
        0, maxFeeLimit, testAddress001, testPriKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info("TriggerTxid: " + TriggerTxid);

    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("infoById : " + infoById);

    Assert.assertEquals(0, infoById.get().getResultValue());
    String contractResult = ByteArray.toHexString(infoById.get()
        .getContractResult(0).toByteArray());

    Assert.assertEquals(""
        + "0000000000000000000000000000000000000000000000000000000000000001"  // 1 : true
        + "0000000000000000000000000000000000000000000000000000000000000040"
        + "0000000000000000000000000000000000000000000000000000000000000060"
        + "0000000000000000000000000000000000000000000000000000000000000001"
        + "0000000000000000000000000000000000000000000000000000000000000000"
        + "39e261b362110781a20878cc19f480cb50df5e6b896ed9a1fea8b8a9a4239a17", contractResult);

  }

  @Test(enabled = true, description = "verify success with fuction call")
  public void verifyMintProofTest006() {
    String argsStr =
        "a634da705bbacb108a276ce26470568873d573e6f1f00d3a2b2e93b93f4b1a0c"
            + "2eb2b8ae07c858dafd7d99f4487a779878b1f87fb632c7fccff14d44c0b23e56"
            + "61ba88273d52c44cf4e1939ce6e76b97ef2611ce4cf472c5e8a61e66463f948d"
            + "8ffed5e9e6125a292dcb2f2855a753893467176b19ed366b3fc7c182e5b62cc1"
            + "d01bb22cba6ca8a514f36c5f24e6dcaf953f77db33c5e6db4f2a756b2e4793b7"
            + "be6e29b29309c37b9a1a5fe1e6ad42b1ed17c6d84d0fb4ed39772dceb5af6d23"
            + "01ed5d94ce6b69efc2bbe863f7798d871ae5bfc3db4eb36073fd9b8eb08d6c0c"
            + "52439f429ee437454cd59b8068ec9350b611f9b41cf5fa840c911227a2db3546"
            + "f0d190023a929d821aaf0529066bd81eac321ad0c9cf98c4a39060d636140a99"

            + "2ac86687e4c5284a8272390684e557d9a70bcd8dbaec6b8c8cb6114b13e01f22"
            + "c1dd79631dc9bd508f87d77bae4bebf31917c981d1ed1f8d8d9e637a7e56db0b"

            + "0000000000000000000000000000000000000000000000000000000000000064"

            + "33e4e8db7e8d3c127620de9901e7c6e65ca675b1c69455784a98aa7e4ed31a91"

            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000"

            + "0000000000000000000000000000000000000000000000000000000000000000";

    String methedStr =
        "VerifyMintProofSize001(bytes32[9],bytes32[2],uint64,bytes32,bytes32[33],uint256)";

    String TriggerTxid = PublicMethed.triggerContract(contractAddress, methedStr, argsStr, true,
        0, maxFeeLimit, testAddress001, testPriKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info("TriggerTxid: " + TriggerTxid);

    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("infoById : " + infoById);

    Assert.assertEquals(0, infoById.get().getResultValue());
    String contractResult = ByteArray.toHexString(infoById.get()
        .getContractResult(0).toByteArray());

    Assert.assertTrue(contractResult.length() > 1000);

  }


}
