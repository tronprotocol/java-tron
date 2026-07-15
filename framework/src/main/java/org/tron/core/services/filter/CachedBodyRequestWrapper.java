package org.tron.core.services.filter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * Wraps a request to replay a pre-read body from a byte array,
 * allowing the body to be read more than once.
 *
 * <p><b>Scope:</b> designed for synchronous, raw-body POST endpoints
 * (e.g. JSON-RPC). It is NOT compatible with:
 * <ul>
 *   <li>{@code application/x-www-form-urlencoded} — cached body cannot back
 *       {@code getParameter*}.</li>
 *   <li>multipart — {@code getPart()/getParts()} read from the original
 *       (already-consumed) stream.</li>
 *   <li>async non-blocking I/O — see {@code setReadListener}.</li>
 *   <li>request dispatch / forward chains.</li>
 * </ul>
 *
 * <p>Multiple calls to {@code getInputStream()} (or {@code getReader()})
 * are allowed and each returns a fresh stream over the same cached body —
 * a deliberate extension of the standard servlet contract.
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
        throw new UnsupportedOperationException(
            "async I/O is not supported on cached body");
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
    Charset charset;
    try {
      charset = encoding != null ? Charset.forName(encoding) : StandardCharsets.UTF_8;
    } catch (IllegalCharsetNameException | UnsupportedCharsetException ex) {
      charset = StandardCharsets.UTF_8;
    }
    return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(body), charset));
  }
}
