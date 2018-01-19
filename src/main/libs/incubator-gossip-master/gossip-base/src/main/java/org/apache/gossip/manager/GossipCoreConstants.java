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
package org.apache.gossip.manager;

public interface GossipCoreConstants {
  String PER_NODE_DATA_SIZE = "gossip.core.pernodedata.size"; 
  String SHARED_DATA_SIZE = "gossip.core.shareddata.size";
  String REQUEST_SIZE = "gossip.core.requests.size";
  String THREADPOOL_ACTIVE = "gossip.core.threadpool.active";
  String THREADPOOL_SIZE = "gossip.core.threadpool.size";
  String MESSAGE_SERDE_EXCEPTION = "gossip.core.message_serde_exception";
  String MESSAGE_TRANSMISSION_EXCEPTION = "gossip.core.message_transmission_exception";
  String MESSAGE_TRANSMISSION_SUCCESS = "gossip.core.message_transmission_success";
}
