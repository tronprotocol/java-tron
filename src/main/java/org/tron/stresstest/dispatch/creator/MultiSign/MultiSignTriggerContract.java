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
import org.tron.protos.Contract.TransferContract;

import java.util.concurrent.TimeUnit;


@Setter
public class MultiSignTriggerContract extends AbstractTransactionCreator implements GoodCaseTransactonCreator {
    private String ownerAddress = "TYtZeP1Xnho7LKcgeNsTY2Xg3LTpjfF6G5";
    private String[] permissionKeyString = new String[5];
    private String contractAddress = commonContractAddress1;
    private long callValue = 0L;
    private String methodSign = "TransferTokenTo(address,trcToken,uint256)";
    private boolean hex = false;
    private String param = "\"" + commonContractAddress2 + "\",1002126,1";
    private long feeLimit = 1000000000L;
    private String privateKey = "795D7F7A3120132695DFB8977CC3B7ACC9770C125EB69037F19DCA55B075C4AE";

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