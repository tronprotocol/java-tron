package org.tron.core.db.api.index;

import static com.googlecode.cqengine.query.QueryFactory.attribute;

import com.googlecode.cqengine.attribute.Attribute;
import com.googlecode.cqengine.index.hash.HashIndex;
import com.googlecode.cqengine.index.navigable.NavigableIndex;
import com.googlecode.cqengine.index.suffix.SuffixTreeIndex;
import com.googlecode.cqengine.persistence.Persistence;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tron.common.utils.Sha256Hash;
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
          block -> block.getBlockHeader().getRawData().getWitnessAddress().toStringUtf8());

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
  }
}
