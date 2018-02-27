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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.ECKey.ECDSASignature;
import org.tron.core.Sha256Hash;
import org.tron.protos.Protocal.Block;
import org.tron.protos.Protocal.BlockHeader;
import org.tron.protos.Protocal.Transaction;


public class BlockCapsule {

  protected static final Logger logger = LoggerFactory.getLogger("BlockCapsule");

  private byte[] data;

  private Block block;

  private boolean unpacked;

  public boolean validated = false;

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

  }

  public void addTransaction(Transaction pendingTrx) {
    this.block = this.block.toBuilder().addTransactions(pendingTrx).build();
  }

  public List<TransactionCapsule> getTransactions() {
    return this.block.getTransactionsList().stream()
        .map(trx->new TransactionCapsule(trx))
        .collect(Collectors.toList());
  }

  public void sign(String privateKey) {
    // TODO private_key == null
    ECKey ecKey = ECKey.fromPrivate(Hex.decode(privateKey));
    ECDSASignature signature = ecKey.sign(getRawHash().getBytes());
    ByteString sig = ByteString.copyFrom(signature.toByteArray());

    BlockHeader blockHeader = this.block.getBlockHeader().toBuilder().setWitnessSignature(sig)
        .build();

    this.block = this.block.toBuilder().setBlockHeader(blockHeader).build();
    validated = true;
  }

  private Sha256Hash getRawHash() {
    unPack();
    return Sha256Hash.of(block.getBlockHeader().getRawData().toByteArray());
  }


  public boolean validateSignature() {
    try {
      return Arrays.equals(ECKey.signatureToAddress(block.getBlockHeader().getRawData().toByteArray(),
          block.getBlockHeader().getWitnessSignature().toString()),
          block.getBlockHeader().getRawData().getWitnessAddress().toByteArray());
    } catch (SignatureException e) {
      e.printStackTrace();
      return false;
    }
  }

  public Sha256Hash getBlockId() {
    pack();
    return Sha256Hash.of(this.block.getBlockHeader().getRawData().toByteArray());
  }

  public Sha256Hash calcMerklerRoot() {
    if (this.block.getTransactionsList().size() == 0) {
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
    return;
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

}
