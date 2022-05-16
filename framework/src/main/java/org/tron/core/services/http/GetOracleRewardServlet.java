package org.tron.core.services.http;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.db.Manager;
import org.tron.protos.Protocol.OracleReward;


@Component
@Slf4j(topic = "API")
public class GetOracleRewardServlet extends RateLimiterServlet {

  @Autowired
  private Manager manager;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      byte[] address = Util.getAddress(request);
      if (address != null) {
        OracleReward reward = manager.getMortgageService().queryOracleReward(address).getInstance();
        response.getWriter().println(JsonFormat.printToString(reward, false));
      } else {
        response.getWriter().println("{}");
      }
    } catch (Exception e) {
      logger.error("", e);
      try {
        response.getWriter().println(Util.printErrorMsg(e));
      } catch (IOException ioe) {
        logger.debug("IOException: {}", ioe.getMessage());
      }
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    doGet(request, response);
  }
}
