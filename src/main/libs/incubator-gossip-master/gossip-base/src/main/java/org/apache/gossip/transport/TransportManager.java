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

import java.io.IOException;
import java.net.URI;

/** interface for manager that sends and receives messages that have already been serialized. */
public interface TransportManager {
  
  /** starts the active gossip thread responsible for reaching out to remote nodes. Not related to `startEndpoint()` */
  void startActiveGossiper();
  
  /** starts the passive gossip thread that receives messages from remote nodes. Not related to `startActiveGossiper()` */
  void startEndpoint();
  
  /** attempts to shutdown all threads. */
  void shutdown();
  
  /** sends a payload to an endpoint. */
  void send(URI endpoint, byte[] buf) throws IOException;
  
  /** gets the next payload being sent to this node */
  byte[] read() throws IOException;
}
