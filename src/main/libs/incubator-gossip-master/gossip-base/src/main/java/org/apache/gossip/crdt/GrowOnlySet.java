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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public class GrowOnlySet<ElementType> implements CrdtSet<ElementType, Set<ElementType>, GrowOnlySet<ElementType>>{

  private final Set<ElementType> hidden = new LinkedHashSet<>();
  
  @SuppressWarnings("unused")
  /*
   * Used by SerDe
   */
  private GrowOnlySet(){
    
  }
  
  public GrowOnlySet(Set<ElementType> c){
    hidden.addAll(c);
  }
  
  public GrowOnlySet(Collection<ElementType> c){
    hidden.addAll(c);
  }
  
  public GrowOnlySet(GrowOnlySet<ElementType> first, GrowOnlySet<ElementType> second){
    hidden.addAll(first.value());
    hidden.addAll(second.value());
  }
  
  @Override
  public GrowOnlySet<ElementType> merge(GrowOnlySet<ElementType> other) {
    return new GrowOnlySet<>(this, other);
  }

  @Override
  public Set<ElementType> value() {
    Set<ElementType> copy = new LinkedHashSet<>();
    copy.addAll(hidden);
    return Collections.unmodifiableSet(copy);
  }
  
  @Override
  public GrowOnlySet<ElementType> optimize() {
    return new GrowOnlySet<>(hidden);
  }

  public int size() {
    return hidden.size();
  }

  public boolean isEmpty() {
    return hidden.isEmpty();
  }

  public boolean contains(Object o) {
    return hidden.contains(o);
  }

  public Iterator<ElementType> iterator() {
    Set<ElementType> copy = new HashSet<>();
    copy.addAll(hidden);
    return copy.iterator();
  }

  public Object[] toArray() {
    return hidden.toArray();
  }

  public <T> T[] toArray(T[] a) {
    return hidden.toArray(a);
  }

  public boolean add(ElementType e) {
    throw new UnsupportedOperationException();
  }

  public boolean remove(Object o) {
    throw new UnsupportedOperationException();
  }

  public boolean containsAll(Collection<?> c) {
    return hidden.containsAll(c);
  }

  public boolean addAll(Collection<? extends ElementType> c) {
    throw new UnsupportedOperationException();
  }

  public boolean retainAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  public boolean removeAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  public void clear() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String toString() {
    return "GrowOnlySet [hidden=" + hidden + "]";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((hidden == null) ? 0 : hidden.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    @SuppressWarnings("rawtypes")
    GrowOnlySet other = (GrowOnlySet) obj;
    if (hidden == null) {
      if (other.hidden != null)
        return false;
    } else if (!hidden.equals(other.hidden))
      return false;
    return true;
  }

  Set<ElementType> getElements(){
    return hidden;
  }
}
