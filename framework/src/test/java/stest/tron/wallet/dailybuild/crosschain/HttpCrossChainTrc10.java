package stest.tron.wallet.dailybuild.crosschain;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Protocol.CrossMessage;
import org.tron.protos.Protocol.CrossMessage.Type;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.BalanceContract;
import org.tron.protos.contract.BalanceContract.CrossContract.CrossDataType;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.CrossChainBase;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class HttpCrossChainTrc10 extends CrossChainBase {

  private Long sendAmount = 12L;
  private String httpnode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(0);
  private String crossHttpnode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(1);

  @Test(enabled = true,description = "Transfer trc10 in cross chain by http")
  public void test01CreateCrossTokenByHttp() throws Exception {
    int times = 0;
    while (times++ < 20) {
      PublicMethed.sendcoin(foundationAddress, 1L, trc10TokenAccountAddress,
          trc10TokenAccountKey, blockingStubFull);
    }
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    final Long beforeFromTokenBalance = PublicMethed
        .getAssetBalanceByAssetId(assetAccountId1, trc10TokenAccountKey,blockingStubFull);
    final Long beforeToTokenBalance = PublicMethed
        .getAssetBalanceByAssetId(assetAccountId2, trc10TokenAccountKey,crossBlockingStubFull);
    final Long beforeFromBalance = PublicMethed
        .queryAccount(trc10TokenAccountAddress,blockingStubFull).getBalance();
    final Long beforeToBalance = PublicMethed
        .queryAccount(trc10TokenAccountAddress,crossBlockingStubFull).getBalance();
    final Long beforeToNetUsaged = PublicMethed
        .queryAccount(trc10TokenAccountAddress,crossBlockingStubFull).getFreeNetUsage();

    final Long beforeBlockNum = blockingStubFull.getNowBlock(EmptyMessage.newBuilder()
        .build()).getBlockHeader().getRawData().getNumber();

    final Long beforeAssetTotalSupply
        = PublicMethed.getAssetBalanceByAssetId(assetAccountId1, trc10TokenAccountKey,
        blockingStubFull);


    String txid = createCrossTrc10TransferByHttp(trc10TokenAccountAddress,
        trc10TokenAccountAddress,assetAccountId1,chainId,6,sendAmount,name1,chainId,crossChainId,
        trc10TokenAccountKey,httpnode);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(crossBlockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(crossBlockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(crossBlockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(crossBlockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(crossBlockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(crossBlockingStubFull);


    //Query first chain
    Optional<Transaction> byId = PublicMethed.getTransactionById(txid, blockingStubFull);
    Any any = byId.get().getRawData().getContract(0).getParameter();
    BalanceContract.CrossContract crossContract = any.unpack(BalanceContract.CrossContract.class);
    Assert.assertEquals(crossContract.getOwnerAddress(),
        ByteString.copyFrom(trc10TokenAccountAddress));
    Assert.assertEquals(crossContract.getToAddress(),
        ByteString.copyFrom(trc10TokenAccountAddress));
    Assert.assertEquals(crossContract.getOwnerChainId(),chainId);
    Assert.assertEquals(crossContract.getToChainId(),crossChainId);
    Assert.assertEquals(crossContract.getType(), CrossDataType.TOKEN);

    Long afterAssetTotalSupply
        = PublicMethed.getAssetBalanceByAssetId(assetAccountId1, trc10TokenAccountKey,
        blockingStubFull);
    Assert.assertEquals((Long)(beforeAssetTotalSupply - sendAmount), afterAssetTotalSupply);

    //Query second chain
    byId = PublicMethed.getTransactionById(txid, crossBlockingStubFull);
    any = byId.get().getRawData().getContract(0).getParameter();
    crossContract = any.unpack(BalanceContract.CrossContract.class);
    Assert.assertEquals(crossContract.getOwnerAddress(),
        ByteString.copyFrom(trc10TokenAccountAddress));
    Assert.assertEquals(crossContract.getToAddress(),
        ByteString.copyFrom(trc10TokenAccountAddress));
    Assert.assertEquals(crossContract.getOwnerChainId(),chainId);
    Assert.assertEquals(crossContract.getToChainId(),crossChainId);
    Assert.assertEquals(crossContract.getType(), CrossDataType.TOKEN);

    final Long afterToNetUsaged = PublicMethed
        .queryAccount(trc10TokenAccountAddress,crossBlockingStubFull).getFreeNetUsage();
    Long afterFromTokenBalance = PublicMethed.getAssetBalanceByAssetId(assetAccountId1,
        trc10TokenAccountKey,blockingStubFull);
    Long afterToTokenBalance = PublicMethed.getAssetBalanceByAssetId(assetAccountId2,
        trc10TokenAccountKey,crossBlockingStubFull);
    Long afterFromBalance = PublicMethed.queryAccount(trc10TokenAccountAddress,blockingStubFull)
        .getBalance();
    Long afterToBalance = PublicMethed.queryAccount(trc10TokenAccountAddress,crossBlockingStubFull)
        .getBalance();
    Optional<TransactionInfo> info = PublicMethed.getTransactionInfoById(txid, blockingStubFull);

    Assert.assertEquals(beforeToBalance,afterToBalance);
    Assert.assertEquals((Long)(beforeFromTokenBalance - afterFromTokenBalance),sendAmount);
    Assert.assertEquals((Long)(afterToTokenBalance - beforeToTokenBalance),sendAmount);
    Assert.assertEquals((Long)(beforeFromBalance - afterFromBalance),(Long)info.get().getFee());
    Assert.assertTrue(info.get().getFee() > 0L);
    info = PublicMethed.getTransactionInfoById(txid, crossBlockingStubFull);
    Assert.assertTrue((Long)(afterToNetUsaged - beforeToNetUsaged)
        >= (Long)info.get().getReceipt().getNetUsage() - 5);


    final Long afterBlockNum = blockingStubFull.getNowBlock(EmptyMessage.newBuilder()
        .build()).getBlockHeader().getRawData().getNumber();


    List<CrossMessage> crossMessageList =  getCrossMessageListFromTargetRange(beforeBlockNum,
        afterBlockNum,blockingStubFull);
    Assert.assertEquals(crossMessageList.size(),2);

    CrossMessage firstCrossMessage = crossMessageList.get(0);
    CrossMessage secondCrossMessage = crossMessageList.get(1);

    Assert.assertEquals(firstCrossMessage.getTransaction().getRawData().getContract(0),
        secondCrossMessage.getTransaction().getRawData().getContract(0));
    Assert.assertEquals(secondCrossMessage.getTransaction().getRawData().getSourceTxId(),
        ByteString.copyFrom(ByteArray.fromHexString(txid)));
    Assert.assertEquals(secondCrossMessage.getType(), Type.ACK);



    final Long beforeSendBackFromAssetBalance = PublicMethed
        .getAssetBalanceByAssetId(assetAccountId2,
        trc10TokenAccountKey,crossBlockingStubFull);
    final Long beforeSendBackToAssetBalance = PublicMethed.getAssetBalanceByAssetId(assetAccountId1,
        trc10TokenAccountKey,blockingStubFull);
    final Long beforeSendBackAssetTotalSupply = PublicMethed
        .getAssetBalanceByAssetId(assetAccountId1, trc10TokenAccountKey, blockingStubFull);

    txid = createCrossTrc10TransferByHttp(trc10TokenAccountAddress,
        trc10TokenAccountAddress,assetAccountId1,chainId,6,sendAmount - 1,name1,
        crossChainId,chainId,
        trc10TokenAccountKey,crossHttpnode);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(crossBlockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(crossBlockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(crossBlockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(crossBlockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(crossBlockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(crossBlockingStubFull);

    afterAssetTotalSupply
        = PublicMethed.getAssetBalanceByAssetId(assetAccountId1,
        trc10TokenAccountKey, blockingStubFull);
    Assert.assertEquals((Long)(beforeSendBackAssetTotalSupply + sendAmount - 1),
        afterAssetTotalSupply);


    Long afterSendBackFromBalance = PublicMethed.getAssetBalanceByAssetId(assetAccountId2,
        trc10TokenAccountKey,crossBlockingStubFull);
    Long afterSendBackToBalance = PublicMethed.getAssetBalanceByAssetId(assetAccountId1,
        trc10TokenAccountKey,blockingStubFull);

    Assert.assertEquals(beforeSendBackFromAssetBalance - afterSendBackFromBalance, sendAmount - 1);
    Assert.assertEquals(afterSendBackToBalance - beforeSendBackToAssetBalance,sendAmount - 1);



    Assert.assertEquals(PublicMethed.getAssetIssueById(assetAccountId1.toStringUtf8(),
        blockingStubFull).getId(), assetAccountId1.toStringUtf8());
    Assert.assertEquals(PublicMethed.getAssetIssueById(assetAccountIdCrossChain.toStringUtf8(),
        crossBlockingStubFull).getId(), assetAccountIdCrossChain.toStringUtf8());
    Assert.assertEquals(PublicMethed.getAssetIssueById(assetAccountId2.toStringUtf8(),
        crossBlockingStubFull).getId(), assetAccountId2.toStringUtf8());






  }
}


