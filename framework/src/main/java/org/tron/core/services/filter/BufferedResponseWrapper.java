package org.tron.core.services.filter;

import java.io.ByteArrayOutputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * Buffers the response body without writing to the underlying response,
 * so the caller can inspect the size before committing.
 */
public class BufferedResponseWrapper extends HttpServletResponseWrapper {

  private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
  private final ServletOutputStream outputStream = new ServletOutputStream() {
    @Override
    public void write(int b) {
      buffer.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) {
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

  public BufferedResponseWrapper(HttpServletResponse response) {
    super(response);
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
