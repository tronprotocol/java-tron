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
package org.apache.gossip.replication;

import org.apache.gossip.LocalMember;
import org.apache.gossip.model.Base;

/**
 * This interface is used to determine whether a data item needs to be replicated to
 * another gossip member.
 *
 * @param <T> A subtype of the class {@link org.apache.gossip.model.Base} which uses this interface
 */
public interface Replicable<T extends Base> {
  /**
   * Test for a given data item needs to be replicated.
   * @param me node that the data item is going to transmit from.
   * @param destination target node to replicate.
   * @param message this parameter is currently ignored
   * @return true if the data item needs to be replicated to the destination. Otherwise false.
   */
  boolean shouldReplicate(LocalMember me, LocalMember destination, T message);
}
