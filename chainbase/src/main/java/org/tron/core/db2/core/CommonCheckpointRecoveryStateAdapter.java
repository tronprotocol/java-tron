package org.tron.core.db2.core;

import java.io.IOException;
import java.util.Objects;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.db2.archive.BlockSnapshotMeta;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.store.DynamicPropertiesStore;

/** Read-only recovery identity adapter over checkpoint-redone persistent Chainbase stores. */
public final class CommonCheckpointRecoveryStateAdapter
    implements CommonCheckpointHotRecovery.PersistentDynamicHeadSource,
    CommonCheckpointHotRecovery.BlockMetaSource {

  private final DynamicPropertiesStore dynamicPropertiesStore;
  private final ChainBaseManager chainBaseManager;

  public CommonCheckpointRecoveryStateAdapter(DynamicPropertiesStore dynamicPropertiesStore,
      ChainBaseManager chainBaseManager) {
    this.dynamicPropertiesStore = Objects.requireNonNull(dynamicPropertiesStore,
        "dynamicPropertiesStore");
    this.chainBaseManager = Objects.requireNonNull(chainBaseManager, "chainBaseManager");
  }

  /** Reads only the persistent root; reversible Snapshot values are not recovery authority. */
  @Override
  public CommonCheckpointHotRecovery.PersistentDynamicHead load() throws IOException {
    final long blockNumber;
    final Sha256Hash blockHash;
    try {
      blockNumber = dynamicPropertiesStore.getLatestBlockHeaderNumberFromDB();
      blockHash = dynamicPropertiesStore.getLatestBlockHeaderHashFromDB();
    } catch (RuntimeException failure) {
      throw new IOException("persistent dynamic block identity cannot be read", failure);
    }
    if (blockNumber < 0 || blockHash == null) {
      throw new IOException("persistent dynamic block identity is unavailable");
    }
    return new CommonCheckpointHotRecovery.PersistentDynamicHead(blockNumber,
        blockHash.getBytes());
  }

  /** Loads the full canonical block metadata needed to validate an exact recovery ceiling. */
  @Override
  public BlockSnapshotMeta loadIfPresent(long blockNumber) throws IOException {
    if (blockNumber < 0) {
      throw new IllegalArgumentException("blockNumber must not be negative");
    }
    final BlockCapsule block;
    try {
      block = chainBaseManager.getBlockByNum(blockNumber);
    } catch (ItemNotFoundException missing) {
      return null;
    } catch (BadItemException corrupt) {
      throw new IOException("Block Store recovery metadata is corrupt", corrupt);
    }
    if (block.getNum() != blockNumber) {
      throw new IOException("Block Store returned a different recovery height");
    }
    return BlockSnapshotMeta.forBlock(blockNumber, block.getBlockId().getBytes(),
        block.getParentHash().getBytes(), block.getTimeStamp());
  }
}
