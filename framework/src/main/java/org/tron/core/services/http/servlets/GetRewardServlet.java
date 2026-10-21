package org.tron.core.services.http.servlets;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.db.Manager;
import org.tron.core.services.http.HttpApi;
import org.tron.core.services.http.HttpApi.Access;
import org.tron.core.services.http.HttpApi.Surface;

@Component
@Slf4j(topic = "API")
@HttpApi(value = "getReward", access = Access.READ,
    surfaces = {Surface.FULL, Surface.SOLIDITY, Surface.PBFT, Surface.SOLIDITY_NODE})
public class GetRewardServlet extends RateLimiterServlet {

  @Autowired
  private Manager manager;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    byte[] address;
    try {
      address = Util.getAddress(request);
    } catch (Exception e) {
      Util.processError(e, response);
      return;
    }
    try {
      long value = manager.getMortgageService().queryReward(address);
      String out = JsonFormat.isInt64AsString()
          ? "{\"reward\": \"" + value + "\"}"
          : "{\"reward\": " + value + "}";
      response.getWriter().println(out);
    } catch (Exception e) {
      Util.processServerError(e, response);
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    doGet(request, response);
  }
}
