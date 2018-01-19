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

package org.apache.gossip.protocol;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import org.apache.gossip.GossipSettings;
import org.apache.gossip.manager.PassiveGossipConstants;
import org.apache.gossip.model.Base;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// doesn't serialize anything besides longs. Uses a static lookup table to read and write objects.
public class UnitTestProtocolManager implements ProtocolManager {
  
  // so it can be shared across gossipers. this works as long as each object has a different memory address.
  private static final Map<Long, Base> lookup = new ConcurrentHashMap<>();
  private final Meter meter;
  
  public UnitTestProtocolManager(GossipSettings settings, String id, MetricRegistry registry) {
    meter = settings.isSignMessages() ?
        registry.meter(PassiveGossipConstants.SIGNED_MESSAGE) :
        registry.meter(PassiveGossipConstants.UNSIGNED_MESSAGE);
  }
  
  private static byte[] longToBytes(long val) {
    byte[] b = new byte[8];
    b[7] = (byte) (val);
    b[6] = (byte) (val >>>  8);
    b[5] = (byte) (val >>> 16);
    b[4] = (byte) (val >>> 24);
    b[3] = (byte) (val >>> 32);
    b[2] = (byte) (val >>> 40);
    b[1] = (byte) (val >>> 48);
    b[0] = (byte) (val >>> 56);
    return b;
  }

  static long bytesToLong(byte[] b) {
    return ((b[7] & 0xFFL)) +
        ((b[6] & 0xFFL) << 8) +
        ((b[5] & 0xFFL) << 16) +
        ((b[4] & 0xFFL) << 24) +
        ((b[3] & 0xFFL) << 32) +
        ((b[2] & 0xFFL) << 40) +
        ((b[1] & 0xFFL) << 48) +
        (((long) b[0]) << 56);
  }
  
  @Override
  public byte[] write(Base message) throws IOException {
    long hashCode = System.identityHashCode(message);
    byte[] serialized = longToBytes(hashCode);
    lookup.put(hashCode, message);
    meter.mark();
    return serialized;
  }

  @Override
  public Base read(byte[] buf) throws IOException {
    long hashCode = bytesToLong(buf);
    return lookup.remove(hashCode);
  }
}
