package org.tron.core.services.http;

import java.io.IOException;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI.AssetIssueList;
import org.tron.api.GrpcAPI.PaginatedMessage;
import org.tron.core.Wallet;
import org.tron.core.services.http.JsonFormat.ParseException;


@Component
@Slf4j
public class GetPaginatedAssetIssueListServlet extends HttpServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    String input = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
    PaginatedMessage.Builder build = PaginatedMessage.newBuilder();
    try {
      JsonFormat.merge(input, build);
    } catch (ParseException e) {
      logger.debug("ParseException: {}", e.getMessage());
    }
    AssetIssueList reply = wallet.getAssetIssueList(build.getOffset(), build.getLimit());
    response.getWriter().println(JsonFormat.printToString(reply));
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    doGet(request, response);
  }
}
