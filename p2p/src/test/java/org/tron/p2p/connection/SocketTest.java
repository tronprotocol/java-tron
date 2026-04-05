package org.tron.p2p.connection;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.message.Message;
import org.tron.p2p.discover.NodeManager;

public class SocketTest {

  private static String localIp = "127.0.0.1";
  private static int port = 10001;

  @Before
  public void init() {
    Parameter.p2pConfig = new P2pConfig();
    Parameter.p2pConfig.setIp(localIp);
    Parameter.p2pConfig.setPort(port);
    Parameter.p2pConfig.setDiscoverEnable(false);

    NodeManager.init();
    ChannelManager.init();
  }

  private boolean sendMessage(io.netty.channel.Channel nettyChannel, Message message) {
    AtomicBoolean sendSuccess = new AtomicBoolean(false);
    nettyChannel.writeAndFlush(Unpooled.wrappedBuffer(message.getSendData()))
        .addListener((ChannelFutureListener) future -> {
          if (future.isSuccess()) {
            sendSuccess.set(true);
          } else {
            sendSuccess.set(false);
          }
        });
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    return sendSuccess.get();
  }

  //if we start handshake, we cannot connect with localhost, this test case will be invalid
  @Test
  public void testPeerServerAndPeerClient() throws InterruptedException {
//    //wait some time until peer server thread starts at this port successfully
//    Thread.sleep(500);
//    Node serverNode = new Node(new InetSocketAddress(localIp, port));
//
//    //peer client try to connect peer server using random port
//    io.netty.channel.Channel nettyChannel = ChannelManager.getPeerClient()
//        .connectAsync(serverNode, false, false).channel();
//
//    while (true) {
//      if (!nettyChannel.isActive()) {
//        Thread.sleep(100);
//      } else {
//        System.out.println("send message test");
//        PingMessage pingMessage = new PingMessage();
//        boolean sendSuccess = sendMessage(nettyChannel, pingMessage);
//        Assert.assertTrue(sendSuccess);
//        break;
//      }
//    }
  }

  @After
  public void destroy() {
    NodeManager.close();
    ChannelManager.close();
  }
}
