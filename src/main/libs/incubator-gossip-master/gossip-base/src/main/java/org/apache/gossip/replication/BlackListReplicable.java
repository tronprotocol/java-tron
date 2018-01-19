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

import java.util.ArrayList;
import java.util.List;

/**
 * Replicable implementation which does not replicate data to given set of nodes.
 *
 * @param <T> A subtype of the class {@link org.apache.gossip.model.Base} which uses this interface
 * @see Replicable
 */
public class BlackListReplicable<T extends Base> implements Replicable<T> {
  
  private final List<LocalMember> blackListMembers;
  
  public BlackListReplicable(List<LocalMember> blackListMembers) {
    if (blackListMembers == null) {
      this.blackListMembers = new ArrayList<>();
    } else {
      this.blackListMembers = blackListMembers;
    }
  }

  public List<LocalMember> getBlackListMembers() {
    return blackListMembers;
  }

  @Override
  public boolean shouldReplicate(LocalMember me, LocalMember destination, T message) {
    return !blackListMembers.contains(destination);
  }
}
