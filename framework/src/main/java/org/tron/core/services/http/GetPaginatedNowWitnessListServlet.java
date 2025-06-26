package org.tron.core.services.http;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.WitnessList;
import org.tron.core.Wallet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


// Get the paged list of witnesses info with realtime vote counts
@Component
@Slf4j(topic = "API")
public class GetPaginatedNowWitnessListServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      boolean visible = Util.getVisible(request);
      long offset = Long.parseLong(request.getParameter("offset"));
      long limit = Long.parseLong(request.getParameter("limit"));
      fillResponse(offset, limit, visible, response);
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      PostParams params = PostParams.getPostParams(request);
      GrpcAPI.PaginatedMessage.Builder build = GrpcAPI.PaginatedMessage.newBuilder();
      JsonFormat.merge(params.getParams(), build, params.isVisible());
      fillResponse(build.getOffset(), build.getLimit(), params.isVisible(), response);
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  private void fillResponse(long offset, long limit, boolean visible, HttpServletResponse response)
      throws IOException {
    GrpcAPI.WitnessList reply = wallet.getPaginatedNowWitnessList(offset, limit);
    if (reply != null) {
      response.getWriter().println(JsonFormat.printToString(reply, visible));
    } else {
      response.getWriter().println("{}");
    }
  }
}
