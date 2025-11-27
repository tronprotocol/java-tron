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
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.services.jsonrpc.types.TransactionResult;
import org.tron.protos.Protocol;
import org.tron.protos.contract.SmartContractOuterClass;

public class TransactionResultTest extends BaseTest {

  @Resource
  private Wallet wallet;

  private static final String OWNER_ADDRESS = "41548794500882809695a8a687866e76d4271a1abc";
  private static final String CONTRACT_ADDRESS = "A0B4750E2CD76E19DCA331BF5D089B71C3C2798548";

  static {
    Args.setParam(new String[] {"-d", dbPath()}, Constant.TEST_CONF);
  }

  @Test
  public void testBuildTransactionResultWithBlock() {
    SmartContractOuterClass.TriggerSmartContract.Builder builder2 =
        SmartContractOuterClass.TriggerSmartContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setContractAddress(ByteString.copyFrom(ByteArray.fromHexString(CONTRACT_ADDRESS)));
    TransactionCapsule transactionCapsule = new TransactionCapsule(builder2.build(),
        Protocol.Transaction.Contract.ContractType.TriggerSmartContract);
    Protocol.Transaction transaction = transactionCapsule.getInstance();
    BlockCapsule blockCapsule = new BlockCapsule(Protocol.Block.newBuilder().setBlockHeader(
        Protocol.BlockHeader.newBuilder().setRawData(Protocol.BlockHeader.raw.newBuilder()
            .setParentHash(ByteString.copyFrom(ByteArray.fromHexString(
                "0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b82")))
            .setNumber(9))).addTransactions(transaction).build());

    TransactionResult transactionResult = new TransactionResult(blockCapsule, 0, transaction,
        100, 1, wallet);
    Assert.assertEquals(transactionResult.getBlockNumber(), "0x9");
    Assert.assertEquals("0x5691531881bc44adbc722060d85fdf29265823db8e884b0d104fcfbba253cf11",
        transactionResult.getHash());
    Assert.assertEquals(transactionResult.getGasPrice(), "0x1");
    Assert.assertEquals(transactionResult.getGas(), "0x64");
  }

  @Test
  public void testBuildTransactionResult() {
    SmartContractOuterClass.TriggerSmartContract.Builder builder2 =
        SmartContractOuterClass.TriggerSmartContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setContractAddress(ByteString.copyFrom(ByteArray.fromHexString(CONTRACT_ADDRESS)));
    TransactionCapsule transactionCapsule = new TransactionCapsule(builder2.build(),
        Protocol.Transaction.Contract.ContractType.TriggerSmartContract);

    TransactionResult transactionResult =
        new TransactionResult(transactionCapsule.getInstance(), wallet);
    Assert.assertEquals("0x5691531881bc44adbc722060d85fdf29265823db8e884b0d104fcfbba253cf11",
        transactionResult.getHash());
    Assert.assertEquals(transactionResult.getGasPrice(), "0x");
    Assert.assertEquals(transactionResult.getNonce(), "0x0000000000000000");
  }

}
