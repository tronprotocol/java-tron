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
package org.apache.gossip.lock;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.apache.gossip.Member;
import org.apache.gossip.lock.exceptions.VoteFailedException;
import org.apache.gossip.lock.vote.MajorityVote;
import org.apache.gossip.lock.vote.Vote;
import org.apache.gossip.lock.vote.VoteCandidate;
import org.apache.gossip.manager.GossipManager;
import org.apache.gossip.model.SharedDataMessage;
import org.apache.log4j.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class LockManager {

  public static final Logger LOGGER = Logger.getLogger(LockManager.class);

  private final GossipManager gossipManager;
  private final LockManagerSettings lockSettings;
  private final ScheduledExecutorService voteService;
  private final AtomicInteger numberOfNodes;
  private final Set<String> lockKeys;
  // For MetricRegistry
  public static final String LOCK_KEY_SET_SIZE = "gossip.lock.key_set_size";
  public static final String LOCK_TIME = "gossip.lock.time";
  private final Timer lockTimeMetric;

  public LockManager(GossipManager gossipManager, final LockManagerSettings lockManagerSettings,
          MetricRegistry metrics) {
    this.gossipManager = gossipManager;
    this.lockSettings = lockManagerSettings;
    this.numberOfNodes = new AtomicInteger(lockSettings.getNumberOfNodes());
    this.lockKeys = new CopyOnWriteArraySet<>();
    metrics.register(LOCK_KEY_SET_SIZE, (Gauge<Integer>) lockKeys::size);
    lockTimeMetric = metrics.timer(LOCK_TIME);
    // Register listener for lock keys
    gossipManager.registerSharedDataSubscriber((key, oldValue, newValue) -> {
      if (key.contains("lock/")) {
        lockKeys.add(key);
      }
    });
    voteService = Executors.newScheduledThreadPool(2);
    voteService.scheduleAtFixedRate(this::updateVotes, 0, lockSettings.getVoteUpdateInterval(),
            TimeUnit.MILLISECONDS);
  }

  public void acquireSharedDataLock(String key) throws VoteFailedException {
    final Timer.Context context = lockTimeMetric.time();
    gossipManager.merge(generateLockMessage(key));
    int deadlockDetectCount = 0;
    while (true) {
      SharedDataMessage message = gossipManager.findSharedGossipData(generateLockKey(key));
      if (message == null || !(message.getPayload() instanceof MajorityVote)) {
        continue;
      }
      MajorityVote majorityVoteResult = (MajorityVote) message.getPayload();
      final Map<String, VoteCandidate> voteCandidatesMap = majorityVoteResult.value();
      final Map<String, Boolean> voteResultMap = new HashMap<>();
      // Store the vote result for each vote candidate nodes
      voteCandidatesMap.forEach((candidateId, voteCandidate) -> voteResultMap
              .put(candidateId, isVoteSuccess(voteCandidate)));

      long passedCandidates = voteResultMap.values().stream().filter(aBoolean -> aBoolean).count();
      String myNodeId = gossipManager.getMyself().getId();
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("NodeId=" + myNodeId + ", VoteMap=" + voteResultMap + ", WinnerCount="
                + passedCandidates);
      }
      // Check for possible dead lock when no candidates were won
      if (passedCandidates == 0) {
        if (isDeadLock(voteCandidatesMap)) {
          deadlockDetectCount++;
          // Testing for deadlock is not always correct, therefore test for continues deadlocks
          if (deadlockDetectCount >= lockSettings.getDeadlockDetectionThreshold()) {
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug("Deadlock detected from node " + myNodeId + ". VoteCandidatesMap="
                      + voteCandidatesMap);
            }
            preventDeadLock(voteCandidatesMap);
          }
        } else {
          deadlockDetectCount = 0;
        }
      } else if (passedCandidates == 1 && voteResultMap.containsKey(myNodeId)) {
        context.stop();
        if (voteResultMap.get(myNodeId)) {
          // There is one winner and that is my node, therefore break the while loop and continue
          break;
        } else {
          throw new VoteFailedException("Node " + myNodeId + " failed to lock on key: " + key);
        }
      } else if (passedCandidates > 1) {
        // Multiple winners are not possible
        context.stop();
        throw new IllegalStateException("Multiple nodes get voted.");
      }

      try {
        Thread.sleep(lockSettings.getResultCalculationDelay());
      } catch (InterruptedException e) {
        throw new VoteFailedException("Node " + myNodeId + " failed to lock on key: " + key, e);
      }
    }
  }

  // Generate Crdt lock message for voting
  private SharedDataMessage generateLockMessage(String key) {
    VoteCandidate voteCandidate = new VoteCandidate(gossipManager.getMyself().getId(), key,
            new ConcurrentHashMap<>());
    voteCandidate.addVote(new Vote(gossipManager.getMyself().getId(), true, false,
            gossipManager.getLiveMembers().stream().map(Member::getId).collect(Collectors.toList()),
            gossipManager.getDeadMembers().stream().map(Member::getId)
                    .collect(Collectors.toList())));
    Map<String, VoteCandidate> voteCandidateMap = new ConcurrentHashMap<>();
    voteCandidateMap.put(voteCandidate.getCandidateNodeId(), voteCandidate);
    MajorityVote majorityVote = new MajorityVote(voteCandidateMap);
    SharedDataMessage lockMessage = new SharedDataMessage();
    lockMessage.setKey(generateLockKey(key));
    lockMessage.setPayload(majorityVote);
    lockMessage.setExpireAt(Long.MAX_VALUE);
    lockMessage.setTimestamp(System.currentTimeMillis());
    return lockMessage;
  }

  // This method will run periodically to vote the other nodes
  private void updateVotes() {
    for (String lockKey : lockKeys) {
      SharedDataMessage message = gossipManager.findSharedGossipData(lockKey);
      if (message == null || !(message.getPayload() instanceof MajorityVote)) {
        continue;
      }
      MajorityVote majorityVote = (MajorityVote) message.getPayload();
      Map<String, VoteCandidate> voteCandidateMap = majorityVote.value();
      String myNodeId = gossipManager.getMyself().getId();
      // No need to vote if my node is already voted to every node for the key
      if (isVotedToAll(myNodeId, voteCandidateMap)) {
        continue;
      }
      String myVoteCandidate = getVotedCandidateNodeId(myNodeId, voteCandidateMap);

      if (myVoteCandidate == null) {
        myVoteCandidate = lockSettings.getVoteSelector().getVoteCandidateId(voteCandidateMap.keySet());
      }
      for (VoteCandidate voteCandidate : voteCandidateMap.values()) {
        if (voteCandidate.getCandidateNodeId().equals(myNodeId)) {
          continue;
        }
        // Vote for selected candidate
        boolean voteResult = voteCandidate.getCandidateNodeId().equals(myVoteCandidate);
        voteCandidate.addVote(new Vote(gossipManager.getMyself().getId(), voteResult, false,
                gossipManager.getLiveMembers().stream().map(Member::getId)
                        .collect(Collectors.toList()),
                gossipManager.getDeadMembers().stream().map(Member::getId)
                        .collect(Collectors.toList())));
      }
    }
  }

  // Return true if every node has a vote from given node id.
  private boolean isVotedToAll(String nodeId, final Map<String, VoteCandidate> voteCandidates) {
    int voteCount = 0;
    for (VoteCandidate voteCandidate : voteCandidates.values()) {
      if (voteCandidate.getVotes().containsKey(nodeId)) {
        voteCount++;
      }
    }
    return voteCount == voteCandidates.size();
  }

  // Returns true if there is a deadlock for given vote candidates
  private boolean isDeadLock(final Map<String, VoteCandidate> voteCandidates) {
    boolean result = true;
    int numberOfLiveNodes;
    if (numberOfNodes.get() > 0) {
      numberOfLiveNodes = numberOfNodes.get();
    } else {
      // numberOfNodes is not set by the user, therefore calculate it.
      Set<String> liveNodes = voteCandidates.values().stream()
              .map(voteCandidate -> voteCandidate.getVotes().values()).flatMap(Collection::stream)
              .map(Vote::getLiveMembers).flatMap(List::stream).collect(Collectors.toSet());
      numberOfLiveNodes = liveNodes.size();
    }
    for (VoteCandidate voteCandidate : voteCandidates.values()) {
      result = result && voteCandidate.getVotes().size() == numberOfLiveNodes;
    }
    return result;
  }

  // Prevent the deadlock by giving up the votes
  private void preventDeadLock(Map<String, VoteCandidate> voteCandidates) {
    String myNodeId = gossipManager.getMyself().getId();
    VoteCandidate myResults = voteCandidates.get(myNodeId);
    if (myResults == null) {
      return;
    }
    // Set of nodes that is going to receive this nodes votes
    List<String> donateCandidateIds = voteCandidates.keySet().stream()
            .filter(s -> s.compareTo(myNodeId) < 0).collect(Collectors.toList());
    if (donateCandidateIds.size() == 0) {
      return;
    }
    // Select a random node to donate
    Random randomizer = new Random();
    String selectedCandidateId = donateCandidateIds
            .get(randomizer.nextInt(donateCandidateIds.size()));
    VoteCandidate selectedCandidate = voteCandidates.get(selectedCandidateId);

    Set<Vote> myVotes = new HashSet<>(myResults.getVotes().values());
    Set<Vote> selectedCandidateVotes = new HashSet<>(selectedCandidate.getVotes().values());
    // Exchange the votes
    for (Vote myVote : myVotes) {
      for (Vote candidateVote : selectedCandidateVotes) {
        if (myVote.getVoteValue() && myVote.getVotingNode().equals(candidateVote.getVotingNode())) {
          myVote.setVoteExchange(true);
          candidateVote.setVoteExchange(true);
          selectedCandidate.getVotes().put(myVote.getVotingNode(), myVote);
          myResults.getVotes().put(candidateVote.getVotingNode(), candidateVote);
        }
      }
    }
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Node " + myNodeId + " give up votes to node " + selectedCandidateId);
    }
  }

  private String getVotedCandidateNodeId(String nodeId,
          final Map<String, VoteCandidate> voteCandidates) {
    for (VoteCandidate voteCandidate : voteCandidates.values()) {
      Vote vote = voteCandidate.getVotes().get(nodeId);
      if (vote != null && vote.getVoteValue()) {
        return voteCandidate.getCandidateNodeId();
      }
    }
    return null;
  }

  // Return true if the given candidate has passed the vote
  private boolean isVoteSuccess(VoteCandidate voteCandidate) {
    Set<String> liveNodes = new HashSet<>();
    int voteCount = 0;
    for (Vote vote : voteCandidate.getVotes().values()) {
      liveNodes.addAll(vote.getLiveMembers());
      if (vote.getVoteValue()) {
        voteCount++;
      }
    }
    int numberOfLiveNodes;
    if (numberOfNodes.get() > 0) {
      numberOfLiveNodes = numberOfNodes.get();
    } else {
      numberOfLiveNodes = liveNodes.size();
    }
    return numberOfLiveNodes > 0 && voteCount >= (numberOfLiveNodes / 2 + 1);
  }

  private String generateLockKey(String key){
    return "lock/" + key;
  }

  public void shutdown(){
    voteService.shutdown();
  }
  /**
   * Get the voted node id from this node for a given key
   * @param key key of the data object
   * @return Voted node id
   */
  public String getVotedCandidateNodeId(String key) {
    SharedDataMessage message = gossipManager.findSharedGossipData(generateLockKey(key));
    if (message == null || !(message.getPayload() instanceof MajorityVote)) {
      return null;
    }
    MajorityVote majorityVote = (MajorityVote) message.getPayload();
    return getVotedCandidateNodeId(gossipManager.getMyself().getId(), majorityVote.value());
  }

  /**
   * Set the number of live nodes. If this value is negative, live nodes will be calculated
   * @param numberOfNodes live node count or negative to calculate.
   */
  public void setNumberOfNodes(int numberOfNodes) {
    this.numberOfNodes.set(numberOfNodes);
  }

}
