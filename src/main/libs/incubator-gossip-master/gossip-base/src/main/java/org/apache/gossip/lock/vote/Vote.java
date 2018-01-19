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

import java.util.List;

/**
 * Store a voter details.
 */
public class Vote {
  private final String votingNode;
  private final Boolean voteValue; // TODO: 7/16/17  weight?
  private Boolean voteExchange;
  private final List<String> liveMembers;
  private final List<String> deadMembers;

  public Vote(String votingNode, Boolean voteValue, Boolean voteExchange, List<String> liveMembers,
          List<String> deadMembers) {
    this.votingNode = votingNode;
    this.voteValue = voteValue;
    this.voteExchange = voteExchange;
    this.liveMembers = liveMembers;
    this.deadMembers = deadMembers;
  }

  public String getVotingNode() {
    return votingNode;
  }

  public Boolean getVoteValue() {
    return voteValue;
  }

  public Boolean getVoteExchange() {
    return voteExchange;
  }

  public void setVoteExchange(Boolean voteExchange) {
    this.voteExchange = voteExchange;
  }

  public List<String> getLiveMembers() {
    return liveMembers;
  }

  public List<String> getDeadMembers() {
    return deadMembers;
  }

  @Override
  public String toString() {
    return "votingNode=" + votingNode + ", voteValue=" + voteValue + ", liveMembers=" + liveMembers
            + ", deadMembers= " + deadMembers;
  }
}
