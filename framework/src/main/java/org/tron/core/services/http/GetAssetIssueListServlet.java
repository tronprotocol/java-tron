package org.tron.core.services.http;

import static org.tron.core.services.http.Util.existVisible;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI.AssetIssueList;
import org.tron.core.Wallet;

@Component
@Slf4j(topic = "API")
public class GetAssetIssueListServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      boolean visible = Util.getVisible(request);
      response(response, visible);
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      PostParams params = PostParams.getPostParams(request);
      boolean visible = Util.getVisible(request);
      if (!existVisible(request)) {
        visible = params.isVisible();
      }
      response(response, visible);
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  private void response(HttpServletResponse response, boolean visible) throws IOException {
    AssetIssueList reply = wallet.getAssetIssueList();
    if (reply != null) {
      response.getWriter().println(JsonFormat.printToString(reply, visible));
    } else {
      response.getWriter().println("{}");
    }
  }
}
