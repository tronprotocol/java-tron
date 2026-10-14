package org.tron.core.services.http.servlets;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI.TransactionSignWeight;
import org.tron.core.exception.ContractSizeNotEqualToOneException;
import org.tron.core.services.http.HttpApi;
import org.tron.core.services.http.HttpApi.Access;
import org.tron.core.services.http.HttpApi.Surface;
import org.tron.core.utils.TransactionUtil;

@Component
@Slf4j(topic = "API")
@HttpApi(value = "getsignweight", access = Access.READ,
    surfaces = {Surface.FULL})
public class GetTransactionSignWeightServlet extends RateLimiterServlet {

  @Autowired
  private TransactionUtil transactionUtil;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {

  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      PostParams params = PostParams.getPostParams(request);
      TransactionSignWeight reply = Util.withPackedTransaction(params,
          transactionUtil::getTransactionSignWeight,
          GetTransactionSignWeightServlet::contractSizeError);
      if (reply != null) {
        response.getWriter().println(Util.printTransactionSignWeight(reply, params.isVisible()));
      } else {
        response.getWriter().println("{}");
      }
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  private static TransactionSignWeight contractSizeError(ContractSizeNotEqualToOneException e) {
    TransactionSignWeight.Result result = TransactionSignWeight.Result.newBuilder()
        .setCode(TransactionSignWeight.Result.response_code.OTHER_ERROR)
        .setMessage("Invalid transaction: " + e.getMessage())
        .build();
    return TransactionSignWeight.newBuilder().setResult(result).build();
  }
}
