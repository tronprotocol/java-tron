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

import java.util.function.BiFunction;

@SuppressWarnings("rawtypes")
public class CrdtBiFunctionMerge implements BiFunction<Crdt,Crdt,Crdt> {

  @SuppressWarnings("unchecked")
  @Override
  public Crdt apply(Crdt t, Crdt u) {
    if (t == null && u == null){
      return null;
    } else if (t == null){
      return u;
    } else if (u == null){
      return t;
    }
    if (! u.getClass().equals(t.getClass())){
      throw new IllegalArgumentException( "Can not merge " + t.getClass() + " "+ u.getClass());
    }
    return t.merge(u);
  }

  @SuppressWarnings("unchecked")
  public static Crdt applyStatic(Crdt t, Crdt u){
    if (t == null && u == null){
      return null;
    } else if (t == null){
      return u;
    } else if (u == null){
      return t;
    }
    if (! u.getClass().equals(t.getClass())){
      throw new IllegalArgumentException( "Can not merge " + t.getClass() + " "+ u.getClass());
    }
    return t.merge(u);
  }
}
