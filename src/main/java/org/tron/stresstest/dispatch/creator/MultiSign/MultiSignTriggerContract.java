package org.tron.stresstest.dispatch.creator.MultiSign;

import lombok.Setter;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import org.tron.stresstest.AbiUtil;
import org.tron.stresstest.dispatch.AbstractTransactionCreator;
import org.tron.stresstest.dispatch.GoodCaseTransactonCreator;
import org.tron.stresstest.dispatch.TransactionFactory;
import org.tron.stresstest.dispatch.creator.CreatorCounter;
import org.tron.stresstest.exception.EncodingException;



@Setter
public class MultiSignTriggerContract extends AbstractTransactionCreator implements GoodCaseTransactonCreator {

    private String ownerAddress = "TYtZeP1Xnho7LKcgeNsTY2Xg3LTpjfF6G5";
    private String ownerKey = "795D7F7A3120132695DFB8977CC3B7ACC9770C125EB69037F19DCA55B075C4AE";

    private String contractAddress = commonContractAddress1;
    private long callValue = 0L;
    private String methodSign = "TransferTokenTo(address,trcToken,uint256)";
    private boolean hex = false;
    private String param = "\"" + commonContractAddress2 + "\",1000001,1";
    //  private String param = "\"" + Wallet.encode58Check(commonContractAddress2.getBytes()) + "\",1000001,1";
    private long feeLimit = 1000000000L;
    private String privateKey = commonOwnerPrivateKey;

    @Override
    protected Protocol.Transaction create() {
        byte[] ownerAddressBytes = Wallet.decodeFromBase58Check(ownerAddress);

        TransactionFactory.context.getBean(CreatorCounter.class).put(this.getClass().getName());

        Contract.TriggerSmartContract contract = null;
        try {

            contract = triggerCallContract(
                    ownerAddressBytes, Wallet.decodeFromBase58Check(contractAddress), callValue, Hex.decode(AbiUtil.parseMethod(methodSign, param, hex)));
//          ownerAddressBytes, contractAddress.getBytes(), callValue, Hex.decode(AbiUtil.parseMethod(methodSign, param, hex)));
        } catch (EncodingException e) {
            e.printStackTrace();
        }

        String[] permissionKeyString = new String[2];

        permissionKeyString[0] = "795D7F7A3120132695DFB8977CC3B7ACC9770C125EB69037F19DCA55B075C4AE";

        permissionKeyString[1] = "76fb5f55710c7ad6a98f73dd38a732f9a69a7b3ce700a694363a50572fa2842a";

        //permissionKeyString[2] = "549c7797b351e48ab1c6bb5857138b418012d97526fc2acba022357d49c93ac0";


        Protocol.Transaction transaction = createTransaction(contract, Protocol.Transaction.Contract.ContractType.TriggerSmartContract);

        transaction = transaction.toBuilder().setRawData(transaction.getRawData().toBuilder().setFeeLimit(feeLimit).build()).build();

        transaction = Multisign(transaction, permissionKeyString);
        return transaction;
    }
}