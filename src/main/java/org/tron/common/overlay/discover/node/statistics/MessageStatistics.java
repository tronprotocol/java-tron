package org.tron.common.overlay.discover.node.statistics;

import lombok.extern.slf4j.Slf4j;
import org.tron.common.net.udp.message.UdpMessageTypeEnum;
import org.tron.core.net.message.MessageTypes;

@Slf4j
public class MessageStatistics {

  //udp discovery
  public final MessageCount discoverInPing = new MessageCount();
  public final MessageCount discoverOutPing = new MessageCount();
  public final MessageCount discoverInPong = new MessageCount();
  public final MessageCount discoverOutPong = new MessageCount();
  public final MessageCount discoverInFindNode = new MessageCount();
  public final MessageCount discoverOutFindNode = new MessageCount();
  public final MessageCount discoverInNeighbours = new MessageCount();
  public final MessageCount discoverOutNeighbours = new MessageCount();

  //tcp p2p
  public final MessageCount p2pInHello = new MessageCount();
  public final MessageCount p2pOutHello = new MessageCount();
  public final MessageCount p2pInPing = new MessageCount();
  public final MessageCount p2pOutPing = new MessageCount();
  public final MessageCount p2pInPong = new MessageCount();
  public final MessageCount p2pOutPong = new MessageCount();
  public final MessageCount p2pInDisconnect = new MessageCount();
  public final MessageCount p2pOutDisconnect = new MessageCount();

  //tcp tron
  public final MessageCount tronInMessage = new MessageCount();
  public final MessageCount tronOutMessage = new MessageCount();


  public void addUdpInMessage(UdpMessageTypeEnum type){
    addUdpMessage(type, true);
  }

  public void addUdpOutMessage(UdpMessageTypeEnum type){
    addUdpMessage(type, false);
  }

  public void addTcpInMessage(MessageTypes type){
    addTcpMessage(type, true);
  }

  public void addTcpOutMessage(MessageTypes type){
    addTcpMessage(type, false);
  }

  private void addUdpMessage(UdpMessageTypeEnum type, boolean flag){
    switch (type){
      case DISCOVER_PING:
        if (flag) discoverInPing.add(); else discoverOutPing.add();
        break;
      case DISCOVER_PONG:
        if (flag) discoverInPong.add(); else discoverOutPong.add();
        break;
      case DISCOVER_FIND_NODE:
        if (flag) discoverInFindNode.add(); else discoverOutFindNode.add();
        break;
      case DISCOVER_NEIGHBORS:
        if (flag) discoverInNeighbours.add(); else discoverOutNeighbours.add();
        break;
      default:
        break;
    }
  }

  private void addTcpMessage(MessageTypes type, boolean flag){
    switch (type){
      case P2P_HELLO:
        if (flag) p2pInHello.add(); else p2pOutHello.add();
        break;
      case P2P_PING:
        if (flag) p2pInPing.add(); else p2pOutPing.add();
        break;
      case P2P_PONG:
        if (flag) p2pInPong.add(); else p2pOutPong.add();
        break;
      case P2P_DISCONNECT:
        if (flag) p2pInDisconnect.add(); else p2pOutDisconnect.add();
        break;
      case SYNC_BLOCK_CHAIN:
      case BLOCK_CHAIN_INVENTORY:
      case INVENTORY:
      case FETCH_INV_DATA:
      case BLOCK:
      case TRXS:
      case TRX:
        if (flag) tronInMessage.add(); else tronOutMessage.add();
        break;
      default:
        break;
    }
  }

}
