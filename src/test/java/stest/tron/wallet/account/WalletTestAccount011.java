package stest.tron.wallet.account;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.api.GrpcAPI.EasyTransferByPrivateMessage;
import org.tron.api.GrpcAPI.EasyTransferMessage;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.TransactionSign;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class WalletTestAccount011 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private ManagedChannel channelFull = null;
  private ManagedChannel channelSolidity = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;

  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] account011Address = ecKey1.getAddress();
  String account011Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(account011Key);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext(true)
        .build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);

  }

  @Test(enabled = true)
  public void testgenerateAddress() {
    EmptyMessage.Builder builder = EmptyMessage.newBuilder();
    blockingStubFull.generateAddress(builder.build());
    blockingStubSolidity.generateAddress(builder.build());
  }

  @Test(enabled = true)
  public void testeasyTransfer() {
    ecKey1 = new ECKey(Utils.getRandom());
    account011Address = ecKey1.getAddress();
    account011Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    PublicMethed.printAddress(testKey002);
    PublicMethed.printAddress(account011Key);
    Assert.assertTrue(PublicMethed.sendcoin(account011Address,10000000L,fromAddress,
        testKey002,blockingStubFull));


    String password = Long.toString(System.currentTimeMillis());
    BytesMessage.Builder builder = BytesMessage.newBuilder();
    builder.setValue(ByteString.copyFrom(password.getBytes()));
    BytesMessage result = blockingStubFull.createAddress(builder.build());
    byte[] address = result.getValue().toByteArray();

    Account fromAccount = PublicMethed.queryAccount(account011Key,blockingStubFull);
    final long beforeFromBaslance = fromAccount.getBalance();

    EasyTransferByPrivateMessage.Builder builder1 = EasyTransferByPrivateMessage.newBuilder();
    builder1.setPrivateKey(ByteString.copyFrom(ByteArray.fromHexString(account011Key)));
    builder1.setToAddress(ByteString.copyFrom(address));
    builder1.setAmount(2000000L);
    Assert.assertTrue(blockingStubFull.easyTransferByPrivate(builder1.build())
        .getResult().getCodeValue() == 0);

    fromAccount = PublicMethed.queryAccount(account011Key,blockingStubFull);
    final long afterFromBaslance = fromAccount.getBalance();
    logger.info("beforeFromBaslance is " + beforeFromBaslance);
    logger.info("afterFromBaslance is  " + afterFromBaslance);
    logger.info("min is " + (beforeFromBaslance - afterFromBaslance));
    Assert.assertTrue(beforeFromBaslance - afterFromBaslance == 2000000L + 100000);


    EasyTransferMessage.Builder builder2 = EasyTransferMessage.newBuilder();
    builder2.setPassPhrase(ByteString.copyFrom(password.getBytes()));
    builder2.setToAddress(ByteString.copyFrom(account011Address));
    builder2.setAmount(100);
    Assert.assertTrue(blockingStubFull.easyTransfer(builder2.build()).getResult()
        .getCodeValue() == 0);

    Contract.TransferContract.Builder builder5 = Contract.TransferContract.newBuilder();
    ByteString bsTo = ByteString.copyFrom(address);
    ByteString bsOwner = ByteString.copyFrom(account011Address);
    builder5.setToAddress(bsTo);
    builder5.setOwnerAddress(bsOwner);
    builder5.setAmount(100L);

    Contract.TransferContract contract = builder5.build();
    Protocol.Transaction transaction = blockingStubFull.createTransaction(contract);

    TransactionSign.Builder builder4 = TransactionSign.newBuilder();
    builder4.setPrivateKey(ByteString.copyFrom(ByteArray.fromHexString(account011Key)));
    builder4.setTransaction(transaction);
    blockingStubFull.getTransactionSign(builder4.build());
    blockingStubFull.getTransactionSign2(builder4.build());
  }

  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelSolidity != null) {
      channelSolidity.shutdown().awaitTermination(5, TimeUnit.SECONDS);

    }

  }
}
