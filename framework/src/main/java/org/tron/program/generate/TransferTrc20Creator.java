package org.tron.program.generate;

import com.google.protobuf.ByteString;
import org.bouncycastle.util.encoders.Hex;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Commons;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.exception.EncodingException;
import org.tron.program.GenerateTransaction;
import org.tron.program.design.factory.Creator;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Transaction.Result.code;
import org.tron.protos.contract.SmartContractOuterClass.TriggerSmartContract;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author liukai
 * @since 2022/9/9.
 */
@Creator(type = "trc20")
public class TransferTrc20Creator extends AbstractTransactionCreator implements TransactionCreator {

  private String ownerAddress = "TRRJbGw8BC8S5ueuium2aTBuRrLnkytnUi";
  // for test usdt
  private String contractAddress = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t";
  private long callValue = 0L;
  private String methodSign = "transfer(address,uint256)";
  private long feeLimit = 1000000000L;
  private String privateKey = "44FE180410D7BF05E41388A881C3C5566C6667840116EC25C6FC924CE678FC4A";
  private static AtomicInteger contractIndex = new AtomicInteger(0);
  private boolean hex = false;


  @Override
  public Protocol.Transaction create() {
    byte[] ownerAddressBytes = Commons.decodeFromBase58Check(ownerAddress);
    String curAccount = GenerateTransaction.accountQueue.poll();
    String param = "\"" + curAccount + "\",1";
    GenerateTransaction.accountQueue.offer(curAccount);

    TriggerSmartContract contract = null;
    try {
      contract = triggerCallContract(
              ownerAddressBytes, Commons.decodeFromBase58Check(contractAddress), callValue, Hex.decode(parseMethod(methodSign, param, hex)));
    } catch (EncodingException e) {
      e.printStackTrace();
    }
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
                                                   long callValue,
                                                   byte[] data) {
    return TriggerSmartContract
            .newBuilder()
            .setOwnerAddress(ByteString.copyFrom(address))
            .setContractAddress(ByteString.copyFrom(contractAddress))
            .setData(ByteString.copyFrom(data))
            .setCallValue(callValue)
            .setTokenId(Long.parseLong("0"))
            .setCallTokenValue(0L)
            .build();
  }

}
