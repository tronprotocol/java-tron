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
package org.apache.gossip;

import java.net.URI;
import java.util.Map;

import org.apache.gossip.accrual.FailureDetector;

/**
 * This object represent a gossip member with the properties known locally. These objects are stored
 * in the local list of gossip members.
 * 
 */
public class LocalMember extends Member {
  /** The failure detector for this member */
  private transient FailureDetector detector;

  /**
   * 
   * @param uri
   *          The uri of the member
   * @param id
   *          id of the node
   * @param heartbeat
   *          The current heartbeat
   */
  public LocalMember(String clusterName, URI uri, String id,
          long heartbeat, Map<String,String> properties, int windowSize, int minSamples, String distribution) {
    super(clusterName, uri, id, heartbeat, properties );
    detector = new FailureDetector(minSamples, windowSize, distribution);
  }

  protected LocalMember(){
    
  }
  
  public void recordHeartbeat(long now){
    detector.recordHeartbeat(now);
  }
  
  public Double detect(long now) {
    return detector.computePhiMeasure(now);
  }

  @Override
  public String toString() {
    Double d = null;
    try {
      d = detect(System.nanoTime());
    } catch (RuntimeException ex) {}
    return "LocalGossipMember [uri=" + uri + ", heartbeat=" + heartbeat + ", clusterName="
            + clusterName + ", id=" + id + ", currentdetect=" + d  +" ]";
  }

}