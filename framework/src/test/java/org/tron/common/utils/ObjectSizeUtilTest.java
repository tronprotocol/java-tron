/*
 * java-tron is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-tron is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.common.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ObjectSizeUtilTest {

  class Person {

    int age;
    String name;
    int[] scores;

    public Person() {
    }

    public Person(int age, String name, int[] scores) {
      this.age = age;
      this.name = name;
      this.scores = scores;
    }
  }

  @Test
  public void testGetObjectSize() {

    Person person = new Person();
    assertEquals(48, com.carrotsearch.sizeof.RamUsageEstimator.sizeOf(person));
    Person person1 = new Person(1, "tom", new int[]{});
    assertEquals(112, com.carrotsearch.sizeof.RamUsageEstimator.sizeOf(person1));

    Person person2 = new Person(1, "tom", new int[]{100});
    assertEquals(120, com.carrotsearch.sizeof.RamUsageEstimator.sizeOf(person2));

    Person person3 = new Person(1, "tom", new int[]{100, 100});
    assertEquals(120, com.carrotsearch.sizeof.RamUsageEstimator.sizeOf(person3));
    Person person4 = new Person(1, "tom", new int[]{100, 100, 100});
    assertEquals(128, com.carrotsearch.sizeof.RamUsageEstimator.sizeOf(person4));
    Person person5 = new Person(1, "tom", new int[]{100, 100, 100, 100});
    assertEquals(128, com.carrotsearch.sizeof.RamUsageEstimator.sizeOf(person5));
    Person person6 = new Person(1, "tom", new int[]{100, 100, 100, 100, 100});
    assertEquals(136, com.carrotsearch.sizeof.RamUsageEstimator.sizeOf(person6));

  }

}
