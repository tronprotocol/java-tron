package org.tron.stresstest.dispatch.creator.contract;

import lombok.Setter;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.protos.Contract.CreateSmartContract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.stresstest.dispatch.AbstractTransactionCreator;
import org.tron.stresstest.dispatch.GoodCaseTransactonCreator;
import org.tron.stresstest.dispatch.TransactionFactory;
import org.tron.stresstest.dispatch.creator.CreatorCounter;

@Setter
public class DeployContractTransactionCreator extends AbstractTransactionCreator implements
    GoodCaseTransactonCreator {

  private String contractName = "createContract";
  private String ownerAddress = commonOwnerAddress;
  private String abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"a\",\"type\":\"int256\"},{\"name\":\"b\",\"type\":\"int256\"}],\"name\":\"multiply\",\"outputs\":[{\"name\":\"output\",\"type\":\"int256\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"name\":\"from\",\"type\":\"address\"},{\"indexed\":false,\"name\":\"a\",\"type\":\"int256\"},{\"indexed\":false,\"name\":\"b\",\"type\":\"int256\"},{\"indexed\":false,\"name\":\"output\",\"type\":\"int256\"}],\"name\":\"MultiplyEvent\",\"type\":\"event\"}]";
  private String code = "6080604052348015600f57600080fd5b5060e98061001e6000396000f300608060405260043610603e5763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416633c4308a881146043575b600080fd5b348015604e57600080fd5b50605b600435602435606d565b60408051918252519081900360200190f35b60408051338152602081018490528082018390528383026060820181905291517feb4e4c25ee4bb2b9466eb38f13989c0c221efa6f1c631b8b4820f00afcf5a3e59181900360800190a1929150505600a165627a7a723058200dbf85f2b87350cd0aaa578b300b50d62fb3508880a151d2db70356c1fe463da0029";
  private long value = 0L;
  private long consumeUserResourcePercent = 100L;
  private String libraryAddressPair = null;
  private long feeLimit = 10000000L;
  private String privateKey = commonOwnerPrivateKey;

  @Override
  protected Protocol.Transaction create() {
    byte[] ownerAddressBytes = Wallet.decodeFromBase58Check(ownerAddress);

    TransactionFactory.context.getBean(CreatorCounter.class).put(this.getClass().getName());

    CreateSmartContract contract = org.tron.stresstest.dispatch.CreateSmartContract.createContractDeployContract(
        contractName,
        ownerAddressBytes,
        abi,
        code,
        value,
        consumeUserResourcePercent,
        libraryAddressPair
    );

    Protocol.Transaction transaction = createTransaction(contract, ContractType.CreateSmartContract);

    transaction = transaction.toBuilder().setRawData(transaction.getRawData().toBuilder().setFeeLimit(feeLimit).build()).build();

    transaction = sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(privateKey)));
    return transaction;
  }
}
