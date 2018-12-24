package org.tron.stresstest.dispatch.creator.witness;

import lombok.Setter;
import java.util.HashMap;
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
public class ProposalCreator extends AbstractTransactionCreator implements GoodCaseTransactonCreator{

    private String ownerAddress = commonWitnessAddress;
    private String firstTokenID = "1000001";
    private long firstTokenBalance = 1L;
    private String secondTokenID = "_";
    private long secondTokenBalance = 1L;
    private String privateKey = commonWitnessPrivateKey;

    @Override
    protected Protocol.Transaction create() {
        byte[] ownerAddressBytes = Wallet.decodeFromBase58Check(ownerAddress);
        TransactionFactory.context.getBean(CreatorCounter.class).put(this.getClass().getName());
        HashMap<Long, Long> parametersMap = new HashMap<>();
        long id = 0L;
        long value = 81000L;
        parametersMap.put(id, value);
//        Contract.ExchangeCreateContract contract = createExchangeCreateContract(ownerAddressBytes, firstTokenID.getBytes(), firstTokenBalance, secondTokenID.getBytes(), secondTokenBalance);
        Contract.ProposalCreateContract contract = createProposalCreateContract(ownerAddressBytes,parametersMap);

        Protocol.Transaction transaction = createTransaction(contract, ContractType.ProposalCreateContract);

        transaction = sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(privateKey)));
        return transaction;
    }

}
