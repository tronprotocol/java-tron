package org.tron.core.services.jsonrpc;

import com.google.protobuf.ByteString;
import javax.annotation.Resource;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.TestConstants;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionRetCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.exception.jsonrpc.JsonRpcInternalException;
import org.tron.core.services.jsonrpc.types.TransactionReceipt;
import org.tron.core.services.jsonrpc.types.TransactionReceipt.TransactionContext;
import org.tron.core.store.TransactionRetStore;
import org.tron.protos.Protocol;

public class TransactionReceiptTest extends BaseTest {

  @Resource private Wallet wallet;

  @Resource private TransactionRetStore transactionRetStore;

  static {
    Args.setParam(new String[] {"-d", dbPath()}, TestConstants.TEST_CONF);
  }

  @Test
  public void testTransactionReceipt() throws JsonRpcInternalException {
    Protocol.TransactionInfo transactionInfo = Protocol.TransactionInfo.newBuilder()
        .setId(ByteString.copyFrom("1".getBytes()))
        .setContractAddress(ByteString.copyFrom("address1".getBytes()))
        .setBlockTimeStamp(1000000L)
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
            Protocol.BlockHeader.raw.newBuilder()
                .setNumber(1)
                .setTimestamp(1000000L)))
        .addTransactions(transaction)
        .build();

    BlockCapsule blockCapsule = new BlockCapsule(block);
    long energyFee = wallet.getEnergyFee(blockCapsule.getTimeStamp());
    TransactionReceipt.TransactionContext context
        = new TransactionContext(0, 2, 3);

    TransactionReceipt transactionReceipt =
        new TransactionReceipt(blockCapsule, transactionInfo, context, energyFee);

    Assert.assertNotNull(transactionReceipt);
    String blockHash = "0x0000000000000001ba51f50f562758a449ff4a98df4febef89e122c1bb7e1a0c";

    // assert basic fields
    Assert.assertEquals(blockHash, transactionReceipt.getBlockHash());
    Assert.assertEquals("0x1", transactionReceipt.getBlockNumber());
    Assert.assertEquals("0x31", transactionReceipt.getTransactionHash());
    Assert.assertEquals("0x0", transactionReceipt.getTransactionIndex());
    Assert.assertEquals(ByteArray.toJsonHex(102), transactionReceipt.getCumulativeGasUsed());
    Assert.assertEquals(ByteArray.toJsonHex(100), transactionReceipt.getGasUsed());
    Assert.assertEquals(ByteArray.toJsonHex(energyFee), transactionReceipt.getEffectiveGasPrice());
    Assert.assertEquals("0x1", transactionReceipt.getStatus());

    // assert contract fields
    Assert.assertEquals(ByteArray.toJsonHexAddress(new byte[0]), transactionReceipt.getFrom());
    Assert.assertEquals(ByteArray.toJsonHexAddress(new byte[0]), transactionReceipt.getTo());
    Assert.assertNull(transactionReceipt.getContractAddress());

    // assert logs fields
    Assert.assertEquals(1, transactionReceipt.getLogs().length);
    Assert.assertEquals("0x3", transactionReceipt.getLogs()[0].getLogIndex());
    Assert.assertEquals(blockHash, transactionReceipt.getLogs()[0].getBlockHash());
    Assert.assertEquals("0x1", transactionReceipt.getLogs()[0].getBlockNumber());
    Assert.assertEquals("0x31", transactionReceipt.getLogs()[0].getTransactionHash());
    Assert.assertEquals("0x0", transactionReceipt.getLogs()[0].getTransactionIndex());
    Assert.assertEquals("0x3e8", transactionReceipt.getLogs()[0].getBlockTimestamp());

    // assert default fields
    Assert.assertNull(transactionReceipt.getRoot());
    Assert.assertEquals("0x0", transactionReceipt.getType());
    Assert.assertEquals(ByteArray.toJsonHex(new byte[256]), transactionReceipt.getLogsBloom());
  }
}
