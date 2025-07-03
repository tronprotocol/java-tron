package org.tron.core.services.jsonrpc;

import com.google.protobuf.ByteString;
import javax.annotation.Resource;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.utils.ByteArray;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionRetCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.exception.JsonRpcInternalException;
import org.tron.core.services.jsonrpc.types.TransactionReceipt;
import org.tron.core.services.jsonrpc.types.TransactionReceipt.TransactionContext;
import org.tron.core.store.TransactionRetStore;
import org.tron.protos.Protocol;

public class TransactionReceiptTest extends BaseTest {

  @Resource private Wallet wallet;

  @Resource private TransactionRetStore transactionRetStore;

  static {
    Args.setParam(new String[] {"-d", dbPath()}, Constant.TEST_CONF);
  }

  @Test
  public void testTransactionReceipt() throws JsonRpcInternalException {
    Protocol.TransactionInfo transactionInfo = Protocol.TransactionInfo.newBuilder()
        .setId(ByteString.copyFrom("1".getBytes()))
        .setContractAddress(ByteString.copyFrom("address1".getBytes()))
        .setReceipt(Protocol.ResourceReceipt.newBuilder()
            .setEnergyUsageTotal(100L)
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

    Protocol.Block block = Protocol.Block.newBuilder().setBlockHeader(
        Protocol.BlockHeader.newBuilder().setRawData(
            Protocol.BlockHeader.raw.newBuilder().setNumber(1))).addTransactions(
        transaction).build();

    BlockCapsule blockCapsule = new BlockCapsule(block);
    long energyFee = wallet.getEnergyFee(blockCapsule.getTimeStamp());
    TransactionReceipt.TransactionContext context
        = new TransactionContext(0, 2, 3);

    TransactionReceipt transactionReceipt =
        new TransactionReceipt(blockCapsule, transactionInfo, context, energyFee);

    Assert.assertNotNull(transactionReceipt);
    String blockHash = "0x0000000000000001464f071c8a336fd22eb5145dff1b245bda013ec89add8497";

    // assert basic fields
    Assert.assertEquals(transactionReceipt.getBlockHash(), blockHash);
    Assert.assertEquals(transactionReceipt.getBlockNumber(), "0x1");
    Assert.assertEquals(transactionReceipt.getTransactionHash(), "0x31");
    Assert.assertEquals(transactionReceipt.getTransactionIndex(), "0x0");
    Assert.assertEquals(transactionReceipt.getCumulativeGasUsed(), ByteArray.toJsonHex(102));
    Assert.assertEquals(transactionReceipt.getGasUsed(), ByteArray.toJsonHex(100));
    Assert.assertEquals(transactionReceipt.getEffectiveGasPrice(), ByteArray.toJsonHex(energyFee));
    Assert.assertEquals(transactionReceipt.getStatus(), "0x1");

    // assert contract fields
    Assert.assertEquals(transactionReceipt.getFrom(), ByteArray.toJsonHexAddress(new byte[0]));
    Assert.assertEquals(transactionReceipt.getTo(), ByteArray.toJsonHexAddress(new byte[0]));
    Assert.assertNull(transactionReceipt.getContractAddress());

    // assert logs fields
    Assert.assertEquals(transactionReceipt.getLogs().length, 1);
    Assert.assertEquals(transactionReceipt.getLogs()[0].getLogIndex(), "0x3");
    Assert.assertEquals(
        transactionReceipt.getLogs()[0].getBlockHash(), blockHash);
    Assert.assertEquals(transactionReceipt.getLogs()[0].getBlockNumber(), "0x1");
    Assert.assertEquals(transactionReceipt.getLogs()[0].getTransactionHash(), "0x31");
    Assert.assertEquals(transactionReceipt.getLogs()[0].getTransactionIndex(), "0x0");

    // assert default fields
    Assert.assertNull(transactionReceipt.getRoot());
    Assert.assertEquals(transactionReceipt.getType(), "0x0");
    Assert.assertEquals(transactionReceipt.getLogsBloom(), ByteArray.toJsonHex(new byte[256]));
  }
}
