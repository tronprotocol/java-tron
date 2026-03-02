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
import org.tron.core.services.jsonrpc.types.BlockResult;
import org.tron.protos.Protocol;

public class BlockResultTest extends BaseTest {

  @Resource
  private Wallet wallet;

  static {
    Args.setParam(new String[]{"-d", dbPath()}, Constant.TEST_CONF);
  }

  @Test
  public void testBlockResult() {
    Protocol.Transaction.raw.Builder raw = Protocol.Transaction.raw.newBuilder();
    Protocol.Transaction.Contract.Builder contract = Protocol.Transaction.Contract.newBuilder();
    contract.setType(Protocol.Transaction.Contract.ContractType.UpdateBrokerageContract);
    raw.addContract(contract.build());

    Protocol.Transaction transaction = Protocol.Transaction.newBuilder().setRawData(raw).build();
    BlockCapsule blockCapsule = new BlockCapsule(Protocol.Block.newBuilder().setBlockHeader(
        Protocol.BlockHeader.newBuilder().setRawData(Protocol.BlockHeader.raw.newBuilder()
            .setParentHash(ByteString.copyFrom(ByteArray.fromHexString(
                    "0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b82")))
            .setNumber(0))).addTransactions(transaction).build());

    BlockResult blockResult = new BlockResult(blockCapsule.getInstance(),true, wallet);
    Assert.assertEquals(blockResult.getHash(),
        "0x000000000000000036393ced0658419d3c251bc14ffab8d10c8b0898451054fa");
    Assert.assertEquals(blockResult.getTransactions().length, 1);
    Assert.assertEquals(blockResult.getGasUsed(),"0x0");
  }

}
