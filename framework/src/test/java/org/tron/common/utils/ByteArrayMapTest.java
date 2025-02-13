package org.tron.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.mchange.v2.collection.MapEntry;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.tron.core.db.ByteArrayWrapper;

public class ByteArrayMapTest {

  private ByteArrayMap<String> byteArrayMap;

  @Before
  public void setUp() {
    byteArrayMap = new ByteArrayMap<>();
  }

  @Test
  public void testPutAndGet() {
    byte[] key = "key1".getBytes();
    String value = "value1";
    byteArrayMap.put(key, value);
    assertEquals("Should return the correct value", value, byteArrayMap.get(key));
  }

  @Test
  public void testSize() {
    byte[] key1 = "key1".getBytes();
    byte[] key2 = "key2".getBytes();
    byteArrayMap.put(key1, "value1");
    byteArrayMap.put(key2, "value2");
    assertEquals("Should return the correct size", 2, byteArrayMap.size());
  }

  @Test
  public void testRemove() {
    byte[] key = "key".getBytes();
    String value = "value";
    byteArrayMap.put(key, value);
    byteArrayMap.remove(key);
    assertNull("Should return null after removal", byteArrayMap.get(key));
  }

  @Test
  public void testContainsKey() {
    byte[] key = "key".getBytes();
    byteArrayMap.put(key, "value");
    assertTrue("Should contain the key", byteArrayMap.containsKey(key));
  }

  @Test
  public void testPutAll() {
    Map<byte[], String> mapToPut = createTestMap();
    byteArrayMap.putAll(mapToPut);
    assertEquals("Should contain all entries after putAll", 2, byteArrayMap.size());
    assertEquals("Should return the correct value for key1",
        "value1", byteArrayMap.get("key1".getBytes()));
    assertEquals("Should return the correct value for key2",
        "value2", byteArrayMap.get("key2".getBytes()));

  }

  @Test
  public void testClear() {
    byte[] key1 = "key1".getBytes();
    byte[] key2 = "key2".getBytes();
    byteArrayMap.put(key1, "value1");
    byteArrayMap.put(key2, "value2");
    assertFalse(byteArrayMap.isEmpty());
    byteArrayMap.clear();
  }

  @Test
  public void testKeySet() {
    byte[] key1 = "key1".getBytes();
    byte[] key2 = "key2".getBytes();
    byteArrayMap.put(key1, "value1");
    byteArrayMap.put(key2, "value2");
    Set<byte[]> set = byteArrayMap.keySet();
    assertTrue("Key set should contain key1", set.contains(key1));
    assertTrue("Key set should contain key2", set.contains(key2));
  }

  @Test
  public void testValues() {
    byte[] key1 = "key1".getBytes();
    byte[] key2 = "key2".getBytes();
    byteArrayMap.put(key1, "value1");
    byteArrayMap.put(key2, "value2");
    Collection<String> values = byteArrayMap.values();
    assertTrue("Values should contain value1", values.contains("value1"));
    assertTrue("Values should contain value1", byteArrayMap.containsValue("value1"));
    assertTrue("Values should contain value2", values.contains("value2"));
  }

  @Test
  public void testEntrySet() {
    byte[] key1 = "key1".getBytes();
    byte[] key2 = "key2".getBytes();
    byteArrayMap.put(key1, "value1");
    byteArrayMap.put(key2, "value2");
    Set<Map.Entry<byte[], String>> entrySet = byteArrayMap.entrySet();
    assertFalse(entrySet.isEmpty());
    assertEquals("Entry set size should be 2", 2, entrySet.size());
    assertThrows(Exception.class, () -> entrySet.contains(new Object()));
    assertThrows(Exception.class, entrySet::toArray);
    assertThrows(Exception.class, () -> entrySet.toArray(new Map.Entry[0]));
    assertThrows(Exception.class, () -> entrySet.add(new MapEntry(key1, "value1")));
    assertThrows(Exception.class, () -> entrySet.remove(new MapEntry(key1, "value1")));
    assertThrows(Exception.class, () -> entrySet.containsAll(new HashSet<>()));
    assertThrows(Exception.class, () -> entrySet.removeAll(new HashSet<>()));
    assertThrows(Exception.class, () -> entrySet.addAll(new HashSet<>()));
    assertThrows(Exception.class, () -> entrySet.retainAll(new HashSet<>()));
    assertThrows(Exception.class, entrySet::clear);
  }

  // Helper method to create a map for testing putAll
  private Map<byte[], String> createTestMap() {
    Map<byte[], String> map = new ByteArrayMap<>();
    map.put("key1".getBytes(), "value1");
    map.put("key2".getBytes(), "value2");
    return map;
  }


  @Test
  public void test() {
    Map<byte[], String> map = new ByteArrayMap<>();
    Map<byte[], String> testMap = createTestMap();
    assertNotEquals(map, testMap);
    assertTrue(testMap.hashCode() <= 0);
    assertNotNull(testMap.toString());
  }
}
