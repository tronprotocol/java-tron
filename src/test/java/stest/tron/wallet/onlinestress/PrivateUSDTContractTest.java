package stest.tron.wallet.onlinestress;

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
import org.tron.api.WalletGrpc;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Transaction.Result.contractResult;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class PrivateUSDTContractTest {

  String txid;
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = "127.0.0.1:50051";

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
  }


  @Test(enabled = true)
  public void testTriggerPivate() {

    byte[] contractAddress = WalletClient
        .decodeFromBase58Check("TH882VtgCWFdAKCyH2Xiwof3iEu82bjMZd");
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    byte[] callerAddress = WalletClient.decodeFromBase58Check("TFsrP7YcSSRwHzLPwaCnXyTKagHs8rXKNJ");

    int i;
    for (i = 0; i < 1; i++) {
      /*
       * TriggerContract TFSLbVx89pebiu3xpRmUM6BWtcKL7YwMV8 mint(uint64,bytes32,bytes32[10],bytes32) 00000000000000000000000000000000000000000000000000000000ee6b2800d763de0ca35072efec2918cdb48fd6681e4eea2a7e9697c460385ea4ff4b46238a0cda63aa8f14e22da008705aeba8ea7987ec42830472bc49524b507a3195c89fa0b80e14d557eeed27ebff3c6aefa18e0aaf54e8aa4e5bd51dd86287641b4a9025ca8c2503ee35266a97f5af738a587e6c5b6943bc455da2d1fd2fc10ec86150f8a1e08163d05466e0da1c913d9c9fab9a365ec5d8bd7046a628ccd4cc4af10990db80786a62912f833186e4b6e5f3afe9a74234bbed1f5785dcd79ede205117dcd895423cda2ac3fb9ff87d9f64c4334190fb495776be2ad494c550e187dba49ec38b7b7d11ff40f898b6427465ea8d22f4e2411817691dc96103e08d4681e4f57f7d0e7d1cd80a556310cb252adf4b74559c51522b3c0df1ff6b9d61726d4f346ea72208dad7f4d9591525405ec367a6058f3625f4dc272d998e62e71de9b3ef70805ba7783fd8bc3e06f3a58d1382306ab1cef3603fe50e702b18652e082e99758548972a8e8822ad47fa1017ff72f06f3ff6a016851f45c398732bc50c true 1000000000 0 0 #
       * */
      txid = PublicMethed.triggerContract(contractAddress,
          "mint(uint64,bytes32,bytes32[10],bytes32)",
          "00000000000000000000000000000000000000000000000000000000ee6b2800d763de0ca35072efec2918cdb48fd6681e4eea2a7e9697c460385ea4ff4b46238a0cda63aa8f14e22da008705aeba8ea7987ec42830472bc49524b507a3195c89fa0b80e14d557eeed27ebff3c6aefa18e0aaf54e8aa4e5bd51dd86287641b4a9025ca8c2503ee35266a97f5af738a587e6c5b6943bc455da2d1fd2fc10ec86150f8a1e08163d05466e0da1c913d9c9fab9a365ec5d8bd7046a628ccd4cc4af10990db80786a62912f833186e4b6e5f3afe9a74234bbed1f5785dcd79ede205117dcd895423cda2ac3fb9ff87d9f64c4334190fb495776be2ad494c550e187dba49ec38b7b7d11ff40f898b6427465ea8d22f4e2411817691dc96103e08d4681e4f57f7d0e7d1cd80a556310cb252adf4b74559c51522b3c0df1ff6b9d61726d4f346ea72208dad7f4d9591525405ec367a6058f3625f4dc272d998e62e71de9b3ef70805ba7783fd8bc3e06f3a58d1382306ab1cef3603fe50e702b18652e082e99758548972a8e8822ad47fa1017ff72f06f3ff6a016851f45c398732bc50c",
          true,
          0L, 1000000000L,
          callerAddress, "650950B193DDDDB35B6E48912DD28F7AB0E7140C1BFDEFD493348F02295BD812",
          blockingStubFull);
      logger.info(txid);

      PublicMethed.waitProduceNextBlock(blockingStubFull);
      Optional<TransactionInfo> infoById = PublicMethed
          .getTransactionInfoById(txid, blockingStubFull);
      Assert.assertEquals(contractResult.SUCCESS, infoById.get().getReceipt().getResult());

    }
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


