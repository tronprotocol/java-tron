package org.tron.program.generate;

import com.google.protobuf.ByteString;
import lombok.Setter;
import org.bouncycastle.util.encoders.Hex;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.Hash;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Commons;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.program.GenerateTransaction;
import org.tron.program.design.factory.Creator;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Transaction.Result.code;
import org.tron.protos.contract.SmartContractOuterClass.TriggerSmartContract;

import java.util.concurrent.atomic.AtomicInteger;

@Setter
@Creator(type = "trc20")
public class TransferTrc20Creator extends AbstractTransactionCreator implements TransactionCreator {

  private String ownerAddress = "";
  private String contractAddress = "";
  private long callValue = 0L;
  private String methodSign = "transfer(address,uint256)";
  private long feeLimit = 1000000000L;
  private String privateKey = "";
  private static AtomicInteger contractIndex = new AtomicInteger(0);


  @Override
  public Protocol.Transaction create() {
    byte[] ownerAddressBytes = Commons.decodeFromBase58Check(ownerAddress);

    String curAccount = GenerateTransaction.accountQueue.poll();
    String param = "\"" + curAccount + "\",1";
    GenerateTransaction.accountQueue.offer(curAccount);

    TriggerSmartContract contract = triggerCallContract(
            ownerAddressBytes, Commons.decodeFromBase58Check(contractAddress), callValue, Hex.decode(parseSelector(methodSign) + param));
    Protocol.Transaction transaction = createTransaction(contract, ContractType.TriggerSmartContract);
    transaction = transaction
            .toBuilder()
            .setRawData(
                    transaction
                            .getRawData()
                            .toBuilder()
                            .setFeeLimit(feeLimit)
                            .setData(
                                    ByteString.copyFromUtf8(getRandomLengthStr(contractIndex.get() % 4)))
                            .build())
            .build();

    TransactionResultCapsule ret = new TransactionResultCapsule();

    ret.setStatus(0, code.SUCESS);
    transaction = transaction.toBuilder().addRet(ret.getInstance())
            .build();
    return sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(privateKey)));
  }

  public static String getRandomLengthStr(long n) {
    StringBuilder result = new StringBuilder();
    for (int i = 1; i <= n; i++) {
      result.append('a');
    }

    return result.toString();
  }

  private TriggerSmartContract triggerCallContract(byte[] address,
                                                   byte[] contractAddress,
                                                   long callValue, byte[] data) {
    TriggerSmartContract.Builder builder = TriggerSmartContract
            .newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(address));
    builder.setContractAddress(ByteString.copyFrom(contractAddress));
    builder.setData(ByteString.copyFrom(data));
    builder.setCallValue(callValue);
    builder.setTokenId(Long.parseLong("0"));
    builder.setCallTokenValue(0L);
    return builder.build();
  }

  public static String parseSelector(String methodSign) {
    byte[] selector = new byte[4];
    System.arraycopy(Hash.sha3(methodSign.getBytes()), 0, selector, 0, 4);
    return Hex.toHexString(selector);
  }

}
