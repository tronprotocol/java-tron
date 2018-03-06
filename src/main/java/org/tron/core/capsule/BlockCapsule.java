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

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.ECKey.ECDSASignature;
import org.tron.core.Sha256Hash;
import org.tron.protos.Protocal.Block;
import org.tron.protos.Protocal.BlockHeader;
import org.tron.protos.Protocal.Transaction;

public class BlockCapsule {

  public class BlockId extends Sha256Hash {

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || (getClass() != o.getClass() && !o.getClass().equals(Sha256Hash.class))) {
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
      return super.compareTo(other);
    }

    private long num = 0;

    /**
     * Use {@link #wrap(byte[])} instead.
     */
    public BlockId(Sha256Hash hash, long num) {
      super(hash.getBytes());
      this.num = num;
    }

    public long getNum() {
      return num;
    }
  }

  private BlockId blockId = new BlockId(Sha256Hash.ZERO_HASH, 0);

  protected static final Logger logger = LoggerFactory.getLogger("BlockCapsule");

  private byte[] data;

  private Block block;

  private boolean unpacked;

  public boolean generatedByMyself = false;

  private synchronized void unPack() {
    if (unpacked) {
      return;
    }

    try {
      this.block = Block.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage());
    }

    unpacked = true;
  }

  public BlockCapsule(long number, ByteString hash, long when, ByteString witnessAddress) {
    // blockheader raw
    BlockHeader.raw.Builder blockHeaderRawBuild = BlockHeader.raw.newBuilder();
    BlockHeader.raw blockHeaderRaw = blockHeaderRawBuild
        .setNumber(number + 1)
        .setParentHash(hash)
        .setTimestamp(when)
        .setWitnessAddress(witnessAddress).build();

    // block header
    BlockHeader.Builder blockHeaderBuild = BlockHeader.newBuilder();
    BlockHeader blockHeader = blockHeaderBuild.setRawData(blockHeaderRaw).build();

    // block
    Block.Builder blockBuild = Block.newBuilder();
    this.block = blockBuild.setBlockHeader(blockHeader).build();
    unpacked = true;
  }

  public BlockCapsule(long timestamp, ByteString parentHash, long number,
      List<Transaction> transactionList) {
    // blockheader raw
    BlockHeader.raw.Builder blockHeaderRawBuild = BlockHeader.raw.newBuilder();
    BlockHeader.raw blockHeaderRaw = blockHeaderRawBuild
        .setTimestamp(timestamp)
        .setParentHash(parentHash)
        .setNumber(number)
        .build();

    // block header
    BlockHeader.Builder blockHeaderBuild = BlockHeader.newBuilder();
    BlockHeader blockHeader = blockHeaderBuild.setRawData(blockHeaderRaw).build();

    // block
    Block.Builder blockBuild = Block.newBuilder();
    transactionList.forEach(trx -> blockBuild.addTransactions(trx));
    this.block = blockBuild.setBlockHeader(blockHeader).build();
    unpacked = true;
  }

  public void addTransaction(Transaction pendingTrx) {
    this.block = this.block.toBuilder().addTransactions(pendingTrx).build();
  }

  public List<TransactionCapsule> getTransactions() {
    return this.block.getTransactionsList().stream()
        .map(trx -> new TransactionCapsule(trx))
        .collect(Collectors.toList());
  }

  public void sign(byte[] privateKey) {
    // TODO private_key == null
    ECKey ecKey = ECKey.fromPrivate(privateKey);
    ECDSASignature signature = ecKey.sign(getRawHash().getBytes());
    ByteString sig = ByteString.copyFrom(signature.toBase64().getBytes());

    BlockHeader blockHeader = this.block.getBlockHeader().toBuilder().setWitnessSignature(sig)
        .build();

    this.block = this.block.toBuilder().setBlockHeader(blockHeader).build();
  }

  private Sha256Hash getRawHash() {
    unPack();
    return Sha256Hash.of(this.block.getBlockHeader().getRawData().toByteArray());
  }

  public boolean validateSignature() {
    try {
      return Arrays
          .equals(ECKey.signatureToAddress(getRawHash().getBytes(),
              block.getBlockHeader().getWitnessSignature().toStringUtf8()),
              block.getBlockHeader().getRawData().getWitnessAddress().toByteArray());
    } catch (SignatureException e) {
      e.printStackTrace();
      return false;
    }
  }

  public Sha256Hash getBlockId() {
    unPack();
    if(blockId.equals(Sha256Hash.ZERO_HASH)) {
      blockId = new BlockId(Sha256Hash.of(this.block.getBlockHeader().toByteArray()), getNum());
    }

    return blockId;
//    return blockId.equals(Sha256Hash.ZERO_HASH)
//        ? blockId = new BlockId(Sha256Hash.of(this.block.getBlockHeader().toByteArray()), getNum())
//        : blockId;
  }

  public Sha256Hash calcMerklerRoot() {
    if (CollectionUtils.isEmpty(this.block.getTransactionsList())) {
      return Sha256Hash.ZERO_HASH;
    }

    Vector<Sha256Hash> ids = new Vector<Sha256Hash>();
    this.block.getTransactionsList().forEach(trx -> {
      TransactionCapsule transactionCapsule = new TransactionCapsule(trx);
      ids.add(transactionCapsule.getHash());
    });

    int hashNum = ids.size();

    while (hashNum > 1) {
      int max = hashNum - (hashNum & 1);
      int k = 0;
      for (int i = 0; i < max; i += 2) {
        ids.set(k++, Sha256Hash
            .of((ids.get(i).getByteString().concat(ids.get(i + 1).getByteString())).toByteArray()));
      }

      if (hashNum % 2 == 1) {
        ids.set(k++, ids.get(max));
      }
    }

    return ids.firstElement();
  }


  public void setMerklerRoot() {
    BlockHeader.raw blockHeaderRaw;
    blockHeaderRaw = this.block.getBlockHeader().getRawData().toBuilder()
        .setTxTrieRoot(calcMerklerRoot().getByteString()).build();

    this.block.toBuilder().setBlockHeader(
        this.block.getBlockHeader().toBuilder().setRawData(blockHeaderRaw));
  }

  public Sha256Hash getMerklerRoot() {
    unPack();
    return Sha256Hash.wrap(this.block.getBlockHeader().getRawData().getTxTrieRoot());
  }


  private void pack() {
    if (data == null) {
      this.data = this.block.toByteArray();
    }
  }

  public boolean validate() {
    unPack();
    return true;
  }

  public BlockCapsule(Block block) {
    this.block = block;
    unpacked = true;
  }

  public BlockCapsule(byte[] data) {
    this.data = data;
    unPack();
  }

  public byte[] getData() {
    pack();
    return data;
  }

  public Sha256Hash getParentHash() {
    unPack();
    return Sha256Hash.wrap(this.block.getBlockHeader().getRawData().getParentHash());
  }

  public ByteString getParentHashStr() {
    unPack();
    return this.block.getBlockHeader().getRawData().getParentHash();
  }

  public long getNum() {
    unPack();
    return this.block.getBlockHeader().getRawData().getNumber();
  }

  public long getTimeStamp() {
    unPack();
    return this.block.getBlockHeader().getRawData().getTimestamp();
  }

  @Override
  public String toString() {
    unPack();
    return this.block.toString();
  }
}
