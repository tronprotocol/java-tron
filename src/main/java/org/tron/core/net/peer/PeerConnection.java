package org.tron.core.net.peer;

import io.scalecube.cluster.Cluster;
import io.scalecube.cluster.Member;
import io.scalecube.transport.Address;
import java.io.UnsupportedEncodingException;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import javafx.util.Pair;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.net.message.Message;
import org.tron.core.net.message.MessageTypes;

@Slf4j
public class PeerConnection {

  @Override
  public int hashCode() {
    return member.hashCode();
  }

  //private
  private Member member;

  private Cluster cluster;

  //broadcast
  @Getter
  @Setter
  private Queue<Sha256Hash> invToUs = new LinkedBlockingQueue<>();

  @Getter
  @Setter
  private Queue<Sha256Hash> invWeAdv = new LinkedBlockingQueue<>();

  @Getter
  @Setter
  private HashMap<Sha256Hash, Long> advObjSpreadToUs = new HashMap<>();

  @Getter
  @Setter
  private HashMap<Sha256Hash, Long> advObjWeSpread = new HashMap<>();

  @Getter
  @Setter
  private HashMap<Sha256Hash, Long> advObjWeRequested = new HashMap<>();

  //sync chain
  @Getter
  @Setter
  private BlockId headBlockWeBothHave;

  @Getter
  @Setter
  private long headBlockTimeWeBothHave;

  @Getter
  private Deque<BlockId> syncBlockToFetch = new LinkedList<>();

  @Getter
  @Setter
  private HashMap<BlockId, Long> syncBlockRequested = new HashMap<>();

  @Getter
  @Setter
  private Pair<LinkedList<BlockId>, Long> syncChainRequested = null;

  @Getter
  @Setter
  private long unfetchSyncNum = 0L;

  @Getter
  @Setter
  private boolean needSyncFromPeer;

  @Getter
  @Setter
  private boolean needSyncFromUs;

  @Getter
  @Setter
  private boolean banned;

  @Getter
  @Setter
  private Set<BlockId> blockInProc = new HashSet<>();

  public Address getAddress() {
    return member.address();
  }

  public void cleanInvGarbage() {
    //TODO: clean advObjSpreadToUs and advObjWeSpread accroding cleaning strategy 
  }

  public PeerConnection(Cluster cluster, Member member) {
    this.cluster = cluster;
    this.member = member;
    //this.needSyncFromPeer = true;
  }

  public boolean isBusy() {
    return !advObjWeRequested.isEmpty()
        && !syncBlockRequested.isEmpty()
        && syncChainRequested != null;
  }

  public void sendMessage(Message message) {
    logger.info("Send message " + message + ", Peer:" + this);

    if (message == null) {
      logger.error("send message = null");
      return;
    }

    MessageTypes type = message.getType();
    byte[] value = message.getData();

    if (cluster == null) {
      logger.error("cluster is null");
      return;
    }

    try {
      io.scalecube.transport.Message msg = io.scalecube.transport.Message.builder()
          .data(new String(value, "ISO-8859-1"))
          .header("type", type.toString())
          .build();

      //logger.info("send message to member");
      cluster.send(member, msg);
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
  }

  @Override
  public String toString() {
    return "PeerConnection{" +
        "member=" + member +
        '}';
  }
}
