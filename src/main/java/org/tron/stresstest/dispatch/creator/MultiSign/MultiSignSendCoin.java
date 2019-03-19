package org.tron.stresstest.dispatch.creator.MultiSign;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Configuration;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import org.tron.stresstest.dispatch.AbstractTransactionCreator;
import org.tron.stresstest.dispatch.GoodCaseTransactonCreator;
import org.tron.stresstest.dispatch.TransactionFactory;
import org.tron.stresstest.dispatch.creator.CreatorCounter;

public class MultiSignSendCoin extends AbstractTransactionCreator implements GoodCaseTransactonCreator {

    private String ownerAddress = Configuration.getByPath("stress.conf").getString("address.mutiSignOwnerAddress");
    private String ownerKey = Configuration.getByPath("stress.conf").getString("privateKey.mutiSignOwnerKey");
    private String toAddress = commonToAddress;
    private long amount = 1L;
    private String privateKey = commonWitnessPrivateKey;
    private String[] permissionKeyString = new String[5];
    //TXtrbmfwZ2LxtoCveEhZT86fTss1w8rwJE
    String manager1Key = Configuration.getByPath("stress.conf").getString("permissioner.key1");
    //TWKKwLswTTcK5cp31F2bAteQrzU8cYhtU5
    String manager2Key = Configuration.getByPath("stress.conf").getString("permissioner.key2");
    //TT4MHXVApKfbcq7cDLKnes9h9wLSD4eMJi
    String manager3Key = Configuration.getByPath("stress.conf").getString("permissioner.key3");
    //TCw4yb4hS923FisfMsxAzQ85srXkK6RWGk
    String manager4Key = Configuration.getByPath("stress.conf").getString("permissioner.key4");
    //TLYUrci5Qw5fUPho2GvFv38kAK4QSmdhhN
    String manager5Key = Configuration.getByPath("stress.conf").getString("permissioner.key5");

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
