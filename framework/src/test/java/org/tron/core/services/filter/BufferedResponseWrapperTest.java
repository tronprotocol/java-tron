package org.tron.core.services.filter;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
}
