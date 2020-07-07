package org.tron.core.services.http;

import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI.PrivateShieldedTRC20Parameters;
import org.tron.api.GrpcAPI.ShieldedTRC20Parameters;
import org.tron.core.Wallet;

@Component
@Slf4j(topic = "API")
public class CreateShieldedContractParametersServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      String contract = request.getReader().lines()
          .collect(Collectors.joining(System.lineSeparator()));
      Util.checkBodySize(contract);

      boolean visible = Util.getVisiblePost(contract);
      PrivateShieldedTRC20Parameters.Builder build = PrivateShieldedTRC20Parameters.newBuilder();
      JsonFormat.merge(contract, build, visible);

      ShieldedTRC20Parameters shieldedTRC20Parameters = wallet
          .createShieldedContractParameters(build.build());
      response.getWriter().println(JsonFormat.printToString(shieldedTRC20Parameters, visible));
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }
}
