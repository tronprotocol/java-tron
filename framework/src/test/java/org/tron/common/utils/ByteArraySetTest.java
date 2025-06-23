package org.tron.common.utils;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class ByteArraySetTest {

  private ByteArraySet byteArraySet;

  @Before
  public void setUp() {
    byteArraySet = new ByteArraySet();
  }

  @Test
  public void testSizeIsEmptyInitially() {
    assertEquals(0, byteArraySet.size());
  }

  @Test
  public void testIsEmptyInitially() {
    assertTrue(byteArraySet.isEmpty());
  }

  @Test
  public void testAddAndGetSize() {
    byte[] bytes1 = {1, 2, 3};
    byte[] bytes2 = {4, 5, 6};

    byteArraySet.add(bytes1);
    assertEquals(1, byteArraySet.size());

    byteArraySet.add(bytes2);
    assertEquals(2, byteArraySet.size());
  }

  @Test
  public void testContains() {
    byte[] bytes1 = {1, 2, 3};
    byte[] bytes2 = {4, 5, 6};

    byteArraySet.add(bytes1);
    assertTrue(byteArraySet.contains(bytes1));
    assertFalse(byteArraySet.contains(bytes2));

    byteArraySet.add(bytes2);
    assertTrue(byteArraySet.contains(bytes2));

    assertThrows(Exception.class, () -> byteArraySet.containsAll(new HashSet<>()));
    assertThrows(Exception.class, () -> byteArraySet.retainAll(new HashSet<>()));
    assertThrows(Exception.class, () -> byteArraySet.removeAll(new HashSet<>()));
    assertThrows(Exception.class, () -> byteArraySet.equals(new ByteArraySet()));
    assertThrows(Exception.class, () -> byteArraySet.hashCode());
  }

  @Test
  public void testIterator() {
    byte[] bytes1 = {1, 2, 3};
    byte[] bytes2 = {4, 5, 6};

    byteArraySet.add(bytes1);
    byteArraySet.add(bytes2);

    Iterator<byte[]> iterator = byteArraySet.iterator();

    assertTrue(iterator.hasNext());
    assertArrayEquals(bytes1, iterator.next());

    assertTrue(iterator.hasNext());
    assertArrayEquals(bytes2, iterator.next());

    assertFalse(iterator.hasNext());
  }

  @Test
  public void testToArray() {
    byte[] bytes1 = {1, 2, 3};
    byte[] bytes2 = {4, 5, 6};

    byteArraySet.add(bytes1);
    byteArraySet.add(bytes2);

    byte[][] array = byteArraySet.toArray(new byte[0][]);

    assertEquals(2, array.length);
    assertArrayEquals(bytes1, array[0]);
    assertArrayEquals(bytes2, array[1]);
  }

  @Test
  public void testAddAll() {
    List<byte[]> list = Arrays.asList(
        new byte[]{1, 2, 3},
        new byte[]{4, 5, 6}
    );

    boolean result = byteArraySet.addAll(list);

    assertTrue(result);
    assertEquals(2, byteArraySet.size());
    assertTrue(byteArraySet.contains(new byte[]{1, 2, 3}));
    assertTrue(byteArraySet.contains(new byte[]{4, 5, 6}));
  }

  @Test
  public void testRemove() {
    byte[] bytes1 = {1, 2, 3};
    byte[] bytes2 = {4, 5, 6};

    byteArraySet.add(bytes1);
    byteArraySet.add(bytes2);

    boolean result = byteArraySet.remove(bytes1);

    assertTrue(result);
    assertEquals(1, byteArraySet.size());
    assertFalse(byteArraySet.contains(bytes1));
    assertTrue(byteArraySet.contains(bytes2));
  }

  @Test
  public void testClear() {
    byte[] bytes1 = {1, 2, 3};
    byte[] bytes2 = {4, 5, 6};

    byteArraySet.add(bytes1);
    byteArraySet.add(bytes2);
    assertTrue(byteArraySet.size() > 0);

    byteArraySet.clear();
  }
}
