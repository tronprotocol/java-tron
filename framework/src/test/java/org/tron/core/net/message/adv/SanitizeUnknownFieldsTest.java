package org.tron.core.net.message.adv;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.google.protobuf.ByteString;
import com.google.protobuf.UnknownFieldSet;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.tron.common.overlay.message.Message;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.BlockHeader;
import org.tron.protos.Protocol.Transaction;

/**
 * Verifies the {@code sanitize()} helpers on {@link BlockCapsule},
 * {@link TransactionCapsule} and {@link BlockMessage}: they strip outer
 * unknown protobuf fields while leaving every consensus-hashed / signed
 * region byte-identical.
 */
public class SanitizeUnknownFieldsTest {

  private static final UnknownFieldSet PADDING = UnknownFieldSet.newBuilder()
      .addField(99999, UnknownFieldSet.Field.newBuilder()
          .addLengthDelimited(ByteString.copyFrom(new byte[1024]))
          .build())
      .build();

  @BeforeClass
  public static void setUp() {
    // BlockMessage(byte[]) calls Message.isFilter() which dereferences the
    // static DynamicPropertiesStore. The mock's primitive-long getter returns
    // 0L by default, so isFilter() returns false.
    Message.setDynamicPropertiesStore(Mockito.mock(DynamicPropertiesStore.class));
  }

  private static BlockHeader.raw sampleRawHeader() {
    return BlockHeader.raw.newBuilder()
        .setNumber(100)
        .setTimestamp(123456789L)
        .build();
  }

  private static Block sampleBlock() {
    return Block.newBuilder()
        .setBlockHeader(BlockHeader.newBuilder().setRawData(sampleRawHeader()).build())
        .build();
  }

  private static Transaction sampleTransaction() {
    return Transaction.newBuilder()
        .setRawData(Transaction.raw.newBuilder().setTimestamp(123456789L).build())
        .build();
  }

  // ---- BlockCapsule.sanitize ----

  @Test
  public void blockCapsuleSanitizeStripsBlockLevelUnknownFields() {
    Block padded = sampleBlock().toBuilder().setUnknownFields(PADDING).build();
    BlockCapsule capsule = new BlockCapsule(padded);
    long originalSize = capsule.getData().length;

    assertTrue("sanitize() should report it mutated the capsule", capsule.sanitize());

    assertTrue("Block-level unknown fields should be stripped",
        capsule.getInstance().getUnknownFields().asMap().isEmpty());
    assertTrue("Sanitized capsule bytes should shrink",
        capsule.getData().length < originalSize);
  }

  @Test
  public void blockCapsuleSanitizeStripsBlockHeaderOuterUnknownFields() {
    BlockHeader paddedHeader = BlockHeader.newBuilder()
        .setRawData(sampleRawHeader())
        .setUnknownFields(PADDING)
        .build();
    Block padded = Block.newBuilder().setBlockHeader(paddedHeader).build();
    BlockCapsule capsule = new BlockCapsule(padded);
    long originalSize = capsule.getData().length;

    assertTrue("sanitize() should report it mutated the capsule", capsule.sanitize());

    assertTrue("BlockHeader outer unknown fields should be stripped",
        capsule.getInstance().getBlockHeader().getUnknownFields().asMap().isEmpty());
    assertTrue(capsule.getData().length < originalSize);
  }

  @Test
  public void blockCapsuleSanitizePreservesBlockHeaderRawData() {
    Block clean = sampleBlock();
    Block padded = clean.toBuilder().setUnknownFields(PADDING).build();
    BlockCapsule capsule = new BlockCapsule(padded);

    capsule.sanitize();

    assertEquals("BlockHeader.raw_data must be byte-identical so block hash matches",
        clean.getBlockHeader().getRawData(),
        capsule.getInstance().getBlockHeader().getRawData());
  }

  @Test
  public void blockCapsuleSanitizeIsNoOpOnCleanBlock() {
    Block clean = sampleBlock();
    BlockCapsule capsule = new BlockCapsule(clean);
    Block beforeInstance = capsule.getInstance();
    byte[] beforeData = capsule.getData();

    assertFalse("sanitize() should report no-op on a clean block", capsule.sanitize());

    assertSame("Underlying Block reference should not be rebuilt",
        beforeInstance, capsule.getInstance());
    assertArrayEquals("Clean block should pass through unchanged", beforeData, capsule.getData());
  }

  // ---- TransactionCapsule.sanitize ----

  @Test
  public void transactionCapsuleSanitizeStripsTopLevelUnknownFields() {
    Transaction padded = sampleTransaction().toBuilder().setUnknownFields(PADDING).build();
    TransactionCapsule capsule = new TransactionCapsule(padded);
    long originalSize = capsule.getData().length;

    assertTrue("sanitize() should report it mutated the capsule", capsule.sanitize());

    assertTrue("Transaction-level unknown fields should be stripped",
        capsule.getInstance().getUnknownFields().asMap().isEmpty());
    assertTrue(capsule.getData().length < originalSize);
  }

  @Test
  public void transactionCapsuleSanitizePreservesTransactionId() {
    Transaction clean = sampleTransaction();
    Transaction padded = clean.toBuilder().setUnknownFields(PADDING).build();
    TransactionCapsule cleanCapsule = new TransactionCapsule(clean);
    TransactionCapsule paddedCapsule = new TransactionCapsule(padded);

    paddedCapsule.sanitize();

    assertEquals("Padding outside raw_data must not change the transaction id",
        cleanCapsule.getTransactionId(),
        paddedCapsule.getTransactionId());
  }

  @Test
  public void transactionCapsuleSanitizeIsNoOpOnCleanTransaction() {
    Transaction clean = sampleTransaction();
    TransactionCapsule capsule = new TransactionCapsule(clean);
    Transaction beforeInstance = capsule.getInstance();
    byte[] beforeData = capsule.getData();

    assertFalse("sanitize() should report no-op on a clean transaction", capsule.sanitize());

    assertSame("Underlying Transaction reference should not be rebuilt",
        beforeInstance, capsule.getInstance());
    assertArrayEquals(beforeData, capsule.getData());
  }

  // ---- BlockMessage.sanitize ----

  @Test
  public void blockMessageSanitizeUpdatesBothCapsuleAndWireBytes() throws Exception {
    Block padded = sampleBlock().toBuilder().setUnknownFields(PADDING).build();
    byte[] paddedBytes = padded.toByteArray();
    BlockMessage msg = new BlockMessage(paddedBytes);
    assertArrayEquals("Constructor should not sanitize on its own",
        paddedBytes, msg.getData());

    msg.sanitize();

    assertTrue("BlockCapsule should be sanitized",
        msg.getBlockCapsule().getInstance().getUnknownFields().asMap().isEmpty());
    assertTrue("msg.data should also be rewritten to canonical bytes",
        msg.getData().length < paddedBytes.length);
    assertArrayEquals("msg.data should equal capsule.getData() after sanitize",
        msg.getBlockCapsule().getData(), msg.getData());
    assertNotEquals("msg.data should no longer match the padded wire bytes",
        paddedBytes.length, msg.getData().length);
  }

  @Test
  public void blockMessageSanitizeSkipsDataRewriteOnCleanBlock() throws Exception {
    byte[] cleanBytes = sampleBlock().toByteArray();
    BlockMessage msg = new BlockMessage(cleanBytes);
    byte[] before = msg.getData();

    msg.sanitize();

    assertSame("msg.data should not be rewritten on the no-op path",
        before, msg.getData());
  }
}
