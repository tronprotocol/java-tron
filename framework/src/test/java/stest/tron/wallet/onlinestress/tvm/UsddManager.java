package stest.tron.wallet.onlinestress.tvm;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter;
import stest.tron.wallet.common.client.utils.PublicMethed;


@Slf4j
public class UsddManager {

  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethed.getFinalAddress(testNetAccountKey);
  byte[] contractAddress = null;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contractExcAddress = ecKey1.getAddress();
  String contractExcKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  String MultiSigAuthorizer = "TLGvyDSZa7trZ36q13q7iNLWykGEJR5yYr";
  String lockContract = "TAYv1gLrNijtu2cCnx8YW6q9cfNDzZPzLg";
  String MultiSigFundRaiser = "TQyGdTsd7qPZDx35s9915qwDwJR1EhQdgy";
  String PSMMultiSigWallet = "TVLxcjofc5i24CFtXkVZQBd4b3LqiNBM7u";



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
    PublicMethed.printAddress(contractExcKey);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext(true)
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);
  }


  @Test(enabled = true, description = "Open experimental check address ")
  public void test01Sig() {
    String ownerPk1="0eb8e2b7a54e775e0ef8baebf5ff9dd952032788f11c6f3b582aff1b9a2443c5";
    String ownerPk2="aac82ddc73c7a25b3b5034cd76badac07247c7561ef235ca54f753c32f60f138";
    String ownerPk3="112cdf38cfcf5af91c8547454e63ce7360e1cf165440d7d28a1e41fc07c633b6";
    String ownerPk4="021682220665ab1cd97099e3ea29dbcf4b9947f1c3e1afc2a31823fc539219e8";
    String ownerPk5="cc9ffeeed1708986ab812e1a0f54d70717060b7c0776e1ae78834e672ad5c13e";
    String ownerPk6="3a486c97ddc991ca795766b50791524249b07567f1a567ee70f79cdf0de79619";
    String ownerPk7="f8c190440b4adfea1eb4507ca8f749d2cbd02979fdbaca9f537d585b55c479f8";
    String[] ownerPks = new String[7];

    ownerPks[0] = ownerPk1;
    ownerPks[1] = ownerPk2;
    ownerPks[2] = ownerPk3;
    ownerPks[3] = ownerPk4;
    ownerPks[4] = ownerPk5;
    ownerPks[5] = ownerPk6;
    ownerPks[6] = ownerPk7;
    for (int i = 1; i < 7; i++) {
      String txid = "";
      String param = "5";
      txid = PublicMethed.triggerContract(PublicMethed.decode58Check(MultiSigAuthorizer),
          "confirmTransaction(uint256)", param, false,
          0, maxFeeLimit, PublicMethed.getFinalAddress(ownerPks[i]), ownerPks[i], blockingStubFull);
//      Optional<Protocol.TransactionInfo> infoById = null;
//      PublicMethed.waitProduceNextBlock(blockingStubFull);
//      infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
//      logger.info(infoById.toString());
//      Assert.assertEquals(0, infoById.get().getResultValue());
    }

    /*for (int i = 2; i < 3; i++) {
      String txid = "";
      String param = "0";
      txid = PublicMethed.triggerContract(PublicMethed.decode58Check(lockContract),
          "revokeConfirmation(uint256)", param, false,
          0, maxFeeLimit, PublicMethed.getFinalAddress(ownerPks[i]), ownerPks[i], blockingStubFull);
      Optional<Protocol.TransactionInfo> infoById = null;
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
      logger.info(infoById.toString());
      Assert.assertEquals(0, infoById.get().getResultValue());
    }*/
  }

  @Test(enabled = true, description = "Open experimental check address ")
  public void test02lock() {
    String ownerPk1="0eb8e2b7a54e775e0ef8baebf5ff9dd952032788f11c6f3b582aff1b9a2443c5";
    String ownerPk2="aac82ddc73c7a25b3b5034cd76badac07247c7561ef235ca54f753c32f60f138";
    String ownerPk3="112cdf38cfcf5af91c8547454e63ce7360e1cf165440d7d28a1e41fc07c633b6";
    String ownerPk4="021682220665ab1cd97099e3ea29dbcf4b9947f1c3e1afc2a31823fc539219e8";
    String ownerPk5="cc9ffeeed1708986ab812e1a0f54d70717060b7c0776e1ae78834e672ad5c13e";
    String ownerPk6="3a486c97ddc991ca795766b50791524249b07567f1a567ee70f79cdf0de79619";
    String ownerPk7="f8c190440b4adfea1eb4507ca8f749d2cbd02979fdbaca9f537d585b55c479f8";
    String[] ownerPks = new String[7];

    ownerPks[0] = ownerPk1;
    ownerPks[1] = ownerPk2;
    ownerPks[2] = ownerPk3;
    ownerPks[3] = ownerPk4;
    ownerPks[4] = ownerPk5;
    ownerPks[5] = ownerPk6;
    ownerPks[6] = ownerPk7;
    for (int i = 0; i < 7; i++) {
      String txid = "";
      String param = "0";
      txid = PublicMethed.triggerContract(PublicMethed.decode58Check(lockContract),
          "confirmTransaction(uint256)", param, false,
          0, maxFeeLimit, PublicMethed.getFinalAddress(ownerPks[i]), ownerPks[i], blockingStubFull);
      Optional<Protocol.TransactionInfo> infoById = null;
      /*PublicMethed.waitProduceNextBlock(blockingStubFull);
      infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
      logger.info(infoById.toString());
      Assert.assertEquals(0, infoById.get().getResultValue());*/
    }

    /*for (int i = 0; i < 3; i++) {
      String txid = "";
      String param = "1";
      txid = PublicMethed.triggerContract(PublicMethed.decode58Check(lockContract),
          "revokeConfirmation(uint256)", param, false,
          0, maxFeeLimit, PublicMethed.getFinalAddress(ownerPks[i]), ownerPks[i], blockingStubFull);
      Optional<Protocol.TransactionInfo> infoById = null;
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
      logger.info(infoById.toString());
      Assert.assertEquals(0, infoById.get().getResultValue());
    }*/
  }


  @Test(enabled = true, description = "Open experimental check address ")
  public void test03Raiser() {
    /*String ownerPk1="aa5a00a862736de71544843a93c4123988418a152fbfe2e1f07c7791a60da533";
    String ownerPk2="f51dd12e73a409b0b8d2ab74c5b56edfcca3bbcd4cb24aea6ff69ae2c1eaabd4";
    String ownerPk3="1ec9c30c9c246572557d8aaf88fd0823b70fb4b5a085be80959d66be0afb2848";
    String ownerPk4="4df12b6b37734c521eadc4ce5811f27f40e8bae8d43d32804dbf580d40aebcd7";
    String ownerPk5="780113819a84426fce36ba0faa01b0eabafd1a41fba1274c41a34cf45f223c43";
    String ownerPk6="a26feab23f5ec682e1c44600f6ae91a343969a7ff8d3cfae71ecc980507af592";
    String ownerPk7="9e5f59f4ec89b8b7fe985f2ebda7a815e670a7d2461ff883381021028c6e243e";*/
    String ownerPk1="0eb8e2b7a54e775e0ef8baebf5ff9dd952032788f11c6f3b582aff1b9a2443c5";
    String ownerPk2="aac82ddc73c7a25b3b5034cd76badac07247c7561ef235ca54f753c32f60f138";
    String ownerPk3="112cdf38cfcf5af91c8547454e63ce7360e1cf165440d7d28a1e41fc07c633b6";
    String ownerPk4="021682220665ab1cd97099e3ea29dbcf4b9947f1c3e1afc2a31823fc539219e8";
    String ownerPk5="cc9ffeeed1708986ab812e1a0f54d70717060b7c0776e1ae78834e672ad5c13e";
    String ownerPk6="3a486c97ddc991ca795766b50791524249b07567f1a567ee70f79cdf0de79619";
    String ownerPk7="f8c190440b4adfea1eb4507ca8f749d2cbd02979fdbaca9f537d585b55c479f8";
    String[] ownerPks = new String[7];

    ownerPks[0] = ownerPk1;
    ownerPks[1] = ownerPk2;
    ownerPks[2] = ownerPk3;
    ownerPks[3] = ownerPk4;
    ownerPks[4] = ownerPk5;
    ownerPks[5] = ownerPk6;
    ownerPks[6] = ownerPk7;
    for (int i = 0; i < 5; i++) {
      String txid = "";
      String param = "10";
      txid = PublicMethed.triggerContract(PublicMethed.decode58Check(MultiSigFundRaiser),
          "confirmTransaction(uint256)", param, false,
          0, maxFeeLimit, PublicMethed.getFinalAddress(ownerPks[i]), ownerPks[i], blockingStubFull);
      Optional<Protocol.TransactionInfo> infoById = null;
      /*PublicMethed.waitProduceNextBlock(blockingStubFull);
      infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
      logger.info(infoById.toString());
      Assert.assertEquals(0, infoById.get().getResultValue());*/
    }
  }

  @Test(enabled = true, description = "Open experimental check address ")
  public void test04Psm() {
    String ownerPk1="cb8e2613d30b303f3240919550ff8d8aacc05885183cc33c52ec665c424731c5";
    String ownerPk2="f499d705ca4d4bb2330bc56baef0713b452b1ec07c66418dd85ed3ec439c7b87";
    String ownerPk3="93e6237942cbbf2a1d15cb623b1ffce94a45a2493b6e7766ed5db75d1769fbd5";
    String ownerPk4="3e49807cef7c1280ae1242823d1d151d3b0b8343e5854c359a5d786e51f25fbd";
    String ownerPk5="71b9ee1b0d1bfb64fac06d502d7075b86c9bd983b52f9a174c332670470ec4c4";
    String ownerPk6="cef2da8b0807f79daccf0576cbe4069bfac85ad92f08365e52df010cf7696a1e";
    String ownerPk7="378898297a704879b45158671f755978f38b1589df22de3ca368fa5ad11eb27d";
    String[] ownerPks = new String[7];

    ownerPks[0] = ownerPk1;
    ownerPks[1] = ownerPk2;
    ownerPks[2] = ownerPk3;
    ownerPks[3] = ownerPk4;
    ownerPks[4] = ownerPk5;
    ownerPks[5] = ownerPk6;
    ownerPks[6] = ownerPk7;
    for (int i = 0; i < 7; i++) {
      String txid = "";
      String param = "29";
      txid = PublicMethed.triggerContract(PublicMethed.decode58Check(PSMMultiSigWallet),
          "confirmTransaction(uint256)", param, false,
          0, maxFeeLimit, PublicMethed.getFinalAddress(ownerPks[i]), ownerPks[i], blockingStubFull);
      Optional<Protocol.TransactionInfo> infoById = null;
      /*PublicMethed.waitProduceNextBlock(blockingStubFull);
      infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
      logger.info(infoById.toString());
      Assert.assertEquals(0, infoById.get().getResultValue());*/
    }
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    long balance = PublicMethed.queryAccount(contractExcKey, blockingStubFull).getBalance();
    PublicMethed.sendcoin(testNetAccountAddress, balance, contractExcAddress, contractExcKey,
        blockingStubFull);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelFull1 != null) {
      channelFull1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }


}

