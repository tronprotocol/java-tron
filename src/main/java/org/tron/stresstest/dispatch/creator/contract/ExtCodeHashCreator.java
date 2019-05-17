package org.tron.stresstest.dispatch.creator.contract;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.Setter;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.protos.Contract.TriggerSmartContract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Transaction.Result.code;
import org.tron.stresstest.AbiUtil2;
import org.tron.stresstest.dispatch.AbstractTransactionCreator;
import org.tron.stresstest.dispatch.GoodCaseTransactonCreator;
import org.tron.stresstest.dispatch.TransactionFactory;
import org.tron.stresstest.dispatch.creator.CreatorCounter;
import org.tron.stresstest.exception.EncodingException;

@Setter
public class ExtCodeHashCreator extends AbstractTransactionCreator implements
    GoodCaseTransactonCreator {

  private String ownerAddress = triggerOwnerAddress;
  private String contractAddress = commonContractAddress6;
  private long callValue = 0L;
  private String methodSign = "test(address[])";
  private boolean hex = false;
  //  private String param = "\"" + commonContractAddress2 + "\",1002136,1";
  private String param = null;
  private long feeLimit = 1000000000L;
  private String privateKey = triggerOwnerKey;

  @Override
  protected Protocol.Transaction create() {

    String paramAddress = commonContractAddress5;
    int paramSize = 50;
    List<String> params = new ArrayList<>();
    for (int i = 0; i < paramSize; i++) {
      params.add(paramAddress);
    }

    byte[] ownerAddressBytes = Wallet.decodeFromBase58Check(ownerAddress);

    TransactionFactory.context.getBean(CreatorCounter.class).put(this.getClass().getName());

    TriggerSmartContract contract = null;
    try {

      contract = triggerCallContract(
          ownerAddressBytes, Wallet.decodeFromBase58Check(contractAddress), callValue,
          Hex.decode(AbiUtil2.parseMethod(methodSign, Arrays.asList(params))));
    } catch (EncodingException e) {
      e.printStackTrace();
    }

    Protocol.Transaction transaction = createTransaction(contract,
        ContractType.TriggerSmartContract);

    transaction = transaction.toBuilder()
        .setRawData(transaction.getRawData().toBuilder().setFeeLimit(feeLimit).build()).build();
    TransactionResultCapsule ret = new TransactionResultCapsule();

    ret.setStatus(0, code.SUCESS);
    transaction = transaction.toBuilder().addRet(ret.getInstance())
        .build();
    transaction = sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(privateKey)));
    return transaction;
  }
}
