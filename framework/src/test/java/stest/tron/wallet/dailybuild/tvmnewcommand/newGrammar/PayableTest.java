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
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class PayableTest {
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

    String filePath = "src/test/resources/soliditycode/payable001.sol";
    String contractName = "PayableTest";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    contractAddress = PublicMethed
        .deployContract(contractName, abi, code, "", maxFeeLimit, 10000, 100, null,
            testFoundationKey, testFoundationAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true, description = "payable(address) transfer")
  public void tryCatchTest001() {

    Account account = PublicMethed
        .queryAccount(PublicMethed.decode58Check(
            "TBXSw8fM4jpQkGc6zZjsVABFpVN7UvXPdV"), blockingStubFull);
    Long balanceBefore = account.getBalance();

    String methodStr = "receiveMoneyTransfer(address,uint256)";
    String argStr = "\"TBXSw8fM4jpQkGc6zZjsVABFpVN7UvXPdV\",3";
    String TriggerTxid = PublicMethed.triggerContract(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> transactionInfo = PublicMethed
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("transactionInfo: " + transactionInfo.get());
    Assert.assertEquals(0,transactionInfo.get().getResultValue());
    Assert.assertTrue(transactionInfo.get().getFee() < maxFeeLimit);
    Long balanceAfter = PublicMethed.queryAccount(PublicMethed.decode58Check(
        "TBXSw8fM4jpQkGc6zZjsVABFpVN7UvXPdV"), blockingStubFull).getBalance();
    Assert.assertEquals(balanceBefore + 3,balanceAfter.longValue());
  }

  @Test(enabled = true, description = "payable(address) send")
  public void tryCatchTest002() {

    Account account = PublicMethed
        .queryAccount(PublicMethed.decode58Check(
            "TBXSw8fM4jpQkGc6zZjsVABFpVN7UvXPdV"), blockingStubFull);
    Long balanceBefore = account.getBalance();

    String methodStr = "receiveMoneySend(address,uint256)";
    String argStr = "\"TBXSw8fM4jpQkGc6zZjsVABFpVN7UvXPdV\",3";
    String TriggerTxid = PublicMethed.triggerContract(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> transactionInfo = PublicMethed
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("transactionInfo: " + transactionInfo.get());
    Assert.assertEquals(0,transactionInfo.get().getResultValue());
    Assert.assertTrue(transactionInfo.get().getFee() < maxFeeLimit);
    Long balanceAfter = PublicMethed.queryAccount(PublicMethed.decode58Check(
        "TBXSw8fM4jpQkGc6zZjsVABFpVN7UvXPdV"), blockingStubFull).getBalance();
    Assert.assertEquals(balanceBefore + 3,balanceAfter.longValue());
  }

  @Test(enabled = true, description = "payable(address(contract)) transfer")
  public void tryCatchTest003() {

    String filePath = "src/test/resources/soliditycode/payable001.sol";
    String contractName = "A";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    byte[] AContract = PublicMethed
        .deployContract(contractName, abi, code, "", maxFeeLimit, 0, 100, null,
            testKey001, testAddress001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);


    Account account = PublicMethed
        .queryAccount(AContract, blockingStubFull);
    Long balanceBefore = account.getBalance();

    String methodStr = "receiveMoneyTransferWithContract(address,uint256)";
    String argStr = "\"" + Base58.encode58Check(AContract) + "\",3";
    String TriggerTxid = PublicMethed.triggerContract(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> transactionInfo = PublicMethed
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("transactionInfo: " + transactionInfo.get());
    Assert.assertEquals(0,transactionInfo.get().getResultValue());
    Assert.assertTrue(transactionInfo.get().getFee() < maxFeeLimit);
    Long balanceAfter = PublicMethed.queryAccount(AContract, blockingStubFull).getBalance();
    Assert.assertEquals(balanceBefore + 3,balanceAfter.longValue());
  }

}
