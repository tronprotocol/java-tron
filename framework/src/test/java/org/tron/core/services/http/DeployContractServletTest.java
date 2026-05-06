package org.tron.core.services.http;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.tron.common.utils.client.utils.HttpMethed.createRequest;

import javax.annotation.Resource;
import org.apache.http.client.methods.HttpPost;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.tron.common.BaseTest;
import org.tron.common.TestConstants;
import org.tron.core.config.args.Args;

public class DeployContractServletTest extends BaseTest {

  static {
    Args.setParam(new String[]{"--output-directory", dbPath()}, TestConstants.TEST_CONF);
  }

  @Resource
  private DeployContractServlet deployContractServlet;

  private static final String OWNER_ADDRESS = "A099357684BC659F5166046B56C95A0E99F1265CBD";

  private MockHttpServletResponse postWithAbi(String abi) {
    String body = "{"
        + "\"owner_address\":\"" + OWNER_ADDRESS + "\","
        + "\"name\":\"abi_validation_test\","
        + "\"bytecode\":\"00\","
        + "\"abi\":" + abi
        + "}";
    MockHttpServletRequest request = createRequest(HttpPost.METHOD_NAME);
    request.setContentType("application/json");
    request.setContent(body.getBytes(UTF_8));
    MockHttpServletResponse response = new MockHttpServletResponse();
    deployContractServlet.doPost(request, response);
    return response;
  }

  private static void assertRejected(MockHttpServletResponse response, String snippet)
      throws Exception {
    Assert.assertEquals(200, response.getStatus());
    String body = response.getContentAsString();
    Assert.assertTrue("expected error containing '" + snippet + "', got: " + body,
        body.contains("Error") && body.contains(snippet));
  }

  @Test
  public void rejectsShorthandUint() throws Exception {
    String abi = "[{\"type\":\"function\",\"name\":\"foo\","
        + "\"inputs\":[{\"name\":\"x\",\"type\":\"uint\"}],\"outputs\":[]}]";
    assertRejected(postWithAbi(abi), "shorthand uint/int");
  }

  @Test
  public void rejectsBadBytesN() throws Exception {
    String abi = "[{\"type\":\"function\",\"name\":\"foo\","
        + "\"inputs\":[{\"name\":\"x\",\"type\":\"bytes33\"}],\"outputs\":[]}]";
    assertRejected(postWithAbi(abi), "bytesN size");
  }

  @Test
  public void rejectsDuplicateFallback() throws Exception {
    String abi = "["
        + "{\"type\":\"fallback\",\"stateMutability\":\"payable\"},"
        + "{\"type\":\"fallback\",\"stateMutability\":\"payable\"}"
        + "]";
    assertRejected(postWithAbi(abi), "only one fallback");
  }

  @Test
  public void rejectsNonPayableReceive() throws Exception {
    String abi = "[{\"type\":\"receive\",\"stateMutability\":\"nonpayable\"}]";
    assertRejected(postWithAbi(abi), "must be payable");
  }

  @Test
  public void rejectsFixedFamily() throws Exception {
    String abi = "[{\"type\":\"function\",\"name\":\"foo\","
        + "\"inputs\":[{\"name\":\"x\",\"type\":\"fixed128x18\"}],\"outputs\":[]}]";
    assertRejected(postWithAbi(abi), "fixed/ufixed");
  }

  @Test
  public void acceptsTuplePermissively() throws Exception {
    String abi = "[{\"type\":\"function\",\"name\":\"foo\","
        + "\"inputs\":[{\"name\":\"x\",\"type\":\"tuple\"},"
        + "{\"name\":\"y\",\"type\":\"tuple[]\"}],\"outputs\":[]}]";
    MockHttpServletResponse response = postWithAbi(abi);
    Assert.assertEquals(200, response.getStatus());
    String body = response.getContentAsString();
    Assert.assertFalse("tuple should not be rejected: " + body, body.contains("\"Error\""));
  }
}
