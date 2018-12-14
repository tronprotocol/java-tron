package org.tron.stresstest.dispatch.creator.asset;

import lombok.Setter;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.stresstest.dispatch.GoodCaseTransactonCreator;
import org.tron.stresstest.dispatch.TransactionFactory;
import org.tron.stresstest.dispatch.creator.CreatorCounter;
import org.tron.stresstest.dispatch.creator.transfer.AbstractTransferTransactionCreator;

@Setter
public class UpdateAssetTransactionCreator extends AbstractTransferTransactionCreator implements GoodCaseTransactonCreator {

  private String ownerAddress = commonOwnerAddress;
  private String assetName = "xxd";
  private String description = "wwwwwww";
  private long newLimit = 100000L;
  private long newPublicLimit = 1000000L;
  private String privateKey = commonOwnerPrivateKey;

  @Override
  protected Protocol.Transaction create() {
    byte[] ownerAddressBytes = Wallet.decodeFromBase58Check(ownerAddress);

    TransactionFactory.context.getBean(CreatorCounter.class).put(this.getClass().getName());

    Contract.UpdateAssetContract contract = createUpdateAssetContract(
        ownerAddressBytes,
        assetName.getBytes(),
        description.getBytes(),
        newLimit,
        newPublicLimit
    );
    Protocol.Transaction transaction = createTransaction(contract, ContractType.UpdateAssetContract);

    transaction = sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(privateKey)));
    return transaction;
  }
}
