package org.tron.stresstest.dispatch.creator.MultiSign;

import com.google.protobuf.ByteString;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import org.tron.stresstest.dispatch.AbstractTransactionCreator;
import org.tron.stresstest.dispatch.GoodCaseTransactonCreator;
import org.tron.stresstest.dispatch.TransactionFactory;
import org.tron.stresstest.dispatch.creator.CreatorCounter;

public class MultiSignSendCoin extends AbstractTransactionCreator implements GoodCaseTransactonCreator {

    private String ownerAddress = "TYtZeP1Xnho7LKcgeNsTY2Xg3LTpjfF6G5";
    private String ownerKey = "795D7F7A3120132695DFB8977CC3B7ACC9770C125EB69037F19DCA55B075C4AE";
    private String toAddress = commonToAddress;
    private long amount = 1L;
    private String privateKey = commonWitnessPrivateKey;

    @Override
    protected Protocol.Transaction create() {
        TransactionFactory.context.getBean(CreatorCounter.class).put(this.getClass().getName());
        String[] permissionKeyString = new String[3];

        permissionKeyString[0] = "795D7F7A3120132695DFB8977CC3B7ACC9770C125EB69037F19DCA55B075C4AE";

        permissionKeyString[1] = "ff5d867c4434ac17d264afc6696e15365832d5e8000f75733ebb336d66df148d";

        permissionKeyString[2] = "2925e186bb1e88988855f11ebf20ea3a6e19ed92328b0ffb576122e769d45b68";


        Contract.TransferContract contract = Contract.TransferContract.newBuilder()
                .setOwnerAddress(ByteString.copyFrom(Wallet.decodeFromBase58Check(ownerAddress)))
                .setToAddress(ByteString.copyFrom(Wallet.decodeFromBase58Check(toAddress)))
                .setAmount(amount)
                .build();
        Protocol.Transaction transaction = createTransaction(contract, Protocol.Transaction.Contract.ContractType.TransferContract);

        transaction = Multisign(transaction, permissionKeyString);
        return transaction;
    }
}
