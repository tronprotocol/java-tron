package stest.tron.wallet.assetissue;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.math.BigInteger;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.api.GrpcAPI.Return;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Contract;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.TransactionUtils;

@Slf4j
public class WalletTestAssetIssue008 {

  //testng001、testng002、testng003、testng004
  private final String testKey001 =
      "8CB4480194192F30907E14B52498F594BD046E21D7C4D8FE866563A6760AC891";
  private final String testKey002 =
      "FC8BF0238748587B9617EB6D15D47A66C0E07C1A1959033CF249C6532DC29FE6";
  private final String testKey003 =
      "6815B367FDDE637E53E9ADC8E69424E07724333C9A2B973CFA469975E20753FC";
  private final String testKey004 =
      "592BB6C9BB255409A6A43EFD18E6A74FECDDCCE93A40D96B70FBE334E6361E32";

  /*  //testng001、testng002、testng003、testng004
  private static final byte[] BACK_ADDRESS = Base58
      .decodeFromBase58Check("TKVyqEJaq8QRPQfWE8s8WPb5c92kanAdLo");
  private static final byte[] fromAddress = Base58
      .decodeFromBase58Check("THph9K2M2nLvkianrMGswRhz5hjSA9fuH7");
  private static final byte[] toAddress = Base58
      .decodeFromBase58Check("TV75jZpdmP2juMe1dRwGrwpV6AMU6mr1EU");
  private static final byte[] NEED_CR_ADDRESS = Base58
      .decodeFromBase58Check("27QEkeaPHhUSQkw9XbxX3kCKg684eC2w67T");*/

  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final byte[] toAddress   = PublicMethed.getFinalAddress(testKey003);

