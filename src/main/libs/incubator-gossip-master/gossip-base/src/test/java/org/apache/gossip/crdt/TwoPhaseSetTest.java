/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.gossip.crdt;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

public class TwoPhaseSetTest {

  private Set<String> sampleSet;

  @Before
  public void setup(){
    sampleSet = new HashSet<>();
    sampleSet.add("a");
    sampleSet.add("b");
    sampleSet.add("d");
  }

  @Test
  public void setConstructorTest(){
    Assert.assertEquals(new TwoPhaseSet<>(sampleSet).value(), sampleSet);
  }

  @Test
  public void valueTest(){
    Set<Character> added = new HashSet<>();
    added.add('a');
    added.add('b');
    Set<Character> removed = new HashSet<>();
    removed.add('b');
    Assert.assertEquals(new TwoPhaseSet<>(added, removed), new TwoPhaseSet<>('a'));
  }

  @Test
  public void optimizeTest(){
    TwoPhaseSet<String> set = new TwoPhaseSet<>(sampleSet);
    set = set.remove("b");
    Assert.assertEquals(set.optimize(), set);
    // check that optimize in this case actually works
    Assert.assertTrue(set.optimize().getAdded().size() < set.getAdded().size());
  }

  @Test
  public void immutabilityTest(){
    TwoPhaseSet<String> set = new TwoPhaseSet<>(sampleSet);
    TwoPhaseSet<String> newSet = set.remove("b");
    Assert.assertNotEquals(set, newSet);
    Assert.assertEquals(set, new TwoPhaseSet<>(sampleSet));
  }

  @Test
  public void removeMissingAddExistingLimitsTest(){
    BiConsumer<TwoPhaseSet<?>, TwoPhaseSet<?>> checkInternals = (f, s) -> {
      Assert.assertEquals(s, f);
      Assert.assertEquals(s.getRemoved(), f.getRemoved());
      Assert.assertEquals(s.getAdded(), f.getAdded());
    };
    TwoPhaseSet<String> set = new TwoPhaseSet<>(sampleSet);
    // remove missing
    checkInternals.accept(set, set.remove("e"));
    // add existing
    checkInternals.accept(set, set.add("a"));
    // limits
    TwoPhaseSet<String> newSet = set.remove("a"); // allow this remove
    Assert.assertEquals(newSet.add("a"), new TwoPhaseSet<>("b", "d")); // discard this add, "a" was added and removed
  }

  @Test
  public void mergeTest(){
    TwoPhaseSet<String> f = new TwoPhaseSet<>(sampleSet);
    TwoPhaseSet<String> s = new TwoPhaseSet<>("a", "c");
    s = s.remove("a");
    TwoPhaseSet<String> res = f.merge(s);
    Assert.assertEquals(res, new TwoPhaseSet<>(f, s)); // check two-sets constructor

    // "a" was both added and deleted in second set => it's deleted in result
    // "b" and "d" comes from first set and "c" comes from second
    Assert.assertEquals(res, new TwoPhaseSet<>("b", "c", "d"));
  }
}
