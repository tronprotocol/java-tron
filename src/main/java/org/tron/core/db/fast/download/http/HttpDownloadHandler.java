package org.tron.core.db.fast.download.http;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.internal.SystemPropertyUtil;
import java.io.File;
import java.io.FileOutputStream;

public class HttpDownloadHandler extends ChannelInboundHandlerAdapter {

  private boolean readingChunks = false; // 分块读取开关
  private FileOutputStream fOutputStream = null;// 文件输出流
  private File localfile = null;// 下载文件的本地对象
  private String local = null;// 待下载文件名
  private int succCode;// 状态码
  public static boolean finish = false;

  public HttpDownloadHandler(String local) {
    this.local = local;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg)
      throws Exception {
    if (msg instanceof HttpResponse) {// response头信息
      HttpResponse response = (HttpResponse) msg;
      succCode = response.getStatus().code();
      if (succCode == 200) {
        setDownLoadFile();// 设置下载文件
        readingChunks = true;
      }
      // System.out.println("CONTENT_TYPE:"
      // + response.headers().get(HttpHeaders.Names.CONTENT_TYPE));
    }
    if (msg instanceof HttpContent) {// response体信息
      HttpContent chunk = (HttpContent) msg;
      if (chunk instanceof LastHttpContent) {
        readingChunks = false;
      }

      ByteBuf buffer = chunk.content();
      byte[] dst = new byte[buffer.readableBytes()];
      if (succCode == 200) {
        while (buffer.isReadable()) {
          buffer.readBytes(dst);
          fOutputStream.write(dst);
          buffer.release();
        }
        if (null != fOutputStream) {
          fOutputStream.flush();
        }
      }

    }
    if (!readingChunks) {
      if (null != fOutputStream) {
        finish = true;
        System.out.println("Download done->" + localfile.getAbsolutePath());
        fOutputStream.flush();
        fOutputStream.close();
        localfile = null;
        fOutputStream = null;
      }
      ctx.channel().close();
    }
  }

  /**
   * 配置本地参数，准备下载
   */
  private void setDownLoadFile() throws Exception {
    if (null == fOutputStream) {
      local = SystemPropertyUtil.get("user.dir") + File.separator + local;
      //System.out.println(local);
      localfile = new File(local);
      if (!localfile.exists()) {
        localfile.createNewFile();
      }
      fOutputStream = new FileOutputStream(localfile);
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
      throws Exception {
    System.out.println("管道异常：" + cause.getMessage());
    cause.printStackTrace();
    ctx.channel().close();
  }
}
