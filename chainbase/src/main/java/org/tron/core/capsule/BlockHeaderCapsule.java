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
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.ECKey.ECDSASignature;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ByteUtil;
import org.tron.common.utils.Sha256Hash;
import org.tron.common.utils.Time;
import org.tron.core.config.Parameter.ChainConstant;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ValidateSignatureException;
import org.tron.core.store.AccountStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.BlockHeader;

import javax.annotation.Nonnull;
import java.security.SignatureException;
import java.util.Arrays;

@Slf4j(topic = "capsule")
public class BlockHeaderCapsule implements ProtoCapsule<BlockHeader> {

  public boolean generatedByMyself = false;

  private BlockCapsule.BlockId blockId = new BlockCapsule.BlockId(Sha256Hash.ZERO_HASH, 0);

  private BlockHeader blockHeader;

  private StringBuilder toStringBuff = new StringBuilder();

  private boolean isSwitch;

  public boolean isSwitch() {
    return isSwitch;
  }

  public BlockHeaderCapsule setSwitch(boolean aSwitch) {
    isSwitch = aSwitch;
    return this;
  }

  public BlockHeaderCapsule(long number, Sha256Hash hash, long when, ByteString witnessAddress) {
    // blockheader raw
    BlockHeader.raw.Builder blockHeaderRawBuild = BlockHeader.raw.newBuilder();
    BlockHeader.raw blockHeaderRaw = blockHeaderRawBuild
        .setNumber(number)
        .setParentHash(hash.getByteString())
        .setTimestamp(when)
        .setVersion(ChainConstant.BLOCK_VERSION)
        .setWitnessAddress(witnessAddress)
        .build();

    // block header
    BlockHeader.Builder blockHeaderBuild = BlockHeader.newBuilder();
    blockHeader = blockHeaderBuild.setRawData(blockHeaderRaw).build();
  }

  public BlockHeaderCapsule(long timestamp, ByteString parentHash, long number) {
    // blockheader raw
    BlockHeader.raw.Builder blockHeaderRawBuild = BlockHeader.raw.newBuilder();
    BlockHeader.raw blockHeaderRaw = blockHeaderRawBuild
        .setTimestamp(timestamp)
        .setParentHash(parentHash)
        .setNumber(number)
        .build();

    // block header
    BlockHeader.Builder blockHeaderBuild = BlockHeader.newBuilder();
    blockHeader = blockHeaderBuild.setRawData(blockHeaderRaw).build();
  }

  public BlockHeaderCapsule(BlockHeader blockHeader) {
    this.blockHeader = blockHeader;
  }

  public BlockHeaderCapsule(byte[] data) throws BadItemException {
    try {
      blockHeader = BlockHeader.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      throw new BadItemException("BlockHeader proto data parse exception");
    }
  }

  public BlockHeaderCapsule(CodedInputStream codedInputStream) throws BadItemException {
    try {
      blockHeader = BlockHeader.parseFrom(codedInputStream);
    } catch (Exception e) {
      logger.error("constructor block error : {}", e.getMessage());
      throw new BadItemException("BlockHeader proto data parse exception");
    }
  }

  public void sign(byte[] privateKey) {
    ECKey ecKey = ECKey.fromPrivate(privateKey);
    ECDSASignature signature = ecKey.sign(getRawHash().getBytes());
    ByteString sig = ByteString.copyFrom(signature.toByteArray());

    blockHeader = blockHeader.toBuilder().setWitnessSignature(sig).build();
  }

  private Sha256Hash getRawHash() {
    return Sha256Hash.of(blockHeader.getRawData().toByteArray());
  }

  public boolean validateSignature(DynamicPropertiesStore dynamicPropertiesStore,
      AccountStore accountStore) throws ValidateSignatureException {
    try {
      byte[] sigAddress = ECKey.signatureToAddress(getRawHash().getBytes(),
          TransactionCapsule.getBase64FromByteString(blockHeader.getWitnessSignature()));
      byte[] witnessAccountAddress = blockHeader.getRawData().getWitnessAddress()
          .toByteArray();

      if (dynamicPropertiesStore.getAllowMultiSign() != 1) {
        return Arrays.equals(sigAddress, witnessAccountAddress);
      } else {
        byte[] witnessPermissionAddress = accountStore.get(witnessAccountAddress)
            .getWitnessPermissionAddress();
        return Arrays.equals(sigAddress, witnessPermissionAddress);
      }

    } catch (SignatureException e) {
      throw new ValidateSignatureException(e.getMessage());
    }
  }

  @Nonnull
  public BlockCapsule.BlockId getBlockId() {
    if (blockId.equals(Sha256Hash.ZERO_HASH)) {
      blockId = new BlockCapsule.BlockId(Sha256Hash.of(blockHeader.getRawData().toByteArray()),
          getNum());
    }
    return blockId;
  }

  /* only for genisis */
  public void setWitness(String witness) {
    BlockHeader.raw blockHeaderRaw = blockHeader.getRawData().toBuilder().setWitnessAddress(
            ByteString.copyFrom(witness.getBytes())).build();

    blockHeader = blockHeader.toBuilder().setRawData(blockHeaderRaw).build();
  }

  public Sha256Hash getMerkleRoot() {
    return Sha256Hash.wrap(blockHeader.getRawData().getTxTrieRoot());
  }

  public Sha256Hash getAccountRoot() {
    if (blockHeader.getRawData().getAccountStateRoot() != null
        && !blockHeader.getRawData().getAccountStateRoot().isEmpty()) {
      return Sha256Hash.wrap(blockHeader.getRawData().getAccountStateRoot());
    }
    return Sha256Hash.ZERO_HASH;
  }

  public ByteString getWitnessAddress() {
    return blockHeader.getRawData().getWitnessAddress();
  }

  @Override
  public byte[] getData() {
    return blockHeader.toByteArray();
  }

  @Override
  public BlockHeader getInstance() {
    return blockHeader;
  }

  public Sha256Hash getParentHash() {
    return Sha256Hash.wrap(blockHeader.getRawData().getParentHash());
  }

  public BlockCapsule.BlockId getParentBlockId() {
    return new BlockCapsule.BlockId(getParentHash(), getNum() - 1);
  }

  public ByteString getParentHashStr() {
    return blockHeader.getRawData().getParentHash();
  }

  public long getNum() {
    return blockHeader.getRawData().getNumber();
  }

  public long getTimeStamp() {
    return blockHeader.getRawData().getTimestamp();
  }

  @Override
  public String toString() {
    toStringBuff.setLength(0);

    toStringBuff.append("BlockHeaderCapsule \n[ ");
    toStringBuff.append("hash=").append(getBlockId()).append("\n");
    toStringBuff.append("number=").append(getNum()).append("\n");
    toStringBuff.append("parentId=").append(getParentHash()).append("\n");
    toStringBuff.append("witness address=")
        .append(ByteUtil.toHexString(getWitnessAddress().toByteArray())).append("\n");

    toStringBuff.append("generated by myself=").append(generatedByMyself).append("\n");
    toStringBuff.append("generate time=").append(Time.getTimeString(getTimeStamp())).append("\n");
    toStringBuff.append("account root=").append(getAccountRoot()).append("\n");

    toStringBuff.append("]");
    return toStringBuff.toString();
  }

  public String getChainId() {
    return ByteArray.toHexString(blockHeader.getRawData().getChainId().toByteArray());
  }
}
