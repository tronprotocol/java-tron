package org.tron.core.services.http;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI.PrivateParameters;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Transaction;


@Component
@Slf4j(topic = "API")
public class CreateShieldedTransactionServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {

  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      PostParams params = PostParams.getPostParams(request);
      PrivateParameters.Builder build = PrivateParameters.newBuilder();
      JsonFormat.merge(params.getParams(), build, params.isVisible());

      Transaction tx = wallet
          .createShieldedTransaction(build.build())
          .getInstance();
      response.getWriter().println(Util.printCreateTransaction(tx, params.isVisible()));
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }
}
