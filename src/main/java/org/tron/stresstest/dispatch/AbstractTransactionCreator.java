package org.tron.stresstest.dispatch;

import com.google.common.base.Charsets;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import lombok.Getter;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.ECKey.ECDSASignature;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.stresstest.dispatch.strategy.Level2Strategy;

@Getter
public abstract class AbstractTransactionCreator extends Level2Strategy {
  protected String privateKey = "b6d8d3382c32d4d066c4f830a7e53c3da9ad8b9665dda4ca081b6cd4e807d09c";
  protected ByteString ownerAddress = ByteString.copyFrom(Wallet.decodeFromBase58Check("TRxETQim3Jn5TYqLeAnpyF5XdQeg7NUcSJ"));
  protected byte[] ownerAddressBytes = Wallet.decodeFromBase58Check("TRxETQim3Jn5TYqLeAnpyF5XdQeg7NUcSJ");
  protected ByteString toAddress = ByteString.copyFrom(Wallet.decodeFromBase58Check("TRxgBU7HFTQvU6zPheLHphqpwhDKNxB6Rk"));
  protected Long amount = 1L;
  protected Long amountOneTrx = 1000_000L;
  protected ByteString assetName = ByteString.copyFrom("1001521".getBytes());

  // exchange
  protected long exchangeId = 1;
  protected byte[] firstTokeID = "_".getBytes();
  protected byte[] secondTokeID = "1001521".getBytes();
  protected long quant = 10;
  protected long expected = 1;

  // inject
  protected long exchangeId2 = 2;
  protected byte[] firstTokeID2 = "_".getBytes();
  protected long quant2 = 1000000;

  // participate
  protected String ownerPrivateKey = "763009595dd132aaf2d248999f2c6e7ba0acbbd9a9dfd88f7c2c158d97327645";
  protected byte[] participateOwnerAddressBytes = Wallet.decodeFromBase58Check("TRxgBU7HFTQvU6zPheLHphqpwhDKNxB6Rk");
  protected long participateAmount = 1;
  protected byte[] participateAssetName = "1001521".getBytes();

  long time = System.currentTimeMillis();
  AtomicLong count = new AtomicLong();
  public Transaction createTransaction(com.google.protobuf.Message message,
      ContractType contractType) {
    Transaction.raw.Builder transactionBuilder = Transaction.raw.newBuilder().addContract(
        Transaction.Contract.newBuilder().setType(contractType).setParameter(
            Any.pack(message)).build());

    Transaction transaction = Transaction.newBuilder().setRawData(transactionBuilder.build())
        .build();

    long gTime = count.incrementAndGet() + time;
    String ref = "" + gTime;

    transaction = setReference(transaction, gTime, ByteArray.fromString(ref));

    transaction = setExpiration(transaction, gTime);

    return transaction;
  }

  private Transaction setReference(Transaction transaction, long blockNum,
      byte[] blockHash) {
    byte[] refBlockNum = ByteArray.fromLong(blockNum);
    Transaction.raw rawData = transaction.getRawData().toBuilder()
        .setRefBlockHash(ByteString.copyFrom(blockHash))
        .setRefBlockBytes(ByteString.copyFrom(refBlockNum))
        .build();
    return transaction.toBuilder().setRawData(rawData).build();
  }

  public Transaction setExpiration(Transaction transaction, long expiration) {
    Transaction.raw rawData = transaction.getRawData().toBuilder().setExpiration(expiration)
        .build();
    return transaction.toBuilder().setRawData(rawData).build();
  }

  public static Transaction sign(Transaction transaction, ECKey myKey) {
    Transaction.Builder transactionBuilderSigned = transaction.toBuilder();
    byte[] hash = Sha256Hash.hash(transaction.getRawData().toByteArray());
    List<Contract> listContract = transaction.getRawData().getContractList();
    for (int i = 0; i < listContract.size(); i++) {
      ECDSASignature signature = myKey.sign(hash);
      ByteString bsSign = ByteString.copyFrom(signature.toByteArray());
      transactionBuilderSigned.addSignature(
          bsSign);//Each contract may be signed with a different private key in the future.
    }

    transaction = transactionBuilderSigned.build();
    return transaction;
  }

  public static org.tron.protos.Contract.ExchangeTransactionContract createExchangeTransactionContract(byte[] owner,
      long exchangeId, byte[] tokenId, long quant, long expected) {
    org.tron.protos.Contract.ExchangeTransactionContract.Builder builder = org.tron.protos.Contract.ExchangeTransactionContract
        .newBuilder();
    builder
        .setOwnerAddress(ByteString.copyFrom(owner))
        .setExchangeId(exchangeId)
        .setTokenId(ByteString.copyFrom(tokenId))
        .setQuant(quant)
        .setExpected(expected);
    return builder.build();
  }

  public static Sha256Hash getID(Transaction transaction) {
    return Sha256Hash.of(transaction.getRawData().toByteArray());
  }

  public static org.tron.protos.Contract.ExchangeInjectContract createExchangeInjectContract(byte[] owner,
      long exchangeId, byte[] tokenId, long quant) {
    org.tron.protos.Contract.ExchangeInjectContract.Builder builder = org.tron.protos.Contract.ExchangeInjectContract.newBuilder();
    builder
        .setOwnerAddress(ByteString.copyFrom(owner))
        .setExchangeId(exchangeId)
        .setTokenId(ByteString.copyFrom(tokenId))
        .setQuant(quant);
    return builder.build();
  }

  public static org.tron.protos.Contract.ExchangeWithdrawContract createExchangeWithdrawContract(byte[] owner,
      long exchangeId, byte[] tokenId, long quant) {
    org.tron.protos.Contract.ExchangeWithdrawContract.Builder builder = org.tron.protos.Contract.ExchangeWithdrawContract
        .newBuilder();
    builder
        .setOwnerAddress(ByteString.copyFrom(owner))
        .setExchangeId(exchangeId)
        .setTokenId(ByteString.copyFrom(tokenId))
        .setQuant(quant);
    return builder.build();
  }

  public static org.tron.protos.Contract.ParticipateAssetIssueContract createParticipateAssetIssueContract(byte[] to,
      byte[] assertName, byte[] owner,
      long amount) {
    org.tron.protos.Contract.ParticipateAssetIssueContract.Builder builder = org.tron.protos.Contract.ParticipateAssetIssueContract
        .newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsName = ByteString.copyFrom(assertName);
    ByteString bsOwner = ByteString.copyFrom(owner);
    builder.setToAddress(bsTo);
    builder.setAssetName(bsName);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);

    return builder.build();
  }

  public org.tron.protos.Contract.TriggerSmartContract triggerCallContract(byte[] address,
      byte[] contractAddress,
      long callValue, byte[] data) {
    org.tron.protos.Contract.TriggerSmartContract.Builder builder = org.tron.protos.Contract.TriggerSmartContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(address));
    builder.setContractAddress(ByteString.copyFrom(contractAddress));
    builder.setData(ByteString.copyFrom(data));
    builder.setCallValue(callValue);
    return builder.build();
  }
}
