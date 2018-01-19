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

import java.util.Map;

import org.apache.gossip.manager.GossipManager;

public class PNCounter implements CrdtCounter<Long, PNCounter> {

  private final GrowOnlyCounter pCount;

  private final GrowOnlyCounter nCount;

  PNCounter(Map<String, Long> pCounters, Map<String, Long> nCounters) {
    pCount = new GrowOnlyCounter(pCounters);
    nCount = new GrowOnlyCounter(nCounters);
  }

  public PNCounter(PNCounter starter, Builder builder) {
    GrowOnlyCounter.Builder pBuilder = builder.makeGrowOnlyCounterBuilder(builder.pCount());
    pCount = new GrowOnlyCounter(starter.pCount, pBuilder);
    GrowOnlyCounter.Builder nBuilder = builder.makeGrowOnlyCounterBuilder(builder.nCount());
    nCount = new GrowOnlyCounter(starter.nCount, nBuilder);
  }

  public PNCounter(Builder builder) {
    GrowOnlyCounter.Builder pBuilder = builder.makeGrowOnlyCounterBuilder(builder.pCount());
    pCount = new GrowOnlyCounter(pBuilder);
    GrowOnlyCounter.Builder nBuilder = builder.makeGrowOnlyCounterBuilder(builder.nCount());
    nCount = new GrowOnlyCounter(nBuilder);
  }

  public PNCounter(GossipManager manager) {
    pCount = new GrowOnlyCounter(manager);
    nCount = new GrowOnlyCounter(manager);
  }

  public PNCounter(PNCounter starter, PNCounter other) {
    pCount = new GrowOnlyCounter(starter.pCount, other.pCount);
    nCount = new GrowOnlyCounter(starter.nCount, other.nCount);
  }

  @Override
  public PNCounter merge(PNCounter other) {
    return new PNCounter(this, other);
  }

  @Override
  public Long value() {
    long pValue = (long) pCount.value();
    long nValue = (long) nCount.value();
    return pValue - nValue;
  }

  @Override
  public PNCounter optimize() {
    return new PNCounter(pCount.getCounters(), nCount.getCounters());
  }

  @Override
  public boolean equals(Object obj) {
    if (getClass() != obj.getClass())
      return false;
    PNCounter other = (PNCounter) obj;
    return value().longValue() == other.value().longValue();
  }

  @Override
  public String toString() {
    return "PnCounter [pCount=" + pCount + ", nCount=" + nCount + ", value=" + value() + "]";
  }

  Map<String, Long> getPCounters() {
    return pCount.getCounters();
  }

  Map<String, Long> getNCounters() {
    return nCount.getCounters();
  }

  public static class Builder {

    private final GossipManager myManager;

    private long value = 0L;

    public Builder(GossipManager gossipManager) {
      myManager = gossipManager;
    }

    public long pCount() {
      if (value > 0) {
        return value;
      }
      return 0;
    }

    public long nCount() {
      if (value < 0) {
        return -value;
      }
      return 0;
    }

    public org.apache.gossip.crdt.GrowOnlyCounter.Builder makeGrowOnlyCounterBuilder(long value) {
      org.apache.gossip.crdt.GrowOnlyCounter.Builder ret = new org.apache.gossip.crdt.GrowOnlyCounter.Builder(
              myManager);
      ret.increment(value);
      return ret;
    }

    public PNCounter.Builder increment(long delta) {
      value += delta;
      return this;
    }

    public PNCounter.Builder decrement(long delta) {
      value -= delta;
      return this;
    }
  }

}
