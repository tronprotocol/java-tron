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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PairTest {

  @Test
  public void testPairObject() {
    Pair<String, String> aPair = new Pair<>("key", "value");
    assertEquals("key", aPair.getKey());
    assertEquals("value", aPair.getValue());
    assertEquals("key=value", aPair.toString());      
  }

  @Test
  public void testPairObjectEquality() {
    Pair<String, String> aPair = new Pair<>("key", "value");
    Pair<String, String> aPair2 = aPair;
    Pair<String, String> anotherPair = new Pair<>("key", "value");
    // reference equality checks
    assertTrue(aPair == aPair2);
    assertFalse(aPair == anotherPair);
    // value equality checks
    assertTrue(aPair.equals(aPair2));
    assertTrue(aPair.equals(anotherPair));
  }  
}
