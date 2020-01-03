package org.tron.core.services.http;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.services.util.WalletUtil;
import org.tron.protos.Protocol.Block;


@Component
@Slf4j(topic = "API")
public class GetNowBlockServlet extends RateLimiterServlet {

  @Autowired
  private WalletUtil wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      boolean visible = Util.getVisible(request);
      Block reply = wallet.getNowBlock();
      if (reply != null) {
        response.getWriter().println(Util.printBlock(reply, visible));
      } else {
        response.getWriter().println("{}");
      }
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    doGet(request, response);
  }
}