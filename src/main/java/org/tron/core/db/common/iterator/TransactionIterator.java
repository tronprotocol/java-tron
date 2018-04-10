package org.tron.core.db.common.iterator;

import java.util.Iterator;
import java.util.Map.Entry;
import org.tron.core.capsule.TransactionCapsule;

public class TransactionIterator extends AbstractIterator<TransactionCapsule> {

  public TransactionIterator(Iterator<Entry<byte[], byte[]>> iterator) {
    super(iterator);
  }

  @Override
  public TransactionCapsule next() {
    Entry<byte[], byte[]> entry = iterator.next();
    return new TransactionCapsule(entry.getValue());
  }
}
