package org.tron.common.utils;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;

public class SetAdapterTest {

  private Map<String, Object> delegate;
  private SetAdapter<String> setAdapter;

  @Before
  public void setUp() {
    delegate = new HashMap<>();
    setAdapter = new SetAdapter<>(delegate);
  }

  @Test
  public void testSizeInitiallyEmpty() {
    assertEquals(0, setAdapter.size());
  }

  @Test
  public void testIsEmptyInitially() {
    assertTrue(setAdapter.isEmpty());
  }

  @Test
  public void testContainsWhenEmpty() {
    assertFalse(setAdapter.contains("test"));
  }

  @Test
  public void testAddAndGetSize() {
    setAdapter.add("one");
    assertEquals(1, setAdapter.size());
  }

  @Test
  public void testAddAndCheckContains() {
    setAdapter.add("two");
    assertTrue(setAdapter.contains("two"));
  }

  @Test
  public void testRemoveAndCheckContains() {
    setAdapter.add("three");
    assertTrue(setAdapter.contains("three"));
    setAdapter.remove("three");
    assertFalse(setAdapter.contains("three"));
  }

  @Test
  public void testIterator() {
    setAdapter.add("four");
    setAdapter.add("five");
    Set<String> expected = new HashSet<>(Arrays.asList("four", "five"));
    Set<String> actual = new HashSet<>(setAdapter); // Convert to HashSet to ignore order
    assertEquals(expected, actual);
  }

  @Test
  public void testToArray() {
    setAdapter.add("six");
    setAdapter.add("seven");
    Object[] array = setAdapter.toArray();
    Arrays.sort(array); // Sorting to ignore order
    assertArrayEquals(new String[]{"seven", "six"}, array);
  }

  @Test
  public void testToArrayWithGivenType() {
    setAdapter.add("eight");
    String[] array = setAdapter.toArray(new String[0]);
    Arrays.sort(array); // Sorting to ignore order
    assertArrayEquals(new String[]{"eight"}, array);
  }

  @Test
  public void testAddAll() {
    setAdapter.addAll(Arrays.asList("nine", "ten"));
    assertTrue(setAdapter.containsAll(Arrays.asList("nine", "ten")));
  }

  @Test
  public void testRemoveAll() {
    setAdapter.addAll(Arrays.asList("eleven", "twelve"));
    setAdapter.removeAll(Collections.singletonList("eleven"));
    assertFalse(setAdapter.contains("eleven"));
    assertTrue(setAdapter.contains("twelve"));
  }

  @Test
  public void testClear() {
    setAdapter.addAll(Arrays.asList("thirteen", "fourteen"));
    setAdapter.clear();
    assertTrue(setAdapter.isEmpty());
  }

  @Test(expected = RuntimeException.class)
  public void testRetainAllThrowsException() {
    setAdapter.retainAll(Collections.emptyList());
  }
}
