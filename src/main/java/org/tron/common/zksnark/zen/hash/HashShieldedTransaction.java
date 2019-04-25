package org.tron.common.zksnark.zen.hash;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.protos.Contract.ShieldedTransferContract;
import org.tron.protos.Contract.SpendDescription;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;


public class HashShieldedTransaction {

  public static byte[] hash(TransactionCapsule tx) throws InvalidProtocolBufferException {
    Any contractParameter = tx.getInstance().getRawData().getContract(0).getParameter();
    ShieldedTransferContract shieldedTransferContract = contractParameter
        .unpack(ShieldedTransferContract.class);
    ShieldedTransferContract.Builder newContract = ShieldedTransferContract.newBuilder();
    newContract.setFromAmount(shieldedTransferContract.getFromAmount());
    newContract.addAllReceiveDescription(shieldedTransferContract.getReceiveDescriptionList());
    newContract.setToAmount(shieldedTransferContract.getToAmount());
    newContract.setValueBalance(shieldedTransferContract.getValueBalance());
    newContract.setTransparentFromAddress(shieldedTransferContract.getTransparentFromAddress());
    newContract.setTransparentToAddress(shieldedTransferContract.getTransparentToAddress());
    for (SpendDescription spendDescription : shieldedTransferContract.getSpendDescriptionList()) {
      newContract
          .addSpendDescription(spendDescription.toBuilder().clearSpendAuthoritySignature().build());
    }

    Transaction.raw.Builder rawBuilder = tx.getInstance().toBuilder()
        .getRawDataBuilder()
        .clearContract()
        .addContract(
            Transaction.Contract.newBuilder().setType(ContractType.ShieldedTransferContract)
                .setParameter(
                    Any.pack(newContract.build())).build());

    Transaction transaction = tx.getInstance().toBuilder().clearRawData()
        .setRawData(rawBuilder).build();

    return Sha256Hash.of(transaction.getRawData().toByteArray())
        .getBytes();
  }
}
