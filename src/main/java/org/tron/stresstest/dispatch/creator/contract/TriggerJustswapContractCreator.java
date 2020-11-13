package org.tron.stresstest.dispatch.creator.contract;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import lombok.Setter;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.Base58;
import org.tron.common.utils.ByteArray;
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
public class TriggerJustswapContractCreator extends AbstractTransactionCreator implements GoodCaseTransactonCreator {

  static{
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    java.util.Date date = null;
    try {
      date = df.parse("2025-12-07");
    } catch (ParseException e) {
      e.printStackTrace();
    }
    Calendar cal = Calendar.getInstance();
    cal.setTime(date);
    long timestamp = cal.getTimeInMillis();
    deadline = timestamp / 1000;
  }

  static long deadline;
  private String ownerAddress = triggerOwnerAddress;
  private String jstContractAddress = jstAddress;
  private String tstExchangeContractAddress = tstExchangeAddress;
  private long callValue = 0L;
  private String methodSign = "tokenToTokenSwapInput(uint256,uint256,uint256,uint256,address)";
  private boolean hex = false;
  private String param = "\"20000\",\"1\",\"1\",\"" + deadline + "\",\"" + jstContractAddress + "\"";
  private long feeLimit = 1000000000L;
  private String privateKey = triggerOwnerKey;

  @Override
  protected Protocol.Transaction create() {
    Random rand = new Random();
    Integer randNum = rand.nextInt(30) + 1;
    randNum = rand.nextInt(4000);
    callValue = randNum;

    byte[] ownerAddressBytes = Wallet.decodeFromBase58Check(ownerAddress);

    TransactionFactory.context.getBean(CreatorCounter.class).put(this.getClass().getName());

    TriggerSmartContract contract = null;
    try {
      contract = triggerCallContract(
          ownerAddressBytes,
          Wallet.decodeFromBase58Check(tstExchangeContractAddress),
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

    Protocol.Transaction transaction = createTransaction(contract, ContractType.TriggerSmartContract);

    transaction = transaction.toBuilder().setRawData(transaction.getRawData().toBuilder().setFeeLimit(feeLimit).build()).build();

    transaction = sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(privateKey)));
    return transaction;
  }
}
