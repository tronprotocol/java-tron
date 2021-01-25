package org.tron.core.services.http;

import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.MarketOrderPairList;


@Component
@Slf4j(topic = "API")
public class GetMarketPairListServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      boolean visible = Util.getVisible(request);
      fillResponse(visible, response);
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      String input = request.getReader().lines()
          .collect(Collectors.joining(System.lineSeparator())).trim();
      Util.checkBodySize(input);

      boolean visible = false;
      if (!"".equals(input)) {
        visible = Util.getVisiblePost(input);
      }
      fillResponse(visible, response);
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  private void fillResponse(boolean visible, HttpServletResponse response)
      throws Exception {
    MarketOrderPairList reply = wallet.getMarketPairList();
    if (reply != null) {
      response.getWriter().println(JsonFormat.printToString(reply, visible));
    } else {
      response.getWriter().println("{}");
    }
  }
}
