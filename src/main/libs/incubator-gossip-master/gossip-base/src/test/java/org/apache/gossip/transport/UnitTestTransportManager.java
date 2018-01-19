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

package org.apache.gossip.transport;

import org.apache.gossip.manager.GossipCore;
import org.apache.gossip.manager.GossipManager;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

/** Only use in unit tests! */
public class UnitTestTransportManager extends AbstractTransportManager { 
  
  private static final Map<URI, UnitTestTransportManager> allManagers = new ConcurrentHashMap<>();
  
  private final URI localEndpoint;
  private BlockingQueue<byte[]> buffers = new ArrayBlockingQueue<byte[]>(1000);
  
  public UnitTestTransportManager(GossipManager gossipManager, GossipCore gossipCore) {
    super(gossipManager, gossipCore);
    localEndpoint = gossipManager.getMyself().getUri();
  }

  @Override
  public void send(URI endpoint, byte[] buf) throws IOException {
    if (allManagers.containsKey(endpoint)) {
      try {
        allManagers.get(endpoint).buffers.put(buf);
      } catch (InterruptedException ex) {
        throw new RuntimeException(ex);
      }
    }
  }

  @Override
  public byte[] read() throws IOException {
    try {
      return buffers.take();
    } catch (InterruptedException ex) {
      // probably not the right thing to do, but we'll see.
      throw new IOException(ex);
    }
  }

  @Override
  public void shutdown() {
    allManagers.remove(localEndpoint);
    super.shutdown();
  }

  @Override
  public void startEndpoint() {
    allManagers.put(localEndpoint, this);
  }
}
