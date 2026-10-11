package org.tron.core.services.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.LocalConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.Test;
import org.tron.json.JSONObject;
import org.tron.protos.Protocol.Account;

public class JsonFormatIdentifierTest {

  private static final String EMOJI = new String(Character.toChars(0x1F600));

  @Test
  public void testSupplementaryCharacterKeepsLegacyReplacement() {
    assertIdentifierError(EMOJI, "?");
    assertIdentifierError("prefix" + EMOJI, "?");
  }

  @Test
  public void testUnpairedSurrogatesKeepLegacyReplacement() {
    String highSurrogate = String.valueOf((char) 0xD83D);
    String lowSurrogate = String.valueOf((char) 0xDE00);

    assertIdentifierError(highSurrogate, "?");
    assertIdentifierError(highSurrogate + "a", "?");
    assertIdentifierError(highSurrogate + highSurrogate, "?");
    assertIdentifierError(lowSurrogate, "?");
    assertIdentifierError(lowSurrogate + highSurrogate, "?");
  }

  @Test
  public void testOtherInvalidCharactersKeepTheirErrorMessages() {
    String bmpCharacter = String.valueOf((char) 0x4E2D);

    assertIdentifierError("@", "@");
    assertIdentifierError("bad-name", "-");
    assertIdentifierError(bmpCharacter, bmpCharacter);
  }

  @Test
  public void testValidIdentifierStillParses() throws Exception {
    Account.Builder account = Account.newBuilder();

    JsonFormat.merge("{\"balance\":7}", account, false);

    assertEquals(7L, account.getBalance());
  }

  @Test
  public void testIdentifierErrorIsValidUtf8WithNativeJettyWriter() throws Exception {
    Server server = new Server();
    LocalConnector connector = new LocalConnector(server);
    server.addConnector(connector);
    ServletContextHandler context = new ServletContextHandler();
    context.setContextPath("/");
    context.addServlet(new ServletHolder(new IdentifierServlet()), "/parse");
    server.setHandler(context);

    try {
      server.start();
      byte[] body = ("{\"" + EMOJI + "\":1}").getBytes(StandardCharsets.UTF_8);
      byte[] headers = ("POST /parse HTTP/1.1\r\n"
          + "Host: localhost\r\n"
          + "Connection: close\r\n"
          + "Content-Type: application/json; charset=utf-8\r\n"
          + "Content-Length: " + body.length + "\r\n\r\n")
          .getBytes(StandardCharsets.US_ASCII);
      ByteBuffer request = ByteBuffer.allocate(headers.length + body.length);
      request.put(headers).put(body).flip();

      ByteBuffer response = connector.getResponse(request);
      byte[] wire = new byte[response.remaining()];
      response.get(wire);
      String raw = new String(wire, StandardCharsets.ISO_8859_1);
      assertTrue(raw.startsWith("HTTP/1.1 200 "));
      int headerEnd = raw.indexOf("\r\n\r\n");
      assertTrue("the response must contain complete headers", headerEnd >= 0);
      byte[] payload = Arrays.copyOfRange(wire, headerEnd + 4, wire.length);

      // String(byte[], UTF_8) replaces malformed bytes and would hide this regression.
      String json = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(payload)).toString();
      String error = JSONObject.parseObject(json).getString("Error");
      assertEquals("1:2: Expected identifier. -?", error);
    } finally {
      server.stop();
    }
  }

  private static void assertIdentifierError(String identifier, String expectedCharacter) {
    JsonFormat.ParseException error = assertThrows(JsonFormat.ParseException.class,
        () -> JsonFormat.merge("{\"" + identifier + "\":1}", Account.newBuilder(), false));

    assertEquals("1:2: Expected identifier. -" + expectedCharacter, error.getMessage());
  }

  private static class IdentifierServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws IOException {
      response.setContentType("application/json; charset=utf-8");
      try {
        JsonFormat.merge(request.getReader(), Account.newBuilder(), false);
      } catch (JsonFormat.ParseException e) {
        Util.processError(e, response);
      }
    }
  }
}
