package org.tron.core.services.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.Arrays;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.tron.api.GrpcAPI.Return;
import org.tron.api.GrpcAPI.Return.response_code;
import org.tron.common.utils.Base58;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.StringUtil;
import org.tron.core.Wallet;
import org.tron.json.JSONObject;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.contract.BalanceContract.TransferContract;

public class BroadcastHexAddressValidationTest {

  @Test
  public void rendersOversizedAuthAddressAsHexWithoutBase58Conversion() throws Exception {
    for (int size : new int[]{22, 4096, 16384, 65536}) {
      Wallet wallet = mock(Wallet.class);
      Transaction transaction = transaction(size);
      when(wallet.broadcastTransaction(transaction)).thenReturn(Return.newBuilder()
          .setResult(false).setCode(response_code.CONTRACT_VALIDATE_ERROR).build());
      try (MockedStatic<Base58> base58 = mockStatic(Base58.class)) {
        JSONObject result = broadcast(transaction, wallet);
        assertEquals(response_code.CONTRACT_VALIDATE_ERROR.name(), result.getString("code"));
        assertTrue(result.getString("transaction")
            .contains(ByteArray.toHexString(bytes(size).toByteArray())));
        base58.verifyNoInteractions();
      }
    }
  }

  @Test
  public void preservesSuccessfulAndRejectedBroadcastResponses() throws Exception {
    Transaction transaction = transaction(21);
    for (response_code code : new response_code[]{response_code.SUCCESS, response_code.SIGERROR}) {
      Wallet wallet = mock(Wallet.class);
      boolean success = code == response_code.SUCCESS;
      Return result = Return.newBuilder().setResult(success).setCode(code)
          .setMessage(ByteString.copyFromUtf8("test result")).build();
      when(wallet.broadcastTransaction(transaction)).thenReturn(result);
      JSONObject response = broadcast(transaction, wallet);
      assertEquals(success, response.getBoolean("result"));
      assertEquals(code.name(), response.getString("code"));
      assertEquals("test result", response.getString("message"));
      assertEquals(JsonFormat.printToString(transaction, true), response.getString("transaction"));
      assertEquals(64, response.getString("txid").length());
      verify(wallet).broadcastTransaction(transaction);
    }
  }

  @Test
  public void jsonFormatterPreservesTwentyByteLogAddresses() {
    ByteString address = bytes(20);
    Protocol.TransactionInfo.Log log = Protocol.TransactionInfo.Log.newBuilder()
        .setAddress(address).build();
    JSONObject json = JSONObject.parseObject(JsonFormat.printToString(log, true));
    assertEquals(StringUtil.encode58Check(address.toByteArray()), json.getString("address"));
    assertEquals(ByteArray.toHexString(bytes(22).toByteArray()), JSONObject.parseObject(
        JsonFormat.printToString(log.toBuilder().setAddress(bytes(22)).build(), true))
        .getString("address"));
    assertEquals(ByteArray.toHexString(bytes(22).toByteArray()), JSONObject.parseObject(
        JsonFormat.printToString(log.toBuilder().setAddress(bytes(22)).build(), false))
        .getString("address"));
  }

  @Test
  public void rawAnyPayloadIsStillHexRatherThanAnExpandedAddress() {
    Transaction transaction = Transaction.newBuilder().setRawData(Transaction.raw.newBuilder()
        .addContract(Transaction.Contract.newBuilder()
            .setType(Transaction.Contract.ContractType.TransferContract)
            .setParameter(Any.pack(TransferContract.newBuilder()
                .setOwnerAddress(bytes(4096)).build())))).build();
    assertEquals(JsonFormat.printToString(transaction, false),
        JsonFormat.printToString(transaction, true));
  }

  private static JSONObject broadcast(Transaction transaction, Wallet wallet) throws Exception {
    BroadcastHexServlet servlet = new BroadcastHexServlet();
    Field walletField = BroadcastHexServlet.class.getDeclaredField("wallet");
    walletField.setAccessible(true);
    walletField.set(servlet, wallet);
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    JSONObject body = new JSONObject();
    body.put("transaction", ByteArray.toHexString(transaction.toByteArray()));
    when(request.getReader()).thenReturn(new BufferedReader(new StringReader(body.toJSONString())));
    StringWriter output = new StringWriter();
    when(response.getWriter()).thenReturn(new PrintWriter(output));
    servlet.doPost(request, response);
    return JSONObject.parseObject(output.toString());
  }

  private static Transaction transaction(int size) {
    return Transaction.newBuilder().setRawData(Transaction.raw.newBuilder()
        .addAuths(Protocol.authority.newBuilder().setAccount(Protocol.AccountId.newBuilder()
            .setAddress(bytes(size)))))
        .addSignature(ByteString.copyFrom(new byte[]{1})).build();
  }

  private static ByteString bytes(int size) {
    byte[] input = new byte[size];
    Arrays.fill(input, (byte) 0x41);
    return ByteString.copyFrom(input);
  }
}
