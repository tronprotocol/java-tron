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
package org.tron.gossip;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * The object represents a gossip member with the properties as received from a remote gossip
 * member.
 * 
 */
public class RemoteMember extends Member {

  /**
   * Constructor.
   * 
   * @param uri
   *          A URI object containing IP/hostname and port
   * @param heartbeat
   *          The current heartbeat
   */
  public RemoteMember(String clusterName, URI uri, String id, long heartbeat, Map<String,String> properties) {
    super(clusterName, uri, id, heartbeat, properties);
  }

  public RemoteMember(String clusterName, URI uri, String id) {
    super(clusterName, uri, id, System.nanoTime(), new HashMap<String,String>());
  }

}
