package org.tron.core.services.http.servlets;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.Wallet;
import org.tron.core.services.http.HttpApi;
import org.tron.core.services.http.HttpApi.Access;
import org.tron.core.services.http.HttpApi.Surface;
import org.tron.protos.contract.BalanceContract;

@Component
@Slf4j(topic = "API")
@HttpApi(value = "getaccountbalance", access = Access.READ,
    surfaces = {Surface.FULL})
public class GetAccountBalanceServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      PostParams params = PostParams.getPostParams(request);
      BalanceContract.AccountBalanceRequest.Builder builder
          = BalanceContract.AccountBalanceRequest.newBuilder();
      JsonFormat.merge(params.getParams(), builder, params.isVisible());
      fillResponse(params.isVisible(), builder.build(), response);
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  private void fillResponse(boolean visible,
                            BalanceContract.AccountBalanceRequest request,
                            HttpServletResponse response)
      throws Exception {
    BalanceContract.AccountBalanceResponse reply = wallet.getAccountBalance(request);
    if (reply != null) {
      response.getWriter().println(JsonFormat.printToString(reply, visible));
    } else {
      response.getWriter().println("{}");
    }
  }
}
