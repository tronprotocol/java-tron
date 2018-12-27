package org.tron.stresstest.dispatch.creator.contract;

import lombok.Setter;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.protos.Contract.TriggerSmartContract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.stresstest.AbiUtil;
import org.tron.stresstest.dispatch.AbstractTransactionCreator;
import org.tron.stresstest.dispatch.GoodCaseTransactonCreator;
import org.tron.stresstest.dispatch.TransactionFactory;
import org.tron.stresstest.dispatch.creator.CreatorCounter;
import org.tron.stresstest.exception.EncodingException;
import org.tron.core.Wallet;

@Setter
public class TransferTokenCreator extends AbstractTransactionCreator implements GoodCaseTransactonCreator {

  private String ownerAddress = commonOwnerAddress;

  private String contractAddress = commonContractAddress1;
  private long callValue = 0L;
  private String methodSign = "TransferTokenTo(address,trcToken,uint256)";
  private boolean hex = false;
  private String param = "\"" + commonContractAddress2 + "\",1000001,1";
//  private String param = "\"" + Wallet.encode58Check(commonContractAddress2.getBytes()) + "\",1000001,1";
  private long feeLimit = 1000000000L;
  private String privateKey = commonOwnerPrivateKey;

  @Override
  protected Protocol.Transaction create() {
    byte[] ownerAddressBytes = Wallet.decodeFromBase58Check(ownerAddress);

    TransactionFactory.context.getBean(CreatorCounter.class).put(this.getClass().getName());

    TriggerSmartContract contract = null;
    try {

      contract = triggerCallContract(
          ownerAddressBytes, Wallet.decodeFromBase58Check(contractAddress), callValue, Hex.decode(AbiUtil.parseMethod(methodSign, param, hex)));
//          ownerAddressBytes, contractAddress.getBytes(), callValue, Hex.decode(AbiUtil.parseMethod(methodSign, param, hex)));
    } catch (EncodingException e) {
      e.printStackTrace();
    }

    Protocol.Transaction transaction = createTransaction(contract, ContractType.TriggerSmartContract);

    transaction = transaction.toBuilder().setRawData(transaction.getRawData().toBuilder().setFeeLimit(feeLimit).build()).build();

    transaction = sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(privateKey)));
    return transaction;
  }
}
