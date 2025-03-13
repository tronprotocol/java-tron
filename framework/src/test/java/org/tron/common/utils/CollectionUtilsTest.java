package org.tron.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import org.junit.Test;

public class CollectionUtilsTest {

  @Test
  public void testCollectList() {
    List<String> numbers = Arrays.asList("1", "2", "3");
    Function<String, Integer> toInt = Integer::parseInt;
    List<Integer> integers = CollectionUtils.collectList(numbers, toInt);
    assertEquals(Arrays.asList(1, 2, 3), integers);
  }

  @Test
  public void testCollectSet() {
    List<String> numbers = Arrays.asList("1", "2", "2", "3");
    Function<String, Integer> toInt = Integer::parseInt;
    Set<Integer> integers = CollectionUtils.collectSet(numbers, toInt);
    assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), integers);
  }

  @Test
  public void testTruncate() {
    List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
    List<Integer> truncated = CollectionUtils.truncate(numbers, 3);
    assertEquals(Arrays.asList(1, 2, 3), truncated);
  }

  @Test
  public void testTruncateNoLimit() {
    List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
    List<Integer> truncated = CollectionUtils.truncate(numbers, 10);
    assertEquals(numbers, truncated);
  }

  @Test
  public void testTruncateRandom() {
    List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
    List<Integer> numbers1 = Arrays.asList(1, 2);
    List<Integer> truncated = CollectionUtils.truncateRandom(numbers, 3, 2);
    List<Integer> truncated1 = CollectionUtils.truncateRandom(numbers1, 3, 2);
    assertEquals(2, truncated1.size());
    assertEquals(3, truncated.size());
    assertTrue(truncated.containsAll(Arrays.asList(1, 2, 3, 4, 5).subList(0, 2)));
    // Last element could be 3, 4, or 5, so we just check that it's one of them
    assertTrue(truncated.contains(3) || truncated.contains(4) || truncated.contains(5));
  }

  @Test
  public void testTruncateRandomConfirmEqualLimit() {
    List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
    List<Integer> truncated = CollectionUtils.truncateRandom(numbers, 5, 5);
    assertEquals(numbers, truncated);
  }

  @Test
  public void testSelectList() {
    List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
    Predicate<Integer> isEven = n -> n % 2 == 0;
    List<Integer> selected = CollectionUtils.selectList(numbers, isEven);
    assertEquals(Arrays.asList(2, 4), selected);
  }

  @Test
  public void testSelectSet() {
    List<Integer> numbers = Arrays.asList(1, 2, 2, 3, 4, 4, 5);
    Predicate<Integer> isEven = n -> n % 2 == 0;
    Set<Integer> selected = CollectionUtils.selectSet(numbers, isEven);
    assertEquals(new HashSet<>(Arrays.asList(2, 4)), selected);
  }
}
