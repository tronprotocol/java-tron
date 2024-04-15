package org.tron.core.db;

import static org.junit.Assert.assertEquals;
import static org.tron.protos.Protocol.Transaction.Contract.ContractType.TransferContract;
import static org.tron.protos.Protocol.Transaction.Result.contractResult.SUCCESS;

import com.google.protobuf.ByteString;
import java.util.Arrays;
import javax.annotation.Resource;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.utils.ByteArray;
import org.tron.core.Constant;
import org.tron.core.capsule.BlockBalanceTraceCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.store.BalanceTraceStore;
import org.tron.protos.Protocol;
import org.tron.protos.contract.BalanceContract;


public class BalanceTraceStoreTest extends BaseTest {

  @Resource
  private BalanceTraceStore balanceTraceStoreUnderTest;

  private static final byte[] contractAddr = Hex.decode(
      "41000000000000000000000000000000000000dEaD");

  BlockCapsule blockCapsule = new BlockCapsule(Protocol.Block.newBuilder().setBlockHeader(
      Protocol.BlockHeader.newBuilder().setRawData(Protocol.BlockHeader.raw.newBuilder()
          .setParentHash(ByteString.copyFrom(ByteArray.fromHexString(
              "0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b81"))))).build());
  final TransactionCapsule transactionCapsule =
      new TransactionCapsule(Protocol.Transaction.newBuilder().build());
  BalanceContract.TransactionBalanceTrace transactionBalanceTrace =
      BalanceContract.TransactionBalanceTrace.newBuilder()
          .setTransactionIdentifier(transactionCapsule.getTransactionId().getByteString())
          .setType(TransferContract.name())
          .setStatus(SUCCESS.name())
          .build();

  static {
    Args.setParam(
        new String[]{
            "--output-directory", dbPath()
        },
        Constant.TEST_CONF
    );
  }

  @Before
  public void clear() {
    balanceTraceStoreUnderTest.resetCurrentTransactionTrace();
    balanceTraceStoreUnderTest.resetCurrentBlockTrace();
  }


  @Test
  public void testSetCurrentTransactionId() throws Exception {
    balanceTraceStoreUnderTest.setCurrentBlockId(blockCapsule);
    balanceTraceStoreUnderTest.setCurrentTransactionId(transactionCapsule);
    Assert.assertEquals(balanceTraceStoreUnderTest.getCurrentTransactionId(),
        transactionCapsule.getTransactionId());
  }

  @Test
  public void testSetCurrentBlockId() {
    balanceTraceStoreUnderTest.setCurrentBlockId(blockCapsule);
    Assert.assertEquals(blockCapsule.getBlockId(), balanceTraceStoreUnderTest.getCurrentBlockId());
  }

  @Test
  public void testResetCurrentTransactionTrace() {
    balanceTraceStoreUnderTest.setCurrentBlockId(blockCapsule);
    balanceTraceStoreUnderTest.setCurrentTransactionId(transactionCapsule);
    balanceTraceStoreUnderTest.resetCurrentTransactionTrace();
    balanceTraceStoreUnderTest.resetCurrentBlockTrace();
    Assert.assertNotNull(balanceTraceStoreUnderTest.getCurrentTransactionId());
    Assert.assertNull(balanceTraceStoreUnderTest.getCurrentTransactionBalanceTrace());
  }

  @Test
  public void testInitCurrentBlockBalanceTrace() {
    balanceTraceStoreUnderTest.initCurrentBlockBalanceTrace(blockCapsule);
    Assert.assertNull(balanceTraceStoreUnderTest.getCurrentBlockId());
  }

  @Test
  public void testInitCurrentTransactionBalanceTrace() {
    balanceTraceStoreUnderTest.setCurrentBlockId(blockCapsule);
    balanceTraceStoreUnderTest.initCurrentTransactionBalanceTrace(transactionCapsule);
    Assert.assertEquals(blockCapsule.getBlockId(), balanceTraceStoreUnderTest.getCurrentBlockId());
    Assert.assertNull(balanceTraceStoreUnderTest.getCurrentTransactionId());
  }

  @Test
  public void testUpdateCurrentTransactionStatus() {
    balanceTraceStoreUnderTest.setCurrentBlockId(blockCapsule);
    balanceTraceStoreUnderTest.updateCurrentTransactionStatus("");
    Assert.assertNull(balanceTraceStoreUnderTest.getCurrentTransactionBalanceTrace());
  }

  @Test
  public void testGetBlockBalanceTrace() throws Exception {
    BlockBalanceTraceCapsule blockBalanceTraceCapsule = new BlockBalanceTraceCapsule(blockCapsule);
    balanceTraceStoreUnderTest.put(ByteArray.fromLong(blockCapsule.getNum()),
        blockBalanceTraceCapsule);
    final BlockBalanceTraceCapsule result =
        balanceTraceStoreUnderTest.getBlockBalanceTrace(blockCapsule.getBlockId());
    assertEquals(Arrays.toString(result.getData()),
        Arrays.toString(blockBalanceTraceCapsule.getData()));
  }

  @Test
  public void testGetTransactionBalanceTrace() throws Exception {
    BlockBalanceTraceCapsule blockBalanceTraceCapsule = new BlockBalanceTraceCapsule(blockCapsule);
    blockBalanceTraceCapsule.addTransactionBalanceTrace(transactionBalanceTrace);
    balanceTraceStoreUnderTest.put(ByteArray.fromLong(blockCapsule.getNum()),
        blockBalanceTraceCapsule);
    final BalanceContract.TransactionBalanceTrace result =
        balanceTraceStoreUnderTest.getTransactionBalanceTrace(blockCapsule.getBlockId(),
            transactionCapsule.getTransactionId());
    Assert.assertEquals(result.getStatus(),"SUCCESS");
  }

}