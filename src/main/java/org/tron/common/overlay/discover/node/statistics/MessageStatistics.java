package org.tron.common.overlay.discover.node.statistics;

import lombok.extern.slf4j.Slf4j;
import org.tron.common.net.udp.message.UdpMessageTypeEnum;
import org.tron.common.overlay.message.Message;
import org.tron.core.net.message.FetchInvDataMessage;
import org.tron.core.net.message.InventoryMessage;
import org.tron.core.net.message.MessageTypes;
import org.tron.core.net.message.TransactionsMessage;

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

  public final MessageCount tronInSyncBlockChain = new MessageCount();
  public final MessageCount tronOutSyncBlockChain = new MessageCount();
  public final MessageCount tronInBlockChainInventory = new MessageCount();
  public final MessageCount tronOutBlockChainInventory = new MessageCount();
  public final MessageCount tronInInventory = new MessageCount();
  public final MessageCount tronOutInventory = new MessageCount();
  public final MessageCount tronInInventoryElement = new MessageCount();
  public final MessageCount tronOutInventoryElement = new MessageCount();
  public final MessageCount tronInFetchInvData = new MessageCount();
  public final MessageCount tronOutFetchInvData = new MessageCount();
  public final MessageCount tronInFetchInvDataElement = new MessageCount();
  public final MessageCount tronOutFetchInvDataElement = new MessageCount();
  public final MessageCount tronInTrx = new MessageCount();
  public final MessageCount tronOutTrx = new MessageCount();
  public final MessageCount tronInTrxs = new MessageCount();
  public final MessageCount tronOutTrxs = new MessageCount();
  public final MessageCount tronInBlock = new MessageCount();
  public final MessageCount tronOutBlock = new MessageCount();

  public void addUdpInMessage(UdpMessageTypeEnum type){
    addUdpMessage(type, true);
  }

  public void addUdpOutMessage(UdpMessageTypeEnum type){
    addUdpMessage(type, false);
  }

  public void addTcpInMessage(Message msg){
    addTcpMessage(msg, true);
  }

  public void addTcpOutMessage(Message msg){
    addTcpMessage(msg, false);
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

  private void addTcpMessage(Message msg, boolean flag){

    if (flag) {
      tronInMessage.add();
    } else {
      tronOutMessage.add();
    }

    switch (msg.getType()){
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
        if (flag) tronInSyncBlockChain.add(); else tronOutSyncBlockChain.add();
        break;
      case BLOCK_CHAIN_INVENTORY:
        if (flag) tronInBlockChainInventory.add(); else tronOutBlockChainInventory.add();
        break;
      case INVENTORY:
        InventoryMessage inventoryMessage = (InventoryMessage) msg;
        if (flag) {
          tronInInventory.add();
          tronInInventoryElement.add(inventoryMessage.getInventory().getIdsCount());
        } else {
          tronOutInventory.add();
          tronOutInventoryElement.add(inventoryMessage.getInventory().getIdsCount());
        }
        break;
      case FETCH_INV_DATA:
        FetchInvDataMessage fetchInvDataMessage = (FetchInvDataMessage) msg;
        if (flag) {
          tronInFetchInvData.add();
          tronInFetchInvDataElement.add(fetchInvDataMessage.getInventory().getIdsCount());
        } else {
          tronOutFetchInvData.add();
          tronOutFetchInvDataElement.add(fetchInvDataMessage.getInventory().getIdsCount());
        }
        break;
      case TRXS:
        TransactionsMessage transactionsMessage = (TransactionsMessage)msg;
        if (flag) {
          tronInTrxs.add();
          tronInTrx.add(transactionsMessage.getTransactions().getTransactionsCount());
        } else {
          tronOutTrxs.add();
          tronOutTrx.add(transactionsMessage.getTransactions().getTransactionsCount());
        }
        break;
      case TRX:
        if (flag) tronInMessage.add(); else tronOutMessage.add();
        break;
      case BLOCK:
        if (flag) tronInBlock.add(); tronOutBlock.add();
        break;
      default:
        break;
    }
  }

}
