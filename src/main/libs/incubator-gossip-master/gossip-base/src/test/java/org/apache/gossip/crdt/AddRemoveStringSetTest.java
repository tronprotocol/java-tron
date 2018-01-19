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
 * Unle<F4>ss required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.gossip.crdt;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
  Abstract test suit to test CrdtSets with Add and Remove operations.
  You can use this suite only if your set supports multiple additions/deletions
    and has behavior similar to Set in single-threaded environment.
  It compares them with simple sets, validates add, remove, equals, value, etc. operations
  To use it you should:
  1. subclass this and implement constructors
  2. implement CrdtAddRemoveSet in your CrdtSet
  3. make your CrdtSet immutable
*/

@Ignore
public abstract class AddRemoveStringSetTest<SetType extends CrdtAddRemoveSet<String, Set<String>, SetType>> {

  abstract SetType construct(Set<String> set);

  abstract SetType construct();

  private Set<String> sampleSet;

  @Before
  public void setup(){
    sampleSet = new HashSet<>();
    sampleSet.add("4");
    sampleSet.add("5");
    sampleSet.add("12");
  }

  @Test
  public void abstractSetConstructorTest(){
    Assert.assertEquals(construct(sampleSet).value(), sampleSet);
  }

  @Test
  public void abstractStressWithSetTest(){
    Set<String> hashSet = new HashSet<>();
    SetType set = construct();
    for (int it = 0; it < 40; it++){
      SetType newSet;
      if (it % 5 == 1){
        //deleting existing
        String forDelete = hashSet.stream().skip((long) (hashSet.size() * Math.random())).findFirst().get();
        newSet = set.remove(forDelete);
        Assert.assertEquals(set.value(), hashSet); // check old version is immutable
        hashSet.remove(forDelete);
      } else {
        //adding
        String forAdd = String.valueOf((int) (10000 * Math.random()));
        newSet = set.add(forAdd);
        Assert.assertEquals(set.value(), hashSet); // check old version is immutable
        hashSet.add(forAdd);
      }
      set = newSet;
      Assert.assertEquals(set.value(), hashSet);
    }
  }

  @Test
  public void abstractEqualsTest(){
    SetType set = construct(sampleSet);
    Assert.assertFalse(set.equals(sampleSet));
    SetType newSet = set.add("25");
    sampleSet.add("25");
    Assert.assertFalse(newSet.equals(set));
    Assert.assertEquals(construct(sampleSet), newSet);
  }

  @Test
  public void abstractRemoveMissingTest(){
    SetType set = construct(sampleSet);
    set = set.add("25");
    set = set.remove("25");
    Assert.assertEquals(set.value(), sampleSet);
    set = set.remove("25");
    set = set.add("25");
    sampleSet.add("25");
    Assert.assertEquals(set.value(), sampleSet);
  }

  @Test
  public void abstractStressMergeTest(){
    // in one-process context, add, remove and merge operations of lww are equal to operations of Set
    // we've already checked it. Now just check merge
    Set<String> hashSet1 = new HashSet<>(), hashSet2 = new HashSet<>();
    SetType set1 = construct(), set2 = construct();

    for (int it = 0; it < 100; it++){
      String forAdd = String.valueOf((int) (10000 * Math.random()));
      if (it % 2 == 0){
        hashSet1.add(forAdd);
        set1 = set1.add(forAdd);
      } else {
        hashSet2.add(forAdd);
        set2 = set2.add(forAdd);
      }
    }
    Assert.assertEquals(set1.value(), hashSet1);
    Assert.assertEquals(set2.value(), hashSet2);
    Set<String> mergedSet = Stream.concat(hashSet1.stream(), hashSet2.stream()).collect(Collectors.toSet());
    Assert.assertEquals(set1.merge(set2).value(), mergedSet);
  }

  @Test
  public void abstractOptimizeTest(){
    Assert.assertEquals(construct(sampleSet).value(), sampleSet);
    Assert.assertEquals(construct(sampleSet).optimize().value(), sampleSet);
  }
}
