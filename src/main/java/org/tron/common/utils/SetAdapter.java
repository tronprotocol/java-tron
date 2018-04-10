/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.tron.common.utils;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class SetAdapter<E> implements Set<E> {

  private static final Object DummyValue = new Object();
  Map<E, Object> delegate;

  public SetAdapter(Map<E, ?> delegate) {
    this.delegate = (Map<E, Object>) delegate;
  }

  @Override
  public int size() {
    return delegate.size();
  }

  @Override
  public boolean isEmpty() {
    return delegate.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    return delegate.containsKey(o);
  }

  @Override
  public Iterator<E> iterator() {
    return delegate.keySet().iterator();
  }

  @Override
  public Object[] toArray() {
    return delegate.keySet().toArray();
  }

  @Override
  public <T> T[] toArray(T[] a) {
    return delegate.keySet().toArray(a);
  }

  @Override
  public boolean add(E e) {
    return delegate.put(e, DummyValue) == null;
  }

  @Override
  public boolean remove(Object o) {
    return delegate.remove(o) != null;
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    return delegate.keySet().containsAll(c);
  }

  @Override
  public boolean addAll(Collection<? extends E> c) {
    boolean ret = false;
    for (E e : c) {
      ret |= add(e);
    }
    return ret;
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    throw new RuntimeException("Not implemented"); // TODO add later if required
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    boolean ret = false;
    for (Object e : c) {
      ret |= remove(e);
    }
    return ret;
  }

  @Override
  public void clear() {
    delegate.clear();
  }
}
