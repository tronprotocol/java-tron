package org.tron.core.services.http;

import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.api.GrpcAPI.SpendAuthSigParameters;
import org.tron.core.Wallet;


@Component
@Slf4j(topic = "API")
public class CreateSpendAuthSigServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {

  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      String input = request.getReader().lines()
          .collect(Collectors.joining(System.lineSeparator()));
      Util.checkBodySize(input);
      boolean visible = Util.getVisiblePost(input);

      SpendAuthSigParameters.Builder build = SpendAuthSigParameters.newBuilder();
      JsonFormat.merge(input, build);

      BytesMessage result = wallet.createSpendAuthSig(build.build());
      response.getWriter().println(JsonFormat.printToString(result, visible));
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }
}