  private ManagedChannel channelFull = null;
  private ManagedChannel channelSolidity = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);

  private static final long now = System.currentTimeMillis();
  private static String name = "assetissue008" + Long.toString(now);
  private static final long totalSupply = now;
  String description = "test query assetissue from soliditynode";
  String url = "https://testqueryassetissue.com/from/soliditynode/";

  //get account
  ECKey ecKey = new ECKey(Utils.getRandom());
  byte[] queryAssetIssueFromSoliAddress = ecKey.getAddress();
  String queryAssetIssueKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  @BeforeClass(enabled = false)
  public void beforeClass() {
    logger.info(ByteArray.toHexString(ecKey.getPrivKeyBytes()));
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext(true)
        .build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);

    Assert.assertTrue(PublicMethed.freezeBalance(fromAddress,10000000,3,testKey002,
        blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(queryAssetIssueFromSoliAddress,2048000000,fromAddress,
        testKey002,blockingStubFull));
    Long start = System.currentTimeMillis() + 2000;
    Long end = System.currentTimeMillis() + 1000000000;
    //Create a new AssetIssue success.
    Assert.assertTrue(PublicMethed.createAssetIssue(queryAssetIssueFromSoliAddress, name,
        totalSupply, 1, 100, start, end, 1, description, url, 10000L,
        10000L,1L,1L,queryAssetIssueKey,blockingStubFull));
  }


  @Test(enabled = false)
  public void testGetAllAssetIssueFromSolidity() {
    GrpcAPI.AssetIssueList assetIssueList = blockingStubSolidity
        .getAssetIssueList(GrpcAPI.EmptyMessage.newBuilder().build());
    logger.info(Long.toString(assetIssueList.getAssetIssueCount()));



    if (assetIssueList.getAssetIssueCount() == 0) {
      Assert.assertTrue(PublicMethed.freezeBalance(fromAddress,10000000L,3,
          testKey002,blockingStubFull));
      Assert.assertTrue(PublicMethed.sendcoin(toAddress,999999L,fromAddress,
          testKey002,blockingStubFull));
      Block currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
      logger.info("fullnode block num is " + Long.toString(currentBlock.getBlockHeader()
          .getRawData().getNumber()));
      Block solidityCurrentBlock = blockingStubSolidity.getNowBlock(GrpcAPI
          .EmptyMessage.newBuilder().build());
      Integer wait = 0;
      while (solidityCurrentBlock.getBlockHeader().getRawData().getNumber()
          < currentBlock.getBlockHeader().getRawData().getNumber() + 1 && wait < 10) {
        try {
          Thread.sleep(3000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        logger.info("Solidity didn't synchronize the fullnode block,please wait");
        solidityCurrentBlock = blockingStubSolidity.getNowBlock(GrpcAPI.EmptyMessage.newBuilder()
            .build());
        wait++;
        logger.info("soliditynode block num is " + Long.toString(solidityCurrentBlock
            .getBlockHeader().getRawData().getNumber()));
        if (wait == 9) {
          logger.info("Didn't syn,skip to next case.");
        }
      }
    }

    assetIssueList = blockingStubSolidity
        .getAssetIssueList(GrpcAPI.EmptyMessage.newBuilder().build());
    Assert.assertTrue(assetIssueList.getAssetIssueCount() >= 1);
    for (Integer j = 0; j < assetIssueList.getAssetIssueCount(); j++) {
      Assert.assertFalse(assetIssueList.getAssetIssue(j).getOwnerAddress().isEmpty());
      Assert.assertFalse(assetIssueList.getAssetIssue(j).getName().isEmpty());
      Assert.assertFalse(assetIssueList.getAssetIssue(j).getUrl().isEmpty());
      Assert.assertTrue(assetIssueList.getAssetIssue(j).getTotalSupply() > 0);
      logger.info("test get all assetissue from solidity");
    }

  }

  @AfterClass(enabled = false)
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelSolidity != null) {
      channelSolidity.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  public Account queryAccount(ECKey ecKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
    byte[] address;
    if (ecKey == null) {
      String pubKey = loadPubKey(); //04 PubKey[128]
      if (StringUtils.isEmpty(pubKey)) {
        logger.warn("Warning: QueryAccount failed, no wallet address !!");
        return null;
      }
      byte[] pubKeyAsc = pubKey.getBytes();
      byte[] pubKeyHex = Hex.decode(pubKeyAsc);
      ecKey = ECKey.fromPublicOnly(pubKeyHex);
    }
    return grpcQueryAccount(ecKey.getAddress(), blockingStubFull);
  }

  public static String loadPubKey() {
    char[] buf = new char[0x100];
    return String.valueOf(buf, 32, 130);
  }

  public byte[] getAddress(ECKey ecKey) {
    return ecKey.getAddress();
  }

  public Account grpcQueryAccount(byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    ByteString addressBs = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBs).build();
    return blockingStubFull.getAccount(request);
  }

  public Block getBlock(long blockNum, WalletGrpc.WalletBlockingStub blockingStubFull) {
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    builder.setNum(blockNum);
    return blockingStubFull.getBlockByNum(builder.build());

  }

  private Transaction signTransaction(ECKey ecKey, Transaction transaction) {
    if (ecKey == null || ecKey.getPrivKey() == null) {
      logger.warn("Warning: Can't sign,there is no private key !!");
      return null;
    }
    transaction = TransactionUtils.setTimestamp(transaction);
    return TransactionUtils.sign(transaction, ecKey);
  }

  public boolean transferAsset(byte[] to, byte[] assertName, long amount, byte[] address,
      String priKey) {
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    Contract.TransferAssetContract.Builder builder = Contract.TransferAssetContract.newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsName = ByteString.copyFrom(assertName);
    ByteString bsOwner = ByteString.copyFrom(address);
    builder.setToAddress(bsTo);
    builder.setAssetName(bsName);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);

    Contract.TransferAssetContract contract = builder.build();
    Transaction transaction = blockingStubFull.transferAsset(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }
    transaction = signTransaction(ecKey, transaction);
    Return response = blockingStubFull.broadcastTransaction(transaction);
    if (response.getResult() == false) {
      return false;
    } else {
      Account search = queryAccount(ecKey, blockingStubFull);
      return true;
    }

  }

  public boolean unFreezeAsset(byte[] addRess, String priKey) {
    byte[] address = addRess;

    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;
    //Account search = queryAccount(ecKey, blockingStubFull);

    Contract.UnfreezeAssetContract.Builder builder = Contract.UnfreezeAssetContract
        .newBuilder();
    ByteString byteAddreess = ByteString.copyFrom(address);

    builder.setOwnerAddress(byteAddreess);

    Contract.UnfreezeAssetContract contract = builder.build();

    Transaction transaction = blockingStubFull.unfreezeAsset(contract);

    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }

    transaction = TransactionUtils.setTimestamp(transaction);
    transaction = TransactionUtils.sign(transaction, ecKey);
    Return response = blockingStubFull.broadcastTransaction(transaction);
    if (response.getResult() == false) {
      logger.info(ByteArray.toStr(response.getMessage().toByteArray()));
      return false;
    } else {
      return true;
    }
  }
}


