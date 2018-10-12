package org.tron.core.net.node.override;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultMessageSizeEstimator;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.tron.core.config.args.Args;
import org.tron.core.net.node.NodeImpl;

@Component
public class PeerClientTest {

  private static final Logger logger = LoggerFactory.getLogger("PeerClient");

  @Autowired
  private ApplicationContext ctx;

  @Autowired
  @Lazy
  private NodeImpl node;

  private EventLoopGroup workerGroup;

  private TronChannelInitializerTest tronChannelInitializer;

  public PeerClientTest() {
    workerGroup = new NioEventLoopGroup(0, new ThreadFactory() {
      AtomicInteger cnt = new AtomicInteger(0);

      @Override
      public Thread newThread(Runnable r) {
        return new Thread(r, "TronJClientWorker-test-" + cnt.getAndIncrement());
      }
    });
  }

  public void prepare(String remoteId) {
    tronChannelInitializer = ctx.getBean(TronChannelInitializerTest.class, remoteId);
    tronChannelInitializer.setNodeImpl(node);
  }

  public void connect(String host, int port, String remoteId) {
    try {
      ChannelFuture f = connectAsync(host, port, remoteId, false);
      f.sync().channel().closeFuture().sync();
    } catch (Exception e) {
      logger
          .info("PeerClient: Can't connect to " + host + ":" + port + " (" + e.getMessage() + ")");
    }
  }

  public ChannelFuture connectAsync(String host, int port, String remoteId, boolean discoveryMode) {

    logger.info("connect peer {} {} {}", host, port, remoteId);

    Bootstrap b = new Bootstrap();
    b.group(workerGroup);
    b.channel(NioSocketChannel.class);

    b.option(ChannelOption.SO_KEEPALIVE, true);
    b.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR, DefaultMessageSizeEstimator.DEFAULT);
    b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Args.getInstance().getNodeConnectionTimeout());
    b.remoteAddress(host, port);

    b.handler(tronChannelInitializer);

    // Start the client.
    return b.connect();
  }

  public void close() {
    tronChannelInitializer.close();
    workerGroup.shutdownGracefully();
    workerGroup.terminationFuture().syncUninterruptibly();
  }
}
