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

package org.tron.core;

import static org.tron.common.crypto.Hash.sha3;
import static org.tron.core.Constant.LAST_HASH;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;
import org.spongycastle.util.Arrays;
import org.spongycastle.util.BigIntegers;
import org.tron.common.utils.ByteArray;
import org.tron.core.peer.Validator;
import org.tron.protos.Protocal.Block;
import org.tron.protos.Protocal.BlockHeader;
import org.tron.protos.Protocal.Transaction;


public class BlockUtils {

  private static Block.Builder block;
  private byte[] serializEncode;

  /**
   * getData a new block.
   *
   * @return {@link Block} block
   */
  public static Block newBlock(List<Transaction> transactions, ByteString
      parentHash, ByteString difficulty, long number) {
    Block.Builder block = Block.newBuilder();

    for (int i = 0; transactions != null && i < transactions.size(); i++) {
      final int index = i;
      Optional.ofNullable(transactions.get(index)).ifPresent((transaction) ->
          block.addTransactions(index, transaction)
      );
    }

    BlockHeader.Builder blockHeaderBuilder = BlockHeader.newBuilder();

    blockHeaderBuilder.setParentHash(parentHash);
    blockHeaderBuilder.setDifficulty(difficulty);
    blockHeaderBuilder.setNumber(number);
    blockHeaderBuilder.setTimestamp(System.currentTimeMillis());

    block.setBlockHeader(blockHeaderBuilder.build());

    blockHeaderBuilder.setHash(ByteString.copyFrom(sha3(prepareData(block
        .build()))));

    block.setBlockHeader(blockHeaderBuilder.build());
    return block.build();
  }

  /**
   * new genesis block.
   *
   * @return {@link Block} block
   */
  public static Block newGenesisBlock(Transaction coinbase) {

    Block.Builder genesisBlock = Block.newBuilder();
    genesisBlock.addTransactions(coinbase);

    BlockHeader.Builder builder = BlockHeader.newBuilder();
    builder.setDifficulty(ByteString.copyFrom(ByteArray.fromHexString
        ("2001")));

    genesisBlock.setBlockHeader(builder.build());

    builder.setHash(ByteString.copyFrom(sha3(prepareData
        (genesisBlock.build()))));

    genesisBlock.setBlockHeader(builder.build());

    return genesisBlock.build();
  }

  public static Block newGenesisBlock(List<Transaction> transactions) {

    Block.Builder genesisBlock = Block.newBuilder();

    for (Transaction tx : transactions) {
      genesisBlock.addTransactions(tx);
    }

    BlockHeader.Builder builder = BlockHeader.newBuilder();
    builder.setDifficulty(ByteString.copyFrom(ByteArray.fromHexString
        ("2001")));

    genesisBlock.setBlockHeader(builder.build());

    builder.setHash(ByteString.copyFrom(sha3(prepareData
        (genesisBlock.build()))));

    genesisBlock.setBlockHeader(builder.build());

    return genesisBlock.build();
  }

  /**
   * getData prepare data of the block.
   *
   * @param block {@link Block} block
   * @return byte[] data
   */
  public static byte[] prepareData(Block block) {
    Block.Builder tmp = block.toBuilder();

    BlockHeader.Builder blockHeader = tmp.getBlockHeaderBuilder();
    blockHeader.clearHash();
    blockHeader.clearNonce();

    tmp.setBlockHeader(blockHeader.build());

    return tmp.build().toByteArray();
  }

  /**
   * the proof block.
   *
   * @param block {@link Block} block
   * @return boolean is it the proof block
   */
  public static boolean isValidate(Block block) {
    return Validator.validate(block);
  }

  /**
   * getData print string of the block.
   *
   * @param block {@link Block} block
   * @return String format string of the block
   */
  public static String toPrintString(Block block) {
    if (block == null) {
      return "";
    }

    DateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    return "\nBlock {\n" +
        "\ttimestamp=" + sdf.format(new Timestamp(block
        .getBlockHeader().getTimestamp
            ())) +
        ", \n\tparentHash=" + ByteArray.toHexString(block
        .getBlockHeader()
        .getParentHash().toByteArray()) +
        ", \n\thash=" + ByteArray.toHexString(block.getBlockHeader()
        .getHash()
        .toByteArray()) +
        ", \n\tnonce=" + ByteArray.toHexString(block.getBlockHeader()
        .getNonce()
        .toByteArray()) +
        ", \n\tdifficulty=" + ByteArray.toHexString(block
        .getBlockHeader()
        .getDifficulty().toByteArray()) +
        ", \n\tnumber=" + block.getBlockHeader().getNumber() +
        "\n}\n";
  }

  /**
   * getData mine value
   *
   * @param block {@link Block} block
   * @return byte[] mine value
   */
  public static byte[] getMineValue(Block block) {
    byte[] concat = Arrays.concatenate(prepareData(block), block
        .getBlockHeader().getNonce
            ().toByteArray());

    return sha3(concat);
  }

  /**
   * getData Verified boundary
   *
   * @param block {@link Block} block
   * @return byte[] boundary
   */
  public static byte[] getPowBoundary(Block block) {
    return BigIntegers.asUnsignedByteArray(32, BigInteger.ONE.shiftLeft
        (256).divide(new BigInteger(1, block.getBlockHeader()
        .getDifficulty()
        .toByteArray())));
  }

  /**
   * getData increase number + 1
   *
   * @return long number
   */
  public static long getIncreaseNumber(Blockchain blockchain) {
    byte[] lastHash = blockchain.getBlockDB().getData(LAST_HASH);
    if (lastHash == null) {
      return 0;
    }

    byte[] value = blockchain.getBlockDB().getData(lastHash);
    if (value == null) {
      return 0;
    }

    long number = 0;
    try {

      Block bpRead = Block.parseFrom(value).toBuilder().build();
      number = bpRead.getBlockHeader().getNumber();
      number += 1;
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }

    return number;
  }

  // Whether the hash of the judge block is equal to the hash of the parent
  // block
  public static boolean isParentOf(Block block1, Block
      block2) {

    return (block1.getBlockHeader().getParentHash() == block2.getBlockHeader
        ().getHash());
  }
}
