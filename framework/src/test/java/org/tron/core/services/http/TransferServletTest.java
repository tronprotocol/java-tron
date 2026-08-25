package org.tron.core.services.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.json.JSONObject;
import org.tron.protos.Protocol;
import org.tron.protos.contract.BalanceContract;

public class TransferServletTest extends BaseHttpTest {

  private TransferServlet servlet;
  private final String ownerAddr = ByteArray.toHexString(new ECKey().getAddress());
  private final String toAddr = ByteArray.toHexString(new ECKey().getAddress());

  @Override
  protected void setUpMocks() throws Exception {
    servlet = new TransferServlet();
    injectWallet(servlet);
    when(wallet.createTransactionCapsule(
        any(BalanceContract.TransferContract.class),
        eq(Protocol.Transaction.Contract.ContractType.TransferContract)))
        .thenReturn(new TransactionCapsule(MINIMAL_TX));
  }

  @Test
  public void testTransfer() throws Exception {
    String jsonParam = "{"
        + "\"owner_address\": \"" + ownerAddr + "\","
        + "\"to_address\": \"" + toAddr + "\","
        + "\"amount\": 100"
        + "}";
    MockHttpServletRequest request = postRequest(jsonParam);

    MockHttpServletResponse response = newResponse();
    servlet.doPost(request, response);
    verify(wallet).createTransactionCapsule(
        argThat(c -> c instanceof BalanceContract.TransferContract
            && addressEquals(((BalanceContract.TransferContract) c)
                .getOwnerAddress(), ownerAddr)
            && addressEquals(((BalanceContract.TransferContract) c)
                .getToAddress(), toAddr)
            && ((BalanceContract.TransferContract) c).getAmount() == 100),
        eq(Protocol.Transaction.Contract.ContractType.TransferContract));
    assertTransactionResponse(response);
  }

  private String transferJson(String permissionIdJson) {
    return "{"
        + "\"owner_address\": \"" + ownerAddr + "\","
        + "\"to_address\": \"" + toAddr + "\","
        + "\"amount\": 100,"
        + "\"Permission_id\": " + permissionIdJson
        + "}";
  }

  private MockHttpServletResponse post(String permissionIdJson) throws Exception {
    MockHttpServletResponse response = newResponse();
    servlet.doPost(postRequest(transferJson(permissionIdJson)), response);
    return response;
  }

  @Test
  public void testPermissionIdReachesTheBuiltTransaction() throws Exception {
    MockHttpServletResponse response = post("2");

    assertTransactionResponse(response);
    JSONObject contract = JSONObject.parseObject(response.getContentAsString())
        .getJSONObject("raw_data").getJSONArray("contract").getJSONObject(0);
    assertEquals(2, contract.getIntValue("Permission_id"));
  }

  /**
   * A fraction never reaches Util.setTransactionPermissionId. Permission_id is not a
   * TransferContract field, so JsonFormat.merge skips it through handleMissingField, which
   * accepts only integers, booleans, strings and null -- lookingAtInteger() sees the leading
   * digit, hands "1.9" to consumeInt64() and that fails first. Kept as a guard that the
   * endpoint rejects it whichever layer does the rejecting.
   */
  @Test
  public void testFractionalPermissionIdIsRejected() throws Exception {
    String content = post("1.9").getContentAsString();

    assertTrue("must report an error", content.contains("\"Error\""));
    assertFalse("must not hand back a transaction", content.contains("txID"));
    assertFalse(content.contains("\"raw_data\""));
  }

  /**
   * The reachable case: 2^32+1 is a valid int64, so it clears JsonFormat.merge and lands in
   * Util.setTransactionPermissionId, where BigDecimal.intValue() used to wrap it to 1 --
   * handing back a transaction built against permission 1, which the caller never asked for.
   */
  @Test
  public void testOutOfRangePermissionIdIsRejected() throws Exception {
    String content = post("4294967297").getContentAsString();

    assertTrue("must report an error", content.contains("\"Error\""));
    assertFalse("must not hand back a transaction built against permission 1",
        content.contains("txID"));
    assertFalse("must not echo the submitted value", content.contains("4294967297"));
  }

  @Test
  public void testQuotedScientificPermissionIdIsRejected() throws Exception {
    String content = post("\"1e2\"").getContentAsString();

    assertTrue("must report an error", content.contains("\"Error\""));
    assertFalse("must not hand back a transaction", content.contains("txID"));
    assertFalse(content.contains("\"raw_data\""));
  }

  /**
   * An explicit null used to reach getInteger(), whose null return blew up on unboxing with a
   * bare NullPointerException. It stays an error -- writing the key states intent, so a null
   * there is a caller-side defect. Omitting the key is how a caller asks for the default.
   */
  @Test
  public void testExplicitNullPermissionIdIsRejected() throws Exception {
    String content = post("null").getContentAsString();

    assertTrue("must report an error", content.contains("\"Error\""));
    assertFalse("must not hand back a transaction", content.contains("txID"));
    assertFalse("must not surface a NullPointerException",
        content.contains("NullPointerException"));
  }

  @Test
  public void testOmittedPermissionIdBuildsTransactionWithoutPermission() throws Exception {
    MockHttpServletResponse response = newResponse();
    servlet.doPost(postRequest("{"
        + "\"owner_address\": \"" + ownerAddr + "\","
        + "\"to_address\": \"" + toAddr + "\","
        + "\"amount\": 100"
        + "}"), response);

    assertTransactionResponse(response);
    JSONObject contract = JSONObject.parseObject(response.getContentAsString())
        .getJSONObject("raw_data").getJSONArray("contract").getJSONObject(0);
    assertFalse(contract.containsKey("Permission_id"));
  }
}
