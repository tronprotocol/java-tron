package org.tron.core.services.jsonrpc;

import com.google.protobuf.ByteString;
import javax.annotation.Resource;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.utils.ByteArray;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.TransactionRetCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.services.jsonrpc.types.TransactionReceipt;
import org.tron.core.store.TransactionRetStore;
import org.tron.protos.Protocol;

public class TransactionReceiptTest extends BaseTest {

  @Resource
  private Wallet wallet;

  @Resource
  private TransactionRetStore transactionRetStore;

  static {
    Args.setParam(new String[]{"-d", dbPath()}, Constant.TEST_CONF);
  }

  @Test
  public void testTransactionReceipt() {
    Protocol.TransactionInfo transactionInfo = Protocol.TransactionInfo.newBuilder()
        .setId(ByteString.copyFrom("1".getBytes()))
        .setContractAddress(ByteString.copyFrom("address1".getBytes()))
        .setReceipt(Protocol.ResourceReceipt.newBuilder()
            .setEnergyUsageTotal(0L)
            .setResult(Protocol.Transaction.Result.contractResult.DEFAULT)
            .build())
        .addLog(Protocol.TransactionInfo.Log.newBuilder()
            .setAddress(ByteString.copyFrom("address1".getBytes()))
            .setData(ByteString.copyFrom("data".getBytes()))
            .build())
        .build();
    TransactionRetCapsule transactionRetCapsule = new TransactionRetCapsule();
    transactionRetCapsule.addTransactionInfo(transactionInfo);
    transactionRetStore.put(ByteArray.fromLong(1), transactionRetCapsule);

    Protocol.Transaction.raw.Builder raw = Protocol.Transaction.raw.newBuilder();
    Protocol.Transaction.Contract.Builder contract = Protocol.Transaction.Contract.newBuilder();
    contract.setType(Protocol.Transaction.Contract.ContractType.UpdateBrokerageContract);
    raw.addContract(contract.build());
    Protocol.Transaction transaction = Protocol.Transaction.newBuilder().setRawData(raw).build();

    TransactionReceipt transactionReceipt = new TransactionReceipt(
        Protocol.Block.newBuilder().setBlockHeader(
            Protocol.BlockHeader.newBuilder().setRawData(
                Protocol.BlockHeader.raw.newBuilder().setNumber(1))).addTransactions(
            transaction).build(), transactionInfo, wallet);

    Assert.assertEquals(transactionReceipt.getBlockNumber(),"0x1");
    Assert.assertEquals(transactionReceipt.getTransactionIndex(),"0x0");
    Assert.assertEquals(transactionReceipt.getLogs().length,1);
    Assert.assertEquals(transactionReceipt.getBlockHash(),
        "0x0000000000000001464f071c8a336fd22eb5145dff1b245bda013ec89add8497");
  }

}
