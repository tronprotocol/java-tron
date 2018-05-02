package org.tron.core.db.common.iterator;

import org.tron.core.capsule.TransactionCapsule;

import java.util.Iterator;
import java.util.Map.Entry;

public class TransactionIterator extends AbstractIterator<TransactionCapsule> {

  public TransactionIterator(Iterator<Entry<byte[], byte[]>> iterator) {
    super(iterator);
  }

  @Override
  protected TransactionCapsule of(byte[] value) {
    return new TransactionCapsule(value);
  }
}
