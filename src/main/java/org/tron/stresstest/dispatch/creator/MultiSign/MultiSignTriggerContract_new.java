package org.tron.stresstest.dispatch.creator.MultiSign;

import com.google.protobuf.ByteString;
import lombok.Setter;
import org.spongycastle.util.encoders.Hex;
import org.tron.api.WalletGrpc;
import org.tron.core.Wallet;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.protos.Contract;
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
public class MultiSignTriggerContract_new extends AbstractTransactionCreator implements GoodCaseTransactonCreator {
    private String ownerAddress = "TJrv22oMNzTQjwYWeN16nCweejN4Hkkh5e";
    private String[] permissionKeyString = new String[2];
    private String contractAddress = commonContractAddress1;
    private long callValue = 0L;
    private String methodSign = "TransferTokenTo(address,trcToken,uint256)";
    private boolean hex = false;
    private String param = "\"" + commonContractAddress2 + "\",1002107,1";
    private long feeLimit = 1000000000L;
    private String privateKey = "848a938229a04a39bf829fbdbc9fa7a3dafbf74d650e42e50f558f797c193ce6";
    @Override
    protected Protocol.Transaction create() {
        permissionKeyString[0] = "0528dc17428585fc4dece68b79fa7912270a1fe8e85f244372f59eb7e8925e04";
        permissionKeyString[1] = "553c7b0dee17d3f5b334925f5a90fe99fb0b93d47073d69ec33eead8459d171e";
        byte[] ownerAddressBytes = Wallet.decodeFromBase58Check(ownerAddress);

        TransactionFactory.context.getBean(CreatorCounter.class).put(this.getClass().getName());

        TriggerSmartContract contract = null;
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
        //transaction = sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(privateKey)));
        transaction = mutiSignNew(transaction, permissionKeyString);
        return transaction;
    }
}
