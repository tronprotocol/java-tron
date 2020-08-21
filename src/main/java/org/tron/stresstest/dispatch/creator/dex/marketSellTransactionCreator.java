package org.tron.stresstest.dispatch.creator.dex;

import com.google.protobuf.ByteString;
import java.util.concurrent.atomic.AtomicLong;
import lombok.Setter;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Configuration;
import org.tron.core.Wallet;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.stresstest.dispatch.GoodCaseTransactonCreator;
import org.tron.stresstest.dispatch.TransactionFactory;
import org.tron.stresstest.dispatch.creator.CreatorCounter;
import org.tron.stresstest.dispatch.creator.transfer.AbstractTransferTransactionCreator;

@Setter
public class marketSellTransactionCreator extends AbstractTransferTransactionCreator implements GoodCaseTransactonCreator {

  private String assetName = commontokenid;
  private String ownerAddress = Configuration.getByPath("stress.conf").getString("address.assetIssueOwnerAddress");
  private String toAddress = commonToAddress;
  private long amount = 1L;
  private long sellTokenQuantity = 10;
  private long buyTokenQuantity = 10;
  private String privateKey = Configuration.getByPath("stress.conf").getString("privateKey.assetIssueOwnerKey");
  public static AtomicLong createMarketSellCount = new AtomicLong();

  @Override
  protected Protocol.Transaction create() {
    createMarketSellCount.incrementAndGet();

    TransactionFactory.context.getBean(CreatorCounter.class).put(this.getClass().getName());


    ownerAddress =  Configuration.getByPath("stress.conf")
        .getString("dexAccount.dexAccount" + createMarketSellCount.get()%10 + "Address");
    privateKey = Configuration.getByPath("stress.conf")
        .getString("dexAccount.dexAccount" + createMarketSellCount.get()%10 + "Key");


    if(createMarketSellCount.get() % 2 == 0L) {
      Contract.MarketSellAssetContract contract = Contract.MarketSellAssetContract.newBuilder()
          .setOwnerAddress(ByteString.copyFrom(Wallet.decodeFromBase58Check(ownerAddress)))
          .setSellTokenId(ByteString.copyFrom(assetName.getBytes()))
          .setSellTokenQuantity(sellTokenQuantity)
          .setBuyTokenId(ByteString.copyFrom("_".getBytes()))
          .setBuyTokenQuantity(buyTokenQuantity)
          .build();

      Protocol.Transaction transaction = createTransaction(contract, ContractType.MarketSellAssetContract);

      transaction = sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(privateKey)));
      return transaction;
    } else {
      Contract.MarketSellAssetContract contract = Contract.MarketSellAssetContract.newBuilder()
          .setOwnerAddress(ByteString.copyFrom(Wallet.decodeFromBase58Check(ownerAddress)))
          .setBuyTokenId(ByteString.copyFrom(assetName.getBytes()))
          .setSellTokenQuantity(sellTokenQuantity)
          .setSellTokenId(ByteString.copyFrom("_".getBytes()))
          .setBuyTokenQuantity(buyTokenQuantity)
          .build();

      Protocol.Transaction transaction = createTransaction(contract, ContractType.MarketSellAssetContract);

      transaction = sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(privateKey)));
      return transaction;
    }






  }
}
