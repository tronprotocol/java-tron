package org.tron.core.db.common.iterator;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.tron.core.capsule.WitnessCapsule;

public class WitnessIterator extends AbstractIterator<WitnessCapsule> {

  public WitnessIterator(Iterator<Entry<byte[], byte[]>> iterator) {
    super(iterator);
  }

  @Override
  public WitnessCapsule next() {
    Map.Entry<byte[], byte[]> entry = iterator.next();
    return new WitnessCapsule(entry.getValue());
  }
}
