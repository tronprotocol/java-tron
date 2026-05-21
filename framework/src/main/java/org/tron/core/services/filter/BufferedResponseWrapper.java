package org.tron.core.services.filter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import lombok.Getter;

/**
 * Buffers the response body without writing to the underlying response,
 * so the caller can replay it after the handler returns.
 *
 * <p>If {@code maxBytes > 0} and the response would exceed that limit, the
 * {@link #isOverflow()} flag is set instead of throwing. The caller should check this flag after
 * the handler returns and write its own error response when true.
 *
 * <p>Header-mutating methods ({@code setStatus}, {@code setContentType}) are buffered here and
 * only forwarded to the real response via {@link #commitToResponse()}.
 */
public class BufferedResponseWrapper extends HttpServletResponseWrapper {

  private final HttpServletResponse actual;
  private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
  private final int maxBytes;
  private int status = HttpServletResponse.SC_OK;
  private String contentType;
  private boolean committed = false;
  @Getter
  private volatile boolean overflow = false;

  private final ServletOutputStream outputStream = new ServletOutputStream() {
    @Override
    public void write(int b) {
      if (overflow) {
        return;
      }
      if (maxBytes > 0 && buffer.size() >= maxBytes) {
        markOverflow();
        return;
      }
      buffer.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) {
      if (overflow) {
        return;
      }
      if (maxBytes > 0 && buffer.size() + len > maxBytes) {
        markOverflow();
        return;
      }
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

  private final PrintWriter writer =
      new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8), true);

  /**
   * @param response the wrapped response
   * @param maxBytes max allowed response bytes; {@code 0} means no limit
   */
  public BufferedResponseWrapper(HttpServletResponse response, int maxBytes) {
    super(response);
    this.actual = response;
    this.maxBytes = maxBytes;
  }

  private void markOverflow() {
    overflow = true;
    buffer.reset();
  }

  /**
   * Early-detection path: if the framework reports the full content length before writing any
   * bytes, we can flag overflow without buffering anything.
   */
  @Override
  public void setContentLength(int len) {
    if (maxBytes > 0 && len > maxBytes) {
      markOverflow();
    }
  }

  @Override
  public void setContentLengthLong(long len) {
    if (maxBytes > 0 && len > maxBytes) {
      markOverflow();
    }
  }

  @Override
  public int getStatus() {
    return this.status;
  }

  @Override
  public void setStatus(int sc) {
    this.status = sc;
  }

  @Override
  public void setHeader(String name, String value) {
    if ("content-length".equalsIgnoreCase(name)) {
      try {
        setContentLengthLong(Long.parseLong(value));
      } catch (NumberFormatException ignored) {
        // malformed value, skip overflow check
      }
    } else {
      super.setHeader(name, value);
    }
  }

  @Override
  public void addHeader(String name, String value) {
    if ("content-length".equalsIgnoreCase(name)) {
      try {
        setContentLengthLong(Long.parseLong(value));
      } catch (NumberFormatException ignored) {
        // malformed value, skip overflow check
      }
    } else {
      super.addHeader(name, value);
    }
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
    return writer;
  }

  public void commitToResponse() throws IOException {
    if (committed) {
      throw new IllegalStateException("commitToResponse() already called");
    }
    committed = true;
    // Flush the PrintWriter's OutputStreamWriter encoder into our ByteArrayOutputStream.
    // PrintWriter(autoFlush=true) only auto-flushes on println/printf/format, not print/write,
    // so bytes can remain buffered in the encoder until an explicit flush.
    writer.flush();
    if (overflow) {
      return;
    }
    if (contentType != null) {
      actual.setContentType(contentType);
    }
    actual.setStatus(status);
    actual.setContentLength(buffer.size());
    buffer.writeTo(actual.getOutputStream());
    actual.getOutputStream().flush();
  }
}
