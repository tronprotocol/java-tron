package org.tron.stresstest.dispatch.creator.contract;

import java.util.concurrent.atomic.AtomicInteger;
import lombok.Setter;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.program.FullNode;
import org.tron.protos.Contract.TriggerSmartContract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Transaction.Result.code;
import org.tron.stresstest.AbiUtil;
import org.tron.stresstest.dispatch.AbstractTransactionCreator;
import org.tron.stresstest.dispatch.GoodCaseTransactonCreator;
import org.tron.stresstest.dispatch.TransactionFactory;
import org.tron.stresstest.dispatch.creator.CreatorCounter;
import org.tron.stresstest.exception.EncodingException;

@Setter
public class TriggerJustLendContractCreator extends AbstractTransactionCreator implements GoodCaseTransactonCreator {

  private String ownerAddress = triggerOwnerAddress;
  private String contractAddress = commonContractAddress4;
  private long callValue = 0L;
  private String methodSign = "borrow(uint256)";
  private boolean hex = false;
  private String param = "1";
  private long feeLimit = 1000000000L;
  private String privateKey = triggerOwnerKey;
  private static AtomicInteger contractIndex = new AtomicInteger(0);
  private Long[] borrow_amount = {
      100000L,
      100000L,
      10000000000000000L,
      10000000000000000L,
      10000000000000L,
      10000000000000000L,
      10000000000000000L,
      10000000000000000L,
      10000000000000000L,
      10000000000000000L,
      100000L,
      1000L,
      10000000000000000L,
      100000L
  };



  @Override
  protected Protocol.Transaction create() {
    byte[] ownerAddressBytes = Wallet.decodeFromBase58Check(ownerAddress);

    TransactionFactory.context.getBean(CreatorCounter.class).put(this.getClass().getName());

    TriggerSmartContract contract = null;

    contractAddress = FullNode.JustLendcontractList.get(contractIndex.getAndAdd(1) % 14);
    param = "" + borrow_amount[contractIndex.getAndAdd(1) % 14 ];
    try {

      contract = triggerCallContract(
          ownerAddressBytes, Wallet.decodeFromBase58Check(contractAddress), callValue, Hex.decode(AbiUtil.parseMethod(methodSign, param, hex)));
    } catch (EncodingException e) {
      e.printStackTrace();
    }

    Protocol.Transaction transaction = createTransaction(contract, ContractType.TriggerSmartContract);

    transaction = transaction.toBuilder().setRawData(transaction.getRawData().toBuilder().setFeeLimit(feeLimit).build()).build();
    TransactionResultCapsule ret = new TransactionResultCapsule();

    ret.setStatus(0, code.SUCESS);
    transaction = transaction.toBuilder().addRet(ret.getInstance())
        .build();
    transaction = sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(privateKey)));
    return transaction;
  }
}
