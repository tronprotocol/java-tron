/*
 * java-tron is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-tron is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.core.capsule;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.ECKey.ECDSASignature;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.utils.MerkleTree;
import org.tron.core.exception.ValidateSignatureException;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.BlockHeader;
import org.tron.protos.Protocol.Transaction;

@Slf4j
public class BlockCapsule implements ProtoCapsule<Block> {

  private BlockId blockId = new BlockId(Sha256Hash.ZERO_HASH, 0);
  private byte[] data;
  private Block block;

  @Getter
  @Setter
  public boolean generatedByMyself = false;

  public BlockCapsule(Block block) {
    this.block = block;
  }

  public BlockCapsule(final byte[] data) {
    this.data = data;
    parseToBlockIfNotNull();
  }

  public BlockCapsule(final long number, final ByteString hash, final long when,
      final ByteString witnessAddress) {
    BlockHeader.raw.Builder blockHeaderRawBuild = BlockHeader.raw.newBuilder();
    BlockHeader.raw blockHeaderRaw = blockHeaderRawBuild
        .setNumber(number)
        .setParentHash(hash)
        .setTimestamp(when)
        .setWitnessAddress(witnessAddress).build();

    BlockHeader.Builder blockHeaderBuild = BlockHeader.newBuilder();
    BlockHeader blockHeader = blockHeaderBuild.setRawData(blockHeaderRaw).build();

    Block.Builder blockBuilder = Block.newBuilder();
    this.block = blockBuilder.setBlockHeader(blockHeader).build();
  }

  public BlockCapsule(final long timestamp, final ByteString parentHash, final long number,
      final List<Transaction> transactionList) {

    BlockHeader.raw.Builder blockHeaderRawBuilder = BlockHeader.raw.newBuilder();
    BlockHeader.raw blockHeaderRaw = blockHeaderRawBuilder
        .setTimestamp(timestamp)
        .setParentHash(parentHash)
        .setNumber(number)
        .build();

    BlockHeader.Builder blockHeaderBuilder = BlockHeader.newBuilder();
    BlockHeader blockHeader = blockHeaderBuilder.setRawData(blockHeaderRaw).build();

    Block.Builder blockBuilder = Block.newBuilder();
    transactionList.forEach(blockBuilder::addTransactions);
    this.block = blockBuilder.setBlockHeader(blockHeader).build();
  }

  public void sign(final byte[] privateKey) {
    // TODO private_key == null
    ECKey ecKey = ECKey.fromPrivate(privateKey);
    ECDSASignature signature = ecKey.sign(getRawHash().getBytes());
    ByteString sig = ByteString.copyFrom(signature.toBase64().getBytes());

    BlockHeader blockHeader = this.block.getBlockHeader().toBuilder().setWitnessSignature(sig)
        .build();

    this.block = this.block.toBuilder().setBlockHeader(blockHeader).build();
  }

  public boolean validateSignature() throws ValidateSignatureException {
    try {
      return Arrays
          .equals(ECKey.signatureToAddress(getRawHash().getBytes(),
              this.block.getBlockHeader().getWitnessSignature().toStringUtf8()),
              getBlockHeaderRawData().getWitnessAddress().toByteArray());
    } catch (SignatureException e) {
      throw new ValidateSignatureException(e.getMessage());
    }
  }

  public Sha256Hash calculateMerkleRoot() {
    List<Transaction> transactionsList = this.block.getTransactionsList();

    if (CollectionUtils.isEmpty(transactionsList)) {
      return Sha256Hash.ZERO_HASH;
    }

    Vector<Sha256Hash> ids = transactionsList.stream()
            .map(TransactionCapsule::new)
            .map(TransactionCapsule::getHash)
            .collect(Collectors.toCollection(Vector::new));

    return MerkleTree.getInstance().createTree(ids).getRoot().getHash();
  }

  public Sha256Hash getMerkleRoot() {
    return Sha256Hash.wrap(getBlockHeaderRawData().getTxTrieRoot());
  }

  public void setMerkleRoot() {
    BlockHeader.raw blockHeaderRaw = getBlockHeaderRawData().toBuilder()
                                                            .setTxTrieRoot(calculateMerkleRoot()
                                                                .getByteString()).build();

    this.block = this.block.toBuilder().setBlockHeader(
        this.block.getBlockHeader().toBuilder().setRawData(blockHeaderRaw)).build();
  }

  public BlockId getBlockId() {
    if (blockId.equals(Sha256Hash.ZERO_HASH)) {
      blockId = new BlockId(Sha256Hash.of(this.block.getBlockHeader().toByteArray()), getNumber());
    }

    return blockId;
  }

  public void addTransaction(final Transaction pendingTrx) {
    this.block = this.block.toBuilder().addTransactions(pendingTrx).build();
  }

  public List<TransactionCapsule> getTransactions() {
    return this.block.getTransactionsList().stream()
                     .map(TransactionCapsule::new)
                     .collect(Collectors.toList());
  }

  private Sha256Hash getRawHash() {
    return Sha256Hash.of(getBlockHeaderRawData().toByteArray());
  }

  public Sha256Hash getHashedParentHash() {
    return Sha256Hash.wrap(getBlockHeaderRawData().getParentHash());
  }

  public ByteString getParentHash() {
    return getBlockHeaderRawData().getParentHash();
  }

  public long getNumber() {
    return getBlockHeaderRawData().getNumber();
  }

  public long getTimestamp() {
    return getBlockHeaderRawData().getTimestamp();
  }

  private BlockHeader.raw getBlockHeaderRawData() {
    return this.block.getBlockHeader().getRawData();
  }

  @Override
  public byte[] getData() {
    return this.block.toByteArray();
  }

  @Override
  public Block getInstance() {
    return this.block;
  }

  @Override
  public String toString() {
    return "BlockCapsule{" +
        "blockId=" + blockId +
        ", number=" + getNumber() +
        ", parentId=" + getHashedParentHash() +
        ", generatedByMyself=" + generatedByMyself +
        '}';
  }

  private synchronized void parseToBlockIfNotNull() {
    if (null != this.block) {
      return;
    }

    try {
      this.block = Block.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      log.debug(e.getMessage());
    }
  }

  public static class BlockId extends Sha256Hash {

    @Getter
    private long number;

    public BlockId() {
      super(Sha256Hash.ZERO_HASH.getBytes());
      number = 0;
    }

    /**
     * Use {@link #wrap(byte[])} instead.
     */
    public BlockId(Sha256Hash hash, long number) {
      super(hash.getBytes());
      this.number = number;
    }

    public BlockId(byte[] hash, long number) {
      super(hash);
      this.number = number;
    }

    public BlockId(ByteString hash, long number) {
      super(hash.toByteArray());
      this.number = number;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || (getClass() != o.getClass() && !(o instanceof Sha256Hash))) {
        return false;
      }
      return Arrays.equals(getBytes(), ((Sha256Hash) o).getBytes());
    }

    @Override
    public String toString() {
      return super.toString();
    }

    @Override
    public int hashCode() {
      return super.hashCode();
    }

    @Override
    public int compareTo(Sha256Hash other) {
      if (other.getClass().equals(BlockId.class)) {
        long otherNum = ((BlockId) other).getNumber();
        if (number > otherNum) {
          return 1;
        } else if (otherNum < number) {
          return -1;
        }
      }
      return super.compareTo(other);
    }
  }
}
