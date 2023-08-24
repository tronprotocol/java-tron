package org.tron.core.services.http;

import static org.tron.core.services.http.Util.existVisible;
import static org.tron.core.services.http.Util.getVisible;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Block;

@Component
@Slf4j(topic = "API")
public class GetNowBlockServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      boolean visible = getVisible(request);
      response(response, visible);
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      PostParams params = PostParams.getPostParams(request);
      boolean visible = getVisible(request);
      if (!existVisible(request)) {
        visible = params.isVisible();
      }
      response(response, visible);
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  private void response(HttpServletResponse response, boolean visible) throws IOException {
    Block reply = wallet.getNowBlock();
    if (reply != null) {
      response.getWriter().println(Util.printBlock(reply, visible));
    } else {
      response.getWriter().println("{}");
    }
  }
}