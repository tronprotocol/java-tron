package org.tron.core.services.http;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI.PricesResponseMessage;
import org.tron.core.Wallet;

@Component
@Slf4j(topic = "API")
public class GetBandwidthPricesServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      PricesResponseMessage reply = wallet.getBandwidthPrices();
      response.getWriter().println(reply == null ? "{}" : JsonFormat.printToString(reply));
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    doGet(request, response);
  }
}
