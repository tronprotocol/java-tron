package org.tron.core.services.http;

import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.api.GrpcAPI.ShieldedTRC20TriggerContractParameters;
import org.tron.core.Wallet;

@Component
@Slf4j(topic = "API")
public class GetTriggerInputForShieldedTRC20ContractServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      PostParams params = PostParams.getPostParams(request);
      ShieldedTRC20TriggerContractParameters.Builder builder =
          ShieldedTRC20TriggerContractParameters
              .newBuilder();
      JsonFormat.merge(params.getParams(), builder, params.isVisible());
      BytesMessage result = wallet.getTriggerInputForShieldedTRC20Contract(builder.build());
      response.getWriter().println(JsonFormat.printToString(result, params.isVisible()));
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }
}
