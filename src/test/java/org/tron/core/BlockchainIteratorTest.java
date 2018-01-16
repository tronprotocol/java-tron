package org.tron.core;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.ByteString;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.protos.core.TronBlock.Block;
import org.tron.protos.core.TronBlockHeader.BlockHeader;
import org.tron.storage.leveldb.LevelDbDataSourceImpl;
import org.tron.utils.ByteArray;

import java.io.IOException;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.tron.utils.ByteArray.toHexString;

public class BlockchainIteratorTest {
  private static final Logger LOGGER = LoggerFactory.getLogger("Test");

  private Blockchain mockBlockchain;
  private BlockchainIterator blockchainIterator;
  private LevelDbDataSourceImpl mockDataSource;

  @Before
  public void setup() throws IOException {
    mockDataSource = Mockito.mock(LevelDbDataSourceImpl.class);

    mockBlockchain = Mockito.mock(Blockchain.class);
    Mockito.when(mockBlockchain.getBlockDB()).thenReturn(mockDataSource);

    final ByteString hash1 = ByteString.copyFrom("1", Charsets.UTF_8);
    final ByteString hash2 = ByteString.copyFrom("2", Charsets.UTF_8);
    final ByteString hash3 = ByteString.copyFrom("3", Charsets.UTF_8);
    final ByteString hash4 = ByteString.copyFrom("4", Charsets.UTF_8);

    Mockito.when(mockBlockchain.getCurrentHash()).thenReturn(ByteArray.fromString("4"));
    ImmutableMap.of(
        hash1.toByteArray(),
        Block.newBuilder()
            .setBlockHeader(
                BlockHeader.newBuilder()
                    .setNumber(1)
                    .setHash(hash1)
                    .setParentHash(ByteString.copyFrom("", Charsets.UTF_8))
                    .build()
            )
            .build(),
        hash2.toByteArray(),
        Block.newBuilder()
            .setBlockHeader(
                BlockHeader.newBuilder()
                    .setNumber(2)
                    .setHash(hash2)
                    .setParentHash(hash1)
                    .build()
            )
            .build(),
        hash3.toByteArray(),
        Block.newBuilder()
            .setBlockHeader(
                BlockHeader.newBuilder()
                    .setNumber(3)
                    .setHash(hash3)
                    .setParentHash(hash2)
                    .build()
            )
            .build(),
        hash4.toByteArray(),
        Block.newBuilder()
            .setBlockHeader(
                BlockHeader.newBuilder()
                    .setNumber(4)
                    .setHash(hash4)
                    .setParentHash(hash3)
                    .build()
            )
            .build())
        .forEach((key, value) -> Mockito.when(mockDataSource.getData(key)).thenReturn(value.toByteArray()));

    blockchainIterator = new BlockchainIterator(mockBlockchain);
  }

  @Test
  public void testIteration() {
    final LinkedList<Block> blocks = new LinkedList<>();
    while (blockchainIterator.hasNext()) {
      Block block = blockchainIterator.next();
      LOGGER.info("block[parentHash={},number={}]",
          toHexString(block.getBlockHeader().getParentHash().toByteArray()),
          block.getBlockHeader().getNumber());

      blocks.add(block);
    }

    assertThat(blocks, hasSize(4));

    final LinkedList<Long> blockNumbers = blocks.stream()
        .map(Block::getBlockHeader)
        .map(BlockHeader::getNumber)
        .collect(Collectors.toCollection(LinkedList::new));

    assertThat(blockNumbers, contains(4L, 3L, 2L, 1L));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testRemoveIsNotSupported() {
    blockchainIterator.remove();
  }

  @Test(expected = NoSuchElementException.class)
  public void testIteratorThrowsNoSuchElementExceptionAfterEnd() {
    while (blockchainIterator.hasNext()) {
      blockchainIterator.next();
    }

    blockchainIterator.next();
  }
}