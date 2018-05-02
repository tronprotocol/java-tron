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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.TronDatabase;
import org.tron.core.db.common.WrappedByteArray;
import org.tron.protos.Protocol.Transaction;

@Component
@Slf4j
public class TransactionIndex extends AbstractIndex<TransactionCapsule, Transaction> {

  public final SimpleAttribute<WrappedByteArray, String> Transaction_ID =
      attribute("transaction id",
          bytes -> new TransactionCapsule(getObject(bytes)).getRawHash().toString());
  public final Attribute<WrappedByteArray, String> OWNERS =
      attribute(String.class, "owner address",
          bytes -> getObject(bytes).getRawData().getContractList().stream()
              .map(TransactionCapsule::getOwner)
              .filter(Objects::nonNull)
              .map(ByteArray::toHexString)
              .collect(Collectors.toList()));
  public final Attribute<WrappedByteArray, String> TOS =
      attribute(String.class, "to address",
          bytes -> getObject(bytes).getRawData().getContractList().stream()
              .map(TransactionCapsule::getToAddress)
              .filter(Objects::nonNull)
              .map(ByteArray::toHexString)
              .collect(Collectors.toList()));
  public final Attribute<WrappedByteArray, Long> TIMESTAMP =
      attribute("timestamp", bytes -> getObject(bytes).getRawData().getTimestamp());

  @Autowired
  public TransactionIndex(
      @Qualifier("transactionStore") final TronDatabase<TransactionCapsule> database) {
    super();
    this.database = database;
  }

  public TransactionIndex(
      final TronDatabase<TransactionCapsule> database,
      Persistence<WrappedByteArray, ? extends Comparable> persistence) {
    super(persistence);
    this.database = database;
  }

  @PostConstruct
  public void init() {
    index.addIndex(SuffixTreeIndex.onAttribute(Transaction_ID));
    index.addIndex(HashIndex.onAttribute(OWNERS));
    index.addIndex(HashIndex.onAttribute(TOS));
    index.addIndex(NavigableIndex.onAttribute(TIMESTAMP));
    fill();
  }
}
