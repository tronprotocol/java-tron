package org.tron.p2p.discover.socket;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.stats.TrafficStats;

@Slf4j(topic = "net")
public class DiscoverServer {

  private volatile Channel channel;
  private EventHandler eventHandler;

  private static final int SERVER_RESTART_WAIT_SECONDS = 5;
  private static final int SERVER_CLOSE_WAIT_SECONDS = 10;
  private static final int SERVER_START_WAIT_SECONDS = 10;
  private final int port = Parameter.p2pConfig.getPort();
  private volatile boolean shutdown = false;
  private final CompletableFuture<Void> initialBind = new CompletableFuture<>();

  public void init(EventHandler eventHandler) {
    this.eventHandler = eventHandler;
    new Thread(() -> {
      try {
        start();
      } catch (Exception e) {
        initialBind.completeExceptionally(e);
        logger.error("Discovery server start failed", e);
      }
    }, "DiscoverServer").start();
    try {
      initialBind.get(SERVER_START_WAIT_SECONDS, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while starting discovery server", e);
    } catch (ExecutionException e) {
      if (e.getCause() instanceof IllegalStateException) {
        throw (IllegalStateException) e.getCause();
      }
      throw new IllegalStateException("Failed to bind UDP discovery port " + port, e.getCause());
    } catch (TimeoutException e) {
      close();
      throw new IllegalStateException("Timed out while starting discovery server", e);
    }
  }

  public void close() {
    logger.info("Closing discovery server...");
    shutdown = true;
    if (channel != null) {
      try {
        channel.close().awaitUninterruptibly(SERVER_CLOSE_WAIT_SECONDS, TimeUnit.SECONDS);
      } catch (Exception e) {
        logger.error("Closing discovery server failed", e);
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
        if (shutdown) {
          channel.close().sync();
          initialBind.completeExceptionally(
              new IllegalStateException("Discovery server startup was cancelled"));
          break;
        }

        logger.info("Discovery server started, bind port {}", port);
        initialBind.complete(null);

        channel.closeFuture().sync();
        if (shutdown) {
          logger.info("Shutdown discovery server");
          break;
        }
        logger.warn("Restart discovery server after 5 sec pause...");
        Thread.sleep(TimeUnit.SECONDS.toMillis(SERVER_RESTART_WAIT_SECONDS));
      }
    } catch (InterruptedException e) {
      logger.warn("Discover server interrupted");
      Thread.currentThread().interrupt();
      initialBind.completeExceptionally(e);
    } catch (Exception e) {
      initialBind.completeExceptionally(e);
      logger.error("Start discovery server with port {} failed", port, e);
    } finally {
      if (!initialBind.isDone()) {
        initialBind.completeExceptionally(shutdown
            ? new IllegalStateException("Discovery server startup was cancelled")
            : new IllegalStateException("Discovery server stopped before initial bind"));
      }
      group.shutdownGracefully().sync();
    }
  }
}
