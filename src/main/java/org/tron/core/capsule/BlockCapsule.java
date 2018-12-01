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

import com.google.common.primitives.Longs;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.ECKey.ECDSASignature;
import org.tron.common.utils.ByteUtil;
import org.tron.common.utils.Sha256Hash;
import org.tron.common.utils.Time;
import org.tron.core.capsule.utils.MerkleTree;
import org.tron.core.config.Parameter.ChainConstant;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ValidateSignatureException;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.BlockHeader;
import org.tron.protos.Protocol.Transaction;

@Slf4j
public class BlockCapsule implements ProtoCapsule<Block> {

  private boolean generatedByMyself;
  private BlockId blockId;
  private Block block;
  private List<TransactionCapsule> transactions;

  public BlockCapsule(long number, Sha256Hash hash, long timestamp, ByteString witnessAddress) {
    BlockHeader.raw blockHeaderRaw = BlockHeader.raw.newBuilder()
        .setNumber(number)
        .setParentHash(hash.getByteString())
        .setTimestamp(timestamp)
        .setVersion(ChainConstant.BLOCK_VERSION)
        .setWitnessAddress(witnessAddress)
        .build();

    BlockHeader blockHeader = BlockHeader.newBuilder().setRawData(blockHeaderRaw).build();

    this.block = Block.newBuilder().setBlockHeader(blockHeader).build();
    this.transactions = initTxs(block);
  }

  public BlockCapsule(long timestamp, ByteString parentHash, long number,
      List<Transaction> transactionList) {
    BlockHeader.raw blockHeaderRaw = BlockHeader.raw.newBuilder()
        .setTimestamp(timestamp)
        .setParentHash(parentHash)
        .setNumber(number)
        .build();

    BlockHeader blockHeader = BlockHeader.newBuilder().setRawData(blockHeaderRaw).build();

    Block.Builder blockBuild = Block.newBuilder();
    transactionList.forEach(blockBuild::addTransactions);
    this.block = blockBuild.setBlockHeader(blockHeader).build();

    this.transactions = initTxs(block);
  }

  public BlockCapsule(Block block) {
    this.block = block;
    this.transactions = initTxs(block);
  }

  public BlockCapsule(byte[] data) throws BadItemException {
    try {
      this.block = Block.parseFrom(data);
      this.transactions = initTxs(block);
    } catch (InvalidProtocolBufferException e) {
      throw new BadItemException("Block proto data parse exception");
    }
  }

  public void addTransaction(TransactionCapsule pendingTrx) {
    this.block = this.block.toBuilder().addTransactions(pendingTrx.getInstance()).build();
    getTransactions().add(pendingTrx);
  }

  public void sign(byte[] privateKey) {
    // TODO private_key == null
    ECKey ecKey = ECKey.fromPrivate(privateKey);
    ECDSASignature signature = ecKey.sign(getRawHash().getBytes());
    ByteString sig = ByteString.copyFrom(signature.toByteArray());

    BlockHeader blockHeader = this.block.getBlockHeader().toBuilder().setWitnessSignature(sig)
        .build();

    this.block = this.block.toBuilder().setBlockHeader(blockHeader).build();
  }

  public boolean validateSignature() throws ValidateSignatureException {
    try {
      return Arrays
          .equals(ECKey.signatureToAddress(getRawHash().getBytes(),
              TransactionCapsule
                  .getBase64FromByteString(block.getBlockHeader().getWitnessSignature())),
              getBlockHeaderRawData().getWitnessAddress().toByteArray());
    } catch (SignatureException e) {
      throw new ValidateSignatureException(e.getMessage());
    }
  }

  public Sha256Hash calcMerkleRoot() {
    List<Transaction> transactions = this.block.getTransactionsList();

    if (CollectionUtils.isEmpty(transactions)) {
      return Sha256Hash.ZERO_HASH;
    }

    Vector<Sha256Hash> ids = transactions.stream()
        .map(TransactionCapsule::new)
        .map(TransactionCapsule::getMerkleHash)
        .collect(Collectors.toCollection(Vector::new));

    return MerkleTree.getInstance().createTree(ids).getRoot().getHash();
  }

  private List<TransactionCapsule> initTxs(Block block) {
    return block.getTransactionsList().stream()
        .map(TransactionCapsule::new)
        .collect(Collectors.toList());
  }

  private Sha256Hash getRawHash() {
    return Sha256Hash.of(getBlockHeaderRawData().toByteArray());
  }

