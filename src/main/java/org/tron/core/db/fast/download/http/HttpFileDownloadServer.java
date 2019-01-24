package org.tron.core.db.fast.download.http;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class HttpFileDownloadServer implements Runnable {

  private int port;

  public HttpFileDownloadServer(int port) {
    super();
    this.port = port;
  }

  @Override
  public void run() {
    EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    EventLoopGroup workerGroup = new NioEventLoopGroup();
    ServerBootstrap serverBootstrap = new ServerBootstrap();
    serverBootstrap.group(bossGroup, workerGroup);
    serverBootstrap.channel(NioServerSocketChannel.class);
    //serverBootstrap.handler(new LoggingHandler(LogLevel.INFO));
    serverBootstrap.childHandler(new HttpChannelInitlalizer());
    try {
      ChannelFuture f = serverBootstrap.bind(port).sync();
      f.channel().closeFuture().sync();
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      bossGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
    }
  }

  public void start() {
    EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    EventLoopGroup workerGroup = new NioEventLoopGroup();
    ServerBootstrap serverBootstrap = new ServerBootstrap();
    serverBootstrap.group(bossGroup, workerGroup);
    serverBootstrap.channel(NioServerSocketChannel.class);
    //serverBootstrap.handler(new LoggingHandler(LogLevel.INFO));
    serverBootstrap.childHandler(new HttpChannelInitlalizer());
    try {
      ChannelFuture f = serverBootstrap.bind(port).sync();
      f.channel().closeFuture().sync();
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      bossGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
    }
  }

  public static void main(String[] args) {
    HttpFileDownloadServer httpFileDownloadServer = new HttpFileDownloadServer(8011);
    httpFileDownloadServer.start();
  }
}