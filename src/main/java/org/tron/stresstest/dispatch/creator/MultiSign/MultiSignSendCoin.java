package org.tron.stresstest.dispatch.creator.MultiSign;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.tron.api.WalletGrpc;
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
    private WalletGrpc.WalletBlockingStub blockingStubFull = commonblockingStubFull;
    private String[] permissionKeyString = new String[5];

    //TXtrbmfwZ2LxtoCveEhZT86fTss1w8rwJE
    String manager1Key = "0528dc17428585fc4dece68b79fa7912270a1fe8e85f244372f59eb7e8925e04";
    //TWKKwLswTTcK5cp31F2bAteQrzU8cYhtU5
    String manager2Key = "553c7b0dee17d3f5b334925f5a90fe99fb0b93d47073d69ec33eead8459d171e";
    //TT4MHXVApKfbcq7cDLKnes9h9wLSD4eMJi
    String manager3Key = "324a2052e491e99026442d81df4d2777292840c1b3949e20696c49096c6bacb8";
    //TCw4yb4hS923FisfMsxAzQ85srXkK6RWGk
    String manager4Key = "ff5d867c4434ac17d264afc6696e15365832d5e8000f75733ebb336d66df148d";
    //TLYUrci5Qw5fUPho2GvFv38kAK4QSmdhhN
    String manager5Key = "2925e186bb1e88988855f11ebf20ea3a6e19ed92328b0ffb576122e769d45b68";

    @Override
    protected Protocol.Transaction create() {
        permissionKeyString[0] = manager1Key;
        permissionKeyString[1] = manager2Key;
        permissionKeyString[2] = manager3Key;
        permissionKeyString[3] = manager4Key;
        permissionKeyString[4] = manager5Key;
        TransactionFactory.context.getBean(CreatorCounter.class).put(this.getClass().getName());

        Contract.TransferContract contract = Contract.TransferContract.newBuilder()
                .setOwnerAddress(ByteString.copyFrom(Wallet.decodeFromBase58Check(ownerAddress)))
                .setToAddress(ByteString.copyFrom(Wallet.decodeFromBase58Check(toAddress)))
                .setAmount(amount)
                .build();
        Protocol.Transaction transaction = createTransaction(contract, Protocol.Transaction.Contract.ContractType.TransferContract);
        transaction = mutiSignNew(transaction, permissionKeyString);

//        transaction = sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(privateKey)));
        return transaction;
    }
}
