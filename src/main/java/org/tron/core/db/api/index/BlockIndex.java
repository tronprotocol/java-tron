package org.tron.core.db.api.index;

import static com.googlecode.cqengine.query.QueryFactory.attribute;

import com.googlecode.cqengine.attribute.Attribute;
import com.googlecode.cqengine.index.hash.HashIndex;
import com.googlecode.cqengine.index.navigable.NavigableIndex;
import com.googlecode.cqengine.index.suffix.SuffixTreeIndex;
import com.googlecode.cqengine.persistence.Persistence;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.TronDatabase;
import org.tron.core.db.common.WrappedByteArray;
import org.tron.protos.Protocol.Block;

@Component
@Slf4j
public class BlockIndex extends AbstractIndex<BlockCapsule, Block> {

  public final Attribute<WrappedByteArray, String> Block_ID =
      attribute("block id",
          bytes -> {
            Block block = getObject(bytes);
            return Sha256Hash.of(block.getBlockHeader().toByteArray()).toString();
          });
  public final Attribute<WrappedByteArray, Long> Block_NUMBER =
      attribute("block number",
          bytes -> {
            Block block = getObject(bytes);
            return block.getBlockHeader().getRawData().getNumber();
          });
  public final Attribute<WrappedByteArray, String> TRANSACTIONS =
      attribute(String.class, "transactions",
          bytes -> {
            Block block = getObject(bytes);
            return block.getTransactionsList().stream()
                .map(t -> Sha256Hash.of(t.getRawData().toByteArray()).toString())
                .collect(Collectors.toList());
          });
  public final Attribute<WrappedByteArray, Long> WITNESS_ID =
      attribute("witness id",
          bytes -> {
            Block block = getObject(bytes);
            return block.getBlockHeader().getRawData().getWitnessId();
          });
  public final Attribute<WrappedByteArray, String> WITNESS_ADDRESS =
      attribute("witness address",
          bytes -> {
            Block block = getObject(bytes);
            return ByteArray.toHexString(
                block.getBlockHeader().getRawData().getWitnessAddress().toByteArray());
          });

  public final Attribute<WrappedByteArray, String> OWNERS =
      attribute(String.class, "owner address",
          bytes -> {
            Block block = getObject(bytes);
            return block.getTransactionsList().stream()
                .map(transaction -> transaction.getRawData().getContractList())
                .flatMap(List::stream)
                .map(TransactionCapsule::getOwner)
                .filter(Objects::nonNull)
                .distinct()
                .map(ByteArray::toHexString)
                .collect(Collectors.toList());
          });
  public final Attribute<WrappedByteArray, String> TOS =
      attribute(String.class, "to address",
          bytes -> {
            Block block = getObject(bytes);
            return block.getTransactionsList().stream()
                .map(transaction -> transaction.getRawData().getContractList())
                .flatMap(List::stream)
                .map(TransactionCapsule::getToAddress)
                .filter(Objects::nonNull)
                .distinct()
                .map(ByteArray::toHexString)
                .collect(Collectors.toList());
          });

  @Autowired
  public BlockIndex(
      @Qualifier("blockStore") final TronDatabase<BlockCapsule> database) {
    super();
    this.database = database;
  }

  public BlockIndex(
      final TronDatabase<BlockCapsule> database,
      Persistence<WrappedByteArray, ? extends Comparable> persistence) {
    super(persistence);
    this.database = database;
  }

  @PostConstruct
  public void init() {
    index.addIndex(SuffixTreeIndex.onAttribute(Block_ID));
    index.addIndex(NavigableIndex.onAttribute(Block_NUMBER));
    index.addIndex(HashIndex.onAttribute(TRANSACTIONS));
    index.addIndex(NavigableIndex.onAttribute(WITNESS_ID));
    index.addIndex(SuffixTreeIndex.onAttribute(WITNESS_ADDRESS));
    index.addIndex(SuffixTreeIndex.onAttribute(OWNERS));
    index.addIndex(SuffixTreeIndex.onAttribute(TOS));
    fill();
  }
}
