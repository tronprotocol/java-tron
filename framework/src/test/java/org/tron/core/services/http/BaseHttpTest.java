package org.tron.core.services.http;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.protobuf.ByteString;
import java.lang.reflect.Field;
import javax.servlet.http.HttpServlet;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.tron.common.TestConstants;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.core.config.args.Args;
import org.tron.protos.Protocol.Transaction;

/**
 * Base class for HTTP servlet unit tests.
 *
 * <p>Manages {@link Args} lifecycle so that
 * {@link org.tron.common.parameter.CommonParameter}
 * (e.g. {@code maxMessageSize}) is properly initialised from
 * {@code config-test.conf} before any servlet touches it, and
 * cleaned up after the test class finishes.
 */
public abstract class BaseHttpTest {

  protected static final Transaction MINIMAL_TX = Transaction.newBuilder()
      .setRawData(Transaction.raw.newBuilder().addContract(Transaction.Contract.newBuilder()))
      .build();

  @Mock
  protected Wallet wallet;
  private AutoCloseable closeable;

  @BeforeClass
  public static void initArgs() {
    Args.setParam(new String[]{}, TestConstants.TEST_CONF);
  }

  @AfterClass
  public static void clearArgs() {
    Args.clearParam();
  }

  @Before
  public void initMocks() throws Exception {
    closeable = MockitoAnnotations.openMocks(this);
    setUpMocks();
  }

  @After
  public void closeMocks() throws Exception {
    if (closeable != null) {
      closeable.close();
      closeable = null;
    }
  }

  /**
   * Override to configure mocks and inject the wallet into the servlet.
   */
  protected abstract void setUpMocks() throws Exception;

  /**
   * Injects the wallet mock into the servlet's private {@code wallet} field.
   */
  protected void injectWallet(HttpServlet servlet) throws Exception {
    Field f = servlet.getClass().getDeclaredField("wallet");
    f.setAccessible(true);
    f.set(servlet, wallet);
  }

  /**
   * Creates a POST request with JSON body.
   */
  protected static MockHttpServletRequest postRequest(String json) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setMethod("POST");
    request.setContentType("application/json");
    request.setContent(json.getBytes(UTF_8));
    request.setCharacterEncoding(UTF_8.name());
    return request;
  }

  /**
   * Creates a GET request with optional query parameters (key, value pairs).
   */
  protected static MockHttpServletRequest getRequest(String... params) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setMethod("GET");
    for (int i = 0; i < params.length - 1; i += 2) {
      request.addParameter(params[i], params[i + 1]);
    }
    return request;
  }

  protected static MockHttpServletResponse newResponse() {
    return new MockHttpServletResponse();
  }

  /**
   * Checks if a protobuf ByteString field matches the expected hex address.
   */
  protected static boolean addressEquals(ByteString actual, String expectedHex) {
    return ByteArray.toHexString(actual.toByteArray()).equals(expectedHex);
  }

  /**
   * Asserts that the servlet response represents a valid transaction:
   * no error, contains txID and raw_data.
   */
  protected static void assertTransactionResponse(MockHttpServletResponse response)
      throws Exception {
    String content = response.getContentAsString();
    assertFalse("Should not contain error", content.contains("\"Error\""));
    assertTrue("Should contain txID", content.contains("txID"));
    assertTrue("Should contain raw_data", content.contains("\"raw_data\""));
  }
}
