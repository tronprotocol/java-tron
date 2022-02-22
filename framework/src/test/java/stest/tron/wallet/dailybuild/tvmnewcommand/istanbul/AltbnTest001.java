package stest.tron.wallet.dailybuild.tvmnewcommand.istanbul;

import static org.tron.protos.Protocol.Transaction.Result.contractResult.OUT_OF_TIME;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class AltbnTest001 {
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
    PublicMethed.printAddress(testKey001);
    channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true).build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    PublicMethed
        .sendcoin(testAddress001, 1000_000_000L, testFoundationAddress, testFoundationKey,
        blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String filePath = "src/test/resources/soliditycode/altbn.sol";
    String contractName = "AltBn128";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    contractAddress = PublicMethed
        .deployContract(contractName, abi, code, "", maxFeeLimit, 0L, 100, null, testKey001,
        testAddress001, blockingStubFull);
  }

  @Test(enabled = true, description = "bn256add energyCost reduced from 500 to 150")
  public void bn256addTest001() {

    String methodStr = "callBn256Add(bytes32,bytes32,bytes32,bytes32)";
    String data = ""
        + "\"0000000000000000000000000000000000000000000000000000000000000001\","
        + "\"0000000000000000000000000000000000000000000000000000000000000002\","
        + "\"0000000000000000000000000000000000000000000000000000000000000001\","
        + "\"0000000000000000000000000000000000000000000000000000000000000002\"";

    logger.info("data: "  + data);
    String txid = PublicMethed
        .triggerContract(contractAddress, methodStr, data, false, 0, maxFeeLimit,
        testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    TransactionInfo option = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull).get();

    long energyCost = option.getReceipt().getEnergyUsageTotal();
    logger.info("energyCost: " + energyCost);

    Assert.assertEquals(0,option.getResultValue());
  }

  @Test(enabled = true, description = "bn256add energyCost reduced from 40000 to 6000")
  public void bn256ScalarMulTest001() {
    String methodStr = "callBn256ScalarMul(bytes32,bytes32,bytes32)";
    String data = ""
        + "\"0000000000000000000000000000000000000000000000000000000000000001\","
        + "\"0000000000000000000000000000000000000000000000000000000000000002\","
        + "\"0000000000000000000000000000000000000000000000000000000000000001\"";

    logger.info("data: "  + data);
    String txid = PublicMethed
        .triggerContract(contractAddress, methodStr, data, false, 0, maxFeeLimit,
        testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    TransactionInfo option = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull).get();

    long energyCost = option.getReceipt().getEnergyUsageTotal();
    logger.info("energyCost: " + energyCost);

    Assert.assertEquals(0,option.getResultValue());
    Assert.assertTrue(energyCost < 40000L);
    Assert.assertTrue(energyCost > 6000L);
  }

  @Test(enabled = true, description = "bn256add energyCost reduced from ( 80000 * pairNum + 100000)"
      + "to ( 34000 * pairNum + 45000) ")
  public void bn256paringTest001() {
    String methodStr = "callBn256Pairing(bytes)";
    String data = ""
          + "0000000000000000000000000000000000000000000000000000000000000020"
          + "0000000000000000000000000000000000000000000000000000000000000180"
          + "1c76476f4def4bb94541d57ebba1193381ffa7aa76ada664dd31c16024c43f59"
          + "3034dd2920f673e204fee2811c678745fc819b55d3e9d294e45c9b03a76aef41"
          + "209dd15ebff5d46c4bd888e51a93cf99a7329636c63514396b4a452003a35bf7"
          + "04bf11ca01483bfa8b34b43561848d28905960114c8ac04049af4b6315a41678"
          + "2bb8324af6cfc93537a2ad1a445cfd0ca2a71acd7ac41fadbf933c2a51be344d"
          + "120a2a4cf30c1bf9845f20c6fe39e07ea2cce61f0c9bb048165fe5e4de877550"
          + "111e129f1cf1097710d41c4ac70fcdfa5ba2023c6ff1cbeac322de49d1b6df7c"
          + "2032c61a830e3c17286de9462bf242fca2883585b93870a73853face6a6bf411"
          + "198e9393920d483a7260bfb731fb5d25f1aa493335a9e71297e485b7aef312c2"
          + "1800deef121f1e76426a00665e5c4479674322d4f75edadd46debd5cd992f6ed"
          + "090689d0585ff075ec9e99ad690c3395bc4b313370b38ef355acdadcd122975b"
          + "12c85ea5db8c6deb4aab71808dcb408fe3d1e7690c43d37b4ce6cc0166fa7daa";

    logger.info("data: "  + data);
    String txid = PublicMethed
        .triggerContract(contractAddress, methodStr, data, true, 0, maxFeeLimit,
        testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    TransactionInfo option = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull).get();

    long energyCost = option.getReceipt().getEnergyUsageTotal();
    logger.info("energyCost: " + energyCost);
    if (option.getResultValue() == 1) {
      Assert.assertEquals(option.getReceipt().getResult(), OUT_OF_TIME);
      return;
    }

    Assert.assertEquals(0,option.getResultValue());
    Assert.assertTrue(energyCost < 80000L * 2 + 100000L);
    Assert.assertTrue(energyCost > 34000L * 2 + 45000L);
  }

}
