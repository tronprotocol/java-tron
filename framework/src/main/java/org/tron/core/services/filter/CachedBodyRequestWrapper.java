package org.tron.core.services.filter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * Wraps a request and replays a pre-read body from a byte array.
 */
public class CachedBodyRequestWrapper extends HttpServletRequestWrapper {

  private enum BodyAccessor { NONE, STREAM, READER }

  private final byte[] body;
  private BodyAccessor accessor = BodyAccessor.NONE;

  public CachedBodyRequestWrapper(HttpServletRequest request, byte[] body) {
    super(request);
    this.body = body;
  }

  @Override
  public ServletInputStream getInputStream() {
    if (accessor == BodyAccessor.READER) {
      throw new IllegalStateException("getReader() has already been called on this request");
    }
    accessor = BodyAccessor.STREAM;
    final ByteArrayInputStream bais = new ByteArrayInputStream(body);
    return new ServletInputStream() {
      @Override
      public int read() {
        return bais.read();
      }

      @Override
      public int read(byte[] b, int off, int len) {
        return bais.read(b, off, len);
      }

      @Override
      public boolean isFinished() {
        return bais.available() == 0;
      }

      @Override
      public boolean isReady() {
        return true;
      }

      @Override
      public void setReadListener(ReadListener readListener) {
      }
    };
  }

  @Override
  public BufferedReader getReader() {
    if (accessor == BodyAccessor.STREAM) {
      throw new IllegalStateException("getInputStream() has already been called on this request");
    }
    accessor = BodyAccessor.READER;
    String encoding = getCharacterEncoding();
    Charset charset = encoding != null ? Charset.forName(encoding) : StandardCharsets.UTF_8;
    return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(body), charset));
  }
}
