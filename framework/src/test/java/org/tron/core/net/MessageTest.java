package org.tron.core.net;

import java.util.ArrayList;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.overlay.message.Message;
import org.tron.core.exception.P2pException;
import org.tron.core.net.message.MessageTypes;
import org.tron.core.net.message.adv.FetchInvDataMessage;
import org.tron.core.net.message.adv.InventoryMessage;
import org.tron.core.net.message.adv.TransactionsMessage;
import org.tron.core.net.message.base.DisconnectMessage;
import org.tron.core.net.service.statistics.MessageStatistics;
import org.tron.protos.Protocol.Inventory.InventoryType;
import org.tron.protos.Protocol.ReasonCode;

public class MessageTest {

  private DisconnectMessage disconnectMessage;

  @Test
  public void test1() throws Exception {
    byte[] bytes = new DisconnectMessage(ReasonCode.TOO_MANY_PEERS).getData();
    DisconnectMessageTest disconnectMessageTest = new DisconnectMessageTest();
    try {
      disconnectMessage = new DisconnectMessage(MessageTypes.P2P_DISCONNECT.asByte(),
          disconnectMessageTest.toByteArray());
    } catch (Exception e) {
      System.out.println(e.getMessage());
      Assert.assertTrue(e instanceof P2pException);
    }
  }

  public void test2() throws Exception {
    DisconnectMessageTest disconnectMessageTest = new DisconnectMessageTest();
    long startTime = System.currentTimeMillis();
    for (int i = 0; i < 100000; i++) {
      disconnectMessage = new DisconnectMessage(MessageTypes.P2P_DISCONNECT.asByte(),
          disconnectMessageTest.toByteArray());
    }
    long endTime = System.currentTimeMillis();
    System.out.println("spend time : " + (endTime - startTime));
  }

  @Test
  public void testMessageStatistics() {
    MessageStatistics messageStatistics = new MessageStatistics();
    Message message1 = new Message(MessageTypes.P2P_HELLO.asByte(), null) {
      @Override
      public Class<?> getAnswerMessage() {
        return null;
      }
    };
    Message message2 = new Message(MessageTypes.P2P_PING.asByte(), null) {
      @Override
      public Class<?> getAnswerMessage() {
        return null;
      }
    };
    Message message3 = new Message(MessageTypes.P2P_PONG.asByte(), null) {
      @Override
      public Class<?> getAnswerMessage() {
        return null;
      }
    };
    Message message4 = new Message(MessageTypes.P2P_DISCONNECT.asByte(), null) {
      @Override
      public Class<?> getAnswerMessage() {
        return null;
      }
    };
    Message message5 = new Message(MessageTypes.SYNC_BLOCK_CHAIN.asByte(), null) {
      @Override
      public Class<?> getAnswerMessage() {
        return null;
      }
    };
    Message message6 = new Message(MessageTypes.BLOCK_CHAIN_INVENTORY.asByte(), null) {
      @Override
      public Class<?> getAnswerMessage() {
        return null;
      }
    };
    Message message7 = new Message(MessageTypes.TRX.asByte(), null) {
      @Override
      public Class<?> getAnswerMessage() {
        return null;
      }
    };
    Message message8 = new Message(MessageTypes.BLOCK.asByte(), null) {
      @Override
      public Class<?> getAnswerMessage() {
        return null;
      }
    };
    InventoryMessage message9 = new InventoryMessage(new ArrayList<>(), InventoryType.TRX);
    FetchInvDataMessage message10 = new FetchInvDataMessage(new ArrayList<>(), InventoryType.TRX);
    TransactionsMessage message11 = new TransactionsMessage(new ArrayList<>());

    messageStatistics.addTcpInMessage(message1);
    messageStatistics.addTcpOutMessage(message1);
    messageStatistics.addTcpInMessage(message2);
    messageStatistics.addTcpOutMessage(message2);
    messageStatistics.addTcpInMessage(message3);
    messageStatistics.addTcpOutMessage(message3);
    messageStatistics.addTcpInMessage(message4);
    messageStatistics.addTcpOutMessage(message4);
    messageStatistics.addTcpInMessage(message5);
    messageStatistics.addTcpOutMessage(message5);
    try {
      Thread.sleep(2000);// so that gap > 1 in MessageCount.update method
    } catch (InterruptedException e) {
      //ignore
    }
    messageStatistics.addTcpInMessage(message6);
    messageStatistics.addTcpOutMessage(message6);
    messageStatistics.addTcpInMessage(message7);
    messageStatistics.addTcpOutMessage(message7);
    messageStatistics.addTcpInMessage(message8);
    messageStatistics.addTcpOutMessage(message8);
    messageStatistics.addTcpInMessage(message9);
    messageStatistics.addTcpOutMessage(message9);
    messageStatistics.addTcpInMessage(message10);
    messageStatistics.addTcpOutMessage(message10);
    messageStatistics.addTcpInMessage(message11);
    messageStatistics.addTcpOutMessage(message11);

    Assert.assertEquals(11, messageStatistics.tronInMessage.getTotalCount());
    Assert.assertEquals(11, messageStatistics.tronOutMessage.getTotalCount());
  }

}
