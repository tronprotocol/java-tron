package org.tron.program.generate;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.contract.AccountContract;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author liukai
 * @since 2022/9/9.
 */
public class AbstractTransactionCreator {

  private final long time = System.currentTimeMillis();
  private final AtomicLong count = new AtomicLong();

  public static Protocol.Transaction sign(Protocol.Transaction transaction, ECKey myKey) {
    Protocol.Transaction.Builder transactionBuilderSigned = transaction.toBuilder();
    byte[] hash = Sha256Hash.hash(true, transaction.getRawData().toByteArray());
    List<Protocol.Transaction.Contract> listContract = transaction.getRawData().getContractList();
    for (int i = 0; i < listContract.size(); i++) {
      ECKey.ECDSASignature signature = myKey.sign(hash);
      ByteString bsSign = ByteString.copyFrom(signature.toByteArray());
      transactionBuilderSigned.addSignature(
              bsSign);
    }

    transaction = transactionBuilderSigned.build();
    return transaction;
  }

  public org.tron.protos.contract.AccountContract.AccountCreateContract createAccountCreateContract(byte[] owner,
                                                                                    byte[] address) {
    AccountContract.AccountCreateContract.Builder builder = AccountContract.AccountCreateContract
            .newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setAccountAddress(ByteString.copyFrom(address));
    return builder.build();
  }

  public Transaction createTransaction(com.google.protobuf.Message message,
                                       ContractType contractType) {
    Transaction.raw.Builder transactionBuilder = Transaction.raw.newBuilder().addContract(
            Transaction.Contract.newBuilder()
                    .setType(contractType)
                    .setParameter(Any.pack(message))
                    .build()
    );

    Transaction transaction = Protocol.Transaction.newBuilder().setRawData(transactionBuilder.build())
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

}
