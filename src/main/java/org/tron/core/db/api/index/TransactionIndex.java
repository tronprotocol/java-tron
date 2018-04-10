package org.tron.core.db.api.index;

import static com.googlecode.cqengine.query.QueryFactory.attribute;

import com.googlecode.cqengine.attribute.Attribute;
import com.googlecode.cqengine.attribute.SimpleAttribute;
import com.googlecode.cqengine.index.hash.HashIndex;
import com.googlecode.cqengine.index.navigable.NavigableIndex;
import com.googlecode.cqengine.index.suffix.SuffixTreeIndex;
import com.googlecode.cqengine.persistence.Persistence;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.protos.Protocol.Transaction;

@Component
@Slf4j
public class TransactionIndex extends AbstractIndex<Transaction> {

  public static final SimpleAttribute<Transaction, String> Transaction_ID =
      attribute("transaction id",
          t -> Sha256Hash.of(t.getRawData().toByteArray()).toString());
  public static final Attribute<Transaction, String> OWNERS =
      attribute(String.class, "owner address",
          t -> t.getRawData().getContractList().stream()
              .map(TransactionCapsule::getOwner)
              .filter(Objects::nonNull)
              .map(ByteArray::toHexString)
              .collect(Collectors.toList()));
  public static final Attribute<Transaction, String> TOS =
      attribute(String.class, "to address",
          t -> t.getRawData().getContractList().stream()
              .map(TransactionCapsule::getToAddress)
              .filter(Objects::nonNull)
              .map(ByteArray::toHexString)
              .collect(Collectors.toList()));
  public static final Attribute<Transaction, Long> TIMESTAMP =
      attribute("timestamp", t -> t.getRawData().getTimestamp());

  public TransactionIndex() {
    super();
  }

  public TransactionIndex(Persistence<Transaction, ? extends Comparable> persistence) {
    super(persistence);
  }

  @PostConstruct
  public void init() {
    addIndex(SuffixTreeIndex.onAttribute(Transaction_ID));
    addIndex(HashIndex.onAttribute(OWNERS));
    addIndex(HashIndex.onAttribute(TOS));
    addIndex(NavigableIndex.onAttribute(TIMESTAMP));
  }
}
