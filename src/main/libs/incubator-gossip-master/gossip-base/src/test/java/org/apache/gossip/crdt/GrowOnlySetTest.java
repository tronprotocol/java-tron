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

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Assert;
import org.junit.Test;

public class GrowOnlySetTest {

  @SuppressWarnings("rawtypes")
  @Test
  public void mergeTest(){
    ConcurrentHashMap<String, Crdt> a = new ConcurrentHashMap<>();
    GrowOnlySet<String> gset = new GrowOnlySet<>(Arrays.asList("a", "b"));
    Assert.assertEquals(gset, a.merge("a", gset, new CrdtBiFunctionMerge()));
    GrowOnlySet<String> over = new GrowOnlySet<>(Arrays.asList("b", "d"));
    Assert.assertEquals(new GrowOnlySet<>(Arrays.asList("a", "b", "d")), 
            a.merge("a", over, CrdtBiFunctionMerge::applyStatic));
  }
}
