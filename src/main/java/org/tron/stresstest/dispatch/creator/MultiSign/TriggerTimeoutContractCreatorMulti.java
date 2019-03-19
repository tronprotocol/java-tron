package org.tron.stresstest.dispatch.creator.MultiSign;

import java.util.concurrent.atomic.AtomicLong;
import lombok.Setter;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.utils.Configuration;
import org.tron.core.Wallet;
import org.tron.protos.Contract.TriggerSmartContract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.stresstest.AbiUtil;
import org.tron.stresstest.dispatch.AbstractTransactionCreator;
import org.tron.stresstest.dispatch.GoodCaseTransactonCreator;
import org.tron.stresstest.dispatch.TransactionFactory;
import org.tron.stresstest.dispatch.creator.CreatorCounter;
import org.tron.stresstest.exception.EncodingException;

@Setter
public class TriggerTimeoutContractCreatorMulti extends AbstractTransactionCreator implements
    GoodCaseTransactonCreator {

  private String ownerAddress = Configuration.getByPath("stress.conf")
      .getString("address.mutiSignOwnerAddress");
  private String contractAddress = commonContractAddress3;
  private String[] permissionKeyString = new String[5];
  private long callValue = 0L;
  private String methodSign = "add2(uint256)";
  private boolean hex = false;
  private String param = "2100";
  private long feeLimit = 1000000000L;
  private String privateKey = triggerOwnerKey;
  public static AtomicLong queryCount = new AtomicLong();

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

    queryCount.incrementAndGet();
    param = "200";
    if (queryCount.get() % 240 == 0) {
      param = "2100";
    }
    byte[] ownerAddressBytes = Wallet.decodeFromBase58Check(ownerAddress);

    TransactionFactory.context.getBean(CreatorCounter.class).put(this.getClass().getName());

    TriggerSmartContract contract = null;
    try {
      contract = triggerCallContract(
          ownerAddressBytes,
          Wallet.decodeFromBase58Check(contractAddress),
//              contractAddress.getBytes(),
          callValue,
          Hex.decode(AbiUtil.parseMethod(
              methodSign,
              param,
              hex
          )));
    } catch (EncodingException e) {
      e.printStackTrace();
    }

    Protocol.Transaction transaction = createTransaction(contract,
        ContractType.TriggerSmartContract);

    transaction = transaction.toBuilder()
        .setRawData(transaction.getRawData().toBuilder().setFeeLimit(feeLimit).build()).build();

//    transaction = sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(privateKey)));
    transaction = mutiSignNew(transaction, permissionKeyString);
    return transaction;
  }
}