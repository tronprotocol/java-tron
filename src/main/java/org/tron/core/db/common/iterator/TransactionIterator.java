package org.tron.core.db.common.iterator;

import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.exception.BadItemException;

import java.util.Iterator;
import java.util.Map.Entry;

public class TransactionIterator extends AbstractIterator<TransactionCapsule> {

  public TransactionIterator(Iterator<Entry<byte[], byte[]>> iterator) {
    super(iterator);
  }

  @Override
  protected TransactionCapsule of(byte[] value) {
    try {
      return new TransactionCapsule(value);
    } catch (BadItemException e) {
      throw new RuntimeException(e);
    }
  }
}
