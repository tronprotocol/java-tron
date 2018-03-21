/*
 * java-tron is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-tron is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.common.overlay.node;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.config.args.Args;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.peer.PeerConnectionDelegate;
import io.scalecube.cluster.Cluster;
import io.scalecube.cluster.ClusterConfig;
import io.scalecube.cluster.membership.MembershipEvent;
import io.scalecube.cluster.membership.MembershipEvent.Type;
import io.scalecube.transport.Address;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

@Slf4j
public class GossipLocalNode implements LocalNode {
  private Cluster cluster = null;

  private PeerConnectionDelegate peerDel;

  private static final GossipLocalNode INSTANCE = new GossipLocalNode();

  //public HashMap<Integer, PeerConnection> listPeer = new HashMap<>();

  private ExecutorService executors;

  private CompositeSubscription subscriptions = new CompositeSubscription();

//  public Collection<PeerConnection> getValidPeer() {
//    //TODO: maintain a valid peer list here by some methods
//    return listPeer.values();
//  }

//  @Override
//  public void broadcast(Message message) {
//    listPeer.forEach((id, peer) -> peer.sendMessage(message));
//  }

  @Override
  public void start() {
    logger.info("listener message");

    ClusterConfig config = ClusterConfig.builder()
            .seedMembers(getAddresses())
            .portAutoIncrement(false)
        .port(Args.getInstance().getOverlay().getPort())
        .syncGroup(Args.getInstance().getChainId())
            .build();

    logger.info("sync group = {}", config.getSyncGroup());

    cluster = Cluster.joinAwait(config);

    //Peer connect
    cluster.otherMembers().forEach(member ->
        peerDel.connectPeer(new PeerConnection(this.cluster, member)));

    //liston peer's change
    Subscription membershipListener = cluster
            .listenMembership()
        .subscribe(event -> onEvent(event));

    executors = new ThreadPoolExecutor(4, 4, 0L, TimeUnit.MILLISECONDS,
        new ArrayBlockingQueue<>(1000), new ThreadPoolExecutor.CallerRunsPolicy());

    Subscription messageSubscription = cluster
            .listen()
            .subscribe(msg -> executors.submit(new StartWorker(msg, peerDel)));

    subscriptions.add(membershipListener);
    subscriptions.add(messageSubscription);
  }

  private void onEvent(MembershipEvent event) {
    if (event.type() == Type.REMOVED) {
      PeerConnection peer = new PeerConnection(this.cluster, event.oldMember());
      peerDel.disconnectPeer(peer);
    } else {
      PeerConnection peer = new PeerConnection(this.cluster, event.newMember());
      peerDel.connectPeer(peer);
    }
  }

  /**
   * stop gossip node.
   */
  public void stop() {
    cluster.shutdown();
    executors.shutdown();
    subscriptions.clear();
    cluster = null;
  }

  public void setPeerDel(PeerConnectionDelegate peerDel) {
    this.peerDel = peerDel;
  }

  public static GossipLocalNode getInstance() {
    return INSTANCE;
  }

  private List<Address> getAddresses() {
    return Args.getInstance().getSeedNode().getIpList().stream()
            .map(ip -> ip.split(":"))
            .filter(ipSplit -> ipSplit.length > 1)
            .map(ipSplit -> Address.create(ipSplit[0], Integer.valueOf(ipSplit[1])))
            .collect(Collectors.toList());
  }
}
