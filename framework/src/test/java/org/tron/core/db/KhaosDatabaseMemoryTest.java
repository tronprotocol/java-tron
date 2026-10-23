package org.tron.core.db;

import com.google.protobuf.ByteString;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.utils.ReflectUtils;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.TransactionRetCapsule;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;

public class KhaosDatabaseMemoryTest {

  @Test
  public void unprocessedBlockAndQueriesDoNotRetainDecodedTransactions() throws Exception {
    KhaosDatabase database = new KhaosDatabase();
    BlockCapsule root = new BlockCapsule(100, Sha256Hash.ZERO_HASH, 100, ByteString.EMPTY);
    database.start(root);
    BlockCapsule header = new BlockCapsule(101, root.getBlockId(), 101, ByteString.EMPTY);
    Block.Builder builder = header.getInstance().toBuilder();
    for (int i = 0; i < 10000; i++) {
      builder.addTransactions(Transaction.getDefaultInstance());
    }
    BlockCapsule block = new BlockCapsule(builder.build());
    database.push(block);
    KhaosDatabase.KhaosBlock stored = database.getMiniStore().getByHash(block.getBlockId());
    Assert.assertTrue("Unprocessed blocks must not retain a decoded capsule",
        ReflectUtils.getFieldObject(stored, "blk") == null);

    BlockCapsule firstRead = database.getBlock(block.getBlockId());
    BlockCapsule secondRead = database.getBlock(block.getBlockId());
    Assert.assertEquals(block.getInstance(), firstRead.getInstance());
    Assert.assertEquals(10000, secondRead.getTransactions().size());
    Assert.assertNotSame(firstRead, secondRead);
    Assert.assertNull(ReflectUtils.getFieldObject(stored, "blk"));
    Assert.assertEquals(root.getBlockId(), stored.getParentHash());
    Assert.assertEquals(1,
        database.getBranch(block.getBlockId(), root.getBlockId()).getKey().size());
    Assert.assertNull(ReflectUtils.getFieldObject(stored, "blk"));
  }

  @Test
  public void storedBytesAreIndependentOfIncomingCapsule() throws Exception {
    KhaosDatabase database = new KhaosDatabase();
    BlockCapsule root = new BlockCapsule(100, Sha256Hash.ZERO_HASH, 100, ByteString.EMPTY);
    database.start(root);
    BlockCapsule block = new BlockCapsule(101, root.getBlockId(), 101, ByteString.EMPTY);
    database.push(block);
    Block original = block.getInstance();
    block.addTransaction(new TransactionCapsule(Transaction.getDefaultInstance()));

    Assert.assertEquals(original, database.getBlock(block.getBlockId()).getInstance());
  }

  @Test
  public void committedBlockRetainsExecutionResultsAndReleasesBytes() throws Exception {
    BlockCapsule block = new BlockCapsule(101, Sha256Hash.ZERO_HASH, 101, ByteString.EMPTY);
    KhaosDatabase.KhaosBlock stored = new KhaosDatabase.KhaosBlock(block);

    TransactionRetCapsule result = new TransactionRetCapsule(block);
    block.setResult(result);
    Assert.assertNull(stored.getBlk().getResult());
    stored.commit(block);

    Assert.assertSame(block, stored.getBlk());
    Assert.assertSame(result, stored.getBlk().getResult());
    Assert.assertNull(ReflectUtils.getFieldObject(stored, "rawData"));
  }

  @Test
  public void uncommittedChangesAreNotRetainedAndCanRetry() throws Exception {
    BlockCapsule block = new BlockCapsule(101, Sha256Hash.ZERO_HASH, 101, ByteString.EMPTY);
    block.generatedByMyself = true;
    block.addTransaction(new TransactionCapsule(Transaction.getDefaultInstance()));
    KhaosDatabase.KhaosBlock stored = new KhaosDatabase.KhaosBlock(block);

    BlockCapsule executing = stored.getBlk();
    Assert.assertTrue(executing.generatedByMyself);
    Assert.assertNotSame(executing, stored.getBlk());
    executing.setResult(new TransactionRetCapsule(executing));
    executing.getTransactions().clear();

    Assert.assertNull(ReflectUtils.getFieldObject(stored, "blk"));
    Assert.assertArrayEquals(block.getData(),
        (byte[]) ReflectUtils.getFieldObject(stored, "rawData"));
    BlockCapsule retry = stored.getBlk();
    Assert.assertNotSame(executing, retry);
    Assert.assertNull(retry.getResult());
    Assert.assertEquals(1, retry.getTransactions().size());
    Assert.assertNull(ReflectUtils.getFieldObject(stored, "blk"));
  }

  @Test
  public void commitRejectsDifferentBlockWithoutReplacingStoredData() throws Exception {
    BlockCapsule block = new BlockCapsule(101, Sha256Hash.ZERO_HASH, 101, ByteString.EMPTY);
    KhaosDatabase.KhaosBlock stored = new KhaosDatabase.KhaosBlock(block);
    BlockCapsule other = new BlockCapsule(102, block.getBlockId(), 102, ByteString.EMPTY);

    Assert.assertThrows(IllegalArgumentException.class, () -> stored.commit(other));

    Assert.assertEquals(block.getInstance(), stored.getBlk().getInstance());
    Assert.assertNull(ReflectUtils.getFieldObject(stored, "blk"));
    Assert.assertArrayEquals(block.getData(),
        (byte[]) ReflectUtils.getFieldObject(stored, "rawData"));
  }

  @Test
  public void startupHeadIsAlreadyProcessed() throws Exception {
    KhaosDatabase database = new KhaosDatabase();
    BlockCapsule root = new BlockCapsule(100, Sha256Hash.ZERO_HASH, 100, ByteString.EMPTY);
    database.start(root);
    Assert.assertSame(root, database.getHead());
    Assert.assertEquals(root.getBlockId(), database.getHeadId());
    Assert.assertNull(ReflectUtils.getFieldObject(
        database.getMiniStore().getByHash(root.getBlockId()), "rawData"));
  }
}
