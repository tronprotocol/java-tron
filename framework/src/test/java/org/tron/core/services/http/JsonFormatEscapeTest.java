package org.tron.core.services.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.protobuf.ByteString;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.tron.common.utils.ByteArray;
import org.tron.protos.contract.AssetIssueContractOuterClass.AssetIssueContract;

/**
 * Outbound escaping of name-string {@code bytes} fields.
 *
 * <p>Before this fix {@code escapeBytesSelfType} only escaped the double quote, leaving backslash
 * and control chars raw, and validated the result with a lenient parser. That let attacker
 * controlled on-chain bytes emit invalid JSON and, worse, forge sibling fields on the
 * re-parse path used by {@link Util#printTransactionToJSON}.
 *
 * <p>Assertions here always state which parser they use:
 * <ul>
 *   <li>{@link #strict()} - RFC 8259 baseline. Trailing tokens are rejected. A bare
 *       {@code readTree} is NOT a strict baseline: it stops after the first value and would
 *       silently accept trailing payloads and duplicated output.</li>
 *   <li>{@link org.tron.json.JSON} - the node's own lenient parser, used by
 *       {@code Util.printTransactionToJSON}. Never used to assert validity, only to reproduce
 *       what the node itself would hand back to a client.</li>
 * </ul>
 */
public class JsonFormatEscapeTest {

  private static final String URL_FIELD = "protocol.AssetIssueContract.url";
  private static final String DESC_FIELD = "protocol.AssetIssueContract.description";
  private static final String NAME_FIELD = "protocol.AssetIssueContract.name";
  private static final String ABBR_FIELD = "protocol.AssetIssueContract.abbr";

  private static final byte[] OWNER = new byte[21];

  static {
    OWNER[0] = 0x41;
  }

  /**
   * Strict RFC 8259 baseline for the features this issue turns on: structural leniency and
   * unescaped control chars. Each is disabled explicitly rather than relying on Jackson's
   * defaults, so a future default change cannot silently weaken these assertions. Number
   * related leniency that {@link org.tron.json.JSON} also enables is not relevant here and is
   * left at Jackson's (already strict) default.
   */
  private static ObjectMapper strict() {
    return JsonMapper.builder()
        .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
        .disable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
        .disable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
        .disable(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES)
        .disable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
        .disable(JsonReadFeature.ALLOW_TRAILING_COMMA)
        .disable(JsonReadFeature.ALLOW_LEADING_ZEROS_FOR_NUMBERS)
        .build();
  }

  /** Guards the guard: the baseline must actually reject what it claims to reject. */
  @Test
  public void testStrictBaselineIsActuallyStrict() {
    assertTrue(parsesStrictly("{\"a\":1}"));
    assertFalse("trailing token", parsesStrictly("{\"a\":1} garbage"));
    assertFalse("duplicated object (print() amplification shape)",
        parsesStrictly("{\"a\":1}\n{\"a\":2}"));
    assertFalse("java comment", parsesStrictly("{\"a\":1}//x"));
    assertFalse("single quotes", parsesStrictly("{'a':1}"));
    assertFalse("unquoted field name", parsesStrictly("{a:1}"));
    assertFalse("raw control char in string", parsesStrictly("{\"a\":\"x\ny\"}"));
  }

  private static String escapeName(byte[] raw, String field) {
    return JsonFormat.escapeBytesSelfType(ByteString.copyFrom(raw), field);
  }

  private static String escapeName(String raw, String field) {
    return escapeName(raw.getBytes(StandardCharsets.UTF_8), field);
  }

