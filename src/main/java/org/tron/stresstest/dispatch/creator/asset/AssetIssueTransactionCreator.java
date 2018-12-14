package org.tron.stresstest.dispatch.creator.asset;

import java.util.HashMap;
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
public class AssetIssueTransactionCreator extends AbstractTransactionCreator implements GoodCaseTransactonCreator {

  private String ownerAddress = commonOwnerAddress;
  private String assetName = "create";
  private long totalSupply = 1000;
  private int trxNum = 1;
  private int icoNum = 1;
  private int precision = 0;
  private int voteScore = 0;
  private String description = "create asset issue";
  private String url = "www.create.com";
  private long freeNetLimit = 100000L;
  private long publicFreeNetLimit = 1000000L;
  private String privateKey = commonOwnerPrivateKey;

  @Override
  protected Protocol.Transaction create() {
    byte[] ownerAddressBytes = Wallet.decodeFromBase58Check(ownerAddress);

    TransactionFactory.context.getBean(CreatorCounter.class).put(this.getClass().getName());
    Contract.AssetIssueContract contract = createAssetIssueContract(
        ownerAddressBytes,
        assetName,
        totalSupply,
        trxNum,
        icoNum,
        precision,
        System.currentTimeMillis() + 60 * 60 * 24 * 30,
        System.currentTimeMillis() + 60 * 60 * 24 * 60,
        voteScore,
        description,
        url,
        freeNetLimit,
        publicFreeNetLimit,
        new HashMap<>()
    );
    Protocol.Transaction transaction = createTransaction(contract, ContractType.AssetIssueContract);
    transaction = sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(privateKey)));
    return transaction;
  }
}
