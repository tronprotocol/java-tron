package org.tron.core.services.filter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import org.tron.core.exception.jsonrpc.JsonRpcResponseTooLargeException;

/**
 * Buffers the response body without writing to the underlying response,
 * so the caller can inspect the size before committing.
 *
 * <p>If {@code maxBytes > 0}, writes that would push the buffer past {@code maxBytes} throw
 * {@link JsonRpcResponseTooLargeException} immediately, bounding memory usage to at most
 * {@code maxBytes} rather than the full response size.
 *
 * <p>Header-mutating methods ({@code setStatus}, {@code setContentType}) are buffered here and
 * only forwarded to the real response via {@link #commitToResponse()}, preventing a timed-out
 * handler thread from racing with the timeout error writer.
 */
public class BufferedResponseWrapper extends HttpServletResponseWrapper {

  private final HttpServletResponse actual;
  private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
  private final int maxBytes;
  private int status = HttpServletResponse.SC_OK;
  private String contentType;
  private final ServletOutputStream outputStream = new ServletOutputStream() {
    @Override
    public void write(int b) {
      checkLimit(1);
      buffer.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) {
      checkLimit(len);
      buffer.write(b, off, len);
    }

    @Override
    public boolean isReady() {
      return true;
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {
    }
  };

  /**
   * @param response the wrapped response
   * @param maxBytes max allowed response bytes; {@code 0} means no limit
   */
  public BufferedResponseWrapper(HttpServletResponse response, int maxBytes) {
    super(response);
    this.actual = response;
    this.maxBytes = maxBytes;
  }

  private void checkLimit(int incoming) {
    if (maxBytes > 0 && buffer.size() + incoming > maxBytes) {
      throw new JsonRpcResponseTooLargeException(
          "Response byte size exceeds the limit of " + maxBytes);
    }
  }

  @Override
  public void setStatus(int sc) {
    this.status = sc;
  }

  @Override
  public void setContentType(String type) {
    this.contentType = type;
  }

  @Override
  public ServletOutputStream getOutputStream() {
    return outputStream;
  }

  @Override
  public PrintWriter getWriter() {
    return new PrintWriter(outputStream, true);
  }

  /**
   * Suppress forwarding Content-Length to the real response; caller sets it after size check.
   */
  @Override
  public void setContentLength(int len) {
  }

  @Override
  public void setContentLengthLong(long len) {
  }

  public void commitToResponse() throws IOException {
    if (contentType != null) {
      actual.setContentType(contentType);
    }
    actual.setStatus(status);
    byte[] bytes = buffer.toByteArray();
    actual.setContentLength(bytes.length);
    actual.getOutputStream().write(bytes);
    actual.getOutputStream().flush();
  }
}