  private static boolean parsesStrictly(String body) {
    try {
      strict().readTree(body);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  /** Renders one asset issue contract exactly as the visible=true HTTP path would. */
  private static String printContract(AssetIssueContract contract) {
    return JsonFormat.printToString(contract, true);
  }

  private static AssetIssueContract assetWithDescription(byte[] description) {
    return AssetIssueContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(OWNER))
        .setName(ByteString.copyFromUtf8("REALTOKEN"))
        .setTotalSupply(1000)
        .setDescription(ByteString.copyFrom(description))
        .setUrl(ByteString.copyFromUtf8("https://honest.example"))
        .setFreeAssetNetLimit(111)
        .setPublicFreeAssetNetLimit(222)
        .build();
  }

  // ---------------------------------------------------------------------------------------
  // 1. escapeBytesSelfType direct behaviour
  // ---------------------------------------------------------------------------------------

  @Test
  public void testAllControlCharsAreEscaped() {
    for (int c = 0x00; c <= 0x1F; c++) {
      byte[] raw = new byte[] {'a', (byte) c, 'b'};
      String escaped = escapeName(raw, URL_FIELD);

      assertFalse("raw control char 0x" + Integer.toHexString(c) + " leaked into the output",
          escaped.chars().anyMatch(ch -> ch <= 0x1F));
      assertTrue("strict parser rejected escaped control char 0x" + Integer.toHexString(c),
          parsesStrictly("{\"url\":\"" + escaped + "\"}"));
    }
  }

  @Test
  public void testBackslashIsEscaped() {
    assertEquals("C:\\\\dir", escapeName("C:\\dir", URL_FIELD));
    assertTrue(parsesStrictly("{\"url\":\"" + escapeName("C:\\dir", URL_FIELD) + "\"}"));
  }

  @Test
  public void testDoubleQuoteIsEscaped() {
    assertEquals("a\\\"b", escapeName("a\"b", URL_FIELD));
    assertTrue(parsesStrictly("{\"url\":\"" + escapeName("a\"b", URL_FIELD) + "\"}"));
  }

  @Test
  public void testNewlineIsEscapedNotRaw() {
    assertEquals("a\\nb", escapeName("a\nb", URL_FIELD));
  }

  @Test
  public void testValidUtf8IsPreserved() {
    assertEquals("ok", escapeName("ok", URL_FIELD));
    assertEquals("中文", escapeName("中文", URL_FIELD));
  }

  @Test
  public void testInvalidUtf8FallsBackToHexWithoutReplacementChar() {
    byte[] raw = new byte[] {0x61, (byte) 0xff, 0x62};
    String escaped = escapeName(raw, URL_FIELD);

    assertEquals("61ff62", escaped);
    assertFalse("invalid UTF-8 must not be decoded into U+FFFD mojibake",
        escaped.indexOf('�') >= 0);
    assertTrue(parsesStrictly("{\"url\":\"" + escaped + "\"}"));
  }

  @Test
  public void testUnpairedSurrogateBytesFallBackToHexInsteadOfThrowing() {
    // 0xED 0xA0 0x80 is the UTF-8 style encoding of an unpaired high surrogate. escapeText()
    // would throw on it, so isValidUtf8() must divert it to hex first.
    byte[] raw = new byte[] {(byte) 0xed, (byte) 0xa0, (byte) 0x80};

    assertEquals("eda080", escapeName(raw, URL_FIELD));
  }

  @Test
  public void testSupplementaryPlaneIsEmittedAsSurrogateEscapes() {
    String emoji = new String(Character.toChars(0x1F600));

    assertEquals("\\ud83d\\ude00", escapeName(emoji, URL_FIELD));
  }

  // ---------------------------------------------------------------------------------------
  // 2. Field forgery on the Util.printTransactionToJSON re-parse path. Highest priority:
  //    this is the most severe consequence of the old escaping.
  // ---------------------------------------------------------------------------------------

  /**
   * Reproduces {@code Util.printTransactionToJSON} line 272:
   * {@code JSONObject.parseObject(JsonFormat.printToString(contract, selfType))}.
   */
  private static org.tron.json.JSONObject reparseAsNodeDoes(AssetIssueContract contract) {
    return org.tron.json.JSONObject.parseObject(printContract(contract));
  }

  /**
   * The node does not stop at parseObject: it re-serializes with toJSONString() and returns
   * that. Pre-fix, that step laundered a forged object into well formed JSON the client could
   * not tell apart from a genuine response. Assert the whole chain, not just the parse.
   */
  private static void assertNoForgeryThroughNormalization(AssetIssueContract contract)
      throws Exception {
    org.tron.json.JSONObject parsed = reparseAsNodeDoes(contract);
    String normalized = parsed.toJSONString();

    assertTrue("normalized output must be valid JSON: " + normalized,
        parsesStrictly(normalized));

    // Assert on parsed FIELD VALUES, not on substring presence: the attacker payload is stored
    // in description, so the literal text "evil"/"TFAKE" legitimately appears there. What must
    // not happen is those values landing in url / name / owner_address.
    JsonNode node = strict().readTree(normalized);
    assertEquals("url must be the real one", "https://honest.example",
        node.get("url").asText());
    assertEquals("name must be the real one", "REALTOKEN", node.get("name").asText());
    assertTrue("owner_address must be the real base58 owner",
        node.get("owner_address").asText().startsWith("T"));
    assertFalse("owner_address must not be attacker supplied",
        node.get("owner_address").asText().contains("TFAKE"));
    assertEquals("total_supply must be the real one", 1000, node.get("total_supply").asLong());
  }

  @Test
  public void testSingleQuotePayloadCannotForgeSiblingField() throws Exception {
    // ALLOW_SINGLE_QUOTES is enabled on the node's lenient parser, so a payload can supply a
    // value without ever using a double quote, which the old replaceAll() would have escaped.
    byte[] payload = "a\\\",url:'https://evil'}//".getBytes(StandardCharsets.UTF_8);
    AssetIssueContract contract = assetWithDescription(payload);

    org.tron.json.JSONObject parsed = reparseAsNodeDoes(contract);

    assertEquals("https://honest.example", String.valueOf(parsed.get("url")));
    assertEquals(new String(payload, StandardCharsets.UTF_8),
        String.valueOf(parsed.get("description")));
    assertTrue("strict parser must accept the emitted document",
        parsesStrictly(printContract(contract)));
    assertNoForgeryThroughNormalization(contract);
  }

  @Test
  public void testDuplicateKeyPayloadCannotOverrideEarlierFields() throws Exception {
    byte[] payload =
        "x\\\",owner_address:'TFAKE',name:'FAKE',total_supply:1,url:'https://evil'}//"
            .getBytes(StandardCharsets.UTF_8);
    AssetIssueContract contract = assetWithDescription(payload);

    org.tron.json.JSONObject parsed = reparseAsNodeDoes(contract);

    assertEquals("REALTOKEN", String.valueOf(parsed.get("name")));
    assertEquals(1000, Integer.parseInt(String.valueOf(parsed.get("total_supply"))));
    assertEquals("https://honest.example", String.valueOf(parsed.get("url")));
    assertFalse("owner_address must not be attacker supplied",
        String.valueOf(parsed.get("owner_address")).contains("TFAKE"));
    assertNoForgeryThroughNormalization(contract);
  }

  @Test
  public void testUnquotedFieldNamePayloadInjectsNothing() {
    AssetIssueContract contract =
        assetWithDescription("a\\\",b:1}//".getBytes(StandardCharsets.UTF_8));

    org.tron.json.JSONObject parsed = reparseAsNodeDoes(contract);

    assertNull("no attacker key may appear", parsed.get("b"));
    assertEquals("https://honest.example", String.valueOf(parsed.get("url")));
  }

  @Test
  public void testBlockCommentPayloadInjectsNothing() {
    AssetIssueContract contract =
        assetWithDescription("a\\\",b:1}/*".getBytes(StandardCharsets.UTF_8));

    org.tron.json.JSONObject parsed = reparseAsNodeDoes(contract);

    assertNull(parsed.get("b"));
    assertEquals("https://honest.example", String.valueOf(parsed.get("url")));
  }

  @Test
  public void testTrailingTokenPayloadInjectsNothing() {
    AssetIssueContract contract =
        assetWithDescription("a\\\"}".getBytes(StandardCharsets.UTF_8));

    org.tron.json.JSONObject parsed = reparseAsNodeDoes(contract);

    assertEquals("https://honest.example", String.valueOf(parsed.get("url")));
    assertTrue(parsesStrictly(printContract(contract)));
  }

  // ---------------------------------------------------------------------------------------
  // 3. Field preservation: nothing after the payload may be dropped.
  // ---------------------------------------------------------------------------------------

  @Test
  public void testFieldsAfterPayloadSurvive() {
    AssetIssueContract contract =
        assetWithDescription("a\\\"}//".getBytes(StandardCharsets.UTF_8));

    org.tron.json.JSONObject parsed = reparseAsNodeDoes(contract);

    assertEquals("https://honest.example", String.valueOf(parsed.get("url")));
    assertEquals(111, Integer.parseInt(String.valueOf(parsed.get("free_asset_net_limit"))));
    assertEquals(222,
        Integer.parseInt(String.valueOf(parsed.get("public_free_asset_net_limit"))));
  }

  // ---------------------------------------------------------------------------------------
  // 4. name and abbr have different length ceilings, so each needs its own payload.
  //    name  <= 32 bytes (MAX_ASSET_NAME_LEN), abbr <= 5 bytes (MAX_TOKEN_ABBR_NAME_LEN).
  // ---------------------------------------------------------------------------------------

  @Test
  public void testNameFieldSixByteBreakPayloadIsEscaped() {
    byte[] payload = "a\\\"}//".getBytes(StandardCharsets.UTF_8);
    assertEquals("payload sized for the name field", 6, payload.length);
    assertEquals("a\\\\\\\"}//", escapeName(payload, NAME_FIELD));

    AssetIssueContract contract = AssetIssueContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(OWNER))
        .setName(ByteString.copyFrom(payload))
        .setUrl(ByteString.copyFromUtf8("https://honest.example"))
        .build();

    assertTrue(parsesStrictly(printContract(contract)));
    assertEquals("https://honest.example",
        String.valueOf(reparseAsNodeDoes(contract).get("url")));
  }

