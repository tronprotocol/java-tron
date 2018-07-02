package org.tron.core.services.http.solidity;

import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.stream.Collectors;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI.AccountPaginated;
import org.tron.api.GrpcAPI.TransactionList;
import org.tron.core.WalletSolidity;
import org.tron.core.services.http.JsonFormat;
import org.tron.core.services.http.JsonFormat.ParseException;

@Component
@Slf4j
public class GetTransactionsFromThisServlet extends HttpServlet {

  @Autowired
  private WalletSolidity walletSolidity;

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    String input = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
    AccountPaginated.Builder builder = AccountPaginated.newBuilder();
    try {
      JsonFormat.merge(input, builder);
    } catch (ParseException e) {
      logger.debug("ParseException: {}", e.getMessage());
    }

    AccountPaginated accountPaginated = builder.build();
    ByteString thisAddress = accountPaginated.getAccount().getAddress();
    long offset = accountPaginated.getOffset();
    long limit = accountPaginated.getLimit();
    if (thisAddress != null && offset >= 0 && limit >= 0) {
      TransactionList list = walletSolidity.getTransactionsFromThis(thisAddress, offset, limit);
      resp.getWriter().println(JsonFormat.printToString(list));
    } else {
      resp.getWriter().print("");
    }
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    super.doGet(req, resp);
  }


}
