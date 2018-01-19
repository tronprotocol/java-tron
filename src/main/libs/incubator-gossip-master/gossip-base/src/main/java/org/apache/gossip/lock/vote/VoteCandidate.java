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

import java.util.Map;
import java.util.Objects;

/**
 * Stores the vote candidate details and its votes.
 */
public class VoteCandidate {

  private final String candidateNodeId;
  private final String votingKey;
  private final Map<String, Vote> votes;

  public VoteCandidate(String candidateNodeId, String votingKey, Map<String, Vote> votes) {

    this.candidateNodeId = candidateNodeId;
    this.votingKey = votingKey;
    this.votes = votes;
  }

  public String getCandidateNodeId() {
    return candidateNodeId;
  }

  public String getVotingKey() {
    return votingKey;
  }

  public Map<String, Vote> getVotes() {
    return votes;
  }

  public void addVote(Vote vote) {
    votes.put(vote.getVotingNode(), vote);
  }

  @Override
  public int hashCode() {
    return Objects.hash(candidateNodeId, votingKey);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof VoteCandidate))
      return false;
    if (obj == this)
      return true;
    VoteCandidate other = (VoteCandidate) obj;
    return this.candidateNodeId.equals(other.candidateNodeId) && this.votingKey
            .equals(other.votingKey);
  }

  @Override
  public String toString() {
    return "candidateNodeId=" + candidateNodeId + ", votingKey=" + votingKey + ", votes= " + votes;
  }
}
