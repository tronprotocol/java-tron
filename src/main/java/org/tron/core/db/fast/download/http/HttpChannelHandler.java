package org.tron.core.db.fast.download.http;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpChunkedInput;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.stream.ChunkedFile;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.SystemPropertyUtil;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Pattern;
import javax.activation.MimetypesFileTypeMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class HttpChannelHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

  public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
  public static final String HTTP_DATE_GMT_TIMEZONE = "GMT";
  public static final int HTTP_CACHE_SECONDS = 60;

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
    // 监测解码情况
    if (!request.getDecoderResult().isSuccess()) {
      sendError(ctx, BAD_REQUEST);
      return;
    }
    final String uri = request.getUri();
    final String path = sanitizeUri(uri);
    System.out.println("get file：" + path);
    if (path == null) {
      sendError(ctx, FORBIDDEN);
      return;
    }
    //读取要下载的文件
    File file = new File(path);
    if (file.isHidden() || !file.exists()) {
      sendError(ctx, NOT_FOUND);
      return;
    }
//    if (!file.isFile()) {
//      sendError(ctx, FORBIDDEN);
//      return;
//    }
    RandomAccessFile raf;
    try {
      raf = new RandomAccessFile(path, "r");
    } catch (FileNotFoundException ignore) {
      sendError(ctx, NOT_FOUND);
      return;
    }
    long fileLength = raf.length();
    HttpResponse response = new DefaultHttpResponse(HTTP_1_1, HttpResponseStatus.OK);
    HttpHeaders.setContentLength(response, fileLength);
    setContentTypeHeader(response, file);
    //setDateAndCacheHeaders(response, file);
    if (HttpHeaders.isKeepAlive(request)) {
      response.headers().set("CONNECTION", HttpHeaders.Values.KEEP_ALIVE);
    }

    logger.info("begin send header data");
    // Write the initial line and the header.
    ctx.write(response);
    logger.info("begin send content data");
    // Write the content.
    //ChannelFuture sendFileFuture =
    ctx.write(new HttpChunkedInput(new ChunkedFile(raf, 0, fileLength, 8192)),
        ctx.newProgressivePromise());
//        sendFuture用于监视发送数据的状态
//        sendFileFuture.addListener(new ChannelProgressiveFutureListener() {
//            @Override
//            public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) {
//                if (total < 0) { // total unknown
//                    System.err.println(future.channel() + " Transfer progress: " + progress);
//                } else {
//                    System.err.println(future.channel() + " Transfer progress: " + progress + " / " + total);
//                }
//            }
//
//            @Override
//            public void operationComplete(ChannelProgressiveFuture future) {
//                System.err.println(future.channel() + " Transfer complete.");
//            }
//        });

    // Write the end marker
    ChannelFuture lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);

    // Decide whether to close the connection or not.
    if (!HttpHeaders.isKeepAlive(request)) {
      // Close the connection when the whole content is written out.
      lastContentFuture.addListener(ChannelFutureListener.CLOSE);
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    cause.printStackTrace();
    if (ctx.channel().isActive()) {
      sendError(ctx, INTERNAL_SERVER_ERROR);
    }
    ctx.close();
  }

  private static final Pattern INSECURE_URI = Pattern.compile(".*[<>&\"].*");

  private static String sanitizeUri(String uri) {
    // Decode the path.
    try {
      uri = URLDecoder.decode(uri, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new Error(e);
    }

    if (!uri.startsWith("/")) {
      return null;
    }

    // Convert file separators.
    uri = uri.replace('/', File.separatorChar);

    // Simplistic dumb security check.
    // You will have to do something serious in the production environment.
    if (uri.contains(File.separator + '.') || uri.contains('.' + File.separator) || uri
        .startsWith(".") || uri.endsWith(".")
        || INSECURE_URI.matcher(uri).matches()) {
      return null;
    }

    // Convert to absolute path.
    if (StringUtils.startsWith(uri, File.separator)) {
      return SystemPropertyUtil.get("user.dir") + uri;
    }
    return SystemPropertyUtil.get("user.dir") + File.separator + uri;
  }


  private static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
    FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status, Unpooled
        .copiedBuffer("Failure: " + status + "\r\n", CharsetUtil.UTF_8));
    response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");

    // Close the connection as soon as the error message is sent.
    ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
  }

  /**
   * Sets the content type header for the HTTP Response
   *
   * @param response HTTP response
   * @param file file to extract content type
   */
  private static void setContentTypeHeader(HttpResponse response, File file) {
    MimetypesFileTypeMap m = new MimetypesFileTypeMap();
    String contentType = m.getContentType(file.getPath());
    if (!contentType.equals("application/octet-stream")) {
      contentType += "; charset=utf-8";
    }
    response.headers().set(CONTENT_TYPE, contentType);
  }

}