  @Test
  public void testAbbrFieldFiveByteBreakPayloadIsEscaped() {
    // The 6 byte name payload exceeds MAX_TOKEN_ABBR_NAME_LEN, so abbr needs a shorter one.
    byte[] payload = "\\\"}".getBytes(StandardCharsets.UTF_8);
    assertTrue("payload must fit the abbr ceiling", payload.length <= 5);
    assertEquals("\\\\\\\"}", escapeName(payload, ABBR_FIELD));

    AssetIssueContract contract = AssetIssueContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(OWNER))
        .setAbbr(ByteString.copyFrom(payload))
        .setUrl(ByteString.copyFromUtf8("https://honest.example"))
        .build();

    assertTrue(parsesStrictly(printContract(contract)));
    assertEquals("https://honest.example",
        String.valueOf(reparseAsNodeDoes(contract).get("url")));
  }

  // ---------------------------------------------------------------------------------------
  // 5. Round trip. Only promised for valid UTF-8.
  // ---------------------------------------------------------------------------------------

  private static void assertRoundTrips(byte[] raw) throws Exception {
    String escaped = escapeName(raw, URL_FIELD);

    // The emitted document must be strictly valid...
    assertTrue("emitted value must parse strictly",
        parsesStrictly("{\"url\":\"" + escaped + "\"}"));

    // ...and the inbound decoder must rebuild the exact bytes. Feed it the RAW escaped token
    // body, which is what Tokenizer does (JsonFormat.java: substring(1, len - 1), no
    // pre-unescaping). Decoding the JSON here first would model a path that does not exist and
    // would double-unescape.
    ByteString back = JsonFormat.Tokenizer.unescapeBytesSelfType(escaped, URL_FIELD);

    assertEquals(ByteString.copyFrom(raw), back);
  }

  @Test
  public void testRoundTripForValidUtf8() throws Exception {
    assertRoundTrips("ok".getBytes(StandardCharsets.UTF_8));
    assertRoundTrips("中文".getBytes(StandardCharsets.UTF_8));
    assertRoundTrips("C:\\dir".getBytes(StandardCharsets.UTF_8));
    assertRoundTrips("a\"b".getBytes(StandardCharsets.UTF_8));
    assertRoundTrips("a\nb".getBytes(StandardCharsets.UTF_8));
    assertRoundTrips("a\\\"}//".getBytes(StandardCharsets.UTF_8));
    assertRoundTrips(new String(Character.toChars(0x1F600)).getBytes(StandardCharsets.UTF_8));
    // Literal backslash-u-0041 on chain. The old code emitted it unchanged, so a client
    // decoded it as the single char 'A' -- the value silently differed from the stored bytes.
    assertRoundTrips("a\\u0041".getBytes(StandardCharsets.UTF_8));
  }

  @Test
  public void testInvalidUtf8IsHexAndNotPromisedToRoundTrip() {
    // Documented boundary: the hex representation is faithful, but visible=true is not a
    // lossless two way format for arbitrary bytes, because unescapeBytesSelfType applies
    // copyFromUtf8 to whatever text it receives.
    byte[] raw = new byte[] {0x61, (byte) 0xff, 0x62};
    String escaped = escapeName(raw, URL_FIELD);

    assertEquals(ByteArray.toHexString(raw), escaped);
    assertTrue(parsesStrictly("{\"url\":\"" + escaped + "\"}"));

    ByteString back = ByteString.copyFromUtf8(escaped);
    assertFalse("hex text is not expected to reconstruct the original bytes",
        back.equals(ByteString.copyFrom(raw)));
  }

  // ---------------------------------------------------------------------------------------
  // 5b. U+FFFF must not truncate. CharacterIterator.DONE is the real code point U+FFFF, so an
  //     iterator-driven escape loop silently dropped everything from the first U+FFFF onward.
  // ---------------------------------------------------------------------------------------

  @Test
  public void testUffffDoesNotTruncateNameStringField() {
    // EF BF BF is valid UTF-8, so isValidUtf8() does not divert it to hex: escapeText must cope.
    String value = "SAFE\uFFFF TAIL";
    assertTrue(ByteString.copyFromUtf8(value).isValidUtf8());

    assertEquals(value, escapeName(value, URL_FIELD));
  }

  @Test
  public void testUffffDoesNotTruncateProtoStringField() {
    // AssetIssueContract.id is a proto string field, escaped via escapeText() on a path this
    // change does not otherwise touch. It was truncated by the same sentinel bug.
    AssetIssueContract contract = AssetIssueContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(OWNER))
        .setId("STR\uFFFF TAIL")
        .build();

    assertTrue(printContract(contract).contains("STR\uFFFF TAIL"));
  }

  @Test
  public void testUffffLeadingCharDoesNotEmptyTheValue() {
    assertEquals("\uFFFFwhole value", escapeName("\uFFFFwhole value", URL_FIELD));
  }

  // ---------------------------------------------------------------------------------------
  // 5c. print -> merge round trip through the REAL public API. This is the
  //     create -> (client signs) -> broadcast shape: if merge cannot rebuild identical bytes,
  //     raw_data and therefore txID differ and the signature is rejected.
  // ---------------------------------------------------------------------------------------

  private static void assertPrintMergeRoundTrips(String description, boolean visible)
      throws Exception {
    AssetIssueContract original = AssetIssueContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(OWNER))
        .setDescription(ByteString.copyFromUtf8(description))
        .setUrl(ByteString.copyFromUtf8("https://ok.example"))
        .build();

    AssetIssueContract.Builder rebuilt = AssetIssueContract.newBuilder();
    JsonFormat.merge(JsonFormat.printToString(original, visible), rebuilt, visible);

    assertEquals("raw_data must be byte identical after print -> merge (visible=" + visible
        + ", value=" + description + ")", original.toByteString(), rebuilt.build().toByteString());
  }

  @Test
  public void testPrintMergeRoundTripVisible() throws Exception {
    for (String v : new String[] {
        "plain",
        "C:\\ndir",      // backslash that forms a valid JSON escape
        "C:\\dir",       // backslash that does not
        "a\"b",          // double quote
        "a\nb",          // real control char
        "tab\there",
        "中文",
        "\uFFFF sentinel",
        "a\\\",url:'https://evil'}//"}) {
      assertPrintMergeRoundTrips(v, true);
    }
  }

  @Test
  public void testPrintMergeRoundTripHex() throws Exception {
    for (String v : new String[] {"plain", "C:\\ndir", "a\"b", "a\nb", "中文"}) {
      assertPrintMergeRoundTrips(v, false);
    }
  }

  // ---------------------------------------------------------------------------------------
  // 5d. Inbound escape handling.
  //     Scope note: unescapeText() is NOT limited to name-string bytes fields. Tokenizer's
  //     consumeString() calls it for every proto string field with no visible/selfType check,
  //     so the escape-set changes here reach proto string fields under visible=false too.
  //     See testInboundEscapeSetAppliesToProtoStringRegardlessOfVisible.
  //     The supported set is every RFC 8259 standard escape PLUS the historical \' extension
  //     that this code has always accepted; it is deliberately not narrowed to exactly RFC
  //     8259. See testInboundKeepsHistoricalSingleQuoteEscape.
  // ---------------------------------------------------------------------------------------

  private static String mergedDescription(String jsonEscapedValue) throws Exception {
    AssetIssueContract.Builder b = AssetIssueContract.newBuilder();
    JsonFormat.merge("{\"description\": \"" + jsonEscapedValue + "\"}", b, true);
    return b.getDescription().toStringUtf8();
  }

  @Test
  public void testInboundAcceptsEscapedSolidus() throws Exception {
    // \/ is valid per RFC 8259 and is emitted by common encoders (e.g. PHP json_encode
    // without JSON_UNESCAPED_SLASHES). escapeText() never produces it, but merge must take it.
    assertTrue(parsesStrictly("{\"d\":\"https:\\/\\/tron.network\"}"));

    assertEquals("https://tron.network", mergedDescription("https:\\/\\/tron.network"));
  }

  @Test
  public void testInboundAcceptsStandardEscapes() throws Exception {
    assertEquals("a\nb", mergedDescription("a\\nb"));
    assertEquals("a\"b", mergedDescription("a\\\"b"));
    assertEquals("C:\\dir", mergedDescription("C:\\\\dir"));
    assertEquals("aA", mergedDescription("a\\u0041"));
  }

  @Test
  public void testInboundRejectsInvalidEscapeWithAccurateMessage() {
    // Not valid JSON, so rejecting is correct; the message must name the real problem.
    JsonFormat.ParseException e = org.junit.Assert.assertThrows(JsonFormat.ParseException.class,
        () -> mergedDescription("a\\uZZZZ"));

    assertTrue("message must describe the escape, not a base58 failure: " + e.getMessage(),
        e.getMessage().contains("Invalid escape sequence"));
    assertFalse("bad \\u must not be reported as a base58 error",
        e.getMessage().contains("base58"));
  }

  @Test
  public void testInboundRejectsSignedUnicodeEscape() {
    // Integer.parseInt accepts a leading sign. Without a per-digit hex check, an escape written
    // with '+' parses as a valid code point, and one written with '-' yields a NEGATIVE value
    // that wraps into a completely unrelated char. Neither is valid JSON.
    for (String bad : new String[] {"a\\u+123", "a\\u-123", "a\\u 123", "a\\u12 3"}) {
      assertFalse("must not be valid JSON: " + bad,
          parsesStrictly("{\"d\":\"" + bad + "\"}"));
      org.junit.Assert.assertThrows("must be rejected: " + bad,
          JsonFormat.ParseException.class, () -> mergedDescription(bad));
    }
  }

  /** Proto string fields reach unescapeText() through consumeString(), with no visible check. */
  private static String mergedId(String jsonEscapedValue, boolean visible) throws Exception {
    AssetIssueContract.Builder b = AssetIssueContract.newBuilder();
    JsonFormat.merge("{\"id\": \"" + jsonEscapedValue + "\"}", b, visible);
    return b.getId();
  }

  @Test
  public void testInboundEscapeSetAppliesToProtoStringRegardlessOfVisible() throws Exception {
    // AssetIssueContract.id is a proto string field. The escape-set changes are NOT scoped to
    // visible=true, and NOT scoped to name-string bytes fields: they apply here under both.
    for (boolean visible : new boolean[] {true, false}) {
      assertEquals("escaped solidus must be accepted (visible=" + visible + ")",
          "a/b", mergedId("a\\/b", visible));
      assertEquals("standard escapes unchanged (visible=" + visible + ")",
          "aA", mergedId("a\\u0041", visible));
      final boolean v = visible;
      org.junit.Assert.assertThrows("signed \\u must be rejected (visible=" + visible + ")",
          JsonFormat.ParseException.class, () -> mergedId("a\\u+123", v));
    }
  }

  @Test
  public void testInboundKeepsHistoricalSingleQuoteEscape() throws Exception {
    // \' is NOT an RFC 8259 escape, but this decoder has always accepted it and the tokenizer
    // also accepts single quoted strings. Kept on purpose; pinned here so the supported set is
    // documented as "RFC 8259 standard escapes PLUS \'", not "exactly RFC 8259".
    assertFalse("not valid JSON", parsesStrictly("{\"d\":\"a\\'b\"}"));

    assertEquals("a'b", mergedDescription("a\\'b"));
    assertEquals("a'b", mergedId("a\\'b", true));
  }

  /**
   * Documents the boundary deliberately NOT changed here. The escape handling inside a double
   * quoted string now matches RFC 8259, but JsonFormat's own tokenizer keeps its historical
   * leniency in the surrounding syntax. Tightening that would be a far larger compatibility
   * change than this fix, so it stays. This test pins the current boundary so a future change
   * to it is a conscious decision rather than an accident.
   */
  @Test
  public void testOuterSyntaxLeniencyIsUnchanged() throws Exception {
    // Raw control chars inside a string: not valid JSON, still accepted by the tokenizer.
    assertFalse(parsesStrictly("{\"d\":\"a\tb\"}"));
    assertEquals("a\tb", mergedDescription("a\tb"));
    assertEquals("a\rb", mergedDescription("a\rb"));

    // Single quoted values and unquoted field names: also still accepted.
    AssetIssueContract.Builder b1 = AssetIssueContract.newBuilder();
    JsonFormat.merge("{\"description\":'abc'}", b1, true);
    assertEquals("abc", b1.getDescription().toStringUtf8());

    AssetIssueContract.Builder b2 = AssetIssueContract.newBuilder();
    JsonFormat.merge("{description:\"abc\"}", b2, true);
    assertEquals("abc", b2.getDescription().toStringUtf8());
  }

  // ---------------------------------------------------------------------------------------
  // 6. JsonGenerator.print must not duplicate the tail on every newline.
  // ---------------------------------------------------------------------------------------

  @Test
  public void testPrintDoesNotDuplicateTailOnNewline() throws Exception {
    assertPrintEmitsExactly("a\nb");
    assertPrintEmitsExactly("a\nb\nc");
    assertPrintEmitsExactly("\n");
    assertPrintEmitsExactly("\n\n");
    assertPrintEmitsExactly("trailing\n");
    assertPrintEmitsExactly("no newline at all");
  }

  private static void assertPrintEmitsExactly(String text) throws Exception {
    StringWriter out = new StringWriter();
    // Assert the exact string. A parser based check would miss duplication that still
    // happens to be well formed.
    new JsonFormat.JsonGenerator(out).print(text);

    assertEquals(text, out.toString());
  }

  @Test
  public void testNewlineHeavyValueDoesNotAmplifyOutput() {
    byte[] url = new byte[256];
    java.util.Arrays.fill(url, (byte) 'A');
    for (int i = 0; i < 128; i++) {
      url[i] = '\n';
    }
    AssetIssueContract contract = AssetIssueContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(OWNER))
        .setUrl(ByteString.copyFrom(url))
        .build();

    String body = printContract(contract);

    assertTrue("output must not blow up relative to the input", body.length() < 1024);
    assertTrue(parsesStrictly(body));
  }

  // ---------------------------------------------------------------------------------------
  // 5e. Unpaired surrogates must not throw out of the serializer.
  //     bytes name-string fields are screened by isValidUtf8(), but proto string fields reach
  //     escapeText() directly, so an unpaired surrogate there used to escape an
  //     IllegalArgumentException from printToString. It is now rendered as '?', matching what
  //     Java and protobuf substitute when encoding such a string to UTF-8 -- which makes the
  //     output identical whether or not the message has been through a protobuf round trip.
  // ---------------------------------------------------------------------------------------

  private static void assertSurrogateHandling(String id, String expectedJsonValue)
      throws Exception {
    AssetIssueContract inMemory = AssetIssueContract.newBuilder().setId(id).build();

    String beforeHop = JsonFormat.printToString(inMemory, true);
    String afterHop = JsonFormat.printToString(
        AssetIssueContract.parseFrom(inMemory.toByteString()), true);

    assertTrue("must be valid JSON: " + beforeHop, parsesStrictly(beforeHop));
    assertEquals("output must not depend on whether a protobuf round trip happened",
        beforeHop, afterHop);
    assertEquals("{\"id\": \"" + expectedJsonValue + "\"}", beforeHop);
  }

  @Test
  public void testUnpairedSurrogateDoesNotThrowAndIsHopInvariant() throws Exception {
    assertSurrogateHandling("x" + (char) 0xD83D, "x?");            // lone high
    assertSurrogateHandling("x" + (char) 0xDE00, "x?");            // lone low
    assertSurrogateHandling("x" + (char) 0xD83D + "y", "x?y");     // high, then a normal char
    assertSurrogateHandling("x" + (char) 0xDE00 + "y", "x?y");     // low, then a normal char
    assertSurrogateHandling("" + (char) 0xD83D + (char) 0xD83D, "??");
  }

  @Test
  public void testWellFormedSurrogatePairStillEscapedAsPair() throws Exception {
    assertSurrogateHandling(new String(Character.toChars(0x1F600)), "\\ud83d\\ude00");
    assertSurrogateHandling("a" + new String(Character.toChars(0x1F600)) + "b",
        "a\\ud83d\\ude00b");
  }

  // ---------------------------------------------------------------------------------------
  // 6b. Deterministic property sweep. Fixed seed and fixed bounds so any reviewer gets the
  //     exact same inputs. Properties, for every input:
  //       P1 escaping never throws
  //       P2 the emitted document is strictly valid
  //       P3 a field serialized AFTER the payload keeps its true value under BOTH the strict
  //          parser and the node's lenient one (no truncation, no forged or dropped key)
  //       P4 print -> merge rebuilds byte identical protobuf (valid UTF-8 only; invalid UTF-8
  //          goes to hex, which is deliberately not round trippable)
  // ---------------------------------------------------------------------------------------

  @Test
  public void testPropertySweep() throws Exception {
    int checked = 0;
    // every single byte, and every byte framed by ASCII
    for (int i = 0; i < 256; i++) {
      checked += sweepOne(new byte[] {(byte) i});
      checked += sweepOne(new byte[] {'a', (byte) i, 'b'});
    }
    // every 2-byte combination: covers all malformed UTF-8 lead/continuation pairs
    for (int i = 0; i < 256; i++) {
      for (int j = 0; j < 256; j++) {
        checked += sweepOne(new byte[] {(byte) i, (byte) j});
      }
    }
    // structural payloads built from the characters that matter to a JSON parser
    byte[] nasty = "\\\"'{}[],:/*\n\r\tu0 ".getBytes(StandardCharsets.UTF_8);
    java.util.Random rnd = new java.util.Random(20260720L);
    for (int n = 0; n < 20000; n++) {
      byte[] buf = new byte[1 + rnd.nextInt(16)];
      for (int i = 0; i < buf.length; i++) {
        buf[i] = rnd.nextInt(2) == 0
            ? nasty[rnd.nextInt(nasty.length)] : (byte) rnd.nextInt(256);
      }
      checked += sweepOne(buf);
    }
    // notable code points, including the U+FFFF sentinel and both surrogate halves
    for (int cp : new int[] {0x00, 0x1F, 0x20, 0x7F, 0x80, 0x7FF, 0x800, 0xFFFD, 0xFFFE,
        0xFFFF, 0x10000, 0x1F600, 0x10FFFF}) {
      checked += sweepOne(new String(Character.toChars(cp)).getBytes(StandardCharsets.UTF_8));
    }

    assertTrue("sweep must actually run", checked > 65000);
  }

  /** Returns 1 so the caller can count coverage. Throws AssertionError on any violation. */
  private static int sweepOne(byte[] raw) throws Exception {
    AssetIssueContract original = AssetIssueContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(OWNER))
        .setDescription(ByteString.copyFrom(raw))
        .setUrl(ByteString.copyFromUtf8("https://sentinel.example"))
        .setFreeAssetNetLimit(4242)
        .build();

    String json = printContract(original);                                            // P1
    String where = " for input " + ByteArray.toHexString(raw);

    JsonNode strictNode;
    try {
      strictNode = strict().readTree(json);                                           // P2
    } catch (Exception e) {
      throw new AssertionError("P2 strict parse failed" + where + ": " + json, e);
    }
    assertEquals("P3 strict: url" + where,
        "https://sentinel.example", strictNode.get("url").asText());
    assertEquals("P3 strict: free_asset_net_limit" + where,
        4242, strictNode.get("free_asset_net_limit").asLong());

    org.tron.json.JSONObject lenient = org.tron.json.JSONObject.parseObject(json);
    assertEquals("P3 lenient: url" + where,
        "https://sentinel.example", String.valueOf(lenient.get("url")));
    assertEquals("P3 lenient: free_asset_net_limit" + where,
        "4242", String.valueOf(lenient.get("free_asset_net_limit")));

    AssetIssueContract.Builder rebuilt = AssetIssueContract.newBuilder();
    JsonFormat.merge(json, rebuilt, true);
    if (ByteString.copyFrom(raw).isValidUtf8()) {
      assertEquals("P4 round trip" + where,
          original.toByteString(), rebuilt.build().toByteString());                   // P4
    } else {
      assertEquals("invalid UTF-8 must be emitted as plain hex" + where,
          ByteArray.toHexString(raw), escapeName(raw, DESC_FIELD));
    }
    return 1;
  }

  // ---------------------------------------------------------------------------------------
  // 7. Regression: content with nothing to escape must be byte identical to before the fix.
  // ---------------------------------------------------------------------------------------

  @Test
  public void testSafeAsciiIsUnchanged() {
    // "Safe ASCII" means no backslash, no double quote and no control chars. Plain ASCII as a
    // whole is not unchanged, because those three classes are exactly what now gets escaped.
    for (String safe : new String[] {
        "https://tron.network",
        "TronToken",
        "abc-123_456.789~xyz",
        ""}) {
      assertEquals(safe, escapeName(safe, URL_FIELD));
    }
  }

  @Test
  public void testAddressFieldsAreUnaffected() {
    String encoded = JsonFormat.escapeBytesSelfType(ByteString.copyFrom(OWNER),
        "protocol.AssetIssueContract.owner_address");

    assertTrue("address fields must still be base58check", encoded.startsWith("T"));
  }

  @Test
  public void testNonNameStringBytesFieldStillHex() {
    byte[] raw = "a\nb".getBytes(StandardCharsets.UTF_8);

    assertEquals(ByteArray.toHexString(raw),
        JsonFormat.escapeBytesSelfType(ByteString.copyFrom(raw), "protocol.Some.unmapped_field"));
  }

  @Test
  public void testVisibleFalseStillHex() {
    byte[] raw = "a\nb".getBytes(StandardCharsets.UTF_8);

    assertEquals(ByteArray.toHexString(raw),
        JsonFormat.escapeBytes(ByteString.copyFrom(raw), DESC_FIELD, false));
  }
}
