package org.tron.common.backup.socket;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.backup.BackupManager;
import org.tron.common.es.ExecutorServiceManager;
import org.tron.common.parameter.CommonParameter;
import org.tron.p2p.stats.TrafficStats;

@Slf4j(topic = "backup")
@Component
public class BackupServer implements AutoCloseable {

  private CommonParameter commonParameter = CommonParameter.getInstance();

  private int port = commonParameter.getBackupPort();

  private BackupManager backupManager;

  private Channel channel;

  private volatile boolean shutdown = false;

  private final String name = "BackupServer";
  private ExecutorService executor;

  @Autowired
  public BackupServer(final BackupManager backupManager) {
    this.backupManager = backupManager;
  }

  public void initServer() {
    if (port > 0 && commonParameter.getBackupMembers().size() > 0) {
      executor = ExecutorServiceManager.newSingleThreadExecutor(name);
      executor.submit(() -> {
        try {
          start();
        } catch (Exception e) {
          logger.error("Start backup server failed, {}", e);
        }
      });
    }
  }

  private void start() throws Exception {
    NioEventLoopGroup group = new NioEventLoopGroup(1);
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
                ch.pipeline().addLast(new PacketDecoder());
                MessageHandler messageHandler = new MessageHandler(ch, backupManager);
                backupManager.setMessageHandler(messageHandler);
                ch.pipeline().addLast(messageHandler);
              }
            });

        channel = b.bind(port).sync().channel();

        logger.info("Backup server started, bind port {}", port);

        channel.closeFuture().sync();
        if (shutdown) {
          logger.info("Shutdown backup BackupServer");
          break;
        }
        logger.warn("Restart backup server ...");
      }
    } catch (Exception e) {
      logger.error("Start backup server with port {} failed.", port, e);
    } finally {
      group.shutdownGracefully().sync();
    }
  }

  @Override
  public void close() {
    logger.info("Closing backup server...");
    shutdown = true;
    backupManager.stop();
    if (channel != null) {
      try {
        channel.close().await(10, TimeUnit.SECONDS);
      } catch (Exception e) {
        logger.warn("Closing backup server failed.", e);
      }
    }
    ExecutorServiceManager.shutdownAndAwaitTermination(executor, name);
    logger.info("Backup server closed.");
  }
}
