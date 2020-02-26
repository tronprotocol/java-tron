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
  private String fullnode = "127.0.0.1:50056";

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
  public void testTriggerPrivateMint() {

    byte[] contractAddress = WalletClient
        .decodeFromBase58Check("TTguX62yeP5yqvrJEvBM8cTqu4pyE8spKL");
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    byte[] callerAddress = WalletClient.decodeFromBase58Check("TJCnKsPa7y5okkXvQAidZBzqx3QyQ6sxMW");

    int i;
    for (i = 0; i < 3; i++) {
      /*
       * TriggerContract TFSLbVx89pebiu3xpRmUM6BWtcKL7YwMV8 mint(uint64,bytes32,bytes32[10],bytes32) 00000000000000000000000000000000000000000000000000000000ee6b2800d763de0ca35072efec2918cdb48fd6681e4eea2a7e9697c460385ea4ff4b46238a0cda63aa8f14e22da008705aeba8ea7987ec42830472bc49524b507a3195c89fa0b80e14d557eeed27ebff3c6aefa18e0aaf54e8aa4e5bd51dd86287641b4a9025ca8c2503ee35266a97f5af738a587e6c5b6943bc455da2d1fd2fc10ec86150f8a1e08163d05466e0da1c913d9c9fab9a365ec5d8bd7046a628ccd4cc4af10990db80786a62912f833186e4b6e5f3afe9a74234bbed1f5785dcd79ede205117dcd895423cda2ac3fb9ff87d9f64c4334190fb495776be2ad494c550e187dba49ec38b7b7d11ff40f898b6427465ea8d22f4e2411817691dc96103e08d4681e4f57f7d0e7d1cd80a556310cb252adf4b74559c51522b3c0df1ff6b9d61726d4f346ea72208dad7f4d9591525405ec367a6058f3625f4dc272d998e62e71de9b3ef70805ba7783fd8bc3e06f3a58d1382306ab1cef3603fe50e702b18652e082e99758548972a8e8822ad47fa1017ff72f06f3ff6a016851f45c398732bc50c true 1000000000 0 0 #
       * */
      txid = PublicMethed.triggerContract(contractAddress,
          "mint(uint64,bytes32,bytes32[10],bytes32)",
          "00000000000000000000000000000000000000000000000000000000ee6b2800d763de0ca35072efec2918cdb48fd6681e4eea2a7e9697c460385ea4ff4b46238a0cda63aa8f14e22da008705aeba8ea7987ec42830472bc49524b507a3195c89fa0b80e14d557eeed27ebff3c6aefa18e0aaf54e8aa4e5bd51dd86287641b4a9025ca8c2503ee35266a97f5af738a587e6c5b6943bc455da2d1fd2fc10ec86150f8a1e08163d05466e0da1c913d9c9fab9a365ec5d8bd7046a628ccd4cc4af10990db80786a62912f833186e4b6e5f3afe9a74234bbed1f5785dcd79ede205117dcd895423cda2ac3fb9ff87d9f64c4334190fb495776be2ad494c550e187dba49ec38b7b7d11ff40f898b6427465ea8d22f4e2411817691dc96103e08d4681e4f57f7d0e7d1cd80a556310cb252adf4b74559c51522b3c0df1ff6b9d61726d4f346ea72208dad7f4d9591525405ec367a6058f3625f4dc272d998e62e71de9b3ef70805ba7783fd8bc3e06f3a58d1382306ab1cef3603fe50e702b18652e082e99758548972a8e8822ad47fa1017ff72f06f3ff6a016851f45c398732bc50c",
          true,
          0L, 1000000000L,
          callerAddress, "D95611A9AF2A2A45359106222ED1AFED48853D9A44DEFF8DC7913F5CBA727366",
          blockingStubFull);
      logger.info(txid);

      PublicMethed.waitProduceNextBlock(blockingStubFull);
      Optional<TransactionInfo> infoById = PublicMethed
          .getTransactionInfoById(txid, blockingStubFull);
      Assert.assertEquals(contractResult.SUCCESS, infoById.get().getReceipt().getResult());

    }
  }


  /*
  * Note: before test this function, it should trigger mint for at least 3 times
  * */
  @Test(enabled = true)
  public void testTriggerPrivateTransfer() {

    byte[] contractAddress = WalletClient
        .decodeFromBase58Check("TTguX62yeP5yqvrJEvBM8cTqu4pyE8spKL");
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    byte[] callerAddress = WalletClient.decodeFromBase58Check("TJCnKsPa7y5okkXvQAidZBzqx3QyQ6sxMW");

    int i;
    for (i = 0; i < 1024; i++) {
      logger.info("trigger the " + i + "th time");
      /*
       * TriggerContract TFSLbVx89pebiu3xpRmUM6BWtcKL7YwMV8 mint(uint64,bytes32,bytes32[10],bytes32) 00000000000000000000000000000000000000000000000000000000ee6b2800d763de0ca35072efec2918cdb48fd6681e4eea2a7e9697c460385ea4ff4b46238a0cda63aa8f14e22da008705aeba8ea7987ec42830472bc49524b507a3195c89fa0b80e14d557eeed27ebff3c6aefa18e0aaf54e8aa4e5bd51dd86287641b4a9025ca8c2503ee35266a97f5af738a587e6c5b6943bc455da2d1fd2fc10ec86150f8a1e08163d05466e0da1c913d9c9fab9a365ec5d8bd7046a628ccd4cc4af10990db80786a62912f833186e4b6e5f3afe9a74234bbed1f5785dcd79ede205117dcd895423cda2ac3fb9ff87d9f64c4334190fb495776be2ad494c550e187dba49ec38b7b7d11ff40f898b6427465ea8d22f4e2411817691dc96103e08d4681e4f57f7d0e7d1cd80a556310cb252adf4b74559c51522b3c0df1ff6b9d61726d4f346ea72208dad7f4d9591525405ec367a6058f3625f4dc272d998e62e71de9b3ef70805ba7783fd8bc3e06f3a58d1382306ab1cef3603fe50e702b18652e082e99758548972a8e8822ad47fa1017ff72f06f3ff6a016851f45c398732bc50c true 1000000000 0 0 #
       * */
      txid = PublicMethed.triggerContract(contractAddress,
          "transfer(bytes32[10],bytes32,bytes32,bytes32[9],bytes32[9],bytes32[2],bytes32)",
          "68e08957ad1b46fb29c45f15a2a3ace028ebc4706240d19f6d441385be24279d8390c081c47bc29fd7f726145a4da43d7eca197a8dfcf9abb3da64799dc2d051d2d84b4ac9da98b091aee43288f626bb7bbb23e3bc82578f85759475c85b440c030d4a9835a11d466942ea1840a11f7cd56036b5f2c87421a47efebe78cfda038023a87c67ddaec9063f9a7d2f712189f043613348187bead57316058397769d27441289a847fc1f6c20c0851d32727ea68e9f30925f95f9726f87bde930cd2c0bf7bca83ed85e71c9af677389505996c115bfb0bc3ac35f2b75f579de8e4b720c12910577e43b1605eb396a05e0a4ad582e0683e37d955574ba8830230925732539bc87de9d2c0e11d690087d72c0c7acb32aec3977cf2893bf93550219f71ad67e35f27c9b00ffbc0da5d2c3f9dc4d4bf45e2012b03cf897d46867fbbf475eb35c67aeb71c80ca4e3aa3fe930b0d934917b2363608edf13748a9236cf2bc2b25d268c008ec3612d28899584e6644f75b774c20e1979cd2c7894afb9d637ce1d6c2225e36a76320ae1c74e9ba9ae5d5b4a65ff70aa3dc900971edd17668fc4de8dbd7d729f25990497f1c2c09e3069a9da84156092e0804bc49f325d2aa992abbb2b79e14a41da6f4be45c10473e12bd561cd6b9fa35ec3cf5bbfb5bb708eabb1dba84f0b4a34aa94a95f05905a57295812f6a7bb1924c70882f98475a6af415bf36b55af8112e0ec5ea06c50971c70a0cbaa41261344c107eac6bb0e4988b50358987138d50f273d757c1f253388f3c66f3580c48e99d3bb22c1a2bb44c4dd12271f6fc0ebfdf38bef58aeb793f039fed9a9841c9cd750527660b69fcd96eccf94bf94ceef5ae223a7f431b67da726b34f3122a8f5ecb799bf42623c4a36ae197d573b83117a18b3ef81bf56c02bf6d465c3247f41b8f9c10d952a97fd3c152af8b99497705539f8506f20867ef69b87e03bf90bbe52d90476f6b98369a5a152309ff5aa3c268791f096ad44fb97326f98cddbd30227ff0a245f1ce2ed7c59d62d7a73c02b49ac71a96c8021a86421c8bd45607d61c0710372d9dfab72fe2094e2e91b3cb901e49f13203f11d92c0e7239d7daeab1b5591880cad9880bef436665acc1fe149a42beb9570aa654d463b23f53464dd11a340c6ef44d4eb63e56601434bb035ab921818ce760791069d21c7816dab38b234e35e678a1a92f4bdb026b1a460998295795aafd3081d138ba6261156b52378023f3ced571a769a6fb5c342949819d48d80f81a5c0332b96609195159269926c80b14b41c39044e722699aea68298dd210fbbb10f89d999dbd25d435a1bca333bef716bd6e5a7c39d61321b1697765d1083827bf164f22e06789ddc80dd27477565bbd4c84c4bb68b7293e950a3c10370e06659dc44b0fc641e637145ea43561d21d3908b00457e60b0a87ac3e896cc434695db48d0020fffa9f2c8bc75dfafaf4fbc7b7659fb84d95",
          true,
          0L, 1000000000L,
          callerAddress, "D95611A9AF2A2A45359106222ED1AFED48853D9A44DEFF8DC7913F5CBA727366",
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


