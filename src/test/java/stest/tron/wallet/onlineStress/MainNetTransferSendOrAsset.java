package stest.tron.wallet.onlineStress;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Account;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class MainNetTransferSendOrAsset {

  //testng001、testng002、testng003、testng004
  //fromAssetIssue
  private final String testKey001 =
      "6815B367FDDE637E53E9ADC8E69424E07724333C9A2B973CFA469975E20753FC";
  //toAssetIssue
  private final String testKey002 =
      "F153A0E1A65193846A3D48A091CD0335594C0A3D9817B3441390FDFF71684C84";
  //fromSend
  private final String testKey003 =
      "2514B1DD2942FF07F68C2DDC0EE791BC7FBE96FDD95E89B7B9BB3B4C4770FFAC";
  //toSend
  private final String testKey004 =
      "56244EE6B33C14C46704DFB67ED5D2BBCBED952EE46F1FD88A50C32C8C5C64CE";
  //Default
  private final String defaultKey =
      "8DFBB4513AECF779A0803C7CEBF2CDCC51585121FAB1E086465C4E0B40724AF1";

  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey001);
  private final byte[] toAddress   = PublicMethed.getFinalAddress(testKey002);
  private final byte[] fromSendAddress = PublicMethed.getFinalAddress(testKey003);
  private final byte[] toSendAddress   = PublicMethed.getFinalAddress(testKey004);
  private final byte[] defaultAddress = PublicMethed.getFinalAddress(defaultKey);


  private final Long transferAmount = 1L;
  private Long start;
  private Long end;
  private Long beforeToBalance;
  private Long afterToBalance;
  private Long beforeToAssetBalance = 0L;
  private Long afterToAssetBalance = 0L;
  private final Long sendAmount = 1L;
  private Long beforeBalance;
  private Long afterBalance;
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);

  Integer codeValue;
  Integer success = 0;
  Integer sigerror = 0;
  Integer contractValidateError = 0;
  Integer contractExeError = 0;
  Integer bandwithError = 0;
  Integer dupTransactionError = 0;
  Integer taposError = 0;
  Integer tooBigTransactionError = 0;
  Integer transactionExpirationError = 0;
  Integer serverBusy = 0;
  Integer otherError = 0;
  Integer clientWrong = 0;
  Integer times = 0;

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  @BeforeClass(enabled = true)
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    Account fromAccount = PublicMethed.queryAccount(testKey002,blockingStubFull);
    beforeBalance = fromAccount.getBalance();

  }

  @Test(enabled = true,threadPoolSize = 10, invocationCount = 10)
  public void freezeAnd() throws InterruptedException {
    Integer i = 0;

    while (i++ < 50) {

      codeValue = PublicMethed.sendcoinGetCode(toAddress,1,fromAddress,testKey001,blockingStubFull);
      times++;

      switch (codeValue) {
        case 0:
          success++;
          break;
        case 1:
          sigerror++;
          break;
        case 2:
          contractValidateError++;
          break;
        case 3:
          contractExeError++;
          break;
        case 4:
          bandwithError++;
          break;
        case 5:
          dupTransactionError++;
          break;
        case 6:
          taposError++;
          break;
        case 7:
          tooBigTransactionError++;
          break;
        case 8:
          transactionExpirationError++;
          break;
        case 9:
          serverBusy++;
          break;
        case 10:
          otherError++;
          break;
        case 12:
          clientWrong++;
          break;
        default:
          logger.info("No code value");
      }
    }
  }

  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Account toAccount   = PublicMethed.queryAccount(testKey002,blockingStubFull);

    afterBalance = toAccount.getBalance();
    logger.info("min is " + (afterBalance - beforeBalance));
    logger.info("times is " + times);

    logger.info("success " + success);
    logger.info("sigerror " + sigerror);
    logger.info("contractValidateError " + contractValidateError);
    logger.info("contractExeError " + contractExeError);
    logger.info("bandwithError " + bandwithError);
    logger.info("dupTransactionError " + dupTransactionError);
    logger.info("taposError " + taposError);
    logger.info("tooBigTransactionError " + tooBigTransactionError);
    logger.info("transactionExpirationError " + transactionExpirationError);
    logger.info("otherError " + otherError);
    logger.info("clientWrong " + clientWrong);


    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }


}


