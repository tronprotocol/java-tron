package org.tron.core.services.http.solidity;

import java.io.IOException;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.core.WalletSolidity;
import org.tron.core.services.http.JsonFormat;
import org.tron.core.services.http.JsonFormat.ParseException;
import org.tron.protos.Protocol.Transaction;

@Component
@Slf4j
public class GetTransactionByIdServlet extends HttpServlet {

  @Autowired
  private WalletSolidity walletSolidity;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String input = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
    BytesMessage.Builder build = BytesMessage.newBuilder();
    try {
      JsonFormat.merge(input, build);
    } catch (ParseException e) {
      logger.debug("ParseException: {}", e.getMessage());
    }
    Transaction tx = walletSolidity.getTransactionById(build.build().getValue());
    response.getWriter().println(JsonFormat.printToString(tx));
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    doGet(request, response);
  }
}