package org.tron.core.services.http.servlets;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI.TransactionApprovedList;
import org.tron.core.Wallet;
import org.tron.core.exception.ContractSizeNotEqualToOneException;
import org.tron.core.services.http.HttpApi;
import org.tron.core.services.http.HttpApi.Access;
import org.tron.core.services.http.HttpApi.Surface;
import org.tron.protos.Protocol.Transaction;

@Component
@Slf4j(topic = "API")
@HttpApi(value = "getapprovedlist", access = Access.READ,
    surfaces = {Surface.FULL})
public class GetTransactionApprovedListServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {

  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      PostParams params = PostParams.getPostParams(request);
      Transaction transaction = Util.packTransaction(params.getParams(), params.isVisible());
      TransactionApprovedList reply = wallet.getTransactionApprovedList(transaction);
      if (reply != null) {
        response.getWriter().println(Util.printTransactionApprovedList(reply, params.isVisible()));
      } else {
        response.getWriter().println("{}");
      }
    } catch (Exception e) {
      Util.processError(e, response, ContractSizeNotEqualToOneException.class,
          GetTransactionApprovedListServlet::contractSizeError);
    }
  }

  private static String contractSizeError(ContractSizeNotEqualToOneException e) {
    TransactionApprovedList.Result result = TransactionApprovedList.Result.newBuilder()
        .setCode(TransactionApprovedList.Result.response_code.OTHER_ERROR)
        .setMessage("Invalid transaction: " + e.getMessage())
        .build();
    TransactionApprovedList reply = TransactionApprovedList.newBuilder().setResult(result).build();
    return Util.printTransactionApprovedList(reply, false);
  }
}
