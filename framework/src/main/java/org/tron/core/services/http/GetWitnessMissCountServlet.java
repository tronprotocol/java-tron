package org.tron.core.services.http;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;

@Component
@Slf4j(topic = "API")
public class GetWitnessMissCountServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      byte[] address = Util.getAddress(request);
      if (address != null) {
        GrpcAPI.NumberMessage reply = wallet.getWitnessMissCount(address);
        if (reply != null) {
          response.getWriter().println(JsonFormat.printToString(reply, false));
        } else {
          response.getWriter().println("{}");
        }
      } else {
        response.getWriter().println("{}");
      }
    } catch (Exception e) {
      try {
        response.getWriter().println("{}");
      } catch (Exception e1) {
        Util.processError(e1, response);
      }
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    doGet(request, response);
  }
}
