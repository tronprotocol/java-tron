package stest.tron.wallet.dailybuild.tvmnewcommand.zenProofCommand;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.Optional;
import jdk.nashorn.internal.runtime.options.Option;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.spongycastle.util.encoders.Hex;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ByteString;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class VerifyBurnProof001 {

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

    PublicMethed.printAddress(testPriKey001);
    Assert.assertTrue(PublicMethed.sendcoin(testAddress001, 1000_000_000L, foundationAddress001,
        foundationKey001, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true, description = "Deploy VerfyMintProof contract ")
  public void verifyBurnProof001() {


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

    String filePath = "./src/test/resources/soliditycode/VerifyBurnProof001.sol";
    String contractName = "VerifyBurnProof001Test";
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

  @Test(enabled = true, description = "data length != 512")
  public void verifyBurnProofTest002() {

    String argsStr = "\""
        + "c9cf924134dd8fbd11d3b245b00adf4797b48c42e001673e7c566ce229b8fdf6"
        + "24097774778540c2c4d5acbeffe333e1f595a1b731cbe10848e3d3a527ba4d1b"
        + "a079c66e70cae2225cd702a7c0977635755ad104a87f435634d4e5382ac2afc8"
        + "1c47919273d4861ad815855ba1b4db5f90cc7e922b65c930c291eddc6d49a6c4"
        + "90771325afc8e6e4a506f9dca0889dff75bcb4c46030702a33899b4d1e81122a"
        + "a236433cf4c8ff426c66446de2f375b08575c4a18802e19a5fa5500922f7d570"
        + "aac680208d05f9f2a9beaef0d9adede10e4a0242a3d1e048dd2a65034ef3f348"
        + "0c108652d93da2ed13a0720fce9dce3a01a25cfa898bbaa8730f3fa8bba4b8a9"
        + "7a609fd9f4d008a9334dea39acc838298c989ae0f31cbaa08e4b00342ba2c0b1"
        + "ba37ac7be8084e0aeb01045f121e87e9cc942ecdc3b5e52933b79aad6f005d8e"

        + "dfc2aabf584106dfb2f6d3eb7f4584f5f2d9cba8340b0f73ba5fab4a4a024db2"
        + "d00c5f0b3aba1f98cba6d1c9750591628daca165bac2de6fd694df833110ee01"

        + "0000000000000000000000000000000000000000000000000000000000000064"

        + "19389f87908cb5f1ede2a9fe0c3047d2ad5fce424d133bacb655ae1179a81084"
        + "102ce5ad22d815a64374da9e2207d722b1c9a3099b292eaea0862edc886ff70d"

        + "b85285dd55258a2fbd04cc6ef365677b286d728f73db42c06ecc0a5822a6334a"
        /// add more 32bytes
        + "0000000000000000000000000000000000000000000000000000000000000064\"";

    //argsStr = argsStr + "0000000000000000000000000000000000000000000000000000000000000064";

    String methedStr = "VerifyBurnProofSize002(bytes)";

    String TriggerTxid = PublicMethed.triggerContract(contractAddress, methedStr, argsStr, false,
        0, maxFeeLimit, testAddress001, testPriKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info("TriggerTxid: " + TriggerTxid);

    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(TriggerTxid,blockingStubFull);

    logger.info("infoById : " + infoById);

    Assert.assertEquals(0,infoById.get().getResultValue());
    String contractResult = ByteArray.toHexString(
        infoById.get().getContractResult(0).toByteArray());

    Assert.assertEquals(""
        + "0000000000000000000000000000000000000000000000000000000000000001"  // 1 : true
        + "0000000000000000000000000000000000000000000000000000000000000040"
        + "0000000000000000000000000000000000000000000000000000000000000020"
        + "0000000000000000000000000000000000000000000000000000000000000000",contractResult);


  }

  @Test(enabled = true, description = "data is empty")
  public void verifyBurnProofTest003() {

    String methedStr = "VerifyBurnProofSize003()";

    String TriggerTxid = PublicMethed.triggerContract(contractAddress, methedStr, "", false,
        0, maxFeeLimit, testAddress001, testPriKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info("TriggerTxid: " + TriggerTxid);

    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(TriggerTxid,blockingStubFull);

    logger.info("infoById : " + infoById);

    Assert.assertEquals(0,infoById.get().getResultValue());
    String contractResult = ByteArray.toHexString(
        infoById.get().getContractResult(0).toByteArray());

    Assert.assertEquals(""
        + "0000000000000000000000000000000000000000000000000000000000000001"  // 1 : true
        + "0000000000000000000000000000000000000000000000000000000000000040"
        + "0000000000000000000000000000000000000000000000000000000000000020"
        + "0000000000000000000000000000000000000000000000000000000000000000",contractResult);


  }

  @Test(enabled = true, description = "value greate than Long.MAX_VALUE")
  public void verifyBurnProofTest004() {

    //String methedStr = "VerifyBurnProofSize002(bytes)";
    String methedStr = "VerifyBurnProofSize001(bytes32[10],bytes32[2],uint64,bytes32[2],bytes32)";
    String argsStr = ""
        + "c9cf924134dd8fbd11d3b245b00adf4797b48c42e001673e7c566ce229b8fdf6"
        + "24097774778540c2c4d5acbeffe333e1f595a1b731cbe10848e3d3a527ba4d1b"
        + "a079c66e70cae2225cd702a7c0977635755ad104a87f435634d4e5382ac2afc8"
        + "1c47919273d4861ad815855ba1b4db5f90cc7e922b65c930c291eddc6d49a6c4"
        + "90771325afc8e6e4a506f9dca0889dff75bcb4c46030702a33899b4d1e81122a"
        + "a236433cf4c8ff426c66446de2f375b08575c4a18802e19a5fa5500922f7d570"
        + "aac680208d05f9f2a9beaef0d9adede10e4a0242a3d1e048dd2a65034ef3f348"
        + "0c108652d93da2ed13a0720fce9dce3a01a25cfa898bbaa8730f3fa8bba4b8a9"
        + "7a609fd9f4d008a9334dea39acc838298c989ae0f31cbaa08e4b00342ba2c0b1"
        + "ba37ac7be8084e0aeb01045f121e87e9cc942ecdc3b5e52933b79aad6f005d8e"

        + "dfc2aabf584106dfb2f6d3eb7f4584f5f2d9cba8340b0f73ba5fab4a4a024db2"
        + "d00c5f0b3aba1f98cba6d1c9750591628daca165bac2de6fd694df833110ee01"

        + "0000000000000000000000ffffffffffffffffffffffffffffffffffffffffff"

        + "19389f87908cb5f1ede2a9fe0c3047d2ad5fce424d133bacb655ae1179a81084"
        + "102ce5ad22d815a64374da9e2207d722b1c9a3099b292eaea0862edc886ff70d"

        + "b85285dd55258a2fbd04cc6ef365677b286d728f73db42c06ecc0a5822a6334a";

    String TriggerTxid = PublicMethed.triggerContract(contractAddress, methedStr, argsStr, true,
        0, maxFeeLimit, testAddress001, testPriKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info("TriggerTxid: " + TriggerTxid);

    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(TriggerTxid,blockingStubFull);

    logger.info("infoById : " + infoById);

    Assert.assertEquals(0,infoById.get().getResultValue());
    String contractResult = ByteArray.toHexString(
        infoById.get().getContractResult(0).toByteArray());

    // parseLong will return Long.MAX_VALUE and checkResult false

    Assert.assertEquals(""
        + "0000000000000000000000000000000000000000000000000000000000000000"  // 1 : true
        ,contractResult);
  }

  @Test(enabled = true, description = "verify success with address call")
  public void verifyBurnProofTest005() {
    String argsStr = "\""
        + "c9cf924134dd8fbd11d3b245b00adf4797b48c42e001673e7c566ce229b8fdf6"
        + "24097774778540c2c4d5acbeffe333e1f595a1b731cbe10848e3d3a527ba4d1b"
        + "a079c66e70cae2225cd702a7c0977635755ad104a87f435634d4e5382ac2afc8"
        + "1c47919273d4861ad815855ba1b4db5f90cc7e922b65c930c291eddc6d49a6c4"
        + "90771325afc8e6e4a506f9dca0889dff75bcb4c46030702a33899b4d1e81122a"
        + "a236433cf4c8ff426c66446de2f375b08575c4a18802e19a5fa5500922f7d570"
        + "aac680208d05f9f2a9beaef0d9adede10e4a0242a3d1e048dd2a65034ef3f348"
        + "0c108652d93da2ed13a0720fce9dce3a01a25cfa898bbaa8730f3fa8bba4b8a9"
        + "7a609fd9f4d008a9334dea39acc838298c989ae0f31cbaa08e4b00342ba2c0b1"
        + "ba37ac7be8084e0aeb01045f121e87e9cc942ecdc3b5e52933b79aad6f005d8e"

        + "dfc2aabf584106dfb2f6d3eb7f4584f5f2d9cba8340b0f73ba5fab4a4a024db2"
        + "d00c5f0b3aba1f98cba6d1c9750591628daca165bac2de6fd694df833110ee01"

        + "0000000000000000000000000000000000000000000000000000000000000064"

        + "19389f87908cb5f1ede2a9fe0c3047d2ad5fce424d133bacb655ae1179a81084"
        + "102ce5ad22d815a64374da9e2207d722b1c9a3099b292eaea0862edc886ff70d"

        + "b85285dd55258a2fbd04cc6ef365677b286d728f73db42c06ecc0a5822a6334a"
        + "\"";

    String methedStr = "VerifyBurnProofSize002(bytes)";

    String TriggerTxid = PublicMethed.triggerContract(contractAddress, methedStr, argsStr, false,
        0, maxFeeLimit, testAddress001, testPriKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info("TriggerTxid: " + TriggerTxid);

    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(TriggerTxid,blockingStubFull);

    logger.info("infoById : " + infoById);

    Assert.assertEquals(0,infoById.get().getResultValue());
    String contractResult = ByteArray.toHexString(
        infoById.get().getContractResult(0).toByteArray());

    Assert.assertEquals(""
        + "0000000000000000000000000000000000000000000000000000000000000001"  // 1 : true
        + "0000000000000000000000000000000000000000000000000000000000000040"
        + "0000000000000000000000000000000000000000000000000000000000000020"
        + "0000000000000000000000000000000000000000000000000000000000000001",contractResult);
  }

  @Test(enabled = true, description = "verify success with fuction call")
  public void verifyBurnProofTest006() {
    String argsStr = ""
        + "c9cf924134dd8fbd11d3b245b00adf4797b48c42e001673e7c566ce229b8fdf6"
        + "24097774778540c2c4d5acbeffe333e1f595a1b731cbe10848e3d3a527ba4d1b"
        + "a079c66e70cae2225cd702a7c0977635755ad104a87f435634d4e5382ac2afc8"
        + "1c47919273d4861ad815855ba1b4db5f90cc7e922b65c930c291eddc6d49a6c4"
        + "90771325afc8e6e4a506f9dca0889dff75bcb4c46030702a33899b4d1e81122a"
        + "a236433cf4c8ff426c66446de2f375b08575c4a18802e19a5fa5500922f7d570"
        + "aac680208d05f9f2a9beaef0d9adede10e4a0242a3d1e048dd2a65034ef3f348"
        + "0c108652d93da2ed13a0720fce9dce3a01a25cfa898bbaa8730f3fa8bba4b8a9"
        + "7a609fd9f4d008a9334dea39acc838298c989ae0f31cbaa08e4b00342ba2c0b1"
        + "ba37ac7be8084e0aeb01045f121e87e9cc942ecdc3b5e52933b79aad6f005d8e"

        + "dfc2aabf584106dfb2f6d3eb7f4584f5f2d9cba8340b0f73ba5fab4a4a024db2"
        + "d00c5f0b3aba1f98cba6d1c9750591628daca165bac2de6fd694df833110ee01"

        + "0000000000000000000000000000000000000000000000000000000000000064"

        + "19389f87908cb5f1ede2a9fe0c3047d2ad5fce424d133bacb655ae1179a81084"
        + "102ce5ad22d815a64374da9e2207d722b1c9a3099b292eaea0862edc886ff70d"

        + "b85285dd55258a2fbd04cc6ef365677b286d728f73db42c06ecc0a5822a6334a";

    String methedStr = "VerifyBurnProofSize001(bytes32[10],bytes32[2],uint64,bytes32[2],bytes32)";

    String TriggerTxid = PublicMethed.triggerContract(contractAddress, methedStr, argsStr, true,
        0, maxFeeLimit, testAddress001, testPriKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info("TriggerTxid: " + TriggerTxid);

    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(TriggerTxid,blockingStubFull);

    logger.info("infoById : " + infoById);

    Assert.assertEquals(0,infoById.get().getResultValue());
    String contractResult = ByteArray.toHexString(
        infoById.get().getContractResult(0).toByteArray());

    Assert.assertEquals(""
        + "0000000000000000000000000000000000000000000000000000000000000001"  // 1 : true
         ,contractResult);
  }


}
