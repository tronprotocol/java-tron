package org.tron.core.capsule;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.Test;
import org.tron.common.crypto.SignUtils;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.common.utils.StringUtil;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.BlockHeader;
import org.tron.protos.Protocol.Transaction;

/**
 * Fixed ECKey outputs captured by running the pre-removal implementation at
 * 4d6c24085ab151d8c7ef821ff0e7ad2b7f168733 with crypto.engine = "eckey".
 *
 * <p>The serialized inputs pin all protobuf fields, including timestamps and block version.
 * The transaction transfers 1,234,567 SUN at timestamp 1,600,000,000,000; the block has
 * height 123,456 and contains that signed transaction. The private key is a public test fixture.
 * Compare literal outputs; do not regenerate expected values using the implementation under test.
 */
public class ECKeyCompatibilityTest {

  private static final String PRIVATE_KEY =
      "0000000000000000000000000000000000000000000000000000000000000001";
  private static final String ADDRESS =
      "417e5f4552091a69125d5dfcb7b8c2659029395bdf";
  private static final String BASE58_ADDRESS =
      "TMVQGm1qAQYVdetCeGRRkTWYYrLXuHK2HC";
  private static final String TRANSACTION_RAW =
      "0a0212342208001122334455667740e0d4bdbbc82e5a67080112630a2d747970652e676f6f676c65"
          + "617069732e636f6d2f70726f746f636f6c2e5472616e73666572436f6e747261637412320a15417e"
          + "5f4552091a69125d5dfcb7b8c2659029395bdf1215412b5ad5c4795c026514f8317c7a215e218dcc"
          + "d6cf1887ad4b708080babbc82e900180ade204";
  private static final String TRANSACTION_ID =
      "23f3345d373b3243e28b0a345f1aa17291ec04ccfb5f5aea125935e062956bf4";
  private static final String TRANSACTION_SIGNATURE =
      "14bb59539502576029fc6edd124caa643471f283706333d58cf81c0007f0103a434cc17c3c7765d2"
          + "61004a0d72e0bf28299d077147978f59d28a24cedee2c77600";
  private static final String BLOCK_HEADER_RAW =
      "08b897babbc82e122095e83c29252f96b3fe128b7625e2bb07bd94d869e7b83f9e6f7b5b4d241121"
          + "391a20000000000001e23f08090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f38c0c4074a"
          + "15417e5f4552091a69125d5dfcb7b8c2659029395bdf5015";
  private static final String BLOCK_ID =
      "000000000001e240bf282e968d494739b6bba6f6f6971c0af5f9185d9a2ac801";
  private static final String BLOCK_RAW_HASH =
      "7f472a361c8195bbbf282e968d494739b6bba6f6f6971c0af5f9185d9a2ac801";
  private static final String BLOCK_SIGNATURE =
      "f762ac19cafb76adecebf1fde5ed4898bd60bb13dc4e66f9225c73822ecc1b3d71b166e88aeb18bc"
          + "794f75620c48fe7803e5a91e02f374f67c3619d4ac7d907c00";
  private static final String MERKLE_ROOT =
      "95e83c29252f96b3fe128b7625e2bb07bd94d869e7b83f9e6f7b5b4d24112139";

  @Test
  public void testAddressVector() {
    byte[] address = SignUtils.fromPrivate(ByteArray.fromHexString(PRIVATE_KEY)).getAddress();
    assertArrayEquals(ByteArray.fromHexString(ADDRESS), address);
    assertEquals(BASE58_ADDRESS, StringUtil.encode58Check(address));
  }

  @Test
  public void testTransactionIdVector() throws Exception {
    assertArrayEquals(ByteArray.fromHexString(TRANSACTION_ID),
        transaction().getTransactionId().getBytes());
  }

  @Test
  public void testTransactionSignatureVector() throws Exception {
    TransactionCapsule transaction = transaction();
    transaction.sign(ByteArray.fromHexString(PRIVATE_KEY));
    assertEquals(1, transaction.getInstance().getSignatureCount());
    ByteString signature = transaction.getInstance().getSignature(0);
    assertArrayEquals(ByteArray.fromHexString(TRANSACTION_SIGNATURE), signature.toByteArray());
    assertArrayEquals(ByteArray.fromHexString(ADDRESS), SignUtils.signatureToAddress(
        ByteArray.fromHexString(TRANSACTION_ID),
        TransactionCapsule.getBase64FromByteString(signature)));
    // Signing must not change the ID, which hashes only the transaction's raw data.
    assertArrayEquals(ByteArray.fromHexString(TRANSACTION_ID),
        new TransactionCapsule(transaction.getInstance()).getTransactionId().getBytes());
  }

  @Test
  public void testBlockIdVector() throws Exception {
    assertArrayEquals(ByteArray.fromHexString(BLOCK_RAW_HASH),
        Sha256Hash.hash(ByteArray.fromHexString(BLOCK_HEADER_RAW)));
    assertArrayEquals(ByteArray.fromHexString(BLOCK_ID), block().getBlockId().getBytes());
  }

  @Test
  public void testBlockSignatureVector() throws Exception {
    BlockCapsule block = block();
    block.sign(ByteArray.fromHexString(PRIVATE_KEY));
    ByteString signature = block.getInstance().getBlockHeader().getWitnessSignature();
    assertArrayEquals(ByteArray.fromHexString(BLOCK_SIGNATURE),
        signature.toByteArray());
    assertArrayEquals(ByteArray.fromHexString(ADDRESS), SignUtils.signatureToAddress(
        ByteArray.fromHexString(BLOCK_RAW_HASH),
        TransactionCapsule.getBase64FromByteString(signature)));
    assertArrayEquals(ByteArray.fromHexString(BLOCK_ID), block.getBlockId().getBytes());
  }

  @Test
  public void testMerkleRootVector() throws Exception {
    BlockCapsule block = block();
    TransactionCapsule transaction = transaction();
    transaction.sign(ByteArray.fromHexString(PRIVATE_KEY));
    block.addTransaction(transaction);
    assertArrayEquals(ByteArray.fromHexString(MERKLE_ROOT), block.calcMerkleRoot().getBytes());
  }

  @Test
  public void testMerkleRootThreeTransactionsVector() throws Exception {
    BlockCapsule block = block();
    for (int i = 0; i < 3; i++) {
      Transaction.raw raw = transaction().getInstance().getRawData().toBuilder()
          .setTimestamp(1_600_000_000_000L + i).build();
      TransactionCapsule transaction =
          new TransactionCapsule(Transaction.newBuilder().setRawData(raw).build());
      transaction.sign(ByteArray.fromHexString(PRIVATE_KEY));
      block.addTransaction(transaction);
    }
    // Three distinct signed leaves exercise concatenation and the odd-leaf rule.
    assertArrayEquals(ByteArray.fromHexString(
        "e57f79925ced3ffd92c801664f45f43ef49aa6851a58afeab9801df1ed1f2c8a"),
        block.calcMerkleRoot().getBytes());
  }

  private static TransactionCapsule transaction() throws InvalidProtocolBufferException {
    return new TransactionCapsule(Transaction.newBuilder().setRawData(
        Transaction.raw.parseFrom(ByteArray.fromHexString(TRANSACTION_RAW))).build());
  }

  private static BlockCapsule block() throws InvalidProtocolBufferException {
    return new BlockCapsule(Block.newBuilder().setBlockHeader(BlockHeader.newBuilder().setRawData(
        BlockHeader.raw.parseFrom(ByteArray.fromHexString(BLOCK_HEADER_RAW)))).build());
  }
}
