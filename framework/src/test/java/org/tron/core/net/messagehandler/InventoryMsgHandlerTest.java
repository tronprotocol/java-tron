package org.tron.core.net.messagehandler;

import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import org.junit.Test;
import org.mockito.Mockito;
import org.tron.core.Constant;
import org.tron.core.config.args.Args;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.net.message.adv.InventoryMessage;
import org.tron.core.net.peer.PeerConnection;
import org.tron.p2p.connection.Channel;
import org.tron.protos.Protocol.Inventory.InventoryType;

public class InventoryMsgHandlerTest {

  @Test
  public void testProcessMessage() throws Exception {
    InventoryMsgHandler handler = new InventoryMsgHandler();
    Args.setParam(new String[] {"-w"}, Constant.TEST_CONF);
    Args.logConfig();

    InventoryMessage msg = new InventoryMessage(new ArrayList<>(), InventoryType.TRX);
    PeerConnection peer = new PeerConnection();
    peer.setChannel(getChannel("1.0.0.3", 1000));
    peer.setNeedSyncFromPeer(true);
    peer.setNeedSyncFromUs(true);
    handler.processMessage(peer, msg);

    peer.setNeedSyncFromPeer(true);
    peer.setNeedSyncFromUs(false);
    handler.processMessage(peer, msg);

    peer.setNeedSyncFromPeer(false);
    peer.setNeedSyncFromUs(true);
    handler.processMessage(peer, msg);

    peer.setNeedSyncFromUs(false);

    TronNetDelegate tronNetDelegate = mock(TronNetDelegate.class);
    Mockito.when(tronNetDelegate.isBlockUnsolidified()).thenReturn(true);

    Field field = handler.getClass().getDeclaredField("tronNetDelegate");
    field.setAccessible(true);
    field.set(handler, tronNetDelegate);

    handler.processMessage(peer, msg);
  }

  private Channel getChannel(String host, int port) throws Exception {
    Channel channel = new Channel();
    InetSocketAddress inetSocketAddress = new InetSocketAddress(host, port);

    Field field =  channel.getClass().getDeclaredField("inetSocketAddress");
    field.setAccessible(true);
    field.set(channel, inetSocketAddress);

    InetAddress inetAddress = inetSocketAddress.getAddress();
    field =  channel.getClass().getDeclaredField("inetAddress");
    field.setAccessible(true);
    field.set(channel, inetAddress);

    return channel;
  }
}
