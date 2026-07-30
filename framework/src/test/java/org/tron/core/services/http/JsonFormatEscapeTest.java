package org.tron.core.services.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.contract.AssetIssueContractOuterClass.AssetIssueContract;

/**
 * Escaping and decoding of name-string {@code bytes} fields.
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
  private static final String CONTRACT_NAME_FIELD =
      "protocol.Transaction.Contract.ContractName";
  private static final String[] NAME_STRING_FIELDS = {
      "protocol.Return.message",
      "protocol.Address.host",
      "protocol.Note.memo",
      "protocol.AccountUpdateContract.account_name",
      "protocol.SetAccountIdContract.account_id",
      "protocol.TransferAssetContract.asset_name",
      "protocol.WitnessCreateContract.url",
      "protocol.WitnessUpdateContract.update_url",
      "protocol.AssetIssueContract.name",
      "protocol.AssetIssueContract.abbr",
      "protocol.AssetIssueContract.description",
      "protocol.AssetIssueContract.url",
      "protocol.ParticipateAssetIssueContract.asset_name",
      "protocol.UpdateAssetContract.url",
      "protocol.UpdateAssetContract.description",
      "protocol.ExchangeCreateContract.first_token_id",
      "protocol.ExchangeCreateContract.second_token_id",
      "protocol.ExchangeInjectContract.token_id",
      "protocol.ExchangeWithdrawContract.token_id",
      "protocol.ExchangeTransactionContract.token_id",
      "protocol.AccountId.name",
      "protocol.Exchange.first_token_id",
      "protocol.Exchange.second_token_id",
      "protocol.Account.account_name",
      "protocol.Account.asset_issued_name",
      "protocol.Account.asset_issued_ID",
      "protocol.Account.account_id",
      "protocol.authority.permission_name",
      "protocol.Transaction.Contract.ContractName",
      "protocol.TransactionInfo.resMessage",
      "protocol.MarketSellAssetContract.sell_token_id",
      "protocol.MarketSellAssetContract.buy_token_id",
      "protocol.MarketOrder.sell_token_id",
      "protocol.MarketOrder.buy_token_id",
      "protocol.MarketOrderPair.sell_token_id",
      "protocol.MarketOrderPair.buy_token_id",
      "protocol.MarketPriceList.sell_token_id",
      "protocol.MarketPriceList.buy_token_id"
  };

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

  // Direct name-string byte escaping

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

  // Field integrity through HTTP normalization

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

  // Preservation of fields serialized after attacker-controlled values

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

  @Test
  public void testAssetNameEscapingPreservesFollowingFields() {
    byte[] value = "a\\\"}//".getBytes(StandardCharsets.UTF_8);
    assertEquals("a\\\\\\\"}//", escapeName(value, NAME_FIELD));

    AssetIssueContract contract = AssetIssueContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(OWNER))
        .setName(ByteString.copyFrom(value))
        .setUrl(ByteString.copyFromUtf8("https://honest.example"))
        .build();

    assertTrue(parsesStrictly(printContract(contract)));
    assertEquals("https://honest.example",
        String.valueOf(reparseAsNodeDoes(contract).get("url")));
  }

  @Test
  public void testAssetAbbrEscapingPreservesFollowingFields() {
    byte[] value = "\\\"}".getBytes(StandardCharsets.UTF_8);
    assertEquals("\\\\\\\"}", escapeName(value, ABBR_FIELD));

    AssetIssueContract contract = AssetIssueContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(OWNER))
        .setAbbr(ByteString.copyFrom(value))
        .setUrl(ByteString.copyFromUtf8("https://honest.example"))
        .build();

    assertTrue(parsesStrictly(printContract(contract)));
    assertEquals("https://honest.example",
        String.valueOf(reparseAsNodeDoes(contract).get("url")));
  }

  @Test
  public void testAllNameStringByteFieldsUseEscapeText() {
    String value = "a\\nb";
    String escaped = JsonFormat.escapeText(value);

    assertEquals("a\\\\nb", escaped);
    for (String field : NAME_STRING_FIELDS) {
      assertTrue(field, HttpSelfFormatFieldName.isNameStringFormat(field));
      assertEquals(field, escaped, escapeName(value, field));
    }
  }

  @Test
  public void testContractNameRetainsVisibleNameStringMapping() throws Exception {
    String value = "contract-name";
    Transaction.Contract.Builder actualContract = Transaction.Contract.newBuilder();
    JsonFormat.merge("{\"ContractName\":\"" + value + "\"}", actualContract, true);
    Transaction.Contract expectedContract = Transaction.Contract.newBuilder()
        .setContractName(ByteString.copyFromUtf8(value))
        .build();
    Transaction actual = Transaction.newBuilder()
        .setRawData(Transaction.raw.newBuilder().addContract(actualContract))
        .build();
    Transaction expected = Transaction.newBuilder()
        .setRawData(Transaction.raw.newBuilder().addContract(expectedContract))
        .build();

    assertEquals(value, escapeName(value, CONTRACT_NAME_FIELD));
    assertEquals(expectedContract.toByteString(), actualContract.build().toByteString());
    assertEquals(expected.getRawData().toByteString(), actual.getRawData().toByteString());
    assertEquals(new TransactionCapsule(expected).getTransactionId(),
        new TransactionCapsule(actual).getTransactionId());
  }

  // Inbound decoding and transaction ID derivation

  private static AssetIssueContract mergeDescription(String rawJsonToken) throws Exception {
    AssetIssueContract.Builder builder = AssetIssueContract.newBuilder();
    JsonFormat.merge("{\"description\":\"" + rawJsonToken + "\"}", builder, true);
    return builder.build();
  }

  private static Transaction transactionWithAsset(AssetIssueContract asset) {
    Transaction.Contract contract = Transaction.Contract.newBuilder()
        .setType(ContractType.AssetIssueContract)
        .setParameter(Any.pack(asset))
        .build();
    return Transaction.newBuilder()
        .setRawData(Transaction.raw.newBuilder()
            .setTimestamp(1_234_567_890L)
            .addContract(contract))
        .build();
  }

  private static void assertDecodedInboundMapping(String rawJsonToken, String expectedValue)
      throws Exception {
    AssetIssueContract actualAsset = mergeDescription(rawJsonToken);
    AssetIssueContract expectedAsset = AssetIssueContract.newBuilder()
        .setDescription(ByteString.copyFromUtf8(expectedValue))
        .build();
    Transaction actual = transactionWithAsset(actualAsset);
    Transaction expected = transactionWithAsset(expectedAsset);

    assertEquals("visible=true name-string bytes must decode JSON escape sequences",
        expectedAsset.toByteString(), actualAsset.toByteString());
    assertEquals("decoded name-string bytes must determine raw_data",
        expected.getRawData().toByteString(), actual.getRawData().toByteString());
    assertEquals("the transaction ID must be derived from the decoded raw_data",
        new TransactionCapsule(expected).getTransactionId(),
        new TransactionCapsule(actual).getTransactionId());
  }

  @Test
  public void testInboundNameStringEscapesDetermineRawDataAndTxId() throws Exception {
    assertDecodedInboundMapping("plain", "plain");
    assertDecodedInboundMapping("C:\\\\dir", "C:\\dir");
    assertDecodedInboundMapping("a\\nb", "a\nb");
    assertDecodedInboundMapping("a\\\"b", "a\"b");
    assertDecodedInboundMapping("a\\u0041", "aA");
  }

  @Test
  public void testInboundNameStringHandlesEscapesAndRejectsMalformedUnicode() throws Exception {
    assertDecodedInboundMapping("https:\\/\\/tron.network", "https://tron.network");
    assertDecodedInboundMapping("a\\u+123", "a" + (char) 0x0123);
    org.junit.Assert.assertThrows(
        "non-hex unicode escapes must still be rejected",
        JsonFormat.ParseException.class,
        () -> mergeDescription("a\\uZZZZ"));
  }

  @Test
  public void testValidUtf8NameStringsRoundTripThroughEscaping() throws Exception {
    for (String value : new String[] {
        "plain",
        "C:\\dir",
        "a\nb",
        "a\"b",
        "a\\u0041",
        "中文",
        "\uFFFF sentinel",
        new String(Character.toChars(0x1F600))}) {
      ByteString original = ByteString.copyFromUtf8(value);
      String escaped = JsonFormat.escapeBytesSelfType(original, DESC_FIELD);
      ByteString decoded = JsonFormat.Tokenizer.unescapeBytesSelfType(escaped, DESC_FIELD);

      assertEquals("valid UTF-8 name-string bytes must survive escape and unescape",
          original, decoded);
    }
  }

  @Test
  public void testOutboundSerializationDoesNotChangeStoredRawDataOrReportedTxId() {
    AssetIssueContract asset = AssetIssueContract.newBuilder()
        .setDescription(ByteString.copyFromUtf8("C:\\dir\n\"quoted\""))
        .build();
    Transaction transaction = transactionWithAsset(asset);
    ByteString rawDataBefore = transaction.getRawData().toByteString();
    String txIdBefore = ByteArray.toHexString(
        new TransactionCapsule(transaction).getTransactionId().getBytes());

    org.tron.json.JSONObject output = Util.printTransactionToJSON(transaction, true);

    assertEquals("query serialization must not modify the protobuf loaded from storage",
        rawDataBefore, transaction.getRawData().toByteString());
    assertEquals("the reported txID must still be derived from the stored raw_data bytes",
        txIdBefore, output.getString("txID"));
  }

  @Test
  public void testInvalidUtf8IsHexAndNotPromisedToRoundTrip() {
    byte[] raw = new byte[] {0x61, (byte) 0xff, 0x62};
    String escaped = escapeName(raw, URL_FIELD);

    assertEquals(ByteArray.toHexString(raw), escaped);
    assertTrue(parsesStrictly("{\"url\":\"" + escaped + "\"}"));
    assertFalse("hex text is not expected to reconstruct the original bytes",
        ByteString.copyFromUtf8(escaped).equals(ByteString.copyFrom(raw)));
  }

  // U+FFFF handling on the selected name-string bytes path

  @Test
  public void testUffffDoesNotTruncateNameStringField() {
    // EF BF BF is valid UTF-8, so it must remain text without reaching escapeText's sentinel.
    String value = "SAFE\uFFFF TAIL";
    assertTrue(ByteString.copyFromUtf8(value).isValidUtf8());

    assertEquals(value, escapeName(value, URL_FIELD));
  }

  @Test
  public void testUffffLeadingCharDoesNotEmptyTheValue() {
    assertEquals("\uFFFFwhole value", escapeName("\uFFFFwhole value", URL_FIELD));
  }

  // Proto string inbound compatibility

  private static String mergeId(String rawJsonToken, boolean visible) throws Exception {
    AssetIssueContract.Builder builder = AssetIssueContract.newBuilder();
    JsonFormat.merge("{\"id\":\"" + rawJsonToken + "\"}", builder, visible);
    return builder.getId();
  }

  @Test
  public void testProtoStringInboundEscapes() throws Exception {
    for (boolean visible : new boolean[] {true, false}) {
      assertEquals("escaped solidus must be accepted as valid JSON",
          "a/b", mergeId("a\\/b", visible));
      assertEquals("signed unicode escape must retain the develop-branch mapping",
          "a" + (char) 0x0123, mergeId("a\\u+123", visible));
      assertEquals("standard unicode escape must remain unchanged",
          "aA", mergeId("a\\u0041", visible));
    }
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

  // Deterministic property sweep

  // The fixed seed and bounds give every reviewer the same inputs. For each input:
  // P1: escaping never throws.
  // P2: the emitted document is strictly valid.
  // P3: fields serialized after the payload retain their values under both the strict parser
  //     and the node's lenient parser; no key is truncated, forged, or dropped.
  // P4: serialization does not modify the source protobuf bytes.
  // P5: valid UTF-8 name-string bytes survive outbound escaping and inbound decoding.

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

    ByteString originalBytes = original.toByteString();
    String json = printContract(original);                                            // P1
    String where = " for input " + ByteArray.toHexString(raw);
    assertEquals("P4 source protobuf changed during serialization" + where,
        originalBytes, original.toByteString());                                      // P4

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

    ByteString rawBytes = ByteString.copyFrom(raw);
    if (rawBytes.isValidUtf8()) {
      assertEquals("P5 valid UTF-8 must round-trip" + where, rawBytes,
          JsonFormat.Tokenizer.unescapeBytesSelfType(
              escapeName(raw, DESC_FIELD), DESC_FIELD));
    } else {
      assertEquals("invalid UTF-8 must be emitted as plain hex" + where,
          ByteArray.toHexString(raw), escapeName(raw, DESC_FIELD));
    }
    return 1;
  }

  // Unaffected-format regressions

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
