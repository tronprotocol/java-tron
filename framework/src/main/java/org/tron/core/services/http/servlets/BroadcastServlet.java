package org.tron.core.services.http.servlets;

import com.google.protobuf.ByteString;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.exception.ContractSizeNotEqualToOneException;
import org.tron.core.services.http.HttpApi;
import org.tron.core.services.http.HttpApi.Access;
import org.tron.core.services.http.HttpApi.Surface;
import org.tron.json.JSONObject;
import org.tron.protos.Protocol.Transaction;

@Component
@Slf4j(topic = "API")
@HttpApi(value = "broadcasttransaction", access = Access.WRITE,
    surfaces = {Surface.FULL})
public class BroadcastServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    PostParams params;
    try {
      params = PostParams.getPostParams(request);
    } catch (Exception e) {
      Util.processError(e, response);
      return;
    }
    try {
      Transaction transaction = Util.packTransaction(params.getParams(), params.isVisible());
      TransactionCapsule transactionCapsule = new TransactionCapsule(transaction);
      String transactionID = ByteArray
          .toHexString(transactionCapsule.getTransactionId().getBytes());
      GrpcAPI.Return result = wallet.broadcastTransaction(transaction);
      response.getWriter().println(toResponse(result, transactionID, params.isVisible()));
    } catch (Exception e) {
      Util.processError(e, response, ContractSizeNotEqualToOneException.class,
          ex -> toResponse(contractSizeError(ex), "", params.isVisible()));
    }
  }

  private static GrpcAPI.Return contractSizeError(ContractSizeNotEqualToOneException e) {
    return GrpcAPI.Return.newBuilder()
        .setResult(false)
        .setCode(GrpcAPI.Return.response_code.CONTRACT_VALIDATE_ERROR)
        .setMessage(ByteString.copyFromUtf8(Wallet.CONTRACT_VALIDATE_ERROR + e.getMessage()))
        .build();
  }

  private static String toResponse(GrpcAPI.Return result, String txid, boolean visible) {
    JSONObject res = JSONObject.outboundParseObject(JsonFormat.printToString(result, visible));
    res.put("txid", txid);
    return res.toJSONString();
  }
}
