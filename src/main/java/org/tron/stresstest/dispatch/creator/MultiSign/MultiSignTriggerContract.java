package org.tron.stresstest.dispatch.creator.MultiSign;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.Setter;
import org.spongycastle.util.encoders.Hex;
import org.tron.api.GrpcAPI;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import org.tron.stresstest.AbiUtil;
import org.tron.stresstest.dispatch.AbstractTransactionCreator;
import org.tron.stresstest.dispatch.GoodCaseTransactonCreator;
import org.tron.stresstest.dispatch.TransactionFactory;
import org.tron.stresstest.dispatch.creator.CreatorCounter;
import org.tron.stresstest.exception.EncodingException;
import org.tron.common.utils.Configuration;


@Setter
public class MultiSignTriggerContract extends AbstractTransactionCreator implements GoodCaseTransactonCreator {
    private String ownerAddress = Configuration.getByPath("stress.conf").getString("address.mutiSignOwnerAddress");
    private String privateKey = Configuration.getByPath("stress.conf").getString("privateKey.mutiSignOwnerKey");
    private String[] permissionKeyString = new String[5];
    private String contractAddress = Configuration.getByPath("stress.conf")
        .getString("address.commonContractAddress1");
    private long callValue = 0L;
    private String methodSign = "TransferTokenTo(address,trcToken,uint256)";
    private boolean hex = false;
    private String param = "\"" + commonContractAddress2 + "\",\"" + commontokenid + "\",1";

    private long feeLimit = 1000000000L;


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

        byte[] ownerAddressBytes = Wallet.decodeFromBase58Check(ownerAddress);

        TransactionFactory.context.getBean(CreatorCounter.class).put(this.getClass().getName());

        Contract.TriggerSmartContract contract = null;
        try {

            contract = triggerCallContract(
                    ownerAddressBytes, Wallet.decodeFromBase58Check(contractAddress), callValue, Hex.decode(AbiUtil.parseMethod(methodSign, param, hex)));
        } catch (EncodingException e) {
            e.printStackTrace();
        }

        Protocol.Transaction transaction = createTransaction(contract, Protocol.Transaction.Contract.ContractType.TriggerSmartContract);

        transaction = transaction.toBuilder().setRawData(transaction.getRawData().toBuilder().setFeeLimit(feeLimit).build()).build();
        TransactionResultCapsule ret = new TransactionResultCapsule();

        ret.setStatus(0, Protocol.Transaction.Result.code.SUCESS);
        transaction = transaction.toBuilder().addRet(ret.getInstance())
                .build();
        //transaction = sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(privateKey)));
        transaction = mutiSignNew(transaction, permissionKeyString);
        return transaction;
    }
}