package stest.tron.wallet.onlinestress.tvm;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.WalletGrpc;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.protos.Protocol;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.PublicMethed;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Random;

import static stest.tron.wallet.common.client.utils.PublicMethed.getFinalAddress;

public class stUSDT {
  private final String testNetAccountKey = "f51dd12e73a409b0b8d2ab74c5b56edfcca3bbcd4cb24aea6ff69ae2c1eaabd4";
  private final byte[] testNetAccountAddress = getFinalAddress(testNetAccountKey);

  byte[] UnstUSDTProxy = null;

  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);

  @BeforeSuite
  public void beforeSuite() throws IOException {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() throws IOException {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    UnstUSDTProxy = WalletClient.decodeFromBase58Check("TVAsF1CnUhmHmN4shcBH4EoRxrncuEFbeu");
  }

  @Test(enabled = true, description = "requestWithdrawal")
  public void requestWithdrawal(){
    for (int i = 0;i<1200;i++){
      String txid = PublicMethed.triggerContract(UnstUSDTProxy,
              "requestWithdrawal(uint256)", "166666666666669", false,
              0, maxFeeLimit, testNetAccountAddress, testNetAccountKey, blockingStubFull);
      Optional<Protocol.TransactionInfo> infoById = null;
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
      Assert.assertTrue(infoById.get().getResultValue() == 0);
      System.out.println("txid:"+txid);
    }
  }

}
