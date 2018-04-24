package org.tron.core.db.common.iterator;

import java.util.Iterator;
import java.util.Map.Entry;
import org.tron.core.capsule.AccountCapsule;

public class AccountIterator extends AbstractIterator<AccountCapsule> {

  public AccountIterator(Iterator<Entry<byte[], byte[]>> iterator) {
    super(iterator);
  }

  @Override
  public AccountCapsule next() {
    Entry<byte[], byte[]> entry = iterator.next();
    return new AccountCapsule(entry.getValue());
  }
}
