package org.tron.core.services.http;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI;
import org.tron.core.Wallet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author kiven.miao
 * @date 2022/7/1 17:00
 */
@Component
@Slf4j(topic = "API")
public class GetOracleRewardPoolServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response){
    try{
      GrpcAPI.OracleRewardPoolMessage reply = wallet.getOracleRewardPool();
      if (reply != null) {
        response.getWriter().println(JsonFormat.printToString(reply, false));
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

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    doGet(request, response);
  }

}
