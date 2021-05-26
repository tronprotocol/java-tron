package stest.tron.wallet.dailybuild.crosschain;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.BalanceContract;
import org.tron.protos.contract.BalanceContract.CrossContract.CrossDataType;
import stest.tron.wallet.common.client.utils.CrossChainBase;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class CrossChainTrc10 extends CrossChainBase {

  private Long sendAmount = 12L;

  @Test(enabled = true,description = "Create cross token for cross chain")
  public void test01CreateCrossToken() throws InvalidProtocolBufferException {
    final Long beforeFromTokenBalance = PublicMethed
        .getAssetBalanceByAssetId(assetAccountId1, trc10TokenAccountKey,blockingStubFull);
    final Long beforeToTokenBalance = PublicMethed
        .getAssetBalanceByAssetId(assetAccountId2, trc10TokenAccountKey,crossBlockingStubFull);
    final Long beforeFromBalance = PublicMethed
        .queryAccount(trc10TokenAccountAddress,blockingStubFull).getBalance();

    String txid = createCrossTrc10Transfer(trc10TokenAccountAddress,
        trc10TokenAccountAddress,assetAccountId1,6,sendAmount,name1,chainId,crossChainId,
        trc10TokenAccountKey,blockingStubFull);
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

    Long afterFromTokenBalance = PublicMethed.getAssetBalanceByAssetId(assetAccountId1,
        trc10TokenAccountKey,blockingStubFull);
    Long afterToTokenBalance = PublicMethed.getAssetBalanceByAssetId(assetAccountId2,
        trc10TokenAccountKey,crossBlockingStubFull);
    Long afterFromBalance = PublicMethed.queryAccount(trc10TokenAccountAddress,blockingStubFull)
        .getBalance();
    Optional<TransactionInfo> info = PublicMethed.getTransactionInfoById(txid, blockingStubFull);

    Assert.assertEquals((Long)(beforeFromTokenBalance - afterFromTokenBalance),sendAmount);
    Assert.assertEquals((Long)(afterToTokenBalance - beforeToTokenBalance),sendAmount);
    Assert.assertEquals((Long)(beforeFromBalance - afterFromBalance),(Long)info.get().getFee());
  }
}


