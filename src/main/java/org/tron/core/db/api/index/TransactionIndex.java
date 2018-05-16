package org.tron.core.db.api.index;

import com.googlecode.cqengine.attribute.Attribute;
import com.googlecode.cqengine.attribute.SimpleAttribute;
import com.googlecode.cqengine.index.disk.DiskIndex;
import com.googlecode.cqengine.index.hash.HashIndex;
import com.googlecode.cqengine.index.navigable.NavigableIndex;
import com.googlecode.cqengine.index.suffix.SuffixTreeIndex;
import com.googlecode.cqengine.persistence.Persistence;
import com.googlecode.cqengine.persistence.disk.DiskPersistence;
import com.googlecode.cqengine.persistence.offheap.OffHeapPersistence;
import com.googlecode.cqengine.persistence.onheap.OnHeapPersistence;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.TronDatabase;
import org.tron.core.db.common.WrappedByteArray;
import org.tron.protos.Protocol.Transaction;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.googlecode.cqengine.query.QueryFactory.attribute;

@Component
@Slf4j
public class TransactionIndex extends AbstractIndex<TransactionCapsule, Transaction> {

  public static SimpleAttribute<WrappedByteArray, String> Transaction_ID;
  public static Attribute<WrappedByteArray, String> OWNERS;
  public static Attribute<WrappedByteArray, String> TOS;
  public static Attribute<WrappedByteArray, Long> TIMESTAMP;

  @Autowired
  public TransactionIndex(
      @Qualifier("transactionStore") final TronDatabase<TransactionCapsule> database) {
    this.database = database;
  }

  @PostConstruct
  public void init() {
    initIndex(DiskPersistence.onPrimaryKeyInFile(Transaction_ID, indexPath));
//    index.addIndex(DiskIndex.onAttribute(Transaction_ID));
    index.addIndex(DiskIndex.onAttribute(OWNERS));
    index.addIndex(DiskIndex.onAttribute(TOS));
    index.addIndex(DiskIndex.onAttribute(TIMESTAMP));
  }

  @Override
  protected void setAttribute() {
    Transaction_ID =
        attribute("transaction id",
            bytes -> new TransactionCapsule(getObject(bytes)).getRawHash().toString());
    OWNERS =
        attribute(String.class, "owner address",
            bytes -> getObject(bytes).getRawData().getContractList().stream()
                .map(TransactionCapsule::getOwner)
                .filter(Objects::nonNull)
                .map(ByteArray::toHexString)
                .collect(Collectors.toList()));
    TOS =
        attribute(String.class, "to address",
            bytes -> getObject(bytes).getRawData().getContractList().stream()
                .map(TransactionCapsule::getToAddress)
                .filter(Objects::nonNull)
                .map(ByteArray::toHexString)
                .collect(Collectors.toList()));
    TIMESTAMP =
        attribute("timestamp", bytes -> getObject(bytes).getRawData().getTimestamp());

  }
}
