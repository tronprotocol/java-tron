package stest.tron.wallet.account;

import com.google.protobuf.ByteString;
import com.googlecode.cqengine.query.simple.In;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.math.BigInteger;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;



@Slf4j
public class WalletTestAccount001 {
  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final String invalidTestKey =
      "592BB6C9BB255409A6A45EFD18E9A74FECDDCCE93A40D96B70FBE334E6361E36";

  private ManagedChannel channelFull = null;
  private ManagedChannel channelSolidity = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;

  private static final long now = System.currentTimeMillis();

  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  @BeforeClass
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext(true)
        .build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);
  }


  @Test
  public void testqueryaccountfromfullnode() {
    //Query success, get the right balance,bandwidth and the account name.
    Account queryResult = queryAccount(testKey002, blockingStubFull);
    /*    Account queryResult = PublicMethed.queryAccountByAddress(fromAddress,blockingStubFull);
    logger.info(ByteArray.toStr(queryResult.getAccountName().toByteArray()));
    logger.info(Long.toString(queryResult.getBalance()));
    logger.info(ByteArray.toStr(queryResult.getAddress().toByteArray()));*/
    Assert.assertTrue(queryResult.getBalance() > 0);
    //Assert.assertTrue(queryResult.getBandwidth() >= 0);
    Assert.assertTrue(queryResult.getAccountName().toByteArray().length > 0);
    Assert.assertFalse(queryResult.getAddress().isEmpty());

    //Query failed
    Account invalidQueryResult = queryAccount(invalidTestKey, blockingStubFull);
    Assert.assertTrue(invalidQueryResult.getAccountName().isEmpty());
    Assert.assertTrue(invalidQueryResult.getAddress().isEmpty());

    //Improve coverage.
    queryResult.hashCode();
    queryResult.getSerializedSize();
    queryResult.equals(queryResult);
    queryResult.equals(invalidQueryResult);
  }

  @Test
  public void testqueryaccountfromsoliditynode() {
    //Query success, get the right balance,bandwidth and the account name.
    Account queryResult = solidityqueryAccount(testKey002, blockingStubSolidity);
    Assert.assertTrue(queryResult.getBalance() > 0);
    //Assert.assertTrue(queryResult.getBandwidth() >= 0);
    Assert.assertTrue(queryResult.getAccountName().toByteArray().length > 0);
    Assert.assertFalse(queryResult.getAddress().isEmpty());

    //Query failed
    Account invalidQueryResult = solidityqueryAccount(invalidTestKey, blockingStubSolidity);
    Assert.assertTrue(invalidQueryResult.getAccountName().isEmpty());
    Assert.assertTrue(invalidQueryResult.getAddress().isEmpty());


  }

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelSolidity != null) {
      channelSolidity.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  public Account queryAccount(String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;
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
    logger.info(Integer.toString(ecKey.getAddress().length));

    //PublicMethed.AddPreFix();
    logger.info(Integer.toString(ecKey.getAddress().length));
    System.out.println("address ====== " + ByteArray.toHexString(ecKey.getAddress()));
    return grpcQueryAccount(ecKey.getAddress(), blockingStubFull);
    //return grpcQueryAccount(address,blockingStubFull);
  }


  public Account solidityqueryAccount(String priKey,
      WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity) {

    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;
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
    //byte[] address = PublicMethed.AddPreFix(ecKey.getAddress());
    return grpcQueryAccountSolidity(ecKey.getAddress(), blockingStubSolidity);
    //return grpcQueryAccountSolidity(address,blockingStubSolidity);
  }

  public String loadPubKey() {
    char[] buf = new char[0x100];
    return String.valueOf(buf, 32, 130);
  }

  public byte[] getAddress(ECKey ecKey) {
    return ecKey.getAddress();
  }

  public Account grpcQueryAccount(byte[] address,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    //address = PublicMethed.AddPreFix(address);
    ByteString addressBs = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBs).build();
    return blockingStubFull.getAccount(request);
  }

  public Account grpcQueryAccountSolidity(byte[] address,
      WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity) {
    //address = PublicMethed.AddPreFix(address);
    ByteString addressBs = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBs).build();
    return blockingStubSolidity.getAccount(request);
  }


  public Block getBlock(long blockNum, WalletGrpc.WalletBlockingStub blockingStubFull) {
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    builder.setNum(blockNum);
    return blockingStubFull.getBlockByNum(builder.build());

  }
}


