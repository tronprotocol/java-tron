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
package org.apache.gossip.event.data;

public class DataEventConstants {
  
  // MetricRegistry
  public static final String PER_NODE_DATA_SUBSCRIBERS_SIZE
          = "gossip.event.data.pernode.subscribers.size";
  public static final String PER_NODE_DATA_SUBSCRIBERS_QUEUE_SIZE
          = "gossip.event.data.pernode.subscribers.queue.size";
  public static final String SHARED_DATA_SUBSCRIBERS_SIZE
          = "gossip.event.data.shared.subscribers.size";
  public static final String SHARED_DATA_SUBSCRIBERS_QUEUE_SIZE
          = "gossip.event.data.shared.subscribers.queue.size";
  
  // Thread pool
  public static final int PER_NODE_DATA_QUEUE_SIZE = 64;
  public static final int PER_NODE_DATA_CORE_POOL_SIZE = 1;
  public static final int PER_NODE_DATA_MAX_POOL_SIZE = 30;
  public static final int PER_NODE_DATA_KEEP_ALIVE_TIME_SECONDS = 1;
  public static final int SHARED_DATA_QUEUE_SIZE = 64;
  public static final int SHARED_DATA_CORE_POOL_SIZE = 1;
  public static final int SHARED_DATA_MAX_POOL_SIZE = 30;
  public static final int SHARED_DATA_KEEP_ALIVE_TIME_SECONDS = 1;
  
}
