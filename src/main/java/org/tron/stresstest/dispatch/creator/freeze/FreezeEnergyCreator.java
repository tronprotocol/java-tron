package org.tron.stresstest.dispatch.creator.freeze;

import lombok.Setter;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.stresstest.dispatch.AbstractTransactionCreator;
import org.tron.stresstest.dispatch.GoodCaseTransactonCreator;
import org.tron.stresstest.dispatch.TransactionFactory;
import org.tron.stresstest.dispatch.creator.CreatorCounter;

@Setter
public class FreezeEnergyCreator extends AbstractTransactionCreator implements GoodCaseTransactonCreator {

  private String ownerAddress = commonOwnerAddress;
  private long frozenBalance = 1000000L;
  private long frozenDuration = 3L;
  private int resourceCode = 1;
  private String delegateAddress = delegateResourceAddress;
  private String privateKey = commonOwnerPrivateKey;



  @Override
  protected Protocol.Transaction create() {
    byte[] ownerAddressBytes = Wallet.decodeFromBase58Check(ownerAddress);

    TransactionFactory.context.getBean(CreatorCounter.class).put(this.getClass().getName());
    Contract.FreezeBalanceContract contract = createFreezeBalanceContract(ownerAddressBytes, frozenBalance, frozenDuration, resourceCode, delegateAddress);
    Protocol.Transaction transaction = createTransaction(contract, ContractType.FreezeBalanceContract);
    transaction = sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(privateKey)));
    return transaction;
  }
}
