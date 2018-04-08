package org.tron.core.db.api;

import static com.googlecode.cqengine.query.QueryFactory.attribute;

import com.googlecode.cqengine.attribute.Attribute;
import com.googlecode.cqengine.persistence.Persistence;
import java.util.stream.Collectors;
import org.tron.common.utils.Sha256Hash;
import org.tron.protos.Protocol.Block;

public class BlockIndex extends AbstractIndex<Block> {

  private static final Attribute<Block, String> Block_ID =
      attribute("block id",
          block -> Sha256Hash.of(block.getBlockHeader().toByteArray()).toString());
  private static final Attribute<Block, Long> Block_NUMBER =
      attribute("block number",
          block -> block.getBlockHeader().getRawData().getNumber());
  private static final Attribute<Block, String> TRANSACTIONS =
      attribute(String.class, "transactions",
          block -> block.getTransactionsList().stream()
              .map(t -> Sha256Hash.of(t.getRawData().toByteArray()).toString())
              .collect(Collectors.toList()));

  public BlockIndex(Persistence<Block, ? extends Comparable> persistence) {
    super(persistence);
  }
}
