package org.tron.core.net.service.sync;

import org.tron.core.capsule.BlockCapsule;

public class UnparsedBlock {

  private final BlockCapsule.BlockId blockId;
  private final byte[] data;

  public UnparsedBlock(BlockCapsule.BlockId blockId, byte[] data) {
    if (blockId == null) {
      throw new IllegalArgumentException("blockId must not be null");
    }
    this.blockId = blockId;
    this.data = data;
  }

  public BlockCapsule.BlockId getBlockId() {
    return blockId;
  }

  public byte[] getData() {
    return data;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof UnparsedBlock)) {
      return false;
    }
    return blockId.equals(((UnparsedBlock) o).blockId);
  }

  @Override
  public int hashCode() {
    return blockId.hashCode();
  }

  @Override
  public String toString() {
    return blockId.getString();
  }
}
