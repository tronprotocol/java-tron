package org.tron.core.services.http;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Block;


@Component
@Slf4j(topic = "API")
public class GetBlockByNumServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      long num = 0;
      String numStr = request.getParameter("num");
      if (numStr != null) {
        num = Long.parseLong(numStr);
      }
      fillResponse(Util.getVisible(request), num, response);
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      PostParams params = PostParams.getPostParams(request);
      NumberMessage.Builder build = NumberMessage.newBuilder();
      JsonFormat.merge(params.getParams(), build, params.isVisible());
      fillResponse(params.isVisible(), build.getNum(), response);
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  private void fillResponse(boolean visible, long num, HttpServletResponse response)
      throws IOException {
    Block reply = wallet.getBlockByNum(num);
    if (reply != null) {
      response.getWriter().println(Util.printBlock(reply, visible));
    } else {
      response.getWriter().println("{}");
    }
  }
}