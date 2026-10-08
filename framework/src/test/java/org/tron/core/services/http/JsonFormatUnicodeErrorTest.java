package org.tron.core.services.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.eclipse.jetty.server.LocalConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.config.args.Args;
import org.tron.core.services.ratelimiter.RateLimiterContainer;
import org.tron.json.JSONObject;
import org.tron.protos.Protocol.Account;

public class JsonFormatUnicodeErrorTest {

  private static final String EMOJI = new String(Character.toChars(0x1F600));

  @Test
  public void testInvalidSupplementaryEscapeKeepsLegacyReplacement() {
    assertEscapeError("\\" + EMOJI, "?");
    assertEscapeError("prefix\\" + EMOJI, "?");
  }

  @Test
  public void testInvalidUnpairedSurrogateEscapesKeepLegacyReplacement() {
    String highSurrogate = String.valueOf((char) 0xD83D);
    String lowSurrogate = String.valueOf((char) 0xDE00);

    assertEscapeError("\\" + highSurrogate, "?");
    assertEscapeError("\\" + highSurrogate + "a", "?");
    assertEscapeError("\\" + highSurrogate + highSurrogate, "?");
    assertEscapeError("\\" + lowSurrogate, "?");
    assertEscapeError("\\" + lowSurrogate + highSurrogate, "?");
  }

  @Test
  public void testOtherInvalidEscapesKeepTheirErrorMessages() {
    String bmpCharacter = String.valueOf((char) 0x4E2D);

    assertEscapeError("\\q", "q");
    assertEscapeError("\\@", "@");
    assertEscapeError("\\" + bmpCharacter, bmpCharacter);
  }

  @Test
  public void testValidUnicodeAndEscapesStillDecode() throws Exception {
    String bmpCharacter = String.valueOf((char) 0x4E2D);

    assertEquals(bmpCharacter + EMOJI, JsonFormat.unescapeText(bmpCharacter + EMOJI));
    assertEquals("\b\f\n\r\t\\/\"'" + bmpCharacter + EMOJI,
        JsonFormat.unescapeText("\\b\\f\\n\\r\\t\\\\\\/\\\"\\'\\u4e2d\\uD83D\\uDE00"));
  }

  @Test
  public void testDeployContractEscapeErrorIsValidUtf8WithNativeJettyWriter() throws Exception {
    JSONObject input = new JSONObject();
    input.put("owner_address", "");
    // The outer JSON is valid; only the embedded ABI contains an invalid escape.
    input.put("abi", "[{\"name\":\"\\" + EMOJI + "\"}]");

    String error = requestError(new DeployContractServlet(), "/wallet/deploycontract", input);

    assertEquals("1:20: Invalid escape sequence: '\\?'", error);
  }

  @Test
  public void testLongIntegerErrorIsValidUtf8WithNativeJettyWriter() throws Exception {
    JSONObject input = new JSONObject();
    // BigInteger's digit groups can split a surrogate pair inside its exception message.
    input.put("balance", "11111" + EMOJI + "1111111");

    String error = requestError(new GetAccountServlet(), "/wallet/getaccount", input);

    assertTrue(error.contains("1:12: Couldn't parse integer:"));
  }

  @Test
  public void testParserErrorsReplaceOnlyUnpairedSurrogates() throws Exception {
    String high = String.valueOf((char) 0xD800);
    String low = String.valueOf((char) 0xDC00);
    String description = high + "x" + EMOJI + low + high;
    String expected = "?x" + EMOJI + "??";
    JsonFormat.Tokenizer tokenizer = new JsonFormat.Tokenizer("first second");
    tokenizer.nextToken();

    String current = tokenizer.parseException(description).getMessage();
    String previous = tokenizer.parseExceptionPreviousToken(description).getMessage();

    assertEquals("1:7: " + expected, current);
    assertEquals("1:1: " + expected, previous);
    StandardCharsets.UTF_8.newEncoder().encode(CharBuffer.wrap(current));
    StandardCharsets.UTF_8.newEncoder().encode(CharBuffer.wrap(previous));
  }

  @Test
  public void testParserErrorsPreserveValidDescriptions() {
    String description = "invalid " + (char) 0x4E2D + EMOJI;
    JsonFormat.Tokenizer tokenizer = new JsonFormat.Tokenizer("field");

    assertEquals("1:1: " + description, tokenizer.parseException(description).getMessage());
    assertEquals("1:1: " + description,
        tokenizer.parseExceptionPreviousToken(description).getMessage());
    assertEquals("1:1: null", tokenizer.parseException(null).getMessage());
    assertEquals("1:1: null", tokenizer.parseExceptionPreviousToken(null).getMessage());
  }

  @Test
  public void testOrdinaryIntegerErrorsKeepTheirMessages() {
    JsonFormat.ParseException invalid = assertThrows(JsonFormat.ParseException.class,
        () -> JsonFormat.merge("{\"balance\":\"bad\"}", Account.newBuilder(), false));
    JsonFormat.ParseException overflow = assertThrows(JsonFormat.ParseException.class,
        () -> JsonFormat.merge("{\"balance\":9223372036854775808}",
            Account.newBuilder(), false));

    assertEquals("1:12: Couldn't parse integer: For input string: \"\"bad\"\"",
        invalid.getMessage());
    assertEquals("1:12: Couldn't parse integer: Number out of range for 64-bit signed integer: "
        + "9223372036854775808", overflow.getMessage());
  }

  private static String requestError(RateLimiterServlet servlet, String path, JSONObject input)
      throws Exception {
    CommonParameter args = Args.getInstance();
    long originalMaxSize = args.getHttpMaxMessageSize();
    boolean originalNonBlocking = args.isRateLimiterApiNonBlocking();
    Server server = new Server();
    LocalConnector connector = new LocalConnector(server);
    server.addConnector(connector);
    ServletContextHandler context = new ServletContextHandler();
    context.setContextPath("/");
    ReflectionTestUtils.setField(servlet, "container", new RateLimiterContainer());
    context.addServlet(new ServletHolder(servlet), path);
    server.setHandler(context);

    try {
      args.setHttpMaxMessageSize(1_000_000L);
      args.setRateLimiterApiNonBlocking(false);
      server.start();
      byte[] body = input.toJSONString().getBytes(StandardCharsets.UTF_8);
      byte[] headers = ("POST " + path + " HTTP/1.1\r\n"
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

      // A replacement decoder would hide invalid bytes emitted by the response writer.
      String json = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(payload)).toString();
      return JSONObject.parseObject(json).getString("Error");
    } finally {
      try {
        server.stop();
      } finally {
        args.setHttpMaxMessageSize(originalMaxSize);
        args.setRateLimiterApiNonBlocking(originalNonBlocking);
      }
    }
  }

  private static void assertEscapeError(String input, String expectedCharacter) {
    JsonFormat.InvalidEscapeSequence error = assertThrows(JsonFormat.InvalidEscapeSequence.class,
        () -> JsonFormat.unescapeText(input));

    assertEquals("Invalid escape sequence: '\\" + expectedCharacter + "'", error.getMessage());
  }
}