  public BlockId getBlockId() {
    if (null == this.blockId) {
      this.blockId = new BlockId(Sha256Hash.of(getBlockHeaderRawData().toByteArray()), getNum());
    }
    return this.blockId;
  }

  public List<TransactionCapsule> getTransactions() {
    return this.transactions;
  }

  public void setMerkleRoot() {
    BlockHeader.raw blockHeaderRaw =
        getBlockHeaderRawData().toBuilder()
            .setTxTrieRoot(calcMerkleRoot().getByteString()).build();

    this.block = this.block.toBuilder().setBlockHeader(
        this.block.getBlockHeader().toBuilder().setRawData(blockHeaderRaw)).build();
  }

  /* only for genisis */
  public void setWitness(String witness) {
    BlockHeader.raw blockHeaderRaw =
        getBlockHeaderRawData().toBuilder().setWitnessAddress(
            ByteString.copyFrom(witness.getBytes())).build();

    this.block = this.block.toBuilder().setBlockHeader(
        this.block.getBlockHeader().toBuilder().setRawData(blockHeaderRaw)).build();
  }

  public Sha256Hash getMerkleRoot() {
    return Sha256Hash.wrap(getBlockHeaderRawData().getTxTrieRoot());
  }

  public ByteString getWitnessAddress() {
    return getBlockHeaderRawData().getWitnessAddress();
  }

  @Override
  public byte[] getData() {
    return this.block.toByteArray();
  }

  @Override
  public Block getInstance() {
    return this.block;
  }

  public Sha256Hash getParentHash() {
    return Sha256Hash.wrap(getBlockHeaderRawData().getParentHash());
  }

  public BlockId getParentBlockId() {
    return new BlockId(getParentHash(), getNum() - 1);
  }

  public long getNum() {
    return getBlockHeaderRawData().getNumber();
  }

  public ByteString getParentHashStr() {
    return getBlockHeaderRawData().getParentHash();
  }

  public long getTimeStamp() {
    return getBlockHeaderRawData().getTimestamp();
  }

  private BlockHeader.raw getBlockHeaderRawData() {
    return this.block.getBlockHeader().getRawData();
  }

  public boolean isGeneratedByMyself() {
    return generatedByMyself;
  }

  public void setGeneratedByMyself(boolean generatedByMyself) {
    this.generatedByMyself = generatedByMyself;
  }

  @Override
  public String toString() {
    StringBuilder toStringBuff = new StringBuilder();
    toStringBuff.setLength(0);

    toStringBuff.append("BlockCapsule \n[ ");
    toStringBuff.append("hash=").append(getBlockId()).append("\n");
    toStringBuff.append("number=").append(getNum()).append("\n");
    toStringBuff.append("parentId=").append(getParentHash()).append("\n");
    toStringBuff.append("witness address=")
        .append(ByteUtil.toHexString(getWitnessAddress().toByteArray())).append("\n");

    toStringBuff.append("generated by myself=").append(this.generatedByMyself).append("\n");
    toStringBuff.append("generate time=").append(Time.getTimeString(getTimeStamp())).append("\n");

    if (!getTransactions().isEmpty()) {
      toStringBuff.append("merkle root=").append(getMerkleRoot()).append("\n");
      toStringBuff.append("txs size=").append(getTransactions().size()).append("\n");
    } else {
      toStringBuff.append("txs are empty\n");
    }
    toStringBuff.append("]");
    return toStringBuff.toString();
  }

  public static class BlockId extends Sha256Hash {

    private long num;

    public BlockId() {
      super(Sha256Hash.ZERO_HASH.getBytes());
      num = 0;
    }

    public BlockId(Sha256Hash blockId) {
      super(blockId.getBytes());
      byte[] blockNum = new byte[8];
      System.arraycopy(blockId.getBytes(), 0, blockNum, 0, 8);
      num = Longs.fromByteArray(blockNum);
    }

    /**
     * Use {@link #wrap(byte[])} instead.
     */
    public BlockId(Sha256Hash hash, long num) {
      super(num, hash);
      this.num = num;
    }

    public BlockId(byte[] hash, long num) {
      super(num, hash);
      this.num = num;
    }

    public BlockId(ByteString hash, long num) {
      super(num, hash.toByteArray());
      this.num = num;
    }

    public long getNum() {
      return num;
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

    public String getString() {
      return "Num:" + num + ",ID:" + super.toString();
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
        long otherNum = ((BlockId) other).getNum();
        return Long.compare(num, otherNum);
      }
      return super.compareTo(other);
    }
  }
}
