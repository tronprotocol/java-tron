package org.tron.core.services.filter;

import java.io.ByteArrayOutputStream;
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
 */
public class BufferedResponseWrapper extends HttpServletResponseWrapper {

  private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
  private final int maxBytes;
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
    this.maxBytes = maxBytes;
  }

  private void checkLimit(int incoming) {
    if (maxBytes > 0 && buffer.size() + incoming > maxBytes) {
      throw new JsonRpcResponseTooLargeException(
          "Response byte size exceeds the limit of " + maxBytes);
    }
  }

  @Override
  public ServletOutputStream getOutputStream() {
    return outputStream;
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

  public byte[] toByteArray() {
    return buffer.toByteArray();
  }

}
