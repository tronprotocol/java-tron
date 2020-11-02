package org.tron.core.services.http;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI.ExchangeList;
import org.tron.api.GrpcAPI.PaginatedMessage;
import org.tron.core.Wallet;


@Component
@Slf4j(topic = "API")
public class GetPaginatedExchangeListServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      boolean visible = Util.getVisible(request);
      long offset = Long.parseLong(request.getParameter("offset"));
      long limit = Long.parseLong(request.getParameter("limit"));
      ExchangeList reply = wallet.getPaginatedExchangeList(offset, limit);
      if (reply != null) {
        response.getWriter().println(JsonFormat.printToString(reply, visible));
      } else {
        response.getWriter().println("{}");
      }
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      PostParams params = PostParams.getPostParams(request);
      PaginatedMessage.Builder build = PaginatedMessage.newBuilder();
      JsonFormat.merge(params.getParams(), build, params.isVisible());
      ExchangeList reply = wallet.getPaginatedExchangeList(build.getOffset(), build.getLimit());
      if (reply != null) {
        response.getWriter().println(JsonFormat.printToString(reply, params.isVisible()));
      } else {
        response.getWriter().println("{}");
      }
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }
}
