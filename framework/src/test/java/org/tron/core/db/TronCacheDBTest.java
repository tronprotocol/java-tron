package org.tron.core.db;

import com.google.common.collect.Maps;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.tron.core.db2.common.TronCacheDB;
import org.tron.core.db2.common.WrappedByteArray;

public class TronCacheDBTest {

  private final TronCacheDB db = new TronCacheDB("tron-cache");

  @Test
  public void testCURD() {
    db.put("key1".getBytes(), "value1".getBytes());
    Assert.assertEquals("value1", new String(db.get("key1".getBytes())));
    Assert.assertNull(db.get("key2".getBytes()));
    db.put("key1".getBytes(), null);
    db.remove("key2".getBytes());
    db.remove(null);
    Assert.assertNotNull(db.get("key1".getBytes()));
    db.remove("key1".getBytes());
    Assert.assertNull(db.get("key1".getBytes()));
    Assert.assertTrue(db.isEmpty());
    db.put("key2".getBytes(), "value2".getBytes());
    Assert.assertFalse(db.isEmpty());
    Assert.assertEquals(1, db.size());
    db.iterator().forEachRemaining(entry -> {
      Assert.assertEquals("key2", new String(entry.getKey()));
      Assert.assertEquals("value2", new String(entry.getValue()));
    });
    Map<WrappedByteArray, WrappedByteArray> batch = Maps.newHashMap();
    batch.put(WrappedByteArray.copyOf("key3".getBytes()),
        WrappedByteArray.copyOf("value3".getBytes()));
    db.flush(batch);
    Assert.assertEquals(2, db.size());
    TronCacheDB newInstance = db.newInstance();
    newInstance.stat();
    Assert.assertTrue(newInstance.isEmpty());
  }
}