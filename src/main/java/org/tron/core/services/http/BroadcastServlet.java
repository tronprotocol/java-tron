package org.tron.core.services.http;

import java.io.IOException;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI;
import org.tron.core.Wallet;
import org.tron.core.services.http.JsonFormat.ParseException;
import org.tron.protos.Protocol.Transaction;

@Component
@Slf4j
public class BroadcastServlet extends HttpServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String input = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
    Transaction.Builder build = Transaction.newBuilder();
    try {
      JsonFormat.merge(input, build);
    } catch (ParseException e) {
      logger.debug("ParseException: {}", e.getMessage());
    }
    GrpcAPI.Return retur = wallet.broadcastTransaction(build.build());
    response.getWriter().println(JsonFormat.printToString(retur));
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    doGet(request, response);
  }
}
