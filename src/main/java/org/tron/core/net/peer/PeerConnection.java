package org.tron.core.net.peer;

import io.scalecube.cluster.Cluster;
import io.scalecube.cluster.Member;
import java.io.UnsupportedEncodingException;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.core.Sha256Hash;
import org.tron.core.net.message.Message;
import org.tron.core.net.message.MessageTypes;


public class PeerConnection {

  private static final Logger logger = LoggerFactory.getLogger("PeerConnection");


  //private
  private Member member;

  private Cluster cluster;

  //broadcast
  private Queue<Sha256Hash> invToUs = new LinkedBlockingQueue<>();

  private Queue<Sha256Hash> invWeAdv = new LinkedBlockingQueue<>();

  //sync chain
  private Sha256Hash lastBlockPeerKnow = Sha256Hash.ZERO_HASH;

  private Queue<Sha256Hash> chainIdsToFetch = new LinkedBlockingDeque<>();

  //private Queue<Sha256Hash> chainIdsPeerRequest = new LinkedBlockingQueue<>();

  private boolean needSyncFromPeer;

  private boolean needSyncFromUs;

  public Sha256Hash getLastBlockPeerKnow() {
    return lastBlockPeerKnow;
  }

  public void setLastBlockPeerKnow(Sha256Hash lastBlockPeerKnow) {
    this.lastBlockPeerKnow = lastBlockPeerKnow;
  }

  public Queue<Sha256Hash> getChainIdsToFetch() {
    return chainIdsToFetch;
  }

  public void setChainIdsToFetch(Queue<Sha256Hash> chainIdsToFetch) {
    this.chainIdsToFetch = chainIdsToFetch;
  }

  public boolean isNeedSyncFromPeer() {
    return needSyncFromPeer;
  }

  public void setNeedSyncFromPeer(boolean needSyncFromPeer) {
    this.needSyncFromPeer = needSyncFromPeer;
  }

  public boolean isNeedSyncFromUs() {
    return needSyncFromUs;
  }

  public void setNeedSyncFromUs(boolean needSyncFromUs) {
    this.needSyncFromUs = needSyncFromUs;
  }

  public Queue<Sha256Hash> getInvToUs() {
    return invToUs;
  }

  public void setInvToUs(Queue<Sha256Hash> invToUs) {
    this.invToUs = invToUs;
  }

  public Queue<Sha256Hash> getInvWeAdv() {
    return invWeAdv;
  }

  public void setInvWeAdv(Queue<Sha256Hash> invWeAdv) {
    this.invWeAdv = invWeAdv;
  }


  public PeerConnection(Cluster cluster, Member member) {
    this.cluster = cluster;
    this.member = member;
    //this.needSyncFromPeer = true;
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
