package org.tron.stresstest.dispatch.creator.exchange;

import java.util.concurrent.atomic.AtomicInteger;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.Setter;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.stresstest.dispatch.AbstractTransactionCreator;
import org.tron.stresstest.dispatch.GoodCaseTransactonCreator;
import org.tron.stresstest.dispatch.TransactionFactory;
import org.tron.stresstest.dispatch.creator.CreatorCounter;

@Setter
public class NiceExchangeTransactionCreator extends AbstractTransactionCreator implements GoodCaseTransactonCreator {
  AtomicInteger integer = new AtomicInteger(0);

  private String firstTokenID = "_";
  private ByteString secondTokenID;
  private String ownerAddress = commonOwnerAddress;
  private long exchangeID = 1L;
  private long quant = 10L;
  private long expected = 1L;
  private String privateKey = commonOwnerPrivateKey;
  private String fullnode = "47.94.239.172:50051";
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;


  @Override
  protected Protocol.Transaction create() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
            .usePlaintext(true)
            .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    ByteString addressBs = ByteString.copyFrom(ownerAddress.getBytes());
    Protocol.Account request = Protocol.Account.newBuilder().setAddress(addressBs).build();
    Account accountinfo = blockingStubFull.getAccount(request);
    secondTokenID=accountinfo.getAssetIssuedID();

    byte[] tokenId = firstTokenID.getBytes();
    if (integer.incrementAndGet() % 2 == 0) {
      tokenId = secondTokenID.toByteArray();
    }
    byte[] ownerAddressBytes = Wallet.decodeFromBase58Check(ownerAddress);

    TransactionFactory.context.getBean(CreatorCounter.class).put(this.getClass().getName());

    Contract.ExchangeTransactionContract contract = createExchangeTransactionContract(ownerAddressBytes,
        exchangeID, tokenId, quant, expected);

    Protocol.Transaction transaction = createTransaction(contract, ContractType.ExchangeTransactionContract);
    transaction = sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(privateKey)));
    return transaction;
  }
}
