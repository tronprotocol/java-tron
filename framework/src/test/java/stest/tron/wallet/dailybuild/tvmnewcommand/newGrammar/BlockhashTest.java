package stest.tron.wallet.dailybuild.tvmnewcommand.newGrammar;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class BlockhashTest {
  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");

  private byte[] contractAddress = null;

  private ECKey ecKey1 = new ECKey(Utils.getRandom());
  private byte[] dev001Address = ecKey1.getAddress();
  private String dev001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {

    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    PublicMethed.printAddress(dev001Key);

    Assert.assertTrue(PublicMethed.sendcoin(dev001Address, 100_000_000L, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String filePath = "./src/test/resources/soliditycode/BlockHash.sol";
    String contractName = "TestBlockHash";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    contractAddress = PublicMethed
        .deployContract(contractName, abi, code, "",
            maxFeeLimit, 0L, 0, 10000,
            "0", 0, null, dev001Key,
            dev001Address, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true, description = "BlockHash should not be change after command OR")
  public void test01BlockHashWithOR() {
    String methodStr = "testOR1(bytes32)";
    String argStr = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff";
    String txid = PublicMethed.triggerContract(contractAddress, methodStr, argStr, true,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0,infoById.get().getResultValue());
    String ContractResult = ByteArray.toHexString(infoById.get()
        .getContractResult(0).toByteArray());
    // 3 bytes32
    Assert.assertEquals(192, ContractResult.length());
    // blockHash before OR should equals to blockHash after OR
    Assert.assertEquals(ContractResult.substring(0,64),ContractResult.substring(128));

    methodStr = "testOR2(bytes32)";
    txid = PublicMethed.triggerContract(contractAddress, methodStr, argStr, true,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0,infoById.get().getResultValue());
    ContractResult = ByteArray.toHexString(infoById.get()
        .getContractResult(0).toByteArray());
    // 3 bytes32
    Assert.assertEquals(192, ContractResult.length());
    // blockHash before OR should equals to blockHash after OR
    Assert.assertEquals(ContractResult.substring(0,64),ContractResult.substring(128));
  }

  @Test(enabled = true, description = "BlockHash should not be change after command AND")
  public void test02BlockHashWithAND() {
    String methodStr = "testAND1(bytes32)";
    String argStr = "0000000000000000000000000000000000000000000000000000000000000000";
    String txid = PublicMethed.triggerContract(contractAddress, methodStr, argStr, true,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0,infoById.get().getResultValue());
    String ContractResult = ByteArray.toHexString(infoById.get()
        .getContractResult(0).toByteArray());
    // 3 bytes32
    Assert.assertEquals(192, ContractResult.length());
    // blockHash before AND should equals to blockHash after AND
    Assert.assertEquals(ContractResult.substring(0,64),ContractResult.substring(128));

    methodStr = "testAND2(bytes32)";
    txid = PublicMethed.triggerContract(contractAddress, methodStr, argStr, true,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0,infoById.get().getResultValue());
    ContractResult = ByteArray.toHexString(infoById.get()
        .getContractResult(0).toByteArray());
    // 3 bytes32
    Assert.assertEquals(192, ContractResult.length());
    // blockHash before AND should equals to blockHash after AND
    Assert.assertEquals(ContractResult.substring(0,64),ContractResult.substring(128));
  }

  @Test(enabled = true, description = "BlockHash should not be change after command XOR")
  public void test03BlockHashWithXOR() {
    String methodStr = "testXOR1(bytes32)";
    String argStr = "00000000000000000000000000000000ffffffffffffffffffffffffffffffff";
    String txid = PublicMethed.triggerContract(contractAddress, methodStr, argStr, true,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0,infoById.get().getResultValue());
    String ContractResult = ByteArray.toHexString(infoById.get()
        .getContractResult(0).toByteArray());
    // 3 bytes32
    Assert.assertEquals(192, ContractResult.length());
    // blockHash before XOR should equals to blockHash after XOR
    Assert.assertEquals(ContractResult.substring(0,64),ContractResult.substring(128));

    methodStr = "testXOR2(bytes32)";
    txid = PublicMethed.triggerContract(contractAddress, methodStr, argStr, true,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0,infoById.get().getResultValue());
    ContractResult = ByteArray.toHexString(infoById.get()
        .getContractResult(0).toByteArray());
    // 3 bytes32
    Assert.assertEquals(192, ContractResult.length());
    // blockHash before XOR should equals to blockHash after XOR
    Assert.assertEquals(ContractResult.substring(0,64),ContractResult.substring(128));
  }

}
