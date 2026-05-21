package org.tron.core.services.filter;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class BufferedResponseWrapperTest {

  private MockHttpServletResponse mockResp;

  @Before
  public void setUp() {
    mockResp = new MockHttpServletResponse();
  }

  // --- isOverflow: false cases ---

  @Test
  public void noLimit_neverOverflows() throws IOException {
    BufferedResponseWrapper w = new BufferedResponseWrapper(mockResp, 0);
    w.getOutputStream().write(new byte[1024 * 1024]);
    assertFalse(w.isOverflow());
  }

  @Test
  public void withinLimit_notOverflow() throws IOException {
    BufferedResponseWrapper w = new BufferedResponseWrapper(mockResp, 10);
    w.getOutputStream().write(new byte[10]);
    assertFalse(w.isOverflow());
  }

  @Test
  public void exactlyAtLimit_notOverflow() throws IOException {
    BufferedResponseWrapper w = new BufferedResponseWrapper(mockResp, 5);
    w.getOutputStream().write(new byte[]{1, 2, 3, 4, 5});
    assertFalse(w.isOverflow());
  }

  // --- isOverflow: true via write ---

  @Test
  public void oneBytePastLimit_overflow() throws IOException {
    BufferedResponseWrapper w = new BufferedResponseWrapper(mockResp, 5);
    w.getOutputStream().write(new byte[]{1, 2, 3, 4, 5, 6});
    assertTrue(w.isOverflow());
  }

  @Test
  public void singleByteWrite_triggerOverflow() throws IOException {
    BufferedResponseWrapper w = new BufferedResponseWrapper(mockResp, 3);
    w.getOutputStream().write(1);
    w.getOutputStream().write(2);
    w.getOutputStream().write(3);
    assertFalse(w.isOverflow());
    w.getOutputStream().write(4);
    assertTrue(w.isOverflow());
  }

  @Test
  public void overflow_bufferIsReleasedOnOverflow() throws IOException {
    BufferedResponseWrapper w = new BufferedResponseWrapper(mockResp, 4);
    w.getOutputStream().write(new byte[]{1, 2, 3, 4, 5});
    assertTrue(w.isOverflow());
    // After overflow, further writes are silently discarded — no exception
    w.getOutputStream().write(new byte[100]);
    assertTrue(w.isOverflow());
  }

  // --- isOverflow: true via setContentLength ---

  @Test
  public void setContentLength_exceedsLimit_overflow() {
    BufferedResponseWrapper w = new BufferedResponseWrapper(mockResp, 100);
    w.setContentLength(101);
    assertTrue(w.isOverflow());
  }

  @Test
  public void setContentLength_exactlyAtLimit_notOverflow() {
    BufferedResponseWrapper w = new BufferedResponseWrapper(mockResp, 100);
    w.setContentLength(100);
    assertFalse(w.isOverflow());
  }

  @Test
  public void setContentLengthLong_exceedsLimit_overflow() {
    BufferedResponseWrapper w = new BufferedResponseWrapper(mockResp, 100);
    w.setContentLengthLong(101L);
    assertTrue(w.isOverflow());
  }

  @Test
  public void setContentLength_noLimit_neverOverflows() {
    BufferedResponseWrapper w = new BufferedResponseWrapper(mockResp, 0);
    w.setContentLength(Integer.MAX_VALUE);
    assertFalse(w.isOverflow());
  }

  // --- setContentLength early detection: writes after early overflow are discarded ---

  @Test
  public void earlyOverflow_subsequentWritesDiscarded() throws IOException {
    BufferedResponseWrapper w = new BufferedResponseWrapper(mockResp, 10);
    w.setContentLength(20);
    assertTrue(w.isOverflow());
    w.getOutputStream().write(new byte[5]);
    // Nothing committed to actual response
    assertFalse(mockResp.isCommitted());
  }

  // --- commitToResponse ---

  @Test
  public void commitToResponse_writesBodyAndHeaders() throws IOException {
    BufferedResponseWrapper w = new BufferedResponseWrapper(mockResp, 0);
    byte[] data = "hello".getBytes(StandardCharsets.UTF_8);
    w.setStatus(200);
    w.setContentType("application/json");
    w.getOutputStream().write(data);
    w.commitToResponse();

    assertEquals(200, mockResp.getStatus());
    assertEquals("application/json", mockResp.getContentType());
    assertArrayEquals(data, mockResp.getContentAsByteArray());
  }

  @Test
  public void commitToResponse_setsCorrectContentLength() throws IOException {
    BufferedResponseWrapper w = new BufferedResponseWrapper(mockResp, 0);
    byte[] data = new byte[]{10, 20, 30};
    w.getOutputStream().write(data);
    w.commitToResponse();

    assertEquals(3, mockResp.getContentLength());
  }

  @Test
  public void commitToResponse_emptyBuffer_writesZeroBytes() throws IOException {
    BufferedResponseWrapper w = new BufferedResponseWrapper(mockResp, 100);
    w.setStatus(200);
    w.commitToResponse();

    assertEquals(0, mockResp.getContentLength());
    assertEquals(0, mockResp.getContentAsByteArray().length);
  }

  // --- header buffering: nothing reaches actual response until commit ---

  @Test
  public void statusNotForwardedBeforeCommit() throws IOException {
    BufferedResponseWrapper w = new BufferedResponseWrapper(mockResp, 0);
    w.setStatus(201);
    // MockHttpServletResponse defaults to 200
    assertEquals(200, mockResp.getStatus());
    w.commitToResponse();
    assertEquals(201, mockResp.getStatus());
  }

  // --- getStatus() ---

  @Test
  public void getStatus_returnsBufferedValue() {
    BufferedResponseWrapper w = new BufferedResponseWrapper(mockResp, 0);
    w.setStatus(404);
    assertEquals(404, w.getStatus());
    // actual response must still be untouched
    assertEquals(200, mockResp.getStatus());
  }

  @Test
  public void getStatus_defaultIs200() {
    BufferedResponseWrapper w = new BufferedResponseWrapper(mockResp, 0);
    assertEquals(200, w.getStatus());
  }

  // --- setHeader / addHeader for Content-Length ---

  @Test
  public void setHeader_contentLength_exceedsLimit_overflow() {
    BufferedResponseWrapper w = new BufferedResponseWrapper(mockResp, 100);
    w.setHeader("Content-Length", "101");
    assertTrue(w.isOverflow());
    // Content-Length must NOT have been forwarded to the actual response
    assertNull(mockResp.getHeader("Content-Length"));
  }

  @Test
  public void setHeader_contentLength_withinLimit_noOverflow() {
    BufferedResponseWrapper w = new BufferedResponseWrapper(mockResp, 100);
    w.setHeader("Content-Length", "100");
    assertFalse(w.isOverflow());
  }

  @Test
  public void setHeader_contentLength_caseInsensitive_overflow() {
    BufferedResponseWrapper w = new BufferedResponseWrapper(mockResp, 50);
    w.setHeader("content-length", "51");
    assertTrue(w.isOverflow());
  }

  @Test
  public void setHeader_contentLength_malformed_ignored() {
    BufferedResponseWrapper w = new BufferedResponseWrapper(mockResp, 100);
    w.setHeader("Content-Length", "not-a-number");
    assertFalse(w.isOverflow());
  }

  @Test
  public void setHeader_nonContentLength_passesThroughToActual() {
    BufferedResponseWrapper w = new BufferedResponseWrapper(mockResp, 0);
    w.setHeader("X-Custom-Header", "hello");
    assertEquals("hello", mockResp.getHeader("X-Custom-Header"));
  }

  @Test
  public void addHeader_contentLength_exceedsLimit_overflow() {
    BufferedResponseWrapper w = new BufferedResponseWrapper(mockResp, 100);
    w.addHeader("Content-Length", "200");
    assertTrue(w.isOverflow());
    assertNull(mockResp.getHeader("Content-Length"));
  }

  @Test
  public void addHeader_contentLength_malformed_ignored() {
    BufferedResponseWrapper w = new BufferedResponseWrapper(mockResp, 100);
    w.addHeader("Content-Length", "bad");
    assertFalse(w.isOverflow());
  }

  @Test
  public void addHeader_nonContentLength_passesThroughToActual() {
    BufferedResponseWrapper w = new BufferedResponseWrapper(mockResp, 0);
    w.addHeader("X-Trace-Id", "abc123");
    assertEquals("abc123", mockResp.getHeader("X-Trace-Id"));
  }

  // --- commitToResponse idempotency ---

  @Test(expected = IllegalStateException.class)
  public void commitToResponse_secondCall_throwsIllegalState() throws IOException {
    BufferedResponseWrapper w = new BufferedResponseWrapper(mockResp, 0);
    w.commitToResponse();
    w.commitToResponse();
  }

  // --- getWriter path ---

  @Test
  public void writeViaWriter_commitToResponse_flushesBody() throws IOException {
    BufferedResponseWrapper w = new BufferedResponseWrapper(mockResp, 0);
    w.getWriter().print("hello");
    w.getWriter().flush();
    w.commitToResponse();
    assertEquals("hello", mockResp.getContentAsString());
  }

  @Test
  public void writeViaWriter_noExplicitFlush_commitToResponse_flushesBody() throws IOException {
    // Regression: PrintWriter(autoFlush=true) does NOT flush on plain print(); bytes can sit
    // in the OutputStreamWriter encoder until commitToResponse() flushes the writer internally.
    BufferedResponseWrapper w = new BufferedResponseWrapper(mockResp, 0);
    w.getWriter().print("hello");
    w.commitToResponse();
    assertEquals("hello", mockResp.getContentAsString());
    assertEquals(5, mockResp.getContentLength());
  }

  @Test
  public void writeViaWriter_noExplicitFlush_flushTripsOverflow() throws IOException {
    // Regression: bytes buffered in the encoder may push the total past maxBytes when
    // commitToResponse() flushes — overflow must be detected and nothing written to actual.
    BufferedResponseWrapper w = new BufferedResponseWrapper(mockResp, 3);
    w.getWriter().print("hello"); // 5 bytes, not yet in ByteArrayOutputStream
    assertFalse("overflow must not trigger before flush", w.isOverflow());
    w.commitToResponse();
    assertTrue("flush inside commitToResponse must trip overflow", w.isOverflow());
    assertEquals(0, mockResp.getContentAsByteArray().length);
  }
}
