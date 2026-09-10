package org.tron.core.services.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.google.protobuf.InvalidProtocolBufferException;
import org.bouncycastle.util.encoders.DecoderException;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.HeaderNotFound;
import org.tron.core.exception.MaintenanceUnavailableException;
import org.tron.core.exception.ZkProofValidateException;
import org.tron.json.JSONException;
import org.tron.json.JSONObject;

public class UtilProcessErrorTest {

  private static final String INTERNAL_SERVER_ERROR = "internal server error";
  private static final String RATE_LIMITER_ERROR_MSG = "lack of computing resources";

  @Test
  public void exactCompatibilityTypesPreserveNonBlankMessage() throws Exception {
    assertError(new JsonFormat.ParseException("1:2: invalid \"field\"\nvalue"),
        "1:2: invalid \"field\"\nvalue");
    assertError(new ContractValidateException("balance is not sufficient"),
        "balance is not sufficient");
    assertError(new MaintenanceUnavailableException("maintenance in progress"),
        "maintenance in progress");
  }

  @Test
  public void unclassifiedTypesFailClosed() throws Exception {
    DecoderException decoder = assertThrows(DecoderException.class, () -> Hex.decode("zz"));
    Exception[] errors = {
        new NullPointerException("internal field name"),
        new JSONException("server serialization detail"),
        new InvalidProtocolBufferException("stored protobuf detail"),
        decoder,
        new HeaderNotFound("latest block not found"),
        new IllegalArgumentException("No enum constant internal.Type.VALUE"),
        new IllegalAccessException(RATE_LIMITER_ERROR_MSG),
        new IllegalAccessException("other access failure"),
        new ZkProofValidateException("wrapped validation detail", true)
    };

    for (Exception error : errors) {
      assertError(error, INTERNAL_SERVER_ERROR);
    }
  }

  @Test
  public void onlyExactFixedControlSignalsArePreserved() throws Exception {
    assertError(new IllegalArgumentException(Util.EVENTS_DEPRECATED_MSG),
        Util.EVENTS_DEPRECATED_MSG);
    assertError(new IllegalArgumentException("other argument failure"), INTERNAL_SERVER_ERROR);
    assertError(new NumberFormatException(Util.EVENTS_DEPRECATED_MSG), INTERNAL_SERVER_ERROR);
  }

  @Test
  public void nullBlankAndSubclassMessagesFailClosed() throws Exception {
    assertError(null, INTERNAL_SERVER_ERROR);
    assertError(new JsonFormat.ParseException(null), INTERNAL_SERVER_ERROR);
    assertError(new JsonFormat.ParseException(""), INTERNAL_SERVER_ERROR);
    assertError(new JsonFormat.ParseException("  "), INTERNAL_SERVER_ERROR);
    assertError(new ContractValidateException("subclass message") { }, INTERNAL_SERVER_ERROR);
  }

  @Test
  public void auditedErrorWriterPreservesTextVerbatim() throws Exception {
    for (String audited : new String[] {Util.INVALID_ADDRESS_MSG, Util.RATE_LIMITER_ERROR_MSG}) {
      MockHttpServletResponse response = new MockHttpServletResponse();
      Util.writeAuditedError(audited, response);
      JSONObject body = JSONObject.parseObject(response.getContentAsString());
      assertEquals(audited, body.getString("Error"));
    }
  }

  @Test
  public void auditedErrorWriterWithNullMessageWritesEmptyObject() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    Util.writeAuditedError(null, response);
    assertEquals("{}", response.getContentAsString().trim());
  }

  @Test
  public void serverErrorChannelSanitizesLikeTheSharedPath() throws Exception {
    assertServerError(new NullPointerException("internal field name"), INTERNAL_SERVER_ERROR);
    assertServerError(new ContractValidateException("balance is not sufficient"),
        "balance is not sufficient");
  }

  private static void assertServerError(Exception error, String expected) throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    Util.processServerError(error, response);
    JSONObject body = JSONObject.parseObject(response.getContentAsString());
    assertEquals(expected, body.getString("Error"));
  }

  private static void assertError(Exception error, String expected) throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    Util.processError(error, response);
    JSONObject body = JSONObject.parseObject(response.getContentAsString());
    assertEquals(expected, body.getString("Error"));
  }
}
