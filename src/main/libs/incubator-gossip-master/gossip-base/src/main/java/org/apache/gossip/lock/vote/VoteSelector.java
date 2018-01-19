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
package org.apache.gossip.lock.vote;

import java.util.Set;

/**
 * This interface defines vote selection algorithm for the vote based locking.
 */
public interface VoteSelector {
  /**
   * This method get call by the lock manager of a node to decide which candidate need to be choose for voting.
   *
   * @param voteCandidateIds node id set for the vote candidates
   * @return selected node id to vote from the given vote candidate set.
   */
  String getVoteCandidateId(Set<String> voteCandidateIds);
}
