package org.tron.core.net;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultMessageSizeEstimator;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.rules.TemporaryFolder;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.PublicMethod;
import org.tron.common.utils.ReflectUtils;
import org.tron.core.Constant;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.net.peer.PeerConnection;

@Slf4j
public class BaseNet {

  @ClassRule
  public static final TemporaryFolder temporaryFolder = new TemporaryFolder();
  private static String dbDirectory = "net-database";
  private static String indexDirectory = "net-index";
  private static int port = 10000;

  protected static TronApplicationContext context;

  private static Application appT;
  private static TronNetDelegate tronNetDelegate;

  private static ExecutorService executorService = Executors.newFixedThreadPool(1);

  public static Channel connect(ByteToMessageDecoder decoder) throws InterruptedException {
    NioEventLoopGroup group = new NioEventLoopGroup(1);
    Bootstrap b = new Bootstrap();
    b.group(group).channel(NioSocketChannel.class)
        .handler(new ChannelInitializer<Channel>() {
          @Override
          protected void initChannel(Channel ch) throws Exception {
            ch.config().setRecvByteBufAllocator(new FixedRecvByteBufAllocator(256 * 1024));
            ch.config().setOption(ChannelOption.SO_RCVBUF, 256 * 1024);
            ch.config().setOption(ChannelOption.SO_BACKLOG, 1024);
            ch.pipeline()
                .addLast("readTimeoutHandler", new ReadTimeoutHandler(600, TimeUnit.SECONDS))
                .addLast("writeTimeoutHandler", new WriteTimeoutHandler(600, TimeUnit.SECONDS));
            ch.pipeline().addLast("protoPender", new ProtobufVarint32LengthFieldPrepender());
            ch.pipeline().addLast("lengthDecode", new ProtobufVarint32FrameDecoder());
            ch.pipeline().addLast("handshakeHandler", decoder);
            ch.closeFuture();
          }
        }).option(ChannelOption.SO_KEEPALIVE, true)
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 60000)
        .option(ChannelOption.MESSAGE_SIZE_ESTIMATOR, DefaultMessageSizeEstimator.DEFAULT);
    return b.connect(Constant.LOCAL_HOST, port).sync().channel();
  }

  @BeforeClass
  public static void init() throws Exception {
    executorService.execute(() -> {
      logger.info("Full node running.");
      try {
        Args.setParam(
            new String[]{
                "--output-directory", temporaryFolder.newFolder().toString(),
                "--storage-db-directory", dbDirectory,
                "--storage-index-directory", indexDirectory
            },
            "config.conf"
        );
      } catch (IOException e) {
        Assert.fail("create temp db directory failed");
      }
      CommonParameter parameter = Args.getInstance();
      parameter.setNodeListenPort(port);
      parameter.getSeedNode().getAddressList().clear();
      parameter.setNodeExternalIp(Constant.LOCAL_HOST);
      parameter.setRpcEnable(true);
      parameter.setRpcPort(PublicMethod.chooseRandomPort());
      parameter.setRpcSolidityEnable(false);
      parameter.setRpcPBFTEnable(false);
      parameter.setFullNodeHttpEnable(false);
      parameter.setSolidityNodeHttpEnable(false);
      parameter.setPBFTHttpEnable(false);
      context = new TronApplicationContext(DefaultConfig.class);
      appT = ApplicationFactory.create(context);
      appT.startup();
      try {
        Thread.sleep(2000);
      } catch (InterruptedException e) {
        //ignore
      }
      tronNetDelegate = context.getBean(TronNetDelegate.class);
      appT.blockUntilShutdown();
    });
    int tryTimes = 0;
    do {
      Thread.sleep(3000); //coverage consumerInvToSpread,consumerInvToFetch in AdvService.init
    } while (++tryTimes < 100 && tronNetDelegate == null);
  }

  @AfterClass
  public static void destroy() {
    if (Objects.nonNull(tronNetDelegate)) {
      Collection<PeerConnection> peerConnections = ReflectUtils
          .invokeMethod(tronNetDelegate, "getActivePeer");
      for (PeerConnection peer : peerConnections) {
        peer.getChannel().close();
      }
    }
    Args.clearParam();
    context.destroy();
  }
}
