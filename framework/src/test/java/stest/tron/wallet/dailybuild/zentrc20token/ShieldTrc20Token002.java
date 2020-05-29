package stest.tron.wallet.dailybuild.zentrc20token;

import io.grpc.ManagedChannelBuilder;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.spongycastle.util.encoders.Hex;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.Note;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.WalletGrpc;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.AbiUtil;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.ShieldAddressInfo;
import stest.tron.wallet.common.client.utils.ShieldedAddressInfo;
import stest.tron.wallet.common.client.utils.ZenTrc20Base;

@Slf4j
public class ShieldTrc20Token002 extends ZenTrc20Base{
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  Optional<ShieldedAddressInfo> receiverShieldAddressInfo;

  private BigInteger publicFromAmount = BigInteger.valueOf(1000L);
  List<Note> shieldOutList = new ArrayList<>();

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

  @Test(enabled = true, description = "Send shield trc20 from T account to shield account")
  public void test01GenerateNewShieldedTrc20Address() throws Exception{
    Long beforeMintAccountBalance = getBalanceOfShieldTrc20(zenTrc20TokenOwnerAddressString,zenTrc20TokenOwnerAddress,
        zenTrc20TokenOwnerKey,blockingStubFull);
    Long beforeMintShieldAccountBalance = getBalanceOfShieldTrc20(shieldAddress,zenTrc20TokenOwnerAddress,
        zenTrc20TokenOwnerKey,blockingStubFull);
    receiverShieldAddressInfo = getNewShieldedAddress(blockingStubFull);
    String memo = "Send shield trc20 from T account to shield account in" + System.currentTimeMillis();
    String receiverShieldAddress = receiverShieldAddressInfo.get().getAddress();
    shieldOutList = PublicMethed.addShieldOutputList(shieldOutList, receiverShieldAddress,
        "" + publicFromAmount, memo);

    GrpcAPI.ShieldedTRC20Parameters shieldedTRC20Parameters = createShieldedTrc20Parameters(publicFromAmount,
        null,shieldOutList,"",0L,blockingStubFull
        );
    String data = encodeMintParamsToHexString(shieldedTRC20Parameters, publicFromAmount);

    String txid = PublicMethed.triggerContract(shieldAddressByte,
        mint, data, true,
        0, maxFeeLimit, zenTrc20TokenOwnerAddress, zenTrc20TokenOwnerKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info(mint + ":" + txid);
    logger.info(mint + infoById.get().getReceipt().getEnergyUsageTotal());
    Assert.assertTrue(infoById.get().getReceipt().getResultValue() == 1);


    Long afterMintAccountBalance = getBalanceOfShieldTrc20(zenTrc20TokenOwnerAddressString,zenTrc20TokenOwnerAddress,
        zenTrc20TokenOwnerKey,blockingStubFull);
    Long afterMintShieldAccountBalance = getBalanceOfShieldTrc20(shieldAddress,zenTrc20TokenOwnerAddress,
        zenTrc20TokenOwnerKey,blockingStubFull);

    logger.info("beforeMintAccountBalance      :" + beforeMintAccountBalance);
    logger.info("beforeMintShieldAccountBalance:" + beforeMintShieldAccountBalance);
    logger.info("afterMintAccountBalance       :" + afterMintAccountBalance);
    logger.info("afterMintShieldAccountBalance :" + afterMintShieldAccountBalance);
    Assert.assertEquals(BigInteger.valueOf(beforeMintAccountBalance - afterMintAccountBalance) , publicFromAmount);
    Assert.assertEquals(BigInteger.valueOf(afterMintShieldAccountBalance - beforeMintShieldAccountBalance) , publicFromAmount);

    GrpcAPI.DecryptNotesTRC20 note = scanShieldedTRC20NoteByIvk(receiverShieldAddressInfo.get(),blockingStubFull);
    //logger.info("" + scanShieldedTRC20NoteByIvk(receiverShieldAddressInfo.get(),blockingStubFull).getNoteTxs(0).getNote());

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


