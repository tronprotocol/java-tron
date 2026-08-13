package org.tron.p2p.discover.socket;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.stats.TrafficStats;

@Slf4j(topic = "net")
public class DiscoverServer {

  private Channel channel;
  private EventHandler eventHandler;

  private final int SERVER_RESTART_WAIT = 5000;
  private final int SERVER_CLOSE_WAIT = 10;
  private final int port = Parameter.p2pConfig.getPort();
  private volatile boolean shutdown = false;

  public void init(EventHandler eventHandler) {
    this.eventHandler = eventHandler;
    new Thread(() -> {
      try {
        start();
      } catch (Exception e) {
        log.error("Discovery server start failed", e);
      }
    }, "DiscoverServer").start();
  }

  public void close() {
    log.info("Closing discovery server...");
    shutdown = true;
    if (channel != null) {
      try {
        channel.close().await(SERVER_CLOSE_WAIT, TimeUnit.SECONDS);
      } catch (Exception e) {
        log.error("Closing discovery server failed", e);
      }
    }
  }

  private void start() throws Exception {
    NioEventLoopGroup group = new NioEventLoopGroup(Parameter.UDP_NETTY_WORK_THREAD_NUM,
        new BasicThreadFactory.Builder().namingPattern("discoverServer").build());
    try {
      while (!shutdown) {
        Bootstrap b = new Bootstrap();
        b.group(group)
            .channel(NioDatagramChannel.class)
            .handler(new ChannelInitializer<NioDatagramChannel>() {
              @Override
              public void initChannel(NioDatagramChannel ch)
                  throws Exception {
                ch.pipeline().addLast(TrafficStats.udp);
                ch.pipeline().addLast(new ProtobufVarint32LengthFieldPrepender());
                ch.pipeline().addLast(new ProtobufVarint32FrameDecoder());
                ch.pipeline().addLast(new P2pPacketDecoder());
                MessageHandler messageHandler = new MessageHandler(ch, eventHandler);
                eventHandler.setMessageSender(messageHandler);
                ch.pipeline().addLast(messageHandler);
              }
            });

        channel = b.bind(port).sync().channel();

        log.info("Discovery server started, bind port {}", port);

        channel.closeFuture().sync();
        if (shutdown) {
          log.info("Shutdown discovery server");
          break;
        }
        log.warn("Restart discovery server after 5 sec pause...");
        Thread.sleep(SERVER_RESTART_WAIT);
      }
    } catch (InterruptedException e) {
      log.warn("Discover server interrupted");
      Thread.currentThread().interrupt();
    } catch (Exception e) {
      log.error("Start discovery server with port {} failed", port, e);
    } finally {
      group.shutdownGracefully().sync();
    }
  }
}
