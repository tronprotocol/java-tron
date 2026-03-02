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
import org.tron.core.config.args.Args;
import org.tron.core.services.jsonrpc.types.TransactionResult;
import org.tron.protos.Protocol;

public class TransactionResultTest extends BaseTest {

  @Resource
  private Wallet wallet;

  static {
    Args.setParam(new String[]{"-d", dbPath()}, Constant.TEST_CONF);
  }

  @Test
  public void testBuildTransactionResultWithBlock() {
    Protocol.Transaction.raw.Builder raw = Protocol.Transaction.raw.newBuilder().addContract(
        Protocol.Transaction.Contract.newBuilder().setType(
            Protocol.Transaction.Contract.ContractType.TriggerSmartContract));
    Protocol.Transaction transaction = Protocol.Transaction.newBuilder().setRawData(raw).build();
    BlockCapsule blockCapsule = new BlockCapsule(Protocol.Block.newBuilder().setBlockHeader(
        Protocol.BlockHeader.newBuilder().setRawData(Protocol.BlockHeader.raw.newBuilder()
            .setParentHash(ByteString.copyFrom(ByteArray.fromHexString(
                "0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b82")))
            .setNumber(9))).addTransactions(transaction).build());

    TransactionResult transactionResult = new TransactionResult(blockCapsule,0, transaction,
        100,1, wallet);
    Assert.assertEquals(transactionResult.getBlockNumber(), "0x9");
    Assert.assertEquals(transactionResult.getHash(),
        "0xdebef90d0a8077620711b1b5af2b702665887ddcbf80868108026e1ab5e0bfb7");
    Assert.assertEquals(transactionResult.getGasPrice(), "0x1");
    Assert.assertEquals(transactionResult.getGas(), "0x64");
  }

  @Test
  public void testBuildTransactionResult() {
    Protocol.Transaction.raw.Builder raw = Protocol.Transaction.raw.newBuilder().addContract(
        Protocol.Transaction.Contract.newBuilder().setType(
            Protocol.Transaction.Contract.ContractType.TriggerSmartContract));
    Protocol.Transaction transaction = Protocol.Transaction.newBuilder().setRawData(raw).build();
    TransactionResult transactionResult = new TransactionResult(transaction, wallet);
    Assert.assertEquals(transactionResult.getHash(),
        "0xdebef90d0a8077620711b1b5af2b702665887ddcbf80868108026e1ab5e0bfb7");
    Assert.assertEquals(transactionResult.getGasPrice(), "0x");
    Assert.assertEquals(transactionResult.getNonce(), "0x0000000000000000");
  }

}
