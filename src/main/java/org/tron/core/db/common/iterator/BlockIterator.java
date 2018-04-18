package org.tron.core.db.common.iterator;

import java.util.Iterator;
import java.util.Map.Entry;
import org.tron.core.capsule.BlockCapsule;

public class BlockIterator extends AbstractIterator<BlockCapsule> {

  public BlockIterator(Iterator<Entry<byte[], byte[]>> iterator) {
    super(iterator);
  }

  @Override
  public BlockCapsule next() {
    Entry<byte[], byte[]> entry = iterator.next();
    return new BlockCapsule(entry.getValue());
  }
}
