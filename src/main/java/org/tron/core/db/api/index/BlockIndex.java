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
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.protos.Protocol.Block;

@Component
@Slf4j
public class BlockIndex extends AbstractIndex<Block> {

  public static final Attribute<Block, String> Block_ID =
      attribute("block id",
          block -> Sha256Hash.of(block.getBlockHeader().toByteArray()).toString());
  public static final Attribute<Block, Long> Block_NUMBER =
      attribute("block number",
          block -> block.getBlockHeader().getRawData().getNumber());
  public static final Attribute<Block, String> TRANSACTIONS =
      attribute(String.class, "transactions",
          block -> block.getTransactionsList().stream()
              .map(t -> Sha256Hash.of(t.getRawData().toByteArray()).toString())
              .collect(Collectors.toList()));
  public static final Attribute<Block, Long> WITNESS_ID =
      attribute("witness id",
          block -> block.getBlockHeader().getRawData().getWitnessId());
  public static final Attribute<Block, String> WITNESS_ADDRESS =
      attribute("witness address",
          block -> ByteArray.toHexString(
              block.getBlockHeader().getRawData().getWitnessAddress().toByteArray()));

  public static final Attribute<Block, String> OWNERS =
      attribute(String.class, "owner address",
          b -> b.getTransactionsList().stream()
              .map(transaction -> transaction.getRawData().getContractList())
              .flatMap(List::stream)
              .map(TransactionCapsule::getOwner)
              .filter(Objects::nonNull)
              .distinct()
              .map(ByteArray::toHexString)
              .collect(Collectors.toList()));
  public static final Attribute<Block, String> TOS =
      attribute(String.class, "to address",
          b -> b.getTransactionsList().stream()
              .map(transaction -> transaction.getRawData().getContractList())
              .flatMap(List::stream)
              .map(TransactionCapsule::getToAddress)
              .filter(Objects::nonNull)
              .distinct()
              .map(ByteArray::toHexString)
              .collect(Collectors.toList()));

  public BlockIndex() {
    super();
  }

  public BlockIndex(Persistence<Block, ? extends Comparable> persistence) {
    super(persistence);
  }

  @PostConstruct
  public void init() {
    addIndex(SuffixTreeIndex.onAttribute(Block_ID));
    addIndex(NavigableIndex.onAttribute(Block_NUMBER));
    addIndex(HashIndex.onAttribute(TRANSACTIONS));
    addIndex(NavigableIndex.onAttribute(WITNESS_ID));
    addIndex(SuffixTreeIndex.onAttribute(WITNESS_ADDRESS));
    addIndex(SuffixTreeIndex.onAttribute(OWNERS));
    addIndex(SuffixTreeIndex.onAttribute(TOS));
  }
}
