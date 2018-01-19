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
package org.apache.gossip.model;

import org.apache.gossip.replication.AllReplicable;
import org.apache.gossip.replication.Replicable;

public class SharedDataMessage extends Base {

  private String nodeId;
  private String key;
  private Object payload;
  private Long timestamp;
  private Long expireAt;
  private Replicable<SharedDataMessage> replicable;

  public String getNodeId() {
    return nodeId;
  }
  public void setNodeId(String nodeId) {
    this.nodeId = nodeId;
  }
  public String getKey() {
    return key;
  }
  public void setKey(String key) {
    this.key = key;
  }
  public Object getPayload() {
    return payload;
  }
  public void setPayload(Object payload) {
    this.payload = payload;
  }
  public Long getTimestamp() {
    return timestamp;
  }
  public void setTimestamp(Long timestamp) {
    this.timestamp = timestamp;
  }
  public Long getExpireAt() {
    return expireAt;
  }
  public void setExpireAt(Long expireAt) {
    this.expireAt = expireAt;
  }
  
  public Replicable<SharedDataMessage> getReplicable() {
    return replicable;
  }
  
  public void setReplicable(Replicable<SharedDataMessage> replicable) {
    this.replicable = replicable;
  }
  
  @Override
  public String toString() {
    return "SharedGossipDataMessage [nodeId=" + nodeId + ", key=" + key + ", payload=" + payload
            + ", timestamp=" + timestamp + ", expireAt=" + expireAt
            + ", replicable=" + replicable + "]";
  }
}

