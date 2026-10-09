package org.tron.core.services.http;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.Arrays;
import java.util.Collections;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.tron.common.BaseTest;
import org.tron.common.utils.StringUtil;
import org.tron.core.Wallet;
import org.tron.json.JSON;
import org.tron.json.JSONObject;

/**
 * The shared address-parameter contract for the reward and brokerage endpoints. The cases live
 * here rather than in each servlet's test so that the endpoints cannot drift apart in how they
 * answer a given malformed request.
 */
public abstract class AddressQueryServletTestBase extends BaseTest {

  static final String INVALID_ADDRESS_ERROR = "INVALID address";

  /** Invokes the concrete servlet's GET handler. */
  protected abstract void invokeGet(MockHttpServletRequest request,
      MockHttpServletResponse response);

  /** Invokes the concrete servlet's POST handler. */
  protected abstract void invokePost(MockHttpServletRequest request,
      MockHttpServletResponse response);

  /** The key this endpoint answers with, for instance {@code reward}. */
  protected abstract String valueKey();

  /** What this endpoint answers for a well-formed address it holds no state for. */
  protected abstract int valueWithoutState();

  protected static MockHttpServletRequest postRequest(String contentType) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setMethod("POST");
    request.setContentType(contentType);
    request.setCharacterEncoding(UTF_8.name());
    return request;
  }

  protected static MockHttpServletRequest getRequest(String address) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setMethod("GET");
    if (address != null) {
      request.addParameter("address", address);
    }
    return request;
  }

  protected static MockHttpServletRequest formRequest(String address) {
    MockHttpServletRequest request = postRequest("application/x-www-form-urlencoded");
    if (address != null) {
      request.addParameter("address", address);
    }
    return request;
  }

  protected static MockHttpServletRequest jsonRequest(String address) {
    return jsonRequest(address, "application/json");
  }

  protected static MockHttpServletRequest jsonRequest(String address, String contentType) {
    MockHttpServletRequest request = postRequest(contentType);
    String json = address == null ? "{}"
        : JSON.toJSONString(Collections.singletonMap("address", address));
    request.setContent(json.getBytes(UTF_8));
    return request;
  }

  /** A base58check payload whose first byte is not the address prefix. */
  protected static String wrongPrefixAddress() {
    byte[] address = new byte[21];
    address[0] = (byte) (Wallet.getAddressPreFixByte() + 1);
    return StringUtil.encode58Check(address);
  }

  /** A well-formed address that no store holds anything for. */
  protected static String canonicalNoStateAddress() {
    byte[] address = new byte[21];
    Arrays.fill(address, (byte) 7);
    address[0] = Wallet.getAddressPreFixByte();
    return StringUtil.encode58Check(address);
  }

  /** Runs the request through the servlet with the verb it carries, and returns the raw body. */
  protected String invoke(MockHttpServletRequest request) throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    if ("GET".equals(request.getMethod())) {
      invokeGet(request, response);
    } else {
      invokePost(request, response);
    }
    return response.getContentAsString();
  }

  private void assertInvalid(MockHttpServletRequest request, String submitted) throws Exception {
    String body = invoke(request);
    JSONObject result = JSONObject.parseObject(body);
    Assert.assertEquals(INVALID_ADDRESS_ERROR, result.get("Error"));
    Assert.assertNull(result.get(valueKey()));
    if (submitted != null && !submitted.isEmpty()) {
      Assert.assertFalse("the error must not echo the submitted address", body.contains(submitted));
    }
  }

  private void assertValue(MockHttpServletRequest request, int expected) throws Exception {
    JSONObject result = JSONObject.parseObject(invoke(request));
    Assert.assertEquals(expected, (int) result.get(valueKey()));
    Assert.assertNull(result.get("Error"));
  }

  @Test
  public void getAndPostRejectMalformedAddresses() throws Exception {
    String invalidBase58 = "Taaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    // '0' is outside the base58 alphabet, so the decoder itself raises
    String illegalBase58Char = "Taaaaaaaaaaaaaaaaa0aaaaaaaaaaaaaaa";
    String invalidHex = "41zz00000000000000000000000000000000000000";
    // unlike a raw "42..." string this reaches the prefix check instead of failing the alphabet
    String wrongPrefix = wrongPrefixAddress();
    String oversizedHex = "41" + StringUtils.repeat('0', 8192);
    String oversizedBase58 = "T" + StringUtils.repeat('a', 8192);

    assertInvalid(getRequest(invalidBase58), invalidBase58);
    assertInvalid(getRequest(illegalBase58Char), illegalBase58Char);
    assertInvalid(getRequest(wrongPrefix), wrongPrefix);
    assertInvalid(getRequest(oversizedHex), oversizedHex);
    assertInvalid(getRequest(oversizedBase58), oversizedBase58);
    assertInvalid(jsonRequest(oversizedBase58), oversizedBase58);
    assertInvalid(jsonRequest(invalidBase58), invalidBase58);
    assertInvalid(jsonRequest(invalidHex), invalidHex);
  }

  /**
   * An address that is absent or blank is rejected like any other address the endpoint cannot
   * use, on every verb and body encoding. It used to answer with the service default instead.
   */
  @Test
  public void missingOrBlankAddressIsRejected() throws Exception {
    assertInvalid(getRequest(null), null);
    assertInvalid(getRequest(""), null);
    assertInvalid(formRequest(null), null);
    assertInvalid(formRequest(""), null);
    assertInvalid(jsonRequest(null), null);
    assertInvalid(jsonRequest(""), null);
  }

  @Test
  public void validAddressWithoutStateRetainsServiceDefault() throws Exception {
    assertValue(getRequest(canonicalNoStateAddress()), valueWithoutState());
  }

  /**
   * The charset-suffixed content type is what browsers actually send, and it must take the
   * json-body branch of Util.checkGetParam rather than the form-parameter branch. The body
   * carries a well-formed address: on the form branch no address would be found at all and
   * the answer would be a rejection instead of the value.
   */
  @Test
  public void jsonBodyWithCharsetSuffixIsParsed() throws Exception {
    assertValue(jsonRequest(canonicalNoStateAddress(), "application/json; charset=utf-8"),
        valueWithoutState());
  }

  @Test
  public void malformedJsonBodyIsRejectedWithoutEchoingIt() throws Exception {
    String marker = "UniqueMarkerValue";
    MockHttpServletRequest request = postRequest("application/json");
    request.setContent(("{\"address\":" + marker + "}").getBytes(UTF_8));

    String body = invoke(request);
    Assert.assertFalse(body.contains(marker));
    JSONObject result = JSONObject.parseObject(body);
    Assert.assertEquals("INVALID JSON body", result.get("Error"));
    Assert.assertNull(result.get(valueKey()));
  }
}
