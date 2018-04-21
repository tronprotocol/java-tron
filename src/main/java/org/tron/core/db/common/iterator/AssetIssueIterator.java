package org.tron.core.db.common.iterator;

import java.util.Iterator;
import java.util.Map.Entry;
import org.tron.core.capsule.AssetIssueCapsule;

public class AssetIssueIterator extends AbstractIterator<AssetIssueCapsule> {

  public AssetIssueIterator(Iterator<Entry<byte[], byte[]>> iterator) {
    super(iterator);
  }

  @Override
  public AssetIssueCapsule next() {
    Entry<byte[], byte[]> entry = iterator.next();
    return new AssetIssueCapsule(entry.getValue());
  }
}