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
package org.apache.gossip.manager.handlers;

import org.apache.gossip.Member;
import org.apache.gossip.RemoteMember;
import org.apache.gossip.manager.GossipCore;
import org.apache.gossip.manager.GossipManager;
import org.apache.gossip.model.Base;
import org.apache.gossip.udp.UdpActiveGossipMessage;
import org.apache.gossip.udp.UdpActiveGossipOk;
import org.apache.gossip.udp.UdpNotAMemberFault;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class ActiveGossipMessageHandler implements MessageHandler {
  
  /**
   * @param gossipCore context.
   * @param gossipManager context.
   * @param base message reference.
   * @return boolean indicating success.
   */
  @Override
  public boolean invoke(GossipCore gossipCore, GossipManager gossipManager, Base base) {
    List<Member> remoteGossipMembers = new ArrayList<>();
    RemoteMember senderMember = null;
    UdpActiveGossipMessage activeGossipMessage = (UdpActiveGossipMessage) base;
    for (int i = 0; i < activeGossipMessage.getMembers().size(); i++) {
      URI u;
      try {
        u = new URI(activeGossipMessage.getMembers().get(i).getUri());
      } catch (URISyntaxException e) {
        GossipCore.LOGGER.debug("Gossip message with faulty URI", e);
        continue;
      }
      RemoteMember member = new RemoteMember(
              activeGossipMessage.getMembers().get(i).getCluster(),
              u,
              activeGossipMessage.getMembers().get(i).getId(),
              activeGossipMessage.getMembers().get(i).getHeartbeat(),
              activeGossipMessage.getMembers().get(i).getProperties());
      if (i == 0) {
        senderMember = member;
      }
      if (!(member.getClusterName().equals(gossipManager.getMyself().getClusterName()))) {
        UdpNotAMemberFault f = new UdpNotAMemberFault();
        f.setException("Not a member of this cluster " + i);
        f.setUriFrom(activeGossipMessage.getUriFrom());
        f.setUuid(activeGossipMessage.getUuid());
        GossipCore.LOGGER.warn(f);
        gossipCore.sendOneWay(f, member.getUri());
        continue;
      }
      remoteGossipMembers.add(member);
    }
    UdpActiveGossipOk o = new UdpActiveGossipOk();
    o.setUriFrom(activeGossipMessage.getUriFrom());
    o.setUuid(activeGossipMessage.getUuid());
    gossipCore.sendOneWay(o, senderMember.getUri());
    gossipCore.mergeLists(senderMember, remoteGossipMembers);
    return true;
  }
}
