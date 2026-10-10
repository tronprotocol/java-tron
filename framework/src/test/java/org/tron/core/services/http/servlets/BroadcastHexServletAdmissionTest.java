package org.tron.core.services.http.servlets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.google.protobuf.ByteString;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.tron.api.GrpcAPI;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.core.admission.TransactionAdmissionException;
import org.tron.core.admission.TransactionAdmissionGuard;
import org.tron.protos.Protocol.Transaction;

public class BroadcastHexServletAdmissionTest {

  @Test
  public void overBudgetPayloadGetsTheFixedClientError() throws Exception {
    byte[] flood = new byte[2 * (TransactionAdmissionGuard.MAX_TX_OCCURRENCES + 1)];
    for (int i = 0; i < flood.length; i += 2) {
      flood[i] = 0x2a;
    }
    assertRejectedWithoutWallet(flood);
  }

  @Test
  public void malformedPayloadGetsTheFixedClientError() throws Exception {
    assertRejectedWithoutWallet(new byte[] {0x08, (byte) 0x80});   // truncated varint
  }

  @Test
  public void groupEncodingGetsTheFixedClientError() throws Exception {
    // Accepted by the generic protobuf parser, not by this admission path.
    assertRejectedWithoutWallet(new byte[] {0x0b, 0x0c});
  }

  @Test
  public void eachRejectionIsLoggedOnceAtDebugWithoutStack() throws Exception {
    BroadcastHexServlet servlet = new BroadcastHexServlet();
    Logger logger = (Logger) LoggerFactory.getLogger("API");
    Level previous = logger.getLevel();
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
    logger.setLevel(Level.DEBUG);
    try {
      for (int i = 0; i < 3; i++) {
        assertRejectedWithoutWallet(servlet, new byte[] {0x0b, 0x0c});
      }
    } finally {
      logger.detachAppender(appender);
      logger.setLevel(previous);
    }
    assertEquals(3, appender.list.size());
    for (ILoggingEvent event : appender.list) {
      assertEquals(Level.DEBUG, event.getLevel());
      assertNull(event.getThrowableProxy());
      assertEquals("broadcasthex rejected by admission check: MALFORMED",
          event.getFormattedMessage());
    }
  }

  @Test
  public void legitimatePayloadIsBroadcast() throws Exception {
    Wallet wallet = mock(Wallet.class);
    when(wallet.broadcastTransaction(any()))
        .thenReturn(GrpcAPI.Return.newBuilder().setResult(true).build());
    Transaction tx = Transaction.newBuilder()
        .setRawData(Transaction.raw.newBuilder().setExpiration(1))
        .addSignature(ByteString.copyFrom(new byte[65])).build();
    String body = post(new BroadcastHexServlet(), wallet,
        ByteArray.toHexString(tx.toByteArray()));
    assertTrue(body, body.contains("\"result\":true"));
    verify(wallet).broadcastTransaction(tx);
  }

  private static void assertRejectedWithoutWallet(byte[] payload) throws Exception {
    assertRejectedWithoutWallet(new BroadcastHexServlet(), payload);
  }

  private static void assertRejectedWithoutWallet(BroadcastHexServlet servlet, byte[] payload)
      throws Exception {
    Wallet wallet = mock(Wallet.class);
    String body = post(servlet, wallet, ByteArray.toHexString(payload));
    assertTrue(body, body.contains(TransactionAdmissionException.REJECTION_MESSAGE));
    assertFalse(body, body.contains("internal server error"));
    verifyNoInteractions(wallet);
  }

  private static String post(BroadcastHexServlet servlet, Wallet wallet, String hex)
      throws Exception {
    ReflectionTestUtils.setField(servlet, "wallet", wallet);
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/wallet/broadcasthex");
    request.setContentType("application/json");
    request.setContent(("{\"transaction\":\"" + hex + "\"}").getBytes(StandardCharsets.UTF_8));
    MockHttpServletResponse response = new MockHttpServletResponse();
    servlet.doPost(request, response);
    return response.getContentAsString();
  }
}
