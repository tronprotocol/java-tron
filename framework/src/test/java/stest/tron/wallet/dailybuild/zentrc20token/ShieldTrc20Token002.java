package stest.tron.wallet.dailybuild.zentrc20token;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannelBuilder;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.Note;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.ShieldedAddressInfo;
import stest.tron.wallet.common.client.utils.ZenTrc20Base;

@Slf4j
public class ShieldTrc20Token002 extends ZenTrc20Base {

  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);
  Optional<ShieldedAddressInfo> receiverShieldAddressInfo;
  private BigInteger publicFromAmount;
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

    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext(true)
        .build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);
    publicFromAmount = getRandomAmount();
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Send shield trc20 from T account to shield account")
  public void test01ShieldTrc20TransactionByTypeMint() throws Exception {
    //Query account before mint balance
    final Long beforeMintAccountBalance = getBalanceOfShieldTrc20(zenTrc20TokenOwnerAddressString,
        zenTrc20TokenOwnerAddress, zenTrc20TokenOwnerKey, blockingStubFull);
    //Query contract before mint balance
    final Long beforeMintShieldAccountBalance = getBalanceOfShieldTrc20(shieldAddress,
        zenTrc20TokenOwnerAddress, zenTrc20TokenOwnerKey, blockingStubFull);
    //Generate new shiled account and set note memo
    receiverShieldAddressInfo = getNewShieldedAddress(blockingStubFull);
    String memo = "Shield trc20 from T account to shield account in" + System.currentTimeMillis();
    String receiverShieldAddress = receiverShieldAddressInfo.get().getAddress();

    shieldOutList.clear();
    shieldOutList = addShieldTrc20OutputList(shieldOutList, receiverShieldAddress,
        "" + publicFromAmount, memo, blockingStubFull);

    //Create shiled trc20 parameters
    GrpcAPI.ShieldedTRC20Parameters shieldedTrc20Parameters
        = createShieldedTrc20Parameters(publicFromAmount,
        null, null, shieldOutList, "", 0L,
        blockingStubFull, blockingStubSolidity
    );
    String data = encodeMintParamsToHexString(shieldedTrc20Parameters, publicFromAmount);

    //Do mint transaction type
    String txid = PublicMethed.triggerContract(shieldAddressByte,
        mint, data, true, 0, maxFeeLimit, zenTrc20TokenOwnerAddress,
        zenTrc20TokenOwnerKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);

    logger.info(mint + ":" + txid);
    logger.info(mint + infoById.get().getReceipt().getEnergyUsageTotal());
    Assert.assertTrue(infoById.get().getReceipt().getEnergyUsageTotal() > 250000);
    Assert.assertTrue(infoById.get().getReceipt().getResultValue() == 1);

    //Query account after mint balance
    Long afterMintAccountBalance = getBalanceOfShieldTrc20(zenTrc20TokenOwnerAddressString,
        zenTrc20TokenOwnerAddress, zenTrc20TokenOwnerKey, blockingStubFull);
    //Query contract after mint balance
    Long afterMintShieldAccountBalance = getBalanceOfShieldTrc20(shieldAddress,
        zenTrc20TokenOwnerAddress, zenTrc20TokenOwnerKey, blockingStubFull);

    Assert.assertEquals(BigInteger.valueOf(beforeMintAccountBalance - afterMintAccountBalance),
        publicFromAmount);
    Assert.assertEquals(BigInteger.valueOf(afterMintShieldAccountBalance
        - beforeMintShieldAccountBalance), publicFromAmount);

    GrpcAPI.DecryptNotesTRC20 note = scanShieldedTrc20NoteByIvk(receiverShieldAddressInfo.get(),
        blockingStubFull);
    logger.info("" + note);

    Assert.assertEquals(note.getNoteTxs(0).getNote().getValue(), publicFromAmount.longValue());
    Assert.assertEquals(note.getNoteTxs(0).getNote().getPaymentAddress(),
        receiverShieldAddressInfo.get().getAddress());
    Assert.assertEquals(note.getNoteTxs(0).getNote().getMemo(), ByteString.copyFromUtf8(memo));
    Assert.assertEquals(note.getNoteTxs(0).getTxid(), infoById.get().getId());
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


