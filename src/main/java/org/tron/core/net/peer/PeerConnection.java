package org.tron.core.net.peer;

import io.scalecube.cluster.Cluster;
import io.scalecube.cluster.Member;
import java.io.UnsupportedEncodingException;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.net.message.Message;
import org.tron.core.net.message.MessageTypes;


public class PeerConnection {

  private static final Logger logger = LoggerFactory.getLogger("PeerConnection");

//  private PeerConnectionDelegate peerDel;

  private Member member;

  public Sha256Hash lastBlockWeKnow = Sha256Hash.ZERO_HASH;

  public Queue<Sha256Hash> blockToFetch = new LinkedBlockingDeque<>();

  public boolean needToSync = false;

  public boolean needSyncFrom = false;

  private Cluster cluster;

  public Queue<Sha256Hash> invToUs = new LinkedBlockingQueue<>();

  public Queue<Sha256Hash> invWeAdv = new LinkedBlockingQueue<>();

  public PeerConnection(Cluster cluster, Member member) {
    this.cluster = cluster;
    this.member = member;
  }

//  public void onMessage(PeerConnection peerConnection, Message msg) {
//    peerDel.onMessage(peerConnection, msg);
//  }
//
//  public Message getMessage(Sha256Hash msgId) {
//    return peerDel.getMessage(msgId);
//  }

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
    return "[" + "peer ID: " + member.id() + " peer IP:" + member.address() + "]";
  }
}
