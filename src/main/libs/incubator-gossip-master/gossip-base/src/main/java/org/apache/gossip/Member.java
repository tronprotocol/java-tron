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

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Map;

/**
 * An abstract class representing a gossip member.
 * 
 */
public abstract class Member implements Comparable<Member> {

  
  protected URI uri;

  protected volatile long heartbeat;

  protected String clusterName;

  /**
   * The purpose of the id field is to be able for nodes to identify themselves beyond their
   * host/port. For example an application might generate a persistent id so if they rejoin the
   * cluster at a different host and port we are aware it is the same node.
   */
  protected String id;

  /* properties provided at startup time */
  protected Map<String,String> properties;
  
  /**
   * Constructor.
   *
   * @param clusterName
   *          The name of the cluster 
   * @param uri
   *          A URI object containing IP/hostname and port
   * @param heartbeat
   *          The current heartbeat
   * @param id
   *          An id that may be replaced after contact
   */
  public Member(String clusterName, URI uri, String id, long heartbeat, Map<String,String> properties) {
    this.clusterName = clusterName;
    this.id = id;
    this.heartbeat = heartbeat;
    this.uri = uri;
    this.properties = properties;
  }

  protected Member(){}
  /**
   * Get the name of the cluster the member belongs to.
   * 
   * @return The cluster name
   */
  public String getClusterName() {
    return clusterName;
  }

 
  /**
   * @return The member address in the form IP/host:port Similar to the toString in
   * {@link InetSocketAddress}
   */
  public String computeAddress() {
    return uri.getHost() + ":" + uri.getPort();
  }

  /**
   * Get the heartbeat of this gossip member.
   * 
   * @return The current heartbeat.
   */
  public long getHeartbeat() {
    return heartbeat;
  }

  /**
   * Set the heartbeat of this gossip member.
   * 
   * @param heartbeat
   *          The new heartbeat.
   */
  public void setHeartbeat(long heartbeat) {
    this.heartbeat = heartbeat;
  }

  public String getId() {
    return id;
  }

  public void setId(String _id) {
    this.id = _id;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }

  public String toString() {
    return "Member [address=" + computeAddress() + ", id=" + id + ", heartbeat=" + heartbeat + "]";
  }

  /**
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    String address = computeAddress();
    result = prime * result + ((address == null) ? 0 : address.hashCode()) + (clusterName == null ? 0
            : clusterName.hashCode());
    return result;
  }

  public URI getUri() {
    return uri;
  }

  /**
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      System.err.println("equals(): obj is null.");
      return false;
    }
    if (!(obj instanceof Member)) {
      System.err.println("equals(): obj is not of type GossipMember.");
      return false;
    }
    // The object is the same of they both have the same address (hostname and port).
    return computeAddress().equals(((LocalMember) obj).computeAddress())
            && getClusterName().equals(((LocalMember) obj).getClusterName());
  }

  public int compareTo(Member other) {
    return this.computeAddress().compareTo(other.computeAddress());
  }
}
