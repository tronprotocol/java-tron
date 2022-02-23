package stest.tron.wallet.dailybuild.tvmnewcommand.istanbul;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter;
import stest.tron.wallet.common.client.utils.PublicMethed;

public class Create2IstanbulTest001 {
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
    String filePath = "src/test/resources/soliditycode/create2Istanbul.sol";
    String contractName = "create2Istanbul";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    contractAddress = PublicMethed
      .deployContract(contractName, abi, code, "", maxFeeLimit, 0L, 100, null, testKey001,
        testAddress001, blockingStubFull);
  }

  /**
   * Create2 Algorithm Changed
   * Before: according to msg.sender`s Address, salt, bytecode to get create2 Address
   * After : according to contract`s Address, salt, bytecode to get create2 Address
   * The calculated Create2 address should be same as get(bytes1,bytes,uint256)
   */

  @Test(enabled = true, description = "create2 Algorithm Change")
  public void create2IstanbulTest001() {
    String filePath = "src/test/resources/soliditycode/create2Istanbul.sol";
    String contractName = "B";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();

    String methodStr = "deploy(bytes,uint256)";
    String argStr = "\"" + code + "\"," + "1";
    String txid = PublicMethed
        .triggerContract(contractAddress, methodStr, argStr, false, 0, maxFeeLimit,
        testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    TransactionInfo option = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull).get();
    String returnHex = ByteArray.toHexString(option.getContractResult(0).toByteArray());

    Assert.assertEquals(0,option.getResultValue());

    String methodStr2 = "get(bytes1,bytes,uint256)";
    String argStr2 = "\"41\",\"" + code + "\"," + 1;
    TransactionExtention returns = PublicMethed
        .triggerConstantContractForExtention(contractAddress, methodStr2, argStr2,
        false, 0,
        maxFeeLimit, "0", 0, testAddress001, testKey001, blockingStubFull);
    String getHex = ByteArray.toHexString(returns.getConstantResult(0).toByteArray());

    Assert.assertEquals(returnHex,getHex);

  }
}
