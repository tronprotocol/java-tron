package stest.tron.wallet.contract.scenario;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
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
import org.tron.protos.Protocol.SmartContract;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class ContractScenario013 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");

  byte[] contractAddress = null;
  String txid = "";
  Optional<TransactionInfo> infoById = null;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contract013Address = ecKey1.getAddress();
  String contract013Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(contract013Key);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    Assert.assertTrue(PublicMethed.sendcoin(contract013Address,20000000000L,fromAddress,
        testKey002,blockingStubFull));
  }

  @Test(enabled = true)
  public void deployTronTrxAndSunContract() {
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(contract013Address,
        blockingStubFull);
    Long energyLimit = accountResource.getEnergyLimit();
    Long energyUsage = accountResource.getEnergyUsed();

    logger.info("before energy limit is " + Long.toString(energyLimit));
    logger.info("before energy usage is " + Long.toString(energyUsage));
    String contractName = "TronTrxAndSunContract";
    String code = "6080604052348015600f57600080fd5b50600180141515601e57600080fd5b603c80141515602c57600080fd5b610e1080141515603b57600080fd5b6201518080141515604b57600080fd5b62093a8080141515605b57600080fd5b6301e1338080141515606c57600080fd5b620f424080141515607c57600080fd5b60358060896000396000f3006080604052600080fd00a165627a7a723058206a36395ee2292959a89e7956d6826a7107c490331e4505fc319010873c26392b0029";
    String abi = "[{\"inputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"constructor\"}]";
    txid = PublicMethed.deployContractAndGetTransactionInfoById(contractName,abi,code,"",
        maxFeeLimit, 0L, 100,null,contract013Key,contract013Address,blockingStubFull);
    logger.info(txid);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(infoById.get().getReceipt().getEnergyUsageTotal() > 0);
    Assert.assertFalse(infoById.get().getContractAddress().isEmpty());
  }

  @Test(enabled = true)
  public void triggerTronTrxAndSunContract() {
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(contract013Address,
        blockingStubFull);
    Long energyLimit = accountResource.getEnergyLimit();
    Long energyUsage = accountResource.getEnergyUsed();

    logger.info("before energy limit is " + Long.toString(energyLimit));
    logger.info("before energy usage is " + Long.toString(energyUsage));
    String contractName = "TronTrxAndSunContract";
    String code = "608060405234801561001057600080fd5b5061011a806100206000396000f300608060405260043610603f576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff16806316ada547146044575b600080fd5b348015604f57600080fd5b506056606c565b6040518082815260200191505060405180910390f35b6000600180141515607c57600080fd5b603c80141515608a57600080fd5b610e1080141515609957600080fd5b620151808014151560a957600080fd5b62093a808014151560b957600080fd5b6301e133808014151560ca57600080fd5b620f42408014151560da57600080fd5b60018014151560e857600080fd5b429050905600a165627a7a72305820eacfee595582d9244a2fb5f052905bd240f87864fb8f602f85fd31fe3b89cda80029";
    String abi = "[{\"constant\":false,\"inputs\":[],\"name\":\"time\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    contractAddress = PublicMethed.deployContract(contractName,abi,code,"",maxFeeLimit,
        0L, 100,null,contract013Key,contract013Address,blockingStubFull);
    txid = PublicMethed.triggerContract(contractAddress,
        "time()", "#", false,
        0, 100000000L, contract013Address, contract013Key, blockingStubFull);
    logger.info(txid);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("result is " + infoById.get().getResultValue());
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(infoById.get().getReceipt().getEnergyUsageTotal() > 0);
    Assert.assertTrue(infoById.get().getFee() == infoById.get().getReceipt().getEnergyFee());
    Assert.assertFalse(infoById.get().getContractAddress().isEmpty());
  }

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


