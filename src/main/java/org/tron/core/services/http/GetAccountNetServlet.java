package org.tron.core.services.http;

import java.io.IOException;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI.AccountNetMessage;
import org.tron.core.Wallet;
import org.tron.core.services.http.JsonFormat.ParseException;
import org.tron.protos.Protocol.Account;


@Component
@Slf4j
public class GetAccountNetServlet extends HttpServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    String account = request.getReader().lines()
        .collect(Collectors.joining(System.lineSeparator()));
    Account.Builder build = Account.newBuilder();
    try {
      JsonFormat.merge(account, build);
    } catch (ParseException e) {
      logger.debug("ParseException: {}", e.getMessage());
    }
    AccountNetMessage reply = wallet.getAccountNet(build.getAddress());
    response.getWriter().println(JsonFormat.printToString(reply));
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    doGet(request, response);
  }
}
