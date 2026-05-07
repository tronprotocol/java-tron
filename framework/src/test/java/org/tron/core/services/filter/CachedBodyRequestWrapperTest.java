package org.tron.core.services.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

public class CachedBodyRequestWrapperTest {

  private static final byte[] BODY = "hello world".getBytes(StandardCharsets.UTF_8);

  private static byte[] readFully(javax.servlet.ServletInputStream in) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] buf = new byte[128];
    int n;
    while ((n = in.read(buf)) != -1) {
      out.write(buf, 0, n);
    }
    return out.toByteArray();
  }

  // --- getInputStream ---

  @Test
  public void getInputStream_returnsBodyContent() throws IOException {
    CachedBodyRequestWrapper w = new CachedBodyRequestWrapper(new MockHttpServletRequest(), BODY);
    byte[] read = readFully(w.getInputStream());
    assertEquals(new String(BODY, StandardCharsets.UTF_8),
        new String(read, StandardCharsets.UTF_8));
  }

  @Test
  public void getInputStream_calledTwice_bothSucceed() throws IOException {
    CachedBodyRequestWrapper w = new CachedBodyRequestWrapper(new MockHttpServletRequest(), BODY);
    w.getInputStream();
    // second call of the same accessor is allowed by the servlet spec
    w.getInputStream();
  }

  // --- getReader ---

  @Test
  public void getReader_returnsBodyContent() throws IOException {
    CachedBodyRequestWrapper w = new CachedBodyRequestWrapper(new MockHttpServletRequest(), BODY);
    String line = w.getReader().readLine();
    assertEquals("hello world", line);
  }

  @Test
  public void getReader_calledTwice_bothSucceed() throws IOException {
    CachedBodyRequestWrapper w = new CachedBodyRequestWrapper(new MockHttpServletRequest(), BODY);
    w.getReader();
    w.getReader();
  }

  // --- mutual exclusion ---

  @Test(expected = IllegalStateException.class)
  public void getReader_afterGetInputStream_throws() throws IOException {
    CachedBodyRequestWrapper w = new CachedBodyRequestWrapper(new MockHttpServletRequest(), BODY);
    w.getInputStream();
    w.getReader();
  }

  @Test(expected = IllegalStateException.class)
  public void getInputStream_afterGetReader_throws() throws IOException {
    CachedBodyRequestWrapper w = new CachedBodyRequestWrapper(new MockHttpServletRequest(), BODY);
    w.getReader();
    w.getInputStream();
  }

  // --- stream contract ---

  @Test
  public void getInputStream_isFinished_afterFullRead() throws IOException {
    CachedBodyRequestWrapper w = new CachedBodyRequestWrapper(new MockHttpServletRequest(), BODY);
    javax.servlet.ServletInputStream in = w.getInputStream();
    while (in.read() != -1) {
      // drain
    }
    assertTrue(in.isFinished());
  }

  @Test
  public void getInputStream_isReady_returnsTrue() {
    CachedBodyRequestWrapper w = new CachedBodyRequestWrapper(new MockHttpServletRequest(), BODY);
    assertTrue(w.getInputStream().isReady());
  }

  @Test
  public void getInputStream_emptyBody_isFinishedImmediately() {
    CachedBodyRequestWrapper w = new CachedBodyRequestWrapper(new MockHttpServletRequest(),
        new byte[0]);
    assertTrue(w.getInputStream().isFinished());
  }

  @Test
  public void getReader_usesRequestCharacterEncoding() throws IOException {
    MockHttpServletRequest req = new MockHttpServletRequest();
    req.setCharacterEncoding("UTF-8");
    byte[] utf8Body = "tron".getBytes(StandardCharsets.UTF_8);
    CachedBodyRequestWrapper w = new CachedBodyRequestWrapper(req, utf8Body);
    assertEquals("tron", w.getReader().readLine());
  }
}
