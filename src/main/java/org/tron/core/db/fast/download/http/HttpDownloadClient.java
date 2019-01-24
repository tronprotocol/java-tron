package org.tron.core.db.fast.download.http;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.stream.ChunkedWriteHandler;
import java.net.URI;

public class HttpDownloadClient {

  /**
   * 下载http资源 向服务器下载直接填写要下载的文件的相对路径 （↑↑↑建议只使用字母和数字对特殊字符对字符进行部分过滤可能导致异常↑↑↑） 向互联网下载输入完整路径
   *
   * @param host 目的主机ip或域名
   * @param port 目标主机端口
   * @param url 文件路径
   * @param local 本地存储路径
   */
  public void connect(String host, int port, String url, final String local) throws Exception {
    EventLoopGroup workerGroup = new NioEventLoopGroup();
    try {
      Bootstrap b = new Bootstrap();
      b.group(workerGroup);
      b.channel(NioSocketChannel.class);
      b.option(ChannelOption.SO_KEEPALIVE, true);
      b.handler(new ChildChannelHandler(local));

      // Start the client.
      ChannelFuture f = b.connect(host, port).sync();

      URI uri = new URI(url);
      DefaultFullHttpRequest request = new DefaultFullHttpRequest(
          HttpVersion.HTTP_1_1, HttpMethod.GET, uri.toASCIIString());

      // 构建http请求
      request.headers().set(HttpHeaders.Names.HOST, host);
      request.headers().set(HttpHeaders.Names.CONNECTION,
          HttpHeaders.Values.KEEP_ALIVE);
      request.headers().set(HttpHeaders.Names.CONTENT_LENGTH,
          request.content().readableBytes());
      // 发送http请求
      f.channel().write(request);
      f.channel().flush();
      f.channel().closeFuture().sync();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      workerGroup.shutdownGracefully();
    }

  }

  private class ChildChannelHandler extends ChannelInitializer<SocketChannel> {

    String local;

    public ChildChannelHandler(String local) {
      this.local = local;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
      // 客户端接收到的是httpResponse响应，所以要使用HttpResponseDecoder进行解码
      ch.pipeline().addLast(new HttpResponseDecoder());
      // 客户端发送的是httprequest，所以要使用HttpRequestEncoder进行编码
      ch.pipeline().addLast(new HttpRequestEncoder());
      ch.pipeline().addLast(new ChunkedWriteHandler());
      ch.pipeline().addLast(new HttpDownloadHandler(local));
    }

  }

  public static void main(String[] args) throws Exception {
    HttpDownloadClient client = new HttpDownloadClient();
    client.connect("127.0.0.1", 8011, "/output-directory",
        "/Users/liangzhiyan/code/output-directory");
    //client.connect("zlysix.gree.com", 80, "http://zlysix.gree.com/HelloWeb/download/20m.apk", "20m.apk");
    while (!HttpDownloadHandler.finish) {

    }
  }
}

