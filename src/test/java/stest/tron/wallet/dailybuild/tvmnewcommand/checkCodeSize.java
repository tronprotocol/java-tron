package stest.tron.wallet.dailybuild.tvmnewcommand;

import com.google.protobuf.ByteString;
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
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class checkCodeSize {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");

  private static final long now = System.currentTimeMillis();
  private static String tokenName = "testAssetIssue_" + Long.toString(now);
  private static ByteString assetAccountId = null;
  private static final long TotalSupply = 1000L;
  private byte[] transferTokenContractAddress = null;
  private byte[] confirmContractAddress = null;

  private String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  private String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");

  private ECKey ecKey1 = new ECKey(Utils.getRandom());
  private byte[] dev001Address = ecKey1.getAddress();
  private String dev001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

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

    PublicMethed.printAddress(dev001Key);
  }

  @Test(enabled = false, description = "DeployContract with correct tokenValue and tokenId")
  public void deployTransferTokenContract() {
    Assert.assertTrue(PublicMethed.sendcoin(dev001Address, 1000_000_000L, fromAddress,
        testKey002, blockingStubFull));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    //before deploy, check account resource
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(dev001Address,
        blockingStubFull);
    long energyLimit = accountResource.getEnergyLimit();
    long energyUsage = accountResource.getEnergyUsed();
    long balanceBefore = PublicMethed.queryAccount(dev001Key, blockingStubFull).getBalance();

    logger.info("before energyLimit is " + Long.toString(energyLimit));
    logger.info("before energyUsage is " + Long.toString(energyUsage));
    logger.info("before balanceBefore is " + Long.toString(balanceBefore));

    String filePath = "./src/test/resources/soliditycode/demo.sol";
    String contractName = "tokenTest";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    transferTokenContractAddress = PublicMethed
        .deployContract(contractName, abi, code, "", maxFeeLimit,
            0L, 100, null, dev001Key,
            dev001Address, blockingStubFull);

    code = "6080604052303b600055610211806100186000396000f3006080604052600436106100565763ffffffff7c0100000000000000000000000000000000000000000000000000000000600035041663a5a23074811461005b578063dce4a4471461009c578063eb4dd8f214610159575b600080fd5b34801561006757600080fd5b50d3801561007457600080fd5b50d2801561008157600080fd5b5061008a6101a1565b60408051918252519081900360200190f35b3480156100a857600080fd5b50d380156100b557600080fd5b50d280156100c257600080fd5b506100e473ffffffffffffffffffffffffffffffffffffffff600435166101a7565b6040805160208082528351818301528351919283929083019185019080838360005b8381101561011e578181015183820152602001610106565b50505050905090810190601f16801561014b5780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b34801561016557600080fd5b50d3801561017257600080fd5b50d2801561017f57600080fd5b5061008a73ffffffffffffffffffffffffffffffffffffffff600435166101ce565b60005490565b60408051603f833b908101601f191682019092528181529080600060208401853c50919050565b6000813b823b15156101df57600080fd5b929150505600a165627a7a72305820309e6f0e0ad58fd7067f04813defe26c999d00f1975d75d8685fd398ec6ccb2e0029";
    abi = "[{\"constant\":false,\"inputs\":[],\"name\":\"getCodeSize\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"_addr\",\"type\":\"address\"}],\"name\":\"at\",\"outputs\":[{\"name\":\"o_code\",\"type\":\"bytes\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"addr\",\"type\":\"address\"}],\"name\":\"confirm\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"}]";
    contractName = "confirmTest";
    confirmContractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, dev001Key,
        dev001Address, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

  }

  @Test(enabled = false, description = "trigger")
  public void triggerConfirm() {
    long balanceBefore = PublicMethed.queryAccount(dev001Key, blockingStubFull).getBalance();
    logger.info("before balanceBefore is " + Long.toString(balanceBefore));

    String contractname = "pulsone()";

    String trxid = PublicMethed
        .triggerContract(transferTokenContractAddress, contractname, "#", false, 0L, maxFeeLimit,
            dev001Address, dev001Key, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(trxid, blockingStubFull);

    logger.info("TrxId = " + trxid);
    logger.info("Info = \n" + infoById);
    //logger.info("contractResult = " + ByteArray
    //    .toLong(infoById.get().getContractResult(0).toByteArray()));

    contractname = "getCodeSize()";

    trxid = PublicMethed
        .triggerContract(transferTokenContractAddress, contractname, "#", false, 0L, maxFeeLimit,
            dev001Address, dev001Key, blockingStubFull);
    infoById = null;
    infoById = PublicMethed.getTransactionInfoById(trxid, blockingStubFull);

    logger.info("TrxId = " + trxid);
    logger.info("Info = \n" + infoById);
    logger.info("contractResult = " + ByteArray
        .toLong(infoById.get().getContractResult(0).toByteArray()));

    contractname = "getCodeSize()";

    trxid = PublicMethed
        .triggerContract(confirmContractAddress, contractname, "#", false, 0L, maxFeeLimit,
            dev001Address, dev001Key, blockingStubFull);
    infoById = null;
    infoById = PublicMethed.getTransactionInfoById(trxid, blockingStubFull);

    logger.info("TrxId = " + trxid);
    logger.info("Info = \n" + infoById);
    logger.info("contractResult = " + ByteArray
        .toLong(infoById.get().getContractResult(0).toByteArray()));

    balanceBefore = PublicMethed.queryAccount(dev001Key, blockingStubFull).getBalance();
    logger.info("before balanceBefore is " + Long.toString(balanceBefore));

    String parm = "\"" + Base58.encode58Check(transferTokenContractAddress) + "\"";
    contractname = "confirm(address)";

    trxid = PublicMethed
        .triggerContract(confirmContractAddress, contractname, parm, false, 0L, maxFeeLimit,
            dev001Address, dev001Key, blockingStubFull);
    infoById = null;
    infoById = PublicMethed.getTransactionInfoById(trxid, blockingStubFull);

    logger.info("TrxId = " + trxid);
    logger.info("Info = \n" + infoById);
    logger.info("contractResult = " + ByteArray
        .toLong(infoById.get().getContractResult(0).toByteArray()));

    balanceBefore = PublicMethed.queryAccount(dev001Key, blockingStubFull).getBalance();
    logger.info("before balanceBefore is " + Long.toString(balanceBefore));

    parm = "\"" + Base58.encode58Check(transferTokenContractAddress) + "\"";
    contractname = "at(address)";

    trxid = PublicMethed
        .triggerContract(confirmContractAddress, contractname, parm, false, 0L, maxFeeLimit,
            dev001Address, dev001Key, blockingStubFull);
    infoById = null;
    infoById = PublicMethed.getTransactionInfoById(trxid, blockingStubFull);

    logger.info("TrxId = " + trxid);
    logger.info("Info = \n" + infoById);
    logger.info("contractResult = " + ByteArray
        .toLong(infoById.get().getContractResult(0).toByteArray()));
  }


  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


